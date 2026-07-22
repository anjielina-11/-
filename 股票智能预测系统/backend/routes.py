"""
后端路由模块 (Blueprint)
定义所有 REST API 端点及页面路由
"""

import time
from datetime import datetime, timedelta

import requests
from flask import Blueprint, g, jsonify, render_template, request

from algorithm import StockPredictor
from backend.config import DEEPSEEK_API_KEY, DEEPSEEK_API_URL, DEEPSEEK_MODEL
from backend.stock_service import (
    get_recent_close_prices,
    get_stock_code_by_name,
    get_training_data,
    search_stock_by_name,
)

# 创建蓝图
api = Blueprint("api", __name__)


# ── 页面路由 ────────────────────────────────────────────────

@api.route("/")
def index():
    """主页"""
    return render_template("index.html")


@api.route("/favorites")
def favorites_page():
    """收藏页面"""
    return render_template("favorites.html")


# ── 股票搜索 API ───────────────────────────────────────────

@api.route("/api/search", methods=["POST"])
def search_stock():
    """股票名称模糊搜索"""
    try:
        data = request.get_json()
        keyword = data.get("keyword", "").strip()
        if not keyword:
            return jsonify({"error": "请输入股票名称关键词"}), 400
        results = search_stock_by_name(keyword)
        return jsonify({"stocks": results})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ── 股票预测 API ───────────────────────────────────────────

@api.route("/api/predict", methods=["POST"])
def predict():
    """Transformer 模型预测"""
    try:
        data = request.get_json()
        stock_name = data.get("stock_name", "").strip()
        if not stock_name:
            return jsonify({"error": "请输入股票名称"}), 400

        code = get_stock_code_by_name(stock_name)
        if code is None:
            return jsonify({
                "error": f"未找到股票「{stock_name}」，请检查名称是否正确"
            }), 404

        fetch_start = time.time()
        print(f"[预测] 正在获取 {stock_name}({code}) 的历史数据...")
        train_prices, train_dates = get_training_data(code, days=500)
        fetch_time = round(time.time() - fetch_start, 2)
        print(f"[预测] 获取到 {len(train_prices)} 条历史数据 (耗时 {fetch_time}s)")

        if len(train_prices) < 50:
            return jsonify({
                "error": f"历史数据不足（仅{len(train_prices)}条），无法训练模型"
            }), 400

        print("[预测] 正在训练 Transformer 模型...")
        predictor = StockPredictor()
        training_result = predictor.train(train_prices, epochs=100, verbose=True)

        recent_prices, recent_dates = get_recent_close_prices(code, days=20)
        predictions = predictor.predict(recent_prices)
        print(f"[预测] 预测结果: {predictions}")

        # 计算未来5个交易日
        last_date = datetime.strptime(recent_dates[-1], "%Y%m%d")
        future_dates = []
        d = last_date
        while len(future_dates) < 5:
            d = d + timedelta(days=1)
            if d.weekday() < 5:
                future_dates.append(d.strftime("%Y%m%d"))

        predicted = [
            {"date": dt, "close": round(float(p), 2)}
            for dt, p in zip(future_dates, predictions.tolist())
        ]

        display_historical = [
            {"date": dt, "close": round(float(p), 2)}
            for dt, p in zip(train_dates[-60:], train_prices[-60:].tolist())
            if len(train_dates) >= 60
        ] or [
            {"date": dt, "close": round(float(p), 2)}
            for dt, p in zip(train_dates, train_prices.tolist())
        ]

        # 模型信息
        mc = training_result["model_config"]
        pc = training_result["param_count"]

        model_info = {
            "architecture": {
                "model_type": "Transformer Encoder",
                "input_length": training_result["input_len"],
                "output_length": training_result["output_len"],
                "d_model": mc["d_model"],
                "attention_heads": mc["nhead"],
                "encoder_layers": mc["num_encoder_layers"],
                "feedforward_dim": mc["dim_feedforward"],
                "dropout": mc["dropout"],
                "positional_encoding": "Sinusoidal",
            },
            "training_config": {
                "optimizer": training_result["optimizer"],
                "learning_rate": training_result["learning_rate"],
                "loss_function": training_result["loss_function"],
                "scheduler": training_result["scheduler"],
                "gradient_clip_norm": training_result["gradient_clip_norm"],
                "train_val_split": training_result["train_val_split"],
                "scaler": training_result["scaler"],
            },
            "complexity": {
                "total_params": pc["total"],
                "trainable_params": pc["trainable"],
                "device": training_result["device"],
            },
            "data_stats": {
                "total_samples": training_result["total_samples"],
                "train_samples": training_result["train_samples"],
                "val_samples": training_result["val_samples"],
                "data_period": f"{train_dates[0]} ~ {train_dates[-1]}",
                "raw_data_points": len(train_prices),
                "fetch_time_sec": fetch_time,
            },
            "metrics": {
                "epochs": training_result["epochs"],
                "final_train_loss": round(training_result["final_train_loss"], 6),
                "final_val_loss": round(training_result["final_val_loss"], 6),
                "train_time_sec": training_result["train_time_sec"],
            },
        }

        return jsonify({
            "stock_name": stock_name,
            "stock_code": code,
            "historical": display_historical,
            "predicted": predicted,
            "last_20_dates": recent_dates,
            "model_info": model_info,
            "training_info": {
                "train_losses": training_result["train_losses"],
                "val_losses": training_result["val_losses"],
            },
        })
    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except RuntimeError as e:
        return jsonify({"error": str(e)}), 500
    except Exception as e:
        return jsonify({"error": f"预测失败: {str(e)}"}), 500


