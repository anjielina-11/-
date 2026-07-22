package com.merchant.ordering.mapper;

import com.merchant.ordering.dto.DashboardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据大屏 Mapper — 统计查询
 */
@Mapper
public interface DashboardMapper {

    /** 今日营业额 */
    @Select("SELECT IFNULL(SUM(total_amount),0) FROM orders WHERE status != 4 AND DATE(create_time) = CURDATE()")
    BigDecimal selectTodayAmount();

    /** 今日订单数 */
    @Select("SELECT COUNT(*) FROM orders WHERE DATE(create_time) = CURDATE()")
    Integer selectTodayOrders();

    /** 本月营业额 */
    @Select("SELECT IFNULL(SUM(total_amount),0) FROM orders WHERE status != 4 AND DATE_FORMAT(create_time,'%Y%m') = DATE_FORMAT(CURDATE(),'%Y%m')")
    BigDecimal selectMonthAmount();

    /** 在售商品数 */
    @Select("SELECT COUNT(*) FROM product WHERE status = 1")
    Integer selectProductCount();

    /** 今日每小时营业额 */
    @Select("SELECT HOUR(create_time) AS hour, IFNULL(SUM(total_amount),0) AS amount " +
            "FROM orders WHERE status != 4 AND DATE(create_time) = CURDATE() " +
            "GROUP BY HOUR(create_time) ORDER BY hour")
    List<Map<String, Object>> selectHourAmountList();

    /** 分类销量排行（通过 order_item → product → category 关联） */
    @Select("SELECT c.name AS categoryName, IFNULL(SUM(oi.quantity),0) AS quantity " +
            "FROM order_item oi " +
            "LEFT JOIN product p ON oi.product_id = p.id " +
            "LEFT JOIN category c ON p.category_id = c.id " +
            "GROUP BY c.id, c.name ORDER BY quantity DESC")
    List<Map<String, Object>> selectCategorySalesList();

    /** 商品销量排行 TOP 5 */
    @Select("SELECT oi.product_name AS productName, SUM(oi.quantity) AS quantity " +
            "FROM order_item oi " +
            "GROUP BY oi.product_name ORDER BY quantity DESC LIMIT 5")
    List<Map<String, Object>> selectProductSalesList();

    /** 最新 5 笔订单（含用户昵称） */
    @Select("SELECT o.order_no, o.total_amount, o.create_time, o.status, " +
            "IFNULL(u.nickname, u.username) AS nickname " +
            "FROM orders o LEFT JOIN user u ON o.user_id = u.id " +
            "ORDER BY o.create_time DESC LIMIT 5")
    List<Map<String, Object>> selectLatestOrderList();
}
