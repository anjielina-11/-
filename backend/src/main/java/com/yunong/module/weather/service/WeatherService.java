package com.yunong.module.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final String DEFAULT_LOCATION = "\u7389\u6eaa\u5e02";
    private static final Pattern CITY_PATTERN = Pattern.compile("([^\u5e02]+\u5e02)");
    private static final Map<String, String> GEOCODING_ALIASES = Map.ofEntries(
            Map.entry("\u6606\u660e", "Kunming"),
            Map.entry("\u7389\u6eaa", "Yuxi"),
            Map.entry("\u66f2\u9756", "Qujing"),
            Map.entry("\u662d\u901a", "Zhaotong"),
            Map.entry("\u4fdd\u5c71", "Baoshan"),
            Map.entry("\u4e3d\u6c5f", "Lijiang"),
            Map.entry("\u666e\u6d31", "Puer"),
            Map.entry("\u4e34\u6ca7", "Lincang"),
            Map.entry("\u695a\u96c4", "Chuxiong"),
            Map.entry("\u7ea2\u6cb3", "Honghe"),
            Map.entry("\u6587\u5c71", "Wenshan"),
            Map.entry("\u897f\u53cc\u7248\u7eb3", "Jinghong"),
            Map.entry("\u5927\u7406", "Dali"),
            Map.entry("\u5fb7\u5b8f", "Mangshi"),
            Map.entry("\u6012\u6c5f", "Liuku"),
            Map.entry("\u8fea\u5e86", "Shangri-La"));

    private final WeatherRecordMapper weatherMapper;
    private final FarmMapper farmMapper;
    private final OpenMeteoClient openMeteoClient;

    public PageResult<WeatherRecord> list(int page, int size, String farmId, String startDate, String endDate) {
        var wrapper = new LambdaQueryWrapper<WeatherRecord>();
        if (farmId != null) wrapper.eq(WeatherRecord::getFarmId, farmId);
        if (startDate != null) wrapper.ge(WeatherRecord::getRecordedAt, LocalDateTime.parse(startDate));
        if (endDate != null) wrapper.le(WeatherRecord::getRecordedAt, LocalDateTime.parse(endDate));
        wrapper.orderByDesc(WeatherRecord::getRecordedAt);
        var result = weatherMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public Map<String, Object> trend(String farmId) {
        var records = weatherMapper.selectList(new LambdaQueryWrapper<WeatherRecord>()
                .eq(WeatherRecord::getFarmId, farmId)
                .ge(WeatherRecord::getRecordedAt, LocalDate.now().atStartOfDay())
                .lt(WeatherRecord::getRecordedAt, LocalDate.now().plusDays(7).atStartOfDay())
                .orderByAsc(WeatherRecord::getRecordedAt));
        var result = new LinkedHashMap<String, Object>();
        result.put("records", records);
        result.put("farmId", farmId);
        return result;
    }

    @Transactional
    public Map<String, Object> refreshForecast(String farmId) {
        var farm = farmMapper.selectById(farmId);
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);

        var locationQuery = extractCity(farm.getAddress());
        var location = openMeteoClient.resolveLocation(locationQuery);
        var fallbackQuery = geocodingAlias(locationQuery);
        if (location.isEmpty() && !fallbackQuery.equals(locationQuery)) {
            location = openMeteoClient.resolveLocation(fallbackQuery);
        }
        var resolvedLocation = location
                .orElseThrow(() -> new IllegalStateException("Unable to resolve weather location: " + locationQuery));
        var forecast = openMeteoClient.fetchForecast(resolvedLocation.latitude(), resolvedLocation.longitude());
        var start = LocalDate.now();
        var end = start.plusDays(7);
        var existingByDate = weatherMapper.selectList(new LambdaQueryWrapper<WeatherRecord>()
                        .eq(WeatherRecord::getFarmId, farmId)
                        .ge(WeatherRecord::getRecordedAt, start.atStartOfDay())
                        .lt(WeatherRecord::getRecordedAt, end.atStartOfDay()))
                .stream()
                .collect(Collectors.toMap(
                        record -> record.getRecordedAt().toLocalDate(),
                        Function.identity(),
                        (first, second) -> second));

        List<WeatherRecord> records = new ArrayList<>();
        forecast.stream()
                .filter(day -> !day.date().isBefore(start) && day.date().isBefore(end))
                .forEach(day -> {
                    var record = existingByDate.getOrDefault(day.date(), new WeatherRecord());
                    record.setFarmId(farmId);
                    record.setTemperature(day.temperature());
                    record.setHumidity(day.humidity());
                    record.setRainfall(day.rainfall());
                    record.setWindSpeed(day.windSpeed());
                    record.setWeatherDesc(day.weatherDesc());
                    record.setSource("Open-Meteo");
                    record.setRecordedAt(day.date().atTime(12, 0));
                    if (record.getId() == null) weatherMapper.insert(record);
                    else weatherMapper.updateById(record);
                    records.add(record);
                });

        var result = new LinkedHashMap<String, Object>();
        result.put("records", records);
        result.put("farmId", farmId);
        result.put("locationQuery", locationQuery);
        result.put("locationName", locationQuery);
        result.put("updatedAt", LocalDateTime.now());
        return result;
    }

    private String geocodingAlias(String locationQuery) {
        return GEOCODING_ALIASES.entrySet().stream()
                .filter(entry -> locationQuery.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(locationQuery);
    }

    private String extractCity(String address) {
        if (address == null || address.isBlank()) return DEFAULT_LOCATION;
        var withoutProvince = address.replaceFirst("^.*?\u7701", "");
        var matcher = CITY_PATTERN.matcher(withoutProvince);
        return matcher.find() ? matcher.group(1) : address.trim();
    }
}
