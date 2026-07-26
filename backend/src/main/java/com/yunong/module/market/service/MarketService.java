package com.yunong.module.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.market.entity.MarketPrice;
import com.yunong.module.market.mapper.MarketPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MarketService {

    private static final String DEFAULT_MARKET = "昆明市呈贡批发市场";

    private final MarketPriceMapper mapper;
    private final CropMapper cropMapper;

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

    /** 为全部基础作物生成当天演示价格；重复刷新时替换当天记录，避免数据堆叠。 */
    @Transactional
    public int collectTodayPrices() {
        var crops = cropMapper.selectList(null);
        if (crops.isEmpty()) {
            return 0;
        }

        var today = LocalDate.now();
        mapper.delete(new LambdaQueryWrapper<MarketPrice>()
                .eq(MarketPrice::getRecordedAt, today)
                .eq(MarketPrice::getMarketName, DEFAULT_MARKET));

        for (var crop : crops) {
            var record = new MarketPrice();
            record.setCropId(crop.getId());
            record.setCropName(crop.getName());
            record.setPrice(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(2.5, 8.0))
                    .setScale(2, RoundingMode.HALF_UP));
            record.setUnit("元/公斤");
            record.setMarketName(DEFAULT_MARKET);
            record.setCategory(crop.getCategory());
            record.setSource("demo-collector");
            record.setRecordedAt(today);
            mapper.insert(record);
        }
        return crops.size();
    }
}