# ── 收藏 API ────────────────────────────────────────────────

@api.route("/api/favorites", methods=["GET"])
def list_favorites():
    """获取所有收藏"""
    try:
        rows = g.db.execute(
            "SELECT id, stock_name, stock_code, added_at FROM favorites ORDER BY id"
        ).fetchall()
        favorites = [
            {
                "id": r["id"],
                "stock_name": r["stock_name"],
                "stock_code": r["stock_code"],
                "added_at": r["added_at"],
            }
            for r in rows
        ]
        return jsonify({"favorites": favorites})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@api.route("/api/favorites", methods=["POST"])
def add_favorite():
    """添加收藏"""
    try:
        data = request.get_json()
        stock_name = data.get("stock_name", "").strip()
        stock_code = data.get("stock_code", "").strip()
        if not stock_name or not stock_code:
            return jsonify({"error": "股票名称和代码不能为空"}), 400

        existing = g.db.execute(
            "SELECT id FROM favorites WHERE stock_name = ?", (stock_name,)
        ).fetchone()
        if existing:
            return jsonify({"error": f"「{stock_name}」已在收藏列表中"}), 409

        g.db.execute(
            "INSERT INTO favorites (stock_name, stock_code) VALUES (?, ?)",
            (stock_name, stock_code),
        )
        g.db.commit()
        return jsonify({"success": True, "message": f"已添加「{stock_name}」到收藏"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@api.route("/api/favorites/<int:fid>", methods=["DELETE"])
def remove_favorite(fid):
    """删除收藏"""
    try:
        row = g.db.execute(
            "SELECT stock_name FROM favorites WHERE id = ?", (fid,)
        ).fetchone()
        if not row:
            return jsonify({"error": "收藏记录不存在"}), 404
        g.db.execute("DELETE FROM favorites WHERE id = ?", (fid,))
        g.db.commit()
        return jsonify({"success": True, "message": f"已移除「{row['stock_name']}」"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ── AI 分析 API ────────────────────────────────────────────

@api.route("/api/analyze", methods=["POST"])
def analyze():
    """DeepSeek AI 分析"""
    try:
        data = request.get_json()
        stock_name = data.get("stock_name", "")
        stock_code = data.get("stock_code", "")
        predicted = data.get("predicted", [])
        historical = data.get("historical", [])

        if not stock_name:
            return jsonify({"error": "缺少股票名称"}), 400

        pred_text = ""
        for p in predicted:
            m, d = p["date"][4:6], p["date"][6:8]
            pred_text += f"  {m}月{d}日: ¥{p['close']}\n"

        last_price = historical[-1]["close"] if historical else "N/A"
        last_date = historical[-1]["date"] if historical else "N/A"
        if last_date != "N/A":
            last_date = f"{last_date[4:6]}月{last_date[6:8]}日"

        system_prompt = (
            "你是一位专业的股票分析师，拥有丰富的A股市场研究经验。"
            "请根据提供的股价预测数据，结合该上市公司的实际经营业务和最新公开财务数据，"
            "给出专业、客观的投资分析建议。"
            "请用中文回答，语言专业但不晦涩，包含以下三个部分：\n"
            "1. 公司经营业务概述\n"
            "2. 近期财务数据与经营状况\n"
            "3. 综合投资建议（含风险提示）"
        )

        user_prompt = (
            f"请根据以下预测结果，分析股票「{stock_name}」（代码：{stock_code}）"
            f"的经营业务及最新财务数据，并给出投资建议。\n\n"
            f"【当前数据】\n"
            f"最近交易日: {last_date}\n"
            f"最新收盘价: ¥{last_price}\n\n"
            f"【未来5个交易日预测收盘价】\n"
            f"{pred_text}\n"
            f"请基于上述预测数据（注意预测反映的是基于历史价格模式的趋势推演），"
            f"结合你所知的{stock_name}公开信息进行分析。"
        )

        print(f"[AI分析] 正在调用 DeepSeek API 分析 {stock_name}...")
        resp = requests.post(
            DEEPSEEK_API_URL,
            headers={
                "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": DEEPSEEK_MODEL,
                "messages": [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                "temperature": 0.7,
                "max_tokens": 2048,
            },
            timeout=60,
        )

        if resp.status_code != 200:
            print(f"[AI分析] API 调用失败: {resp.status_code}")
            return jsonify({
                "error": f"AI 分析服务暂时不可用 (HTTP {resp.status_code})"
            }), 500

        result = resp.json()
        analysis_text = result["choices"][0]["message"]["content"]
        print(f"[AI分析] 分析完成，共 {len(analysis_text)} 字符")
        return jsonify({"analysis": analysis_text})

    except requests.exceptions.Timeout:
        return jsonify({"error": "AI 分析请求超时，请稍后重试"}), 504
    except requests.exceptions.ConnectionError:
        return jsonify({"error": "无法连接到 AI 分析服务"}), 502
    except Exception as e:
        print(f"[AI分析] 异常: {e}")
        return jsonify({"error": f"AI 分析失败: {str(e)}"}), 500
