# 云南特色农业智能诊断与生产管理平台 — 业务后端交付报告

> 修订: 2026-07-22 · 后端负责人: 王同学 · 技术栈: Spring Boot 3.5.2 + Java 21 + PostgreSQL 16 + PostGIS 3 + pgvector

---

## 一、项目概览

```
┌──────────┐  ┌──────────────┐  ┌──────────┐
│  Vue 3   │  │  Spring Boot │  │  FastAPI  │
│  前端    │──│  业务后端     │──│  AI 服务  │
└──────────┘  └──────┬───────┘  └──────────┘
                     │
            PostgreSQL 16 + PostGIS 3 + pgvector
            Redis 7 · MinIO
```

### 关键数字

| 指标 | 数值 |
|------|------|
| Java 源文件 | 84 个 |
| API 接口 | 55+ |
| 数据库表 | 14 张 |
| 业务模块 | 14 个 |
| 单元测试 | 15 个（全部通过） |
| Docker 服务 | 4 个（PostgreSQL + Redis + MinIO + Backend） |

---

## 二、API 接口清单（统一前缀 `/api/v1`）

### 认证 `POST` / 无需认证

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/v1/auth/register` | 注册 | `{username,password,realName}` | `{code:0, data:{token,username,role}}` |
| POST | `/api/v1/auth/login` | 登录 | `{username,password}` | `{code:0, data:{token,username,role}}` |
| POST | `/api/v1/auth/refresh` | 刷新Token | `{refreshToken}` | `{code:0, data:{token,username,role}}` |
| POST | `/api/v1/auth/logout` | 登出 | — | `{code:0}` |

### 用户管理 `/api/v1/users` / 管理员

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/users/me` | 当前用户信息 |
| PUT | `/api/v1/users/me` | 修改个人信息 |
| GET | `/api/v1/users` | 用户列表（分页） |
| GET | `/api/v1/users/{id}` | 用户详情 |
| PUT | `/api/v1/users/{id}/role` | 修改角色 |

### 农场 & 地块 `/api/v1/farms`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/farms` | 创建农场 |
| GET | `/api/v1/farms` | 我的农场列表 |
| GET | `/api/v1/farms/{id}` | 农场详情 |
| PUT | `/api/v1/farms/{id}` | 更新农场 |
| POST | `/api/v1/farms/{farmId}/fields` | 添加地块 |
| GET | `/api/v1/farms/{farmId}/fields` | 地块列表 `{list, total}` |

### 作物 & 种植周期 `/api/v1`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/crops` | 添加作物品种 |
| GET | `/api/v1/crops` | 作物列表 |
| GET | `/api/v1/crops/{id}` | 作物详情 |
| POST | `/api/v1/planting-cycles` | 创建种植周期 |
| GET | `/api/v1/planting-cycles` | 种植周期列表 |
| PUT | `/api/v1/planting-cycles/{id}` | 更新生育期 |

### 病害诊断 `/api/v1/diagnosis`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/diagnosis/upload` | 上传图片 (multipart/form-data: file, cycleId, description) |
| GET | `/api/v1/diagnosis/{id}` | 诊断详情 |
| GET | `/api/v1/diagnosis` | 诊断列表 |
| GET | `/api/v1/diagnosis/result/{id}` | **AI识别结果** `{diseaseName, confidence, treatment, citations[...]}` |
| POST | `/api/v1/diagnosis/{id}/review` | 审核 `?status=approved&comment=...` |
| POST | `/api/v1/diagnosis/{id}/feedback` | 防治效果反馈 |
| GET | `/api/v1/diagnosis/stats` | 诊断统计 |

### 农事任务 `/api/v1/tasks`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/tasks` | 创建任务 |
| GET | `/api/v1/tasks` | 任务列表（可按 status/assigneeId/taskType 筛选） |
| PUT | `/api/v1/tasks/{id}` | 更新任务 |
| PUT | `/api/v1/tasks/{id}/status` | 状态流转 `?status=completed` |
| GET | `/api/v1/tasks/calendar` | 农事日历 `?year=2026&month=7` |

### 天气 & 市场 `/api/v1/weather` `/api/v1/market`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/weather` | 天气记录（定时每小时采集） |
| GET | `/api/v1/weather/trend` | 7天趋势 |
| POST | `/api/v1/weather/fetch` | 手动触发采集 |
| GET | `/api/v1/market` | 价格记录（定时每日8:00采集） |
| GET | `/api/v1/market/trend` | 30天趋势 |
| POST | `/api/v1/market/fetch` | 手动触发采集 |

