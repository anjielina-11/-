package com.yunong.module.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FarmService {

    private static final Set<String> FARM_STATUSES = Set.of("active", "archived");

    private final FarmMapper farmMapper;
    private final FieldMapper fieldMapper;
    private final PlantingCycleMapper cycleMapper;

    public Farm create(Farm farm, String ownerId) {
        farm.setOwnerId(ownerId);
        farm.setStatus("active");
        farmMapper.insert(farm);
        return farm;
    }

    public PageResult<Farm> listByOwner(String ownerId, int page, int size) {
        return listByOwner(ownerId, page, size, false);
    }

    public PageResult<Farm> listByOwner(String ownerId, int page, int size, boolean includeArchived) {
        var wrapper = new LambdaQueryWrapper<Farm>()
                .eq(Farm::getOwnerId, ownerId);
        if (!includeArchived) wrapper.eq(Farm::getStatus, "active");
        wrapper.orderByDesc(Farm::getCreatedAt);
        var result = farmMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public PageResult<Farm> listAll(int page, int size) {
        return listAll(page, size, false);
    }

    public PageResult<Farm> listAll(int page, int size, boolean includeArchived) {
        var wrapper = new LambdaQueryWrapper<Farm>();
        if (!includeArchived) wrapper.eq(Farm::getStatus, "active");
        wrapper.orderByDesc(Farm::getCreatedAt);
        var result = farmMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public Farm getById(String id, String ownerId) {
        var farm = farmMapper.selectById(id);
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        if (!farm.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.NOT_FARM_OWNER);
        return farm;
    }

    public Farm update(String id, Farm update, String ownerId) {
        var farm = getById(id, ownerId);
        if (update.getName() != null) farm.setName(update.getName());
        if (update.getAddress() != null) farm.setAddress(update.getAddress());
        if (update.getAreaMu() != null) farm.setAreaMu(update.getAreaMu());
        if (update.getContact() != null) farm.setContact(update.getContact());
        if (update.getRemark() != null) farm.setRemark(update.getRemark());
        farmMapper.updateById(farm);
        return farm;
    }

    public Farm updateStatus(String id, String status, String ownerId) {
        if (!FARM_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.FARM_STATUS_INVALID);
        }
        var farm = getById(id, ownerId);
        farm.setStatus(status);
        farmMapper.updateById(farm);
        return farm;
    }

    public Field addField(String farmId, Field field, String ownerId) {
        getActiveOwnedFarm(farmId, ownerId);
        field.setFarmId(farmId);
        fieldMapper.insert(field);
        return field;
    }

    public List<Field> listFields(String farmId, String ownerId) {
        getById(farmId, ownerId);
        return fieldMapper.selectList(new LambdaQueryWrapper<Field>()
                .eq(Field::getFarmId, farmId).orderByDesc(Field::getCreatedAt));
    }

    public Field updateField(String farmId, String fieldId, Field update, String ownerId) {
        getActiveOwnedFarm(farmId, ownerId);
        var field = getField(farmId, fieldId);
        if (update.getName() != null) field.setName(update.getName());
        if (update.getAreaMu() != null) field.setAreaMu(update.getAreaMu());
        if (update.getSoilType() != null) field.setSoilType(update.getSoilType());
        if (update.getRemark() != null) field.setRemark(update.getRemark());
        fieldMapper.updateById(field);
        return field;
    }

    public void deleteField(String farmId, String fieldId, String ownerId) {
        getActiveOwnedFarm(farmId, ownerId);
        getField(farmId, fieldId);
        long cycleCount = cycleMapper.selectCount(new LambdaQueryWrapper<PlantingCycle>()
                .eq(PlantingCycle::getFieldId, fieldId));
        if (cycleCount > 0) {
            throw new BusinessException(ErrorCode.FIELD_HAS_PLANTING_HISTORY,
                    "地块存在 " + cycleCount + " 条种植记录，请保留历史数据或先处理关联记录");
        }
        fieldMapper.deleteById(fieldId);
    }

    private Farm getActiveOwnedFarm(String farmId, String ownerId) {
        var farm = getById(farmId, ownerId);
        if (!"active".equals(farm.getStatus())) {
            throw new BusinessException(ErrorCode.FARM_ARCHIVED);
        }
        return farm;
    }

    private Field getField(String farmId, String fieldId) {
        var field = fieldMapper.selectById(fieldId);
        if (field == null || !farmId.equals(field.getFarmId())) {
            throw new BusinessException(ErrorCode.FIELD_NOT_FOUND);
        }
        return field;
    }
}
