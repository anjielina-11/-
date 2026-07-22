package com.yunong.module.user.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.auth.entity.User;
import com.yunong.module.user.service.UserService;
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

    private final UserService service;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<User> me(@AuthenticationPrincipal UserDetails principal) {
        return R.ok(service.getMe(principal.getUsername()));
    }

    @PutMapping("/me")
    @AuditLog(action = "修改个人信息")
    @Operation(summary = "修改个人信息")
    public R<User> updateMe(@AuthenticationPrincipal UserDetails principal, @RequestBody User update) {
        return R.ok(service.updateMe(principal.getUsername(), update));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "用户列表(管理员)")
    public R<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        return R.ok(service.list(page, size, role, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "用户详情(管理员)")
    public R<User> getById(@PathVariable String id) {
        return R.ok(service.getById(id));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(action = "修改用户角色")
    @Operation(summary = "修改用户角色(管理员)")
    public R<Void> updateRole(@PathVariable String id, @RequestParam String role) {
        service.updateRole(id, role);
        return R.ok();
    }
}
