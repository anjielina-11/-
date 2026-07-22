package com.merchant.ordering.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品返回 VO（含分类名称）
 */
@Data
public class ProductVO {

    private Long id;

    /** 所属分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 价格 */
    private BigDecimal price;

    /** 图片地址 */
    private String image;

    /** 库存 */
    private Integer stock;

    /** 销量 */
    private Integer sales;

    /** 状态: 0=停售, 1=在售 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
