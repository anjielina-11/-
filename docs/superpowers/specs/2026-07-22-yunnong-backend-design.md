# 云南特色农业智能诊断与生产管理平台 — 设计规格说明

> 2026-07-22 | 三人小组作业 | 第一部分：业务后端 + 数据库

## 1. 架构概览

```
┌──────────┐  ┌──────────────┐  ┌──────────┐
│  Vue3    │  │  Spring Boot │  │  FastAPI │
│  前端    │──│  业务后端     │──│  AI服务  │
└──────────┘  └──────┬───────┘  └──────────┘
                     │
              ┌──────┴───────┐
              │ PostgreSQL   │
              │ + PostGIS    │
              │ + pgvector   │
              ├──────────────┤
              │ Redis        │
              ├──────────────┤
              │ MinIO        │
              └──────────────┘
```

- **前端**: Vue 3 + TypeScript + Vite + Pinia + Element Plus + ECharts
- **业务后端**: Spring Boot 3.5 + Java 17 + MyBatis-Plus + Swagger/OpenAPI
- **AI 服务**: Python 3.11 + FastAPI + LangChain/LlamaIndex
- **数据库**: PostgreSQL 16 + PostGIS 3 + pgvector
- **部署**: Docker Compose + Nginx

## 2. 数据库设计

数据库: `yunnong`

### 2.1 表列表 (14张)

| # | 表名 | 说明 | 关键字段 |
|---|------|------|----------|
| 1 | users | 用户 | id(UUID), username, password_hash, role, phone |
| 2 | farms | 农场 | id(UUID), owner_id→users, name, address, area, location(GEOMETRY) |
| 3 | fields | 地块 | id(UUID), farm_id→farms, name, area, soil_type, location(GEOMETRY) |
| 4 | crops | 作物品种 | id(UUID), name, category, growth_days, description |
| 5 | planting_cycles | 种植周期 | id(UUID), field_id→fields, crop_id→crops, start_date, growth_stage |
| 6 | observations | 田间观察 | id(UUID), cycle_id→planting_cycles, desc, images, observed_at |
| 7 | diagnosis_records | 病害诊断 | id(UUID), observation_id→observations, image_url, image_hash(UNIQUE), ai_result(JSONB), confidence, review_status, review_by→users |
| 8 | weather_records | 天气记录 | id(UUID), farm_id→farms, temperature, humidity, rainfall, recorded_at |
| 9 | market_prices | 市场价格 | id(UUID), crop_id→crops, price, market, recorded_at |
| 10 | farming_tasks | 农事任务 | id(UUID), diagnosis_id→diagnosis_records, assignee→users, task_type, status, due_date |
| 11 | knowledge_documents | 知识文档 | id(UUID), title, content, category, embedding(pgvector), version |
| 12 | model_versions | 模型版本 | id(UUID), model_name, version, accuracy, deployed_at |
| 13 | agent_runs | Agent运行 | id(UUID), diagnosis_id, agent_type, input, output(JSONB), run_at |
| 14 | review_queue | 审核队列 | id(UUID), diagnosis_id, priority, status, assigned_to→users |

### 2.2 扩展与索引

- PostGIS: `farms.location`, `fields.location` → `GEOMETRY(Point, 4326)`, GiST 索引
- pgvector: `knowledge_documents.embedding` → `vector(1536)`, IVFFlat 索引
- 唯一约束: `diagnosis_records.image_hash`, `users.username`
- 外键: 所有 `_id` 字段
- JSONB: `diagnosis_records.ai_result`, `agent_runs.output`
- 审计: 所有表带 `created_at`, `updated_at`

### 2.3 业务闭环数据流

```
建立农场(farms) → 创建地块(fields) → 登记作物(planting_cycles)
→ 上传异常图片(observations) → AI识别(diagnosis_records)
→ RAG检索(knowledge_documents) → Agent综合(agent_runs)
→ 农技审核(review_queue) → 生成任务(farming_tasks)
→ 跟踪效果
```

