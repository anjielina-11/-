package com.yunong.module.farm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.common.R;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.farm.dto.FieldDTO;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@Tag(name = "农场管理", description = "农场和地块CRUD")
public class FarmController {

    private final FarmMapper farmMapper;
    private final FieldMapper fieldMapper;

    @PostMapping
    @Operation(summary = "创建农场")
    public R<Farm> create(@Valid @RequestBody Farm farm, @AuthenticationPrincipal UserDetails principal) {
        farm.setOwnerId(principal.getUsername()); // username is userId
        farmMapper.insert(farm);
        return R.ok(farm);
    }

    @GetMapping
    @Operation(summary = "我的农场列表")
    public R<PageResult<Farm>> list(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        var wrapper = new LambdaQueryWrapper<Farm>()
                .eq(Farm::getOwnerId, principal.getUsername())
                .orderByDesc(Farm::getCreatedAt);
        var result = farmMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "农场详情")
    public R<Farm> getById(@PathVariable String id) {
        var farm = farmMapper.selectById(id);
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        return R.ok(farm);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新农场")
    public R<Farm> update(@PathVariable String id, @RequestBody Farm update) {
        var farm = farmMapper.selectById(id);
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        if (update.getName() != null) farm.setName(update.getName());
        if (update.getAddress() != null) farm.setAddress(update.getAddress());
        if (update.getAreaMu() != null) farm.setAreaMu(update.getAreaMu());
        if (update.getContact() != null) farm.setContact(update.getContact());
        if (update.getRemark() != null) farm.setRemark(update.getRemark());
        farmMapper.updateById(farm);
        return R.ok(farm);
    }

    @PostMapping("/{farmId}/fields")
    @Operation(summary = "添加地块")
    public R<Field> addField(@PathVariable String farmId, @Valid @RequestBody Field field) {
        if (farmMapper.selectById(farmId) == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        field.setFarmId(farmId);
        fieldMapper.insert(field);
        return R.ok(field);
    }

    @GetMapping("/{farmId}/fields")
    @Operation(summary = "地块列表")
    public R<PageResult<FieldDTO>> listFields(@PathVariable String farmId) {
        var fields = fieldMapper.selectList(new LambdaQueryWrapper<Field>()
                .eq(Field::getFarmId, farmId).orderByDesc(Field::getCreatedAt));
        // TODO: cropType 需关联 planting_cycles → crops 查询，暂时留空
        List<FieldDTO> list = fields.stream()
                .map(f -> FieldDTO.from(f, null))
                .collect(Collectors.toList());
        return R.ok(PageResult.of(list, list.size()));
    }
}
