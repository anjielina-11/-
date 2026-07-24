package com.yunong.module.farm.dto;

import com.yunong.module.farm.entity.Field;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class FieldDTO {

    private String id;
    private String name;
    private BigDecimal area;
    private String soilType;
    private String remark;
    private String cropType;

    public static FieldDTO from(Field field, String cropType) {
        return new FieldDTO(
                field.getId(),
                field.getName(),
                field.getAreaMu(),
                field.getSoilType(),
                field.getRemark(),
                cropType
        );
    }
}
