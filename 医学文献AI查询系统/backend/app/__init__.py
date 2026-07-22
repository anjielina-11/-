from flask import Flask, jsonify
from flask_cors import CORS
from config import get_config
from app.utils.logger import logger


def create_app():
    app = Flask(__name__)
    config = get_config()
    app.config.from_object(config)

    CORS(app, resources={r"/api/*": {"origins": "*"}})

    from app.api.keyword_bp import keyword_bp
    from app.api.literature_bp import literature_bp
    from app.api.summary_bp import summary_bp
    from app.api.health_bp import health_bp

    app.register_blueprint(keyword_bp, url_prefix='/api/keywords')
    app.register_blueprint(literature_bp, url_prefix='/api/literatures')
    app.register_blueprint(summary_bp, url_prefix='/api/summaries')
    app.register_blueprint(health_bp, url_prefix='/api')

    @app.errorhandler(Exception)
    def handle_exception(e):
        logger.error(f"Unhandled exception: {str(e)}", exc_info=True)
        return jsonify({
            'error_code': 'INTERNAL_ERROR',
            'message': '服务内部错误，请稍后重试'
        }), 500

    @app.errorhandler(400)
    def handle_bad_request(e):
        return jsonify({
            'error_code': 'INVALID_INPUT',
            'message': str(e.description) if hasattr(e, 'description') else '请求参数错误'
        }), 400

    logger.info("MedSearch Flask app initialized")
    return app
