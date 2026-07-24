package com.yunong.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @Size(min = 6, max = 100) String password,
        @NotBlank String realName,
        String phone,
        @Email String email,
        @NotBlank String role,
        Integer status
) {}
