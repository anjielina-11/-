package com.yunong.module.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diagnosis_records")
public class DiagnosisRecord extends BaseEntity {

    @TableId
    private String id;
    private String observationId;
    private String imageUrl;
    private String imageHash;
    private String thumbnailUrl;
    private String diseaseName;
    private String diseaseCategory;
    private BigDecimal confidence;
    private String aiResult;
    private String modelVersionId;
    private String reviewStatus;
    private String reviewComment;
    private String reviewerId;
    private LocalDateTime reviewedAt;
    private String feedback;
    private LocalDateTime feedbackAt;
    private String severity;
}