## 3. API 接口设计

统一前缀 `/api/v1`，JWT Bearer 认证，Swagger/OpenAPI 3.0 文档。

### 3.1 统一响应

```json
{ "code": 200, "message": "success", "data": {}, "timestamp": 1700000000 }
```

分页响应:
```json
{ "code": 200, "message": "success", "data": { "records": [], "total": 100, "page": 1, "size": 10 } }
```

### 3.2 接口清单 (45+)

#### auth (4个) — 公开
- `POST /api/v1/auth/login` — 登录
- `POST /api/v1/auth/register` — 注册
- `POST /api/v1/auth/refresh` — 刷新Token
- `POST /api/v1/auth/logout` — 登出

#### user (6个) — 需认证
- `GET /api/v1/users/me` — 当前用户信息
- `PUT /api/v1/users/me` — 修改个人信息
- `GET /api/v1/users` — 用户列表(管理员)
- `GET /api/v1/users/{id}` — 用户详情
- `PUT /api/v1/users/{id}/role` — 修改角色(管理员)
- `DELETE /api/v1/users/{id}` — 删除用户(管理员)

#### farm (6个) — 需认证
- `POST /api/v1/farms` — 创建农场
- `GET /api/v1/farms` — 我的农场列表
- `GET /api/v1/farms/{id}` — 农场详情
- `PUT /api/v1/farms/{id}` — 更新农场
- `POST /api/v1/farms/{farmId}/fields` — 添加地块
- `GET /api/v1/farms/{farmId}/fields` — 地块列表

#### crop (6个) — 需认证
- `POST /api/v1/crops` — 添加作物
- `GET /api/v1/crops` — 作物列表
- `GET /api/v1/crops/{id}` — 作物详情
- `POST /api/v1/planting-cycles` — 创建种植周期
- `GET /api/v1/planting-cycles` — 种植周期列表
- `PUT /api/v1/planting-cycles/{id}` — 更新生育期

#### diagnosis (6个) — 需认证
- `POST /api/v1/diagnosis/upload` — 上传图片(异步)
- `GET /api/v1/diagnosis/{id}` — 诊断详情
- `GET /api/v1/diagnosis` — 诊断列表
- `POST /api/v1/diagnosis/{id}/review` — 审核(农技人员)
- `POST /api/v1/diagnosis/{id}/feedback` — 反馈效果(农户)
- `GET /api/v1/diagnosis/stats` — 诊断统计

#### task (5个) — 需认证
- `GET /api/v1/tasks` — 任务列表(分页/筛选)
- `POST /api/v1/tasks` — 创建任务
- `PUT /api/v1/tasks/{id}` — 更新任务
- `PUT /api/v1/tasks/{id}/status` — 状态流转
- `GET /api/v1/tasks/calendar` — 农事日历

#### weather (3个) — 需认证
- `GET /api/v1/weather` — 天气记录查询
- `GET /api/v1/weather/trend` — 天气趋势
- `POST /api/v1/weather/fetch` — 手动触发采集(管理员)

#### market (3个) — 需认证
- `GET /api/v1/market` — 价格记录查询
- `GET /api/v1/market/trend` — 价格趋势
- `POST /api/v1/market/fetch` — 手动触发采集(管理员)

#### knowledge (5个) — 需认证
- `POST /api/v1/knowledge/documents` — 上传知识文档
- `GET /api/v1/knowledge/documents` — 文档列表
- `GET /api/v1/knowledge/documents/{id}` — 文档详情
- `GET /api/v1/knowledge/search` — RAG检索(语义搜索)
- `PUT /api/v1/knowledge/documents/{id}` — 更新文档

#### monitor (4个) — 管理员/农技人员
- `GET /api/v1/monitor/model-performance` — 模型性能
- `GET /api/v1/monitor/data-drift` — 数据漂移
- `GET /api/v1/monitor/unknown-samples` — 未知样本
- `GET /api/v1/monitor/overview` — 监控概览

