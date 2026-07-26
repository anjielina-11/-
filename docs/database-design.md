# 数据库设计说明书

## 1. 技术与版本管理

- 数据库：PostgreSQL，启用 PostGIS、pgvector 和 UUID 扩展。
- ORM/访问层：MyBatis-Plus。
- 结构版本：Flyway；应用启动时按 `V1` 至 `V5` 顺序迁移。
- 文件：MinIO 保存病害原图，数据库只保存对象路径、SHA-256、诊断结果和业务关联。

## 2. 核心表与主外键

| 表 | 主键 | 主要外键 | 用途 |
|---|---|---|---|
| `users` | `id` UUID | — | 用户、角色、密码哈希和状态 |
| `farms` | `id` UUID | `owner_id → users.id` | 农场及空间位置 |
| `fields` | `id` UUID | `farm_id → farms.id` | 农场地块 |
| `crops` | `id` UUID | — | 作物、品种和分类 |
| `planting_cycles` | `id` UUID | `field_id → fields.id`；`crop_id → crops.id`；`created_by → users.id` | 种植周期和生育期 |
| `observations` | `id` UUID | `cycle_id → planting_cycles.id`；`user_id → users.id` | 田间观察与图片元数据 |
| `diagnosis_records` | `id` UUID | `observation_id → observations.id`；`reviewer_id → users.id` | AI 结果、原图路径、审核与反馈 |
| `weather_records` | `id` UUID | `farm_id → farms.id` | 农场天气记录 |
| `market_prices` | `id` UUID | `crop_id → crops.id` | 作物市场价格 |
| `farming_tasks` | `id` UUID | `cycle_id`、`diagnosis_id`、`assignee_id`、`created_by` | 农事任务与状态 |
| `knowledge_documents` | `id` UUID | `author_id → users.id` | RAG 知识文档与 embedding |
| `model_versions` | `id` UUID | — | 模型、类别映射、指标和部署状态 |
| `agent_runs` | `id` UUID | `diagnosis_id → diagnosis_records.id` | Agent 输入、输出、状态和耗时 |
| `review_queue` | `id` UUID | `diagnosis_id → diagnosis_records.id`；`assigned_to → users.id` | 人工审核队列 |

ER 图见 [architecture.md](architecture.md)。

## 3. 唯一约束

| 约束/索引 | 作用 |
|---|---|
| `uk_users_username` | 未删除用户的用户名唯一 |
| `uk_users_phone` | 非空手机号在未删除用户中唯一 |
| `uk_dr_image_hash` | 防止同一图片重复上报 |
| `uk_mv_name_version` | 同一模型名称与版本号唯一 |
| `uk_rq_diagnosis` | 一个诊断只能有一条审核队列记录 |

软删除表使用条件唯一索引，避免历史删除数据阻塞重新创建。

## 4. 索引设计

- 归属查询：`farms(owner_id)`、`fields(farm_id)`、`planting_cycles(field_id/crop_id/status)`。
- 诊断查询：`diagnosis_records(observation_id/review_status/disease_name)`。
- 时间趋势：`weather_records(farm_id, recorded_at DESC)`、`market_prices(crop_id, recorded_at DESC)`。
- 任务和审核：`farming_tasks(assignee_id/status/scheduled_date)`、`review_queue(status/assigned_to)`。
- 空间查询：农场和地块 `location` 使用 PostGIS GIST 索引。
- RAG：`knowledge_documents.embedding` 使用 pgvector `ivfflat` cosine 索引。
- `V3__optimize_query_indexes.sql` 增加 owner/status/time 等组合索引，减少列表和看板扫描。

## 5. 事务设计

Spring Service 使用 `@Transactional` 保护需要原子提交的多写操作：

- 用户注册；
- 知识文档发布、更新、归档和同步；
- 模型版本部署状态切换；
- 天气批量刷新；
- 农技审核状态更新与自动生成农事任务。

业务异常抛出后事务回滚，避免出现“审核已通过但任务未生成”等部分成功状态。MinIO 和 AI 属于外部服务，不参与数据库本地事务；系统通过状态字段、可重试异步处理和人工审核降低跨服务不一致风险。

## 6. Flyway 迁移脚本

| 版本 | 文件 | 内容 |
|---|---|---|
| V1 | `V1__init_schema.sql` | 扩展、13 张核心表、主外键、唯一约束和基础索引 |
| V2 | `V2__seed_demo_users.sql` | 四类演示账号 |
| V3 | `V3__optimize_query_indexes.sql` | 常用列表和状态查询组合索引 |
| V4 | `V4__seed_common_crops.sql` | 常见作物和品种基础数据 |
| V5 | `V5__ai_integration.sql` | 模型 Runtime 字段、模型/知识/Agent 演示数据与状态对接 |

禁止直接手工修改已执行的 migration；后续结构变化新建 `V6__*.sql`。

## 7. 数据库验收

```powershell
docker compose -f deploy/docker-compose.yml exec -T postgres psql -U yunong -d yunnong -c "select version, success from flyway_schema_history order by installed_rank;"
```

验收同时检查：主外键可用、重复图片哈希被拒绝、模型名称版本唯一、审核通过能生成关联任务、知识和模型初始化数据可查询。