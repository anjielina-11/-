package com.merchant.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.merchant.ordering.dto.OrderCreateRequest;
import com.merchant.ordering.dto.OrderDetailVO;
import com.merchant.ordering.entity.Orders;

import java.util.List;

/**
 * 订单 Service 接口
 */
public interface OrdersService extends IService<Orders> {

    /**
     * 创建订单（含明细）
     */
    OrderDetailVO createOrder(OrderCreateRequest request);

    /**
     * 根据用户ID获取订单列表
     */
    List<OrderDetailVO> listByUserId(Long userId);

    /**
     * 获取订单详情
     */
    OrderDetailVO getOrderDetail(Long orderId);

    /**
     * 获取所有订单
     */
    List<OrderDetailVO> listAll();
}
