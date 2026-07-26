from pathlib import Path

from src.core.config import Settings, settings


AI_SERVICE_ROOT = Path(__file__).resolve().parents[1]


def test_default_runtime_paths_are_anchored_to_ai_service_root():
    assert Path(settings.MODEL_PATH) == AI_SERVICE_ROOT / "best_model.pth"
    assert Path(settings.CLASS_TO_IDX_PATH) == AI_SERVICE_ROOT / "class_to_idx.pth"
    assert Path(settings.RAG_VECTOR_DB_PATH) == AI_SERVICE_ROOT / "chroma_db"
    assert Path(settings.RAG_KNOWLEDGE_DOCS_PATH) == AI_SERVICE_ROOT / "knowledge_docs"


def test_relative_path_overrides_are_resolved_from_ai_service_root():
    configured = Settings(
        MODEL_PATH="models/custom.pth",
        CLASS_TO_IDX_PATH="models/classes.pth",
        RAG_VECTOR_DB_PATH="runtime/chroma",
        RAG_KNOWLEDGE_DOCS_PATH="docs",
        _env_file=None,
    )

    assert Path(configured.MODEL_PATH) == AI_SERVICE_ROOT / "models/custom.pth"
    assert Path(configured.CLASS_TO_IDX_PATH) == AI_SERVICE_ROOT / "models/classes.pth"
    assert Path(configured.RAG_VECTOR_DB_PATH) == AI_SERVICE_ROOT / "runtime/chroma"
    assert Path(configured.RAG_KNOWLEDGE_DOCS_PATH) == AI_SERVICE_ROOT / "docs"
