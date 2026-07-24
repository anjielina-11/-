package com.yunong.module.task;

import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.task.entity.FarmingTask;
import com.yunong.module.task.mapper.FarmingTaskMapper;
import com.yunong.module.task.service.TaskService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    @Test
    void rejectsStatusUpdateByNonAssigneeAndInvalidStatus() {
        FarmingTaskMapper mapper = mock(FarmingTaskMapper.class);
        var task = new FarmingTask();
        task.setId("task-1");
        task.setAssigneeId("farmer-1");
        when(mapper.selectById("task-1")).thenReturn(task);
        var service = new TaskService(mapper);

        var ownershipError = assertThrows(BusinessException.class,
                () -> service.updateStatus("task-1", "completed", "farmer-2", false));
        assertEquals(ErrorCode.NOT_TASK_ASSIGNEE.getCode(), ownershipError.getCode());

        var statusError = assertThrows(BusinessException.class,
                () -> service.updateStatus("task-1", "unknown", "farmer-1", false));
        assertEquals(ErrorCode.TASK_STATUS_INVALID.getCode(), statusError.getCode());
    }
}
