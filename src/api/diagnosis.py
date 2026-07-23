from fastapi import APIRouter, UploadFile, File, HTTPException
from typing import Union
from ..models.schemas import DiseaseAdviceResponse, PendingReviewResponse, ErrorResponse
from ..services.inference_service import DiseaseClassifier, UnknownDiseaseError
from ..services.agent_service import AgentService
from ..services.diagnosis_service import DiagnosisService

router = APIRouter(prefix="/api/v1/diagnosis", tags=["diagnosis"])


@router.post("/image", responses={
    200: {"model": Union[DiseaseAdviceResponse, PendingReviewResponse]},
    400: {"model": ErrorResponse}
})
async def diagnose_image(
    image: UploadFile = File(...),
    crop_info: str = "未知作物",
    weather_info: str = "未知天气"
):
    image_bytes = await image.read()
    
    is_valid, error_msg = DiagnosisService.validate_image(image_bytes, image.filename)
    if not is_valid:
        raise HTTPException(status_code=400, detail=error_msg)
    
    classifier = DiseaseClassifier()
    
    try:
        disease_name, confidence = classifier.predict_from_bytes(image_bytes)
        
        advice = AgentService.generate_advice(
            disease_name=disease_name,
            crop_info=crop_info,
            weather_info=weather_info
        )
        
        return DiseaseAdviceResponse(
            disease_name=disease_name,
            confidence=confidence,
            advice=advice
        )
    
    except UnknownDiseaseError:
        return PendingReviewResponse(
            status="pending_review",
            message="未知病害，已转入人工审核"
        )