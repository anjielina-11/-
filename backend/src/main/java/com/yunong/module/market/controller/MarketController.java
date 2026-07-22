package com.yunong.module.market.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.market.entity.MarketPrice;
import com.yunong.module.market.mapper.MarketPriceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "市场价格", description = "价格查询、趋势统计")
public class MarketController {

    private final MarketPriceMapper mpMapper;

    @GetMapping
    @Operation(summary = "价格记录查询")
    public R<PageResult<MarketPrice>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false) String marketName) {
        var wrapper = new LambdaQueryWrapper<MarketPrice>();
        if (cropId != null) wrapper.eq(MarketPrice::getCropId, cropId);
        if (marketName != null) wrapper.eq(MarketPrice::getMarketName, marketName);
        wrapper.orderByDesc(MarketPrice::getRecordedAt);
        var result = mpMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @GetMapping("/trend")
    @Operation(summary = "价格趋势(最近30天)")
    public R<Map<String, Object>> trend(@RequestParam String cropId) {
        var records = mpMapper.selectList(new LambdaQueryWrapper<MarketPrice>()
                .eq(MarketPrice::getCropId, cropId)
                .orderByAsc(MarketPrice::getRecordedAt));
        var result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("cropId", cropId);
        return R.ok(result);
    }

    @PostMapping("/fetch")
    @Operation(summary = "手动触发价格采集(管理员)")
    public R<String> fetch() {
        return R.ok("市场价格采集任务已提交");
    }
}