### 3.3 角色权限矩阵

| 接口模块 | 农户 | 农技人员 | 合作社管理 | 管理员 |
|----------|------|----------|-----------|--------|
| auth | ✅ | ✅ | ✅ | ✅ |
| user(自己) | ✅ | ✅ | ✅ | ✅ |
| user(他人) | ❌ | ❌ | ❌ | ✅ |
| farm | ✅(自己) | ✅(查看) | ✅(管辖) | ✅ |
| crop | ✅ | ✅ | ✅ | ✅ |
| diagnosis上传 | ✅ | ✅ | ❌ | ❌ |
| diagnosis审核 | ❌ | ✅ | ❌ | ✅ |
| task | ✅(自己) | ✅(分配) | ✅ | ✅ |
| weather | ✅ | ✅ | ✅ | ✅ |
| market | ✅ | ✅ | ✅ | ✅ |
| knowledge | ❌ | ✅ | ✅ | ✅ |
| monitor | ❌ | ✅ | ✅ | ✅ |

## 4. 后端技术选型

| 组件 | 选择 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.5.x | 最新稳定 |
| JDK | Java 17 (LTS) | |
| ORM | MyBatis-Plus 3.5.x | 代码生成 + 分页 |
| 安全 | Spring Security + JWT (jjwt) | |
| 参数校验 | Jakarta Validation | |
| 文档 | SpringDoc OpenAPI (Swagger 3) | |
| 工具 | Lombok, MapStruct, Hutool | |
| 缓存 | Spring Cache + Redis | |
| 文件 | MinIO Java SDK | |
| 定时 | Spring @Scheduled | |
| 日志 | SLF4J + Logback | |

## 5. 部署设计 (Docker Compose)

```yaml
services:
  postgres:    # PostgreSQL 16 + PostGIS 3 + pgvector
  redis:       # Redis 7
  minio:       # MinIO (文件存储)
  backend:     # Spring Boot jar
  frontend:    # Nginx + Vue dist (后续)
  ai-service:  # FastAPI (后续)
```

## 6. 今天交付范围（第一部分）

### 6.1 目录结构
```
homework/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/yunong/
│       │   ├── YunnongApplication.java
│       │   ├── common/          # R, PageResult, BaseEntity, GlobalExceptionHandler
│       │   ├── config/          # SecurityConfig, JwtConfig, SwaggerConfig, MyBatisPlusConfig, CorsConfig, RedisConfig
│       │   ├── security/        # JwtTokenProvider, JwtAuthFilter, UserDetailsServiceImpl
│       │   ├── module/
│       │   │   ├── auth/        # controller/service/dto/mapper
│       │   │   ├── user/
│       │   │   ├── farm/
│       │   │   ├── crop/
│       │   │   ├── diagnosis/
│       │   │   ├── task/
│       │   │   ├── weather/
│       │   │   ├── market/
│       │   │   ├── knowledge/
│       │   │   └── monitor/
│       │   └── exception/       # BusinessException, ErrorCode
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   └── db/migration/    # Flyway SQL
│       └── test/java/com/yunong/
├── deploy/
│   ├── docker-compose.yml
│   ├── nginx/nginx.conf
│   └── init-db/01-init.sql
└── docs/
    └── superpowers/specs/
```

### 6.2 具体交付物
1. ✅ Maven 项目骨架 (pom.xml + 启动类)
2. ✅ 14 张表 Flyway 迁移脚本
3. ✅ 10 个模块 Controller + Service + Mapper + Entity + DTO
4. ✅ JWT 认证 + RBAC 拦截器
5. ✅ 统一响应/异常/分页
6. ✅ Swagger 文档（每个接口有注解）
7. ✅ MinIO 文件上传集成
8. ✅ Redis 缓存配置
9. ✅ 单元测试（auth + user + diagnosis 核心）
10. ✅ Docker Compose (postgres + redis + minio + backend)
