package com.merchant.ordering.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细返回 VO
 */
@Data
public class OrderItemVO {

    private Long id;

    private Long orderId;

    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品单价 */
    private BigDecimal productPrice;

    /** 商品图片 */
    private String productImage;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 */
    private BigDecimal amount;
}
