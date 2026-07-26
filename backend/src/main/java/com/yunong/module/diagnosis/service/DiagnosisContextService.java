package com.yunong.module.diagnosis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.diagnosis.dto.DiagnosisContext;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.weather.entity.WeatherRecord;
import com.yunong.module.weather.mapper.WeatherRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class DiagnosisContextService {

    private final DiagnosisRecordMapper diagnosisMapper;
    private final ObservationMapper observationMapper;
    private final PlantingCycleMapper cycleMapper;
    private final CropMapper cropMapper;
    private final FieldMapper fieldMapper;
    private final FarmMapper farmMapper;
    private final WeatherRecordMapper weatherMapper;
    private final Clock clock;

    @Autowired
    public DiagnosisContextService(
            DiagnosisRecordMapper diagnosisMapper,
            ObservationMapper observationMapper,
            PlantingCycleMapper cycleMapper,
            CropMapper cropMapper,
            FieldMapper fieldMapper,
            FarmMapper farmMapper,
            WeatherRecordMapper weatherMapper
    ) {
        this(diagnosisMapper, observationMapper, cycleMapper, cropMapper, fieldMapper,
                farmMapper, weatherMapper, Clock.systemDefaultZone());
    }

    public DiagnosisContextService(
            DiagnosisRecordMapper diagnosisMapper,
            ObservationMapper observationMapper,
            PlantingCycleMapper cycleMapper,
            CropMapper cropMapper,
            FieldMapper fieldMapper,
            FarmMapper farmMapper,
            WeatherRecordMapper weatherMapper,
            Clock clock
    ) {
        this.diagnosisMapper = diagnosisMapper;
        this.observationMapper = observationMapper;
        this.cycleMapper = cycleMapper;
        this.cropMapper = cropMapper;
        this.fieldMapper = fieldMapper;
        this.farmMapper = farmMapper;
        this.weatherMapper = weatherMapper;
        this.clock = clock;
    }

    public DiagnosisContext load(String diagnosisId) {
        var diagnosis = diagnosisMapper.selectById(diagnosisId);
        if (diagnosis == null) {
            throw new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        }
        var observation = observationMapper.selectById(diagnosis.getObservationId());
        if (observation == null || observation.getCycleId() == null) {
            throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND, "诊断记录未关联有效观察记录");
        }
        var cycle = cycleMapper.selectById(observation.getCycleId());
        if (cycle == null) {
            throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND);
        }

        var crop = cycle.getCropId() == null ? null : cropMapper.selectById(cycle.getCropId());
        var field = cycle.getFieldId() == null ? null : fieldMapper.selectById(cycle.getFieldId());
        var farm = field == null || field.getFarmId() == null ? null : farmMapper.selectById(field.getFarmId());

        var cropContext = new DiagnosisContext.CropContext(
                crop == null || crop.getName() == null ? "未知作物" : crop.getName(),
                crop == null ? null : crop.getVariety(),
                cycle.getPlantingDate(),
                normalizeGrowthStage(cycle.getGrowthStage())
        );
        var fieldContext = new DiagnosisContext.FieldContext(
                field == null || field.getName() == null ? "未知地块" : field.getName(),
                farm == null || farm.getName() == null ? "未知农场" : farm.getName()
        );
        return new DiagnosisContext(cropContext, fieldContext, loadWeather(farm == null ? null : farm.getId()));
    }

    private String normalizeGrowthStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return null;
        }
        return switch (stage) {
            case "\u64ad\u79cd\u671f" -> "sowing";
            case "\u82d7\u671f" -> "seedling";
            case "\u5206\u8616\u671f" -> "tillering";
            case "\u5f00\u82b1\u671f" -> "flowering";
            case "\u7ed3\u679c\u671f" -> "fruiting";
            case "\u6210\u719f\u671f" -> "maturity";
            default -> stage;
        };
    }

    private List<DiagnosisContext.WeatherForecast> loadWeather(String farmId) {
        if (farmId == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(7).atTime(LocalTime.MIN);
        var records = weatherMapper.selectList(new LambdaQueryWrapper<WeatherRecord>()
                .eq(WeatherRecord::getFarmId, farmId)
                .ge(WeatherRecord::getRecordedAt, start)
                .lt(WeatherRecord::getRecordedAt, end)
                .orderByAsc(WeatherRecord::getRecordedAt));

        var daily = new LinkedHashMap<LocalDate, WeatherRecord>();
        for (var record : records) {
            if (record.getRecordedAt() != null) {
                daily.put(record.getRecordedAt().toLocalDate(), record);
            }
        }
        return daily.entrySet().stream().limit(7).map(entry -> {
            var record = entry.getValue();
            return new DiagnosisContext.WeatherForecast(
                    entry.getKey(),
                    record.getWeatherDesc() == null ? "未知" : record.getWeatherDesc(),
                    record.getTemperature(),
                    record.getHumidity(),
                    record.getRainfall(),
                    record.getWindSpeed()
            );
        }).toList();
    }
}