from PIL import Image
from io import BytesIO
from dataclasses import dataclass
from typing import Optional
from src.core.config import settings
from .inference_service import DiseaseClassifier, UnknownDiseaseError


@dataclass
class DiagnosisItem:
    """诊断结果内部数据类"""
    disease_name: str
    confidence: float
    description: str
    recommended_treatment: str
    severity: str


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
    def analyze_image(image_bytes: bytes, filename: str) -> list[DiagnosisItem]:
        classifier = DiagnosisService._get_classifier()

        if classifier is None:
            return [
                DiagnosisItem(
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
                "citrus_canker": "柑橘溃疡病是由柑橘黄单胞菌引起的检疫性病害，叶片出现火山口状病斑。",
                "citrus_red_spider": "柑橘红蜘蛛（柑橘全爪螨）吸食叶片汁液，导致叶片灰白早落。",
                "corn_borer": "玉米螟幼虫钻入茎秆和果穗为害，造成茎折和籽粒损失。",
                "corn_leaf_blight": "玉米大斑病由大斑病菌引起，叶片出现大型梭形灰褐色病斑。",
                "corn_smut": "玉米黑粉病由黑粉菌引起，在受害部位形成菌瘿。",
                "cotton_verticillium": "棉花黄萎病由大丽轮枝菌引起，维管束变褐，叶片枯焦。",
                "cucumber_downy_mildew": "黄瓜霜霉病由假霜霉菌引起，叶片出现多角形黄斑，背面有霜状霉层。",
                "cucumber_powdery_mildew": "黄瓜白粉病由白粉菌引起，叶片表面产生白色粉状霉层。",
                "pepper_anthracnose": "辣椒炭疽病由炭疽菌引起，果实出现凹陷黑褐色病斑。",
                "potato_late_blight": "马铃薯晚疫病由致病疫霉菌引起，叶片出现水渍状斑，湿度大时叶背有白霉。",
                "rice_blast": "稻瘟病由稻瘟病菌引起，叶片出现梭形病斑，穗颈瘟造成白穗。",
                "rice_sheath_blight": "水稻纹枯病由立枯丝核菌引起，叶鞘出现椭圆形灰白色斑。",
                "rice_stem_maggot": "水稻秆蝇幼虫钻入稻茎为害，造成枯心苗。",
                "soybean_pod_borer": "大豆食心虫幼虫钻入豆荚取食豆粒，造成豆粒残缺。",
                "tomato_gray_mold": "番茄灰霉病由灰葡萄孢菌引起，果实腐烂，表面有灰色霉层。",
                "tomato_late_blight": "番茄晚疫病由致病疫霉菌引起，是毁灭性病害，叶片出现水渍状斑。",
                "wheat_rust": "小麦锈病由锈菌引起，叶片出现鲜黄色或橙褐色锈粉状夏孢子堆。",
                "wheat_scab": "小麦赤霉病由镰刀菌引起，穗部变黄褐色，产生粉红色霉层和毒素。",
            }

            treatments = {
                "citrus_canker": "铜制剂（氢氧化铜）保护性喷雾，春梢/夏梢/秋梢萌发期各喷药1-2次。",
                "citrus_red_spider": "阿维菌素或哒螨灵喷雾，保护捕食螨等天敌。",
                "corn_borer": "心叶末期用辛硫磷颗粒剂丢心，或释放赤眼蜂进行生物防治。",
                "corn_leaf_blight": "大喇叭口期喷施苯醚甲环唑或吡唑醚菌酯+戊唑醇。",
                "corn_smut": "深耕翻埋菌源，减少机械损伤，早期摘除菌瘿深埋。",
                "cotton_verticillium": "选用抗病品种，与水稻/玉米轮作3-5年，发病初期恶霉灵灌根。",
                "cucumber_downy_mildew": "加强通风降湿，发病初期用烯酰吗啉或霜脲·锰锌喷雾。",
                "cucumber_powdery_mildew": "加强通风光照，用醚菌酯或戊唑醇喷雾，注意交替用药。",
                "pepper_anthracnose": "发病初期用咪鲜胺或苯醚甲环唑喷雾，7-10天一次，连续2-3次。",
                "potato_late_blight": "现蕾期至开花期是防治关键期，用烯酰吗啉·霜脲氰或氟吡菌胺喷雾。",
                "rice_blast": "分蘖盛期喷施三环唑预防，抽穗期喷施稻瘟灵治疗，破口期和齐穗期各一次。",
                "rice_sheath_blight": "分蘖末期至孕穗期喷施井冈霉素或苯醚甲环唑+丙环唑。",
                "rice_stem_maggot": "苗期至分蘖期用毒死蜱或阿维菌素喷雾，清除田边杂草。",
                "soybean_pod_borer": "大豆结荚期（成虫产卵盛期）用高效氯氟氰菊酯或毒死蜱喷雾。",
                "tomato_gray_mold": "加强通风降湿，及时摘除病叶病果，用腐霉利或嘧霉胺喷雾。",
                "tomato_late_blight": "高畦栽培、地膜覆盖降低湿度，发病初期用甲霜灵锰锌或霜脲·锰锌喷雾。",
                "wheat_rust": "病叶率>5%时用三唑酮或戊唑醇喷雾，7-10天一次，视病情1-3次。",
                "wheat_scab": "抽穗扬花期（始花至盛花）用戊唑醇·咪鲜胺或氰烯菌酯预防。",
            }

            description = descriptions.get(disease_name, f"由{settings.NUM_CLASSES}类病害模型识别")
            treatment = treatments.get(disease_name, "请咨询当地农技人员获取防治建议")
            severity = "轻微" if confidence < 0.7 else ("中等" if confidence < 0.9 else "严重")

            return [
                DiagnosisItem(
                    disease_name=disease_name,
                    confidence=confidence,
                    description=description,
                    recommended_treatment=treatment,
                    severity=severity
                )
            ]

        except UnknownDiseaseError as e:
            return [
                DiagnosisItem(
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
