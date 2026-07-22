package com.yunong.module.auth.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.auth.dto.*;
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.security.JwtTokenProvider;
import com.yunong.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        // 用 Spring Security 认证
        var authToken = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);

        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUserId());

        // 更新最后登录时间
        var user = userMapper.selectById(userDetails.getUserId());
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        var userInfo = new LoginResponse.UserInfo(
                userDetails.getUserId(),
                userDetails.getUsername(),
                user != null ? user.getRealName() : "",
                userDetails.getRole(),
                user != null ? user.getAvatarUrl() : null
        );

        return new LoginResponse(accessToken, refreshToken, "Bearer", 86400, userInfo);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 检查用户名是否存在
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        // 检查手机号
        if (StrUtil.isNotBlank(request.getPhone()) &&
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, request.getPhone())) > 0) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        var user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(StrUtil.isNotBlank(request.getRole()) ? request.getRole() : "ROLE_FARMER");
        user.setStatus(1);
        userMapper.insert(user);

        // 直接登录
        var authToken = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        var userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole(), null);

        return new LoginResponse(accessToken, refreshToken, "Bearer", 86400, userInfo);
    }

    public LoginResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        var claims = jwtTokenProvider.parseToken(request.getRefreshToken());
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String userId = claims.getSubject();
        var user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        var userDetails = new UserDetailsImpl();
        userDetails.setUserId(user.getId());
        userDetails.setUsername(user.getUsername());
        userDetails.setPassword(user.getPasswordHash());
        userDetails.setRole(user.getRole());
        userDetails.setEnabled(user.getStatus() == 1);

        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String accessToken = jwtTokenProvider.generateAccessToken(auth);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        var userInfo = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole(), user.getAvatarUrl());
        return new LoginResponse(accessToken, newRefreshToken, "Bearer", 86400, userInfo);
    }
}
