from datetime import date as Date
from typing import Any, Dict, List, Literal, Optional

from pydantic import BaseModel, Field


class WeatherInfo(BaseModel):
    city: str = Field(..., description="城市名称")
    temperature: float = Field(..., description="温度（摄氏度）")
    humidity: int = Field(..., description="湿度（%）")
    weather: str = Field(..., description="天气状况")
    wind_speed: float = Field(..., description="风速（m/s）")
    rain: float = Field(..., description="降雨量（mm）")
    clouds: int = Field(..., description="云量（%）")
    is_real: bool = Field(False, description="是否为真实数据")
    note: Optional[str] = Field(None, description="备注信息")


class ReferenceSource(BaseModel):
    content: str = Field(..., description="参考内容片段")
    source: str = Field(..., description="来源文件")
    score: float = Field(..., description="相似度分数")
    metadata: Optional[Dict] = Field(None, description="元数据")


class DiseaseAdvice(BaseModel):
    advice: str = Field(..., description="生成的综合防治建议（Markdown格式）")
    references: List[ReferenceSource] = Field(default_factory=list, description="参考资料列表")
    weather_info: str = Field("", description="天气信息")


class DiseaseClassification(BaseModel):
    disease_name: str = Field(..., description="识别到的病害名称")
    confidence: float = Field(..., ge=0, le=1, description="置信度（0-1）")
    description: Optional[str] = Field(None, description="病害描述")
    severity: str = Field("未知", description="病害严重程度", pattern="^(轻微|中等|严重|未知)$")


class DiagnosisResult(BaseModel):
    classification: DiseaseClassification = Field(..., description="病害分类结果")
    weather: Optional[WeatherInfo] = Field(None, description="天气信息")
    advice: Optional[DiseaseAdvice] = Field(None, description="防治建议")


class DiseaseAdviceResponse(BaseModel):
    disease_name: str = Field(..., description="识别到的病害名称")
    confidence: float = Field(..., ge=0, le=1, description="置信度（0-1）")
    advice: str = Field(..., description="生成的综合防治建议")
    references: List[ReferenceSource] = Field(default_factory=list, description="参考资料列表")
    weather_info: str = Field("", description="天气信息")


class CropContext(BaseModel):
    name: str = Field("未知作物", description="作物名称")
    variety: Optional[str] = Field(None, description="品种")
    planting_date: Optional[Date] = Field(None, description="种植日期")
    growth_stage: Optional[str] = Field(None, description="生育期代码")


class FieldContext(BaseModel):
    name: str = Field("未知地块", description="地块名称")
    farm_name: str = Field("未知农场", description="农场名称")


class WeatherForecastItem(BaseModel):
    date: Date = Field(..., description="预报日期")
    weather: str = Field("未知", description="天气现象")
    temperature: Optional[float] = Field(None, description="温度（摄氏度）")
    humidity: Optional[float] = Field(None, description="湿度（%）")
    rainfall: Optional[float] = Field(None, description="降雨量（mm）")
    wind_speed: Optional[float] = Field(None, description="风速（m/s）")


class AgentTrace(BaseModel):
    agent: str
    status: Literal["completed", "no-data"]
    summary: str


class AdviceRequest(BaseModel):
    disease_name: str = Field(..., description="病害名称")
    confidence: float = Field(..., ge=0, le=1, description="识别置信度")
    crop: CropContext = Field(default_factory=CropContext)
    field: FieldContext = Field(default_factory=FieldContext)
    weather_forecast: List[WeatherForecastItem] = Field(default_factory=list)
    citations: List[Dict[str, Any]] = Field(default_factory=list, description="RAG 引用")


class AdviceResponse(BaseModel):
    advice: str
    references: List[Dict[str, Any]] = Field(default_factory=list)
    context_summary: Dict[str, Any] = Field(default_factory=dict)
    agent_trace: List[AgentTrace] = Field(default_factory=list)
    weather_info: str = "未知天气"


class PendingReviewResponse(BaseModel):
    status: str = Field("pending_review", description="状态")
    message: str = Field(..., description="消息")


class ErrorResponse(BaseModel):
    success: bool = Field(False, description="请求是否成功")
    message: str = Field(..., description="错误消息")
    error_code: Optional[int] = Field(None, description="错误代码")


class HealthResponse(BaseModel):
    status: str = Field(..., description="服务状态")
    app_name: str = Field(..., description="应用名称")
    version: str = Field(..., description="版本号")
    model_loaded: bool = Field(..., description="模型是否已加载")
    vector_db_ready: bool = Field(..., description="向量数据库是否就绪")


class IngestRequest(BaseModel):
    docs_dir: str = Field(..., description="文档目录路径")


class IngestResponse(BaseModel):
    success: bool = Field(..., description="是否成功")
    message: str = Field(..., description="消息")
    chunks_count: int = Field(..., description="分块数量")


class KnowledgeSyncDocument(BaseModel):
    id: str
    title: str
    category: str
    version: int
    content: str
    tags: List[str] = Field(default_factory=list)


class KnowledgeSyncRequest(BaseModel):
    documents: List[KnowledgeSyncDocument] = Field(default_factory=list)


class KnowledgeSyncResponse(BaseModel):
    success: bool
    documents_count: int
    chunks_count: int


class RetrieveRequest(BaseModel):
    query: str = Field(..., description="查询文本")
    top_k: Optional[int] = Field(3, description="返回数量")


class RetrieveResponse(BaseModel):
    success: bool = Field(..., description="是否成功")
    results: List[ReferenceSource] = Field(..., description="检索结果")


class DiseaseListResponse(BaseModel):
    diseases: List[str] = Field(..., description="支持的病害列表")


class WeatherResponse(BaseModel):
    success: bool = Field(..., description="是否成功")
    weather: WeatherInfo = Field(..., description="天气信息")