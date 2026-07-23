package com.yunong.module.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunong.module.review.entity.ReviewQueue;
import com.yunong.module.review.mapper.ReviewQueueMapper;
import com.yunong.module.review.service.ReviewQueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("审核队列服务测试")
class ReviewQueueServiceTest {

    @Mock private ReviewQueueMapper mapper;
    @InjectMocks private ReviewQueueService service;

    @Test
    @DisplayName("创建审核任务")
    void enqueue() {
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(mapper.insert(any(ReviewQueue.class))).thenAnswer(inv -> {
            ReviewQueue rq = inv.getArgument(0);
            rq.setId("rq-001");
            return 1;
        });

        var result = service.enqueue("diag-001", 5, "低置信度");
        assertThat(result.getDiagnosisId()).isEqualTo("diag-001");
        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(result.getPriority()).isEqualTo(5);
    }

    @Test
    @DisplayName("重复诊断不入队")
    void enqueueDuplicate() {
        var exist = new ReviewQueue();
        exist.setId("rq-001");
        exist.setDiagnosisId("diag-001");
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(exist);

        var result = service.enqueue("diag-001", 3, "再次提交");
        assertThat(result.getId()).isEqualTo("rq-001"); // 返回已有记录
        verify(mapper, never()).insert(any(ReviewQueue.class));
    }

    @Test
    @DisplayName("分配审核任务")
    void assign() {
        var rq = new ReviewQueue();
        rq.setId("rq-001");
        rq.setStatus("pending");
        when(mapper.selectById("rq-001")).thenReturn(rq);

        var result = service.assign("rq-001", "tech-001");
        assertThat(result.getStatus()).isEqualTo("assigned");
        assertThat(result.getAssignedTo()).isEqualTo("tech-001");
        assertThat(result.getAssignedAt()).isNotNull();
        verify(mapper).updateById(rq);
    }

    @Test
    @DisplayName("完成审核")
    void complete() {
        var rq = new ReviewQueue();
        rq.setId("rq-001");
        rq.setStatus("assigned");
        when(mapper.selectById("rq-001")).thenReturn(rq);

        var result = service.complete("rq-001");
        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getCompletedAt()).isNotNull();
        verify(mapper).updateById(rq);
    }
}