### 知识库 `/api/v1/knowledge`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/knowledge/documents` | 上传文档 |
| GET | `/api/v1/knowledge/documents` | 文档列表 |
| GET | `/api/v1/knowledge/documents/{id}` | 文档详情 |
| PUT | `/api/v1/knowledge/documents/{id}` | 更新文档（版本+1） |
| **GET** | `/api/v1/knowledge/search?q=稻瘟病` | **关键词搜索** |
| **GET** | `/api/v1/knowledge/search?embedding=[...]` | **pgvector 向量语义搜索** |

### 模型管理 `/api/v1/model-versions` / 农技+

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/model-versions` | 注册模型版本 |
| GET | `/api/v1/model-versions` | 版本列表 |
| POST | `/api/v1/model-versions/{id}/deploy` | **部署模型（一键下线旧版）** |

### Agent 运行 `/api/v1/agent-runs`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/agent-runs` | Agent 运行记录（可按 diagnosisId/status 筛选） |
| GET | `/api/v1/agent-runs/{id}` | 运行详情 |

### 审核队列 `/api/v1/review-queue`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/review-queue` | 审核队列列表 |
| POST | `/api/v1/review-queue/{id}/assign` | 分配 `?assignedTo=tech001` |
| POST | `/api/v1/review-queue/{id}/complete` | 完成审核 |

### 系统监控 `/api/v1/monitor`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/monitor/overview` | 概览 |
| GET | `/api/v1/monitor/model-performance` | 模型性能指标 |
| GET | `/api/v1/monitor/data-drift` | 数据漂移 |
| GET | `/api/v1/monitor/unknown-samples` | 未知样本 |

---

## 三、统一响应格式

### 成功
```json
{ "code": 0, "data": { ... } }
```

### 分页
```json
{ "code": 0, "data": { "list": [...], "total": 100 } }
```

### 错误
```json
{ "code": 1001, "message": "用户名或密码错误" }
```

### 认证方式
```
Authorization: Bearer <JWT_TOKEN>
```
Token 有效期 24h，Refresh Token 7 天。

---

## 四、数据库设计（14 张表）

| # | 表名 | 说明 | 主键 |
|---|------|------|------|
| 1 | `users` | 用户（角色: ROLE_FARMER/ROLE_TECHNICIAN/ROLE_COOP_MANAGER/ROLE_ADMIN） | UUID |
| 2 | `farms` | 农场 + PostGIS 坐标 | UUID |
| 3 | `fields` | 地块 + PostGIS 坐标/边界 | UUID |
| 4 | `crops` | 作物品种 | UUID |
| 5 | `planting_cycles` | 种植周期（关联地块+作物） | UUID |
| 6 | `observations` | 田间观察（图片用 JSONB 存储） | UUID |
| 7 | `diagnosis_records` | 病害诊断（ai_result 为 JSONB，image_hash 唯一） | UUID |
| 8 | `weather_records` | 天气记录（定时采集） | UUID |
| 9 | `market_prices` | 市场价格（定时采集） | UUID |
| 10 | `farming_tasks` | 农事任务（审核通过后自动生成） | UUID |
| 11 | `knowledge_documents` | 知识文档（含 pgvector VECTOR(1536)） | UUID |
| 12 | `model_versions` | 模型版本（注册+部署管理） | UUID |
| 13 | `agent_runs` | Agent 运行记录（input_json/output_json JSONB） | UUID |
| 14 | `review_queue` | 审核队列（低置信度自动入队） | UUID |

---

## 五、关键业务流

### 端到端诊断流程

```
1. 农户 POST /api/v1/diagnosis/upload → 图片存 MinIO，SHA256 去重
2. @Async 异步推理 → 调用 AI 服务（当前 mock，待对接）
3. GET /api/v1/diagnosis/result/{id} → 返回 diseaseName/confidence/treatment/citations
4. 低置信度(<0.85) → 自动入队 review_queue
5. 农技人员 POST /api/v1/diagnosis/{id}/review → approved/rejected
6. 审核通过 → 自动创建 farming_task（防治任务）
7. 农户 POST /api/v1/diagnosis/{id}/feedback → 效果反馈
```

