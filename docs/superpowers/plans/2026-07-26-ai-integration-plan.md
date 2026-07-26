# 云农智诊 AI 全链路对接 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将图像识别、真实作物/生育期/七天天气、RAG、多 Agent、知识库管理和模型 Runtime 串成可管理、可追踪、可演示的完整业务闭环。

**Architecture:** Spring Boot Backend 作为业务与管理 source of truth，FastAPI AI Service 作为推理/RAG/Agent Runtime，Vue Frontend 只访问 Backend。跨服务使用强类型 JSON DTO；知识发布触发 Chroma 全量一致性同步，模型部署先真实激活 AI Runtime 再提交数据库状态。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、Flyway、PostgreSQL；Python 3、FastAPI、Pydantic v2、PyTorch、Chroma、Pytest；Vue 3、TypeScript、Element Plus、Vitest、Playwright；Docker Compose。

## Global Constraints

- 状态统一：知识 `draft/published/archived`；模型 `training/deployed/deprecated`。
- Backend 是知识与模型管理数据源，AI Service 是 Chroma 与模型 Runtime 执行端。
- 未配置 LLM API Key 时必须使用真实作物、生育期、天气和 RAG 生成确定性 fallback。
- 不重做前端视觉，只补字段、真实状态、操作按钮和错误提示。
- 所有业务代码先写失败测试并确认 RED，再写最小实现确认 GREEN。
- 完成前必须通过 Backend、AI、Frontend、typecheck、build、integration、Docker、DB、Playwright snapshot/screenshot、Console 和 Network 验收。

## File Structure

### AI Service

- Create `ai-service/src/services/model_runtime_service.py`: 当前 classifier 的线程安全加载、校验、查询和原子切换。
- Create `ai-service/src/api/models.py`: `GET /api/v1/models/runtime` 与 `POST /api/v1/models/activate`。
- Modify `ai-service/src/models/schemas.py`: 结构化诊断上下文、Agent trace、RAG 同步、Runtime DTO。
- Modify `ai-service/src/services/agent_service.py`: 四个逻辑 Agent 与真实上下文 fallback。
- Modify `ai-service/src/services/rag_service.py`: 用 Backend 发布文档重建 collection。
- Modify `ai-service/src/services/inference_service.py`: 支持 Runtime 服务复用 classifier。
- Modify `ai-service/src/api/diagnosis.py`: 接收结构化 AdviceRequest 并返回 trace/context。
- Modify `ai-service/src/api/rag.py`: 新增文档同步接口。
- Modify `ai-service/src/main.py`: 注册 models router，并在 lifespan 初始化 Runtime/RAG。
- Test `ai-service/tests/test_agent.py`, `test_rag.py`, `test_model_runtime.py`, `test_api.py`。

### Backend

- Create `backend/src/main/java/com/yunong/module/diagnosis/dto/DiagnosisContext.java`: Agent 请求所需强类型上下文。
- Create `backend/src/main/java/com/yunong/module/diagnosis/service/DiagnosisContextService.java`: 诊断关联数据聚合。
- Create `backend/src/main/java/com/yunong/integration/ai/AiServiceClient.java`: advice、RAG sync、runtime activate/query 的统一 HTTP 客户端。
- Create `backend/src/main/java/com/yunong/integration/ai/dto/*.java`: 跨服务请求/响应 record。
- Modify `backend/src/main/java/com/yunong/module/diagnosis/service/AsyncDiagnosisService.java`: 使用真实上下文并保存 trace/context/references。
- Modify `backend/src/main/java/com/yunong/module/knowledge/service/KnowledgeService.java`: 状态、版本、归档与同步事务。
- Modify `backend/src/main/java/com/yunong/module/knowledge/controller/KnowledgeController.java`: 归档和手动同步接口。
- Modify `backend/src/main/java/com/yunong/module/model/entity/ModelVersion.java`: 增加类别映射路径和类别数。
- Modify `backend/src/main/java/com/yunong/module/model/service/ModelVersionService.java`: Runtime-first 部署语义。
- Modify `backend/src/main/java/com/yunong/module/model/controller/ModelVersionController.java`: Runtime 查询接口。
- Create `backend/src/main/java/com/yunong/integration/ai/AiConsistencyInitializer.java`: 启动后非阻塞知识同步。
- Create `backend/src/main/resources/db/migration/V5__ai_integration.sql`: schema、模型和六份知识初始化。
- Test `DiagnosisContextServiceTest`, `AsyncDiagnosisServiceTest`, `KnowledgeServiceTest`, `ModelVersionServiceTest`, `DatabaseMigrationTest`。

