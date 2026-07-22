package com.merchant.ordering.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态: 1=待支付, 2=已支付, 3=已完成, 4=已取消 */
    private Integer status;

    /** 支付状态: 0=未支付, 1=已支付 */
    private Integer payStatus;

    /** 支付方式: 1=微信支付, 2=支付宝 */
    private Integer payMethod;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
