package com.yunong.module.farm;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.farm.service.FarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {

    @Mock FarmMapper farmMapper;
    @Mock FieldMapper fieldMapper;
    @Mock PlantingCycleMapper cycleMapper;

    private FarmService service;

    @BeforeEach
    void setUp() {
        service = new FarmService(farmMapper, fieldMapper, cycleMapper);
    }

    @Test
    void rejectsAccessToAnotherUsersFarm() {
        var farm = farm("farm-1", "owner-1");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);

        var error = assertThrows(BusinessException.class,
                () -> service.getById("farm-1", "owner-2"));

        assertEquals(ErrorCode.NOT_FARM_OWNER.getCode(), error.getCode());
    }

    @Test
    void listsAllFarmsForCoopDashboard() {
        var first = farm("farm-1", "owner-1");
        var second = farm("farm-2", "owner-2");
        var page = new Page<Farm>(1, 100);
        page.setRecords(java.util.List.of(first, second));
        page.setTotal(2);
        when(farmMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.listAll(1, 100, false);

        assertEquals(2, result.getTotal());
        assertEquals(java.util.List.of(first, second), result.getList());
    }

    @Test
    void archivesAndRestoresOwnedFarm() {
        var farm = farm("farm-1", "owner-1");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);

        var archived = service.updateStatus("farm-1", "archived", "owner-1");
        assertEquals("archived", archived.getStatus());
        verify(farmMapper).updateById(farm);

        var restored = service.updateStatus("farm-1", "active", "owner-1");
        assertEquals("active", restored.getStatus());
    }

    @Test
    void updatesAndDeletesOwnedFieldWithoutPlantingHistory() {
        var farm = farm("farm-1", "owner-1");
        var field = new Field();
        field.setId("field-1");
        field.setFarmId("farm-1");
        field.setName("旧地块");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(fieldMapper.selectById("field-1")).thenReturn(field);
        when(cycleMapper.selectCount(any())).thenReturn(0L);

        var update = new Field();
        update.setName("新地块");
        service.updateField("farm-1", "field-1", update, "owner-1");
        service.deleteField("farm-1", "field-1", "owner-1");

        assertEquals("新地块", field.getName());
        verify(fieldMapper).updateById(field);
        verify(fieldMapper).deleteById("field-1");
    }

    @Test
    void rejectsDeletingFieldWithPlantingHistory() {
        var farm = farm("farm-1", "owner-1");
        var field = new Field();
        field.setId("field-1");
        field.setFarmId("farm-1");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(fieldMapper.selectById("field-1")).thenReturn(field);
        when(cycleMapper.selectCount(any())).thenReturn(2L);

        var error = assertThrows(BusinessException.class,
                () -> service.deleteField("farm-1", "field-1", "owner-1"));

        assertEquals(ErrorCode.FIELD_HAS_PLANTING_HISTORY.getCode(), error.getCode());
        verify(fieldMapper, never()).deleteById("field-1");
    }


    @Test
    void rejectsFarmWithoutValidNameOrArea() {
        var farm = new Farm();
        farm.setName("  ");
        farm.setAreaMu(BigDecimal.TEN);
        var nameError = assertThrows(BusinessException.class, () -> service.create(farm, "owner-1"));
        assertEquals(ErrorCode.FARM_NAME_REQUIRED.getCode(), nameError.getCode());

        farm.setName("\u793a\u8303\u519c\u573a");
        farm.setAreaMu(BigDecimal.ZERO);
        var areaError = assertThrows(BusinessException.class, () -> service.create(farm, "owner-1"));
        assertEquals(ErrorCode.FARM_AREA_INVALID.getCode(), areaError.getCode());
        verify(farmMapper, never()).insert(any(Farm.class));
    }

    @Test
    void rejectsFarmAreaSmallerThanExistingFields() {
        var farm = farm("farm-1", "owner-1");
        farm.setAreaMu(new BigDecimal("20"));
        var field = field("field-1", "farm-1", "12");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(fieldMapper.selectList(any())).thenReturn(List.of(field));
        var update = new Farm();
        update.setAreaMu(new BigDecimal("10"));

        var error = assertThrows(BusinessException.class,
                () -> service.update("farm-1", update, "owner-1"));

        assertEquals(ErrorCode.FARM_AREA_BELOW_FIELDS.getCode(), error.getCode());
        verify(farmMapper, never()).updateById(any(Farm.class));
    }

    @Test
    void rejectsFieldWhenFarmCapacityWouldBeExceeded() {
        var farm = farm("farm-1", "owner-1");
        farm.setAreaMu(new BigDecimal("10"));
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(fieldMapper.selectList(any())).thenReturn(List.of(field("field-1", "farm-1", "6")));
        var newField = field(null, null, "5");
        newField.setName("\u4e8c\u53f7\u5730\u5757");

        var error = assertThrows(BusinessException.class,
                () -> service.addField("farm-1", newField, "owner-1"));

        assertEquals(ErrorCode.FIELD_AREA_EXCEEDS_FARM.getCode(), error.getCode());
        verify(fieldMapper, never()).insert(any(Field.class));
    }

    @Test
    void rejectsShrinkingFieldBelowUnharvestedPlantingArea() {
        var farm = farm("farm-1", "owner-1");
        farm.setAreaMu(new BigDecimal("20"));
        var field = field("field-1", "farm-1", "10");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(fieldMapper.selectById("field-1")).thenReturn(field);
        var cycle = new com.yunong.module.crop.entity.PlantingCycle();
        cycle.setAreaMu(new BigDecimal("8"));
        cycle.setStatus("active");
        when(cycleMapper.selectList(any())).thenReturn(List.of(cycle));
        var update = new Field();
        update.setAreaMu(new BigDecimal("7"));

        var error = assertThrows(BusinessException.class,
                () -> service.updateField("farm-1", "field-1", update, "owner-1"));

        assertEquals(ErrorCode.FIELD_AREA_BELOW_ACTIVE_PLANTING.getCode(), error.getCode());
        verify(fieldMapper, never()).updateById(any(Field.class));
    }

    @Test
    void rejectsArchivingFarmWithUnharvestedPlantingCycle() {
        var farm = farm("farm-1", "owner-1");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(fieldMapper.selectList(any())).thenReturn(List.of(field("field-1", "farm-1", "5")));
        when(cycleMapper.selectCount(any())).thenReturn(1L);

        var error = assertThrows(BusinessException.class,
                () -> service.updateStatus("farm-1", "archived", "owner-1"));

        assertEquals(ErrorCode.FARM_HAS_ACTIVE_PLANTING.getCode(), error.getCode());
    }

    private Field field(String id, String farmId, String area) {
        var field = new Field();
        field.setId(id);
        field.setFarmId(farmId);
        field.setName("\u4e00\u53f7\u5730\u5757");
        field.setAreaMu(new BigDecimal(area));
        return field;
    }

    private Farm farm(String id, String ownerId) {
        var farm = new Farm();
        farm.setId(id);
        farm.setOwnerId(ownerId);
        farm.setStatus("active");
        return farm;
    }
}
