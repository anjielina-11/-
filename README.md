# 云南特色农业智能诊断与生产管理平台

面向云南特色农业的 AI 辅助诊断与生产管理平台，覆盖“地块 → 作物 → 图片 → 模型 → RAG → Agent → 审核 → 任务 → 效果跟踪”业务闭环。

## 技术架构

| 层 | 技术 |
|---|---|
| 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus、ECharts |
| 业务后端 | Spring Boot 3、MyBatis-Plus、Flyway、OpenAPI |
| AI 服务 | FastAPI、PyTorch ResNet50、LangChain、ChromaDB |
| 基础设施 | PostgreSQL/PostGIS/pgvector、Redis、MinIO、Nginx |
| 测试 | Vitest、Playwright、JUnit 5、Pytest |

## 一键启动

环境要求：Docker Desktop。

```powershell
docker compose -f deploy/docker-compose.yml up -d --build
```

启动完成后访问：

| 服务 | 地址 |
|---|---|
| 系统入口 | http://localhost |
| Swagger UI | http://localhost/swagger-ui.html |
| OpenAPI JSON | http://localhost/api-docs |
| Backend 健康检查 | http://localhost/actuator/health |
| AI 健康检查 | http://localhost/ai/health |

除 Nginx 前端入口外，Backend、AI、PostgreSQL、Redis 和 MinIO 均只在 Compose 内部网络开放。

## 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 农户 | `farmer` | `farmer123` |
| 农技人员 | `tech` | `tech123` |
| 合作社管理人员 | `coop` | `coop123` |
| 管理员 | `admin` | `admin123` |

## 核心演示流程

1. 农户创建农场、地块和种植档案。
2. 农户上传病害图片，Backend 保存至 MinIO 并异步调用 AI 服务。
3. AI 完成 18 类病害分类、未知拒识、RAG 检索和 Agent 建议生成。
4. 农技人员审核诊断，审核通过后自动生成农户防治任务。
5. 农户执行任务、更新状态并提交防治效果反馈。

## 本地验证

```powershell
# Frontend
cd frontend
npm.cmd test
npm.cmd run build
npm.cmd run test:e2e

# Backend
cd ..\backend
mvn.cmd test
mvn.cmd package -DskipTests

# AI
cd ..\ai-service
.\.venv\Scripts\python.exe -m pytest -q

# Integration
cd ..
.\ai-service\.venv\Scripts\python.exe -m pytest tests/integration/test_api.py -q
```

## 文档

- `docs/requirements_specification.md`：需求规格说明书
- `docs/system_design.md`：系统设计、ER 图、时序图、部署图
- `docs/API_DOCS.md`：接口、鉴权和错误码
- `docs/test_report.md`：最新测试报告
- `docs/audit_report.md`：对照《期末任务》的验收审计报告
- `docs/git_workflow.md`：Git 协作规范及本地可核验记录说明

## 配置说明

可复制 `deploy/.env.example` 后修改数据库、Redis、MinIO、JWT 和外部 LLM/API 配置。未配置 `LLM_API_KEY` 时，Agent 使用确定性建议模板，并将真实 RAG 检索内容附入建议供农技人员审核。
