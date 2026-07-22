package com.merchant.ordering.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情返回 VO（含用户昵称 + 订单明细）
 */
@Data
public class OrderDetailVO {

    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 用户昵称 */
    private String userName;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态: 1=待支付, 2=已支付, 3=已完成, 4=已取消 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 订单明细 */
    private List<OrderItemVO> items;
}
