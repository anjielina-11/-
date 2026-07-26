# 云南特色农业智能诊断与生产管理平台

面向云南特色农业的 AI 辅助诊断与生产管理平台，完整覆盖：

```text
农场/地块 → 种植档案 → 病害图片 → AI 分类 → RAG 建议
→ 农技审核 → 防治任务 → 农户反馈
```

## 项目结构

```text
frontend/       Vue 3 + TypeScript 前端
backend/        Spring Boot 业务后端
ai-service/     FastAPI + PyTorch + RAG AI 服务
data-pipeline/  数据采集脚本
deploy/         Docker Compose 与数据库初始化
tests/          跨服务集成测试
docs/           正式交付文档与关键测试证据
```

## 技术栈

| 模块 | 技术 |
|---|---|
| Frontend | Vue 3、TypeScript、Vite、Pinia、Element Plus、ECharts |
| Backend | Spring Boot 3、Java 21、MyBatis-Plus、Flyway、OpenAPI |
| AI | FastAPI、PyTorch ResNet50、LangChain、ChromaDB |
| Infrastructure | PostgreSQL/PostGIS/pgvector、Redis、MinIO、Nginx |
| Test | Vitest、Playwright、JUnit 5、Pytest |

## 一键启动

前置条件：Docker Desktop 已启动。

```powershell
docker compose -f deploy/docker-compose.yml up -d --build
```

查看状态：

```powershell
docker compose -f deploy/docker-compose.yml ps
```

| 入口 | 地址 |
|---|---|
| 系统 | `http://localhost` |
| Swagger UI | `http://localhost/swagger-ui.html` |
| OpenAPI JSON | `http://localhost/api-docs` |
| Backend Health | `http://localhost/actuator/health` |
| AI Health | `http://localhost/ai/health` |

## 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 农户 | `farmer` | `farmer123` |
| 农技人员 | `tech` | `tech123` |
| 合作社管理人员 | `coop` | `coop123` |
| 管理员 | `admin` | `admin123` |

## 推荐演示流程

1. 使用农户账号创建农场、地块和种植周期。
2. 进入病害上报，点击或拖拽上传图片并提交诊断。
3. 查看 AI 病害名称、置信度、防治建议和知识库引用。
4. 使用农技账号进入审核详情，查看原图并审核通过。
5. 切回农户账号，在任务列表完成自动生成的防治任务并提交效果反馈。
6. 使用合作社账号展示天气、市场和生产看板；使用管理员账号展示用户、知识库和模型管理。

## 本地验证

```powershell
# Frontend
npm.cmd --prefix frontend test -- --run
npm.cmd --prefix frontend run build

# Backend
mvn.cmd -q -f backend/pom.xml test

# AI
.\ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests -q

# Integration（服务启动后）
.\ai-service\.venv\Scripts\python.exe -m pytest tests/integration/test_api.py -q
```

## 文档导航

正式交付文档统一位于 [`docs/README.md`](docs/README.md)：

- 需求规格
- 系统架构
- API 索引
- 测试与验收
- 小组协作

## 配置

可复制 `deploy/.env.example` 后修改数据库、Redis、MinIO、JWT 和外部 LLM/API 配置。未配置 `LLM_API_KEY` 时，Agent 使用确定性建议模板，并保留真实 RAG 检索引用。
