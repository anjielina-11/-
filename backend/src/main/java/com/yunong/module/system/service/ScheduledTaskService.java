package com.yunong.module.system.service;

import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.market.service.MarketService;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定时数据采集服务 —— 天气 & 市场价格
 * 定时采集使用稳定的课程演示数据；实时天气预报由 WeatherService 调用 Open-Meteo 获取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final WeatherRecordMapper weatherMapper;
    private final MarketService marketService;
    private final FarmMapper farmMapper;

    /** 每小时采集天气数据 */
    @Scheduled(cron = "0 0 * * * ?")
    public void fetchWeather() {
        log.info("定时任务: 采集天气数据");
        try {
            var farms = farmMapper.selectList(null);
            for (var farm : farms) {
                // 定时历史记录使用稳定基准值，避免第三方接口波动影响课程演示。
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
            int count = marketService.collectTodayPrices();
            log.debug("市场价格采集完成: {} 条", count);
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
