package com.yunong.module.crop.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("planting_cycles")
public class PlantingCycle extends BaseEntity {

    @TableId
    private String id;
    private String fieldId;
    private String cropId;
    private LocalDate plantingDate;
    private LocalDate expectedHarvestDate;
    private LocalDate actualHarvestDate;
    private String growthStage;
    private String status;
    private BigDecimal areaMu;
    private String remark;
    private String createdBy;
}
