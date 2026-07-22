package com.merchant.ordering.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求
 */
@Data
public class OrderCreateRequest {

    /** 下单用户ID */
    private Long userId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 备注 */
    private String remark;

    /** 订单明细 */
    private List<OrderItemRequest> items;
}
