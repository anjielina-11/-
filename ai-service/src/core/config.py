from pathlib import Path

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

from .paths import AI_SERVICE_ROOT, resolve_service_path


class Settings(BaseSettings):
    APP_NAME: str = "Agricultural AI Service"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    MAX_IMAGE_SIZE: int = 10 * 1024 * 1024
    ALLOWED_IMAGE_EXTENSIONS: str = "jpg,jpeg,png,gif,bmp"

    @property
    def allowed_extensions_list(self):
        return [ext.strip() for ext in self.ALLOWED_IMAGE_EXTENSIONS.split(",")]

    MODEL_PATH: Path = AI_SERVICE_ROOT / "best_model.pth"
    CLASS_TO_IDX_PATH: Path = AI_SERVICE_ROOT / "class_to_idx.pth"
    NUM_CLASSES: int = 18
    CONFIDENCE_THRESHOLD: float = 0.6
    MODEL_ALLOWED_ROOTS: str = ""

    @property
    def model_allowed_roots_list(self):
        roots = [AI_SERVICE_ROOT, Path("/app")]
        roots.extend(
            resolve_service_path(value.strip())
            for value in self.MODEL_ALLOWED_ROOTS.split(",")
            if value.strip()
        )
        return list(dict.fromkeys(root.resolve() for root in roots))

    RAG_VECTOR_DB_PATH: Path = AI_SERVICE_ROOT / "chroma_db"
    RAG_KNOWLEDGE_DOCS_PATH: Path = AI_SERVICE_ROOT / "knowledge_docs"
    RAG_EMBEDDING_MODEL: str = "all-MiniLM-L6-v2"
    RAG_CHUNK_SIZE: int = 512
    RAG_CHUNK_OVERLAP: int = 64
    RAG_TOP_K: int = 3

    LLM_API_KEY: str = ""
    LLM_API_BASE: str = ""
    LLM_MODEL_NAME: str = "gpt-3.5-turbo"

    WEATHER_API_KEY: str = ""
    WEATHER_API_BASE: str = "https://api.openweathermap.org/data/2.5"

    @field_validator(
        "MODEL_PATH",
        "CLASS_TO_IDX_PATH",
        "RAG_VECTOR_DB_PATH",
        "RAG_KNOWLEDGE_DOCS_PATH",
        mode="before",
    )
    @classmethod
    def resolve_runtime_path(cls, value):
        return resolve_service_path(value)

    model_config = SettingsConfigDict(
        env_file=AI_SERVICE_ROOT / ".env",
        env_file_encoding="utf-8",
        validate_default=True,
    )


settings = Settings()
