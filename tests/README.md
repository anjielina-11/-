# 测试

## 测试层次

| 层次 | 目录 | 工具 | 负责人 |
|------|------|------|--------|
| 前端单元测试 | `frontend/src/__tests__/` | Vitest | 前端 |
| 后端单元测试 | `backend/src/test/` | JUnit 5 + Mockito | 后端 |
| AI 单元测试 | `ai-service/tests/` | Pytest | AI |
| 接口集成测试 | `tests/integration/` | Pytest / REST Assured | 后端 |
| E2E 测试 | `tests/e2e/` | Playwright | 前端 |
| 性能测试 | `tests/performance/` | JMeter / Locust | 协作 |

## 集成测试
验证后端 API 和 AI 服务之间的交互、数据库读写、文件上传等。
```bash
cd tests/integration
pip install pytest requests
pytest -v
```

## E2E 测试
完整业务流程：登录 → 创建农场 → 上传图片 → 查看诊断 → 审核 → 任务完成。
```bash
cd tests/e2e
npx playwright test
```
