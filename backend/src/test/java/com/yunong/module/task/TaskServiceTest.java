package com.yunong.module.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.task.entity.FarmingTask;
import com.yunong.module.task.mapper.FarmingTaskMapper;
import com.yunong.module.task.service.TaskService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

class TaskServiceTest {

    @Test
    void rejectsStatusUpdateByNonAssigneeAndInvalidStatus() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        var task = task("pending");
        when(mapper.selectById("task-1")).thenReturn(task);
        var service = service(mapper);

        var ownershipError = assertThrows(BusinessException.class,
                () -> service.updateStatus("task-1", "completed", "farmer-2", false));
        assertEquals(ErrorCode.NOT_TASK_ASSIGNEE.getCode(), ownershipError.getCode());

        var statusError = assertThrows(BusinessException.class,
                () -> service.updateStatus("task-1", "unknown", "farmer-1", false));
        assertEquals(ErrorCode.TASK_STATUS_INVALID.getCode(), statusError.getCode());
    }

    @Test
    void cancelsOnlyPendingTask() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        var pending = task("pending");
        when(mapper.selectById("task-1")).thenReturn(pending);
        var service = service(mapper);

        var cancelled = service.updateStatus("task-1", "cancelled", "farmer-1", false);
        assertEquals("cancelled", cancelled.getStatus());
        verify(mapper).updateById(pending);

        pending.setStatus("in_progress");
        var error = assertThrows(BusinessException.class,
                () -> service.updateStatus("task-1", "cancelled", "farmer-1", false));
        assertEquals(ErrorCode.TASK_CANNOT_CANCEL.getCode(), error.getCode());
    }

    @Test
    void enrichesTaskFieldNameAndUsesChineseDiseaseTitle() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        PlantingCycleMapper cycleMapper = mock(PlantingCycleMapper.class);
        FieldMapper fieldMapper = mock(FieldMapper.class);
        var service = new TaskService(mapper, cycleMapper, fieldMapper);

        var task = new FarmingTask();
        task.setId("task-1");
        task.setCycleId("cycle-1");
        var page = new Page<FarmingTask>(1, 10);
        page.setRecords(java.util.List.of(task));
        page.setTotal(1);
        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);

        var cycle = new PlantingCycle();
        cycle.setId("cycle-1");
        cycle.setFieldId("field-1");
        when(cycleMapper.selectBatchIds(java.util.List.of("cycle-1"))).thenReturn(java.util.List.of(cycle));
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);
        var field = new Field();
        field.setId("field-1");
        field.setName("一号示范地块");
        when(fieldMapper.selectBatchIds(java.util.List.of("field-1"))).thenReturn(java.util.List.of(field));

        var result = service.list(1, 10, null, null, null, "farmer-1", false);
        assertEquals("一号示范地块", result.getList().getFirst().getFieldName());

        var generated = service.autoCreateFromDiagnosis("diag-1", "citrus_canker", "处理建议",
                "farmer-1", "cycle-1");
        assertEquals("防治：柑橘溃疡病", generated.getTitle());
    }


    @Test
    void validatesTaskCreationAndForcesPendingStatus() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        PlantingCycleMapper cycleMapper = mock(PlantingCycleMapper.class);
        var service = new TaskService(mapper, cycleMapper, mock(FieldMapper.class));
        var task = new FarmingTask();
        task.setTitle("\u55b7\u836f");
        task.setTaskType("treatment");
        task.setAssigneeId("farmer-1");
        task.setPriority(3);
        task.setScheduledDate(LocalDate.now());
        task.setStatus("completed");

        var created = service.create(task, "tech-1");

        assertEquals("pending", created.getStatus());
        verify(mapper).insert(task);

        var invalid = new FarmingTask();
        invalid.setTitle(" ");
        var error = assertThrows(BusinessException.class, () -> service.create(invalid, "tech-1"));
        assertEquals(ErrorCode.TASK_DATA_INVALID.getCode(), error.getCode());
    }

    @Test
    void rejectsTaskScheduledBeforeRelatedPlantingCycle() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        PlantingCycleMapper cycleMapper = mock(PlantingCycleMapper.class);
        var service = new TaskService(mapper, cycleMapper, mock(FieldMapper.class));
        var cycle = new PlantingCycle();
        cycle.setId("cycle-1");
        cycle.setPlantingDate(LocalDate.now());
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);
        var task = new FarmingTask();
        task.setTitle("\u65bd\u80a5");
        task.setTaskType("fertilizing");
        task.setAssigneeId("farmer-1");
        task.setPriority(2);
        task.setCycleId("cycle-1");
        task.setScheduledDate(LocalDate.now().minusDays(1));

        var error = assertThrows(BusinessException.class, () -> service.create(task, "tech-1"));

        assertEquals(ErrorCode.TASK_DATE_INVALID.getCode(), error.getCode());
    }

    @Test
    void enforcesForwardOnlyTaskStatusTransitions() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        var service = service(mapper);
        var pending = task("pending");
        when(mapper.selectById("task-1")).thenReturn(pending);
        var skipError = assertThrows(BusinessException.class,
                () -> service.updateStatus("task-1", "completed", "farmer-1", false));
        assertEquals(ErrorCode.TASK_STATUS_TRANSITION_INVALID.getCode(), skipError.getCode());

        pending.setStatus("in_progress");
        var reverseError = assertThrows(BusinessException.class,
                () -> service.updateStatus("task-1", "pending", "farmer-1", false));
        assertEquals(ErrorCode.TASK_STATUS_TRANSITION_INVALID.getCode(), reverseError.getCode());
    }

    @Test
    void blocksStructuralEditsAfterTaskCompletionButAllowsFeedbackRemark() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        var service = service(mapper);
        var completed = task("completed");
        when(mapper.selectById("task-1")).thenReturn(completed);
        var structural = new FarmingTask();
        structural.setTitle("\u4fee\u6539\u6807\u9898");
        var error = assertThrows(BusinessException.class,
                () -> service.update("task-1", structural, "farmer-1", false));
        assertEquals(ErrorCode.TASK_CANNOT_EDIT.getCode(), error.getCode());

        var feedback = new FarmingTask();
        feedback.setRemark("\u6548\u679c\u826f\u597d");
        var updated = service.update("task-1", feedback, "farmer-1", false);
        assertEquals("\u6548\u679c\u826f\u597d", updated.getRemark());
    }


    @Test
    void allowsRemarkOnOverdueTaskButRejectsReschedulingIntoPast() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        var service = service(mapper);
        var overdue = task("pending");
        overdue.setTitle("\u65bd\u80a5");
        overdue.setTaskType("fertilizing");
        overdue.setPriority(2);
        overdue.setScheduledDate(LocalDate.now().minusDays(1));
        when(mapper.selectById("task-1")).thenReturn(overdue);

        var feedback = new FarmingTask();
        feedback.setRemark("\u5df2\u8865\u5145\u8bf4\u660e");
        var updated = service.update("task-1", feedback, "farmer-1", false);
        assertEquals("\u5df2\u8865\u5145\u8bf4\u660e", updated.getRemark());

        var reschedule = new FarmingTask();
        reschedule.setScheduledDate(LocalDate.now().minusDays(2));
        var error = assertThrows(BusinessException.class,
                () -> service.update("task-1", reschedule, "farmer-1", false));
        assertEquals(ErrorCode.TASK_DATE_INVALID.getCode(), error.getCode());
    }

    private TaskService service(FarmingTaskMapper mapper) {
        return new TaskService(mapper, mock(PlantingCycleMapper.class), mock(FieldMapper.class));
    }

    private FarmingTask task(String status) {
        var task = new FarmingTask();
        task.setId("task-1");
        task.setAssigneeId("farmer-1");
        task.setStatus(status);
        return task;
    }
}
