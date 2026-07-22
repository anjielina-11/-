package com.yunong.module.system.service;

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
import java.util.UUID;

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

    /** 每小时采集天气数据 */
    @Scheduled(cron = "0 0 * * * ?")
    public void fetchWeather() {
        log.info("定时任务: 采集天气数据");
        try {
            // TODO: 对接真实天气 API
            var wr = new WeatherRecord();
            wr.setFarmId("mock-farm-001");
            wr.setTemperature(new BigDecimal("25.5"));
            wr.setHumidity(new BigDecimal("68.0"));
            wr.setRainfall(new BigDecimal("0.0"));
            wr.setWindSpeed(new BigDecimal("2.3"));
            wr.setWindDir("SW");
            wr.setPressure(new BigDecimal("1013.2"));
            wr.setWeatherDesc("多云");
            wr.setSource("scheduled");
            wr.setRecordedAt(LocalDateTime.now());
            weatherMapper.insert(wr);
            log.debug("天气数据采集完成: temp={}C, humidity={}%", wr.getTemperature(), wr.getHumidity());
        } catch (Exception e) {
            log.error("天气数据采集失败", e);
        }
    }

    /** 每天 8:00 采集市场价格 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void fetchMarketPrices() {
        log.info("定时任务: 采集市场价格");
        try {
            // TODO: 对接真实市场价格 API
            String[] cropIds = {"crop-001", "crop-002"};
            String[] cropNames = {"水稻", "玉米"};
            for (int i = 0; i < cropIds.length; i++) {
                var mp = new MarketPrice();
                mp.setCropId(cropIds[i]);
                mp.setCropName(cropNames[i]);
                mp.setPrice(new BigDecimal(String.valueOf(2.5 + Math.random())));
                mp.setUnit("元/公斤");
                mp.setMarketName("昆明市呈贡批发市场");
                mp.setCategory("粮食");
                mp.setSource("scheduled");
                mp.setRecordedAt(LocalDate.now());
                marketMapper.insert(mp);
            }
            log.debug("市场价格采集完成: {} 条", cropIds.length);
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
