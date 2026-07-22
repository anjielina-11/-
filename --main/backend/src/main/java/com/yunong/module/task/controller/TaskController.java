package com.yunong.module.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.task.entity.FarmingTask;
import com.yunong.module.task.mapper.FarmingTaskMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "农事任务", description = "任务CRUD、状态流转、农事日历")
public class TaskController {

    private final FarmingTaskMapper taskMapper;

    @PostMapping
    @Operation(summary = "创建农事任务")
    public R<FarmingTask> create(@Valid @RequestBody FarmingTask task,
                                  @AuthenticationPrincipal UserDetails principal) {
        task.setCreatedBy(principal.getUsername());
        if (task.getStatus() == null) task.setStatus("pending");
        taskMapper.insert(task);
        return R.ok(task);
    }

    @GetMapping
    @Operation(summary = "任务列表")
    public R<PageResult<FarmingTask>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String taskType) {
        var wrapper = new LambdaQueryWrapper<FarmingTask>();
        if (status != null) wrapper.eq(FarmingTask::getStatus, status);
        if (assigneeId != null) wrapper.eq(FarmingTask::getAssigneeId, assigneeId);
        if (taskType != null) wrapper.eq(FarmingTask::getTaskType, taskType);
        wrapper.orderByDesc(FarmingTask::getCreatedAt);
        var result = taskMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    public R<FarmingTask> update(@PathVariable String id, @RequestBody FarmingTask update) {
        var task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        if (update.getTitle() != null) task.setTitle(update.getTitle());
        if (update.getDescription() != null) task.setDescription(update.getDescription());
        if (update.getScheduledDate() != null) task.setScheduledDate(update.getScheduledDate());
        if (update.getPriority() != null) task.setPriority(update.getPriority());
        if (update.getRemark() != null) task.setRemark(update.getRemark());
        taskMapper.updateById(task);
        return R.ok(task);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "任务状态流转")
    public R<FarmingTask> updateStatus(
            @PathVariable String id,
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails principal) {
        var task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        task.setStatus(status);
        if ("completed".equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        }
        taskMapper.updateById(task);
        return R.ok(task);
    }

    @GetMapping("/calendar")
    @Operation(summary = "农事日历(按月)")
    public R<List<FarmingTask>> calendar(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails principal) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        var tasks = taskMapper.selectList(new LambdaQueryWrapper<FarmingTask>()
                .eq(FarmingTask::getAssigneeId, principal.getUsername())
                .ge(FarmingTask::getScheduledDate, start)
                .lt(FarmingTask::getScheduledDate, end)
                .orderByAsc(FarmingTask::getScheduledDate));
        return R.ok(tasks);
    }
}
