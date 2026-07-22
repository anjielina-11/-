"""
后端模块 (Backend Module)
Flask Web 应用工厂
"""

import os
import sqlite3
from datetime import datetime

from flask import Flask, g

from .config import DB_DIR, DB_PATH, DEFAULT_FAVORITES, FRONTEND_TEMPLATES


def create_app() -> Flask:
    """Flask 应用工厂函数"""
    app = Flask(__name__, template_folder=FRONTEND_TEMPLATES)

    # ── 数据库初始化 ──────────────────────────────────────
    os.makedirs(DB_DIR, exist_ok=True)

    def _init_db():
        conn = sqlite3.connect(DB_PATH)
        conn.execute(
            """CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                stock_name TEXT NOT NULL UNIQUE,
                stock_code TEXT NOT NULL,
                added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )"""
        )
        count = conn.execute("SELECT COUNT(*) FROM favorites").fetchone()[0]
        if count == 0:
            now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            conn.executemany(
                "INSERT INTO favorites (stock_name, stock_code, added_at) VALUES (?, ?, ?)",
                [(name, code, now) for name, code in DEFAULT_FAVORITES],
            )
        conn.commit()
        conn.close()

    _init_db()

    # ── 请求级数据库连接 ──────────────────────────────────
    @app.before_request
    def _before_request():
        db = sqlite3.connect(DB_PATH)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA journal_mode=WAL")
        g.db = db

    @app.teardown_appcontext
    def _close_db(exception):
        db = g.pop("db", None)
        if db is not None:
            db.close()

    # ── 注册路由蓝图 ──────────────────────────────────────
    from .routes import api

    app.register_blueprint(api)

    return app
