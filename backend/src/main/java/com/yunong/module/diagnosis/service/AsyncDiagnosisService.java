package com.yunong.module.diagnosis.service;

import cn.hutool.json.JSONUtil;
import com.yunong.module.agent.service.AgentRunService;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.review.service.ReviewQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 异步诊断推理服务 —— 模拟 AI 服务调用
 * 后续对接 FastAPI AI 服务时只需替换本类中的 mock 逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncDiagnosisService {

    private final DiagnosisRecordMapper drMapper;
    private final AgentRunService agentRunService;
    private final ReviewQueueService reviewQueueService;

    @Async("diagnosisExecutor")
    public void processAsync(String diagnosisId, String imageUrl) {
        log.info("开始异步诊断: diagnosisId={}", diagnosisId);
        // 记录 Agent 运行
        var run = agentRunService.start(diagnosisId, "diagnosis", "yunnong-classifier",
                JSONUtil.toJsonStr(Map.of("imageUrl", imageUrl)));

        try {
            // TODO: 替换为真实 AI 服务 HTTP 调用
            Thread.sleep(1500); // 模拟推理耗时

            // Mock 结果
            String diseaseName = "稻瘟病";
            BigDecimal confidence = new BigDecimal("0.92");
            String treatment = "1. 选用抗病品种\n2. 合理施肥，避免偏施氮肥\n3. 抽穗期喷施三环唑防治\n4. 及时清除病残体";
            var citations = List.of(
                    Map.of("docTitle", "水稻病虫害防治规范", "snippet", "抽穗期遇阴雨天气，稻瘟病易流行，应及时喷药防治。"),
                    Map.of("docTitle", "云南省农业技术推广手册", "snippet", "稻瘟病是云南水稻产区主要病害之一，三环唑为推荐药剂。")
            );

            var aiResult = JSONUtil.toJsonStr(Map.of(
                    "diseaseName", diseaseName,
                    "confidence", confidence,
                    "treatment", treatment,
                    "citations", citations
            ));

            // 更新诊断记录
            var dr = drMapper.selectById(diagnosisId);
            if (dr != null) {
                dr.setDiseaseName(diseaseName);
                dr.setConfidence(confidence);
                dr.setAiResult(aiResult);
                dr.setReviewStatus(confidence.compareTo(new BigDecimal("0.85")) < 0 ? "pending_review" : "pending");
                dr.setSeverity(confidence.compareTo(new BigDecimal("0.90")) >= 0 ? "high" : "medium");
                drMapper.updateById(dr);

                // 低置信度 → 加入审核队列
                if (confidence.compareTo(new BigDecimal("0.85")) < 0) {
                    reviewQueueService.enqueue(diagnosisId, 5, "AI 置信度不足(" + confidence + ")，需人工审核");
                }
            }

            agentRunService.complete(run.getId(), aiResult, 150, new java.math.BigDecimal("0.003"));
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
}
