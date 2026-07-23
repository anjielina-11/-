package com.yunong.module.review.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.review.entity.ReviewQueue;
import com.yunong.module.review.service.ReviewQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-queue")
@RequiredArgsConstructor
@Tag(name = "审核队列", description = "诊断审核任务的分配与处理")
public class ReviewQueueController {

    private final ReviewQueueService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @Operation(summary = "审核队列列表")
    public R<PageResult<ReviewQueue>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo) {
        return R.ok(service.list(page, size, status, assignedTo));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @AuditLog(action = "分配审核任务")
    @Operation(summary = "分配审核任务")
    public R<ReviewQueue> assign(@PathVariable String id, @RequestParam String assignedTo) {
        return R.ok(service.assign(id, assignedTo));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @AuditLog(action = "完成审核")
    @Operation(summary = "完成审核")
    public R<ReviewQueue> complete(@PathVariable String id) {
        return R.ok(service.complete(id));
    }
}
