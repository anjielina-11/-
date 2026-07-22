package com.yunong;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class YunnongApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 容器启动正常
    }
}
