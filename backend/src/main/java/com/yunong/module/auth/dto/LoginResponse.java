package com.yunong.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UserInfo user;

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String username;
        private String role;
        private String name;
    }
}
