package com.yunong.module.diagnosis.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.config.MinioConfig;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.entity.Observation;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/diagnosis")
@RequiredArgsConstructor
@Tag(name = "病害诊断", description = "图片上传、AI识别结果查询、人工审核")
public class DiagnosisController {

    private final DiagnosisRecordMapper drMapper;
    private final ObservationMapper obsMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostMapping("/upload")
    @Operation(summary = "上传病害图片(异步推理)")
    public R<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String cycleId,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserDetails principal) throws Exception {

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
        obs.setUserId(principal.getUsername());
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

        var result = new HashMap<String, Object>();
        result.put("diagnosisId", dr.getId());
        result.put("observationId", obs.getId());
        result.put("imageUrl", imageUrl);
        result.put("status", "pending");
        return R.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取诊断详情")
    public R<DiagnosisRecord> getById(@PathVariable String id) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        return R.ok(dr);
    }

    @GetMapping
    @Operation(summary = "诊断列表")
    public R<PageResult<DiagnosisRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String diseaseName) {
        var wrapper = new LambdaQueryWrapper<DiagnosisRecord>();
        if (reviewStatus != null) wrapper.eq(DiagnosisRecord::getReviewStatus, reviewStatus);
        if (diseaseName != null) wrapper.eq(DiagnosisRecord::getDiseaseName, diseaseName);
        wrapper.orderByDesc(DiagnosisRecord::getCreatedAt);
        var result = drMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @Operation(summary = "诊断审核(农技人员)")
    public R<DiagnosisRecord> review(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserDetails principal) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        if (!"pending".equals(dr.getReviewStatus())) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_ALREADY_REVIEWED);
        }
        dr.setReviewStatus(status);
        dr.setReviewComment(comment);
        dr.setReviewerId(principal.getUsername());
        dr.setReviewedAt(LocalDateTime.now());
        drMapper.updateById(dr);
        return R.ok(dr);
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "防治效果反馈(农户)")
    public R<DiagnosisRecord> feedback(
            @PathVariable String id,
            @RequestParam String feedback,
            @AuthenticationPrincipal UserDetails principal) {
        var dr = drMapper.selectById(id);
        if (dr == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        dr.setFeedback(feedback);
        dr.setFeedbackAt(LocalDateTime.now());
        drMapper.updateById(dr);
        return R.ok(dr);
    }

    @GetMapping("/stats")
    @Operation(summary = "诊断统计")
    public R<Map<String, Object>> stats() {
        var result = new HashMap<String, Object>();
        result.put("total", drMapper.selectCount(null));
        result.put("pending", drMapper.selectCount(
                new LambdaQueryWrapper<DiagnosisRecord>().eq(DiagnosisRecord::getReviewStatus, "pending")));
        result.put("approved", drMapper.selectCount(
                new LambdaQueryWrapper<DiagnosisRecord>().eq(DiagnosisRecord::getReviewStatus, "approved")));
        result.put("rejected", drMapper.selectCount(
                new LambdaQueryWrapper<DiagnosisRecord>().eq(DiagnosisRecord::getReviewStatus, "rejected")));
        return R.ok(result);
    }
}
