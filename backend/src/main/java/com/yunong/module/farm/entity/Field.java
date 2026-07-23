package com.yunong.module.farm.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fields")
public class Field extends BaseEntity {

    @TableId
    private String id;
    private String farmId;
    private String name;
    private java.math.BigDecimal areaMu;
    private String soilType;
    private String location;   // PostGIS GEOMETRY(Point, 4326) — TODO: PostGIS TypeHandler
    private String boundary;   // PostGIS GEOMETRY(Polygon, 4326) — TODO: PostGIS TypeHandler
    private String remark;
}
