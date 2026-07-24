# 云南特色农业智能诊断与生产管理平台 — 系统设计说明书

> 版本: 1.0 · 日期: 2026-07-24

---

## 一、系统架构

### 1.1 总体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                          客户端层                                    │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  浏览器 (Vue 3 SPA)                   移动端 (H5 适配)         │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
│                           │ HTTPS                                    │
│                           ▼                                          │
├─────────────────────────────────────────────────────────────────────┤
│                          网关层                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │              Nginx (反向代理 + 静态资源)                       │   │
│  │    /          → 前端静态资源 (dist/)                           │   │
│  │    /api/*     → 业务后端 (Spring Boot :8080)                  │   │
│  │    /ai/*      → AI 服务 (FastAPI :8000)                       │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                          应用层                                      │
│  ┌──────────────────┐  ┌──────────────────┐                        │
│  │  业务后端          │  │  AI 服务          │                        │
│  │  Spring Boot 3    │  │  FastAPI          │                        │
│  │  Java 21          │◄─┤  Python 3.9       │                        │
│  │  MyBatis-Plus     │  │  PyTorch/ResNet50 │                        │
│  │  Spring Security  │  │  LangChain/Chroma │                        │
│  │  Flyway           │  │  sentence-transf. │                        │
│  │  :8080            │  │  :8000            │                        │
│  └────────┬─────────┘  └────────┬─────────┘                        │
│           │                     │                                     │
├───────────┼─────────────────────┼─────────────────────────────────────┤
│           │         数据层       │                                     │
│  ┌────────▼─────────────────────▼──────────────────────────────┐   │
│  │  PostgreSQL 16     Redis 7       MinIO                       │   │
│  │  + PostGIS 3       (缓存/Session)  (图片/文件存储)             │   │
│  │  + pgvector         :6379          :9000/:9001                │   │
│  │  :5432                                                       │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 技术架构分层

| 层 | 技术 | 职责 |
|----|------|------|
| 客户端 | Vue 3 + TypeScript + Element Plus + ECharts | 用户界面与交互 |
| 网关 | Nginx | 反向代理、静态资源、负载均衡 |
| 业务后端 | Spring Boot 3.5.2 | 认证鉴权、CRUD、任务调度、审核流程 |
| AI 服务 | FastAPI + PyTorch | 图像识别、RAG 检索、Agent 编排、天气查询 |
| 数据库 | PostgreSQL 16 + PostGIS + pgvector | 业务数据、地理空间、向量存储 |
| 缓存 | Redis 7 | Token/session 缓存、任务队列 |
| 文件存储 | MinIO | 病害图片、缩略图、知识文档附件 |
| 容器化 | Docker + Docker Compose | 一键部署、环境隔离 |

### 1.3 数据流

```
用户上传图片
    │
    ▼
┌─────────┐    POST /api/v1/diagnosis/upload     ┌──────────┐
│  Vue 3  │ ───────────────────────────────────▶ │  Spring   │
│  前端   │                                      │  Boot    │
│         │ ◀─── { diagnosisId, status }         │  后端    │
└─────────┘                                      └────┬─────┘
                                                     │
                                          ┌──────────┼──────────┐
                                          │ 存入MinIO │ @Async   │
                                          │ SHA256去重│ 异步推理  │
                                          └──────────┘     │     │
                                                           │     │
                                                    ┌──────▼─────┐
                                                    │  AI 服务   │
                                                    │  FastAPI   │
                                                    │            │
                                                    │ 1.图像分类 │
                                                    │ 2.RAG检索  │
                                                    │ 3.Agent编排│
                                                    │ 4.返回结果 │
                                                    └──────┬─────┘
                                                           │
                                              ┌────────────▼──┐
                                              │ 低置信度(<0.85)│
                                              │ → 审核队列     │
                                              │ 通过 → 农事任务│
                                              └───────────────┘
```

---

## 二、ER 图（数据库实体关系）

```
┌──────────┐       ┌──────────┐       ┌──────────┐
│   users  │1─────N│  farms   │1─────N│  fields  │
│  (用户)  │       │  (农场)  │       │  (地块)  │
└────┬─────┘       └──────────┘       └────┬─────┘
     │                                     │
     │ 1                                   │ 1
     │                                     │
     ├── N ┌────────────────┐              │ N
     │     │ planting_cycles│◄─────────────┘
     │     │   (种植周期)    │
     │     └───────┬────────┘
     │             │ 1
     │             │
     │             │ N
     │     ┌───────▼────────┐
     │     │  observations  │
     ├─────│  (田间观察)     │
     │     └───────┬────────┘
     │             │ 1
     │             │
     │             │ 1
     │     ┌───────▼────────────┐
     │     │ diagnosis_records  │◄─────┐
     │     │   (诊断记录)        │      │
     │     └──┬────────┬────────┘      │
     │        │ 1      │ 1             │
     │        │        │               │
     │   ┌────▼──┐ ┌───▼────────┐     │
     │   │ agent │ │review_queue│      │
     │   │ _runs │ │ (审核队列)  │      │
     │   └───────┘ └────┬───────┘      │
     │                  │              │
     │             ┌────▼──────┐       │
     │             │ farming_  │◄──────┘
     │             │  tasks    │
     │             │ (农事任务) │
     │             └───────────┘
     │
     ├── N ┌──────────────────┐
     │     │knowledge_documents│
     │     │   (知识文档)       │
     │     └──────────────────┘
     │
     ├── N ┌──────────────────┐
     │     │  model_versions  │
     │     │   (模型版本)      │
     │     └──────────────────┘
     │
     └─────┌──────────────────┐
           │  crops (作物库)  │◄─────┐
           └──────────────────┘      │
                                     │
           ┌──────────────────┐      │
           │ weather_records  │      │
           │   (天气记录)     │      │
           └──────────────────┘      │
                                     │
           ┌──────────────────┐      │
           │  market_prices   │◄─────┘
           │   (市场价格)     │
           └──────────────────┘
```

### 数据库表清单（14 张）

| # | 表名 | 中文 | 主键 | 关键外键 | 关键索引/约束 |
|---|------|------|------|----------|-------------|
| 1 | `users` | 用户 | UUID PK | farm_id→farms | UNIQUE(username), UNIQUE(phone) |
| 2 | `farms` | 农场 | UUID PK | owner_id→users | GIST(location), idx_farms_owner |
| 3 | `fields` | 地块 | UUID PK | farm_id→farms | GIST(location), GIST(boundary) |
| 4 | `crops` | 作物品种 | UUID PK | — | — |
| 5 | `planting_cycles` | 种植周期 | UUID PK | field_id→fields, crop_id→crops, created_by→users | idx_pc_field, idx_pc_status |
| 6 | `observations` | 田间观察 | UUID PK | cycle_id→planting_cycles, user_id→users | idx_obs_cycle, JSONB(images) |
| 7 | `diagnosis_records` | 病害诊断 | UUID PK | observation_id→observations, reviewer_id→users | UNIQUE(image_hash), JSONB(ai_result) |
| 8 | `weather_records` | 天气记录 | UUID PK | farm_id→farms | idx_wr_farm_time |
| 9 | `market_prices` | 市场价格 | UUID PK | crop_id→crops | idx_mp_crop_date |
| 10 | `farming_tasks` | 农事任务 | UUID PK | cycle_id→planting_cycles, diagnosis_id→diagnosis_records, assignee_id→users | idx_ft_assignee, idx_ft_scheduled |
| 11 | `knowledge_documents` | 知识文档 | UUID PK | author_id→users | IVFFLAT(embedding vector_cosine_ops), JSONB(tags) |
| 12 | `model_versions` | 模型版本 | UUID PK | — | UNIQUE(model_name, version) |
| 13 | `agent_runs` | Agent 运行 | UUID PK | diagnosis_id→diagnosis_records | JSONB(input_json, output_json) |
| 14 | `review_queue` | 审核队列 | UUID PK | diagnosis_id→diagnosis_records, assigned_to→users | UNIQUE(diagnosis_id) |

---

## 三、时序图

### 3.1 病害诊断全流程

```
农户(前端)     Spring Boot      MinIO       FastAPI/AI      PostgreSQL    农技人员
    │               │              │             │               │            │
    │ POST /upload  │              │             │               │            │
    │ (image,info)  │              │             │               │            │
    │──────────────▶│              │             │               │            │
    │               │ SHA256(file) │             │               │            │
    │               │─────────────▶│             │               │            │
    │               │ upload image │             │               │            │
    │               │◀─────────────│             │               │            │
    │               │              │             │               │            │
    │               │ INSERT diagnosis_record    │               │            │
    │               │───────────────────────────────────────────▶│            │
    │               │ INSERT observation                        │            │
    │               │───────────────────────────────────────────▶│            │
    │ {diagnosisId, │              │             │               │            │
    │  status:"processing"}        │             │               │            │
    │◀──────────────│              │             │               │            │
    │               │              │             │               │            │
    │               │ @Async processAsync()     │               │            │
    │               │──────────────────────────▶│               │            │
    │               │              │ POST/predict(image_url)     │            │
    │               │              │◄────────────────────────────│            │
    │               │              │  ResNet50 推理              │            │
    │               │              │────────────────────────────▶│            │
    │               │              │  {disease, confidence}      │            │
    │               │              │◄────────────────────────────│            │
    │               │              │             │               │            │
    │               │              │ POST/rag/retrieve(disease)  │            │
    │               │              │◄────────────────────────────│            │
    │               │              │  ChromaDB 向量检索          │            │
    │               │              │────────────────────────────▶│            │
    │               │              │  {citations}                │            │
    │               │              │◄────────────────────────────│            │
    │               │              │             │               │            │
    │               │              │ POST/agent/run(diag+weather+rag) │       │
    │               │              │◄────────────────────────────│            │
    │               │              │  LLM 生成防治建议           │            │
    │               │              │────────────────────────────▶│            │
    │               │              │  {treatment_plan}           │            │
    │               │              │◄────────────────────────────│            │
    │               │              │             │               │            │
    │               │ UPDATE diagnosis_record                    │            │
    │               │ (disease,confidence,ai_result)             │            │
    │               │───────────────────────────────────────────▶│            │
    │               │              │             │               │            │
    │               │ [if confidence < 0.85]                     │            │
    │               │ INSERT review_queue  │                     │            │
    │               │───────────────────────────────────────────▶│            │
    │               │              │             │               │            │
    │  GET /result  │              │             │               │            │
    │──────────────▶│              │             │               │            │
    │ {disease, conf, treatment, citations}      │               │            │
    │◀──────────────│              │             │               │            │
    │               │              │             │               │            │
    │               │ [审核流程]   │             │               │ 审核诊断   │
    │               │◀─────────────────────────────────────────────────────│
    │               │ POST /review?status=approved        │  批准/驳回   │
    │               │─────────────────────────────────────────────────────▶│
    │               │              │             │               │            │
    │               │ [审核通过 → 自动创建 farming_task]   │               │
    │               │ INSERT farming_task                  │               │
    │               │───────────────────────────────────────────▶│            │
    │               │              │             │               │            │
    │ 查看任务      │              │             │               │            │
    │──────────────▶│              │             │               │            │
    │ {task list}   │              │             │               │            │
    │◀──────────────│              │             │               │            │
```

### 3.2 登录认证流程

```
用户      前端         后端         PostgreSQL    Redis
 │         │            │              │            │
 │ 输入用户名密码        │              │            │
 │────────▶│            │              │            │
 │         │ POST /auth/login          │            │
 │         │───────────▶│              │            │
 │         │            │ SELECT user  │            │
 │         │            │─────────────▶│            │
 │         │            │◀─────────────│            │
 │         │            │ BCrypt.verify(password)    │
 │         │            │              │            │
 │         │            │ 生成 JWT (24h)            │
 │         │            │ 生成 RefreshToken (7d)     │
 │         │            │              │            │
 │         │            │ SET token → Redis         │
 │         │            │──────────────────────────▶│
 │         │            │              │            │
 │         │ {token, username, role}   │            │
 │         │◀───────────│              │            │
 │         │            │              │            │
 │ 存 localStorage      │              │            │
 │◀────────│            │              │            │
 │         │            │              │            │
 │ 后续请求 (Header: Authorization: Bearer <token>) │
 │         │───────────▶│              │            │
 │         │            │ JwtAuthFilter.verify()    │
 │         │            │──────────────────────────▶│
 │         │            │ 校验通过 → 注入 SecurityContext  │
```

---

## 四、部署图

```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Host (Linux/Windows)                   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Docker Network: yunnong-net (bridge)         │   │
│  │                                                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │   │
│  │  │  nginx       │  │  backend     │  │  ai-service  │   │   │
│  │  │  (frontend)  │  │  Spring Boot │  │  FastAPI     │   │   │
│  │  │  Port: 80    │  │  Port: 8080  │  │  Port: 8000  │   │   │
│  │  │  / → dist/   │  │              │  │              │   │   │
│  │  │  /api→:8080  │  │  ┌────────┐  │  │  ┌────────┐  │   │   │
│  │  │  /ai →:8000  │  │  │  JVM   │  │  │  │ PyTorch│  │   │   │
│  │  └──────────────┘  │  │  21    │  │  │  │ ResNet │  │   │   │
│  │                     │  └────────┘  │  │  │ LangCh.│  │   │   │
│  │                     └──────────────┘  │  └────────┘  │   │   │
│  │                                        └──────────────┘   │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │   │
│  │  │  postgres    │  │  redis       │  │  minio       │   │   │
│  │  │  PG 16       │  │  Redis 7     │  │  MinIO       │   │   │
│  │  │  + PostGIS   │  │  alpine      │  │  Port: 9000  │   │   │
│  │  │  + pgvector  │  │  Port: 6379  │  │  + :9001     │   │   │
│  │  │  Port: 5432  │  │              │  │  (console)   │   │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │   │
│  │                                                          │   │
│  │  ┌──────────────────────────────────────────────────┐   │   │
│  │  │  Volumes (持久化)                                 │   │   │
│  │  │  pgdata:/var/lib/postgresql/data                  │   │   │
│  │  │  redisdata:/data                                  │   │   │
│  │  │  miniodata:/data                                  │   │   │
│  │  │  ai_uploads:/app/uploads                          │   │   │
│  │  └──────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  外部暴露端口:                                                    │
│  :80    → 前端 (通过 Nginx)                                      │
│  :8080  → 业务后端 Swagger                                       │
│  :8000  → AI 服务 API                                            │
│  :9001  → MinIO 控制台                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 一键启动命令

```bash
# 0. 准备模型文件（确保存在）
ls ai-service/best_model.pth ai-service/class_to_idx.pth

# 1. 构建并启动全部服务
cd deploy
docker compose up -d --build

# 2. 验证
curl http://localhost:8080/actuator/health    # 后端
curl http://localhost:8000/health             # AI 服务
curl http://localhost:80/                      # 前端

# 3. 查看日志
docker compose logs -f backend
docker compose logs -f ai-service

# 4. 停止
docker compose down
```

---

## 五、后端模块设计

### 5.1 模块结构

```
backend/src/main/java/com/yunong/
├── YunnongApplication.java          ← 启动类
├── common/                           ← 公共组件
│   ├── R.java                       ← 统一响应 {code, data, message}
│   ├── PageResult.java              ← 分页结果
│   ├── AuditLog.java / AuditLogAspect.java  ← 审计日志 AOP
├── config/                           ← 配置
│   ├── SecurityConfig.java          ← Spring Security
│   ├── AsyncConfig.java             ← 异步线程池
│   ├── UuidGenerator.java           ← UUID 生成器
├── exception/                        ← 异常处理
│   ├── ErrorCode.java               ← 错误码枚举
│   ├── GlobalExceptionHandler.java  ← 全局异常处理
├── security/                         ← 安全
│   ├── JwtAuthenticationFilter.java ← JWT 过滤器
├── module/
│   ├── auth/          ← 认证 (AuthService, AuthController)
│   ├── user/          ← 用户管理 (UserService, UserController)
│   ├── farm/          ← 农场&地块 (Farm, Field, DTO)
│   ├── crop/          ← 作物&种植周期
│   ├── diagnosis/     ← 病害诊断 (DiagnosisService, AsyncDiagnosisService)
│   ├── task/          ← 农事任务 (CRUD + 日历)
│   ├── knowledge/     ← 知识库 (CRUD + 关键词/向量搜索)
│   ├── market/        ← 市场价格 (采集 + 趋势)
│   ├── weather/       ← 天气 (采集 + 趋势)
│   ├── model/         ← 模型版本 (注册/部署)
│   ├── agent/         ← Agent 运行记录
│   ├── review/        ← 审核队列
│   ├── monitor/       ← 系统监控 (概览/性能/漂移/未知样本)
│   └── system/        ← 定时任务 (ScheduledTaskService)
```

### 5.2 API 设计原则

- **统一前缀**: `/api/v1`
- **统一响应**: `{code: 0, data: {...}}` 或 `{code: 1001, message: "..."}`
- **认证**: JWT Bearer Token，HTTP Header `Authorization: Bearer <token>`
- **分页**: `{list: [...], total: N}`
- **软删除**: `deleted` 字段标记，MyBatis-Plus 自动过滤

---

## 六、AI 服务设计

### 6.1 模块结构

```
ai-service/
├── src/
│   ├── main.py            ← FastAPI 应用入口
│   ├── api/
│   │   ├── diagnosis.py   ← /api/v1/diagnosis/* 端点
│   │   ├── rag.py         ← /api/v1/rag/* 端点
│   │   └── weather.py     ← /api/v1/weather/* 端点
│   ├── services/
│   │   ├── inference_service.py   ← ResNet50 图像分类
│   │   ├── rag_service.py         ← ChromaDB 向量检索
│   │   ├── agent_service.py       ← LLM Agent 编排
│   │   ├── weather_service.py     ← 天气 API 集成
│   │   ├── diagnosis_service.py   ← 完整诊断流程编排
│   │   └── data_loader.py         ← 数据集加载
│   ├── models/
│   │   └── schemas.py      ← Pydantic 数据模型
│   └── core/
│       └── config.py       ← 配置管理
├── data/                    ← 训练/验证数据 (18类)
├── scripts/                 ← 辅助脚本
├── train.py                 ← 模型训练脚本
├── prepare_data.py          ← 数据划分脚本
├── best_model.pth           ← ResNet50 最佳权重
└── class_to_idx.pth         ← 类别→索引映射
```

### 6.2 核心流程

```
POST /api/v1/diagnosis/image
    │
    ├── 1. 图片验证 (大小 ≤ 10MB, 格式 jpg/jpeg/png)
    ├── 2. InferenceService.predict(image)
    │       ├── ResNet50 前向推理
    │       ├── Softmax → confidence
    │       └── 返回 (disease_name, confidence)
    ├── 3. WeatherService.get_weather(city/lat/lon)
    │       └── OpenWeatherMap API (或模拟数据)
    ├── 4. RAGService.retrieve(disease_name)
    │       ├── ChromaDB 向量相似度搜索
    │       └── 返回 top-K 相关文档片段
    ├── 5. AgentService.generate_advice(...)
    │       ├── 组装 Prompt (诊断+天气+RAG+生育期)
    │       ├── LLM 生成防治建议
    │       └── 返回 (advice, references, weather_info)
    └── 6. 返回完整 DiagnosisResult
```

---

## 七、前端设计

### 7.1 页面结构

```
frontend/src/views/
├── Login.vue             ← 登录页
├── Dashboard.vue          ← 首页概览 (统计卡片 + 图表)
├── Profile.vue            ← 个人中心
│
├── FarmList.vue           ← 农场 & 地块管理 (农户)
├── FarmerCrops.vue        ← 作物 & 种植周期 (农户)
├── FarmerTask.vue         ← 农事任务 (农户)
├── Farmer.vue             ← 农户工作台入口
│
├── DiseaseUpload.vue      ← 病害图片上传 (核心页面)
├── ResultDetail.vue       ← 诊断结果详情
│
├── TechWorkbench.vue      ← 农技工作台 (审核入口)
├── Tech.vue               ← 农技首页
│
├── CoopMarket.vue         ← 市场价格监控 (合作社)
├── Coop.vue               ← 合作社首页
│
├── Admin.vue              ← 管理首页
├── AdminUsers.vue         ← 用户管理
├── AdminKnowledge.vue     ← 知识库管理
├── AdminModels.vue        ← 模型版本管理
│
└── components/
    ├── Sidebar.vue         ← 侧边导航
    └── NotificationPanel.vue ← 通知面板
```

### 7.2 技术栈

| 库 | 用途 |
|----|------|
| Vue 3 + Composition API | 组件框架 |
| TypeScript | 类型安全 |
| Vite 6 | 构建工具 |
| Pinia | 状态管理 |
| Vue Router 4 | 路由 |
| Element Plus | UI 组件库 |
| ECharts 6 | 图表 |
| Axios | HTTP 请求 |

---

## 八、安全设计

| 措施 | 实现 |
|------|------|
| 认证 | JWT (HS256, 24h 有效期) |
| 密码加密 | BCrypt (Spring Security) |
| 权限控制 | 4 角色 + `@PreAuthorize` 方法级 |
| CORS | 白名单域名限制 |
| XSS | Vue 3 自动转义 + CSP Header |
| SQL 注入 | MyBatis-Plus 参数化查询 |
| 文件上传 | 大小限制 (20MB) + 类型白名单 |
| 去重 | SHA256 图片哈希唯一索引 |
| 删除保护 | 所有表软删除 (deleted=0/1) |
| 审计 | AOP 切面记录操作日志 |

---

## 九、关键指标

| 指标 | 目标值 |
|------|--------|
| 支持病害类别 | 18 类 |
| 模型准确率 | ≥ 90% |
| 推理延迟 | < 15 秒 |
| 数据库表 | 14 张 |
| API 接口 | 55+ |
| 并发用户 | ≥ 50 |
| 部署时间 | < 5 分钟 (docker compose up) |
