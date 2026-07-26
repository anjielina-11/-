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
