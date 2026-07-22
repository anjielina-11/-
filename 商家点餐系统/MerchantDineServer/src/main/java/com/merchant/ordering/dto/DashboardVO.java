package com.merchant.ordering.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据大屏返回 VO
 */
@Data
public class DashboardVO {

    /** 今日营业额 */
    private BigDecimal todayAmount;

    /** 今日订单数 */
    private Integer todayOrders;

    /** 本月营业额 */
    private BigDecimal monthAmount;

    /** 商品总数 */
    private Integer productCount;

    /** 今日每小时营业额 */
    private List<HourAmount> hourAmountList;

    /** 分类销量排行 */
    private List<CategorySales> categorySalesList;

    /** 商品销量排行 */
    private List<ProductSales> productSalesList;

    /** 最新订单列表 */
    private List<LatestOrder> latestOrderList;

    // ========== 内嵌类 ==========

    @Data
    public static class HourAmount {
        private Integer hour;
        private BigDecimal amount;
    }

    @Data
    public static class CategorySales {
        private String categoryName;
        private Integer quantity;
    }

    @Data
    public static class ProductSales {
        private String productName;
        private Integer quantity;
    }

    @Data
    public static class LatestOrder {
        private String orderNo;
        private BigDecimal totalAmount;
        private String createTime;
        private String nickname;
        private Integer status;
    }
}
