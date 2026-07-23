package com.yunong.module.task.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.task.entity.FarmingTask;
import com.yunong.module.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.yunong.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "农事任务", description = "任务CRUD、状态流转、农事日历")
public class TaskController {

    private final TaskService service;

    @PostMapping
    @AuditLog(action = "创建农事任务")
    @Operation(summary = "创建农事任务")
    public R<FarmingTask> create(@Valid @RequestBody FarmingTask task,
                                  @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.create(task, principal.getUserId()));
    }

    @GetMapping
    @Operation(summary = "任务列表")
    public R<PageResult<FarmingTask>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String taskType) {
        return R.ok(service.list(page, size, status, assigneeId, taskType));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "更新农事任务")
    @Operation(summary = "更新任务")
    public R<FarmingTask> update(@PathVariable String id, @RequestBody FarmingTask update) {
        return R.ok(service.update(id, update));
    }

    @PutMapping("/{id}/status")
    @AuditLog(action = "流转任务状态")
    @Operation(summary = "任务状态流转")
    public R<FarmingTask> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return R.ok(service.updateStatus(id, status));
    }

    @GetMapping("/calendar")
    @Operation(summary = "农事日历(按月)")
    public R<List<FarmingTask>> calendar(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.calendar(year, month, principal.getUserId()));
    }
}
