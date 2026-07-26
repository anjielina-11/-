# 云农智诊全链路 AI 对接设计

**日期：** 2026-07-26  
**依据：** 《期末任务.docx》管理员、后端、RAG、多 Agent、版本维护及完整业务闭环要求  
**目标：** 将现有独立可运行的种植、天气、图像识别、RAG、Agent、知识库管理和模型版本管理能力连接成可追踪、可管理、可演示的完整闭环。

## 1. 范围与验收目标

本次实现以下交付能力：

1. 病害诊断从种植周期读取真实作物、品种、种植日期和生育期。
2. 诊断通过地块所属农场读取当天起未来七天天气，并交给 AI Agent。
3. AI Service 通过天气分析、生育期分析、RAG 检索和防治建议四个逻辑 Agent 生成可追踪结果。
4. 未配置外部 LLM API 时，本地 fallback 仍使用真实作物、生育期、天气和 RAG 引用生成确定性建议。
5. PostgreSQL `knowledge_documents` 作为管理员知识数据源，发布内容同步到 AI Service 的 Chroma 向量库。
6. 知识草稿不进入 RAG；发布、修改、归档后触发全量一致性同步。
7. PostgreSQL `model_versions` 作为模型注册数据源，AI Service 暴露 Runtime 查询和激活接口。
8. 管理端能登记模型路径和评测指标、执行部署，并显示真实部署状态。
9. Flyway 初始化现有六份知识文档和当前 ResNet50 模型版本，空数据库启动后管理页面即可演示。
10. `agent_runs` 和诊断结果保存实际上下文、Agent trace、RAG 引用和最终建议。

本次不实现在线上传 90MB 模型权重、分布式消息队列、模型训练调度和自动回滚；这些超出期末项目必要范围。模型文件继续通过 Docker volume 或镜像文件提供，管理端负责注册、验证和激活。

## 2. 总体架构

Backend 是业务和管理数据的 source of truth；AI Service 是推理、RAG 和 Agent runtime；Frontend 只通过 Backend 管理业务，不直接调用 AI Service。

```text
Frontend
   │
   ▼
Spring Boot Backend ─────────────── PostgreSQL / MinIO
   │                                   │
   │ AgentAdviceRequest                │ knowledge_documents
   │ RagSyncRequest                    │ model_versions
   │ ModelActivateRequest              │ agent_runs
   ▼                                   │
FastAPI AI Service ─────────────── Chroma / PyTorch Runtime
```

所有跨服务接口采用 JSON DTO，不通过共享文件路径读取 Backend 数据库内容。Backend 对外保持 `/api/v1`，AI Service 继续使用内部 `/api/v1` 接口。

## 3. Agent 上下文与多 Agent 编排

### 3.1 Backend 上下文装配

新增独立 `DiagnosisContextService`，根据 `diagnosisId` 装配：

- `DiagnosisRecord`：病害名称、置信度、图片地址；
- `Observation`：种植周期 ID、观察时间；
- `PlantingCycle`：种植日期、生育期；
- `Crop`：名称、品种、生长周期；
- `Field`：地块名称、农场 ID；
- `Farm`：农场名称、位置；
- `WeatherRecord`：当天起七天预报。

上下文缺少非关键数据时使用明确的“未知”或空数组，不中断图像识别；种植周期不存在属于数据一致性错误，诊断进入 failed 并记录原因。

### 3.2 AI 请求契约

Backend 调用 `/api/v1/diagnosis/advice` 时发送结构化请求：

```json
{
  "disease_name": "rice_blast",
  "confidence": 0.92,
  "crop": {
    "name": "水稻",
    "variety": "滇粳验收",
    "planting_date": "2026-07-25",
    "growth_stage": "sowing"
  },
  "field": {
    "name": "A-01地块",
    "farm_name": "验收农场"
  },
  "weather_forecast": [
    {
      "date": "2026-07-26",
      "weather": "阵雨",
      "temperature": 21.8,
      "humidity": 80.6,
      "rainfall": 4.2
    }
  ],
  "citations": []
}
```

