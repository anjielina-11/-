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
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.entity.Observation;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.task.service.TaskService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserMapper userMapper;
    private final FieldMapper fieldMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final AsyncDiagnosisService asyncDiagnosisService;
    private final TaskService taskService;

    /** 上传图片并提交异步推理 */
    public Map<String, Object> upload(MultipartFile file, String cycleId, String description, String userId) throws Exception {
        var cycle = plantingCycleMapper.selectById(cycleId);
        if (cycle == null) throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND);
        if (!userId.equals(cycle.getCreatedBy())) throw new BusinessException(ErrorCode.FORBIDDEN);
        validateUploadFile(file);

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

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择需要上传的病害图片");
        }
        var contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        var filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        boolean allowedType = Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(contentType);
        boolean allowedExtension = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif").stream()
                .anyMatch(filename::endsWith);
        if (!allowedType || !allowedExtension) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    "仅支持 JPG、PNG、WebP 或 GIF 图片");
        }
    }

    public DiagnosisRecord getById(String id, String userId, boolean privileged) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        assertOwner(dr, userId, privileged);
        return dr;
    }

    public DiagnosisImage getImage(String id, String userId, boolean privileged) {
        var dr = getById(id, userId, privileged);
        if (CharSequenceUtil.isBlank(dr.getImageUrl())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "诊断原图路径为空");
        }
        try (var object = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucket())
                .object(dr.getImageUrl())
                .build())) {
            return new DiagnosisImage(object.readAllBytes(), contentType(dr.getImageUrl()));
        } catch (Exception e) {
            log.error("读取诊断原图失败: id={}, object={}", id, dr.getImageUrl(), e);
            throw new BusinessException(ErrorCode.MINIO_ERROR, "读取诊断原图失败");
        }
    }

    private String contentType(String objectName) {
        var lower = objectName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    public record DiagnosisImage(byte[] content, String contentType) {}

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
        enrichDisplayFields(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    private void enrichDisplayFields(List<DiagnosisRecord> records) {
        if (records.isEmpty()) return;

        var observationIds = records.stream().map(DiagnosisRecord::getObservationId)
                .filter(Objects::nonNull).distinct().toList();
        if (observationIds.isEmpty()) return;
        Map<String, Observation> observations = obsMapper.selectBatchIds(observationIds).stream()
                .collect(java.util.stream.Collectors.toMap(Observation::getId, item -> item));

        var userIds = observations.values().stream().map(Observation::getUserId)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, User> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, item -> item));

        var cycleIds = observations.values().stream().map(Observation::getCycleId)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, PlantingCycle> cycles = cycleIds.isEmpty() ? Map.of() : plantingCycleMapper.selectBatchIds(cycleIds).stream()
                .collect(java.util.stream.Collectors.toMap(PlantingCycle::getId, item -> item));
        var fieldIds = cycles.values().stream().map(PlantingCycle::getFieldId)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, Field> fields = fieldIds.isEmpty() ? Map.of() : fieldMapper.selectBatchIds(fieldIds).stream()
                .collect(java.util.stream.Collectors.toMap(Field::getId, item -> item));

        for (var record : records) {
            var observation = observations.get(record.getObservationId());
            if (observation == null) continue;
            var user = users.get(observation.getUserId());
            if (user != null) {
                record.setFarmerName(CharSequenceUtil.isNotBlank(user.getRealName())
                        ? user.getRealName() : user.getUsername());
            }
            var cycle = cycles.get(observation.getCycleId());
            var field = cycle != null ? fields.get(cycle.getFieldId()) : null;
            if (field != null) record.setFieldName(field.getName());
        }
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
        Map<String, Object> contextSummary = Collections.emptyMap();
        List<Map<String, Object>> agentTrace = Collections.emptyList();
        if (CharSequenceUtil.isNotBlank(dr.getAiResult())) {
            try {
                var aiJson = JSONUtil.parseObj(dr.getAiResult());
                treatment = aiJson.getStr("treatment");
                var citationJson = aiJson.getJSONArray("citations");
                if (citationJson != null) {
                    citations = citationJson.stream()
                            .map(JSONUtil::parseObj)
                            .map(item -> new DiagnosisResultResponse.Citation(
                                    item.getStr("docTitle", item.getStr("source", "")),
                                    item.getStr("snippet", item.getStr("content", ""))))
                            .toList();
                }
                var contextJson = aiJson.getJSONObject("contextSummary");
                if (contextJson != null) contextSummary = new LinkedHashMap<>(contextJson);
                var traceJson = aiJson.getJSONArray("agentTrace");
                if (traceJson != null) {
                    agentTrace = traceJson.stream()
                            .map(JSONUtil::parseObj)
                            .map(item -> (Map<String, Object>) new LinkedHashMap<String, Object>(item))
                            .toList();
                }
            } catch (Exception ex) {
                log.warn("诊断 {} 的 AI 结果解析失败，将返回基础结果: {}", dr.getId(), ex.getMessage());
            }
        }
        return new DiagnosisResultResponse(status, dr.getDiseaseName(),
                dr.getConfidence() != null ? dr.getConfidence() : BigDecimal.ZERO, treatment, citations,
                contextSummary, agentTrace);
    }

    @Transactional
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
                catch (Exception ex) {
                    log.warn("诊断 {} 的防治建议解析失败，将保留原始结果: {}", dr.getId(), ex.getMessage());
                }
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