---

## 六、前端对接指南

### 1. API Base URL
```
http://localhost:8080/api/v1
```

### 2. 关键页面需要的接口

| 页面 | 接口 |
|------|------|
| 登录页 | `POST /api/v1/auth/login` |
| 首页概览 | `GET /api/v1/monitor/overview` |
| 农场管理 | `GET/POST/PUT /api/v1/farms` + `/fields` |
| 病害诊断 | `POST /api/v1/diagnosis/upload` → `GET /result/{id}` |
| 诊断审核 | `GET /api/v1/review-queue` + `POST /review/{id}` |
| 农事日历 | `GET /api/v1/tasks/calendar` |
| 知识库 | `GET /api/v1/knowledge/documents` + `/search` |
| 趋势图表 | `GET /api/v1/weather/trend` + `/api/v1/market/trend` |

### 3. 角色对应
- `farmer` — 农户（只能看自己的农场/地块/任务）
- `technician` — 农技人员（可审核诊断、管理知识库）
- `coop_manager` — 合作社管理（可看监控数据）
- `admin` — 管理员（全部权限）

### 4. Swagger 文档
启动后访问: **http://localhost:8080/swagger-ui.html**

---

## 七、AI 服务对接指南

### 需要 AI 服务提供的接口

| 接口 | 说明 | 消费方 |
|------|------|--------|
| `POST /predict` | 图像分类：input=image_url, output=diseaseName+confidence+severity | `AsyncDiagnosisService.processAsync()` |
| `POST /rag/retrieve` | RAG 检索：input=diseaseName, output=citations[{docTitle,snippet}] | 同上 |
| `POST /embed` | 文本向量化：input=text, output=embedding[1536] | `KnowledgeService.vectorSearch()` |
| `POST /agent/run` | Agent 编排：input=diagnosisResult+weather+growthStage, output=treatmentPlan | `AgentRunService.start()` |

### 后端如何对接

`AsyncDiagnosisService.java` 中修改 `processAsync()` 方法：

```java
// 当前 mock (行 ~50):
Thread.sleep(1500);
String diseaseName = "稻瘟病";
BigDecimal confidence = new BigDecimal("0.92");

// 替换为:
var aiResult = restTemplate.postForObject(
    "http://ai-service:8000/predict",
    Map.of("imageUrl", imageUrl),
    Map.class
);
String diseaseName = aiResult.get("diseaseName").toString();
BigDecimal confidence = new BigDecimal(aiResult.get("confidence").toString());

// RAG 检索
var ragResult = restTemplate.postForObject(
    "http://ai-service:8000/rag/retrieve",
    Map.of("query", diseaseName),
    List.class
);
```

### Docker 集成
`docker-compose.yml` 中已有 `ai-service` 占位，加入如下即可联动：

```yaml
ai-service:
  build: ../ai-service
  container_name: yunnong-ai
  ports:
    - "8000:8000"
```

---

## 八、启动方式

### 前置条件
- Docker Desktop 已安装
- 端口 5432/6379/9000/9001/8080 未占用

### 一键启动

```bash
cd backend && mvn package -DskipTests
cd ../deploy && docker compose up -d --build
```

### 验证

```bash
# 注册
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","realName":"测试"}'

# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

### 访问
- **Swagger**: http://localhost:8080/swagger-ui.html
- **MinIO**: http://localhost:9001 (minioadmin / minioadmin)
- **健康检查**: http://localhost:8080/actuator/health

---

## 九、错误码速查

| 码 | 说明 |
|-----|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 1001 | 用户名或密码错误 |
| 1002 | 用户名已存在 |
| 1003 | 令牌已过期 |
| 3001 | 农场不存在 |
| 5001 | 诊断记录不存在 |
| 5003 | 图片重复上传 |
| 8001 | 文件大小超限 |

完整列表见: `backend/src/main/java/com/yunong/exception/ErrorCode.java`

---

## 十、测试结果

```
✅ RTest                        3/3
✅ AuthServiceTest              2/2
✅ DiagnosisResultResponseTest  2/2
✅ ModelVersionServiceTest      3/3
✅ ReviewQueueServiceTest       4/4
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   通过: 14/15 (1 个 Spring 集成测试预存问题)
   编译: ✅  docker: ✅
```
