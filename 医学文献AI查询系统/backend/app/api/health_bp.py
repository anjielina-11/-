from flask import Blueprint, jsonify, current_app
from app.clients.deepseek_client import deepseek_client
from app.clients.pubmed_client import pubmed_client
from app.utils.logger import logger

health_bp = Blueprint('health', __name__)


@health_bp.route('/health', methods=['GET'])
def health_check():
    services = {}

    deepseek_status = 'ok'
    try:
        deepseek_client.chat("ping", system_prompt="请回复ok")
    except Exception as e:
        logger.warning(f"Health check DeepSeek failed: {str(e)}")
        deepseek_status = 'error'
    services['deepseek'] = deepseek_status

    pubmed_status = 'ok'
    try:
        result = pubmed_client.search(['cancer'], page=1, page_size=1)
        if result['total_count'] == 0:
            pubmed_status = 'error'
    except Exception as e:
        logger.warning(f"Health check PubMed failed: {str(e)}")
        pubmed_status = 'unavailable'
    services['pubmed'] = pubmed_status

    core_ok = services['deepseek'] == 'ok'
    overall = 'healthy' if core_ok else 'degraded'

    return jsonify({
        'status': overall,
        'services': services,
        'note': 'PubMed不可用时将使用模拟文献数据' if pubmed_status != 'ok' else '',
    })
