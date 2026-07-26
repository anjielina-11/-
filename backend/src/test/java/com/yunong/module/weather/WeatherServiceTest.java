package com.yunong.module.weather;

import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import com.yunong.module.weather.service.OpenMeteoClient;
import com.yunong.module.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock WeatherRecordMapper weatherMapper;
    @Mock FarmMapper farmMapper;
    @Mock OpenMeteoClient openMeteoClient;
    @InjectMocks WeatherService weatherService;

    @Test
    void refreshForecastPersistsSevenDistinctDaysFromToday() {
        var farm = new Farm();
        farm.setId("farm-1");
        farm.setName("验收农场");
        farm.setAddress("云南省昆明市呈贡区");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(openMeteoClient.resolveLocation("昆明市"))
                .thenReturn(Optional.of(new OpenMeteoClient.Location(24.88, 102.83, "昆明市")));
        var start = LocalDate.now();
        var days = java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> new OpenMeteoClient.ForecastDay(
                        start.plusDays(index),
                        BigDecimal.valueOf(20 + index),
                        BigDecimal.valueOf(60 + index),
                        BigDecimal.valueOf(index),
                        BigDecimal.valueOf(10 + index),
                        index == 0 ? "晴" : "多云"))
                .toList();
        when(openMeteoClient.fetchForecast(24.88, 102.83)).thenReturn(days);
        when(weatherMapper.selectList(any())).thenReturn(List.of());

        var result = weatherService.refreshForecast("farm-1");

        assertEquals(7, ((List<?>) result.get("records")).size());
        assertEquals("昆明市", result.get("locationName"));
        assertTrue(((List<WeatherRecord>) result.get("records")).stream()
                .map(record -> record.getRecordedAt().toLocalDate())
                .distinct().count() == 7);
        verify(weatherMapper, org.mockito.Mockito.times(7)).insert(any(WeatherRecord.class));
    }

    @Test
    void refreshForecastFallsBackToRomanizedCityForOpenMeteo() {
        var farm = new Farm();
        farm.setId("farm-3");
        farm.setAddress("\u4e91\u5357\u7701\u6606\u660e\u5e02\u5448\u8d21\u533a");
        when(farmMapper.selectById("farm-3")).thenReturn(farm);
        when(openMeteoClient.resolveLocation("\u6606\u660e\u5e02")).thenReturn(Optional.empty());
        when(openMeteoClient.resolveLocation("Kunming"))
                .thenReturn(Optional.of(new OpenMeteoClient.Location(25.04, 102.72, "Kunming")));
        when(openMeteoClient.fetchForecast(25.04, 102.72)).thenReturn(List.of());
        when(weatherMapper.selectList(any())).thenReturn(List.of());

        var result = weatherService.refreshForecast("farm-3");

        assertEquals("\u6606\u660e\u5e02", result.get("locationName"));
    }

    @Test
    void refreshForecastUsesYuxiWhenFarmHasNoAddress() {
        var farm = new Farm();
        farm.setId("farm-2");
        farm.setName("示范园");
        when(farmMapper.selectById("farm-2")).thenReturn(farm);
        when(openMeteoClient.resolveLocation("玉溪市"))
                .thenReturn(Optional.of(new OpenMeteoClient.Location(24.35, 102.54, "玉溪市")));
        when(openMeteoClient.fetchForecast(24.35, 102.54)).thenReturn(List.of());
        when(weatherMapper.selectList(any())).thenReturn(List.of());

        var result = weatherService.refreshForecast("farm-2");

        assertEquals("玉溪市", result.get("locationName"));
        assertEquals("玉溪市", result.get("locationQuery"));
    }
}
