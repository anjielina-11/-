-- =====================================================
-- 商家点餐系统 - 数据库与表结构定义
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS my_app
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE my_app;

-- 关闭外键检查，避免删表时因外键约束报错
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 用户表
-- =====================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`            BIGINT          NOT NULL    AUTO_INCREMENT  COMMENT '主键ID',
    `username`      VARCHAR(50)     NOT NULL                    COMMENT '用户名',
    `password`      VARCHAR(255)    NOT NULL                    COMMENT '密码',
    `nickname`      VARCHAR(50)     DEFAULT NULL                COMMENT '昵称',
    `phone`         VARCHAR(20)     DEFAULT NULL                COMMENT '手机号',
    `email`         VARCHAR(100)    DEFAULT NULL                COMMENT '邮箱',
    `avatar`        VARCHAR(255)    DEFAULT NULL                COMMENT '头像地址',
    `status`        TINYINT         NOT NULL    DEFAULT 1       COMMENT '状态: 0=禁用, 1=正常',
    `create_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- =====================================================
-- 商品分类表
-- =====================================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`            BIGINT          NOT NULL    AUTO_INCREMENT  COMMENT '主键ID',
    `name`          VARCHAR(50)     NOT NULL                    COMMENT '分类名称',
    `type`          TINYINT         NOT NULL    DEFAULT 1       COMMENT '类型: 1=菜品分类, 2=套餐分类',
    `sort`          INT             NOT NULL    DEFAULT 0       COMMENT '排序值（越小越靠前）',
    `status`        TINYINT         NOT NULL    DEFAULT 1       COMMENT '状态: 0=禁用, 1=正常',
    `create_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类表';

-- =====================================================
-- 商品表
-- =====================================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`            BIGINT          NOT NULL    AUTO_INCREMENT  COMMENT '主键ID',
    `name`          VARCHAR(100)    NOT NULL                    COMMENT '商品名称',
    `category_id`   BIGINT          NOT NULL                    COMMENT '所属分类ID',
    `price`         DECIMAL(10,2)   NOT NULL                    COMMENT '价格',
    `image`         VARCHAR(255)    DEFAULT NULL                COMMENT '图片地址',
    `description`   TEXT            DEFAULT NULL                COMMENT '商品描述',
    `status`        TINYINT         NOT NULL    DEFAULT 1       COMMENT '状态: 0=停售, 1=在售',
    `create_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品表';

-- =====================================================
-- 订单表
-- =====================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`            BIGINT          NOT NULL    AUTO_INCREMENT  COMMENT '主键ID',
    `order_no`      VARCHAR(50)     NOT NULL                    COMMENT '订单编号',
    `user_id`       BIGINT          NOT NULL                    COMMENT '下单用户ID',
    `total_amount`  DECIMAL(10,2)   NOT NULL                    COMMENT '订单总金额',
    `status`        TINYINT         NOT NULL    DEFAULT 1       COMMENT '订单状态: 1=待支付, 2=已支付, 3=已完成, 4=已取消',
    `pay_status`    TINYINT         NOT NULL    DEFAULT 0       COMMENT '支付状态: 0=未支付, 1=已支付',
    `pay_method`    TINYINT         DEFAULT NULL                COMMENT '支付方式: 1=微信支付, 2=支付宝',
    `remark`        VARCHAR(255)    DEFAULT NULL                COMMENT '备注',
    `create_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';

-- =====================================================
-- 订单明细表
-- =====================================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`            BIGINT          NOT NULL    AUTO_INCREMENT  COMMENT '主键ID',
    `order_id`      BIGINT          NOT NULL                    COMMENT '订单ID',
    `product_id`    BIGINT          NOT NULL                    COMMENT '商品ID',
    `product_name`  VARCHAR(100)    NOT NULL                    COMMENT '商品名称（快照）',
    `product_price` DECIMAL(10,2)   NOT NULL                    COMMENT '商品单价（快照）',
    `quantity`      INT             NOT NULL                    COMMENT '购买数量',
    `amount`        DECIMAL(10,2)   NOT NULL                    COMMENT '小计金额',
    `create_time`   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;
