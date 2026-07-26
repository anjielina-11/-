# API 接口索引

## 1. 统一约定

- 浏览器统一入口：`http://localhost`
- Backend 前缀：`/api/v1`
- AI 反向代理前缀：`/ai`
- Backend Swagger UI：`/swagger-ui.html`
- OpenAPI JSON：`/api-docs`
- 除登录和注册外，Backend 请求通常需要：`Authorization: Bearer <token>`
- Backend 统一响应：`{ "code": 0, "message": "success", "data": ... }`

## 2. Backend 接口

### 认证与用户

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/auth/login` | 登录 |
| POST | `/api/v1/auth/register` | 注册普通用户 |
| POST | `/api/v1/auth/refresh` | 刷新 Token |
| POST | `/api/v1/auth/logout` | 登出 |
| GET/PUT | `/api/v1/users/me` | 当前用户资料 |
| GET/POST | `/api/v1/users` | 用户列表/管理员创建用户 |
| GET/PUT | `/api/v1/users/{id}` | 用户详情/修改 |
| PUT | `/api/v1/users/{id}/role` | 修改角色 |
| PUT | `/api/v1/users/{id}/status` | 修改状态 |

### 农场、地块和种植周期

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/api/v1/farms` | 查询/创建农场 |
| GET | `/api/v1/farms/accessible` | 查询当前角色可访问农场 |
| GET/PUT | `/api/v1/farms/{id}` | 农场详情/修改 |
| GET/POST | `/api/v1/farms/{farmId}/fields` | 查询/创建地块 |
| PUT/DELETE | `/api/v1/farms/{farmId}/fields/{fieldId}` | 修改/删除地块 |
| GET/POST | `/api/v1/crops` | 查询/创建作物 |
| GET | `/api/v1/crops/{id}` | 作物详情 |
| GET/POST | `/api/v1/planting-cycles` | 查询/创建种植周期 |
| PUT/DELETE | `/api/v1/planting-cycles/{id}` | 修改/删除种植周期 |

### 病害诊断

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/diagnosis/upload` | multipart 上传图片，字段包含 `file`、`cycleId`、`description` |
| GET | `/api/v1/diagnosis` | 分页查询诊断记录 |
| GET | `/api/v1/diagnosis/{id}` | 诊断详情 |
| GET | `/api/v1/diagnosis/{id}/image` | 经过鉴权的原图读取 |
| GET | `/api/v1/diagnosis/result/{id}` | 查询异步识别结果 |
| POST | `/api/v1/diagnosis/{id}/review` | 农技审核 |
| POST | `/api/v1/diagnosis/{id}/feedback` | 农户防治反馈 |
| GET | `/api/v1/diagnosis/stats` | 诊断统计 |

### 任务、天气与市场

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/api/v1/tasks` | 查询/创建农事任务 |
| PUT | `/api/v1/tasks/{id}` | 修改任务 |
| PUT | `/api/v1/tasks/{id}/status` | 任务状态流转 |
| GET | `/api/v1/tasks/calendar` | 农事日历 |
| GET | `/api/v1/weather` | 天气数据 |
| GET | `/api/v1/weather/trend` | 未来七天趋势 |
| POST | `/api/v1/weather/fetch` | 手动实时更新 |
| GET | `/api/v1/market` | 市场价格 |
| GET | `/api/v1/market/trend` | 市场趋势 |
| POST | `/api/v1/market/fetch` | 手动采集 |

### 知识、模型和监控

| 资源前缀 | 主要能力 |
|---|---|
| `/api/v1/knowledge` | 文档上传、列表、详情、更新和检索 |
| `/api/v1/model-versions` | 模型版本注册、查询、修改、部署和删除 |
| `/api/v1/review-queue` | 审核队列查询、分配和完成 |
| `/api/v1/agent-runs` | Agent 运行记录查询 |
| `/api/v1/monitor` | 总览、模型性能、数据漂移和未知样本指标 |

## 3. AI 服务接口

Backend 在 Compose 内通过 `http://ai-service:8000` 调用；浏览器调试时通过 `/ai` 代理访问。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ai/health` | 健康检查与模型状态 |
| POST | `/ai/diagnosis/image` | 图片分类并返回建议 |
| POST | `/ai/diagnosis/simple` | 仅返回分类结果 |
| POST | `/ai/diagnosis/full` | 分类 + 天气 + RAG + 建议 |
| POST | `/ai/diagnosis/advice` | 根据结构化诊断生成建议 |
| GET | `/ai/diagnosis/diseases` | 支持病害类别 |
| POST | `/ai/rag/ingest` | 导入知识文档 |
| POST | `/ai/rag/retrieve` | 检索知识片段 |
| GET | `/ai/weather/city` | 按城市查询天气 |
| GET | `/ai/weather/coords` | 按坐标查询天气 |

## 4. 常见状态与错误

| HTTP 状态 | 含义 |
|---|---|
| 400 | 参数或业务状态非法 |
| 401 | 未登录、Token 缺失或失效 |
| 403 | 角色权限不足或资源不属于当前用户 |
| 404 | 业务资源不存在 |
| 409 | 重复数据或状态冲突 |
| 413 | 上传文件过大 |
| 500 | 未处理的服务异常 |

接口字段以运行中的 Swagger/OpenAPI 为最终准确信息；本文件负责提供演示和联调索引，避免复制大量容易过期的请求/响应示例。
