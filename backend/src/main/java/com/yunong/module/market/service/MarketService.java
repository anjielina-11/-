package com.yunong.module.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.module.market.entity.MarketPrice;
import com.yunong.module.market.mapper.MarketPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketPriceMapper mapper;

    public PageResult<MarketPrice> list(int page, int size, String cropId, String marketName) {
        var wrapper = new LambdaQueryWrapper<MarketPrice>();
        if (cropId != null) wrapper.eq(MarketPrice::getCropId, cropId);
        if (marketName != null) wrapper.eq(MarketPrice::getMarketName, marketName);
        wrapper.orderByDesc(MarketPrice::getRecordedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public Map<String, Object> trend(String cropId) {
        var records = mapper.selectList(new LambdaQueryWrapper<MarketPrice>()
                .eq(MarketPrice::getCropId, cropId)
                .ge(MarketPrice::getRecordedAt, LocalDate.now().minusDays(30))
                .orderByAsc(MarketPrice::getRecordedAt));
        var result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("cropId", cropId);
        return result;
    }
}
