package com.yunong.config;

import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusConfigTest {

    @Test
    void limitsPageSizeToProtectDatabaseFromOversizedQueries() {
        var interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        assertThat(interceptor.getInterceptors()).hasSize(1);
        assertThat(interceptor.getInterceptors().getFirst())
                .isInstanceOf(PaginationInnerInterceptor.class);

        var pagination = (PaginationInnerInterceptor) interceptor.getInterceptors().getFirst();
        assertThat(pagination.getMaxLimit()).isEqualTo(100L);
    }
}
