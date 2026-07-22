package com.merchant.ordering.controller;

import com.merchant.ordering.common.Result;
import com.merchant.ordering.dto.ProductVO;
import com.merchant.ordering.entity.Product;
import com.merchant.ordering.entity.User;
import com.merchant.ordering.service.ProductService;
import com.merchant.ordering.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品 Controller — 前缀 /api/product
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有商品（无需登录）
     * GET /api/product
     */
    @GetMapping
    public Result<List<ProductVO>> list() {
        List<ProductVO> list = productService.listWithCategoryName();
        return Result.ok(list);
    }

    /**
     * 创建商品（仅管理员）
     * POST /api/product
     */
    @PostMapping
    public Result<Product> save(@RequestBody Product product,
                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        productService.save(product);
        return Result.ok(product);
    }

    /**
     * 更新商品（仅管理员）
     * PUT /api/product/{id}
     */
    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable Long id,
                                  @RequestBody Product product,
                                  @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        product.setId(id);
        productService.updateById(product);
        Product updated = productService.getById(id);
        return Result.ok(updated);
    }

    /**
     * 删除商品（仅管理员）
     * DELETE /api/product/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        productService.removeById(id);
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
        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }
    }
}
