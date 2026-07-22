package com.merchant.ordering.service;

import com.merchant.ordering.dto.DashboardVO;

/**
 * 数据大屏 Service 接口
 */
public interface DashboardService {

    /**
     * 获取大屏数据
     */
    DashboardVO getDashboardData();
}
