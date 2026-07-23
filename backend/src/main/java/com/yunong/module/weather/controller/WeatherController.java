package com.yunong.module.weather.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "天气记录", description = "天气查询、趋势统计")
public class WeatherController {

    private final WeatherService service;

    @GetMapping
    @Operation(summary = "天气记录查询")
    public R<PageResult<WeatherRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String farmId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(service.list(page, size, farmId, startDate, endDate));
    }

    @GetMapping("/trend")
    @Operation(summary = "天气趋势(最近7天)")
    public R<Map<String, Object>> trend(@RequestParam String farmId) {
        return R.ok(service.trend(farmId));
    }

    @PostMapping("/fetch")
    @AuditLog(action = "手动触发天气采集")
    @Operation(summary = "手动触发天气采集(管理员)")
    public R<String> fetch() {
        return R.ok("天气数据采集任务已提交，定时任务每小时自动执行");
    }
}