### 3.3 逻辑 Agent

AI Service 内部按固定顺序执行四个逻辑 Agent：

1. `WeatherRiskAgent`：识别高湿、降雨、高温等施药和扩散风险；
2. `GrowthStageAgent`：把 `sowing/seedling/tillering/flowering/fruiting/maturity` 映射为中文生育期并生成阶段注意事项；
3. `RagEvidenceAgent`：整理检索资料、来源和适用措施；
4. `TreatmentAgent`：把病害、天气、生育期和 RAG 证据组合成最终建议。

结果返回：

```json
{
  "advice": "Markdown建议",
  "references": [],
  "context_summary": {},
  "agent_trace": [
    {"agent": "weather-risk", "status": "completed", "summary": "未来两天高湿有雨"},
    {"agent": "growth-stage", "status": "completed", "summary": "当前为播种期"},
    {"agent": "rag-evidence", "status": "completed", "summary": "引用3条规范"},
    {"agent": "treatment", "status": "completed", "summary": "已生成综合建议"}
  ]
}
```

外部 LLM 可用时，`TreatmentAgent` 调用 LLM；不可用或调用失败时，使用确定性模板，但必须包含真实天气风险、生育期、病害和 RAG 来源，不能退化成与上下文无关的通用文本。

### 3.4 追踪与任务生成

`agent_runs.input_json` 保存结构化请求，`output_json` 保存 `context_summary`、`agent_trace` 和最终建议。诊断 `ai_result` 保存相同 trace 和引用。农技员审核通过后，现有任务生成逻辑继续使用最终建议作为任务描述。

## 4. 知识库管理与 RAG 同步

### 4.1 数据源规则

- PostgreSQL `knowledge_documents` 是管理数据源；
- 只有 `status=published` 且未逻辑删除的文档进入 RAG；
- `version` 创建时为 1；标题、正文、分类、标签或状态发生变化时递增；
- 状态允许 `draft/published/archived`；
- 管理端“删除”使用归档，不物理删除。

### 4.2 同步接口

AI Service 新增：

```http
PUT /api/v1/rag/documents
```

请求为当前全部已发布文档。AI Service 为每条文档生成稳定 metadata：

```json
{
  "document_id": "uuid",
  "title": "水稻病害规范",
  "category": "disease",
  "version": 2,
  "tags": ["水稻", "稻瘟病"]
}
```

RAG Service 在内存中完成文档切分和 embedding，成功后替换 Chroma collection。请求为空数组时清空管理型知识集合。同步失败时 Backend 抛出明确业务错误并回滚当前知识修改，避免页面显示已发布但 AI 未更新。

### 4.3 启动一致性

Backend 启动完成后执行一次非阻塞同步：

- 成功：记录同步文档数；
- AI Service 尚未就绪：记录 warning，不阻止 Backend 启动；
- 后续管理员发布或修改知识时再次同步并恢复一致性。

AI Service 仍保留目录导入接口用于测试和离线维护，但正式管理链路使用数据库文档同步接口。

## 5. 模型版本与 Runtime

### 5.1 状态和字段

统一状态：

- `training`：训练或尚未部署；
- `deployed`：当前运行版本；
- `deprecated`：已废弃版本。

模型记录字段包括：名称、类型、版本、Accuracy、Precision、Recall、F1、模型路径、类别映射路径、配置 JSON、状态、部署时间和描述。

### 5.2 AI Runtime 接口

新增：

```http
GET  /api/v1/models/runtime
POST /api/v1/models/activate
```

激活请求包含 `model_path`、`class_to_idx_path`、`num_classes` 和 `confidence_threshold`。AI Service 必须：

1. 将路径限制在 AI Service 根目录或配置允许目录内；
2. 验证模型和类别映射文件存在；
3. 实例化 `DiseaseClassifier` 完成真实加载验证；
4. 验证成功后原子替换当前 runtime 配置；
5. 失败时保留旧模型并返回错误。

