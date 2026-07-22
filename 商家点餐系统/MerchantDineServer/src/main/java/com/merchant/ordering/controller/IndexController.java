package com.merchant.ordering.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首页 - API 导航
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> api = new LinkedHashMap<>();
        api.put("project", "商家点餐系统");
        api.put("version", "1.0.0");

        Map<String, String> apis = new LinkedHashMap<>();
        apis.put("GET  /user/page?current=1&size=10", "用户分页查询");
        apis.put("GET  /user/{id}", "用户详情");
        apis.put("POST /user", "新增用户");
        apis.put("PUT  /user/{id}", "更新用户");
        apis.put("DELETE /user/{id}", "删除用户");
        apis.put("GET  /category/list?type=1", "分类列表");
        apis.put("POST /category", "新增分类");
        apis.put("PUT  /category/{id}", "更新分类");
        apis.put("DELETE /category/{id}", "删除分类");
        apis.put("GET  /product/page?current=1&size=10&categoryId=1", "商品分页查询");
        apis.put("POST /product", "新增商品");
        apis.put("PUT  /product/{id}", "更新商品");
        apis.put("DELETE /product/{id}", "删除商品");
        apis.put("GET  /orders/page?current=1&size=10&status=1", "订单分页查询");
        apis.put("POST /orders", "新增订单");
        apis.put("PUT  /orders/{id}", "更新订单");
        apis.put("DELETE /orders/{id}", "删除订单");
        apis.put("GET  /order-item/list/{orderId}", "订单明细列表");
        apis.put("POST /order-item", "新增订单明细");
        apis.put("DELETE /order-item/{id}", "删除订单明细");

        api.put("apis", apis);
        return api;
    }
}
