-- 数据库初始化数据

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
(1, 1, '麻辣牛肉', '精选牛肉，麻辣鲜香', 58.00, 'https://img0.baidu.com/it/u=4166123889,2287736904&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500', 100, 200, 1, NOW(), NOW()),
(2, 1, '牛百叶', '新鲜牛百叶，口感爽脆', 58.00, 'https://img2.baidu.com/it/u=3384865286,58048331&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=737', 80, 180, 1, NOW(), NOW()),
(3, 2, '番茄锅底', '酸甜可口，营养丰富', 38.00, 'https://img2.baidu.com/it/u=678431157,2941632352&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=501', 50, 150, 1, NOW(), NOW()),
(4, 2, '麻辣锅底', '正宗川味，麻辣过瘾', 48.00, 'https://img1.baidu.com/it/u=4064818027,339059701&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=621', 60, 200, 1, NOW(), NOW()),
(5, 3, '羊肉串', '新疆风味，肉质鲜嫩', 3.00, 'https://img2.baidu.com/it/u=200259127,347038397&fm=253&fmt=auto&app=138&f=JPEG?w=800&h=1199', 200, 500, 1, NOW(), NOW()),
(6, 3, '烤鸡翅', '外焦里嫩，香气扑鼻', 8.00, 'https://img1.baidu.com/it/u=913355184,1841193878&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=1190', 100, 300, 1, NOW(), NOW()),
(7, 4, '白米饭', '粒粒分明，香甜软糯', 3.00, 'https://img0.baidu.com/it/u=822804078,1037075766&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500', 500, 800, 1, NOW(), NOW()),
(8, 4, '蛋炒饭', '鸡蛋金黄，米饭粒粒', 15.00, 'https://img0.baidu.com/it/u=1792023533,4193432130&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=667', 100, 400, 1, NOW(), NOW()),
(9, 5, '可乐', '冰爽解渴，经典口味', 6.00, 'https://img0.baidu.com/it/u=679597788,3404069428&fm=253&fmt=auto?w=608&h=844', 200, 600, 1, NOW(), NOW()),
(10, 5, '雪碧', '清凉透心，畅爽无比', 6.00, 'https://img0.baidu.com/it/u=877956368,345235278&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=594', 200, 550, 1, NOW(), NOW());
