package com.yunong.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户CRUD和角色管理")
public class UserController {

    private final UserMapper userMapper;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<User> me(@AuthenticationPrincipal UserDetails principal) {
        var user = userMapper.selectById(principal.getUsername()); // username is userId in JWT
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setPasswordHash(null); // 不返回密码
        return R.ok(user);
    }

    @PutMapping("/me")
    @Operation(summary = "修改个人信息")
    public R<User> updateMe(@AuthenticationPrincipal UserDetails principal, @RequestBody User update) {
        var user = userMapper.selectById(principal.getUsername());
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (update.getRealName() != null) user.setRealName(update.getRealName());
        if (update.getPhone() != null) user.setPhone(update.getPhone());
        if (update.getEmail() != null) user.setEmail(update.getEmail());
        userMapper.updateById(user);
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "用户列表(管理员)")
    public R<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        var wrapper = new LambdaQueryWrapper<User>();
        if (role != null) wrapper.eq(User::getRole, role);
        if (keyword != null) wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        wrapper.orderByDesc(User::getCreatedAt);
        var result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return R.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "用户详情(管理员)")
    public R<User> getById(@PathVariable String id) {
        var user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setPasswordHash(null);
        return R.ok(user);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "修改用户角色(管理员)")
    public R<Void> updateRole(@PathVariable String id, @RequestParam String role) {
        var user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setRole(role);
        userMapper.updateById(user);
        return R.ok();
    }
}
