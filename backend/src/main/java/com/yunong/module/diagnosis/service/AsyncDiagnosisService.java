package com.yunong.module.diagnosis.service;

import cn.hutool.json.JSONUtil;
import com.yunong.config.MinioConfig;
import com.yunong.integration.ai.AiServiceClient;
import com.yunong.integration.ai.dto.AgentAdviceRequest;
import com.yunong.integration.ai.dto.AgentAdviceResponse;
import com.yunong.module.agent.service.AgentRunService;
import com.yunong.module.diagnosis.dto.DiagnosisContext;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.review.service.ReviewQueueService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;

/**
 * 异步诊断推理服务 —— 对接 Python FastAPI AI 服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncDiagnosisService {

    private final DiagnosisRecordMapper drMapper;
    private final AgentRunService agentRunService;
    private final ReviewQueueService reviewQueueService;
    private final DiagnosisContextService diagnosisContextService;
    private final AiServiceClient aiServiceClient;
    private final RestTemplate restTemplate;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Qualifier("aiServiceUrl")
    private final String aiServiceUrl;

    private static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("0.85");
    private static final BigDecimal UNKNOWN_THRESHOLD = new BigDecimal("0.60");
    private static final String ADVICE_FALLBACK = "防治建议生成失败，请联系农技人员手动审核。";

    @Async("diagnosisExecutor")
    public void processAsync(String diagnosisId, String imageUrl) {
        log.info("开始异步诊断: diagnosisId={}, imageUrl={}", diagnosisId, imageUrl);
        var run = agentRunService.start(diagnosisId, "diagnosis", "yunnong-classifier",
                JSONUtil.toJsonStr(Map.of("imageUrl", imageUrl)));

        try {
            var diagnosisResp = callImageDiagnosis(imageUrl);
            String diseaseName = (String) diagnosisResp.getOrDefault("disease_name", "未知病害");
            double confidenceVal = ((Number) diagnosisResp.getOrDefault("confidence", 0.0)).doubleValue();
            BigDecimal confidence = BigDecimal.valueOf(confidenceVal);

            if (confidence.compareTo(UNKNOWN_THRESHOLD) < 0) {
                String rejectionMessage = String.format(
                        Locale.ROOT,
                        "本地模型最高候选置信度 %.2f%%，低于 60%% 识别阈值，已转入人工审核队列。",
                        confidenceVal * 100
                );
                var rejectedAdvice = new AgentAdviceResponse(
                        rejectionMessage, List.of(), Map.of(), List.of());
                var output = buildAiResult("未知病害", confidence, rejectedAdvice, List.of());
                saveDiagnosisResult(diagnosisId, "未知病害", confidence, output);
                agentRunService.complete(run.getId(), JSONUtil.toJsonStr(output), 0, BigDecimal.ZERO);
                log.info("异步诊断完成(拒识): diagnosisId={}, confidence={}", diagnosisId, confidence);
                return;
            }

            DiagnosisContext context = diagnosisContextService.load(diagnosisId);
            List<Map<String, Object>> citations = aiServiceClient.retrieveKnowledge(diseaseName + " 防治方法", 3);
            AgentAdviceResponse adviceResponse = callAgentGenerate(diseaseName, confidence, citations, context);
            var output = buildAiResult(diseaseName, confidence, adviceResponse, citations);

            saveDiagnosisResult(diagnosisId, diseaseName, confidence, output);
            agentRunService.complete(run.getId(), JSONUtil.toJsonStr(output),
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

    private Map<String, Object> buildAiResult(String diseaseName, BigDecimal confidence,
                                               AgentAdviceResponse adviceResponse,
                                               List<Map<String, Object>> retrievedCitations) {
        String treatment = adviceResponse.advice() == null || adviceResponse.advice().isBlank()
                ? ADVICE_FALLBACK : adviceResponse.advice();
        List<Map<String, Object>> citations = adviceResponse.references().isEmpty()
                ? retrievedCitations : adviceResponse.references();

        var output = new LinkedHashMap<String, Object>();
        output.put("diseaseName", diseaseName);
        output.put("confidence", confidence);
        output.put("treatment", treatment);
        output.put("citations", citations);
        output.put("contextSummary", adviceResponse.contextSummary());
        output.put("agentTrace", adviceResponse.agentTrace());
        return output;
    }

    private void saveDiagnosisResult(String diagnosisId, String diseaseName, BigDecimal confidence,
                                     Map<String, Object> output) {
        var dr = drMapper.selectById(diagnosisId);
        if (dr != null) {
            dr.setDiseaseName(diseaseName);
            dr.setConfidence(confidence);
            dr.setAiResult(JSONUtil.toJsonStr(output));
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
     * POST /api/v1/diagnosis/simple → AI 图像分类。
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
            @Override
            public String getFilename() {
                int slash = objectName.lastIndexOf('/');
                return slash >= 0 ? objectName.substring(slash + 1) : objectName;
            }
        });

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                aiServiceUrl + "/api/v1/diagnosis/simple?crop_info=未知作物",
                new HttpEntity<>(body, headers), Map.class);
        return resp.getBody() != null ? resp.getBody() : Map.of();
    }

    /**
     * POST /api/v1/diagnosis/advice → Agent 生成结构化防治建议。
     */
    public AgentAdviceResponse callAgentGenerate(String diseaseName, BigDecimal confidence,
                                                  List<Map<String, Object>> citations,
                                                  DiagnosisContext context) {
        return aiServiceClient.generateAdvice(new AgentAdviceRequest(
                diseaseName,
                confidence,
                context.crop(),
                context.field(),
                context.weatherForecast(),
                citations
        ));
    }
}