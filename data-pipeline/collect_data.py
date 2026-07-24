"""数据管道：定时采集天气数据和市场价格"""
import requests
import json
import os
import time
from datetime import datetime, timedelta

# ===== 天气数据采集 =====

# 云南主要农业城市
CITIES = [
    {"name": "昆明", "lat": 25.04, "lon": 102.68},
    {"name": "大理", "lat": 25.59, "lon": 100.23},
    {"name": "昭通", "lat": 27.34, "lon": 103.72},
    {"name": "曲靖", "lat": 25.49, "lon": 103.79},
    {"name": "文山", "lat": 23.37, "lon": 104.24},
    {"name": "红河", "lat": 23.37, "lon": 103.39},
    {"name": "玉溪", "lat": 24.35, "lon": 102.55},
    {"name": "楚雄", "lat": 25.04, "lon": 101.55},
    {"name": "普洱", "lat": 22.78, "lon": 100.97},
    {"name": "临沧", "lat": 23.88, "lon": 100.09},
]

# 主要作物市场价格参考
CROPS_MARKET = [
    {"name": "水稻", "unit": "元/公斤", "avg_price": 3.5},
    {"name": "玉米", "unit": "元/公斤", "avg_price": 2.8},
    {"name": "马铃薯", "unit": "元/公斤", "avg_price": 2.2},
    {"name": "番茄", "unit": "元/公斤", "avg_price": 5.0},
    {"name": "黄瓜", "unit": "元/公斤", "avg_price": 4.5},
    {"name": "辣椒", "unit": "元/公斤", "avg_price": 8.0},
    {"name": "柑橘", "unit": "元/公斤", "avg_price": 6.5},
    {"name": "大豆", "unit": "元/公斤", "avg_price": 7.0},
    {"name": "小麦", "unit": "元/公斤", "avg_price": 3.2},
]


def fetch_weather(api_key=None):
    """采集天气数据（支持真实API或模拟数据）"""
    results = []
    for city in CITIES:
        if api_key:
            try:
                url = f"https://api.openweathermap.org/data/2.5/weather"
                params = {"lat": city["lat"], "lon": city["lon"], "appid": api_key, "units": "metric", "lang": "zh_cn"}
                resp = requests.get(url, params=params, timeout=10)
                if resp.status_code == 200:
                    data = resp.json()
                    results.append({
                        "city": city["name"],
                        "temperature": data["main"]["temp"],
                        "humidity": data["main"]["humidity"],
                        "weather": data["weather"][0]["description"],
                        "wind_speed": data["wind"]["speed"],
                        "pressure": data["main"]["pressure"],
                        "recorded_at": datetime.now().isoformat(),
                        "is_real": True
                    })
                    continue
            except Exception as e:
                print(f"获取{city['name']}天气失败: {e}")

        # 模拟数据降级
        import random
        results.append({
            "city": city["name"],
            "temperature": round(random.uniform(18, 30), 1),
            "humidity": random.randint(40, 90),
            "weather": random.choice(["晴朗", "多云", "阴天", "小雨"]),
            "wind_speed": round(random.uniform(1, 5), 1),
            "pressure": round(random.uniform(1000, 1020), 1),
            "recorded_at": datetime.now().isoformat(),
            "is_real": False
        })
    return results


def fetch_market_prices():
    """采集市场价格（模拟数据）"""
    import random
    results = []
    today = datetime.now().date()
    for crop in CROPS_MARKET:
        # 模拟30天价格数据
        prices = []
        for i in range(30):
            date = today - timedelta(days=29 - i)
            base = crop["avg_price"]
            variation = random.uniform(-0.5, 0.5)
            prices.append({
                "date": date.isoformat(),
                "price": round(base + variation, 2),
                "unit": crop["unit"]
            })
        results.append({
            "crop_name": crop["name"],
            "prices": prices
        })
    return results


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="农业数据管道")
    parser.add_argument("--type", choices=["weather", "market", "all"], default="all")
    parser.add_argument("--api-key", help="OpenWeatherMap API Key")
    parser.add_argument("--output", default="pipeline_output.json")
    args = parser.parse_args()

    output = {}
    if args.type in ("weather", "all"):
        print("正在采集天气数据...")
        output["weather"] = fetch_weather(args.api_key)
        print(f"天气数据采集完成: {len(output['weather'])} 个城市")

    if args.type in ("market", "all"):
        print("正在采集市场价格...")
        output["market"] = fetch_market_prices()
        print(f"市场价格采集完成: {len(output['market'])} 种作物")

    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    print(f"数据已保存至: {args.output}")
