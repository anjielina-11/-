package com.yunong.module.diagnosis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunong.exception.BusinessException;
import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.diagnosis.entity.DiagnosisRecord;
import com.yunong.module.diagnosis.entity.Observation;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import com.yunong.module.diagnosis.service.DiagnosisContextService;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisContextServiceTest {

    @Mock private DiagnosisRecordMapper diagnosisMapper;
    @Mock private ObservationMapper observationMapper;
    @Mock private PlantingCycleMapper cycleMapper;
    @Mock private CropMapper cropMapper;
    @Mock private FieldMapper fieldMapper;
    @Mock private FarmMapper farmMapper;
    @Mock private WeatherRecordMapper weatherMapper;

    private DiagnosisContextService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-26T00:00:00Z"), ZoneOffset.UTC);
        service = new DiagnosisContextService(diagnosisMapper, observationMapper, cycleMapper,
                cropMapper, fieldMapper, farmMapper, weatherMapper, clock);
    }

    @Test
    void loadsCropStageFarmAndSevenDayWeather() {
        var diagnosis = new DiagnosisRecord();
        diagnosis.setId("diag-1");
        diagnosis.setObservationId("obs-1");
        when(diagnosisMapper.selectById("diag-1")).thenReturn(diagnosis);

        var observation = new Observation();
        observation.setId("obs-1");
        observation.setCycleId("cycle-1");
        when(observationMapper.selectById("obs-1")).thenReturn(observation);

        var cycle = new PlantingCycle();
        cycle.setId("cycle-1");
        cycle.setCropId("crop-1");
        cycle.setFieldId("field-1");
        cycle.setPlantingDate(LocalDate.of(2026, 7, 1));
        cycle.setGrowthStage("tillering");
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);

        var crop = new Crop();
        crop.setId("crop-1");
        crop.setName("水稻");
        crop.setVariety("滇粳验收");
        when(cropMapper.selectById("crop-1")).thenReturn(crop);

        var field = new Field();
        field.setId("field-1");
        field.setFarmId("farm-1");
        field.setName("A-01");
        when(fieldMapper.selectById("field-1")).thenReturn(field);

        var farm = new Farm();
        farm.setId("farm-1");
        farm.setName("验收农场");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);

        var weather = new WeatherRecord();
        weather.setRecordedAt(LocalDateTime.of(2026, 7, 26, 8, 0));
        weather.setWeatherDesc("阵雨");
        weather.setTemperature(new BigDecimal("28"));
        weather.setHumidity(new BigDecimal("86"));
        weather.setRainfall(new BigDecimal("8.5"));
        when(weatherMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(weather));

        var context = service.load("diag-1");

        assertThat(context.crop().name()).isEqualTo("水稻");
        assertThat(context.crop().variety()).isEqualTo("滇粳验收");
        assertThat(context.crop().growthStage()).isEqualTo("tillering");
        assertThat(context.field().farmName()).isEqualTo("验收农场");
        assertThat(context.weatherForecast()).hasSize(1);
        assertThat(context.weatherForecast().getFirst().date()).isEqualTo(LocalDate.of(2026, 7, 26));
    }

    @Test
    void returnsEmptyForecastWhenWeatherIsUnavailable() {
        stubMinimumContext();

        assertThat(service.load("diag-1").weatherForecast()).isEmpty();
    }

    @Test
    void failsWhenPlantingCycleIsMissing() {
        var diagnosis = new DiagnosisRecord();
        diagnosis.setObservationId("obs-1");
        when(diagnosisMapper.selectById("diag-1")).thenReturn(diagnosis);
        var observation = new Observation();
        observation.setCycleId("missing-cycle");
        when(observationMapper.selectById("obs-1")).thenReturn(observation);
        when(cycleMapper.selectById("missing-cycle")).thenReturn(null);

        assertThatThrownBy(() -> service.load("diag-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("种植周期");
    }

    private void stubMinimumContext() {
        var diagnosis = new DiagnosisRecord();
        diagnosis.setObservationId("obs-1");
        when(diagnosisMapper.selectById("diag-1")).thenReturn(diagnosis);
        var observation = new Observation();
        observation.setCycleId("cycle-1");
        when(observationMapper.selectById("obs-1")).thenReturn(observation);
        var cycle = new PlantingCycle();
        cycle.setCropId("crop-1");
        cycle.setFieldId("field-1");
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);
        var crop = new Crop();
        crop.setName("水稻");
        when(cropMapper.selectById("crop-1")).thenReturn(crop);
        var field = new Field();
        field.setFarmId("farm-1");
        when(fieldMapper.selectById("field-1")).thenReturn(field);
        var farm = new Farm();
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
    }
}
