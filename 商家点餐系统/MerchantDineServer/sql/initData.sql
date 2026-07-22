-- 数据库初始化数据

USE my_app;

-- 先清空已有数据
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `order_item`;
TRUNCATE TABLE `orders`;
TRUNCATE TABLE `product`;
TRUNCATE TABLE `category`;
TRUNCATE TABLE `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- 用户表
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `role`, `status`, `create_time`, `update_time`) VALUES
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '13800138000', 1, 1, NOW(), NOW()),
(2, 'user', 'e10adc3949ba59abbe56e057f20f883e', '普通用户', '13800138001', 0, 1, NOW(), NOW());

-- 分类表
INSERT INTO `category` (`id`, `name`, `sort_order`, `status`, `create_time`, `update_time`) VALUES
(1, '热菜', 1, 1, NOW(), NOW()),
(2, '火锅', 2, 1, NOW(), NOW()),
(3, '烧烤', 3, 1, NOW(), NOW()),
(4, '主食', 4, 1, NOW(), NOW()),
(5, '饮品', 5, 1, NOW(), NOW());

-- 商品表
INSERT INTO `product` (`id`, `category_id`, `name`, `description`, `price`, `image`, `stock`, `sales`, `status`, `create_time`, `update_time`) VALUES
(1, 1, '麻辣牛肉', '精选牛肉，麻辣鲜香', 58.00, NULL, 100, 200, 1, NOW(), NOW()),
(2, 1, '牛百叶', '新鲜牛百叶，口感爽脆', 58.00, NULL, 80, 180, 1, NOW(), NOW()),
(3, 2, '番茄锅底', '酸甜可口，营养丰富', 38.00, NULL, 50, 150, 1, NOW(), NOW()),
(4, 2, '麻辣锅底', '正宗川味，麻辣过瘾', 48.00, NULL, 60, 200, 1, NOW(), NOW()),
(5, 3, '羊肉串', '新疆风味，肉质鲜嫩', 3.00, NULL, 200, 500, 1, NOW(), NOW()),
(6, 3, '烤鸡翅', '外焦里嫩，香气扑鼻', 8.00, NULL, 100, 300, 1, NOW(), NOW()),
(7, 4, '白米饭', '粒粒分明，香甜软糯', 3.00, NULL, 500, 800, 1, NOW(), NOW()),
(8, 4, '蛋炒饭', '鸡蛋金黄，米饭粒粒', 15.00, NULL, 100, 400, 1, NOW(), NOW()),
(9, 5, '可乐', '冰爽解渴，经典口味', 6.00, NULL, 200, 600, 1, NOW(), NOW()),
(10, 5, '雪碧', '清凉透心，畅爽无比', 6.00, NULL, 200, 550, 1, NOW(), NOW());
