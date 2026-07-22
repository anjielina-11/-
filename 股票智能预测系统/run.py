"""
股票智能预测系统 - 统一入口
启动方式: python run.py
"""

from backend import create_app
from backend.config import FLASK_HOST, FLASK_PORT, FLASK_DEBUG

if __name__ == "__main__":
    print("=" * 60)
    print("  股票智能预测系统 (Stock Intelligent Prediction System)")
    print("  模块架构: frontend / backend / algorithm")
    print("=" * 60)
    print(f"  主页:     http://127.0.0.1:{FLASK_PORT}")
    print(f"  收藏页:   http://127.0.0.1:{FLASK_PORT}/favorites")
    print("=" * 60)

    app = create_app()
    app.run(debug=FLASK_DEBUG, host=FLASK_HOST, port=FLASK_PORT)
