package com.yunong.module.diagnosis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DiagnosisContext(
        CropContext crop,
        FieldContext field,
        List<WeatherForecast> weatherForecast
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CropContext(
            String name,
            String variety,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate plantingDate,
            String growthStage
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FieldContext(
            String name,
            String farmName
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WeatherForecast(
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            String weather,
            BigDecimal temperature,
            BigDecimal humidity,
            BigDecimal rainfall,
            BigDecimal windSpeed
    ) {}
}