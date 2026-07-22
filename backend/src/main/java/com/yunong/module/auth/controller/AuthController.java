package com.yunong.module.auth.controller;

import com.yunong.common.R;
import com.yunong.module.auth.dto.*;
import com.yunong.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录、注册、Token管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public R<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public R<Void> logout() {
        // 无状态 JWT，客户端删除 token 即可
        return R.ok();
    }
}
