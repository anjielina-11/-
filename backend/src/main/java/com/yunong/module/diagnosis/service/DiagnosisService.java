package com.yunong.module.diagnosis.service;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.config.MinioConfig;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.diagnosis.dto.DiagnosisResultResponse;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.entity.Observation;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.task.service.TaskService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRecordMapper drMapper;
    private final ObservationMapper obsMapper;
    private final PlantingCycleMapper plantingCycleMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final AsyncDiagnosisService asyncDiagnosisService;
    private final TaskService taskService;

    /** 上传图片并提交异步推理 */
    public Map<String, Object> upload(MultipartFile file, String cycleId, String description, String userId) throws Exception {
        var cycle = plantingCycleMapper.selectById(cycleId);
        if (cycle == null) throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND);
        if (!userId.equals(cycle.getCreatedBy())) throw new BusinessException(ErrorCode.FORBIDDEN);

        String hash = DigestUtil.sha256Hex(file.getBytes());
        if (drMapper.selectCount(new LambdaQueryWrapper<DiagnosisRecord>()
                .eq(DiagnosisRecord::getImageHash, hash)) > 0) {
            throw new BusinessException(ErrorCode.IMAGE_HASH_DUPLICATE);
        }

        String objectName = "diagnosis/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        var obs = new Observation();
        obs.setCycleId(cycleId);
        obs.setUserId(userId);
        obs.setObservationType("image");
        obs.setDescription(description);
        obs.setImages("[\"" + objectName + "\"]");
        obs.setObservedAt(LocalDateTime.now());
        obsMapper.insert(obs);

        var dr = new DiagnosisRecord();
        dr.setObservationId(obs.getId());
        dr.setImageUrl(objectName);
        dr.setImageHash(hash);
        dr.setReviewStatus("pending");
        drMapper.insert(dr);

        asyncDiagnosisService.processAsync(dr.getId(), objectName);

        var result = new HashMap<String, Object>();
        result.put("diagnosisId", dr.getId());
        result.put("observationId", obs.getId());
        result.put("imageUrl", objectName);
        result.put("status", "processing");
        return result;
    }

    public DiagnosisRecord getById(String id, String userId, boolean privileged) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        assertOwner(dr, userId, privileged);
        return dr;
    }

    public PageResult<DiagnosisRecord> list(int page, int size, String reviewStatus, String diseaseName,
                                            String userId, boolean privileged) {
        var wrapper = new LambdaQueryWrapper<DiagnosisRecord>();
        if (!privileged) {
            var observationIds = obsMapper.selectList(new LambdaQueryWrapper<Observation>()
                            .eq(Observation::getUserId, userId)).stream()
                    .map(Observation::getId).toList();
            if (observationIds.isEmpty()) return PageResult.of(List.of(), 0);
            wrapper.in(DiagnosisRecord::getObservationId, observationIds);
        }
        if (reviewStatus != null) wrapper.eq(DiagnosisRecord::getReviewStatus, reviewStatus);
        if (diseaseName != null) wrapper.eq(DiagnosisRecord::getDiseaseName, diseaseName);
        wrapper.orderByDesc(DiagnosisRecord::getCreatedAt);
        var result = drMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public DiagnosisResultResponse getResult(String taskId, String userId, boolean privileged) {
        var dr = drMapper.selectById(taskId);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        assertOwner(dr, userId, privileged);

        String status;
        if ("approved".equals(dr.getReviewStatus())) status = "completed";
        else if ("failed".equals(dr.getReviewStatus())) status = "failed";
        else if (CharSequenceUtil.isNotBlank(dr.getAiResult())) status = "need_review";
        else status = "processing";

        String treatment = null;
        List<DiagnosisResultResponse.Citation> citations = Collections.emptyList();
        if (CharSequenceUtil.isNotBlank(dr.getAiResult())) {
            try {
                var aiJson = JSONUtil.parseObj(dr.getAiResult());
                treatment = aiJson.getStr("treatment");
                citations = aiJson.getJSONArray("citations").stream()
                        .map(item -> JSONUtil.parseObj(item))
                        .map(item -> new DiagnosisResultResponse.Citation(
                                item.getStr("docTitle", item.getStr("source", "")),
                                item.getStr("snippet", item.getStr("content", ""))))
                        .toList();
            } catch (Exception ignored) {}
        }
        return new DiagnosisResultResponse(status, dr.getDiseaseName(),
                dr.getConfidence() != null ? dr.getConfidence() : BigDecimal.ZERO, treatment, citations);
    }

    public DiagnosisRecord review(String id, String status, String comment, String reviewerId) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        if (!Set.of("pending", "pending_review").contains(dr.getReviewStatus()))
            throw new BusinessException(ErrorCode.DIAGNOSIS_ALREADY_REVIEWED);
        if (!Set.of("approved", "rejected").contains(status))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核结果只能为 approved/rejected");

        dr.setReviewStatus(status);
        dr.setReviewComment(comment);
        dr.setReviewerId(reviewerId);
        dr.setReviewedAt(LocalDateTime.now());
        drMapper.updateById(dr);

        if ("approved".equals(status)) {
            var obs = obsMapper.selectById(dr.getObservationId());
            String treatment = dr.getAiResult();
            if (CharSequenceUtil.isNotBlank(dr.getAiResult())) {
                try { treatment = JSONUtil.parseObj(dr.getAiResult()).getStr("treatment"); }
                catch (Exception ignored) {}
            }
            taskService.autoCreateFromDiagnosis(dr.getId(), dr.getDiseaseName(), treatment,
                    obs != null ? obs.getUserId() : reviewerId, obs != null ? obs.getCycleId() : null);
        }
        return dr;
    }

    public DiagnosisRecord feedback(String id, String feedback, String userId) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        assertOwner(dr, userId, false);
        dr.setFeedback(feedback);
        dr.setFeedbackAt(LocalDateTime.now());
        drMapper.updateById(dr);
        return dr;
    }

    private void assertOwner(DiagnosisRecord dr, String userId, boolean privileged) {
        if (privileged) return;
        var observation = obsMapper.selectById(dr.getObservationId());
        if (observation == null || !userId.equals(observation.getUserId()))
            throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    public Map<String, Object> stats() {
        var result = new HashMap<String, Object>();
        result.put("total", drMapper.selectCount(null));
        result.put("pending", drMapper.selectCount(
                new LambdaQueryWrapper<DiagnosisRecord>().eq(DiagnosisRecord::getReviewStatus, "pending")));
        result.put("approved", drMapper.selectCount(
                new LambdaQueryWrapper<DiagnosisRecord>().eq(DiagnosisRecord::getReviewStatus, "approved")));
        result.put("rejected", drMapper.selectCount(
                new LambdaQueryWrapper<DiagnosisRecord>().eq(DiagnosisRecord::getReviewStatus, "rejected")));
        return result;
    }
}
