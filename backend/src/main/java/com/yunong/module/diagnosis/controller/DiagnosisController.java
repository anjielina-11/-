package com.yunong.module.diagnosis.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.diagnosis.dto.DiagnosisResultResponse;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.service.DiagnosisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagnosis")
@RequiredArgsConstructor
@Tag(name = "病害诊断", description = "图片上传、AI识别结果查询、人工审核")
public class DiagnosisController {

    private final DiagnosisService service;

    @PostMapping("/upload")
    @AuditLog(action = "上传病害图片")
    @Operation(summary = "上传病害图片(异步推理)")
    public R<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String cycleId,
            @RequestParam(required = false) String description,
            @AuthenticationPrincipal UserDetails principal) throws Exception {
        return R.ok(service.upload(file, cycleId, description, principal.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取诊断详情")
    public R<DiagnosisRecord> getById(@PathVariable String id) {
        return R.ok(service.getById(id));
    }

    @GetMapping
    @Operation(summary = "诊断列表")
    public R<PageResult<DiagnosisRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String diseaseName) {
        return R.ok(service.list(page, size, reviewStatus, diseaseName));
    }

    @GetMapping("/result/{taskId}")
    @Operation(summary = "查询AI识别结果")
    public R<DiagnosisResultResponse> getResult(@PathVariable String taskId) {
        return R.ok(service.getResult(taskId));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @AuditLog(action = "审核诊断")
    @Operation(summary = "诊断审核(农技人员)")
    public R<DiagnosisRecord> review(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserDetails principal) {
        return R.ok(service.review(id, status, comment, principal.getUsername()));
    }

    @PostMapping("/{id}/feedback")
    @AuditLog(action = "防治效果反馈")
    @Operation(summary = "防治效果反馈(农户)")
    public R<DiagnosisRecord> feedback(
            @PathVariable String id,
            @RequestParam String feedback) {
        return R.ok(service.feedback(id, feedback));
    }

    @GetMapping("/stats")
    @Operation(summary = "诊断统计")
    public R<Map<String, Object>> stats() {
        return R.ok(service.stats());
    }
}
