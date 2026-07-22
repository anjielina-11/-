package com.merchant.ordering.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.merchant.ordering.common.Result;
import com.merchant.ordering.entity.OrderItem;
import com.merchant.ordering.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单明细 Controller
 */
@RestController
@RequestMapping("/order-item")
public class OrderItemController {

    @Autowired
    private OrderItemService orderItemService;

    /**
     * 根据订单ID查询明细列表
     */
    @GetMapping("/list/{orderId}")
    public Result<List<OrderItem>> listByOrderId(@PathVariable Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return Result.ok(orderItemService.list(wrapper));
    }

    /**
     * 根据ID查询明细
     */
    @GetMapping("/{id}")
    public Result<OrderItem> getById(@PathVariable Long id) {
        OrderItem orderItem = orderItemService.getById(id);
        if (orderItem == null) {
            return Result.fail("订单明细不存在");
        }
        return Result.ok(orderItem);
    }

    /**
     * 新增明细
     */
    @PostMapping
    public Result<?> save(@RequestBody OrderItem orderItem) {
        orderItemService.save(orderItem);
        return Result.ok("新增成功");
    }

    /**
     * 删除明细
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        orderItemService.removeById(id);
        return Result.ok("删除成功");
    }
}
