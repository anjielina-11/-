-- 为首次使用和课堂演示提供常见作物字典；自定义品种仍可通过 POST /api/v1/crops 新增。
INSERT INTO crops (id, name, category, variety, growth_days, optimal_temp_min, optimal_temp_max, description)
SELECT seed.id::uuid, seed.name, seed.category, seed.variety, seed.growth_days,
       seed.optimal_temp_min, seed.optimal_temp_max, seed.description
FROM (VALUES
    ('10000000-0000-0000-0000-000000000001', '水稻',   '粮食作物', '滇粳优8号',  145, 20.0, 30.0, '适合云南地区种植的粳稻品种'),
    ('10000000-0000-0000-0000-000000000002', '玉米',   '粮食作物', '云瑞88',     120, 18.0, 30.0, '常见高产玉米品种'),
    ('10000000-0000-0000-0000-000000000003', '小麦',   '粮食作物', '云麦56',     180, 12.0, 24.0, '适合冬春季种植的小麦品种'),
    ('10000000-0000-0000-0000-000000000004', '马铃薯', '粮食作物', '合作88',     110, 15.0, 22.0, '常见马铃薯品种'),
    ('10000000-0000-0000-0000-000000000005', '大豆',   '粮食作物', '云大豆2号',  115, 20.0, 28.0, '适合温暖地区种植的大豆品种'),
    ('10000000-0000-0000-0000-000000000006', '油菜',   '经济作物', '云油杂15号', 210, 10.0, 25.0, '常见油料作物品种'),
    ('10000000-0000-0000-0000-000000000007', '番茄',   '蔬菜',     '云番茄1号',  100, 18.0, 28.0, '适合设施或露地栽培的番茄品种'),
    ('10000000-0000-0000-0000-000000000008', '辣椒',   '蔬菜',     '云椒12号',   120, 20.0, 30.0, '常见鲜食辣椒品种'),
    ('10000000-0000-0000-0000-000000000009', '黄瓜',   '蔬菜',     '云黄瓜3号',   75,  18.0, 30.0, '生长周期较短的常见蔬菜品种')
) AS seed(id, name, category, variety, growth_days, optimal_temp_min, optimal_temp_max, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM crops existing
    WHERE existing.name = seed.name
      AND COALESCE(existing.variety, '') = COALESCE(seed.variety, '')
      AND existing.deleted = 0
);
