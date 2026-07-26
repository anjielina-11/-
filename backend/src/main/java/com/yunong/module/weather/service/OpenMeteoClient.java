package com.yunong.module.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenMeteoClient {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    private final RestClient restClient;

    public OpenMeteoClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public Optional<Location> resolveLocation(String query) {
        var uri = UriComponentsBuilder.fromUriString(GEOCODING_URL)
                .queryParam("name", query)
                .queryParam("count", 1)
                .queryParam("language", "zh")
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();
        var body = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        var results = body == null ? null : body.path("results");
        if (results == null || !results.isArray() || results.isEmpty()) return Optional.empty();
        var first = results.get(0);
        return Optional.of(new Location(
                first.path("latitude").asDouble(),
                first.path("longitude").asDouble(),
                first.path("name").asText(query)));
    }

    public List<ForecastDay> fetchForecast(double latitude, double longitude) {
        var uri = UriComponentsBuilder.fromUriString(FORECAST_URL)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("daily", "weather_code,temperature_2m_mean,precipitation_sum,wind_speed_10m_max")
                .queryParam("hourly", "relative_humidity_2m")
                .queryParam("timezone", "Asia/Shanghai")
                .queryParam("forecast_days", 7)
                .build()
                .encode()
                .toUri();
        var body = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        if (body == null) return List.of();

        var humidityByDate = averageHumidityByDate(body.path("hourly"));
        var daily = body.path("daily");
        var dates = daily.path("time");
        var records = new ArrayList<ForecastDay>();
        for (int index = 0; index < dates.size(); index++) {
            var date = LocalDate.parse(dates.get(index).asText());
            records.add(new ForecastDay(
                    date,
                    decimalAt(daily.path("temperature_2m_mean"), index),
                    humidityByDate.getOrDefault(date, BigDecimal.ZERO),
                    decimalAt(daily.path("precipitation_sum"), index),
                    decimalAt(daily.path("wind_speed_10m_max"), index),
                    describeWeather(intAt(daily.path("weather_code"), index))));
        }
        return records;
    }

    private Map<LocalDate, BigDecimal> averageHumidityByDate(JsonNode hourly) {
        var sums = new HashMap<LocalDate, BigDecimal>();
        var counts = new HashMap<LocalDate, Integer>();
        var times = hourly.path("time");
        var values = hourly.path("relative_humidity_2m");
        for (int index = 0; index < Math.min(times.size(), values.size()); index++) {
            if (values.get(index).isNull()) continue;
            var date = OffsetDateTime.parse(times.get(index).asText() + "+08:00").toLocalDate();
            sums.merge(date, values.get(index).decimalValue(), BigDecimal::add);
            counts.merge(date, 1, Integer::sum);
        }
        var averages = new HashMap<LocalDate, BigDecimal>();
        sums.forEach((date, sum) -> averages.put(date,
                sum.divide(BigDecimal.valueOf(counts.get(date)), 1, RoundingMode.HALF_UP)));
        return averages;
    }

    private BigDecimal decimalAt(JsonNode array, int index) {
        return index < array.size() && !array.get(index).isNull() ? array.get(index).decimalValue() : BigDecimal.ZERO;
    }

    private int intAt(JsonNode array, int index) {
        return index < array.size() && !array.get(index).isNull() ? array.get(index).asInt() : -1;
    }

    private String describeWeather(int code) {
        if (code == 0) return "\u6674";
        if (code >= 1 && code <= 3) return "\u591a\u4e91";
        if (code == 45 || code == 48) return "\u96fe";
        if (code >= 51 && code <= 67) return "\u96e8";
        if (code >= 71 && code <= 77) return "\u96ea";
        if (code >= 80 && code <= 82) return "\u9635\u96e8";
        if (code >= 95 && code <= 99) return "\u96f7\u66b4";
        return "\u672a\u77e5";
    }

    public record Location(double latitude, double longitude, String name) {}

    public record ForecastDay(LocalDate date, BigDecimal temperature, BigDecimal humidity,
                              BigDecimal rainfall, BigDecimal windSpeed, String weatherDesc) {}
}