### Frontend / Integration

- Modify `frontend/src/views/AdminKnowledge.vue`: `archived`、真实归档、同步状态与失败提示。
- Modify `frontend/src/views/AdminModels.vue`: Precision、路径、类别数、部署按钮和 Runtime 标识。
- Create `frontend/src/views/__tests__/AdminKnowledge.test.ts`, `AdminModels.test.ts`。
- Modify `tests/integration/test_api.py`: 管理端、RAG、Runtime 和诊断闭环验证。
- Modify `frontend/e2e/diagnosis-flow.spec.ts`: 浏览器验收完整链路。

---

### Task 1: AI 结构化上下文与多 Agent fallback

**Files:**
- Modify: `ai-service/src/models/schemas.py`
- Modify: `ai-service/src/services/agent_service.py`
- Modify: `ai-service/src/api/diagnosis.py`
- Test: `ai-service/tests/test_agent.py`
- Test: `ai-service/tests/test_diagnosis_api.py`

**Interfaces:**
- Produces: `AdviceRequest(crop, field, weather_forecast, citations)`。
- Produces: `AdviceResponse(advice, references, context_summary, agent_trace)`。
- Produces: `AgentService.generate_advice(request: AdviceRequest) -> AdviceResponse`。

- [ ] **Step 1: 写天气与生育期 Agent 的失败测试**

```python
def test_fallback_uses_real_weather_growth_stage_and_trace(monkeypatch):
    monkeypatch.setattr(settings, "llm_api_key", "")
    request = AdviceRequest(
        disease_name="稻瘟病",
        confidence=0.92,
        crop={"name": "水稻", "variety": "滇粳验收", "planting_date": "2026-07-01", "growth_stage": "tillering"},
        field={"name": "A-01", "farm_name": "验收农场"},
        weather_forecast=[{"date": "2026-07-26", "weather": "阵雨", "temperature": 28, "humidity": 86, "rainfall": 8.5}],
        citations=[{"content": "高湿条件下及时防治", "source": "水稻病害规范", "score": 0.91}],
    )
    result = AgentService.generate_advice(request)
    assert "分蘖期" in result.advice
    assert "阵雨" in result.advice
    assert "86" in result.advice
    assert [item.agent for item in result.agent_trace] == ["weather-risk", "growth-stage", "rag-evidence", "treatment"]
```

- [ ] **Step 2: 运行测试确认 RED**

Run: `ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests/test_agent.py -q`
Expected: FAIL，原因是 `AdviceRequest` 尚无结构化字段或响应无 `agent_trace`。

- [ ] **Step 3: 最小实现结构化 DTO 与四段编排**

```python
class CropContext(BaseModel):
    name: str
    variety: str | None = None
    planting_date: date | None = None
    growth_stage: str | None = None

class AgentTrace(BaseModel):
    agent: str
    status: Literal["completed", "no-data"]
    summary: str

class AdviceResponse(BaseModel):
    advice: str
    references: list[dict[str, Any]] = Field(default_factory=list)
    context_summary: dict[str, Any] = Field(default_factory=dict)
    agent_trace: list[AgentTrace] = Field(default_factory=list)
```

`AgentService.generate_advice` 固定执行 `_analyze_weather`、`_analyze_growth_stage`、`_summarize_rag`、`_compose_treatment`；LLM 异常捕获后调用同一上下文的 `_generate_fallback_response`。

- [ ] **Step 4: 增加 no-data、LLM 失败、API schema 测试并确认 GREEN**

