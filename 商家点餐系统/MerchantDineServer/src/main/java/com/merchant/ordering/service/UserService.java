package com.merchant.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.merchant.ordering.dto.LoginRequest;
import com.merchant.ordering.dto.RegisterRequest;
import com.merchant.ordering.entity.User;

/**
 * 用户 Service 接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     * @param request 登录请求（用户名 + 明文密码）
     * @return 登录成功返回用户实体，失败抛异常
     */
    User login(LoginRequest request);

    /**
     * 用户注册
     * @param request 注册请求（密码明文，后端MD5加密存储）
     * @return 注册成功的用户实体
     */
    User register(RegisterRequest request);
}
