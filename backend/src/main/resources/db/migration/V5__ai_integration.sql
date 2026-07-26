-- AI Runtime schema, canonical model metadata, and managed RAG knowledge.

ALTER TABLE model_versions
    ADD COLUMN IF NOT EXISTS class_mapping_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS num_classes INT;

ALTER TABLE model_versions ALTER COLUMN status SET DEFAULT 'training';
ALTER TABLE knowledge_documents ALTER COLUMN status SET DEFAULT 'draft';

UPDATE model_versions SET status = 'deployed' WHERE status = 'active';
UPDATE model_versions SET status = 'deprecated' WHERE status = 'inactive';
UPDATE model_versions SET status = 'training' WHERE status = 'draft';

UPDATE model_versions SET status = 'deprecated'
WHERE model_name = '云农病害识别 ResNet50'
  AND version <> 'v1.0.0'
  AND status = 'deployed';

INSERT INTO model_versions (
    id, model_name, model_type, version, accuracy, precision_val, recall_val, f1_score,
    model_path, class_mapping_path, num_classes, config_json, status, deployed_at, description, created_at, updated_at
) VALUES (
    '20000000-0000-0000-0000-000000000001', '云农病害识别 ResNet50', 'classification', 'v1.0.0',
    0.8387, 0.7957, 0.8387, 0.8043,
    '/app/best_model.pth', '/app/class_to_idx.pth', 18,
    '{"confidence_threshold":0.6,"framework":"PyTorch","architecture":"ResNet50"}'::jsonb,
    'deployed', now(), '18 类农作物病虫害识别模型，与 AI Runtime 默认模型一致', now(), now()
)
ON CONFLICT (model_name, version) DO UPDATE SET
    accuracy = EXCLUDED.accuracy,
    precision_val = EXCLUDED.precision_val,
    recall_val = EXCLUDED.recall_val,
    f1_score = EXCLUDED.f1_score,
    model_path = EXCLUDED.model_path,
    class_mapping_path = EXCLUDED.class_mapping_path,
    num_classes = EXCLUDED.num_classes,
    config_json = EXCLUDED.config_json,
    status = EXCLUDED.status,
    deployed_at = EXCLUDED.deployed_at,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO knowledge_documents (
    id, title, content, category, tags, version, status, created_at, updated_at, deleted
) VALUES (
    '10000000-0000-0000-0000-000000000001', '水稻主要病害防治规范',
$knowledge$
# 水稻主要病害防治规范

## 稻瘟病 (Rice Blast)

### 症状识别
稻瘟病是由稻瘟病菌（*Magnaporthe oryzae*）引起的水稻重要病害。可分为苗瘟、叶瘟、节瘟、穗颈瘟和谷粒瘟。
- **叶瘟**：叶片出现暗绿色水渍状斑点，后扩大为梭形或纺锤形病斑，中间灰白色，边缘褐色
- **穗颈瘟**：穗颈节变褐坏死，造成白穗，对产量影响最大
- **节瘟**：茎节变黑褐色，易折断

### 发病条件
- 温度 24-28°C，相对湿度 > 90%
- 偏施氮肥、种植密度过大
- 云南山区雾多露重，早晚温差大，易发稻瘟病

### 防治方法
1. **农业防治**：选用抗病品种（如滇优系列）；合理施肥，氮磷钾配合，避免偏施氮肥；合理密植
2. **物理防治**：清除病残体，减少菌源
3. **化学防治**：
   - 预防：分蘖盛期喷施三环唑（75%可湿性粉剂 25-30g/亩）
   - 治疗：抽穗期喷施稻瘟灵（40%乳油 80-100ml/亩）或春雷霉素
   - 穗颈瘟关键防治期：破口期和齐穗期各喷药一次
4. **注意事项**：雨后及时排水，降低田间湿度；发病田块收获后深翻

### 云南地区特别提示
云南水稻产区以滇中、滇南为主，稻瘟病常年发生。7-8月雨季为发病高峰期，应提前做好预防。

---

## 水稻纹枯病 (Rice Sheath Blight)

### 症状识别
由立枯丝核菌（*Rhizoctonia solani*）引起。主要危害叶鞘和叶片。
- 近水面叶鞘出现水渍状暗绿色斑点，后扩大为椭圆形灰白色斑
- 严重时叶片枯死，茎秆腐烂倒伏
- 湿度大时病部可见白色菌丝和褐色菌核

### 发病条件
- 高温高湿（28-32°C，RH>95%）
- 偏施氮肥、长期深灌水
- 田间郁闭、通风不良

### 防治方法
1. **农业防治**：合理施肥，增施钾肥；浅水灌溉，适时晒田；合理密植
2. **化学防治**：
   - 井冈霉素（5%水剂 150-200ml/亩）喷雾
   - 苯醚甲环唑+丙环唑（爱苗）20-30ml/亩
   - 防治适期：分蘖末期至孕穗期

---

## 水稻秆蝇 (Rice Stem Maggot)

### 症状识别
水稻秆蝇幼虫钻入稻茎为害，造成枯心苗。
- 苗期受害：心叶枯黄，易拔出，基部有虫粪
- 分蘖期受害：主茎枯死，刺激分蘖增多但无效分蘖多

### 防治方法
1. **农业防治**：清除田边杂草；合理轮作
2. **化学防治**：
   - 苗期至分蘖期：毒死蜱（48%乳油 80-100ml/亩）或阿维菌素喷雾
   - 结合稻飞虱防治同时进行
$knowledge$,
    'disease', '["水稻","稻瘟病","纹枯病","潜叶蝇"]'::jsonb, 1, 'published', now(), now(), 0
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    category = EXCLUDED.category,
    tags = EXCLUDED.tags,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO knowledge_documents (
    id, title, content, category, tags, version, status, created_at, updated_at, deleted
) VALUES (
    '10000000-0000-0000-0000-000000000002', '番茄主要病害防治规范',
$knowledge$
# 番茄主要病害防治规范

## 番茄晚疫病 (Tomato Late Blight)

### 症状识别
由致病疫霉菌（*Phytophthora infestans*）引起，是番茄生产中最具毁灭性的病害之一。
- **叶片**：出现暗绿色水渍状不规则病斑，湿度大时叶背可见白色霉层
- **茎秆**：黑褐色长条斑，易折断
- **果实**：暗绿色至棕褐色不规则斑，潮湿时腐烂
- 低温高湿条件下 3-5 天可全田毁灭

### 发病条件
- 温度 18-22°C，相对湿度 > 90%
- 连续阴雨天气、雾天
- 云南秋冬番茄种植季（9-11月）多发

### 防治方法
1. **农业防治**：
   - 选用抗病品种
   - 高畦栽培，地膜覆盖，降低田间湿度
   - 合理灌水，避免大水漫灌
   - 及时整枝打杈，保持通风透光
2. **物理防治**：发现病株及时拔除并深埋；收获后彻底清洁田园
3. **化学防治**：
   - 预防为主：代森锰锌（80%可湿性粉剂 150-200g/亩）7-10天一次
   - 发病初期：甲霜灵锰锌（58%可湿性粉剂 100-120g/亩）或霜脲·锰锌
   - 交替用药：嘧菌酯、烯酰吗啉等
   - 严重时每 5-7 天施药一次，连续 2-3 次
4. **注意事项**：
   - 注意药剂交替使用，避免病菌产生抗药性
   - 安全间隔期：甲霜灵锰锌 7 天，嘧菌酯 5 天

### 云南地区特别提示
云南元谋、建水等冬春番茄产区，12月-2月低温高湿天气多发晚疫病，应注意天气预报提前预防。

---

## 番茄灰霉病 (Tomato Gray Mold)

### 症状识别
由灰葡萄孢菌（*Botrytis cinerea*）引起。
- **叶片**：从叶缘开始出现 V 字形褐色病斑
- **花器**：花瓣和花萼先发病，灰褐色腐烂
- **果实**：水渍状腐烂，表面密生灰褐色霉层
- 病部可见灰色霉层（分生孢子）

### 发病条件
- 温度 18-23°C，相对湿度 > 90%
- 大棚种植通风不良时高发
- 低温寡照天气

### 防治方法
1. **农业防治**：
   - 加强通风降湿（大棚种植关键措施）
   - 及时摘除病叶、病花、病果
   - 地膜覆盖，降低棚内湿度
2. **化学防治**：
   - 腐霉利（50%可湿性粉剂 50-60g/亩）
   - 嘧霉胺（40%悬浮剂 60-80ml/亩）
   - 啶酰菌胺等新型药剂
   - 大棚内优先使用烟雾剂或粉尘剂
$knowledge$,
    'disease', '["番茄","灰霉病","晚疫病"]'::jsonb, 1, 'published', now(), now(), 0
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    category = EXCLUDED.category,
    tags = EXCLUDED.tags,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO knowledge_documents (
    id, title, content, category, tags, version, status, created_at, updated_at, deleted
) VALUES (
    '10000000-0000-0000-0000-000000000003', '玉米主要病虫害防治规范',
$knowledge$
# 玉米主要病虫害防治规范

## 玉米大斑病 (Corn Leaf Blight)

### 症状识别
由大斑病菌（*Exserohilum turcicum*）引起。
- **叶片**：出现大型梭形或长条形病斑，长 5-10cm，宽 1-2cm
- 初期为水渍状灰绿色斑点，后变为灰褐色至枯黄色
- 严重时叶片枯死，影响光合作用
- 从下部叶片开始发病，逐渐向上蔓延

### 发病条件
- 温度 18-28°C，相对湿度 > 90%
- 玉米抽穗至灌浆期最易感病
- 云南山区玉米多 6-8 月进入抽穗期，正值雨季

### 防治方法
1. **农业防治**：
   - 选用抗病杂交品种
   - 合理施肥，增施磷钾肥
   - 适时早播，避开病害高发期
   - 收获后清除病残体
2. **化学防治**：
   - 大喇叭口期开始预防：苯醚甲环唑（10%水分散粒剂 30-50g/亩）
   - 发病初期：吡唑醚菌酯+戊唑醇
   - 严重时 7-10 天一次，连续 2-3 次

---

## 玉米螟 (Corn Borer)

### 症状识别
亚洲玉米螟（*Ostrinia furnacalis*）幼虫为害。
- **心叶期**：幼虫钻入心叶取食，展开后出现排孔（"花叶"状）
- **抽穗期**：钻入雄穗和茎秆，造成折断
- **穗期**：钻入雌穗取食籽粒，影响产量和品质
- 茎秆上可见蛀孔和虫粪

### 防治方法
1. **农业防治**：
   - 越冬期处理秸秆，减少虫源
   - 种植抗虫品种（Bt 转基因玉米效果显著）
2. **生物防治**：
   - 释放赤眼蜂（Trichogramma），每亩 1-2 万头，分 2-3 次
   - 使用 Bt 制剂（苏云金杆菌）喷雾
3. **化学防治**：
   - 防治适期：心叶末期（大喇叭口期）
   - 辛硫磷颗粒剂丢心（3%颗粒剂 1-1.5kg/亩）
   - 高效氯氟氰菊酯（2.5%乳油 30-40ml/亩）喷雾

---

## 玉米黑粉病 (Corn Smut)

### 症状识别
由玉米黑粉菌（*Ustilago maydis*）引起。
- 在茎秆、叶片、雄穗和雌穗上形成各种大小的菌瘿（瘤状物）
- 初期为白色有光泽，后变为灰黑色，破裂后散出大量黑色粉末（冬孢子）
- 果穗受害损失最大

### 防治方法
1. **农业防治**：
   - 收获后深耕翻埋菌源
   - 合理轮作（2-3 年）
   - 减少机械损伤（伤口是病菌入侵途径）
2. **化学防治**：
   - 种子处理：戊唑醇悬浮种衣剂拌种
   - 田间早期发现菌瘿及时摘除深埋
$knowledge$,
    'disease', '["玉米","螟虫","叶斑病","黑粉病"]'::jsonb, 1, 'published', now(), now(), 0
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    category = EXCLUDED.category,
    tags = EXCLUDED.tags,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO knowledge_documents (
    id, title, content, category, tags, version, status, created_at, updated_at, deleted
) VALUES (
    '10000000-0000-0000-0000-000000000004', '马铃薯和小麦主要病害防治规范',
$knowledge$
# 马铃薯和小麦主要病害防治规范

## 马铃薯晚疫病 (Potato Late Blight)

### 症状识别
由致病疫霉菌（*Phytophthora infestans*）引起，历史上曾导致爱尔兰大饥荒。
- **叶片**：出现暗绿色水渍状斑点，湿度大时叶背有白色霉层，迅速扩展导致叶片枯死
- **茎秆**：黑褐色条斑，易折断
- **块茎**：褐色至紫色不规则凹陷斑，切开可见红褐色干腐

### 发病条件
- 温度 16-22°C，相对湿度 > 90%
- 连续 48 小时湿度 > 90% 即可能爆发
- 云南昭通、曲靖等马铃薯主产区，7-9 月雨季为发病高峰期

### 防治方法
1. **农业防治**：
   - 选用抗病品种（如合作88号、云薯系列）
   - 高垄栽培，排水良好
   - 合理施肥，增施钾肥
   - 收获前 1-2 周割秧，减少块茎感染
2. **化学防治**：
   - 预防：代森锰锌或霜脲·锰锌 7-10 天一次
   - 发病初期：烯酰吗啉·霜脲氰或氟吡菌胺·霜霉威
   - **关键**：现蕾期至开花期是防治关键窗口期
3. **贮藏防治**：收获后晾干，剔除病薯，贮藏温度 2-4°C

### 云南地区特别提示
云南昭通为全国马铃薯晚疫病重发区，建议采用预警系统（如 CARAH 模型）指导施药时机。

---

## 小麦锈病 (Wheat Rust)

### 症状识别
由锈菌（*Puccinia* spp.）引起，主要为条锈病和叶锈病。
- **条锈病**：鲜黄色夏孢子堆沿叶脉排列成虚线状（"条锈成行"）
- **叶锈病**：橙褐色夏孢子堆散生于叶片，排列不规则
- 严重时全叶布满锈粉，光合作用受阻，籽粒瘪瘦

### 发病条件
- 温度 10-20°C（条锈病偏低温），湿度高
- 云南小麦产区（滇中、滇西北）冬季温暖，条锈菌可越冬
- 春季 3-4 月为流行高峰期

### 防治方法
1. **农业防治**：
   - 选用抗锈病品种
   - 适期播种，避免过早播种
   - 合理施肥，避免偏施氮肥
2. **化学防治**：
   - 防治指标：病叶率 > 5%
   - 三唑酮（20%乳油 40-50ml/亩）
   - 戊唑醇（430g/L 悬浮剂 15-20ml/亩）
   - 吡唑醚菌酯+氟环唑等复配剂
   - 一般 7-10 天一次，视病情 1-3 次

---

## 小麦赤霉病 (Wheat Scab)

### 症状识别
由禾谷镰刀菌（*Fusarium graminearum*）引起。
- **穗部**：开花后小穗变黄褐色至枯白，湿度大时颖壳缝隙有粉红色霉层
- 籽粒皱缩，千粒重下降，品质变劣
- **重要**：病菌产生 DON 毒素，对人畜有毒

### 发病条件
- 小麦抽穗扬花期遇连续阴雨（3 天以上）
- 温度 15-28°C
- 前茬为玉米的田块菌源量大

### 防治方法
1. **农业防治**：
   - 深耕翻埋秸秆，减少菌源
   - 合理轮作（与马铃薯等非寄主作物）
2. **化学防治**（预防为主，见花打药）：
   - **关键时期**：抽穗扬花期（始花至盛花）
   - 戊唑醇·咪鲜胺或氰烯菌酯
   - 如花期遇持续阴雨，5-7 天后再喷一次
   - 多菌灵因抗药性问题，建议不再单独使用
3. **收获后处理**：及时晾晒，水分 < 13% 方可入库
$knowledge$,
    'disease', '["马铃薯","小麦","晚疫病","锈病","赤霉病"]'::jsonb, 1, 'published', now(), now(), 0
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    category = EXCLUDED.category,
    tags = EXCLUDED.tags,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO knowledge_documents (
    id, title, content, category, tags, version, status, created_at, updated_at, deleted
) VALUES (
    '10000000-0000-0000-0000-000000000005', '黄瓜和辣椒主要病害防治规范',
$knowledge$
# 黄瓜和辣椒主要病害防治规范

## 黄瓜霜霉病 (Cucumber Downy Mildew)

### 症状识别
由古巴假霜霉菌（*Pseudoperonospora cubensis*）引起。
- **叶片**：叶面出现多角形黄色斑点（受叶脉限制），后变为黄褐色枯斑
- 湿度大时叶背有灰黑色霉层
- 严重时叶片迅速枯死，俗称"跑马干"
- 从下部叶片开始向上蔓延

### 发病条件
- 温度 16-24°C，相对湿度 > 85%
- 大棚通风不良、叶面结露时间长
- 云南保护地黄瓜冬春季多发

### 防治方法
1. **农业防治**：
   - 选用抗病品种
   - 加强通风换气，降低棚内湿度
   - 地膜覆盖，膜下滴灌
   - 合理密植，及时整枝
2. **化学防治**：
   - 预防：代森锰锌或百菌清，7-10 天一次
   - 发病初期：烯酰吗啉或霜脲·锰锌
   - 氟吡菌胺·霜霉威（银法利）等新型药剂
3. **注意事项**：大棚内优先使用烟雾剂，减少湿度

---

## 黄瓜白粉病 (Cucumber Powdery Mildew)

### 症状识别
由白粉菌（*Sphaerotheca fuliginea*）引起。
- **叶片**：叶面和叶背出现白色粉状斑点，后扩大连片布满全叶
- 严重时叶片变黄枯死，但一般不造成快速死亡
- 植株光合作用下降，果实发育不良

### 发病条件
- 温度 20-30°C，最适 25°C
- 相对湿度 45-75%，干湿交替利于发病
- 大棚内通风不良、光照不足

### 防治方法
1. **农业防治**：
   - 加强通风和光照
   - 增施磷钾肥，提高植株抗性
2. **化学防治**：
   - 硫磺制剂（如 50%硫磺悬浮剂 200-300 倍液）
   - 嘧菌酯或醚菌酯
   - 氟硅唑等三唑类药剂
   - 注意交替用药

---

## 辣椒炭疽病 (Pepper Anthracnose)

### 症状识别
由炭疽菌（*Colletotrichum* spp.）引起。
- **果实**：出现圆形或椭圆形凹陷斑，中央灰白色，边缘暗褐色
- 湿度大时病部出现同心轮纹状排列的黑色小点（分生孢子盘）
- 叶片和茎秆也可受害，产生褐色斑点

### 发病条件
- 温度 25-30°C，多雨高湿
- 果实成熟期最易感病
- 云南辣椒主产区（文山、红河等）7-9 月高温多雨

### 防治方法
1. **农业防治**：
   - 选用抗病品种
   - 合理轮作（与非茄科作物 2-3 年）
   - 高垄栽培，排水良好
   - 及时采摘成熟果实
2. **化学防治**：
   - 发病初期：咪鲜胺或苯醚甲环唑
   - 吡唑醚菌酯+代森联等复配剂
   - 7-10 天一次，连续 2-3 次
$knowledge$,
    'disease', '["黄瓜","辣椒","霜霉病","白粉病","炭疽病"]'::jsonb, 1, 'published', now(), now(), 0
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    category = EXCLUDED.category,
    tags = EXCLUDED.tags,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO knowledge_documents (
    id, title, content, category, tags, version, status, created_at, updated_at, deleted
) VALUES (
    '10000000-0000-0000-0000-000000000006', '柑橘、大豆和棉花主要病虫害防治规范',
$knowledge$
# 柑橘、大豆和棉花主要病虫害防治规范

## 柑橘溃疡病 (Citrus Canker)

### 症状识别
由柑橘黄单胞菌（*Xanthomonas citri* subsp. *citri*）引起，为检疫性病害。
- **叶片**：出现黄色油渍状斑点，后隆起呈海绵状，表面开裂呈火山口状
- 病斑周围有黄色晕圈
- **果实**：出现类似病斑，影响商品价值
- **枝条**：木质化病斑

### 发病条件
- 温度 25-35°C，多雨高湿
- 暴风雨造成伤口利于病菌入侵
- 云南柑橘产区（宾川、华宁等）雨季多发

### 防治方法
1. **检疫**：严禁从病区调运苗木和接穗
2. **农业防治**：
   - 种植无病苗木
   - 合理修剪，保持通风透光
   - 果实套袋
3. **化学防治**：
   - 铜制剂（氢氧化铜、氧化亚铜）保护性喷雾
   - 春梢、夏梢、秋梢萌发期各喷药 1-2 次
   - 暴风雨后及时补喷

---

## 柑橘红蜘蛛 (Citrus Red Spider/Panonychus citri)

### 症状识别
- 成螨和若螨群集叶片背面吸食汁液
- 受害叶片出现密集的灰白色小斑点，严重时全叶灰白
- 导致叶片早落，树势衰弱
- 干旱季节发生严重

### 防治方法
1. **生物防治**：
   - 释放捕食螨（如胡瓜钝绥螨）
   - 保护瓢虫、草蛉等天敌
2. **化学防治**：
   - 防治指标：每叶平均 3-5 头
   - 阿维菌素（1.8%乳油 2000-3000 倍液）
   - 哒螨灵、螺螨酯等杀螨剂
   - 注意轮换用药，避免抗药性

---

## 大豆食心虫 (Soybean Pod Borer)

### 症状识别
大豆食心虫（*Leguminivora glycinivorella*）幼虫钻入豆荚取食豆粒。
- 豆荚表面有微小蛀孔
- 幼虫在荚内取食嫩豆，造成豆粒残缺不全
- 严重时豆荚内充满虫粪，品质严重下降

### 防治方法
1. **农业防治**：
   - 选用抗虫品种
   - 合理轮作（与水稻等非寄主作物）
   - 深翻灭茬，减少越冬虫源
2. **化学防治**：
   - 防治适期：大豆结荚期（成虫产卵盛期）
   - 高效氯氟氰菊酯（2.5%乳油 25-30ml/亩）
   - 毒死蜱（48%乳油 80-100ml/亩）

---

## 棉花黄萎病 (Cotton Verticillium Wilt)

### 症状识别
由大丽轮枝菌（*Verticillium dahliae*）引起，为土传系统性病害。
- **叶片**：叶缘和叶脉间出现黄色斑块，呈"西瓜皮"状花斑
- 叶片由下向上逐渐枯焦脱落
- 茎秆维管束变褐色（纵剖可见）
- 植株矮化，严重时整株枯死

### 发病条件
- 土壤温度 25-28°C 最适发病
- 连作棉田发病重
- 土壤湿度高利于病菌繁殖

### 防治方法
1. **农业防治**：
   - 选用抗黄萎病品种
   - 合理轮作（与水稻、玉米等 3-5 年轮作）
   - 深翻土壤，增施有机肥
2. **化学防治**：
   - 种子处理：咯菌腈悬浮种衣剂拌种
   - 发病初期：恶霉灵或甲基硫菌灵灌根
3. **生物防治**：施用枯草芽孢杆菌等生防制剂
$knowledge$,
    'disease', '["柑橘","大豆","棉花","病虫害"]'::jsonb, 1, 'published', now(), now(), 0
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    content = EXCLUDED.content,
    category = EXCLUDED.category,
    tags = EXCLUDED.tags,
    status = EXCLUDED.status,
    updated_at = now();