Run: `ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests/test_agent.py ai-service/tests/test_diagnosis_api.py -q`
Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add ai-service/src/models/schemas.py ai-service/src/services/agent_service.py ai-service/src/api/diagnosis.py ai-service/tests/test_agent.py ai-service/tests/test_diagnosis_api.py
git commit -m "feat(ai): add contextual multi-agent advice"
```

### Task 2: Backend 真实诊断上下文与 Agent 运行记录

**Files:**
- Create: `backend/src/main/java/com/yunong/module/diagnosis/dto/DiagnosisContext.java`
- Create: `backend/src/main/java/com/yunong/module/diagnosis/service/DiagnosisContextService.java`
- Create: `backend/src/main/java/com/yunong/integration/ai/AiServiceClient.java`
- Create: `backend/src/main/java/com/yunong/integration/ai/dto/AgentAdviceRequest.java`
- Create: `backend/src/main/java/com/yunong/integration/ai/dto/AgentAdviceResponse.java`
- Modify: `backend/src/main/java/com/yunong/module/diagnosis/service/AsyncDiagnosisService.java`
- Test: `backend/src/test/java/com/yunong/module/diagnosis/DiagnosisContextServiceTest.java`
- Test: `backend/src/test/java/com/yunong/module/diagnosis/AsyncDiagnosisServiceTest.java`

**Interfaces:**
- Produces: `DiagnosisContextService.load(String diagnosisId)`。
- Produces: `AiServiceClient.generateAdvice(AgentAdviceRequest)`。
- Consumes: Task 1 `/api/v1/diagnosis/advice` schema。

- [ ] **Step 1: 写完整关联和无天气的失败测试**

```java
@Test
void loadsCropStageFarmAndSevenDayWeather() {
    DiagnosisContext context = service.load("diag-1");
    assertThat(context.crop().name()).isEqualTo("水稻");
    assertThat(context.crop().growthStage()).isEqualTo("tillering");
    assertThat(context.field().farmName()).isEqualTo("验收农场");
    assertThat(context.weatherForecast()).hasSize(7);
}
```

使用 mapper mock 固定链路 `DiagnosisRecord -> Observation -> PlantingCycle -> Crop/Field -> Farm -> WeatherRecord`；另测天气为空时返回空 list，种植周期缺失时抛业务异常。

- [ ] **Step 2: 运行确认 RED**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=DiagnosisContextServiceTest test`
Expected: FAIL，类不存在。

- [ ] **Step 3: 实现最小上下文装配与 AI Client**

```java
public record DiagnosisContext(
    CropContext crop,
    FieldContext field,
    List<WeatherForecast> weatherForecast
) {}

public DiagnosisContext load(String diagnosisId) {
    // 严格按外键链路查询，天气限定 [today, today + 6 days]
}
```

`AiServiceClient` 使用现有 `RestTemplate` 和 `aiServiceUrl`，统一设置连接错误消息；`AsyncDiagnosisService` 不再传 `未知作物/未知天气`，并把 `contextSummary`、`agentTrace`、`references` 写入 `aiResult` 与 `agent_runs.output_data`。

- [ ] **Step 4: 运行上下文和异步诊断测试确认 GREEN**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=DiagnosisContextServiceTest,AsyncDiagnosisServiceTest test`
Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add backend/src/main/java/com/yunong/module/diagnosis backend/src/main/java/com/yunong/integration/ai backend/src/test/java/com/yunong/module/diagnosis
git commit -m "feat(backend): send real diagnosis context to agents"
```

### Task 3: AI RAG 发布文档同步

**Files:**
- Modify: `ai-service/src/models/schemas.py`
- Modify: `ai-service/src/services/rag_service.py`
- Modify: `ai-service/src/api/rag.py`
- Test: `ai-service/tests/test_rag.py`
- Test: `ai-service/tests/test_api.py`

**Interfaces:**
- Produces: `PUT /api/v1/rag/documents`。
- Produces: `RAGService.replace_documents(documents: list[KnowledgeSyncDocument]) -> int`。

- [ ] **Step 1: 写替换、空集清理和 metadata 的失败测试**

```python
def test_replace_documents_rebuilds_collection(tmp_path, monkeypatch):
    service = build_test_rag(tmp_path, monkeypatch)
    count = service.replace_documents([
        KnowledgeSyncDocument(id="k1", title="唯一验收知识", category="disease", version=2, content="稻瘟病连续降雨后加强巡田")
    ])
    assert count > 0
    result = service.retrieve("连续降雨 巡田", top_k=1)
    assert result[0]["metadata"]["document_id"] == "k1"
    service.replace_documents([])
    assert service.retrieve("连续降雨", top_k=1) == []
```

- [ ] **Step 2: 运行确认 RED**

Run: `ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests/test_rag.py -q`
Expected: FAIL，`replace_documents` 和同步 DTO 不存在。

- [ ] **Step 3: 最小实现全量替换**

```python
class KnowledgeSyncDocument(BaseModel):
    id: str
    title: str
    category: str
    version: int
    content: str
    tags: list[str] = Field(default_factory=list)

class KnowledgeSyncRequest(BaseModel):
    documents: list[KnowledgeSyncDocument]
```

