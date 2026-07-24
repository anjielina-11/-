# 数据管道

定时采集天气、市场价格和农业通报数据。

## 结构

```
data-pipeline/
├── collect_data.py   ← 主采集脚本（天气+市场价格）
└── README.md
```

## 使用方式

### 采集天气数据

```bash
# 使用模拟数据
python collect_data.py --type weather

# 使用真实 API（需配置 OpenWeatherMap API Key）
python collect_data.py --type weather --api-key YOUR_API_KEY
```

### 采集市场价格

```bash
python collect_data.py --type market
```

### 采集全部

```bash
python collect_data.py --type all --output data_$(date +%Y%m%d).json
```

## 定时任务配置 (crontab)

```bash
# 每小时采集天气
0 * * * * cd /path/to/data-pipeline && python collect_data.py --type weather

# 每天8:00采集市场价格
0 8 * * * cd /path/to/data-pipeline && python collect_data.py --type market
```

## 数据格式

### 天气数据
```json
{
  "city": "昆明",
  "temperature": 24.5,
  "humidity": 65,
  "weather": "多云",
  "wind_speed": 3.2,
  "pressure": 1013.5,
  "recorded_at": "2026-07-24T08:00:00",
  "is_real": false
}
```

### 市场价格
```json
{
  "crop_name": "水稻",
  "prices": [
    {"date": "2026-07-01", "price": 3.45, "unit": "元/公斤"}
  ]
}
```
