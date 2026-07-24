package com.yunong.module.user.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.auth.entity.User;
import com.yunong.module.user.service.UserService;
import com.yunong.module.user.dto.AdminUserRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.yunong.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户CRUD和角色管理")
public class UserController {

    private final UserService service;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<User> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.getMe(principal.getUserId()));
    }

    @PutMapping("/me")
    @AuditLog(action = "修改个人信息")
    @Operation(summary = "修改个人信息")
    public R<User> updateMe(@AuthenticationPrincipal UserDetailsImpl principal, @RequestBody User update) {
        return R.ok(service.updateMe(principal.getUserId(), update));
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(action = "创建用户")
    @Operation(summary = "创建用户(管理员)")
    public R<User> create(@Valid @RequestBody AdminUserRequest request) {
        return R.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(action = "更新用户")
    @Operation(summary = "更新用户(管理员)")
    public R<User> update(@PathVariable String id, @Valid @RequestBody AdminUserRequest request,
                          @AuthenticationPrincipal UserDetailsImpl principal) {
        return R.ok(service.update(id, request, principal.getUserId()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(action = "修改用户角色")
    @Operation(summary = "修改用户角色(管理员)")
    public R<Void> updateRole(@PathVariable String id, @RequestBody RoleUpdateRequest request,
                              @AuthenticationPrincipal UserDetailsImpl principal) {
        service.updateRole(id, request.role(), principal.getUserId());
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(action = "修改用户状态")
    @Operation(summary = "修改用户状态(管理员)")
    public R<Void> updateStatus(@PathVariable String id, @RequestBody StatusUpdateRequest request,
                                @AuthenticationPrincipal UserDetailsImpl principal) {
        service.updateStatus(id, request.status(), principal.getUserId());
        return R.ok();
    }

    public record RoleUpdateRequest(String role) {}
    public record StatusUpdateRequest(int status) {}
}