在临时 collection 完成分块和 embedding 后再替换正式 collection；异常不删除原 collection。接口返回 `documents_count` 和 `chunks_count`。

- [ ] **Step 4: 运行 RAG/API 测试确认 GREEN**

Run: `ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests/test_rag.py ai-service/tests/test_api.py -q`
Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add ai-service/src/models/schemas.py ai-service/src/services/rag_service.py ai-service/src/api/rag.py ai-service/tests/test_rag.py ai-service/tests/test_api.py
git commit -m "feat(ai): synchronize managed knowledge into rag"
```

### Task 4: Backend 知识状态、版本与事务同步

**Files:**
- Create: `backend/src/main/java/com/yunong/integration/ai/dto/KnowledgeSyncRequest.java`
- Modify: `backend/src/main/java/com/yunong/integration/ai/AiServiceClient.java`
- Modify: `backend/src/main/java/com/yunong/module/knowledge/service/KnowledgeService.java`
- Modify: `backend/src/main/java/com/yunong/module/knowledge/controller/KnowledgeController.java`
- Create: `backend/src/main/java/com/yunong/integration/ai/AiConsistencyInitializer.java`
- Test: `backend/src/test/java/com/yunong/module/knowledge/KnowledgeServiceTest.java`

**Interfaces:**
- Consumes: Task 3 `PUT /api/v1/rag/documents`。
- Produces: `KnowledgeService.archive(String id)`、`KnowledgeService.syncPublished()`。
- Produces: `POST /api/v1/knowledge/documents/sync` 与 `POST /api/v1/knowledge/documents/{id}/archive`。

- [ ] **Step 1: 写状态、版本和失败回滚测试**

```java
@Test
void publishingSynchronizesAllPublishedDocuments() {
    KnowledgeDocument update = new KnowledgeDocument();
    update.setStatus("published");
    update.setContent("新内容");
    KnowledgeDocument result = service.update("k1", update);
    assertThat(result.getVersion()).isEqualTo(2);
    verify(aiServiceClient).replaceKnowledge(argThat(req -> req.documents().stream().allMatch(d -> d.status().equals("published"))));
}

@Test
void rejectsUnknownStatus() {
    KnowledgeDocument doc = new KnowledgeDocument();
    doc.setStatus("deleted");
    assertThatThrownBy(() -> service.create(doc, "admin")).isInstanceOf(BusinessException.class);
}
```

同步异常测试验证方法抛出，交给 `@Transactional` 回滚；归档后同步请求不含该文档。

- [ ] **Step 2: 运行确认 RED**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=KnowledgeServiceTest test`
Expected: FAIL，archive/sync 和状态语义尚不存在。

- [ ] **Step 3: 实现最小事务语义**

`create/update/archive` 标注 `@Transactional`；内容或状态变化时版本 `+1`；只查询 `status='published'` 构造全量同步请求。启动初始化器在 `ApplicationReadyEvent` 后异步调用 `syncPublished()`，仅记录 warning，不阻断启动。

- [ ] **Step 4: 运行确认 GREEN**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=KnowledgeServiceTest test`
Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add backend/src/main/java/com/yunong/integration/ai backend/src/main/java/com/yunong/module/knowledge backend/src/test/java/com/yunong/module/knowledge
git commit -m "feat(backend): synchronize published knowledge"
```

### Task 5: AI 模型 Runtime 激活与安全路径

**Files:**
- Create: `ai-service/src/services/model_runtime_service.py`
- Create: `ai-service/src/api/models.py`
- Modify: `ai-service/src/models/schemas.py`
- Modify: `ai-service/src/api/diagnosis.py`
- Modify: `ai-service/src/main.py`
- Modify: `ai-service/src/core/config.py`
- Test: `ai-service/tests/test_model_runtime.py`
- Test: `ai-service/tests/test_diagnosis_api.py`

**Interfaces:**
- Produces: `ModelRuntimeService.activate(request) -> RuntimeInfo`。
- Produces: `ModelRuntimeService.get_classifier() -> DiseaseClassifier`。
- Produces: `GET /api/v1/models/runtime`, `POST /api/v1/models/activate`。

- [ ] **Step 1: 写合法激活、路径逃逸、加载失败保留旧模型测试**

