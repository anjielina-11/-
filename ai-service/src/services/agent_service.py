import json
from datetime import date
from typing import Dict, List, Optional

import requests

from .rag_service import RAGService
from ..core.config import settings
from ..models.schemas import (
    AdviceRequest,
    AdviceResponse,
    AgentTrace,
    CropContext,
    FieldContext,
    WeatherForecastItem,
)


class AgentService:
    GROWTH_STAGE_LABELS = {
        "sowing": "播种期",
        "seedling": "苗期",
        "tillering": "分蘖期",
        "flowering": "开花期",
        "fruiting": "结果期",
        "maturity": "成熟期",
    }

    @classmethod
    def generate_advice(
        cls,
        request: Optional[AdviceRequest] = None,
        *,
        disease_name: Optional[str] = None,
        crop_info: str = "未知作物",
        weather_info: str = "未知天气",
        weather_data: Optional[Dict] = None,
        citations: Optional[List[Dict]] = None,
    ) -> AdviceResponse:
        structured_request = request is not None
        if request is None:
            forecast = []
            if weather_data:
                forecast = [
                    WeatherForecastItem(
                        date=date.today(),
                        weather=str(weather_data.get("weather", "未知")),
                        temperature=weather_data.get("temperature"),
                        humidity=weather_data.get("humidity"),
                        rainfall=weather_data.get("rain", weather_data.get("rainfall")),
                        wind_speed=weather_data.get("wind_speed"),
                    )
                ]
            request = AdviceRequest(
                disease_name=disease_name or "未知病害",
                confidence=0.0,
                crop=CropContext(name=crop_info),
                field=FieldContext(),
                weather_forecast=forecast,
                citations=citations or [],
            )

        reference_docs = list(request.citations)
        if not structured_request and citations is None:
            reference_docs = cls._retrieve_reference(request.disease_name)

        weather_status, weather_summary, weather_risk = cls._analyze_weather(
            request.weather_forecast,
            legacy_weather_info=weather_info if not structured_request else None,
        )
        growth_status, growth_summary = cls._analyze_growth_stage(request)
        rag_status, rag_summary = cls._summarize_rag(reference_docs)
        prompt = cls._build_prompt(request, weather_summary, weather_risk, growth_summary, reference_docs)
        fallback = cls._generate_fallback_response(
            request,
            weather_summary,
            weather_risk,
            growth_summary,
            reference_docs,
        )
        advice = cls._call_llm(prompt, fallback)

        context_summary = {
            "crop_name": request.crop.name,
            "variety": request.crop.variety,
            "growth_stage": request.crop.growth_stage,
            "growth_stage_label": cls.GROWTH_STAGE_LABELS.get(request.crop.growth_stage or "", "未知生育期"),
            "field_name": request.field.name,
            "farm_name": request.field.farm_name,
            "weather_days": len(request.weather_forecast),
            "disease_name": request.disease_name,
            "confidence": request.confidence,
        }
        trace = [
            AgentTrace(agent="weather-risk", status=weather_status, summary=weather_risk),
            AgentTrace(agent="growth-stage", status=growth_status, summary=growth_summary),
            AgentTrace(agent="rag-evidence", status=rag_status, summary=rag_summary),
            AgentTrace(agent="treatment", status="completed", summary="已综合病害、天气、生育期和知识证据生成建议"),
        ]
        return AdviceResponse(
            advice=advice,
            references=reference_docs,
            context_summary=context_summary,
            agent_trace=trace,
            weather_info=weather_summary,
        )

    @classmethod
    def _analyze_weather(
        cls,
        forecast: List[WeatherForecastItem],
        legacy_weather_info: Optional[str] = None,
    ) -> tuple[str, str, str]:
        if not forecast:
            if legacy_weather_info and legacy_weather_info != "未知天气":
                return "completed", legacy_weather_info, f"天气信息：{legacy_weather_info}"
            return "no-data", "暂无未来七天天气数据", "未取得天气数据，施药前需再次确认降雨和风力"

        lines = []
        risks = []
        for item in forecast:
            details = [item.weather]
            if item.temperature is not None:
                details.append(f"{item.temperature:g}°C")
            if item.humidity is not None:
                details.append(f"湿度{item.humidity:g}%")
                if item.humidity >= 80:
                    risks.append(f"{item.date.isoformat()} 高湿{item.humidity:g}%可能加快病害扩散")
            if item.rainfall is not None:
                details.append(f"降雨{item.rainfall:g}mm")
                if item.rainfall > 0:
                    risks.append(f"{item.date.isoformat()} 有降雨，避免雨前或雨中施药")
            if item.temperature is not None and item.temperature >= 35:
                risks.append(f"{item.date.isoformat()} 高温，避免正午施药")
            lines.append(f"{item.date.isoformat()} {'，'.join(details)}")
        risk_summary = "；".join(dict.fromkeys(risks)) if risks else "未来天气未发现明显高湿、降雨或高温施药风险"
        return "completed", "；".join(lines), risk_summary

    @classmethod
    def _analyze_growth_stage(cls, request: AdviceRequest) -> tuple[str, str]:
        code = request.crop.growth_stage
        if not code:
            return "no-data", "未登记生育期，需结合现场长势复核"
        label = cls.GROWTH_STAGE_LABELS.get(code, code)
        planting = request.crop.planting_date.isoformat() if request.crop.planting_date else "未登记种植日期"
        return "completed", f"当前为{label}，种植日期：{planting}"

    @classmethod
    def _summarize_rag(cls, reference_docs: List[Dict]) -> tuple[str, str]:
        if not reference_docs:
            return "no-data", "未检索到知识库证据，建议由农技人员复核"
        sources = [str(doc.get("source") or doc.get("title") or f"文档{i + 1}") for i, doc in enumerate(reference_docs[:3])]
        return "completed", f"已引用{len(reference_docs)}条知识证据：{'、'.join(sources)}"

    @classmethod
    def _retrieve_reference(cls, disease_name: str) -> List[Dict]:
        try:
            all_results = []
            for query in (f"{disease_name} 防治方法", f"{disease_name} 症状识别", f"{disease_name} 农药选择"):
                all_results.extend(RAGService.retrieve(query, top_k=2))
            all_results.sort(key=lambda item: item.get("score", 1.0))
            unique_results = []
            seen = set()
            for doc in all_results:
                content_hash = hash(str(doc.get("content", ""))[:100])
                if content_hash in seen:
                    continue
                seen.add(content_hash)
                unique_results.append(doc)
                if len(unique_results) >= 3:
                    break
            return unique_results
        except (ValueError, RuntimeError):
            return []

    @classmethod
    def _build_prompt(
        cls,
        request: AdviceRequest,
        weather_summary: str,
        weather_risk: str,
        growth_summary: str,
        reference_docs: List[Dict],
    ) -> str:
        references = "\n".join(
            f"- {doc.get('source', doc.get('title', f'文档{i + 1}'))}: {str(doc.get('content', ''))[:500]}"
            for i, doc in enumerate(reference_docs[:3])
        ) or "- 无可用知识库证据，必须提示农技人员复核"
        return f"""你是一位农业技术专家。请只依据给定上下文生成可执行的 Markdown 防治建议。

作物：{request.crop.name}；品种：{request.crop.variety or '未登记'}
地块：{request.field.name}；农场：{request.field.farm_name}
病害：{request.disease_name}；置信度：{request.confidence:.2%}
生育期：{growth_summary}
未来天气：{weather_summary}
天气风险：{weather_risk}
知识证据：
{references}

输出必须包含病害分析、天气与生育期风险、农业防治、用药建议、处置时机、复核事项和参考来源。"""

    @classmethod
    def _call_llm(cls, prompt: str, fallback: str) -> str:
        if not settings.LLM_API_KEY or not settings.LLM_API_BASE:
            return fallback
        headers = {"Content-Type": "application/json", "Authorization": f"Bearer {settings.LLM_API_KEY}"}
        payload = {
            "model": settings.LLM_MODEL_NAME,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.3,
            "max_tokens": 2000,
        }
        try:
            response = requests.post(
                f"{settings.LLM_API_BASE}/chat/completions",
                headers=headers,
                json=payload,
                timeout=60,
            )
            response.raise_for_status()
            return response.json()["choices"][0]["message"]["content"].strip()
        except (requests.RequestException, KeyError, TypeError, json.JSONDecodeError):
            return fallback

    @classmethod
    def _generate_fallback_response(
        cls,
        request: AdviceRequest,
        weather_summary: str,
        weather_risk: str,
        growth_summary: str,
        reference_docs: List[Dict],
    ) -> str:
        if reference_docs:
            reference_section = "\n\n".join(
                f"- 来源：{doc.get('source', doc.get('title', f'文档{i + 1}'))}\n  {str(doc.get('content', '')).strip()[:500]}"
                for i, doc in enumerate(reference_docs[:3])
            )
        else:
            reference_section = "暂无可用知识库引用，请由农技人员复核。"

        return f"""## 1. 病害与地块信息
- 作物：{request.crop.name}（品种：{request.crop.variety or '未登记'}）
- 地块：{request.field.name} / {request.field.farm_name}
- 识别结果：{request.disease_name}，置信度 {request.confidence:.1%}

## 2. 生育期分析
{growth_summary}。当前处置必须避免影响该阶段正常生长。

## 3. 天气风险
- 未来天气：{weather_summary}
- 风险判断：{weather_risk}

## 4. 综合防治建议
- 先隔离并标记疑似病株，清理严重病叶，减少传播源。
- 改善田间通风与排水，根据实际病斑范围分区处置。
- 选择登记用于“{request.crop.name}—{request.disease_name}”的药剂，严格按标签剂量和安全间隔期使用。
- 有降雨或大风时暂停喷药；处置后 2—3 天复查病斑扩展情况并记录效果。

## 5. 农技员复核事项
核对病害类别、作物生育期、天气窗口和药剂登记范围；知识证据不足时不得仅依据自动建议施药。

---

### 参考来源
{reference_section}"""