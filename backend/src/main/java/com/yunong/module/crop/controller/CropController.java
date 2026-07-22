package com.yunong.module.crop.controller;

import com.yunong.common.AuditLog;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.service.CropService;
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

    private final CropService service;

    @PostMapping("/crops")
    @AuditLog(action = "添加作物品种")
    @Operation(summary = "添加作物品种")
    public R<Crop> createCrop(@Valid @RequestBody Crop crop) {
        return R.ok(service.createCrop(crop));
    }

    @GetMapping("/crops")
    @Operation(summary = "作物列表")
    public R<PageResult<Crop>> listCrops(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        return R.ok(service.listCrops(page, size, category));
    }

    @GetMapping("/crops/{id}")
    @Operation(summary = "作物详情")
    public R<Crop> getCrop(@PathVariable String id) {
        return R.ok(service.getCrop(id));
    }

    @PostMapping("/planting-cycles")
    @AuditLog(action = "创建种植周期")
    @Operation(summary = "创建种植周期")
    public R<PlantingCycle> createCycle(@Valid @RequestBody PlantingCycle cycle,
                                         @AuthenticationPrincipal UserDetails principal) {
        return R.ok(service.createCycle(cycle, principal.getUsername()));
    }

    @GetMapping("/planting-cycles")
    @Operation(summary = "种植周期列表")
    public R<PageResult<PlantingCycle>> listCycles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String fieldId,
            @RequestParam(required = false) String status) {
        return R.ok(service.listCycles(page, size, fieldId, status));
    }

    @PutMapping("/planting-cycles/{id}")
    @AuditLog(action = "更新种植周期")
    @Operation(summary = "更新种植周期(生育期)")
    public R<PlantingCycle> updateCycle(@PathVariable String id, @RequestBody PlantingCycle update) {
        return R.ok(service.updateCycle(id, update));
    }
}
