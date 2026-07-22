package com.yunong.module.weather.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "天气记录", description = "天气查询、趋势统计")
public class WeatherController {

    private final WeatherRecordMapper wrMapper;

    @GetMapping
    @Operation(summary = "天气记录查询")
    public R<PageResult<WeatherRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String farmId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        var wrapper = new LambdaQueryWrapper<WeatherRecord>();
        if (farmId != null) wrapper.eq(WeatherRecord::getFarmId, farmId);
        if (startDate != null) wrapper.ge(WeatherRecord::getRecordedAt, LocalDateTime.parse(startDate));
        if (endDate != null) wrapper.le(WeatherRecord::getRecordedAt, LocalDateTime.parse(endDate));
        wrapper.orderByDesc(WeatherRecord::getRecordedAt);
        var result = wrMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/trend")
    @Operation(summary = "天气趋势(最近7天)")
    public R<Map<String, Object>> trend(@RequestParam String farmId) {
        var records = wrMapper.selectList(new LambdaQueryWrapper<WeatherRecord>()
                .eq(WeatherRecord::getFarmId, farmId)
                .ge(WeatherRecord::getRecordedAt, LocalDateTime.now().minusDays(7))
                .orderByAsc(WeatherRecord::getRecordedAt));
        var result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("farmId", farmId);
        return R.ok(result);
    }

    @PostMapping("/fetch")
    @Operation(summary = "手动触发天气采集(管理员)")
    public R<String> fetch() {
        // 实际采集由定时任务或 AI 服务处理，这里返回提示
        return R.ok("天气数据采集任务已提交");
    }
}
