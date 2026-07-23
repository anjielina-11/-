package com.yunong.module.farm.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("farms")
public class Farm extends BaseEntity {

    @TableId
    private String id;
    private String ownerId;
    private String name;
    private String address;
    private java.math.BigDecimal areaMu;
    private String contact;
    private String remark;
    private String location;   // PostGIS GEOMETRY(Point, 4326) — TODO: PostGIS TypeHandler
}
