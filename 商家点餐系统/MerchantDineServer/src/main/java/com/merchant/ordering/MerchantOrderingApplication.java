package com.merchant.ordering;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商家点餐系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.merchant.ordering.mapper")
public class MerchantOrderingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantOrderingApplication.class, args);
    }
}
