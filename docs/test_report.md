# 云南特色农业智能诊断与生产管理平台测试报告

测试日期：2026-07-24

## 1. 最新结果

| 层级 | 命令/工具 | 结果 |
|---|---|---|
| Frontend 单元测试 | `npm.cmd test` | 16 passed |
| Frontend 类型检查与构建 | `npm.cmd run build` | passed |
| Browser E2E | `npm.cmd run test:e2e` | 2 passed |
| Backend 单元测试 | `mvn.cmd test` | 38 passed |
| Backend 打包 | `mvn.cmd package -DskipTests` | passed |
| AI 单元/API/RAG 测试 | `pytest -q` | 13 passed, 3 skipped |
| 统一入口集成测试 | `pytest tests/integration/test_api.py -q` | 7 passed |
| Docker Compose | config/build/up/health | 6/6 healthy |

AI 的 3 个 skipped 用例用于模型文件缺失/异常环境分支，不属于失败。

## 2. 已验证业务闭环

真实 API + MinIO + AI + RAG + PostgreSQL 流程已执行：

`创建四类账号 → 农场 → 地块 → 作物 → 种植周期 → 图片上传 → AI 分类 → RAG 引用 → Agent 建议 → 农技审核 → 自动生成农户任务 → 完成任务 → 效果反馈`

关键结果：

- 识别类别：`rice_blast`，前端显示“水稻稻瘟病”
- 置信度：`0.9958`
- RAG 引用：3 条真实知识库片段
- 诊断 ID：`1c6b6f7b-a398-4fc8-87d2-9679e034f8f3`
- 任务 ID：`38dae9ee-b264-4495-8282-7b45db253c16`

## 3. 安全与异常测试

以下场景均已验证：

- 未登录访问诊断接口被拒绝
- 农户不能访问其他农户的农场、诊断和任务
- 农户不能执行农技审核
- 非法任务状态被拒绝
- 错误密码返回业务错误，不再返回 HTTP 500
- 特殊字符、中文和 UTF-8 反馈正常保存
- 空数据页面显示空状态，不白屏
- failed 诊断停止轮询且不显示审核操作

## 4. 浏览器验收

已对登录页、农户地块/上传/任务、农技审核/结果列表/结果详情、合作社看板、管理员用户/知识/模型页面执行 accessibility snapshot、截图和 Console 检查。最新关键页面 Console 为 0 error / 0 warning；截图等本地验收产物不提交至 Git 仓库。

## 5. 模型评测

`ai-service/model_evaluation_report.json` 为真实验证集评测结果：

| 指标 | 数值 |
|---|---:|
| 验证样本 | 31 |
| 类别数 | 18 |
| Accuracy | 83.87% |
| Weighted Precision | 79.57% |
| Weighted Recall | 83.87% |
| Weighted F1 | 80.43% |

验证集较小，以上指标只用于课程演示，不能视为生产泛化能力证明。

## 6. 性能与非阻断项

- 核心接口 Smoke 响应正常，图片诊断耗时受 CPU 模型推理影响。
- 尚未执行正式并发压测，因此不声明 P95/QPS 指标。
- Vite 构建存在约 1.1 MB vendor chunk warning，但页面加载未超过 3 秒验收阈值，暂不做高风险拆包重构。
- LangChain/Chroma/FastAPI 存在依赖弃用 warning，当前不影响功能，建议后续版本升级时统一迁移。
- Mockito 动态 agent 和 Spring Boot layertools 有未来版本 warning，当前构建通过。
