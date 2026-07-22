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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CropService {

    private final CropMapper cropMapper;
    private final PlantingCycleMapper cycleMapper;

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
        cycle.setCreatedBy(createdBy);
        cycle.setStatus("active");
        cycleMapper.insert(cycle);
        return cycle;
    }

    public PageResult<PlantingCycle> listCycles(int page, int size, String fieldId, String status) {
        var wrapper = new LambdaQueryWrapper<PlantingCycle>();
        if (fieldId != null) wrapper.eq(PlantingCycle::getFieldId, fieldId);
        if (status != null) wrapper.eq(PlantingCycle::getStatus, status);
        wrapper.orderByDesc(PlantingCycle::getCreatedAt);
        var result = cycleMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public PlantingCycle updateCycle(String id, PlantingCycle update) {
        var cycle = cycleMapper.selectById(id);
        if (cycle == null) throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND);
        if (update.getGrowthStage() != null) cycle.setGrowthStage(update.getGrowthStage());
        if (update.getStatus() != null) cycle.setStatus(update.getStatus());
        if (update.getActualHarvestDate() != null) cycle.setActualHarvestDate(update.getActualHarvestDate());
        if (update.getRemark() != null) cycle.setRemark(update.getRemark());
        cycleMapper.updateById(cycle);
        return cycle;
    }
}
