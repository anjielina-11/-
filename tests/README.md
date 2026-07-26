# 测试

## 测试层次

| 层次 | 目录 | 工具 | 负责人 |
|---|---|---|---|
| 前端单元测试 | `frontend/src/views/__tests__/` 等 | Vitest + Vue Test Utils | 前端 |
| 后端单元测试 | `backend/src/test/` | JUnit 5 + Mockito | 后端 |
| AI 单元/API/RAG/Agent | `ai-service/tests/` | Pytest | AI |
| 接口集成测试 | `tests/integration/` | Pytest + requests | 三方联调 |
| 性能冒烟测试 | `tests/performance/` | Python + requests | 三方联调 |
| 浏览器验收 | 正式截图见 `docs/test-evidence/` | Playwright/人工 | 三方联调 |

## 集成测试

服务启动后运行：

```powershell
ai-service\.venv\Scripts\python.exe -m pytest tests/integration/test_api.py -q
```

覆盖知识、模型、农场、地块、种植周期、天气、图片诊断、RAG/Agent、审核、任务和反馈。

## 性能冒烟测试

```powershell
ai-service\.venv\Scripts\python.exe tests/performance/smoke_test.py `
  --requests 60 --concurrency 6 `
  --output docs/test-evidence/performance-smoke-2026-07-26.json
```

可通过 `BASE_URL`、`PERF_USERNAME`、`PERF_PASSWORD` 环境变量切换环境和账号。此脚本用于课程验收，不替代生产级容量、稳定性和安全压测。

## 完整端到端流程

```text
登录 → 创建农场/地块/种植周期 → 上传图片 → AI/RAG/Agent
→ 农技审核 → 自动任务 → 农户完成与反馈
```