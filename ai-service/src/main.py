import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from .api.diagnosis import router as diagnosis_router
from .api.weather import router as weather_router
from .api.rag import router as rag_router
from .api.models import router as models_router
from .core.config import settings
from .services.rag_service import RAGService
from .services.model_runtime_service import ModelRuntimeService
from .models.schemas import HealthResponse, ModelActivateRequest

logger = logging.getLogger(__name__)

DEFAULT_MODEL_REQUEST = ModelActivateRequest(
    model_name="default-disease-classifier",
    version="v1.0.0",
    model_path=str(settings.MODEL_PATH),
    class_to_idx_path=str(settings.CLASS_TO_IDX_PATH),
    num_classes=settings.NUM_CLASSES,
    confidence_threshold=settings.CONFIDENCE_THRESHOLD,
)
MODEL_RUNTIME = ModelRuntimeService(
    allowed_roots=settings.model_allowed_roots_list,
    initial_request=DEFAULT_MODEL_REQUEST,
)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    try:
        if not _app.state.model_runtime.get_runtime().loaded:
            _app.state.model_runtime.activate(DEFAULT_MODEL_REQUEST)
            logger.info("Loaded default disease classifier")
    except Exception:
        logger.exception("Failed to load default disease classifier")

    try:
        chunks_count = RAGService.ensure_initialized(settings.RAG_KNOWLEDGE_DOCS_PATH)
        if chunks_count:
            logger.info("Initialized RAG knowledge base with %s chunks", chunks_count)
    except Exception:
        logger.exception("Failed to initialize RAG knowledge base")
    yield

app = FastAPI(
    title=settings.APP_NAME,
    lifespan=lifespan,
    version=settings.APP_VERSION,
    description="""农业AI服务 - 提供作物病害诊断、天气查询和知识库检索等功能。

**核心功能：**
- 🌱 **图片诊断**：上传作物叶片图片，智能识别病害类型
- 🌤️ **天气查询**：获取实时天气信息，辅助防治决策
- 📚 **知识库检索**：基于RAG技术检索农业技术文档
- 🤖 **智能建议**：结合病害诊断、天气和知识库，生成综合防治建议

**技术栈：**
- FastAPI + Python
- PyTorch + ResNet50（图像识别）
- LangChain + Chroma（RAG检索）
- Docker（容器化部署）
""",
    contact={
        "name": "Agricultural AI Service Team",
        "email": "support@agri-ai-service.com"
    },
    openapi_tags=[
        {
            "name": "health",
            "description": "健康检查接口"
        },
        {
            "name": "diagnosis",
            "description": "病害诊断相关接口"
        },
        {
            "name": "weather",
            "description": "天气查询相关接口"
        },
        {
            "name": "rag",
            "description": "知识库检索相关接口"
        }
    ]
)
app.state.model_runtime = MODEL_RUNTIME

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(diagnosis_router)
app.include_router(weather_router)
app.include_router(rag_router)
app.include_router(models_router)



@app.get(
    "/health",
    tags=["health"],
    response_model=HealthResponse,
    summary="健康检查",
    description="检查服务运行状态，包括模型加载和向量数据库状态。"
)
async def health_check(request: Request):
    model_loaded = False
    vector_db_ready = False
    
    try:
        model_loaded = request.app.state.model_runtime.get_runtime().loaded
    except Exception:
        model_loaded = False
    
    try:
        vector_store = RAGService._get_vector_store()
        vector_db_ready = vector_store is not None
    except Exception:
        vector_db_ready = False
    
    return HealthResponse(
        status="healthy" if (model_loaded or vector_db_ready) else "degraded",
        app_name=settings.APP_NAME,
        version=settings.APP_VERSION,
        model_loaded=model_loaded,
        vector_db_ready=vector_db_ready
    )
