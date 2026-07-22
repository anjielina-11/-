package com.yunong.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.task.entity.FarmingTask;
import com.yunong.module.task.mapper.FarmingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final FarmingTaskMapper mapper;

    public FarmingTask create(FarmingTask task, String createdBy) {
        task.setCreatedBy(createdBy);
        if (task.getStatus() == null) task.setStatus("pending");
        mapper.insert(task);
        return task;
    }

    public PageResult<FarmingTask> list(int page, int size, String status, String assigneeId, String taskType) {
        var wrapper = new LambdaQueryWrapper<FarmingTask>();
        if (status != null) wrapper.eq(FarmingTask::getStatus, status);
        if (assigneeId != null) wrapper.eq(FarmingTask::getAssigneeId, assigneeId);
        if (taskType != null) wrapper.eq(FarmingTask::getTaskType, taskType);
        wrapper.orderByDesc(FarmingTask::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public FarmingTask update(String id, FarmingTask update) {
        var task = mapper.selectById(id);
        if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        if (update.getTitle() != null) task.setTitle(update.getTitle());
        if (update.getDescription() != null) task.setDescription(update.getDescription());
        if (update.getScheduledDate() != null) task.setScheduledDate(update.getScheduledDate());
        if (update.getPriority() != null) task.setPriority(update.getPriority());
        if (update.getRemark() != null) task.setRemark(update.getRemark());
        mapper.updateById(task);
        return task;
    }

    public FarmingTask updateStatus(String id, String status) {
        var task = mapper.selectById(id);
        if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        task.setStatus(status);
        if ("completed".equals(status)) task.setCompletedAt(LocalDateTime.now());
        mapper.updateById(task);
        return task;
    }

    public List<FarmingTask> calendar(int year, int month, String assigneeId) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        return mapper.selectList(new LambdaQueryWrapper<FarmingTask>()
                .eq(FarmingTask::getAssigneeId, assigneeId)
                .ge(FarmingTask::getScheduledDate, start)
                .lt(FarmingTask::getScheduledDate, end)
                .orderByAsc(FarmingTask::getScheduledDate));
    }

    /** 审核通过后自动创建防治任务 */
    public FarmingTask autoCreateFromDiagnosis(String diagnosisId, String diseaseName, String treatment,
                                                String assigneeId, String cycleId) {
        var task = new FarmingTask();
        task.setDiagnosisId(diagnosisId);
        task.setCycleId(cycleId);
        task.setTaskType("treatment");
        task.setTitle("防治: " + (diseaseName != null ? diseaseName : "未知病害"));
        task.setDescription(treatment != null ? treatment : "请根据诊断结果进行防治处理");
        task.setPriority(3);
        task.setStatus("pending");
        task.setAssigneeId(assigneeId);
        task.setScheduledDate(LocalDate.now().plusDays(1));
        mapper.insert(task);
        return task;
    }
}
