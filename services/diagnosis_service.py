from PIL import Image
from io import BytesIO
from models.schemas import DiagnosisResult
from core.config import settings
from services.inference_service import DiseaseClassifier, UnknownDiseaseError


class DiagnosisService:
    _classifier = None

    @classmethod
    def _get_classifier(cls):
        if cls._classifier is None:
            try:
                cls._classifier = DiseaseClassifier()
            except FileNotFoundError:
                cls._classifier = None
        return cls._classifier

    @staticmethod
    def analyze_image(image_bytes: bytes, filename: str) -> list[DiagnosisResult]:
        classifier = DiagnosisService._get_classifier()
        
        if classifier is None:
            return [
                DiagnosisResult(
                    disease_name="模型未加载",
                    confidence=0.0,
                    description="模型文件未找到，使用默认诊断结果",
                    recommended_treatment="请先训练模型并保存权重",
                    severity="未知"
                )
            ]
        
        try:
            disease_name, confidence = classifier.predict_from_bytes(image_bytes)
            
            descriptions = {
                "番茄晚疫病": "番茄晚疫病是由疫霉菌引起的一种毁灭性病害，主要危害番茄的叶片、茎和果实。",
                "番茄灰霉病": "番茄灰霉病是由灰葡萄孢菌引起的病害，主要危害果实，也能危害叶片和茎。",
                "番茄早疫病": "番茄早疫病是由链格孢菌引起的病害，主要危害叶片，形成同心轮纹状病斑。",
                "番茄叶霉病": "番茄叶霉病是由褐孢霉引起的病害，主要危害叶片，叶背产生紫灰色霉层。",
                "番茄病毒病": "番茄病毒病是由多种病毒引起的病害，表现为叶片皱缩、黄化、畸形等症状。",
                "黄瓜霜霉病": "黄瓜霜霉病是由假霜霉菌引起的病害，叶片出现黄色多角形病斑，背面有霜状霉层。",
                "黄瓜白粉病": "黄瓜白粉病是由白粉菌引起的病害，叶片表面产生白色粉状霉层。",
                "黄瓜炭疽病": "黄瓜炭疽病是由炭疽菌引起的病害，叶片和果实上产生圆形褐色病斑。",
                "黄瓜细菌性角斑病": "黄瓜细菌性角斑病是由假单胞杆菌引起的病害，叶片出现水渍状角形病斑。",
                "健康": "作物生长健康，未发现明显病害症状。"
            }
            
            treatments = {
                "番茄晚疫病": "及时摘除病叶，使用甲霜灵锰锌或霜霉威盐酸盐等药剂进行防治。",
                "番茄灰霉病": "加强通风，降低湿度，使用腐霉利或异菌脲等药剂进行防治。",
                "番茄早疫病": "及时清除病叶，使用代森锰锌或苯醚甲环唑等药剂进行防治。",
                "番茄叶霉病": "加强通风透光，使用嘧菌酯或肟菌酯等药剂进行防治。",
                "番茄病毒病": "及时防治蚜虫等传毒媒介，发病初期可喷施病毒抑制剂。",
                "黄瓜霜霉病": "加强通风，降低湿度，使用烯酰吗啉或霜霉威等药剂进行防治。",
                "黄瓜白粉病": "加强通风透光，使用醚菌酯或戊唑醇等药剂进行防治。",
                "黄瓜炭疽病": "及时清除病残体，使用咪鲜胺或苯醚甲环唑等药剂进行防治。",
                "黄瓜细菌性角斑病": "选用抗病品种，使用农用链霉素或噻唑锌等药剂进行防治。",
                "健康": "继续保持良好的栽培管理措施。"
            }
            
            severity = "轻微" if confidence < 0.7 else ("中等" if confidence < 0.9 else "严重")
            
            return [
                DiagnosisResult(
                    disease_name=disease_name,
                    confidence=confidence,
                    description=descriptions.get(disease_name, "病害描述暂未收录"),
                    recommended_treatment=treatments.get(disease_name, "暂无推荐处理方案"),
                    severity=severity
                )
            ]
        
        except UnknownDiseaseError as e:
            return [
                DiagnosisResult(
                    disease_name="未知病害",
                    confidence=0.0,
                    description=str(e),
                    recommended_treatment="需要人工审核确认病害类型",
                    severity="未知"
                )
            ]

    @staticmethod
    def validate_image(image_bytes: bytes, filename: str) -> tuple[bool, str]:
        if len(image_bytes) > settings.MAX_IMAGE_SIZE:
            return False, f"图片大小超过限制（最大 {settings.MAX_IMAGE_SIZE // 1024 // 1024}MB）"
        
        extension = filename.split(".")[-1].lower()
        if extension not in settings.allowed_extensions_list:
            return False, f"不支持的图片格式，支持的格式：{', '.join(settings.allowed_extensions_list)}"
        
        try:
            Image.open(BytesIO(image_bytes))
        except Exception as e:
            return False, f"无效的图片文件：{str(e)}"
        
        return True, ""