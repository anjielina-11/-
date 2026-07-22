package com.merchant.ordering.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 所属分类ID */
    private Long categoryId;

    /** 价格 */
    private BigDecimal price;

    /** 图片地址 */
    private String image;

    /** 库存 */
    private Integer stock;

    /** 销量 */
    private Integer sales;

    /** 商品描述 */
    private String description;

    /** 状态: 0=停售, 1=在售 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
