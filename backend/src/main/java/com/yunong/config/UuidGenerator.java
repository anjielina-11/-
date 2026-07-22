package com.yunong.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MyBatis-Plus 自定义 ID 生成器，生成 PostgreSQL UUID 兼容的 36 位字符串（带连字符）
 */
@Component
public class UuidGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return null; // 不使用数字 ID
    }

    @Override
    public String nextUUID(Object entity) {
        return UUID.randomUUID().toString(); // 生成 "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    }
}
