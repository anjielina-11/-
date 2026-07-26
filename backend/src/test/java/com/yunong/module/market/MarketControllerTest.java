package com.yunong.module.market;

import com.yunong.module.market.controller.MarketController;
import com.yunong.module.market.service.MarketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    @Mock MarketService service;
    @InjectMocks MarketController controller;

    @Test
    void manualFetchActuallyCollectsPrices() {
        when(service.collectTodayPrices()).thenReturn(9);

        var response = controller.fetch();

        verify(service).collectTodayPrices();
        assertEquals("已更新 9 种农产品的市场价格", response.getData());
    }
}
