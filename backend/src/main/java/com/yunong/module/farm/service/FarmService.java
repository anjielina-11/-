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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FarmService {

    private static final Set<String> FARM_STATUSES = Set.of("active", "archived");
    private static final Set<String> OCCUPYING_CYCLE_STATUSES = Set.of("active", "pending_harvest");

    private final FarmMapper farmMapper;
    private final FieldMapper fieldMapper;
    private final PlantingCycleMapper cycleMapper;

    @Transactional
    public Farm create(Farm farm, String ownerId) {
        validateFarmName(farm.getName());
        validatePositiveArea(farm.getAreaMu(), ErrorCode.FARM_AREA_INVALID);
        farm.setName(farm.getName().trim());
        farm.setOwnerId(ownerId);
        farm.setStatus("active");
        farmMapper.insert(farm);
        return farm;
    }

    public PageResult<Farm> listByOwner(String ownerId, int page, int size) {
        return listByOwner(ownerId, page, size, false);
    }

    public PageResult<Farm> listByOwner(String ownerId, int page, int size, boolean includeArchived) {
        var wrapper = new LambdaQueryWrapper<Farm>().eq(Farm::getOwnerId, ownerId);
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
        if (!ownerId.equals(farm.getOwnerId())) throw new BusinessException(ErrorCode.NOT_FARM_OWNER);
        return farm;
    }

    @Transactional
    public Farm update(String id, Farm update, String ownerId) {
        var farm = getById(id, ownerId);
        if (update.getName() != null) {
            validateFarmName(update.getName());
            farm.setName(update.getName().trim());
        }
        if (update.getAddress() != null) farm.setAddress(update.getAddress());
        if (update.getAreaMu() != null) {
            validatePositiveArea(update.getAreaMu(), ErrorCode.FARM_AREA_INVALID);
            BigDecimal fieldTotal = totalFieldArea(id, null);
            if (update.getAreaMu().compareTo(fieldTotal) < 0) {
                throw new BusinessException(ErrorCode.FARM_AREA_BELOW_FIELDS,
                        "\u5df2\u6709\u5730\u5757\u5408\u8ba1 " + fieldTotal + " \u4ea9");
            }
            farm.setAreaMu(update.getAreaMu());
        }
        if (update.getContact() != null) farm.setContact(update.getContact());
        if (update.getRemark() != null) farm.setRemark(update.getRemark());
        farmMapper.updateById(farm);
        return farm;
    }

    @Transactional
    public Farm updateStatus(String id, String status, String ownerId) {
        if (!FARM_STATUSES.contains(status)) throw new BusinessException(ErrorCode.FARM_STATUS_INVALID);
        var farm = getById(id, ownerId);
        if ("archived".equals(status) && !"archived".equals(farm.getStatus())) {
            var fieldIds = fieldsOfFarm(id).stream().map(Field::getId).toList();
            if (!fieldIds.isEmpty()) {
                long count = cycleMapper.selectCount(new LambdaQueryWrapper<PlantingCycle>()
                        .in(PlantingCycle::getFieldId, fieldIds)
                        .in(PlantingCycle::getStatus, OCCUPYING_CYCLE_STATUSES));
                if (count > 0) {
                    throw new BusinessException(ErrorCode.FARM_HAS_ACTIVE_PLANTING,
                            "\u5f53\u524d\u5171 " + count + " \u6761\u672a\u6536\u83b7\u8bb0\u5f55");
                }
            }
        }
        farm.setStatus(status);
        farmMapper.updateById(farm);
        return farm;
    }

    @Transactional
    public Field addField(String farmId, Field field, String ownerId) {
        var farm = getActiveOwnedFarm(farmId, ownerId);
        validateField(field);
        ensureFarmCapacity(farm, field.getAreaMu(), null);
        field.setName(field.getName().trim());
        field.setFarmId(farmId);
        fieldMapper.insert(field);
        return field;
    }

    public List<Field> listFields(String farmId, String ownerId) {
        getById(farmId, ownerId);
        return fieldsOfFarm(farmId).stream()
                .sorted((a, b) -> compareCreatedAtDesc(a, b))
                .toList();
    }

    @Transactional
    public Field updateField(String farmId, String fieldId, Field update, String ownerId) {
        var farm = getActiveOwnedFarm(farmId, ownerId);
        var field = getField(farmId, fieldId);
        if (update.getName() != null) {
            if (update.getName().isBlank()) throw new BusinessException(ErrorCode.FIELD_NAME_REQUIRED);
            field.setName(update.getName().trim());
        }
        if (update.getAreaMu() != null) {
            validatePositiveArea(update.getAreaMu(), ErrorCode.FIELD_AREA_INVALID);
            BigDecimal occupied = currentPlantingArea(fieldId, null);
            if (update.getAreaMu().compareTo(occupied) < 0) {
                throw new BusinessException(ErrorCode.FIELD_AREA_BELOW_ACTIVE_PLANTING,
                        "\u5f53\u524d\u672a\u6536\u83b7\u79cd\u690d\u9762\u79ef\u5408\u8ba1 " + occupied + " \u4ea9");
            }
            ensureFarmCapacity(farm, update.getAreaMu(), fieldId);
            field.setAreaMu(update.getAreaMu());
        }
        if (update.getSoilType() != null) field.setSoilType(update.getSoilType());
        if (update.getRemark() != null) field.setRemark(update.getRemark());
        fieldMapper.updateById(field);
        return field;
    }

    @Transactional
    public void deleteField(String farmId, String fieldId, String ownerId) {
        getActiveOwnedFarm(farmId, ownerId);
        getField(farmId, fieldId);
        long cycleCount = cycleMapper.selectCount(new LambdaQueryWrapper<PlantingCycle>()
                .eq(PlantingCycle::getFieldId, fieldId));
        if (cycleCount > 0) {
            throw new BusinessException(ErrorCode.FIELD_HAS_PLANTING_HISTORY,
                    "\u5730\u5757\u5b58\u5728 " + cycleCount + " \u6761\u79cd\u690d\u8bb0\u5f55\uff0c\u8bf7\u4fdd\u7559\u5386\u53f2\u6570\u636e\u6216\u5148\u5904\u7406\u5173\u8054\u8bb0\u5f55");
        }
        fieldMapper.deleteById(fieldId);
    }

    private Farm getActiveOwnedFarm(String farmId, String ownerId) {
        var farm = getById(farmId, ownerId);
        if (!"active".equals(farm.getStatus())) throw new BusinessException(ErrorCode.FARM_ARCHIVED);
        return farm;
    }

    private Field getField(String farmId, String fieldId) {
        var field = fieldMapper.selectById(fieldId);
        if (field == null || !farmId.equals(field.getFarmId())) throw new BusinessException(ErrorCode.FIELD_NOT_FOUND);
        return field;
    }

    private void validateFarmName(String name) {
        if (name == null || name.isBlank()) throw new BusinessException(ErrorCode.FARM_NAME_REQUIRED);
    }

    private void validateField(Field field) {
        if (field.getName() == null || field.getName().isBlank()) {
            throw new BusinessException(ErrorCode.FIELD_NAME_REQUIRED);
        }
        validatePositiveArea(field.getAreaMu(), ErrorCode.FIELD_AREA_INVALID);
    }

    private void validatePositiveArea(BigDecimal area, ErrorCode errorCode) {
        if (area == null || area.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException(errorCode);
    }

    private void ensureFarmCapacity(Farm farm, BigDecimal proposedArea, String excludedFieldId) {
        validatePositiveArea(farm.getAreaMu(), ErrorCode.FARM_AREA_INVALID);
        BigDecimal otherFields = totalFieldArea(farm.getId(), excludedFieldId);
        BigDecimal total = otherFields.add(proposedArea);
        if (total.compareTo(farm.getAreaMu()) > 0) {
            throw new BusinessException(ErrorCode.FIELD_AREA_EXCEEDS_FARM,
                    "\u519c\u573a " + farm.getAreaMu() + " \u4ea9\uff0c\u5730\u5757\u5408\u8ba1\u5c06\u8fbe " + total + " \u4ea9");
        }
    }

    private BigDecimal totalFieldArea(String farmId, String excludedFieldId) {
        return fieldsOfFarm(farmId).stream()
                .filter(field -> excludedFieldId == null || !excludedFieldId.equals(field.getId()))
                .map(Field::getAreaMu)
                .filter(area -> area != null && area.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal currentPlantingArea(String fieldId, String excludedCycleId) {
        var cycles = cycleMapper.selectList(new LambdaQueryWrapper<PlantingCycle>()
                .eq(PlantingCycle::getFieldId, fieldId)
                .in(PlantingCycle::getStatus, OCCUPYING_CYCLE_STATUSES));
        if (cycles == null) return BigDecimal.ZERO;
        return cycles.stream()
                .filter(cycle -> excludedCycleId == null || !excludedCycleId.equals(cycle.getId()))
                .map(PlantingCycle::getAreaMu)
                .filter(area -> area != null && area.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Field> fieldsOfFarm(String farmId) {
        var fields = fieldMapper.selectList(new LambdaQueryWrapper<Field>().eq(Field::getFarmId, farmId));
        return fields == null ? List.of() : fields;
    }

    private int compareCreatedAtDesc(Field a, Field b) {
        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
        if (a.getCreatedAt() == null) return 1;
        if (b.getCreatedAt() == null) return -1;
        return b.getCreatedAt().compareTo(a.getCreatedAt());
    }
}
