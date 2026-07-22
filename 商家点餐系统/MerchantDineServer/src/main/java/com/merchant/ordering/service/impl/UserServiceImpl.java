package com.merchant.ordering.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.merchant.ordering.dto.LoginRequest;
import com.merchant.ordering.dto.RegisterRequest;
import com.merchant.ordering.entity.User;
import com.merchant.ordering.mapper.UserMapper;
import com.merchant.ordering.service.UserService;
import com.merchant.ordering.util.Md5Utils;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(LoginRequest request) {
        // 校验参数
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 根据用户名查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 校验状态
        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }

        // MD5加密后比对密码
        String encryptedPassword = Md5Utils.md5(request.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        return user;
    }

    @Override
    public User register(RegisterRequest request) {
        // 校验参数
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 构建用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        // 密码MD5加密后存储
        user.setPassword(Md5Utils.md5(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setRole(0);       // 默认普通用户
        user.setStatus(1);     // 默认正常状态

        this.save(user);
        return user;
    }
}
