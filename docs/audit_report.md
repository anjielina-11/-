# 《期末任务》项目验收审计报告

审计日期：2026-07-24
任务原文：`C:\Users\wangy\Downloads\期末任务.docx`

## 1. 完成度结论

按《期末任务》列出的 24 个可在本地代码/运行环境核验的功能和工程项统计：

- 完全实现：19 项
- 部分实现：5 项
- 未实现：0 项
- 本地可核验完成度：19 / 24 = 79.2%

核心业务闭环、四类角色、技术栈、数据库表、Docker Compose、测试和主要文档均已达标。部分项主要是监控/采集 UI、正式性能压测和外部协作证据，不阻断现场核心演示。

## 2. 逐项验收

| 文档要求 | 状态 | 证据/说明 |
|---|---|---|
| Spring Boot + Python AI 服务解耦 | 完全 | `backend/` + `ai-service/` |
| 四类用户角色 | 完全 | 农户、农技、合作社、管理员登录和路由鉴权 |
| 农场、地块、种植档案 | 完全 | Frontend CRUD + Backend ownership 校验 |
| 图片上传及结果可视化 | 完全 | MinIO、异步推理、中文病害名、置信度和建议 |
| 农事日历和待办任务 | 完全 | 任务列表、日历接口、状态流转 |
| 天气、病害和市场趋势 | 完全 | 合作社真实 API 看板和市场页 |
| 防治建议与规范原文对照 | 完全 | RAG citations 在结果详情展示 |
| 模型性能/漂移/未知样本监控 | 部分 | Backend API 已有，模型 UI 有性能图；漂移和未知样本无独立页面，部分指标为演示值 |
| 异步上传、推理和通知 | 完全 | AsyncDiagnosisService + 前端轮询/通知 |
| 审核后生成任务并跟踪效果 | 完全 | 已执行真实 E2E 闭环 |
| 定时采集天气/市场/通报 | 部分 | 天气和市场有定时服务/脚本；农业通报仅知识文档导入，无独立采集调度 |
| 图像模型、RAG、Agent 调用 | 完全 | AI 分类、20 chunks 向量库、真实引用、fallback 引用 RAG |
| 未知拒识与人工审核 | 完全 | confidence threshold、need_review、failed 流转 |
| 模型/数据/知识库版本 | 部分 | 模型和知识库版本完整；数据集缺少独立版本实体/UI |
| 核心数据库表 | 完全 | 文档列出的 14 张核心表均在 Flyway schema 中 |
| MinIO 保存图片、DB 保存地址和哈希 | 完全 | 已联调验证 |
| 需求规格说明书 | 完全 | `docs/requirements_specification.md` |
| 系统设计说明书 | 完全 | `docs/system_design.md` 含 Mermaid 图 |
| 接口文档 | 完全 | `docs/API_DOCS.md` + Swagger/OpenAPI |
| 数据库设计与迁移 | 完全 | Flyway、主外键、唯一约束和索引 |
| 测试报告 | 完全 | `docs/test_report.md` |
| Docker Compose 一键启动 | 完全 | 6 个服务 healthy，仅暴露 `80` |
| Git Issue/PR/评审记录 | 部分 | 有 `docs/git_workflow.md`；远程平台历史不能由本地代码补造 |
| 正式性能测试 | 部分 | 已做功能/Smoke；未执行可引用的并发压测 |

## 3. 本次修复重点

- 统一 Frontend API 解包、角色映射、真实 CRUD 和状态枚举。
- 修复 Farm/Field/Cycle/Diagnosis/Task 的资源归属检查和 IDOR 风险。
- 修复公开注册越权、用户角色与状态管理、JWT/错误密码异常映射。
- 打通 Backend → MinIO → AI multipart 分类、RAG、Agent、审核、任务和反馈。
- 修复 RAG 自动初始化、持久化、测试隔离和无 Key fallback 忽略引用问题。
- 修复结果页英文 code、Markdown 原文、无 ID 空白页、failed 状态误审核和无限轮询。
- 修复 Compose 密码、healthcheck、MinIO 固定版本、内部端口暴露和演示账号。

## 4. 当前遗留项

1. 管理员无独立“采集任务”管理页面，采集由 Backend `@Scheduled` 和 `data-pipeline/` 脚本执行。
2. 漂移和未知样本监控 API 可用，但尚未做独立 Frontend 页面，且部分监控值为演示常量。
3. 未配置外部 LLM Key 时使用确定性建议模板；已保证附带真实 RAG 规范内容，但不等同于在线大模型生成。
4. 训练/验证数据规模较小，真实验证集 Accuracy 为 83.87%，不能用于生产承诺。
5. GitHub/GitLab Issue、PR 和代码评审记录以及正式性能压测结果需由团队在远程平台/答辩材料中提供。

## 5. 启动与验收入口

```powershell
docker compose -f deploy/docker-compose.yml up -d --build
```

- 系统：http://localhost
- Swagger：http://localhost/swagger-ui.html
- 演示账号见根目录 `README.md`
- 浏览器与核心 E2E 已完成本地验收，结果汇总见 `docs/test_report.md`
