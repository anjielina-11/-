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
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final AsyncDiagnosisService asyncDiagnosisService;
    private final TaskService taskService;

    /** 上传图片并提交异步推理 */
    public Map<String, Object> upload(MultipartFile file, String cycleId, String description, String userId) throws Exception {
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
        String imageUrl = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;

        var obs = new Observation();
        obs.setCycleId(cycleId);
        obs.setUserId(userId);
        obs.setObservationType("image");
        obs.setDescription(description);
        obs.setImages("[\"" + imageUrl + "\"]");
        obs.setObservedAt(LocalDateTime.now());
        obsMapper.insert(obs);

        var dr = new DiagnosisRecord();
        dr.setObservationId(obs.getId());
        dr.setImageUrl(imageUrl);
        dr.setImageHash(hash);
        dr.setReviewStatus("pending");
        drMapper.insert(dr);

        asyncDiagnosisService.processAsync(dr.getId(), imageUrl);

        var result = new HashMap<String, Object>();
        result.put("diagnosisId", dr.getId());
        result.put("observationId", obs.getId());
        result.put("imageUrl", imageUrl);
        result.put("status", "processing");
        return result;
    }

    public DiagnosisRecord getById(String id) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        return dr;
    }

    public PageResult<DiagnosisRecord> list(int page, int size, String reviewStatus, String diseaseName) {
        var wrapper = new LambdaQueryWrapper<DiagnosisRecord>();
        if (reviewStatus != null) wrapper.eq(DiagnosisRecord::getReviewStatus, reviewStatus);
        if (diseaseName != null) wrapper.eq(DiagnosisRecord::getDiseaseName, diseaseName);
        wrapper.orderByDesc(DiagnosisRecord::getCreatedAt);
        var result = drMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public DiagnosisResultResponse getResult(String taskId) {
        var dr = drMapper.selectById(taskId);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);

        String status;
        if ("approved".equals(dr.getReviewStatus())) status = "completed";
        else if ("rejected".equals(dr.getReviewStatus())) status = "need_review";
        else status = "processing";

        String treatment = null;
        List<DiagnosisResultResponse.Citation> citations = Collections.emptyList();
        if (CharSequenceUtil.isNotBlank(dr.getAiResult())) {
            try {
                var aiJson = JSONUtil.parseObj(dr.getAiResult());
                treatment = aiJson.getStr("treatment");
                citations = aiJson.getJSONArray("citations")
                        .toList(DiagnosisResultResponse.Citation.class);
            } catch (Exception ignored) {}
        }
        return new DiagnosisResultResponse(status, dr.getDiseaseName(),
                dr.getConfidence() != null ? dr.getConfidence() : BigDecimal.ZERO, treatment, citations);
    }

    public DiagnosisRecord review(String id, String status, String comment, String reviewerId) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        if (!"pending".equals(dr.getReviewStatus()))
            throw new BusinessException(ErrorCode.DIAGNOSIS_ALREADY_REVIEWED);

        dr.setReviewStatus(status);
        dr.setReviewComment(comment);
        dr.setReviewerId(reviewerId);
        dr.setReviewedAt(LocalDateTime.now());
        drMapper.updateById(dr);

        if ("approved".equals(status)) {
            var obs = obsMapper.selectById(dr.getObservationId());
            taskService.autoCreateFromDiagnosis(dr.getId(), dr.getDiseaseName(), dr.getAiResult(),
                    reviewerId, obs != null ? obs.getCycleId() : null);
        }
        return dr;
    }

    public DiagnosisRecord feedback(String id, String feedback) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        dr.setFeedback(feedback);
        dr.setFeedbackAt(LocalDateTime.now());
        drMapper.updateById(dr);
        return dr;
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
