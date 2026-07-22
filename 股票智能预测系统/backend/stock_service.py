"""
股票数据服务层
- 通过 akshare 获取 A 股数据（免费、无需 token）
- 股票名称 → 代码自动转换（支持模糊搜索）
- 兜底方案：akshare 不可用时使用本地缓存
"""

import json
import os
from datetime import datetime, timedelta

import akshare as ak
import numpy as np
import pandas as pd

# 缓存文件路径
CACHE_DIR = os.path.join(os.path.dirname(__file__), ".cache")
STOCK_LIST_CACHE = os.path.join(CACHE_DIR, "stock_list.json")


def _ensure_cache_dir():
    """确保缓存目录存在"""
    if not os.path.exists(CACHE_DIR):
        os.makedirs(CACHE_DIR)


def _code_to_exchange_prefix(code: str) -> str:
    """
    根据股票代码判断交易所前缀

    规则：
    - 60xxxx, 68xxxx → 上海交易所 (sh)
    - 00xxxx, 30xxxx → 深圳交易所 (sz)
    """
    code = str(code).zfill(6)
    if code.startswith("60") or code.startswith("68"):
        return "sh"
    else:
        return "sz"


def _code_to_ak_symbol(code: str) -> str:
    """
    将纯数字代码转换为 akshare 格式
    例: '000001' → 'sz000001', '600000' → 'sh600000'
    """
    code = str(code).zfill(6)
    prefix = _code_to_exchange_prefix(code)
    return prefix + code


def _load_stock_list_from_cache() -> pd.DataFrame | None:
    """从本地缓存加载股票列表"""
    if os.path.exists(STOCK_LIST_CACHE):
        try:
            with open(STOCK_LIST_CACHE, "r", encoding="utf-8") as f:
                data = json.load(f)
            cache_time = datetime.fromisoformat(data["cached_at"])
            # 缓存有效期：7天
            if datetime.now() - cache_time < timedelta(days=7):
                return pd.DataFrame(data["stocks"])
        except Exception:
            pass
    return None


def _save_stock_list_to_cache(df: pd.DataFrame):
    """保存股票列表到本地缓存"""
    _ensure_cache_dir()
    data = {
        "cached_at": datetime.now().isoformat(),
        "stocks": df.to_dict(orient="records"),
    }
    with open(STOCK_LIST_CACHE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def get_stock_list() -> pd.DataFrame:
    """
    获取 A 股股票列表（带缓存）
    返回包含 code, name 的 DataFrame
    """
    # 先尝试从缓存加载
    cached = _load_stock_list_from_cache()
    if cached is not None:
        return cached

    # 从 akshare 获取
    try:
        df = ak.stock_info_a_code_name()
        df = df.rename(columns={"code": "code", "name": "name"})
        # 确保 code 是 6 位字符串
        df["code"] = df["code"].astype(str).str.zfill(6)
        _save_stock_list_to_cache(df)
        return df
    except Exception as e:
        raise RuntimeError(f"获取股票列表失败: {e}")


def search_stock_by_name(keyword: str) -> list[dict]:
    """
    根据股票名称模糊搜索

    Args:
        keyword: 股票名称关键词（如"平安"、"茅台"）

    Returns:
        list[dict]: 匹配的股票列表，每项含 name, code
    """
    df = get_stock_list()
    # 模糊匹配：名称包含关键词（不区分大小写）
    mask = df["name"].str.contains(keyword, na=False, case=False)
    results = df[mask].head(20)  # 最多返回20条

    stocks = []
    for _, row in results.iterrows():
        code = str(row["code"]).zfill(6)
        exchange = "沪市" if code.startswith(("60", "68")) else "深市"
        stocks.append(
            {
                "name": row["name"],
                "code": code,
                "ts_code": code + (".SH" if exchange == "沪市" else ".SZ"),
                "exchange": exchange,
            }
        )
    return stocks


def get_stock_code_by_name(name: str) -> str | None:
    """
    精确匹配股票名称 → 股票代码 (纯数字)

    Args:
        name: 股票名称（如"平安银行"）

    Returns:
        6位股票代码字符串或 None
    """
    df = get_stock_list()
    match = df[df["name"] == name.strip()]
    if len(match) > 0:
        return str(match.iloc[0]["code"]).zfill(6)
    return None


def get_daily_data(code: str, limit: int = 500) -> pd.DataFrame:
    """
    获取股票日线数据 (通过 akshare)

    Args:
        code: 6位股票代码（如"000001"）
        limit: 获取的数据条数（从历史累积数据中取最近 N 条）

    Returns:
        DataFrame: 包含 date, close 等字段，按日期升序排列
    """
    try:
        symbol = _code_to_ak_symbol(code)
        # akshare 返回该股票全部历史日线数据
        df = ak.stock_zh_a_daily(symbol=symbol, adjust="qfq")

        if df is None or len(df) == 0:
            raise RuntimeError(f"未获取到 {code} 的日线数据")

        # 取最近 limit 条
        df = df.tail(limit).copy()
        # 重命名列以统一接口
        df = df.rename(columns={"date": "trade_date"})
        # 将日期统一转为 "YYYYMMDD" 字符串格式
        if pd.api.types.is_datetime64_any_dtype(df["trade_date"]):
            df["trade_date"] = df["trade_date"].dt.strftime("%Y%m%d")
        else:
            df["trade_date"] = df["trade_date"].astype(str).str.replace("-", "")
        # 按日期升序排列
        df = df.sort_values("trade_date", ascending=True).reset_index(drop=True)
        return df
    except Exception as e:
        raise RuntimeError(f"获取 {code} 日线数据失败: {e}")


def get_recent_close_prices(code: str, days: int = 20) -> tuple[np.ndarray, list[str]]:
    """
    获取最近 N 个交易日的收盘价

    Args:
        code: 6位股票代码
        days: 需要的交易日数量

    Returns:
        tuple: (收盘价数组 [float], 交易日期列表 [str, YYYYMMDD])
    """
    df = get_daily_data(code, limit=days + 10)
    # 取最近 days 条
    df = df.tail(days)
    prices = df["close"].values.astype(np.float64)
    dates = df["trade_date"].tolist()
    return prices, dates


def get_training_data(code: str, days: int = 500) -> tuple[np.ndarray, list[str]]:
    """
    获取用于模型训练的大量历史数据

    Args:
        code: 6位股票代码
        days: 获取的天数

    Returns:
        tuple: (收盘价数组, 交易日期列表)
    """
    df = get_daily_data(code, limit=days)
    prices = df["close"].values.astype(np.float64)
    dates = df["trade_date"].tolist()
    return prices, dates
