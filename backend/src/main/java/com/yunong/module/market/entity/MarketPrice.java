package com.yunong.module.market.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("market_prices")
public class MarketPrice {

    @TableId
    private String id;
    private String cropId;
    private String cropName;
    private BigDecimal price;
    private String unit;
    private String marketName;
    private String category;
    private String source;
    private LocalDate recordedAt;
    private LocalDateTime createdAt;
}
