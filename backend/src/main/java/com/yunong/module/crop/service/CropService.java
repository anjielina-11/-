package com.yunong.module.crop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.diagnosis.entity.Observation;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.task.entity.FarmingTask;
import com.yunong.module.task.mapper.FarmingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CropService {

    private static final Set<String> CROP_STATUSES = Set.of("active", "inactive");
    private static final Set<String> CYCLE_STATUSES = Set.of("active", "pending_harvest", "completed");
    private static final Set<String> OCCUPYING_CYCLE_STATUSES = Set.of("active", "pending_harvest");

    private final CropMapper cropMapper;
    private final PlantingCycleMapper cycleMapper;
    private final FieldMapper fieldMapper;
    private final FarmMapper farmMapper;
    private final ObservationMapper observationMapper;
    private final FarmingTaskMapper taskMapper;

    public Crop createCrop(Crop crop) {
        validateCrop(crop);
        crop.setName(crop.getName().trim());
        crop.setStatus("active");
        cropMapper.insert(crop);
        return crop;
    }

    public PageResult<Crop> listCrops(int page, int size, String category) {
        return listCrops(page, size, category, false);
    }

    public PageResult<Crop> listCrops(int page, int size, String category, boolean includeInactive) {
        var wrapper = new LambdaQueryWrapper<Crop>();
        if (!includeInactive) wrapper.eq(Crop::getStatus, "active");
        if (category != null && !category.isBlank()) wrapper.eq(Crop::getCategory, category);
        wrapper.orderByDesc(Crop::getCreatedAt);
        var result = cropMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public Crop getCrop(String id) {
        var crop = cropMapper.selectById(id);
        if (crop == null) throw new BusinessException(ErrorCode.CROP_NOT_FOUND);
        return crop;
    }

    public Crop updateCrop(String id, Crop update) {
        var crop = getCrop(id);
        if (update.getName() != null) crop.setName(update.getName());
        if (update.getCategory() != null) crop.setCategory(update.getCategory());
        if (update.getVariety() != null) crop.setVariety(update.getVariety());
        if (update.getGrowthDays() != null) crop.setGrowthDays(update.getGrowthDays());
        if (update.getOptimalTempMin() != null) crop.setOptimalTempMin(update.getOptimalTempMin());
        if (update.getOptimalTempMax() != null) crop.setOptimalTempMax(update.getOptimalTempMax());
        if (update.getDescription() != null) crop.setDescription(update.getDescription());
        if (update.getImageUrl() != null) crop.setImageUrl(update.getImageUrl());
        validateCrop(crop);
        crop.setName(crop.getName().trim());
        cropMapper.updateById(crop);
        return crop;
    }

    public Crop updateCropStatus(String id, String status) {
        if (!CROP_STATUSES.contains(status)) throw new BusinessException(ErrorCode.CROP_STATUS_INVALID);
        var crop = getCrop(id);
        crop.setStatus(status);
        cropMapper.updateById(crop);
        return crop;
    }

    @Transactional
    public PlantingCycle createCycle(PlantingCycle cycle, String createdBy) {
        Field field = getOwnedField(cycle.getFieldId(), createdBy, true);
        assertActiveCrop(cycle.getCropId());
        cycle.setCreatedBy(createdBy);
        cycle.setStatus("active");
        cycle.setActualHarvestDate(null);
        validateCycle(cycle);
        ensureFieldCapacity(field, cycle, null);
        cycleMapper.insert(cycle);
        return cycle;
    }

    public PageResult<PlantingCycle> listCycles(int page, int size, String fieldId, String status, String userId) {
        if (status != null && !status.isBlank() && !CYCLE_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.PLANTING_STATUS_INVALID);
        }
        var wrapper = new LambdaQueryWrapper<PlantingCycle>();
        wrapper.eq(PlantingCycle::getCreatedBy, userId);
        if (fieldId != null && !fieldId.isBlank()) wrapper.eq(PlantingCycle::getFieldId, fieldId);
        if (status != null && !status.isBlank()) wrapper.eq(PlantingCycle::getStatus, status);
        wrapper.orderByDesc(PlantingCycle::getCreatedAt);
        var result = cycleMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Transactional
    public void deleteCycle(String id, String userId) {
        getOwnedCycle(id, userId);
        long observationCount = observationMapper.selectCount(new LambdaQueryWrapper<Observation>()
                .eq(Observation::getCycleId, id));
        long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<FarmingTask>()
                .eq(FarmingTask::getCycleId, id));
        if (observationCount > 0 || taskCount > 0) {
            throw new BusinessException(ErrorCode.CYCLE_HAS_BUSINESS_HISTORY,
                    "\u5b58\u5728 " + observationCount + " \u6761\u89c2\u6d4b\u8bb0\u5f55\u548c " + taskCount + " \u6761\u519c\u4e8b\u4efb\u52a1\uff0c\u4e0d\u80fd\u5220\u9664\u79cd\u690d\u5468\u671f");
        }
        cycleMapper.deleteById(id);
    }

    @Transactional
    public PlantingCycle updateCycle(String id, PlantingCycle update, String userId) {
        var cycle = getOwnedCycle(id, userId);
        if ("completed".equals(cycle.getStatus()) && update.getStatus() != null
                && !"completed".equals(update.getStatus())) {
            throw new BusinessException(ErrorCode.CYCLE_ALREADY_COMPLETED);
        }
        if (update.getCropId() != null && !Objects.equals(update.getCropId(), cycle.getCropId())) {
            assertActiveCrop(update.getCropId());
            cycle.setCropId(update.getCropId());
        }
        if (update.getPlantingDate() != null) cycle.setPlantingDate(update.getPlantingDate());
        if (update.getExpectedHarvestDate() != null) cycle.setExpectedHarvestDate(update.getExpectedHarvestDate());
        if (update.getAreaMu() != null) cycle.setAreaMu(update.getAreaMu());
        if (update.getGrowthStage() != null) cycle.setGrowthStage(update.getGrowthStage());
        if (update.getStatus() != null) cycle.setStatus(update.getStatus());
        if (update.getActualHarvestDate() != null) cycle.setActualHarvestDate(update.getActualHarvestDate());
        if (update.getRemark() != null) cycle.setRemark(update.getRemark());

        validateCycle(cycle);
        boolean requiresActiveFarm = !"completed".equals(cycle.getStatus());
        Field field = getOwnedField(cycle.getFieldId(), userId, requiresActiveFarm);
        ensureFieldCapacity(field, cycle, id);
        cycleMapper.updateById(cycle);
        return cycle;
    }

    private PlantingCycle getOwnedCycle(String id, String userId) {
        var cycle = cycleMapper.selectById(id);
        if (cycle == null) throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND);
        if (!userId.equals(cycle.getCreatedBy())) throw new BusinessException(ErrorCode.NOT_FARM_OWNER);
        return cycle;
    }

    private Field getOwnedField(String fieldId, String userId, boolean requireActiveFarm) {
        if (fieldId == null || fieldId.isBlank()) throw new BusinessException(ErrorCode.PLANTING_REQUIRED_FIELDS);
        Field field = fieldMapper.selectById(fieldId);
        if (field == null) throw new BusinessException(ErrorCode.FIELD_NOT_FOUND);
        Farm farm = farmMapper.selectById(field.getFarmId());
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        if (!userId.equals(farm.getOwnerId())) throw new BusinessException(ErrorCode.NOT_FARM_OWNER);
        if (requireActiveFarm && !"active".equals(farm.getStatus())) throw new BusinessException(ErrorCode.FARM_ARCHIVED);
        return field;
    }

    private void assertActiveCrop(String cropId) {
        if (cropId == null || cropId.isBlank()) throw new BusinessException(ErrorCode.PLANTING_REQUIRED_FIELDS);
        var crop = getCrop(cropId);
        if (!"active".equals(crop.getStatus())) throw new BusinessException(ErrorCode.CROP_INACTIVE);
    }

    private void validateCrop(Crop crop) {
        if (isBlank(crop.getName())) {
            throw new BusinessException(ErrorCode.CROP_DATA_INVALID, "\u4f5c\u7269\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (crop.getGrowthDays() != null && crop.getGrowthDays() <= 0) {
            throw new BusinessException(ErrorCode.CROP_DATA_INVALID, "\u751f\u957f\u5468\u671f\u5fc5\u987b\u5927\u4e8e0\u5929");
        }
        if (crop.getOptimalTempMin() != null && crop.getOptimalTempMax() != null
                && crop.getOptimalTempMin().compareTo(crop.getOptimalTempMax()) > 0) {
            throw new BusinessException(ErrorCode.CROP_DATA_INVALID, "\u6700\u4f4e\u9002\u5b9c\u6e29\u5ea6\u4e0d\u80fd\u9ad8\u4e8e\u6700\u9ad8\u9002\u5b9c\u6e29\u5ea6");
        }
    }

    private void validateCycle(PlantingCycle cycle) {
        if (isBlank(cycle.getFieldId()) || isBlank(cycle.getCropId())
                || cycle.getPlantingDate() == null || cycle.getExpectedHarvestDate() == null) {
            throw new BusinessException(ErrorCode.PLANTING_REQUIRED_FIELDS);
        }
        if (!CYCLE_STATUSES.contains(cycle.getStatus())) {
            throw new BusinessException(ErrorCode.PLANTING_STATUS_INVALID);
        }
        if (cycle.getAreaMu() == null || cycle.getAreaMu().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PLANTING_AREA_INVALID);
        }
        LocalDate today = LocalDate.now();
        if (cycle.getPlantingDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.PLANTING_DATE_INVALID, "\u79cd\u690d\u65e5\u671f\u4e0d\u80fd\u665a\u4e8e\u4eca\u5929");
        }
        if (cycle.getExpectedHarvestDate().isBefore(cycle.getPlantingDate())) {
            throw new BusinessException(ErrorCode.PLANTING_DATE_INVALID, "\u9884\u8ba1\u6536\u83b7\u65e5\u671f\u4e0d\u80fd\u65e9\u4e8e\u79cd\u690d\u65e5\u671f");
        }
        if (cycle.getActualHarvestDate() != null
                && (cycle.getActualHarvestDate().isBefore(cycle.getPlantingDate())
                || cycle.getActualHarvestDate().isAfter(today))) {
            throw new BusinessException(ErrorCode.PLANTING_DATE_INVALID,
                    "\u5b9e\u9645\u6536\u83b7\u65e5\u671f\u5fc5\u987b\u4ecb\u4e8e\u79cd\u690d\u65e5\u671f\u548c\u4eca\u5929\u4e4b\u95f4");
        }
        if ("completed".equals(cycle.getStatus()) && cycle.getActualHarvestDate() == null) {
            throw new BusinessException(ErrorCode.PLANTING_COMPLETION_INVALID);
        }
        if (!"completed".equals(cycle.getStatus()) && cycle.getActualHarvestDate() != null) {
            throw new BusinessException(ErrorCode.PLANTING_COMPLETION_INVALID,
                    "\u672a\u5b8c\u6210\u7684\u79cd\u690d\u5468\u671f\u4e0d\u5e94\u586b\u5199\u5b9e\u9645\u6536\u83b7\u65e5\u671f");
        }
    }

    private void ensureFieldCapacity(Field field, PlantingCycle cycle, String excludedCycleId) {
        if (!OCCUPYING_CYCLE_STATUSES.contains(cycle.getStatus())) return;
        if (field.getAreaMu() == null || field.getAreaMu().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.FIELD_AREA_INVALID);
        }
        var existing = cycleMapper.selectList(new LambdaQueryWrapper<PlantingCycle>()
                .eq(PlantingCycle::getFieldId, field.getId())
                .in(PlantingCycle::getStatus, OCCUPYING_CYCLE_STATUSES));
        BigDecimal occupied = (existing == null ? List.<PlantingCycle>of() : existing).stream()
                .filter(item -> excludedCycleId == null || !excludedCycleId.equals(item.getId()))
                .map(PlantingCycle::getAreaMu)
                .filter(area -> area != null && area.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = occupied.add(cycle.getAreaMu());
        if (total.compareTo(field.getAreaMu()) > 0) {
            throw new BusinessException(ErrorCode.PLANTING_AREA_EXCEEDS_FIELD,
                    "\u5730\u5757 " + field.getAreaMu() + " \u4ea9\uff0c\u5f53\u524d\u672a\u6536\u83b7\u79cd\u690d\u5408\u8ba1\u5c06\u8fbe " + total + " \u4ea9");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
