package com.yunong.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.module.user.dto.AdminUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public User getMe(String userId) {
        var user = mapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setPasswordHash(null);
        return user;
    }

    public User updateMe(String userId, User update) {
        var user = mapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (update.getRealName() != null) user.setRealName(update.getRealName());
        if (update.getPhone() != null) user.setPhone(update.getPhone());
        if (update.getEmail() != null) user.setEmail(update.getEmail());
        mapper.updateById(user);
        user.setPasswordHash(null);
        return user;
    }

    public PageResult<User> list(int page, int size, String role, String keyword) {
        var wrapper = new LambdaQueryWrapper<User>();
        if (role != null) wrapper.eq(User::getRole, role);
        if (keyword != null) wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        wrapper.orderByDesc(User::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPasswordHash(null));
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public User getById(String id) {
        var user = mapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setPasswordHash(null);
        return user;
    }

    public User create(AdminUserRequest request) {
        if (mapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username())) > 0)
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        if (request.phone() != null && mapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone())) > 0)
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        var user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRole(normalizeRole(request.role()));
        user.setStatus(request.status() == null ? 1 : request.status());
        validateStatus(user.getStatus());
        mapper.insert(user);
        return user;
    }

    public User update(String id, AdminUserRequest request, String currentUserId) {
        var user = mapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setRealName(request.realName());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        if (request.role() != null && !request.role().equalsIgnoreCase(user.getRole())) {
            if (id.equals(currentUserId)) throw new BusinessException(ErrorCode.ROLE_CANNOT_CHANGE_OWN);
            user.setRole(normalizeRole(request.role()));
        }
        if (request.status() != null) {
            if (id.equals(currentUserId) && request.status() == 0)
                throw new BusinessException(ErrorCode.ROLE_CANNOT_CHANGE_OWN);
            validateStatus(request.status());
            user.setStatus(request.status());
        }
        mapper.updateById(user);
        user.setPasswordHash(null);
        return user;
    }

    public void updateRole(String id, String role, String currentUserId) {
        if (id.equals(currentUserId)) throw new BusinessException(ErrorCode.ROLE_CANNOT_CHANGE_OWN);
        var user = mapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setRole(normalizeRole(role));
        mapper.updateById(user);
    }

    public void updateStatus(String id, int status, String currentUserId) {
        if (id.equals(currentUserId)) throw new BusinessException(ErrorCode.ROLE_CANNOT_CHANGE_OWN);
        validateStatus(status);
        var user = mapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setStatus(status);
        mapper.updateById(user);
    }

    private void validateStatus(int status) {
        if (status != 0 && status != 1)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户状态只能为 0/1");
    }

    private String normalizeRole(String role) {
        if (role == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "用户角色不能为空");
        return switch (role.toUpperCase()) {
            case "FARMER", "ROLE_FARMER" -> "ROLE_FARMER";
            case "TECH", "TECHNICIAN", "ROLE_TECHNICIAN" -> "ROLE_TECHNICIAN";
            case "COOP", "COOP_MANAGER", "ROLE_COOP_MANAGER" -> "ROLE_COOP_MANAGER";
            case "ADMIN", "ROLE_ADMIN" -> "ROLE_ADMIN";
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的用户角色: " + role);
        };
    }
}