诊断接口通过 `ModelRuntimeService.get_classifier()` 获取当前 classifier，不再每次自行使用固定配置创建无追踪实例。

### 5.3 Backend 部署语义

Backend `/model-versions/{id}/deploy` 先调用 AI Runtime 激活接口。成功后在同一数据库事务内：

- 当前版本设为 `deployed` 并记录 `deployedAt`；
- 同模型名的其他 `deployed` 版本设为 `deprecated`；
- 返回部署后的版本。

AI 激活失败时数据库状态不改变。普通更新接口禁止直接把任意模型伪装为 `deployed`，部署必须走 deploy API。

### 5.4 Frontend

保持当前视觉样式，只补充：

- Precision 输入和展示；
- 模型路径、类别映射路径；
- “部署”按钮及确认；
- Runtime 当前模型标识；
- 与 Backend 一致的状态枚举和错误提示。

## 6. 初始化数据

新增 Flyway migration，幂等插入：

1. 当前模型：`云农病害识别 ResNet50`、`v1.0.0`、18 类、Accuracy 0.8387、Precision 0.7957、Recall 0.8387、F1 0.8043、路径 `/app/best_model.pth`、类别映射 `/app/class_to_idx.pth`、状态 `deployed`；
2. 六份现有农技知识文档，内容来自 `ai-service/knowledge_docs`，状态为 `published`、版本为 1。

唯一约束和 `ON CONFLICT` 确保已有数据库不会重复插入。

## 7. 错误处理与安全

- 跨服务错误统一转换为 Backend 业务错误，不向前端暴露内部堆栈；
- RAG 同步和模型部署设置连接、读取超时；
- 模型路径必须经过归一化和目录边界检查，禁止 `../` 逃逸；
- Agent 缺少天气时继续生成建议，但 trace 标记 `no-data`；
- Agent 缺少 RAG 时继续生成建议，但明确要求农技员复核；
- 数据库更新与远程同步失败采用事务回滚；
- 管理接口继续使用现有 ADMIN/TECHNICIAN 权限控制。

## 8. 测试策略

### Backend

- `DiagnosisContextServiceTest`：完整上下文、无天气、缺少非关键关联；
- `AsyncDiagnosisServiceTest`：真实上下文进入 Agent 请求和 `agent_runs`；
- `KnowledgeServiceTest`：状态、版本递增、归档、同步失败回滚；
- `ModelVersionServiceTest`：部署成功、AI 激活失败、其他版本状态迁移；
- migration test：初始化数据存在且幂等。

### AI Service

- Agent 测试：天气风险、生育期映射、fallback 使用真实上下文、trace 顺序；
- RAG 测试：数据库文档同步、空集清理、metadata、检索新内容；
- Runtime 测试：合法激活、路径逃逸拒绝、文件缺失、加载失败保留旧模型；
- API schema 测试：请求和响应字段稳定。

### Frontend

- Domain mapper 测试：比例和百分比；
- 管理页面测试：知识状态、模型字段、部署 API、错误提示；
- `vue-tsc --noEmit` 和生产 build。

### 集成与浏览器验收

1. Docker 重建并确认六个服务健康；
2. 管理员页面显示六份知识和一个已部署模型；
3. 新增一条唯一测试知识，RAG 能检索；
4. 修改或归档后，RAG 结果同步变化；
5. 上传病害图片并等待诊断完成；
6. 检查结果包含作物、生育期、天气、RAG 引用和 Agent trace；
7. 农技员审核通过后生成任务；
8. Playwright 同时检查 snapshot、screenshot、Console 和 Network；
9. 运行 Backend、AI、Frontend、integration 全套测试。

## 9. 成功标准

只有同时满足以下条件才可宣布完成：

- 管理员知识库和模型页面不再为空；
- 管理端知识变更真实影响 RAG；
- 模型部署真实影响 AI Runtime 状态；
- 诊断建议可证明使用真实天气和生育期；
- 多 Agent trace 可查询；
- 审核、任务生成和处置链路无回归；
- test、typecheck、build、Docker、数据库联调和浏览器验收全部通过。
