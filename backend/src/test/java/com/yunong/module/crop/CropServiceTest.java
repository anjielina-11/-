package com.yunong.module.crop;

import com.yunong.module.crop.entity.PlantingCycle;
import com.yunong.module.crop.mapper.CropMapper;
import com.yunong.module.crop.mapper.PlantingCycleMapper;
import com.yunong.module.crop.service.CropService;
import com.yunong.module.farm.entity.Farm;
import com.yunong.module.farm.entity.Field;
import com.yunong.module.farm.mapper.FarmMapper;
import com.yunong.module.farm.mapper.FieldMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CropServiceTest {
    @Mock CropMapper cropMapper;
    @Mock PlantingCycleMapper cycleMapper;
    @Mock FieldMapper fieldMapper;
    @Mock FarmMapper farmMapper;

    @Test
    void deleteExistingPlantingCycle() {
        var cycle = new PlantingCycle();
        cycle.setId("cycle-1");
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);
        var service = new CropService(cropMapper, cycleMapper, fieldMapper, farmMapper);

        cycle.setCreatedBy("user-1");
        service.deleteCycle("cycle-1", "user-1");

        verify(cycleMapper).deleteById("cycle-1");
    }

    @Test
    void rejectsCycleCreationForAnotherUsersField() {
        var field = new Field();
        field.setId("field-1");
        field.setFarmId("farm-1");
        var farm = new Farm();
        farm.setId("farm-1");
        farm.setOwnerId("owner-1");
        when(fieldMapper.selectById("field-1")).thenReturn(field);
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        var service = new CropService(cropMapper, cycleMapper, fieldMapper, farmMapper);
        var cycle = new PlantingCycle();
        cycle.setFieldId("field-1");

        var error = assertThrows(BusinessException.class,
                () -> service.createCycle(cycle, "owner-2"));

        assertEquals(ErrorCode.NOT_FARM_OWNER.getCode(), error.getCode());
    }
}
