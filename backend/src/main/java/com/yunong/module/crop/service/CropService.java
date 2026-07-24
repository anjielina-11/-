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
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CropService {

    private final CropMapper cropMapper;
    private final PlantingCycleMapper cycleMapper;
    private final FieldMapper fieldMapper;
    private final FarmMapper farmMapper;

    public Crop createCrop(Crop crop) {
        cropMapper.insert(crop);
        return crop;
    }

    public PageResult<Crop> listCrops(int page, int size, String category) {
        var wrapper = new LambdaQueryWrapper<Crop>();
        if (category != null) wrapper.eq(Crop::getCategory, category);
        wrapper.orderByDesc(Crop::getCreatedAt);
        var result = cropMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public Crop getCrop(String id) {
        var crop = cropMapper.selectById(id);
        if (crop == null) throw new BusinessException(ErrorCode.CROP_NOT_FOUND);
        return crop;
    }

    public PlantingCycle createCycle(PlantingCycle cycle, String createdBy) {
        assertFieldOwner(cycle.getFieldId(), createdBy);
        cycle.setCreatedBy(createdBy);
        cycle.setStatus("active");
        cycleMapper.insert(cycle);
        return cycle;
    }

    public PageResult<PlantingCycle> listCycles(int page, int size, String fieldId, String status, String userId) {
        var wrapper = new LambdaQueryWrapper<PlantingCycle>();
        wrapper.eq(PlantingCycle::getCreatedBy, userId);
        if (fieldId != null) wrapper.eq(PlantingCycle::getFieldId, fieldId);
        if (status != null) wrapper.eq(PlantingCycle::getStatus, status);
        wrapper.orderByDesc(PlantingCycle::getCreatedAt);
        var result = cycleMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public void deleteCycle(String id, String userId) {
        getOwnedCycle(id, userId);
        cycleMapper.deleteById(id);
    }

    public PlantingCycle updateCycle(String id, PlantingCycle update, String userId) {
        var cycle = getOwnedCycle(id, userId);
        if (update.getCropId() != null) cycle.setCropId(update.getCropId());
        if (update.getPlantingDate() != null) cycle.setPlantingDate(update.getPlantingDate());
        if (update.getExpectedHarvestDate() != null) cycle.setExpectedHarvestDate(update.getExpectedHarvestDate());
        if (update.getAreaMu() != null) cycle.setAreaMu(update.getAreaMu());
        if (update.getGrowthStage() != null) cycle.setGrowthStage(update.getGrowthStage());
        if (update.getStatus() != null) cycle.setStatus(update.getStatus());
        if (update.getActualHarvestDate() != null) cycle.setActualHarvestDate(update.getActualHarvestDate());
        if (update.getRemark() != null) cycle.setRemark(update.getRemark());
        cycleMapper.updateById(cycle);
        return cycle;
    }

    private PlantingCycle getOwnedCycle(String id, String userId) {
        var cycle = cycleMapper.selectById(id);
        if (cycle == null) throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND);
        if (!userId.equals(cycle.getCreatedBy())) throw new BusinessException(ErrorCode.NOT_FARM_OWNER);
        return cycle;
    }

    private void assertFieldOwner(String fieldId, String userId) {
        Field field = fieldMapper.selectById(fieldId);
        if (field == null) throw new BusinessException(ErrorCode.FIELD_NOT_FOUND);
        Farm farm = farmMapper.selectById(field.getFarmId());
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        if (!userId.equals(farm.getOwnerId())) throw new BusinessException(ErrorCode.NOT_FARM_OWNER);
    }
}