```python
def test_rejects_path_outside_allowed_root(tmp_path):
    service = ModelRuntimeService(allowed_roots=[tmp_path / "models"], classifier_factory=FakeClassifier)
    with pytest.raises(ValueError, match="允许目录"):
        service.activate(ModelActivateRequest(model_path=str(tmp_path / "outside.pth"), class_to_idx_path=str(tmp_path / "map.pth"), num_classes=18))

def test_failed_activation_keeps_previous_classifier(runtime_service):
    before = runtime_service.get_classifier()
    with pytest.raises(RuntimeError):
        runtime_service.activate(broken_request)
    assert runtime_service.get_classifier() is before
```

- [ ] **Step 2: 运行确认 RED**

Run: `ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests/test_model_runtime.py -q`
Expected: FAIL，Runtime service 不存在。

- [ ] **Step 3: 实现锁、路径边界和原子切换**

```python
class ModelRuntimeService:
    def activate(self, request: ModelActivateRequest) -> RuntimeInfo:
        model_path = self._validate_path(request.model_path)
        mapping_path = self._validate_path(request.class_to_idx_path)
        candidate = self._classifier_factory(model_path, mapping_path, request.confidence_threshold)
        with self._lock:
            self._classifier = candidate
            self._runtime_info = RuntimeInfo(...)
        return self._runtime_info
```

诊断 API 从 `request.app.state.model_runtime.get_classifier()` 获取 classifier；lifespan 用默认配置初始化。

- [ ] **Step 4: 运行 Runtime 和诊断测试确认 GREEN**

Run: `ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests/test_model_runtime.py ai-service/tests/test_diagnosis_api.py -q`
Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add ai-service/src/services/model_runtime_service.py ai-service/src/api/models.py ai-service/src/models/schemas.py ai-service/src/api/diagnosis.py ai-service/src/main.py ai-service/src/core/config.py ai-service/tests/test_model_runtime.py ai-service/tests/test_diagnosis_api.py
git commit -m "feat(ai): add safe model runtime activation"
```

### Task 6: Backend 模型部署与 Runtime 状态

**Files:**
- Create: `backend/src/main/java/com/yunong/integration/ai/dto/ModelActivateRequest.java`
- Create: `backend/src/main/java/com/yunong/integration/ai/dto/ModelRuntimeResponse.java`
- Modify: `backend/src/main/java/com/yunong/integration/ai/AiServiceClient.java`
- Modify: `backend/src/main/java/com/yunong/module/model/entity/ModelVersion.java`
- Modify: `backend/src/main/java/com/yunong/module/model/service/ModelVersionService.java`
- Modify: `backend/src/main/java/com/yunong/module/model/controller/ModelVersionController.java`
- Test: `backend/src/test/java/com/yunong/module/model/ModelVersionServiceTest.java`

**Interfaces:**
- Consumes: Task 5 Runtime APIs。
- Produces: `ModelVersionService.deploy(String id)`，成功状态 `deployed`，旧版本 `deprecated`。
- Produces: `GET /api/v1/model-versions/runtime`。

- [ ] **Step 1: 重写部署失败测试为 Runtime-first 语义**

```java
@Test
void deployActivatesRuntimeBeforeChangingDatabase() {
    when(mapper.selectById("mv-1")).thenReturn(candidate);
    when(aiServiceClient.activateModel(any())).thenReturn(runtimeResponse("/app/best_model.pth"));
    ModelVersion result = service.deploy("mv-1");
    InOrder order = inOrder(aiServiceClient, mapper);
    order.verify(aiServiceClient).activateModel(any());
    order.verify(mapper, atLeastOnce()).updateById(any());
    assertThat(result.getStatus()).isEqualTo("deployed");
}

@Test
void activationFailureLeavesDatabaseUntouched() {
    when(aiServiceClient.activateModel(any())).thenThrow(new BusinessException("AI 模型加载失败"));
    assertThatThrownBy(() -> service.deploy("mv-1")).isInstanceOf(BusinessException.class);
    verify(mapper, never()).updateById(any());
}
```

- [ ] **Step 2: 运行确认 RED**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=ModelVersionServiceTest test`
Expected: FAIL，服务仍使用 `draft/active/inactive` 且未调用 AI。

- [ ] **Step 3: 最小实现统一状态和部署事务**

新增 `classMappingPath`、`numClasses`；create 默认 `training`；普通 update 忽略 `deployed` 伪造；deploy 先激活 Runtime，再将同模型旧 `deployed` 改为 `deprecated`，当前改为 `deployed`。

