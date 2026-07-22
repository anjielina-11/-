package com.yunong.module.crop.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crops")
public class Crop extends BaseEntity {

    @TableId
    private String id;
    private String name;
    private String category;
    private String variety;
    private Integer growthDays;
    private BigDecimal optimalTempMin;
    private BigDecimal optimalTempMax;
    private String description;
    private String imageUrl;
}
