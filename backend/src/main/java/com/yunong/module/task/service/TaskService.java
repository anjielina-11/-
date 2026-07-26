package com.yunong.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.task.entity.FarmingTask;
import com.yunong.module.task.mapper.FarmingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final FarmingTaskMapper mapper;
    private final PlantingCycleMapper cycleMapper;
    private final FieldMapper fieldMapper;

    private static final Map<String, String> DISEASE_NAMES = Map.ofEntries(
            Map.entry("citrus_canker", "柑橘溃疡病"),
            Map.entry("citrus_red_spider", "柑橘红蜘蛛"),
            Map.entry("corn_borer", "玉米螟"),
            Map.entry("corn_leaf_blight", "玉米大斑病"),
            Map.entry("corn_smut", "玉米黑粉病"),
            Map.entry("cotton_verticillium", "棉花黄萎病"),
            Map.entry("cucumber_downy_mildew", "黄瓜霜霉病"),
            Map.entry("cucumber_powdery_mildew", "黄瓜白粉病"),
            Map.entry("pepper_anthracnose", "辣椒炭疽病"),
            Map.entry("potato_late_blight", "马铃薯晚疫病"),
            Map.entry("rice_blast", "水稻稻瘟病"),
            Map.entry("rice_sheath_blight", "水稻纹枯病"),
            Map.entry("rice_stem_maggot", "水稻秆蝇"),
            Map.entry("soybean_pod_borer", "大豆食心虫"),
            Map.entry("tomato_gray_mold", "番茄灰霉病"),
            Map.entry("tomato_late_blight", "番茄晚疫病"),
            Map.entry("wheat_rust", "小麦锈病"),
            Map.entry("wheat_scab", "小麦赤霉病")
    );

    public FarmingTask create(FarmingTask task, String createdBy) {
        task.setCreatedBy(createdBy);
        if (task.getStatus() == null) task.setStatus("pending");
        mapper.insert(task);
        return task;
    }

    public PageResult<FarmingTask> list(int page, int size, String status, String assigneeId, String taskType,
                                        String currentUserId, boolean privileged) {
        var wrapper = new LambdaQueryWrapper<FarmingTask>();
        if (!privileged) wrapper.eq(FarmingTask::getAssigneeId, currentUserId);
        if (status != null) wrapper.eq(FarmingTask::getStatus, status);
        if (assigneeId != null) wrapper.eq(FarmingTask::getAssigneeId, assigneeId);
        if (taskType != null) wrapper.eq(FarmingTask::getTaskType, taskType);
        wrapper.orderByDesc(FarmingTask::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        enrichFieldNames(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public FarmingTask update(String id, FarmingTask update, String currentUserId, boolean privileged) {
        var task = mapper.selectById(id);
        if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        assertAssignee(task, currentUserId, privileged);
        if (update.getTitle() != null) task.setTitle(update.getTitle());
        if (update.getDescription() != null) task.setDescription(update.getDescription());
        if (update.getScheduledDate() != null) task.setScheduledDate(update.getScheduledDate());
        if (update.getPriority() != null) task.setPriority(update.getPriority());
        if (update.getRemark() != null) task.setRemark(update.getRemark());
        mapper.updateById(task);
        return task;
    }

    public FarmingTask updateStatus(String id, String status, String currentUserId, boolean privileged) {
        if (!Set.of("pending", "in_progress", "completed", "cancelled").contains(status))
            throw new BusinessException(ErrorCode.TASK_STATUS_INVALID);
        var task = mapper.selectById(id);
        if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        assertAssignee(task, currentUserId, privileged);
        if ("cancelled".equals(status) && !"pending".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_CANNOT_CANCEL);
        }
        if (("completed".equals(task.getStatus()) || "cancelled".equals(task.getStatus()))
                && !task.getStatus().equals(status)) {
            throw new BusinessException(ErrorCode.TASK_STATUS_INVALID, "已完成或已取消的任务不能再次流转");
        }
        task.setStatus(status);
        if ("completed".equals(status)) task.setCompletedAt(LocalDateTime.now());
        mapper.updateById(task);
        return task;
    }

    private void assertAssignee(FarmingTask task, String currentUserId, boolean privileged) {
        if (!privileged && !currentUserId.equals(task.getAssigneeId()))
            throw new BusinessException(ErrorCode.NOT_TASK_ASSIGNEE);
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
    private void enrichFieldNames(List<FarmingTask> tasks) {
        var cycleIds = tasks.stream()
                .map(FarmingTask::getCycleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (cycleIds.isEmpty()) return;

        Map<String, PlantingCycle> cycles = cycleMapper.selectBatchIds(cycleIds).stream()
                .collect(java.util.stream.Collectors.toMap(PlantingCycle::getId, item -> item));
        var fieldIds = cycles.values().stream()
                .map(PlantingCycle::getFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (fieldIds.isEmpty()) return;

        Map<String, Field> fields = fieldMapper.selectBatchIds(fieldIds).stream()
                .collect(java.util.stream.Collectors.toMap(Field::getId, item -> item));
        for (var task : tasks) {
            var cycle = cycles.get(task.getCycleId());
            var field = cycle != null ? fields.get(cycle.getFieldId()) : null;
            if (field != null) task.setFieldName(field.getName());
        }
    }

    public FarmingTask autoCreateFromDiagnosis(String diagnosisId, String diseaseName, String treatment,
                                                String assigneeId, String cycleId) {
        var task = new FarmingTask();
        task.setDiagnosisId(diagnosisId);
        task.setCycleId(cycleId);
        task.setTaskType("treatment");
        String displayDiseaseName = diseaseName == null ? "未知病害"
                : DISEASE_NAMES.getOrDefault(diseaseName.toLowerCase(), diseaseName);
        task.setTitle("防治：" + displayDiseaseName);
        task.setDescription(treatment != null ? treatment : "请根据诊断结果进行防治处理");
        task.setPriority(3);
        task.setStatus("pending");
        task.setAssigneeId(assigneeId);
        task.setScheduledDate(LocalDate.now().plusDays(1));
        mapper.insert(task);
        return task;
    }
}
