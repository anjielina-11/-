package com.yunong.module.system.service;

import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.market.service.MarketService;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskServiceTest {

    @Mock WeatherRecordMapper weatherMapper;
    @Mock MarketService marketService;
    @Mock FarmMapper farmMapper;
    @InjectMocks ScheduledTaskService service;

    @Test
    void weatherCollectionUsesExistingFarmIds() {
        var farm = new Farm();
        farm.setId("65ec2afb-3157-48fa-b1ac-ddaf26177a10");
        when(farmMapper.selectList(any())).thenReturn(List.of(farm));

        service.fetchWeather();

        var record = ArgumentCaptor.forClass(WeatherRecord.class);
        verify(weatherMapper).insert(record.capture());
        assertEquals(farm.getId(), record.getValue().getFarmId());
    }

    @Test
    void marketCollectionDelegatesToMarketService() {
        when(marketService.collectTodayPrices()).thenReturn(9);

        service.fetchMarketPrices();

        verify(marketService).collectTodayPrices();
    }

    @Test
    void weatherCollectionSkipsInsertWhenThereAreNoFarms() {
        when(farmMapper.selectList(any())).thenReturn(List.of());

        service.fetchWeather();

        verify(weatherMapper, never()).insert(any(WeatherRecord.class));
    }
}