- [ ] **Step 4: 运行确认 GREEN**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=ModelVersionServiceTest test`
Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add backend/src/main/java/com/yunong/integration/ai backend/src/main/java/com/yunong/module/model backend/src/test/java/com/yunong/module/model
git commit -m "feat(backend): deploy models through ai runtime"
```

### Task 7: Flyway 初始化当前模型与六份知识

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__ai_integration.sql`
- Modify: `backend/src/test/java/com/yunong/config/DatabaseMigrationTest.java`

**Interfaces:**
- Produces: `knowledge_documents` 六条 `published` 数据。
- Produces: `model_versions` 一条 `deployed` ResNet50 v1.0.0 数据。

- [ ] **Step 1: 写 migration 断言失败测试**

```java
@Test
void aiIntegrationSeedContainsManagedKnowledgeAndRuntimeModel() throws IOException {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V5__ai_integration.sql"));
    assertThat(sql).contains("云农病害识别 ResNet50", "/app/best_model.pth", "/app/class_to_idx.pth");
    assertThat(sql).contains("01_rice_diseases.md", "06_citrus_soybean_cotton.md");
    assertThat(sql).contains("ON CONFLICT");
}
```

- [ ] **Step 2: 运行确认 RED**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=DatabaseMigrationTest test`
Expected: FAIL，V5 不存在。

- [ ] **Step 3: 创建幂等 migration**

Migration 增加 `class_mapping_path VARCHAR(500)`、`num_classes INTEGER`，补唯一索引 `(model_name, version)` 与知识来源唯一索引；使用 `INSERT ... ON CONFLICT DO NOTHING` 写入模型指标和六份 Markdown 正文。

- [ ] **Step 4: 使用临时 PostgreSQL/Flyway 或 Docker 重建确认 GREEN**

Run: `mvn.cmd -q -f backend/pom.xml -Dtest=DatabaseMigrationTest test`
Expected: PASS。

Run: `docker compose -f deploy/docker-compose.yml up -d --build postgres backend`
Expected: Backend health 为 `UP`，Flyway V5 成功。

- [ ] **Step 5: 提交**

```powershell
git add backend/src/main/resources/db/migration/V5__ai_integration.sql backend/src/test/java/com/yunong/config/DatabaseMigrationTest.java
git commit -m "feat(db): seed managed knowledge and runtime model"
```

### Task 8: 管理端知识与模型页面真实操作

**Files:**
- Modify: `frontend/src/views/AdminKnowledge.vue`
- Modify: `frontend/src/views/AdminModels.vue`
- Create: `frontend/src/views/__tests__/AdminKnowledge.test.ts`
- Create: `frontend/src/views/__tests__/AdminModels.test.ts`

**Interfaces:**
- Consumes: Task 4 knowledge archive/sync API。
- Consumes: Task 6 model deploy/runtime API。

- [ ] **Step 1: 写页面请求行为失败测试**

```typescript
it('归档知识调用后端并重新加载列表', async () => {
  await wrapper.find('[data-test="archive-k1"]').trigger('click')
  await flushPromises()
  expect(request.post).toHaveBeenCalledWith('/knowledge/documents/k1/archive')
})

it('部署模型调用 deploy 并显示 runtime 标识', async () => {
  await wrapper.find('[data-test="deploy-m1"]').trigger('click')
  await flushPromises()
  expect(request.post).toHaveBeenCalledWith('/model-versions/m1/deploy')
  expect(wrapper.text()).toContain('当前运行')
})
```

- [ ] **Step 2: 运行确认 RED**

Run: `npm.cmd --prefix frontend test -- --run AdminKnowledge.test.ts AdminModels.test.ts`
Expected: FAIL，操作按钮/API/字段不存在。

- [ ] **Step 3: 最小实现字段、状态和错误提示**

知识页面加入 archived 标签和真实 archive API；模型页面加入 `precisionVal`、`modelPath`、`classMappingPath`、`numClasses`，加载 `/model-versions/runtime`，部署只调用 deploy API；提交按钮防重复，异常显示 `ElMessage.error`。

- [ ] **Step 4: 运行测试、typecheck 和 build 确认 GREEN**

Run: `npm.cmd --prefix frontend test -- --run AdminKnowledge.test.ts AdminModels.test.ts`
Expected: PASS。

Run: `Push-Location frontend; npm.cmd exec -- vue-tsc --noEmit -p tsconfig.app.json; Pop-Location`
Expected: exit 0。

