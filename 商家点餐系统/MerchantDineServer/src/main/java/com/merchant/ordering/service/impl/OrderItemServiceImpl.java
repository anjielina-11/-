package com.merchant.ordering.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.merchant.ordering.entity.OrderItem;
import com.merchant.ordering.mapper.OrderItemMapper;
import com.merchant.ordering.service.OrderItemService;
import org.springframework.stereotype.Service;

/**
 * 订单明细 Service 实现
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
