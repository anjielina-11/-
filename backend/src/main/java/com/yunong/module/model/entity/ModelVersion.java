package com.yunong.module.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("model_versions")
public class ModelVersion {

    @TableId
    private String id;
    private String modelName;
    private String modelType;
    private String version;
    private BigDecimal accuracy;
    private BigDecimal precisionVal;
    private BigDecimal recallVal;
    private BigDecimal f1Score;
    private String modelPath;
    private String configJson;
    private String status;
    private LocalDateTime deployedAt;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