Run: `npm.cmd --prefix frontend run build`
Expected: exit 0。

- [ ] **Step 5: 提交**

```powershell
git add frontend/src/views/AdminKnowledge.vue frontend/src/views/AdminModels.vue frontend/src/views/__tests__/AdminKnowledge.test.ts frontend/src/views/__tests__/AdminModels.test.ts
git commit -m "feat(frontend): connect ai administration pages"
```

### Task 9: 全链路集成、Docker 与浏览器验收

**Files:**
- Modify: `tests/integration/test_api.py`
- Modify: `frontend/e2e/diagnosis-flow.spec.ts`
- Modify when required by verified failures: only the source/test files directly causing the failure。

**Interfaces:**
- Verifies: 管理员登录、知识 CRUD/RAG、模型 Runtime、图片诊断、审核、任务生成。

- [ ] **Step 1: 扩展集成测试并确认现有系统 RED**

```python
def test_managed_knowledge_changes_rag(admin_client, ai_client):
    title = "E2E唯一知识-20260726"
    created = admin_client.post("/api/v1/knowledge/documents", json={"title": title, "category": "disease", "content": "连续降雨后检查叶片病斑", "status": "published"})
    assert created.status_code == 200
    retrieved = ai_client.post("/api/v1/rag/retrieve", json={"query": "连续降雨 叶片病斑", "top_k": 3})
    assert title in str(retrieved.json())
```

另测 Runtime 当前路径、诊断结果 `contextSummary/agentTrace/references`、审核后任务存在。

- [ ] **Step 2: 重建六服务并运行集成测试**

Run: `docker compose -f deploy/docker-compose.yml up -d --build`
Expected: 六个容器 healthy。

Run: `ai-service\.venv\Scripts\python.exe -m pytest tests/integration/test_api.py -q`
Expected: 先暴露未接通项；按失败根因只做局部修复，直到 PASS。

- [ ] **Step 3: 全量自动化验证**

Run: `mvn.cmd -q -f backend/pom.xml test`
Expected: PASS。

Run: `ai-service\.venv\Scripts\python.exe -m pytest ai-service/tests -q`
Expected: PASS。

Run: `npm.cmd --prefix frontend test -- --run`
Expected: PASS。

Run: `Push-Location frontend; npm.cmd exec -- vue-tsc --noEmit -p tsconfig.app.json; Pop-Location`
Expected: exit 0。

Run: `npm.cmd --prefix frontend run build`
Expected: exit 0。

- [ ] **Step 4: 数据库和 API 验收**

```powershell
docker exec yunnong-db psql -U yunong -d yunnong -c "select status,count(*) from knowledge_documents group by status;"
docker exec yunnong-db psql -U yunong -d yunnong -c "select model_name,version,status,model_path,class_mapping_path from model_versions;"
docker exec yunnong-db psql -U yunong -d yunnong -c "select status,count(*) from agent_runs group by status;"
```

Expected: 至少六份 published 知识、一个 deployed 模型、Agent 运行有 completed 记录。

- [ ] **Step 5: Playwright 视觉与网络验收**

使用管理员 `admin/admin123`：

1. 打开知识库和模型页面；
2. 同时执行 accessibility snapshot 与 full-page screenshot；
3. 读取 screenshot 检查空白、遮挡、乱码、按钮和状态；
4. 完成知识新增/发布/归档与模型 Runtime 查看；
5. 完成图片上传、诊断详情、审核和任务生成；
6. 检查 Console error 为 0；
7. 检查关键 Network 请求均为 2xx。

- [ ] **Step 6: 最终提交**

```powershell
git add tests/integration/test_api.py frontend/e2e/diagnosis-flow.spec.ts
git commit -m "test: verify complete ai diagnosis workflow"
```

## Self-Review

- Spec coverage: Agent 真实天气/生育期、RAG、多 Agent、知识同步、模型 Runtime、初始化数据、前端管理和闭环验收均有对应任务。
- Placeholder scan: 所有步骤均包含具体文件、接口、测试、命令和期望结果。
- Type consistency: `AdviceRequest/AdviceResponse`、`KnowledgeSyncRequest`、`ModelActivateRequest/RuntimeInfo` 在 AI 与 Backend 任务中命名一致；状态枚举在三端一致。
- Dependency order: Task 1→2，Task 3→4，Task 5→6，Task 7→8→9；每组均可独立 RED/GREEN 和提交。


