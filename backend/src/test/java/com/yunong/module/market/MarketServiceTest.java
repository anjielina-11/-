package com.yunong.module.market;

import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.market.entity.MarketPrice;
import com.yunong.module.market.mapper.MarketPriceMapper;
import com.yunong.module.market.service.MarketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock MarketPriceMapper marketMapper;
    @Mock CropMapper cropMapper;
    @InjectMocks MarketService service;

    @Test
    void collectTodayPricesReplacesTodayDataForEveryCrop() {
        var rice = crop("crop-rice", "水稻", "粮食作物");
        var tomato = crop("crop-tomato", "番茄", "蔬菜");
        when(cropMapper.selectList(any())).thenReturn(List.of(rice, tomato));

        int count = service.collectTodayPrices();

        assertEquals(2, count);
        verify(marketMapper).delete(any());
        var captor = ArgumentCaptor.forClass(MarketPrice.class);
        verify(marketMapper, times(2)).insert(captor.capture());
        assertEquals(List.of("水稻", "番茄"), captor.getAllValues().stream().map(MarketPrice::getCropName).toList());
        captor.getAllValues().forEach(record -> assertEquals(LocalDate.now(), record.getRecordedAt()));
    }

    private Crop crop(String id, String name, String category) {
        var crop = new Crop();
        crop.setId(id);
        crop.setName(name);
        crop.setCategory(category);
        return crop;
    }
}
