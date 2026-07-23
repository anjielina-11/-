import requests
from typing import Optional, Dict
from core.config import settings


class WeatherService:
    @classmethod
    def get_weather(cls, city: str) -> Dict:
        api_key = settings.WEATHER_API_KEY
        api_base = settings.WEATHER_API_BASE
        
        if not api_key:
            return cls._get_mock_weather(city)
        
        try:
            url = f"{api_base}/weather"
            params = {
                "q": city,
                "appid": api_key,
                "units": "metric",
                "lang": "zh_cn"
            }
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()
            return cls._format_weather_response(data)
        except Exception:
            return cls._get_mock_weather(city)

    @classmethod
    def get_weather_by_coords(cls, lat: float, lon: float) -> Dict:
        api_key = settings.WEATHER_API_KEY
        api_base = settings.WEATHER_API_BASE
        
        if not api_key:
            return cls._get_mock_weather_by_coords(lat, lon)
        
        try:
            url = f"{api_base}/weather"
            params = {
                "lat": lat,
                "lon": lon,
                "appid": api_key,
                "units": "metric",
                "lang": "zh_cn"
            }
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()
            return cls._format_weather_response(data)
        except Exception:
            return cls._get_mock_weather_by_coords(lat, lon)

    @classmethod
    def _format_weather_response(cls, data: Dict) -> Dict:
        return {
            "city": data.get("name", "未知城市"),
            "temperature": data.get("main", {}).get("temp", 0),
            "humidity": data.get("main", {}).get("humidity", 0),
            "weather": data.get("weather", [{}])[0].get("description", "未知"),
            "wind_speed": data.get("wind", {}).get("speed", 0),
            "rain": data.get("rain", {}).get("1h", 0),
            "clouds": data.get("clouds", {}).get("all", 0),
            "is_real": True
        }

    @classmethod
    def _get_mock_weather(cls, city: str) -> Dict:
        weather_patterns = {
            "北京": {"weather": "晴朗", "temperature": 28, "humidity": 45, "wind_speed": 3, "rain": 0},
            "上海": {"weather": "多云", "temperature": 30, "humidity": 70, "wind_speed": 4, "rain": 0},
            "广州": {"weather": "小雨", "temperature": 26, "humidity": 85, "wind_speed": 2, "rain": 5},
            "成都": {"weather": "阴", "temperature": 24, "humidity": 80, "wind_speed": 1, "rain": 0},
            "西安": {"weather": "晴朗", "temperature": 32, "humidity": 35, "wind_speed": 5, "rain": 0},
        }
        
        pattern = weather_patterns.get(city, {"weather": "多云", "temperature": 25, "humidity": 60, "wind_speed": 3, "rain": 0})
        
        return {
            "city": city,
            "temperature": pattern["temperature"],
            "humidity": pattern["humidity"],
            "weather": pattern["weather"],
            "wind_speed": pattern["wind_speed"],
            "rain": pattern["rain"],
            "clouds": 40,
            "is_real": False,
            "note": "当前使用模拟天气数据，请配置 WEATHER_API_KEY 以获取真实天气"
        }

    @classmethod
    def _get_mock_weather_by_coords(cls, lat: float, lon: float) -> Dict:
        if lat > 30:
            weather = "晴朗"
            temp = 28
            humidity = 45
        else:
            weather = "多云"
            temp = 25
            humidity = 70
            
        return {
            "city": f"坐标({lat:.2f}, {lon:.2f})",
            "temperature": temp,
            "humidity": humidity,
            "weather": weather,
            "wind_speed": 3,
            "rain": 0,
            "clouds": 40,
            "is_real": False,
            "note": "当前使用模拟天气数据，请配置 WEATHER_API_KEY 以获取真实天气"
        }