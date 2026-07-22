package com.merchant.ordering.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.merchant.ordering.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
}
