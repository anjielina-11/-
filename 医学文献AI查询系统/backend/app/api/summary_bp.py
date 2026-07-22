from flask import Blueprint, request, jsonify
from app.services.summary_service import summary_service
from app.utils.logger import logger

summary_bp = Blueprint('summary', __name__)


@summary_bp.route('/generate', methods=['POST'])
def generate_summary():
    data = request.get_json()
    if not data:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '请求参数不能为空'}), 400

    pmid = data.get('pmid')
    title_en = data.get('title_en')
    abstract = data.get('abstract', '')

    if not pmid:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '缺少pmid参数'}), 400

    if not title_en:
        return jsonify({'error_code': 'INVALID_INPUT', 'message': '缺少title_en参数'}), 400

    try:
        result = summary_service.generate_summary(pmid, title_en, abstract)
        return jsonify(result)
    except Exception as e:
        logger.error(f"Generate summary API error: pmid={pmid}, error={str(e)}")
        error_msg = str(e)

        if '超时' in error_msg:
            return jsonify({
                'error_code': 'AI_SERVICE_TIMEOUT',
                'message': 'AI总结生成超时，请稍后重试'
            }), 504

        return jsonify({
            'error_code': 'AI_SERVICE_UNAVAILABLE',
            'message': 'AI总结生成服务暂时不可用，请稍后重试'
        }), 503
