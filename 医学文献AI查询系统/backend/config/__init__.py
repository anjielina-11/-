import os
from dotenv import load_dotenv

load_dotenv()


class Config:
    DEEPSEEK_API_URL = os.getenv('DEEPSEEK_API_URL', 'https://api.deepseek.com/v1/chat/completions')
    DEEPSEEK_API_KEY = os.getenv('DEEPSEEK_API_KEY', '')
    DEEPSEEK_MODEL_ID = os.getenv('DEEPSEEK_MODEL_ID', 'deepseek-chat')
    PUBMED_API_KEY = os.getenv('PUBMED_API_KEY', '')
    PUBMED_BASE_URL = os.getenv('PUBMED_BASE_URL', 'https://eutils.ncbi.nlm.nih.gov/entrez/eutils')
    PUBMED_RATE_LIMIT = int(os.getenv('PUBMED_RATE_LIMIT', '3'))
    REQUEST_TIMEOUT = int(os.getenv('REQUEST_TIMEOUT', '30'))
    HTTP_PROXY = os.getenv('HTTP_PROXY', '')


class DevelopmentConfig(Config):
    DEBUG = True


class ProductionConfig(Config):
    DEBUG = False


config_map = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
}


def get_config():
    env = os.getenv('FLASK_ENV', 'development')
    return config_map.get(env, DevelopmentConfig)
