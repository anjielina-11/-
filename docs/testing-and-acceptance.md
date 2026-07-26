# 测试与验收报告

> 最近完整验收日期：2026-07-26

## 1. 自动化测试

| 层级 | 验证命令 | 最近结果 |
|---|---|---|
| Frontend 单元测试 | `npm.cmd --prefix frontend test -- --run` | 12 files，49 tests passed |
| Frontend 类型检查 | `npm.cmd exec -- vue-tsc --noEmit -p tsconfig.app.json`（在 `frontend/` 执行） | passed |
| Frontend 构建 | `npm.cmd --prefix frontend run build` | Vite build passed |
| Backend JUnit/Mockito | `mvn.cmd -q -f backend/pom.xml test` | 69 tests passed |
| AI 单元/API/RAG/Agent | `ai-service/.venv/Scripts/python.exe -m pytest ai-service/tests -q` | 31 passed，3 skipped |
| 跨服务接口集成 | `ai-service/.venv/Scripts/python.exe -m pytest tests/integration/test_api.py -q` | 10 passed |
| Docker Compose | `docker compose -f deploy/docker-compose.yml ps` | 6/6 healthy |

AI skipped 用例用于模型文件缺失或异常环境分支，不属于失败。Mockito、LangChain/Chroma、npm 和 Vite 的现有提示为依赖升级 warning，不影响课程交付；本轮不冒险升级整套依赖。

## 2. 功能与接口集成测试

已自动和人工验证以下闭环：

```text
登录 → 新建农场 → 新建地块 → 选择作物与生育期 → 上传病害图片
→ 模型分类 → RAG 引用 → Agent 综合未来天气与生育期
→ 农技查看原图并审核 → 自动生成任务 → 农户完成任务 → 效果反馈
```

Integration 10 项覆盖：Backend/AI 健康、知识发布同步与 RAG、模型 Runtime 与部署、农场/地块/种植周期、未来七天天气、唯一图片上传、结构化 Agent 上下文、受保护图片、农技审核、任务和效果反馈。

浏览器验收确认：

- 点击选择和拖拽均可上传；非图片类型由 Backend 拒绝。
- 农技结果详情页显示原图、作物、品种、生育期、七天天气、四个 Agent 和 RAG 引用。
- 管理员知识库显示真实文档并可同步；模型页显示当前 Runtime、18 类、路径和评测指标。
- 新建地块时可先创建农场；空数据、网络错误和对象存储错误均有提示。
- 关键页面 Console 0 errors，关键 API 与图片请求返回 200。

## 3. 数据库测试

| 检查项 | 结果 |
|---|---|
| Flyway `V1`–`V6` | 全部 `success=true` |
| 主外键关系 | 农场—地块—种植周期—观察—诊断—任务链路写入正常 |
| 唯一约束 | 用户名、手机号、图片哈希、模型名称版本、审核队列诊断唯一 |
| 索引 | 基础、组合、PostGIS GIST、pgvector ivfflat 索引已创建 |
| 事务 | 审核通过与任务生成使用同一事务；知识、模型、天气等多写操作可回滚 |
| 初始化数据 | 常见作物、四类账号、知识文档、模型 Runtime 和 Agent 配置可查询 |

数据库结构与命令见 [database-design.md](database-design.md)。

## 4. AI 效果评测

`ai-service/model_evaluation_report.json` 当前记录：

| 指标 | 数值 |
|---|---:|
| 验证样本 | 31 |
| 类别数 | 18 |
| Accuracy | 83.87% |
| Weighted Precision | 79.57% |
| Weighted Recall | 83.87% |
| Weighted F1 | 80.43% |

本次完整诊断还验证：`growth_stage=tillering`、中文标签“分蘖期”、未来天气 7 天、四条 Agent trace 均为 `completed`。验证集规模较小，指标只用于课程演示，不作为生产性能承诺。

## 5. 性能测试

执行课程验收级 API 性能冒烟测试：

```powershell
ai-service\.venv\Scripts\python.exe tests/performance/smoke_test.py `
  --requests 60 --concurrency 6 `
  --output docs/test-evidence/performance-smoke-2026-07-26.json
```

测试对象轮询 Backend health、诊断列表和模型 Runtime 三类读接口；本机 Docker Desktop 热身环境结果：

| 指标 | 结果 |
|---|---:|
| 请求数 / 并发 | 60 / 6 |
| 成功率 | 100%（60/60） |
| 吞吐量 | 256.12 requests/s |
| Median | 16.81 ms |
| P95 | 42.91 ms |
| Max | 55.89 ms |

结果文件：`docs/test-evidence/performance-smoke-2026-07-26.json`。该数据仅证明课程演示环境无明显阻塞，不等同于生产压测，也不承诺公网 QPS 或 SLA。

## 6. 安全、异常与 Bug 排查

- 错误密码、未登录、越权资源和越权审核均被拒绝。
- 图片校验空文件、MIME、扩展名、大小和重复哈希。
- 非法审核/任务状态被拒绝；表单防重复提交。
- 空列表不白屏；AI、网络、MinIO 失败返回可读提示。
- 跨服务日期统一序列化为 `yyyy-MM-dd`，避免 FastAPI 422。
- 结果 API 暴露真实 `contextSummary` 与 `agentTrace`，前端不再用模拟数据。
- 已清除源码中的历史连续问号编码损坏字符串。

安全边界见 [security-and-audit.md](security-and-audit.md)。

## 7. 关键截图证据

正式证据位于 `docs/test-evidence/2026-07-25/`：

| 文件 | 证明内容 |
|---|---|
| `full-flow-01-farmer-fields.png` | 农户地块管理 |
| `full-flow-04-crops-date-fixed.png` | 作物与种植周期 |
| `full-flow-05-ai-rag-result.png` | AI 结果、建议与 RAG 引用 |
| `full-flow-07-tech-approved.png` | 农技审核通过 |
| `full-flow-10-task-feedback-final.png` | 任务完成与反馈闭环 |

本轮浏览器还验证了农技 Agent 上下文、管理员知识库和模型 Runtime；根目录临时截图不作为正式交付文件保留。

## 8. 已知非阻断项

1. 未配置外部 LLM API Key 时，Agent 使用确定性建议模板，但保留真实上下文与 RAG 引用。
2. 模型数据规模和评测样本有限，不宣称生产级准确率或跨地区泛化能力。
3. 当前性能测试为本机课程冒烟，不是长期稳定性、峰值容量或公网压测。
4. 审计日志写入容器日志，生产环境需接入集中日志和不可篡改存储。
5. 第三方依赖存在弃用 warning，当前测试和运行不受影响。

## 9. 现场验收入口

```powershell
docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml ps
```

- 系统：`http://localhost`
- Swagger：`http://localhost/swagger-ui.html`
- Backend Health：`http://localhost/actuator/health`
- AI Health：`http://localhost/ai/health`