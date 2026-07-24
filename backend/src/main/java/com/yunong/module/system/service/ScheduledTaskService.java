package com.yunong.module.system.service;

import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.market.entity.MarketPrice;
import com.yunong.module.market.mapper.MarketPriceMapper;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定时数据采集服务 —— 天气 & 市场价格
 * 当前使用模拟数据，后续对接真实 API 只需替换模拟逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final WeatherRecordMapper weatherMapper;
    private final MarketPriceMapper marketMapper;
    private final FarmMapper farmMapper;
    private final CropMapper cropMapper;

    /** 每小时采集天气数据 */
    @Scheduled(cron = "0 0 * * * ?")
    public void fetchWeather() {
        log.info("定时任务: 采集天气数据");
        try {
            var farms = farmMapper.selectList(null);
            for (var farm : farms) {
                // TODO: 对接真实天气 API 后按农场坐标获取数据。
                var record = new WeatherRecord();
                record.setFarmId(farm.getId());
                record.setTemperature(new BigDecimal("25.5"));
                record.setHumidity(new BigDecimal("68.0"));
                record.setRainfall(new BigDecimal("0.0"));
                record.setWindSpeed(new BigDecimal("2.3"));
                record.setWindDir("SW");
                record.setPressure(new BigDecimal("1013.2"));
                record.setWeatherDesc("多云");
                record.setSource("scheduled");
                record.setRecordedAt(LocalDateTime.now());
                weatherMapper.insert(record);
            }
            log.debug("天气数据采集完成: {} 个农场", farms.size());
        } catch (Exception e) {
            log.error("天气数据采集失败", e);
        }
    }

    /** 每天 8:00 采集市场价格 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void fetchMarketPrices() {
        log.info("定时任务: 采集市场价格");
        try {
            var crops = cropMapper.selectList(null);
            for (var crop : crops) {
                // TODO: 对接真实市场价格 API 后按作物和市场获取数据。
                var record = new MarketPrice();
                record.setCropId(crop.getId());
                record.setCropName(crop.getName());
                record.setPrice(BigDecimal.valueOf(2.5 + Math.random()));
                record.setUnit("元/公斤");
                record.setMarketName("昆明市呈贡批发市场");
                record.setCategory(crop.getCategory());
                record.setSource("scheduled");
                record.setRecordedAt(LocalDate.now());
                marketMapper.insert(record);
            }
            log.debug("市场价格采集完成: {} 条", crops.size());
        } catch (Exception e) {
            log.error("市场价格采集失败", e);
        }
    }

    /** 每 30 分钟清理过期缓存（占位，后续对接 Redis） */
    @Scheduled(cron = "0 */30 * * * ?")
    public void cleanExpiredCache() {
        log.debug("定时任务: 缓存清理检查");
    }
}
