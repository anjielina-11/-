package com.yunong.module.crop;

import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.crop.entity.Crop;
import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.crop.service.CropService;
import com.yunong.module.diagnosis.mapper.ObservationMapper;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import com.yunong.module.task.mapper.FarmingTaskMapper;
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

@ExtendWith(MockitoExtension.class)
class CropServiceTest {
    @Mock CropMapper cropMapper;
    @Mock PlantingCycleMapper cycleMapper;
    @Mock FieldMapper fieldMapper;
    @Mock FarmMapper farmMapper;
    @Mock ObservationMapper observationMapper;
    @Mock FarmingTaskMapper taskMapper;

    private CropService service;

    @BeforeEach
    void setUp() {
        service = new CropService(cropMapper, cycleMapper, fieldMapper, farmMapper, observationMapper, taskMapper);
    }

    @Test
    void createsActiveCropAndCanDisableIt() {
        var crop = new Crop();
        crop.setId("crop-1");

        service.createCrop(crop);
        assertEquals("active", crop.getStatus());

        when(cropMapper.selectById("crop-1")).thenReturn(crop);
        var disabled = service.updateCropStatus("crop-1", "inactive");
        assertEquals("inactive", disabled.getStatus());
        verify(cropMapper).updateById(crop);
    }

    @Test
    void deletesPlantingCycleWithoutBusinessHistory() {
        var cycle = ownedCycle();
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);
        when(observationMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.selectCount(any())).thenReturn(0L);

        service.deleteCycle("cycle-1", "user-1");

        verify(cycleMapper).deleteById("cycle-1");
    }

    @Test
    void rejectsDeletingCycleWithObservationOrTaskHistory() {
        var cycle = ownedCycle();
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);
        when(observationMapper.selectCount(any())).thenReturn(1L);

        var observationError = assertThrows(BusinessException.class,
                () -> service.deleteCycle("cycle-1", "user-1"));
        assertEquals(ErrorCode.CYCLE_HAS_BUSINESS_HISTORY.getCode(), observationError.getCode());
        verify(cycleMapper, never()).deleteById("cycle-1");

        when(observationMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.selectCount(any())).thenReturn(1L);
        var taskError = assertThrows(BusinessException.class,
                () -> service.deleteCycle("cycle-1", "user-1"));
        assertEquals(ErrorCode.CYCLE_HAS_BUSINESS_HISTORY.getCode(), taskError.getCode());
    }

    @Test
    void rejectsCycleCreationForAnotherUsersField() {
        var field = new Field();
        field.setId("field-1");
        field.setFarmId("farm-1");
        var farm = new Farm();
        farm.setId("farm-1");
        farm.setOwnerId("owner-1");
        farm.setStatus("active");
        when(fieldMapper.selectById("field-1")).thenReturn(field);
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        var cycle = new PlantingCycle();
        cycle.setFieldId("field-1");

        var error = assertThrows(BusinessException.class,
                () -> service.createCycle(cycle, "owner-2"));

        assertEquals(ErrorCode.NOT_FARM_OWNER.getCode(), error.getCode());
    }

    private PlantingCycle ownedCycle() {
        var cycle = new PlantingCycle();
        cycle.setId("cycle-1");
        cycle.setCreatedBy("user-1");
        return cycle;
    }
}
