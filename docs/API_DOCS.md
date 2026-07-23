# 农业AI服务 API 接口文档

## 概述

本服务提供农业病害诊断相关的AI能力，包括图片识别、天气查询、知识库检索和智能防治建议生成。

**服务地址**: `http://localhost:8000`

**API版本**: v1

**技术栈**: FastAPI + PyTorch + LangChain + Chroma

---

## 目录

1. [健康检查](#1-健康检查)
2. [病害诊断](#2-病害诊断)
   - [图片诊断](#21-图片诊断)
   - [简单诊断](#22-简单诊断)
   - [完整诊断](#23-完整诊断)
   - [获取病害列表](#24-获取病害列表)
3. [天气查询](#3-天气查询)
   - [按城市查询](#31-按城市查询)
   - [按坐标查询](#32-按坐标查询)
4. [知识库检索](#4-知识库检索)
   - [导入文档](#41-导入文档)
   - [检索文档](#42-检索文档)
5. [业务闭环流程](#5-业务闭环流程)
6. [响应模型说明](#6-响应模型说明)
7. [前端对接指南](#7-前端对接指南)

---

## 1. 健康检查

### 接口地址

`GET /health`

### 功能描述

检查服务运行状态，包括模型加载和向量数据库状态。

### 请求示例

```bash
curl http://localhost:8000/health
```

### 响应示例

```json
{
    "status": "healthy",
    "app_name": "Agricultural AI Service",
    "version": "1.0.0",
    "model_loaded": true,
    "vector_db_ready": true
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| status | string | 服务状态：healthy（正常）、degraded（降级） |
| app_name | string | 应用名称 |
| version | string | 版本号 |
| model_loaded | bool | 模型是否已加载 |
| vector_db_ready | bool | 向量数据库是否就绪 |

---

## 2. 病害诊断

### 2.1 图片诊断

#### 接口地址

`POST /api/v1/diagnosis/image`

#### 功能描述

上传作物叶片图片进行病害诊断，返回病害名称、置信度和综合防治建议。

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| image | File | 是 | 作物叶片图片（支持jpg、jpeg、png格式，最大10MB） |
| crop_info | string | 否 | 作物信息（如：番茄、黄瓜、土豆等），默认"未知作物" |
| weather_info | string | 否 | 天气信息（如：晴朗、25°C、湿度60%），默认"未知天气" |
| city | string | 否 | 城市名称，用于获取实时天气（如：北京、上海） |
| lat | float | 否 | 纬度，与lon配合使用获取实时天气 |
| lon | float | 否 | 经度，与lat配合使用获取实时天气 |

#### 请求示例

```bash
curl -X POST http://localhost:8000/api/v1/diagnosis/image \
  -F "image=@leaf.jpg" \
  -F "crop_info=番茄" \
  -F "city=北京"
```

#### 成功响应（200）

```json
{
    "disease_name": "Tomato_Late_blight",
    "confidence": 0.95,
    "advice": "## 1. 病害分析\n番茄晚疫病是由疫霉菌引起的...\n\n## 2. 农业防治\n及时清除病叶...\n\n## 3. 物理防治\n人工摘除病叶...\n\n## 4. 化学防治\n推荐使用甲霜灵锰锌...\n\n## 5. 注意事项\n当前湿度较高，注意通风降湿...\n\n---\n\n### 参考来源\ntomato_disease.txt",
    "references": [
        {
            "content": "番茄晚疫病防治方法：及时清除病株，加强通风...",
            "source": "tomato_disease.txt",
            "score": 0.85
        }
    ],
    "weather_info": "城市: 北京\n天气: 晴朗\n温度: 28°C\n湿度: 45%\n风速: 3m/s\n降雨量: 0mm"
}
```

#### 人工审核响应（200）

当置信度低于阈值（默认0.6）时返回：

```json
{
    "status": "pending_review",
    "message": "未知病害，已转入人工审核"
}
```

#### 错误响应（400）

```json
{
    "detail": "图片大小超过限制（最大 10MB）"
}
```

### 2.2 简单诊断

#### 接口地址

`POST /api/v1/diagnosis/simple`

#### 功能描述

仅进行病害识别，不生成详细防治建议，返回速度更快。

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| image | File | 是 | 作物叶片图片 |
| crop_info | string | 否 | 作物信息 |

#### 请求示例

```bash
curl -X POST http://localhost:8000/api/v1/diagnosis/simple \
  -F "image=@leaf.jpg" \
  -F "crop_info=番茄"
```

#### 响应示例

```json
{
    "disease_name": "Tomato_Late_blight",
    "confidence": 0.95,
    "description": "番茄晚疫病是由疫霉菌引起的一种毁灭性病害...",
    "severity": "严重"
}
```

### 2.3 完整诊断

#### 接口地址

`POST /api/v1/diagnosis/full`

#### 功能描述

进行病害识别，获取天气信息，生成防治建议，返回完整的诊断结果对象。

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| image | File | 是 | 作物叶片图片 |
| crop_info | string | 否 | 作物信息 |
| city | string | 否 | 城市名称 |
| lat | float | 否 | 纬度 |
| lon | float | 否 | 经度 |

#### 响应示例

```json
{
    "classification": {
        "disease_name": "Tomato_Late_blight",
        "confidence": 0.95,
        "severity": "严重"
    },
    "weather": {
        "city": "北京",
        "temperature": 28.0,
        "humidity": 45,
        "weather": "晴朗",
        "wind_speed": 3.0,
        "rain": 0.0,
        "clouds": 20,
        "is_real": false,
        "note": "当前使用模拟天气数据"
    },
    "advice": {
        "advice": "## 1. 病害分析\n...",
        "references": [...],
        "weather_info": "城市: 北京\n天气: 晴朗..."
    }
}
```

### 2.4 获取病害列表

#### 接口地址

`GET /api/v1/diagnosis/diseases`

#### 功能描述

获取模型支持识别的所有病害名称列表。

#### 请求示例

```bash
curl http://localhost:8000/api/v1/diagnosis/diseases
```

#### 响应示例

```json
{
    "diseases": [
        "Tomato_Early_blight",
        "Tomato_Late_blight",
        "Tomato_Leaf_Mold",
        "Tomato_Septoria_leaf_spot",
        "Tomato_Spider_mites",
        "Tomato_Target_Spot",
        "Tomato_Yellow_Leaf_Curl_Virus",
        "Tomato_Mosaic_Virus",
        "Tomato_Healthy",
        "Potato_Early_blight"
    ]
}
```

---

## 3. 天气查询

### 3.1 按城市查询

#### 接口地址

`GET /api/v1/weather/city?city={city}`

#### 功能描述

根据城市名称获取天气信息。

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| city | string | 是 | 城市名称（如：北京、上海、广州） |

#### 请求示例

```bash
curl "http://localhost:8000/api/v1/weather/city?city=北京"
```

#### 响应示例

```json
{
    "success": true,
    "weather": {
        "city": "北京",
        "temperature": 28.0,
        "humidity": 45,
        "weather": "晴朗",
        "wind_speed": 3.0,
        "rain": 0.0,
        "clouds": 20,
        "is_real": false,
        "note": "当前使用模拟天气数据，请配置 WEATHER_API_KEY 以获取真实天气"
    }
}
```

### 3.2 按坐标查询

#### 接口地址

`GET /api/v1/weather/coords?lat={lat}&lon={lon}`

#### 功能描述

根据经纬度坐标获取天气信息。

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| lat | float | 是 | 纬度 |
| lon | float | 是 | 经度 |

#### 请求示例

```bash
curl "http://localhost:8000/api/v1/weather/coords?lat=39.9042&lon=116.4074"
```

---

## 4. 知识库检索

### 4.1 导入文档

#### 接口地址

`POST /api/v1/rag/ingest`

#### 功能描述

将指定目录下的文档导入到向量数据库。

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| docs_dir | string | 是 | 文档目录路径 |

#### 请求示例

```bash
curl -X POST http://localhost:8000/api/v1/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"docs_dir": "test_docs"}'
```

#### 响应示例

```json
{
    "success": true,
    "message": "成功导入 15 个文档片段",
    "chunks_count": 15
}
```

### 4.2 检索文档

#### 接口地址

`POST /api/v1/rag/retrieve`

#### 功能描述

根据查询文本检索向量数据库中最相关的文档片段。

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | string | 是 | 查询文本 |
| top_k | int | 否 | 返回数量，默认3 |

#### 请求示例

```bash
curl -X POST http://localhost:8000/api/v1/rag/retrieve \
  -H "Content-Type: application/json" \
  -d '{"query": "番茄晚疫病防治方法", "top_k": 3}'
```

#### 响应示例

```json
{
    "success": true,
    "results": [
        {
            "content": "番茄晚疫病防治方法：及时清除病株，加强通风...",
            "source": "tomato_disease.txt",
            "score": 0.85
        },
        {
            "content": "晚疫病症状识别：叶片出现暗绿色水渍状病斑...",
            "source": "tomato_late_blight.txt",
            "score": 0.78
        }
    ]
}
```

---

## 5. 业务闭环流程

### 完整诊断流程

```
用户上传图片 → 图片识别 → 获取天气 → RAG检索 → 生成防治建议 → 返回结果
     ↓              ↓            ↓            ↓            ↓
  图片验证     ResNet50      天气API     Chroma DB      LLM模型
                                    ↓
                              知识库匹配
```

### 详细步骤

1. **图片上传与验证**
   - 检查文件大小（最大10MB）
   - 验证图片格式（支持jpg、jpeg、png、gif、bmp）
   - 验证图片完整性

2. **病害识别**
   - 使用预训练的ResNet50模型进行分类
   - 返回病害名称和置信度
   - 置信度低于阈值（默认0.6）时触发人工审核

3. **天气获取**
   - 根据城市或坐标获取天气信息
   - 支持真实API和模拟数据两种模式
   - 返回温度、湿度、天气状况等信息

4. **知识库检索**
   - 使用RAG技术检索相关防治文档
   - 支持多查询词检索（防治方法、症状识别、农药选择）
   - 返回相似度最高的3个文档片段

5. **智能建议生成**
   - 结合病害诊断、天气和知识库信息
   - 使用LLM生成综合防治建议
   - 输出格式：病害分析、农业防治、物理防治、化学防治、注意事项

---

## 6. 响应模型说明

### DiseaseClassification（病害分类）

| 字段 | 类型 | 说明 |
|------|------|------|
| disease_name | string | 病害名称 |
| confidence | float | 置信度（0-1） |
| description | string | 病害描述 |
| severity | string | 严重程度：轻微/中等/严重/未知 |

### DiseaseAdvice（防治建议）

| 字段 | 类型 | 说明 |
|------|------|------|
| advice | string | 综合防治建议（Markdown格式） |
| references | array | 参考资料列表 |
| weather_info | string | 天气信息 |

### ReferenceSource（参考来源）

| 字段 | 类型 | 说明 |
|------|------|------|
| content | string | 参考内容片段 |
| source | string | 来源文件 |
| score | float | 相似度分数 |

### WeatherInfo（天气信息）

| 字段 | 类型 | 说明 |
|------|------|------|
| city | string | 城市名称 |
| temperature | float | 温度（摄氏度） |
| humidity | int | 湿度（%） |
| weather | string | 天气状况 |
| wind_speed | float | 风速（m/s） |
| rain | float | 降雨量（mm） |
| is_real | bool | 是否为真实数据 |

---

## 7. 前端对接指南

### 7.1 基础配置

```javascript
const BASE_URL = 'http://localhost:8000';

// 请求配置
const axios = require('axios');
axios.defaults.baseURL = BASE_URL;
axios.defaults.timeout = 60000;
```

### 7.2 图片诊断示例

```javascript
async function diagnoseImage(file, cropInfo, city) {
    const formData = new FormData();
    formData.append('image', file);
    formData.append('crop_info', cropInfo);
    if (city) {
        formData.append('city', city);
    }
    
    try {
        const response = await axios.post('/api/v1/diagnosis/image', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        
        if (response.data.status === 'pending_review') {
            // 处理人工审核情况
            showPendingReview(response.data.message);
        } else {
            // 展示诊断结果
            displayDiagnosisResult(response.data);
        }
    } catch (error) {
        console.error('诊断失败:', error.response?.data?.detail || error.message);
    }
}
```

### 7.3 天气查询示例

```javascript
async function getWeather(city) {
    try {
        const response = await axios.get('/api/v1/weather/city', {
            params: { city }
        });
        return response.data.weather;
    } catch (error) {
        console.error('获取天气失败:', error.message);
        return null;
    }
}
```

### 7.4 前端展示建议

#### 诊断结果页面布局

```
┌─────────────────────────────────────┐
│          诊断结果                    │
├─────────────────────────────────────┤
│  [图片预览]    [病害名称]            │
│               置信度: 95% [严重]    │
├─────────────────────────────────────┤
│  🌤️ 天气信息                        │
│  北京 | 晴朗 | 28°C | 湿度 45%     │
├─────────────────────────────────────┤
│  📝 防治建议                        │
│  ┌──────────────────────────────┐  │
│  │ 1. 病害分析                   │  │
│  │    番茄晚疫病是由疫霉菌...     │  │
│  ├──────────────────────────────┤  │
│  │ 2. 农业防治                   │  │
│  │    • 及时清除病叶             │  │
│  │    • 加强通风透光             │  │
│  ├──────────────────────────────┤  │
│  │ 3. 物理防治                   │  │
│  │    • 人工摘除病叶             │  │
│  ├──────────────────────────────┤  │
│  │ 4. 化学防治                   │  │
│  │    • 推荐使用甲霜灵锰锌       │  │
│  ├──────────────────────────────┤  │
│  │ 5. 注意事项                   │  │
│  │    • 当前湿度较高...           │  │
│  └──────────────────────────────┘  │
├─────────────────────────────────────┤
│  📚 参考资料                        │
│  • tomato_disease.txt (相似度: 0.85)│
└─────────────────────────────────────┘
```

#### 置信度展示规则

| 置信度范围 | 颜色 | 状态 | 建议 |
|-----------|------|------|------|
| ≥ 0.9 | 绿色 | 高置信 | 直接展示结果 |
| 0.7 - 0.9 | 黄色 | 中置信 | 提示用户确认 |
| 0.6 - 0.7 | 橙色 | 低置信 | 建议人工确认 |
| < 0.6 | 红色 | 未知 | 转入人工审核 |

#### 严重程度展示

| 严重程度 | 图标 | 颜色 |
|----------|------|------|
| 严重 | 🔴 | 红色 |
| 中等 | 🟡 | 黄色 |
| 轻微 | 🟢 | 绿色 |

---

## 8. 环境配置

### 配置文件（.env）

```env
# 基本配置
APP_NAME=Agricultural AI Service
APP_VERSION=1.0.0
DEBUG=False
HOST=0.0.0.0
PORT=8000

# 图片配置
MAX_IMAGE_SIZE=10485760
ALLOWED_IMAGE_EXTENSIONS=jpg,jpeg,png,gif,bmp

# 模型配置
MODEL_PATH=best_model.pth
CLASS_TO_IDX_PATH=class_to_idx.pth
NUM_CLASSES=10
CONFIDENCE_THRESHOLD=0.6

# RAG配置
RAG_VECTOR_DB_PATH=chroma_db
RAG_EMBEDDING_MODEL=all-MiniLM-L6-v2
RAG_CHUNK_SIZE=512
RAG_CHUNK_OVERLAP=64
RAG_TOP_K=3

# LLM配置（可选）
LLM_API_KEY=your_api_key
LLM_API_BASE=https://api.openai.com/v1
LLM_MODEL_NAME=gpt-3.5-turbo

# 天气API配置（可选）
WEATHER_API_KEY=your_weather_api_key
WEATHER_API_BASE=https://api.openweathermap.org/data/2.5
```

### Docker部署

```bash
# 构建镜像
docker build -t agricultural-ai-service .

# 运行容器
docker run -d -p 8000:8000 agricultural-ai-service

# 或者使用 docker-compose
docker-compose up -d
```

---

## 9. 错误码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误（图片格式错误、大小超限等） |
| 500 | 服务器内部错误 |

---

## 10. Swagger文档

服务启动后，可访问以下地址查看交互式API文档：

- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`