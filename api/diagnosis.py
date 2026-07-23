from fastapi import APIRouter, UploadFile, File, HTTPException, Query
from typing import Optional, Union
from models.schemas import (
    DiseaseAdviceResponse, PendingReviewResponse, ErrorResponse,
    DiseaseClassification, DiagnosisResult, DiseaseListResponse
)
from services.inference_service import DiseaseClassifier, UnknownDiseaseError
from services.agent_service import AgentService
from services.diagnosis_service import DiagnosisService
from services.weather_service import WeatherService
from core.config import settings

router = APIRouter(prefix="/api/v1/diagnosis", tags=["diagnosis"])


@router.post(
    "/image",
    responses={
        200: {"model": DiseaseAdviceResponse, "description": "诊断成功，返回病害信息和防治建议"},
        400: {"model": ErrorResponse, "description": "请求参数错误"},
        500: {"model": ErrorResponse, "description": "服务器内部错误"}
    },
    summary="图片诊断接口",
    description="上传作物叶片图片进行病害诊断，返回病害名称、置信度和综合防治建议。"
)
async def diagnose_image(
    image: UploadFile = File(..., description="作物叶片图片（支持jpg、jpeg、png格式）"),
    crop_info: str = Query("未知作物", description="作物信息（如：番茄、黄瓜、土豆等）"),
    weather_info: str = Query("未知天气", description="天气信息（如：晴朗、25°C、湿度60%）"),
    city: Optional[str] = Query(None, description="城市名称，用于获取实时天气（如：北京、上海）"),
    lat: Optional[float] = Query(None, description="纬度，与lon配合使用获取实时天气"),
    lon: Optional[float] = Query(None, description="经度，与lat配合使用获取实时天气")
):
    image_bytes = await image.read()
    
    is_valid, error_msg = DiagnosisService.validate_image(image_bytes, image.filename)
    if not is_valid:
        raise HTTPException(status_code=400, detail=error_msg)
    
    weather_data = None
    if city:
        weather_data = WeatherService.get_weather(city)
    elif lat is not None and lon is not None:
        weather_data = WeatherService.get_weather_by_coords(lat, lon)
    
    classifier = DiseaseClassifier()
    
    try:
        disease_name, confidence = classifier.predict_from_bytes(image_bytes)
        
        advice_result = AgentService.generate_advice(
            disease_name=disease_name,
            crop_info=crop_info,
            weather_info=weather_info,
            weather_data=weather_data
        )
        
        return DiseaseAdviceResponse(
            disease_name=disease_name,
            confidence=confidence,
            advice=advice_result["advice"],
            references=advice_result["references"],
            weather_info=advice_result["weather_info"]
        )
    
    except UnknownDiseaseError:
        return PendingReviewResponse(
            status="pending_review",
            message="未知病害，已转入人工审核"
        )


@router.post(
    "/simple",
    responses={
        200: {"model": DiseaseClassification, "description": "诊断成功，返回病害分类结果"},
        400: {"model": ErrorResponse, "description": "请求参数错误"}
    },
    summary="简单诊断接口",
    description="仅进行病害识别，不生成详细防治建议，返回速度更快。"
)
async def diagnose_simple(
    image: UploadFile = File(..., description="作物叶片图片"),
    crop_info: str = Query("未知作物", description="作物信息")
):
    image_bytes = await image.read()
    
    is_valid, error_msg = DiagnosisService.validate_image(image_bytes, image.filename)
    if not is_valid:
        raise HTTPException(status_code=400, detail=error_msg)
    
    results = DiagnosisService.analyze_image(image_bytes, image.filename)
    
    if results:
        result = results[0]
        return DiseaseClassification(
            disease_name=result.disease_name,
            confidence=result.confidence,
            description=result.description,
            severity=result.severity
        )
    else:
        raise HTTPException(status_code=500, detail="诊断失败")


@router.post(
    "/full",
    responses={
        200: {"model": DiagnosisResult, "description": "诊断成功，返回完整诊断结果"},
        400: {"model": ErrorResponse, "description": "请求参数错误"}
    },
    summary="完整诊断接口",
    description="进行病害识别，获取天气信息，生成防治建议，返回完整的诊断结果对象。"
)
async def diagnose_full(
    image: UploadFile = File(..., description="作物叶片图片"),
    crop_info: str = Query("未知作物", description="作物信息"),
    city: Optional[str] = Query(None, description="城市名称"),
    lat: Optional[float] = Query(None, description="纬度"),
    lon: Optional[float] = Query(None, description="经度")
):
    image_bytes = await image.read()
    
    is_valid, error_msg = DiagnosisService.validate_image(image_bytes, image.filename)
    if not is_valid:
        raise HTTPException(status_code=400, detail=error_msg)
    
    weather_data = None
    if city:
        weather_data = WeatherService.get_weather(city)
    elif lat is not None and lon is not None:
        weather_data = WeatherService.get_weather_by_coords(lat, lon)
    
    classifier = DiseaseClassifier()
    
    try:
        disease_name, confidence = classifier.predict_from_bytes(image_bytes)
        
        severity = "轻微" if confidence < 0.7 else ("中等" if confidence < 0.9 else "严重")
        
        advice_result = AgentService.generate_advice(
            disease_name=disease_name,
            crop_info=crop_info,
            weather_data=weather_data
        )
        
        return DiagnosisResult(
            classification=DiseaseClassification(
                disease_name=disease_name,
                confidence=confidence,
                severity=severity
            ),
            weather=weather_data,
            advice=advice_result
        )
    
    except UnknownDiseaseError:
        return DiagnosisResult(
            classification=DiseaseClassification(
                disease_name="未知病害",
                confidence=0.0,
                severity="未知"
            ),
            weather=weather_data,
            advice=None
        )


@router.get(
    "/diseases",
    responses={
        200: {"model": DiseaseListResponse, "description": "返回支持的病害列表"}
    },
    summary="获取支持的病害列表",
    description="获取模型支持识别的所有病害名称列表。"
)
async def get_disease_list():
    try:
        classifier = DiseaseClassifier()
        diseases = classifier.class_names
        return DiseaseListResponse(diseases=diseases)
    except Exception:
        return DiseaseListResponse(diseases=[])