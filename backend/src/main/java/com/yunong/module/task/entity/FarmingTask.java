package com.yunong.module.task.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("farming_tasks")
public class FarmingTask extends BaseEntity {

    @TableId
    private String id;
    private String cycleId;
    private String diagnosisId;
    private String taskType;
    private String title;
    private String description;
    private Integer priority;
    private String status;
    private String assigneeId;
    private String createdBy;
    private LocalDate scheduledDate;
    private LocalDateTime completedAt;
    private String remark;
}
