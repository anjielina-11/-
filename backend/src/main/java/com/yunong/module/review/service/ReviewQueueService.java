package com.yunong.module.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.review.entity.ReviewQueue;
import com.yunong.module.review.mapper.ReviewQueueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewQueueService {

    private final ReviewQueueMapper mapper;

    /** 将诊断记录加入审核队列 */
    public ReviewQueue enqueue(String diagnosisId, int priority, String reason) {
        // 检查是否已存在
        var exist = mapper.selectOne(new LambdaQueryWrapper<ReviewQueue>()
                .eq(ReviewQueue::getDiagnosisId, diagnosisId));
        if (exist != null) return exist;

        var rq = new ReviewQueue();
        rq.setDiagnosisId(diagnosisId);
        rq.setPriority(priority);
        rq.setReason(reason);
        rq.setStatus("pending");
        mapper.insert(rq);
        return rq;
    }

    /** 分配审核任务 */
    public ReviewQueue assign(String id, String assignedTo) {
        var rq = mapper.selectById(id);
        if (rq == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        rq.setAssignedTo(assignedTo);
        rq.setAssignedAt(LocalDateTime.now());
        rq.setStatus("assigned");
        mapper.updateById(rq);
        return rq;
    }

    /** 完成审核 */
    public ReviewQueue complete(String id) {
        var rq = mapper.selectById(id);
        if (rq == null) throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        rq.setStatus("completed");
        rq.setCompletedAt(LocalDateTime.now());
        mapper.updateById(rq);
        return rq;
    }

    /** 审核队列列表 */
    public PageResult<ReviewQueue> list(int page, int size, String status, String assignedTo) {
        var wrapper = new LambdaQueryWrapper<ReviewQueue>();
        if (status != null) wrapper.eq(ReviewQueue::getStatus, status);
        if (assignedTo != null) wrapper.eq(ReviewQueue::getAssignedTo, assignedTo);
        wrapper.orderByAsc(ReviewQueue::getPriority);
        wrapper.orderByDesc(ReviewQueue::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }
}
