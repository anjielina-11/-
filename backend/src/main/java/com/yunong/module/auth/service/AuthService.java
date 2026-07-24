package com.yunong.module.auth.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.auth.dto.LoginRequest;
import com.yunong.module.auth.dto.LoginResponse;
import com.yunong.module.auth.dto.RefreshRequest;
import com.yunong.module.auth.dto.RegisterRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();

        var user = userMapper.selectById(userDetails.getUserId());
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);
        } else {
            user = toUser(userDetails);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUserId());
        return response(accessToken, refreshToken, user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
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
        // 公开注册只允许创建农户，防止通过请求体提升权限。
        user.setRole("ROLE_FARMER");
        user.setStatus(1);
        userMapper.insert(user);

        var authToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        var authentication = authenticationManager.authenticate(authToken);
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return response(accessToken, refreshToken, user);
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
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        var userDetails = new UserDetailsImpl();
        userDetails.setUserId(user.getId());
        userDetails.setUsername(user.getUsername());
        userDetails.setPassword(user.getPasswordHash());
        userDetails.setRole(user.getRole());
        userDetails.setEnabled(user.getStatus() == 1);

        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String accessToken = jwtTokenProvider.generateAccessToken(auth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        return response(accessToken, refreshToken, user);
    }

    private LoginResponse response(String accessToken, String refreshToken, User user) {
        return new LoginResponse(accessToken, refreshToken, new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), normalizeRole(user.getRole()), user.getRealName()));
    }

    private User toUser(UserDetailsImpl details) {
        var user = new User();
        user.setId(details.getUserId());
        user.setUsername(details.getUsername());
        user.setRole(details.getRole());
        return user;
    }

    private String normalizeRole(String role) {
        if (role == null) return null;
        return switch (role.replace("ROLE_", "").toUpperCase()) {
            case "TECHNICIAN" -> "tech";
            case "COOP_MANAGER" -> "coop";
            default -> role.replace("ROLE_", "").toLowerCase();
        };
    }
}
