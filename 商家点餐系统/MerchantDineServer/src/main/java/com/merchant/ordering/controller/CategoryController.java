package com.merchant.ordering.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.merchant.ordering.common.Result;
import com.merchant.ordering.entity.Category;
import com.merchant.ordering.entity.User;
import com.merchant.ordering.service.CategoryService;
import com.merchant.ordering.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类 Controller — 前缀 /api/category
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有分类
     * GET /api/category
     */
    @GetMapping
    public Result<List<Category>> list() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return Result.ok(categoryService.list(wrapper));
    }

    /**
     * 创建分类（仅管理员）
     * POST /api/category
     */
    @PostMapping
    public Result<Category> save(@RequestBody Category category,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        categoryService.save(category);
        return Result.ok(category);
    }

    /**
     * 删除分类（仅管理员）
     * DELETE /api/category/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        categoryService.removeById(id);
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
