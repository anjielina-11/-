from pydantic_settings import BaseSettings, SettingsConfigDict


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
    
    MODEL_PATH: str = "best_model.pth"
    CLASS_TO_IDX_PATH: str = "class_to_idx.pth"
    NUM_CLASSES: int = 18
    CONFIDENCE_THRESHOLD: float = 0.6
    
    RAG_VECTOR_DB_PATH: str = "chroma_db"
    RAG_EMBEDDING_MODEL: str = "all-MiniLM-L6-v2"
    RAG_CHUNK_SIZE: int = 512
    RAG_CHUNK_OVERLAP: int = 64
    RAG_TOP_K: int = 3

    LLM_API_KEY: str = ""
    LLM_API_BASE: str = ""
    LLM_MODEL_NAME: str = "gpt-3.5-turbo"

    WEATHER_API_KEY: str = ""
    WEATHER_API_BASE: str = "https://api.openweathermap.org/data/2.5"

    model_config = SettingsConfigDict(env_file=".env")


settings = Settings()