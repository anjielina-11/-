package com.yunong.module.weather.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("weather_records")
public class WeatherRecord {

    @TableId
    private String id;
    private String farmId;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal rainfall;
    private BigDecimal windSpeed;
    private String windDir;
    private BigDecimal pressure;
    private String weatherDesc;
    private String source;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
}
