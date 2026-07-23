# 云南特色农业智能诊断与生产管理平台

## 项目简介

面向云南特色农业的 AI 辅助诊断与生产管理平台。农户上传作物图片 → AI 识别病害 → RAG 检索农技规范 → Agent 生成防治建议 → 农技人员审核 → 生成农事任务 → 跟踪处置效果。

## 目录结构

```
project/
├── frontend/          ← 前端 (Vue 3 + Vite + Element Plus)
│   └── src/views/        16 个页面 (Dashboard/FarmList/DiseaseUpload/...)
│
├── backend/           ← 业务后端 (Spring Boot + MyBatis-Plus)
│   └── src/main/java/com/yunong/module/
│       ├── auth/         认证模块 (登录/注册/JWT)
│       ├── farm/         农场 & 地块管理
│       ├── crop/         作物 & 种植周期
│       ├── diagnosis/    病害诊断 (上传/推理/审核)
│       ├── task/         农事任务
│       ├── knowledge/    知识库
│       ├── market/       市场价格
│       ├── weather/      天气数据
│       ├── monitor/      模型监控 (数据漂移/未知样本)
│       ├── model/        模型版本管理
│       ├── agent/        Agent 运行记录
│       ├── review/       审核队列
│       └── user/         用户管理
│
├── ai-service/        ← AI 服务 (Python + FastAPI)
│   └── src/
│       ├── api/          diagnosis.py / rag.py / weather.py
│       ├── services/     agent_service / rag_service / inference_service
│       ├── core/         config.py
│       ├── models/       schemas.py
│       └── tests/
│
├── data-pipeline/     ← 数据管道 (定时采集天气/市场/通报)
├── deploy/            ← Docker Compose (PG + Redis + MinIO + Backend)
├── tests/             ← 集成 & E2E 测试
└── docs/              ← 需求文档 / API文档 / 设计文档
```

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + TypeScript + Vite + Pinia + Element Plus + ECharts |
| 业务后端 | Spring Boot 3 + MyBatis-Plus + Flyway + Swagger |
| AI 服务 | Python + FastAPI + LangChain + ChromaDB + sentence-transformers |
| 数据库 | PostgreSQL 16 + PostGIS 3 + pgvector |
| 缓存 | Redis 7 |
| 文件存储 | MinIO |
| 部署 | Docker Compose |

## 环境要求

> 每人按自己负责的模块安装，装完打钩。版本统一避免出问题。

### 所有人必备

| 工具 | 版本 | 安装方式 | 已装? |
|------|------|------|:--:|
| Git | 最新 | `winget install Git.Git` 或 [git-scm.com](https://git-scm.com) | |
| Docker Desktop | 最新 | `winget install Docker.DockerDesktop` 或 [docker.com](https://docker.com) | |

### 后端 (backend/)

| 工具 | 版本 | 安装方式 | 已装? |
|------|------|------|:--:|
| JDK | **21** | `winget install EclipseAdoptium.Temurin.21.JDK` | |
| Maven | 3.9+ | `winget install Apache.Maven.3` 或用 `backend/mvnw` | |

### 前端 (frontend/)

| 工具 | 版本 | 安装方式 | 已装? |
|------|------|------|:--:|
| Node.js | **20 LTS** | `winget install OpenJS.NodeJS.LTS` 或 [nodejs.org](https://nodejs.org) | |
| npm | 10+ | 自带于 Node.js | |

### AI 服务 (ai-service/)

| 工具 | 版本 | 安装方式 | 已装? |
|------|------|------|:--:|
| Python | **3.9+** | `winget install Python.Python.3.12` 或 [python.org](https://python.org) | |
| pip | 最新 | 自带于 Python | |

> 装完 `cd ai-service && pip install -r requirements.txt` 即可。

---

## 当前进度

> 请在自己负责的模块后面打钩，改了东西就更新这个表。

| 模块 | 负责人 | 状态 | 还差什么 |
|------|:--:|:--:|------|
| `backend/` 业务后端 | 王艺霖 | 🟢 基本完成 | 配合前端联调, 补测试 |
| `frontend/` 前端 | | 🟡 页面完成 | 关 mock 对接真实 API, 补测试 |
| `ai-service/` AI服务 | | 🟡 代码完成 | 补模型权重(.pth), 补知识库文档 |
| `data-pipeline/` 数据管道 | | 🔴 未开始 | 天气/市场/通报定时采集脚本 |
| `tests/` 测试 | | 🔴 未开始 | 单元/集成/E2E/性能测试 |
| `deploy/` 部署 | 王艺霖 | 🟢 基础设施可用 | 补前端/AI容器化 |

## 快速开始

### 1. 启动基础设施 + 后端

```bash
cd deploy
docker compose up -d
```

### 2. 启动前端 (开发模式)

```bash
cd frontend
npm install
npm run dev
```

- Mock 模式 (默认): `VITE_MOCK_LOGIN=true` → 假数据登录, 不调后端
- 真实模式: 改 `.env` 为 `VITE_MOCK_LOGIN=false` → 调后端 API

### 3. 启动 AI 服务 (开发模式)

```bash
cd ai-service
pip install -r requirements.txt
uvicorn src.main:app --reload --port 8000
```

### 4. 访问

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 Swagger | http://localhost:8080/swagger-ui.html |
| AI 服务 | http://localhost:8000 |
| MinIO 控制台 | http://localhost:9001 |

## 修改记录

> 每次改完代码顺手记一行，方便其他人知道发生了什么。

| 日期 | 谁 | 改了什么 |
|------|:--:|------|
| 07-23 | 王艺霖 | 后端完成, 项目结构调整, 合并前端组员代码, admin 密码 hash 修复 |
| 07-22 | | 前端 16 页面完成 (mock 模式) |
| 07-22 | | AI 服务代码提交 (FastAPI + RAG + Agent) |
