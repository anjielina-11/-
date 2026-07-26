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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        crop.setName("\u6c34\u7a3b");

        service.createCrop(crop);
        assertEquals("active", crop.getStatus());

        when(cropMapper.selectById("crop-1")).thenReturn(crop);
        var disabled = service.updateCropStatus("crop-1", "inactive");
        assertEquals("inactive", disabled.getStatus());
        verify(cropMapper).updateById(crop);
    }


    @Test
    void rejectsInvalidCropMetadata() {
        var blankName = new Crop();
        blankName.setName(" ");
        var nameError = assertThrows(BusinessException.class, () -> service.createCrop(blankName));
        assertEquals(ErrorCode.CROP_DATA_INVALID.getCode(), nameError.getCode());

        var invalidGrowthDays = new Crop();
        invalidGrowthDays.setName("\u6c34\u7a3b");
        invalidGrowthDays.setGrowthDays(0);
        var growthError = assertThrows(BusinessException.class, () -> service.createCrop(invalidGrowthDays));
        assertEquals(ErrorCode.CROP_DATA_INVALID.getCode(), growthError.getCode());

        var invalidTemperature = new Crop();
        invalidTemperature.setName("\u756a\u8304");
        invalidTemperature.setOptimalTempMin(new BigDecimal("30"));
        invalidTemperature.setOptimalTempMax(new BigDecimal("10"));
        var temperatureError = assertThrows(BusinessException.class, () -> service.createCrop(invalidTemperature));
        assertEquals(ErrorCode.CROP_DATA_INVALID.getCode(), temperatureError.getCode());
    }

    @Test
    void allowsEditingCycleWhenItsExistingCropWasLaterDisabled() {
        prepareOwnedField("10");
        var existing = validCycle("5");
        existing.setId("cycle-1");
        existing.setCreatedBy("owner-1");
        when(cycleMapper.selectById("cycle-1")).thenReturn(existing);

        var update = new PlantingCycle();
        update.setCropId("crop-1");
        update.setRemark("updated");
        var result = service.updateCycle("cycle-1", update, "owner-1");

        assertEquals("updated", result.getRemark());
        verify(cycleMapper).updateById(existing);
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


    @Test
    void rejectsInvalidPlantingDates() {
        prepareOwnedField("10");
        prepareActiveCrop();
        var cycle = validCycle("5");
        cycle.setExpectedHarvestDate(cycle.getPlantingDate().minusDays(1));

        var error = assertThrows(BusinessException.class,
                () -> service.createCycle(cycle, "owner-1"));

        assertEquals(ErrorCode.PLANTING_DATE_INVALID.getCode(), error.getCode());
        verify(cycleMapper, never()).insert(any(PlantingCycle.class));
    }

    @Test
    void rejectsFuturePlantingDateAndInvalidCompletionDate() {
        prepareOwnedField("10");
        prepareActiveCrop();
        var cycle = validCycle("5");
        cycle.setPlantingDate(LocalDate.now().plusDays(1));
        cycle.setExpectedHarvestDate(LocalDate.now().plusDays(30));
        var futureError = assertThrows(BusinessException.class,
                () -> service.createCycle(cycle, "owner-1"));
        assertEquals(ErrorCode.PLANTING_DATE_INVALID.getCode(), futureError.getCode());

        var existing = validCycle("5");
        existing.setId("cycle-1");
        existing.setCreatedBy("owner-1");
        when(cycleMapper.selectById("cycle-1")).thenReturn(existing);
        var update = new PlantingCycle();
        update.setStatus("completed");
        update.setActualHarvestDate(existing.getPlantingDate().minusDays(1));
        var completionError = assertThrows(BusinessException.class,
                () -> service.updateCycle("cycle-1", update, "owner-1"));
        assertEquals(ErrorCode.PLANTING_DATE_INVALID.getCode(), completionError.getCode());
    }

    @Test
    void rejectsPlantingAreaBeyondRemainingFieldCapacity() {
        prepareOwnedField("10");
        prepareActiveCrop();
        var occupied = validCycle("6");
        occupied.setStatus("pending_harvest");
        when(cycleMapper.selectList(any())).thenReturn(List.of(occupied));

        var error = assertThrows(BusinessException.class,
                () -> service.createCycle(validCycle("5"), "owner-1"));

        assertEquals(ErrorCode.PLANTING_AREA_EXCEEDS_FIELD.getCode(), error.getCode());
        verify(cycleMapper, never()).insert(any(PlantingCycle.class));
    }

    @Test
    void requiresActualHarvestDateAndPreventsCompletedCycleFromReopening() {
        var cycle = validCycle("5");
        cycle.setId("cycle-1");
        cycle.setCreatedBy("owner-1");
        when(cycleMapper.selectById("cycle-1")).thenReturn(cycle);
        var complete = new PlantingCycle();
        complete.setStatus("completed");
        var missingDate = assertThrows(BusinessException.class,
                () -> service.updateCycle("cycle-1", complete, "owner-1"));
        assertEquals(ErrorCode.PLANTING_COMPLETION_INVALID.getCode(), missingDate.getCode());

        cycle.setStatus("completed");
        cycle.setActualHarvestDate(LocalDate.now());
        var reopen = new PlantingCycle();
        reopen.setStatus("active");
        var reopenError = assertThrows(BusinessException.class,
                () -> service.updateCycle("cycle-1", reopen, "owner-1"));
        assertEquals(ErrorCode.CYCLE_ALREADY_COMPLETED.getCode(), reopenError.getCode());
    }

    private void prepareOwnedField(String area) {
        var field = new Field();
        field.setId("field-1");
        field.setFarmId("farm-1");
        field.setAreaMu(new BigDecimal(area));
        var farm = new Farm();
        farm.setId("farm-1");
        farm.setOwnerId("owner-1");
        farm.setStatus("active");
        when(fieldMapper.selectById("field-1")).thenReturn(field);
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
    }

    private void prepareActiveCrop() {
        var crop = new Crop();
        crop.setId("crop-1");
        crop.setStatus("active");
        when(cropMapper.selectById("crop-1")).thenReturn(crop);
    }

    private PlantingCycle validCycle(String area) {
        var cycle = new PlantingCycle();
        cycle.setFieldId("field-1");
        cycle.setCropId("crop-1");
        cycle.setPlantingDate(LocalDate.now());
        cycle.setExpectedHarvestDate(LocalDate.now().plusDays(90));
        cycle.setAreaMu(new BigDecimal(area));
        cycle.setGrowthStage("seedling");
        cycle.setStatus("active");
        return cycle;
    }

    private PlantingCycle ownedCycle() {
        var cycle = new PlantingCycle();
        cycle.setId("cycle-1");
        cycle.setCreatedBy("user-1");
        return cycle;
    }
}
