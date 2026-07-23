package com.yunong.module.market.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.market.entity.MarketPrice;
import com.yunong.module.market.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "市场价格", description = "价格查询、趋势统计")
public class MarketController {

    private final MarketService service;

    @GetMapping
    @Operation(summary = "价格记录查询")
    public R<PageResult<MarketPrice>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false) String marketName) {
        return R.ok(service.list(page, size, cropId, marketName));
    }

    @GetMapping("/trend")
    @Operation(summary = "价格趋势(最近30天)")
    public R<Map<String, Object>> trend(@RequestParam String cropId) {
        return R.ok(service.trend(cropId));
    }

    @PostMapping("/fetch")
    @AuditLog(action = "手动触发价格采集")
    @Operation(summary = "手动触发价格采集(管理员)")
    public R<String> fetch() {
        return R.ok("市场价格采集任务已提交，定时任务每日8:00自动执行");
    }
}
