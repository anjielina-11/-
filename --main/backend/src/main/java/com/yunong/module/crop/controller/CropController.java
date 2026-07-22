package com.yunong.module.crop.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "作物与种植管理", description = "作物品种CRUD和种植周期管理")
public class CropController {

    private final CropMapper cropMapper;
    private final PlantingCycleMapper cycleMapper;

    // === 作物 ===
    @PostMapping("/crops")
    @Operation(summary = "添加作物品种")
    public R<Crop> createCrop(@Valid @RequestBody Crop crop) {
        cropMapper.insert(crop);
        return R.ok(crop);
    }

    @GetMapping("/crops")
    @Operation(summary = "作物列表")
    public R<PageResult<Crop>> listCrops(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        var wrapper = new LambdaQueryWrapper<Crop>();
        if (category != null) wrapper.eq(Crop::getCategory, category);
        wrapper.orderByDesc(Crop::getCreatedAt);
        var result = cropMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/crops/{id}")
    @Operation(summary = "作物详情")
    public R<Crop> getCrop(@PathVariable String id) {
        var crop = cropMapper.selectById(id);
        if (crop == null) throw new BusinessException(ErrorCode.CROP_NOT_FOUND);
        return R.ok(crop);
    }

    // === 种植周期 ===
    @PostMapping("/planting-cycles")
    @Operation(summary = "创建种植周期")
    public R<PlantingCycle> createCycle(@Valid @RequestBody PlantingCycle cycle,
                                         @AuthenticationPrincipal UserDetails principal) {
        cycle.setCreatedBy(principal.getUsername());
        cycle.setStatus("active");
        cycleMapper.insert(cycle);
        return R.ok(cycle);
    }

    @GetMapping("/planting-cycles")
    @Operation(summary = "种植周期列表")
    public R<PageResult<PlantingCycle>> listCycles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String fieldId,
            @RequestParam(required = false) String status) {
        var wrapper = new LambdaQueryWrapper<PlantingCycle>();
        if (fieldId != null) wrapper.eq(PlantingCycle::getFieldId, fieldId);
        if (status != null) wrapper.eq(PlantingCycle::getStatus, status);
        wrapper.orderByDesc(PlantingCycle::getCreatedAt);
        var result = cycleMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PutMapping("/planting-cycles/{id}")
    @Operation(summary = "更新种植周期(生育期)")
    public R<PlantingCycle> updateCycle(@PathVariable String id, @RequestBody PlantingCycle update) {
        var cycle = cycleMapper.selectById(id);
        if (cycle == null) throw new BusinessException(ErrorCode.PLANTING_CYCLE_NOT_FOUND);
        if (update.getGrowthStage() != null) cycle.setGrowthStage(update.getGrowthStage());
        if (update.getStatus() != null) cycle.setStatus(update.getStatus());
        if (update.getActualHarvestDate() != null) cycle.setActualHarvestDate(update.getActualHarvestDate());
        if (update.getRemark() != null) cycle.setRemark(update.getRemark());
        cycleMapper.updateById(cycle);
        return R.ok(cycle);
    }
}
