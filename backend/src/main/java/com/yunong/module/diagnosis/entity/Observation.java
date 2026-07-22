package com.yunong.module.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("observations")
public class Observation extends BaseEntity {

    @TableId
    private String id;
    private String cycleId;
    private String userId;
    private String observationType;
    private String description;
    private String images;
    private String weatherInfo;
    private LocalDateTime observedAt;
}
