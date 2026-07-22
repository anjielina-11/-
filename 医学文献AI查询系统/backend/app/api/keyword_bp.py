from flask import Blueprint, request, jsonify
from app.services.keyword_service import keyword_service
from app.utils.logger import logger

keyword_bp = Blueprint('keyword', __name__)


@keyword_bp.route('/generate', methods=['POST'])
def generate_keywords():
    data = request.get_json()
    if not data or 'description' not in data:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '缺少description参数'}), 400

    description = data['description']
    if not isinstance(description, str):
        return jsonify({'error_code': 'INVALID_INPUT', 'message': 'description必须为字符串'}), 400

    if len(description) < 10:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '请输入至少10个字符的疾病描述'}), 400

    if len(description) > 300:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '疾病描述不能超过300个字符'}), 400

    try:
        keywords = keyword_service.generate_keywords(description)
        return jsonify({'keywords': keywords})
    except Exception as e:
        logger.error(f"Generate keywords API error: {str(e)}")
        return jsonify({
            'error_code': 'AI_SERVICE_UNAVAILABLE',
            'message': '关键词生成服务暂时不可用，请稍后重试'
        }), 503


@keyword_bp.route('/translate', methods=['POST'])
def translate_keywords():
    data = request.get_json()
    if not data or 'keywords' not in data:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '缺少keywords参数'}), 400

    keywords = data['keywords']
    if not isinstance(keywords, list) or len(keywords) == 0:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': 'keywords必须为非空数组'}), 400

    if len(keywords) > 5:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '关键词数量不能超过5个'}), 400

    try:
        translated = keyword_service.translate_keywords(keywords)
        return jsonify({'translated_keywords': translated})
    except Exception as e:
        logger.error(f"Translate keywords API error: {str(e)}")
        return jsonify({
            'error_code': 'AI_SERVICE_UNAVAILABLE',
            'message': '翻译服务暂时不可用，请稍后重试'
        }), 503
