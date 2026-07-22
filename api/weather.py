from fastapi import APIRouter, Query
from typing import Optional
from models.schemas import WeatherResponse, WeatherInfo
from services.weather_service import WeatherService

router = APIRouter(prefix="/api/v1/weather", tags=["weather"])


@router.get(
    "/city",
    responses={
        200: {"model": WeatherResponse, "description": "获取天气信息成功"}
    },
    summary="根据城市获取天气",
    description="根据城市名称获取天气信息，支持模拟数据和真实API两种模式。"
)
async def get_weather_by_city(
    city: str = Query(..., description="城市名称（如：北京、上海、广州）")
):
    weather_data = WeatherService.get_weather(city)
    return WeatherResponse(
        success=True,
        weather=WeatherInfo(**weather_data)
    )


@router.get(
    "/coords",
    responses={
        200: {"model": WeatherResponse, "description": "获取天气信息成功"}
    },
    summary="根据坐标获取天气",
    description="根据经纬度坐标获取天气信息。"
)
async def get_weather_by_coords(
    lat: float = Query(..., description="纬度"),
    lon: float = Query(..., description="经度")
):
    weather_data = WeatherService.get_weather_by_coords(lat, lon)
    return WeatherResponse(
        success=True,
        weather=WeatherInfo(**weather_data)
    )