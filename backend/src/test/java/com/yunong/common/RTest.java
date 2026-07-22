package com.yunong.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("统一响应测试")
class RTest {

    @Test
    @DisplayName("成功响应带数据")
    void okWithData() {
        var result = R.ok("hello");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("hello");
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    @DisplayName("成功响应无数据")
    void okWithoutData() {
        var result = R.ok();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("失败响应")
    void fail() {
        var result = R.fail(500, "服务器错误");
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("服务器错误");
        assertThat(result.getData()).isNull();
    }
}
