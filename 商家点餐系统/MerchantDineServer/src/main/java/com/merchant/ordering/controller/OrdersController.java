package com.merchant.ordering.controller;

import com.merchant.ordering.common.Result;
import com.merchant.ordering.dto.OrderCreateRequest;
import com.merchant.ordering.dto.OrderDetailVO;
import com.merchant.ordering.entity.User;
import com.merchant.ordering.service.OrdersService;
import com.merchant.ordering.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 Controller — 前缀 /api/order
 */
@RestController
@RequestMapping("/api/order")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private UserService userService;

    /**
     * 创建订单
     * POST /api/order
     */
    @PostMapping
    public Result<OrderDetailVO> create(@RequestBody OrderCreateRequest request) {
        OrderDetailVO vo = ordersService.createOrder(request);
        return Result.ok("下单成功", vo);
    }

    /**
     * 根据用户ID获取订单列表
     * GET /api/order/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Result<List<OrderDetailVO>> listByUser(@PathVariable Long userId) {
        List<OrderDetailVO> list = ordersService.listByUserId(userId);
        return Result.ok(list);
    }

    /**
     * 获取订单详情
     * GET /api/order/{orderId}
     */
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@PathVariable Long orderId) {
        OrderDetailVO vo = ordersService.getOrderDetail(orderId);
        return Result.ok(vo);
    }

    /**
     * 获取所有订单（管理员）
     * GET /api/order
     */
    @GetMapping
    public Result<List<OrderDetailVO>> listAll(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        List<OrderDetailVO> list = ordersService.listAll();
        return Result.ok(list);
    }

    /**
     * 删除订单（仅管理员）
     * DELETE /api/order/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public Result<String> delete(@PathVariable Long orderId,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        ordersService.removeById(orderId);
        return Result.ok("删除成功");
    }

    /**
     * 校验是否为管理员
     */
    private void checkAdmin(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户认证信息");
        }
        User user = userService.getById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new IllegalArgumentException("无权限，仅管理员可操作");
        }
    }
}
