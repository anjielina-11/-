package com.merchant.ordering.dto;

import lombok.Data;

/**
 * 用户注册请求参数
 */
@Data
public class RegisterRequest {

    /** 用户名 */
    private String username;

    /** 密码（明文，后端MD5加密后存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;
}
