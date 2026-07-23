import json
import requests
from typing import Optional, Dict, List
from .rag_service import RAGService
from .weather_service import WeatherService
from ..core.config import settings


class AgentService:
    @classmethod
    def generate_advice(
        cls,
        disease_name: str,
        crop_info: str = "未知作物",
        weather_info: str = "未知天气",
        weather_data: Optional[Dict] = None
    ) -> Dict:
        reference_docs = cls._retrieve_reference(disease_name)

        if weather_data:
            weather_info = cls._format_weather_info(weather_data)

        prompt = cls._build_prompt(
            disease_name=disease_name,
            crop_info=crop_info,
            weather_info=weather_info,
            reference_docs=reference_docs
        )

        llm_response = cls._call_llm(prompt)

        return {
            "advice": llm_response,
            "references": reference_docs,
            "weather_info": weather_info
        }

    @classmethod
    def _format_weather_info(cls, weather_data: Dict) -> str:
        return (
            f"城市: {weather_data.get('city', '未知')}\n"
            f"天气: {weather_data.get('weather', '未知')}\n"
            f"温度: {weather_data.get('temperature', 0)}°C\n"
            f"湿度: {weather_data.get('humidity', 0)}%\n"
            f"风速: {weather_data.get('wind_speed', 0)}m/s\n"
            f"降雨量: {weather_data.get('rain', 0)}mm"
        )

    @classmethod
    def _retrieve_reference(cls, disease_name: str) -> List[Dict]:
        try:
            queries = [
                f"{disease_name} 防治方法",
                f"{disease_name} 症状识别",
                f"{disease_name} 农药选择"
            ]
            
            all_results = []
            for query in queries:
                results = RAGService.retrieve(query, top_k=2)
                all_results.extend(results)
            
            all_results.sort(key=lambda x: x['score'])
            
            seen = set()
            unique_results = []
            for doc in all_results:
                content_hash = hash(doc['content'][:100])
                if content_hash not in seen:
                    seen.add(content_hash)
                    unique_results.append(doc)
                    if len(unique_results) >= 3:
                        break
            
            return unique_results
        except ValueError:
            return []

    @classmethod
    def _build_prompt(
        cls,
        disease_name: str,
        crop_info: str,
        weather_info: str,
        reference_docs: List[Dict]
    ) -> str:
        reference_text = ""
        reference_sources = []
        
        if reference_docs:
            reference_text = "\n\n【参考资料】\n"
            for i, doc in enumerate(reference_docs):
                source = doc.get('source', f"文档{i+1}")
                reference_sources.append(source)
                reference_text += f"资料{i+1}（来源: {source}，相似度: {doc.get('score', 0):.4f}）:\n"
                reference_text += f"   {doc.get('content', '')[:500]}\n\n"

        prompt = f"""你是一位资深的农业技术专家，精通各种农作物病害的诊断与防治。

请根据以下信息，为用户提供专业、详细的综合防治建议：

【作物信息】
{crop_info}

【诊断结果】
病害名称：{disease_name}

【天气情况】
{weather_info}

{reference_text}

请按照以下结构输出防治建议（使用Markdown格式）：

## 1. 病害分析
简要说明该病害的特点和对当前作物的影响

## 2. 农业防治
从栽培管理角度给出预防和控制措施

## 3. 物理防治
适合当前天气条件的物理防治方法

## 4. 化学防治
推荐合适的农药及使用方法（如需要）

## 5. 注意事项
针对当前天气和作物情况的特别提醒

---

### 参考来源
{', '.join(reference_sources) if reference_sources else '暂无'}

注意：
- 建议要结合当前天气情况给出（如高湿天气注意通风降湿，雨天避免喷药等）
- 如果有参考资料，请优先参考资料内容，并在建议中体现
- 输出语言要通俗易懂，便于农户理解和操作
- 避免使用过于专业的术语，或在使用时给出解释
- 化学防治部分请注明推荐农药名称和安全间隔期"""

        return prompt

    @classmethod
    def _call_llm(cls, prompt: str) -> str:
        api_key = settings.LLM_API_KEY
        api_base = settings.LLM_API_BASE
        model_name = settings.LLM_MODEL_NAME

        if not api_key or not api_base:
            return cls._generate_fallback_response(prompt)

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}"
        }

        payload = {
            "model": model_name,
            "messages": [
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "temperature": 0.7,
            "max_tokens": 2000
        }

        try:
            response = requests.post(
                f"{api_base}/chat/completions",
                headers=headers,
                json=payload,
                timeout=60
            )
            response.raise_for_status()
            result = response.json()
            return result["choices"][0]["message"]["content"].strip()
        except Exception as e:
            return cls._generate_fallback_response(prompt)

    @classmethod
    def _generate_fallback_response(cls, prompt: str) -> str:
        return """## 1. 病害分析
该病害对作物生长有一定影响，需要及时采取防治措施。

## 2. 农业防治
- 及时清除病株和病叶，减少病菌传播源
- 加强田间通风透光，降低湿度
- 合理施肥，增强植株抗病能力
- 实行轮作制度，避免连作障碍

## 3. 物理防治
- 人工摘除病叶、病果，集中深埋或烧毁
- 利用防虫网阻止害虫传播病菌
- 采用高温闷棚等措施杀灭病菌

## 4. 化学防治
根据病害类型选择合适的农药进行防治，严格按照农药使用说明操作，注意安全间隔期。建议选择高效低毒农药。

## 5. 注意事项
- 喷药时要均匀周到，叶片正反面都要喷到
- 注意喷药时间，避免高温时段和雨天喷药
- 遵守农药安全间隔期规定
- 注意药剂交替使用，避免病菌产生抗药性

---

### 参考来源
当前环境未配置大语言模型API，以上为通用防治建议框架。如需更精准的建议，请配置 LLM_API_KEY 和 LLM_API_BASE。"""