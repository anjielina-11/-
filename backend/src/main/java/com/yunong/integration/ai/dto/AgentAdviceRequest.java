package com.yunong.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yunong.module.diagnosis.dto.DiagnosisContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AgentAdviceRequest(
        @JsonProperty("disease_name") String diseaseName,
        BigDecimal confidence,
        DiagnosisContext.CropContext crop,
        DiagnosisContext.FieldContext field,
        @JsonProperty("weather_forecast") List<DiagnosisContext.WeatherForecast> weatherForecast,
        List<Map<String, Object>> citations
) {}