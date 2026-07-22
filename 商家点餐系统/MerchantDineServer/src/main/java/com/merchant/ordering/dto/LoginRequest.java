package com.merchant.ordering.dto;

import lombok.Data;

/**
 * 用户登录请求参数
 */
@Data
public class LoginRequest {

    /** 用户名 */
    private String username;

    /** 密码（明文，后端MD5加密后比对） */
    private String password;
}
