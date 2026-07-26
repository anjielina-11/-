# 测试与验收报告

> 最近完整验证：2026-07-26

## 1. 自动化验证

| 层级 | 验证命令 | 最近结果 |
|---|---|---|
| Frontend 单元测试 | `npm.cmd --prefix frontend test -- --run` | 10 files，35 tests passed |
| Frontend 类型检查与构建 | `npm.cmd --prefix frontend run build` | `vue-tsc` 与 Vite build passed |
| Backend 测试 | `mvn.cmd -q -f backend/pom.xml test` | passed |
| AI 单元/API/RAG 测试 | `ai-service/.venv/Scripts/python.exe -m pytest ai-service/tests -q` | 18 passed，3 skipped |
| 跨服务接口测试 | `pytest tests/integration/test_api.py -q` | 7 passed |
| Docker Compose | config、build、health | 6/6 healthy |

AI skipped 用例用于模型文件缺失或异常环境分支，不属于失败。LangChain、Chroma、Mockito 和 npm 的现有提示属于依赖升级 warning，不是当前功能失败。

## 2. 已验证业务闭环

```text
登录 → 新建农场 → 新建地块 → 建立种植周期 → 上传病害图片
→ AI 分类 → RAG 引用与防治建议 → 农技查看原图并审核
→ 自动生成农户任务 → 农户更新任务状态 → 提交效果反馈
```

真实浏览器验收已确认：

- 点击选择和拖拽均可上传病害图片。
- 上传后能够显示原图预览和诊断状态。
- 农技详情页能够读取受保护的已上传图片。
- 审核通过后任务归属上报农户，并显示在农户任务列表。
- 新建地块时可先创建新农场，空农场数据不会阻断流程。
- 天气可手动更新并显示从当天开始的未来七天。

## 3. 安全与异常场景

- 未登录访问受保护接口会被拒绝。
- 农户不能访问其他用户的农场、诊断、图片和任务。
- 农户不能执行农技审核。
- 错误密码返回明确认证错误，不返回内部堆栈。
- 非法任务状态和无效审核状态被拒绝。
- 空列表显示空状态，不白屏。
- 图片上传失败、网络失败和 AI 失败有明确提示。
- 相对文件路径不依赖命令执行目录或个人电脑路径。

## 4. 关键证据

精简后的关键截图位于 `docs/test-evidence/2026-07-25/`：

| 文件 | 证明内容 |
|---|---|
| `full-flow-01-farmer-fields.png` | 农户地块管理 |
| `full-flow-04-crops-date-fixed.png` | 作物与种植周期 |
| `full-flow-05-ai-rag-result.png` | AI 结果、建议与 RAG 引用 |
| `full-flow-07-tech-approved.png` | 农技审核通过 |
| `full-flow-10-task-feedback-final.png` | 任务完成与反馈闭环 |

## 5. 模型评测

`ai-service/model_evaluation_report.json` 记录当前验证集结果：

| 指标 | 数值 |
|---|---:|
| 验证样本 | 31 |
| 类别数 | 18 |
| Accuracy | 83.87% |
| Weighted Precision | 79.57% |
| Weighted Recall | 83.87% |
| Weighted F1 | 80.43% |

验证集规模较小，指标只用于课程演示，不作为生产性能承诺。

## 6. 已知非阻断项

1. 未配置外部 LLM API Key 时，Agent 使用确定性建议模板，但仍附带真实 RAG 引用。
2. 数据漂移和未知样本已有 Backend 指标接口，Frontend 主要展示课程范围内的模型管理与性能信息。
3. 未执行正式并发压测，不声明生产级 P95 或 QPS。
4. 第三方依赖存在弃用 warning，当前测试和运行不受影响，后续升级应集中处理。

## 7. 现场验收入口

```powershell
docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml ps
```

- 系统入口：`http://localhost`
- Swagger：`http://localhost/swagger-ui.html`
- Backend 健康检查：`http://localhost/actuator/health`
- AI 健康检查：`http://localhost/ai/health`
