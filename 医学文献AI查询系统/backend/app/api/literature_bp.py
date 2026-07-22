from flask import Blueprint, request, jsonify
from app.services.literature_service import literature_service
from app.utils.logger import logger

literature_bp = Blueprint('literature', __name__)


@literature_bp.route('/search', methods=['POST'])
def search_literatures():
    data = request.get_json()
    if not data or 'keywords' not in data:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '缺少keywords参数'}), 400

    keywords = data['keywords']
    if not isinstance(keywords, list) or len(keywords) == 0:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': 'keywords必须为非空数组'}), 400

    if len(keywords) > 5:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '关键词数量不能超过5个'}), 400

    page = data.get('page', 1)
    page_size = data.get('page_size', 9)

    try:
        result = literature_service.search_literatures(keywords, page, page_size)
        return jsonify(result)
    except Exception as e:
        error_msg = str(e)
        logger.error(f"Search literatures API error: {error_msg}")

        if 'PubMed' in error_msg or '检索' in error_msg:
            return jsonify({
                'error_code': 'PUBMED_SERVICE_UNAVAILABLE',
                'message': '文献检索服务暂时不可用，请稍后重试'
            }), 503

        if 'AI' in error_msg or '翻译' in error_msg:
            return jsonify({
                'error_code': 'AI_SERVICE_UNAVAILABLE',
                'message': '翻译服务暂时不可用，请稍后重试'
            }), 503

        return jsonify({
            'error_code': 'SERVICE_ERROR',
            'message': '文献检索服务暂时不可用，请稍后重试'
        }), 503
