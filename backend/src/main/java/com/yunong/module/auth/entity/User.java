package com.yunong.module.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    @TableId
    private String id;

    private String username;

    private String passwordHash;

    private String role;

    private String realName;

    private String phone;

    private String email;

    private String avatarUrl;

    private String farmId;

    private Integer status;

    private LocalDateTime lastLoginAt;
}
