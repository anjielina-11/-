package com.yunong.module.system.service;

import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.market.entity.MarketPrice;
import com.yunong.module.market.mapper.MarketPriceMapper;
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
    @Mock MarketPriceMapper marketMapper;
    @Mock FarmMapper farmMapper;
    @Mock CropMapper cropMapper;
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
    void marketCollectionUsesExistingCrops() {
        var crop = new Crop();
        crop.setId("f4d4388a-d610-4b73-8793-dfcfa58a309b");
        crop.setName("水稻");
        crop.setCategory("粮食");
        when(cropMapper.selectList(any())).thenReturn(List.of(crop));

        service.fetchMarketPrices();

        var record = ArgumentCaptor.forClass(MarketPrice.class);
        verify(marketMapper).insert(record.capture());
        assertEquals(crop.getId(), record.getValue().getCropId());
        assertEquals(crop.getName(), record.getValue().getCropName());
    }

    @Test
    void collectionSkipsInsertWhenThereAreNoBusinessEntities() {
        when(farmMapper.selectList(any())).thenReturn(List.of());
        when(cropMapper.selectList(any())).thenReturn(List.of());

        service.fetchWeather();
        service.fetchMarketPrices();

        verify(weatherMapper, never()).insert(any(WeatherRecord.class));
        verify(marketMapper, never()).insert(any(MarketPrice.class));
    }
}
