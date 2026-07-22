from pydantic import BaseModel, Field
from typing import Optional, List


class DiagnosisResult(BaseModel):
    disease_name: str = Field(..., description="识别到的病害名称")
    confidence: float = Field(..., ge=0, le=1, description="置信度（0-1）")
    description: str = Field(..., description="病害描述")
    recommended_treatment: str = Field(..., description="推荐处理方案")
    severity: str = Field(..., description="病害严重程度", pattern="^(轻微|中等|严重)$")


class DiagnosisResponse(BaseModel):
    success: bool = Field(..., description="请求是否成功")
    message: str = Field(..., description="响应消息")
    results: Optional[List[DiagnosisResult]] = Field(None, description="诊断结果列表")
    image_info: Optional[dict] = Field(None, description="上传图片信息")


class DiseaseAdviceResponse(BaseModel):
    disease_name: str = Field(..., description="识别到的病害名称")
    confidence: float = Field(..., ge=0, le=1, description="置信度（0-1）")
    advice: str = Field(..., description="生成的综合防治建议")


class PendingReviewResponse(BaseModel):
    status: str = Field("pending_review", description="状态")
    message: str = Field(..., description="消息")


class ErrorResponse(BaseModel):
    success: bool = Field(False, description="请求是否成功")
    message: str = Field(..., description="错误消息")
    error_code: Optional[int] = Field(None, description="错误代码")