package com.yunong.module.farm;

import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {

    @Mock FarmMapper farmMapper;
    @Mock FieldMapper fieldMapper;

    private FarmService service;

    @BeforeEach
    void setUp() {
        service = new FarmService(farmMapper, fieldMapper);
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
    void updatesAndDeletesOwnedField() {
        var farm = farm("farm-1", "owner-1");
        var field = new Field();
        field.setId("field-1");
        field.setFarmId("farm-1");
        field.setName("旧地块");
        when(farmMapper.selectById("farm-1")).thenReturn(farm);
        when(fieldMapper.selectById("field-1")).thenReturn(field);

        var update = new Field();
        update.setName("新地块");
        service.updateField("farm-1", "field-1", update, "owner-1");
        service.deleteField("farm-1", "field-1", "owner-1");

        assertEquals("新地块", field.getName());
        verify(fieldMapper).updateById(field);
        verify(fieldMapper).deleteById("field-1");
    }

    private Farm farm(String id, String ownerId) {
        var farm = new Farm();
        farm.setId(id);
        farm.setOwnerId(ownerId);
        return farm;
    }
}
