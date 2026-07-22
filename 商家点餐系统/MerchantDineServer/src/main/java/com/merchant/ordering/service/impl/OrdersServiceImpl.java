package com.merchant.ordering.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.merchant.ordering.dto.*;
import com.merchant.ordering.entity.OrderItem;
import com.merchant.ordering.entity.Orders;
import com.merchant.ordering.entity.User;
import com.merchant.ordering.mapper.OrderItemMapper;
import com.merchant.ordering.mapper.OrdersMapper;
import com.merchant.ordering.mapper.UserMapper;
import com.merchant.ordering.service.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单 Service 实现
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public OrderDetailVO createOrder(OrderCreateRequest request) {
        // 1. 生成订单编号
        String orderNo = generateOrderNo();

        // 2. 保存订单
        Orders orders = new Orders();
        orders.setOrderNo(orderNo);
        orders.setUserId(request.getUserId());
        orders.setTotalAmount(request.getTotalAmount());
        orders.setStatus(1);            // 待支付
        orders.setPayStatus(0);         // 未支付
        orders.setRemark(request.getRemark());
        this.save(orders);

        // 3. 保存订单明细
        List<OrderItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (OrderItemRequest itemReq : request.getItems()) {
                OrderItem item = new OrderItem();
                item.setOrderId(orders.getId());
                item.setProductId(itemReq.getProductId());
                item.setProductName(itemReq.getProductName());
                item.setProductPrice(itemReq.getProductPrice());
                item.setProductImage(itemReq.getProductImage());
                item.setQuantity(itemReq.getQuantity());
                item.setAmount(itemReq.getProductPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
                items.add(item);
            }
            for (OrderItem item : items) {
                orderItemMapper.insert(item);
            }
        }

        // 4. 组装返回
        return buildOrderDetailVO(orders, items);
    }

    @Override
    public List<OrderDetailVO> listByUserId(Long userId) {
        List<Orders> ordersList = this.lambdaQuery()
                .eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreateTime)
                .list();
        return ordersList.stream().map(o -> {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
            return buildOrderDetailVO(o, items);
        }).collect(Collectors.toList());
    }

    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        Orders orders = this.getById(orderId);
        if (orders == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        return buildOrderDetailVO(orders, items);
    }

    @Override
    public List<OrderDetailVO> listAll() {
        // 查询所有用户，构建 id -> nickname 映射
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));

        // 查询所有订单
        List<Orders> ordersList = this.lambdaQuery()
                .orderByDesc(Orders::getCreateTime)
                .list();

        return ordersList.stream().map(o -> {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
            OrderDetailVO vo = buildOrderDetailVO(o, items);
            vo.setUserName(userMap.get(o.getUserId()));
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 生成订单编号: ORD + yyyyMMdd + 3位序列
     */
    private String generateOrderNo() {
        String prefix = "ORD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询今日最大编号
        Orders last = this.lambdaQuery()
                .likeRight(Orders::getOrderNo, prefix)
                .orderByDesc(Orders::getOrderNo)
                .last("LIMIT 1")
                .one();
        int seq = 1;
        if (last != null && last.getOrderNo().length() == prefix.length() + 3) {
            try {
                seq = Integer.parseInt(last.getOrderNo().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {}
        }
        return prefix + String.format("%03d", seq);
    }

    /**
     * 构建 OrderDetailVO
     */
    private OrderDetailVO buildOrderDetailVO(Orders orders, List<OrderItem> items) {
        User user = userMapper.selectById(orders.getUserId());

        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(orders.getId());
        vo.setOrderNo(orders.getOrderNo());
        vo.setUserId(orders.getUserId());
        vo.setUserName(user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : null);
        vo.setTotalAmount(orders.getTotalAmount());
        vo.setStatus(orders.getStatus());
        vo.setPayTime(orders.getPayStatus() != null && orders.getPayStatus() == 1 ? orders.getUpdateTime() : null);
        vo.setRemark(orders.getRemark());
        vo.setCreateTime(orders.getCreateTime());
        vo.setUpdateTime(orders.getUpdateTime());

        // 转换明细
        List<OrderItemVO> itemVOs = items.stream().map(i -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setId(i.getId());
            itemVO.setOrderId(i.getOrderId());
            itemVO.setProductId(i.getProductId());
            itemVO.setProductName(i.getProductName());
            itemVO.setProductPrice(i.getProductPrice());
            itemVO.setProductImage(i.getProductImage());
            itemVO.setQuantity(i.getQuantity());
            itemVO.setAmount(i.getAmount());
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);

        return vo;
    }
}
