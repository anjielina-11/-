package com.yunong.module.weather.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherRecordMapper mapper;

    public PageResult<WeatherRecord> list(int page, int size, String farmId, String startDate, String endDate) {
        var wrapper = new LambdaQueryWrapper<WeatherRecord>();
        if (farmId != null) wrapper.eq(WeatherRecord::getFarmId, farmId);
        if (startDate != null) wrapper.ge(WeatherRecord::getRecordedAt, LocalDateTime.parse(startDate));
        if (endDate != null) wrapper.le(WeatherRecord::getRecordedAt, LocalDateTime.parse(endDate));
        wrapper.orderByDesc(WeatherRecord::getRecordedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public Map<String, Object> trend(String farmId) {
        var records = mapper.selectList(new LambdaQueryWrapper<WeatherRecord>()
                .eq(WeatherRecord::getFarmId, farmId)
                .ge(WeatherRecord::getRecordedAt, LocalDateTime.now().minusDays(7))
                .orderByAsc(WeatherRecord::getRecordedAt));
        var result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("farmId", farmId);
        return result;
    }
}
