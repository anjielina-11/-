package com.merchant.ordering.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单 — 订单明细请求
 */
@Data
public class OrderItemRequest {

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品单价 */
    private BigDecimal productPrice;

    /** 商品图片 */
    private String productImage;

    /** 购买数量 */
    private Integer quantity;
}
