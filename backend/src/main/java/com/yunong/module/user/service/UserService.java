package com.yunong.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper mapper;

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

    public void updateRole(String id, String role) {
        var user = mapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        user.setRole(role);
        mapper.updateById(user);
    }
}
