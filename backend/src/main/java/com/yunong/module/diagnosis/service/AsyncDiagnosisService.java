package com.yunong.module.diagnosis.service;

import cn.hutool.json.JSONUtil;
import com.yunong.module.agent.service.AgentRunService;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.review.service.ReviewQueueService;
import com.yunong.config.MinioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 异步诊断推理服务 —— 对接 Python FastAPI AI 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncDiagnosisService {

    private final DiagnosisRecordMapper drMapper;
    private final AgentRunService agentRunService;
    private final ReviewQueueService reviewQueueService;
    private final RestTemplate restTemplate;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Qualifier("aiServiceUrl")
    private final String aiServiceUrl;

    private static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("0.85");
    private static final BigDecimal UNKNOWN_THRESHOLD = new BigDecimal("0.60");

    @Async("diagnosisExecutor")
    public void processAsync(String diagnosisId, String imageUrl) {
        log.info("开始异步诊断: diagnosisId={}, imageUrl={}", diagnosisId);
        var run = agentRunService.start(diagnosisId, "diagnosis", "yunnong-classifier",
                JSONUtil.toJsonStr(Map.of("imageUrl", imageUrl)));

        try {
            // 1. 调用 AI 图像分类
            var diagnosisResp = callImageDiagnosis(imageUrl);
            String diseaseName = (String) diagnosisResp.getOrDefault("disease_name", "未知病害");
            double confidenceVal = ((Number) diagnosisResp.getOrDefault("confidence", 0.0)).doubleValue();
            BigDecimal confidence = BigDecimal.valueOf(confidenceVal);

            // 2. 未知病害 → 拒识，入审核队列
            if (confidence.compareTo(UNKNOWN_THRESHOLD) < 0) {
                saveDiagnosisResult(diagnosisId, "未知病害", confidence,
                        "该样本暂时无法识别，已转入人工审核队列。", List.of());
                agentRunService.complete(run.getId(),
                        JSONUtil.toJsonStr(Map.of("diseaseName", "未知病害", "confidence", confidence)),
                        0, BigDecimal.ZERO);
                log.info("异步诊断完成(拒识): diagnosisId={}, confidence={}", diagnosisId, confidence);
                return;
            }

            // 3. RAG 检索相关防治文档
            var ragResp = callRagRetrieve(diseaseName);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> citations = (List<Map<String, Object>>) ragResp.getOrDefault("results", List.of());

            // 4. Agent 生成防治建议
            String treatment = callAgentGenerate(diseaseName, confidence, citations);

            // 5. 保存结果
            saveDiagnosisResult(diagnosisId, diseaseName, confidence, treatment, citations);

            agentRunService.complete(run.getId(),
                    JSONUtil.toJsonStr(Map.of("diseaseName", diseaseName, "confidence", confidence, "treatment", treatment)),
                    150, new BigDecimal("0.003"));
            log.info("异步诊断完成: diagnosisId={}, disease={}, confidence={}", diagnosisId, diseaseName, confidence);

        } catch (Exception e) {
            log.error("异步诊断失败: diagnosisId={}", diagnosisId, e);
            agentRunService.fail(run.getId(), e.getMessage());
            var dr = drMapper.selectById(diagnosisId);
            if (dr != null) {
                dr.setReviewStatus("failed");
                drMapper.updateById(dr);
            }
        }
    }

    private void saveDiagnosisResult(String diagnosisId, String diseaseName, BigDecimal confidence,
                                      String treatment, List<Map<String, Object>> citations) {
        var aiResult = JSONUtil.toJsonStr(Map.of(
                "diseaseName", diseaseName,
                "confidence", confidence,
                "treatment", treatment,
                "citations", citations
        ));

        var dr = drMapper.selectById(diagnosisId);
        if (dr != null) {
            dr.setDiseaseName(diseaseName);
            dr.setConfidence(confidence);
            dr.setAiResult(aiResult);
            dr.setReviewStatus(confidence.compareTo(REVIEW_THRESHOLD) < 0 ? "pending_review" : "pending");
            dr.setSeverity(confidence.compareTo(new BigDecimal("0.90")) >= 0 ? "high" : "medium");
            drMapper.updateById(dr);

            if (confidence.compareTo(REVIEW_THRESHOLD) < 0) {
                reviewQueueService.enqueue(diagnosisId, 5,
                        "AI 置信度不足(" + confidence + ")，需人工审核");
            }
        }
    }

    /**
     * POST /api/v1/diagnosis/simple → AI 图像分类
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> callImageDiagnosis(String objectName) throws Exception {
        byte[] imageBytes;
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucket()).object(objectName).build())) {
            imageBytes = stream.readAllBytes();
        }
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(imageBytes) {
            @Override public String getFilename() {
                int slash = objectName.lastIndexOf('/');
                return slash >= 0 ? objectName.substring(slash + 1) : objectName;
            }
        });

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                aiServiceUrl + "/api/v1/diagnosis/simple?crop_info=未知作物",
                new HttpEntity<>(body, headers), Map.class);
        return resp.getBody();
    }

    /**
     * POST /api/v1/rag/retrieve → 知识库检索
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callRagRetrieve(String diseaseName) {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                aiServiceUrl + "/api/v1/rag/retrieve",
                Map.of("query", diseaseName + " 防治方法", "top_k", 3),
                Map.class);
        return resp.getBody() != null ? resp.getBody() : Map.of("results", List.of());
    }

    /**
     * POST /api/v1/diagnosis/full → Agent 生成防治建议
     */
    @SuppressWarnings("unchecked")
    public String callAgentGenerate(String diseaseName, BigDecimal confidence,
                                    List<Map<String, Object>> citations) {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                aiServiceUrl + "/api/v1/diagnosis/advice",
                Map.of("disease_name", diseaseName, "confidence", confidence,
                        "crop_info", "未知作物", "weather_info", "未知天气", "citations", citations),
                Map.class);

        if (resp.getBody() != null && resp.getBody().get("advice") != null) {
            return resp.getBody().get("advice").toString();
        }
        return "防治建议生成失败，请联系农技人员手动审核。";
    }
}
