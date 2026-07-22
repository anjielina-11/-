package com.merchant.ordering.service.impl;

import com.merchant.ordering.dto.DashboardVO;
import com.merchant.ordering.mapper.DashboardMapper;
import com.merchant.ordering.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据大屏 Service 实现
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public DashboardVO getDashboardData() {
        DashboardVO vo = new DashboardVO();

        // 基础统计
        vo.setTodayAmount(dashboardMapper.selectTodayAmount());
        vo.setTodayOrders(dashboardMapper.selectTodayOrders());
        vo.setMonthAmount(dashboardMapper.selectMonthAmount());
        vo.setProductCount(dashboardMapper.selectProductCount());

        // 每小时营业额（补全 0~23 点）
        vo.setHourAmountList(buildHourAmountList());

        // 分类销量排行
        vo.setCategorySalesList(dashboardMapper.selectCategorySalesList().stream()
                .map(m -> {
                    DashboardVO.CategorySales cs = new DashboardVO.CategorySales();
                    cs.setCategoryName((String) m.get("categoryName"));
                    Object qty = m.get("quantity");
                    cs.setQuantity(qty instanceof Long ? ((Long) qty).intValue() : ((BigDecimal) qty).intValue());
                    return cs;
                }).collect(Collectors.toList()));

        // 商品销量 TOP5
        vo.setProductSalesList(dashboardMapper.selectProductSalesList().stream()
                .map(m -> {
                    DashboardVO.ProductSales ps = new DashboardVO.ProductSales();
                    ps.setProductName((String) m.get("productName"));
                    Object qty = m.get("quantity");
                    ps.setQuantity(qty instanceof Long ? ((Long) qty).intValue() : ((BigDecimal) qty).intValue());
                    return ps;
                }).collect(Collectors.toList()));

        // 最新 5 笔订单
        vo.setLatestOrderList(dashboardMapper.selectLatestOrderList().stream()
                .map(m -> {
                    DashboardVO.LatestOrder lo = new DashboardVO.LatestOrder();
                    lo.setOrderNo((String) m.get("order_no"));
                    lo.setTotalAmount((BigDecimal) m.get("total_amount"));
                    Object createTime = m.get("create_time");
                    if (createTime instanceof LocalDateTime) {
                        lo.setCreateTime(((LocalDateTime) createTime).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } else {
                        lo.setCreateTime(String.valueOf(createTime));
                    }
                    lo.setNickname((String) m.get("nickname"));
                    lo.setStatus((Integer) m.get("status"));
                    return lo;
                }).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 构建今日每小时营业额（补全 0~23）
     */
    private List<DashboardVO.HourAmount> buildHourAmountList() {
        List<Map<String, Object>> dbList = dashboardMapper.selectHourAmountList();

        // 转为 Map<hour, amount>
        Map<Integer, BigDecimal> hourMap = dbList.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("hour")).intValue(),
                        m -> (BigDecimal) m.get("amount")
                ));

        List<DashboardVO.HourAmount> result = new ArrayList<>();
        for (int h = 9; h <= 20; h++) {
            DashboardVO.HourAmount ha = new DashboardVO.HourAmount();
            ha.setHour(h);
            ha.setAmount(hourMap.getOrDefault(h, BigDecimal.ZERO));
            result.add(ha);
        }
        return result;
    }
}
