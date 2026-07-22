import json
import requests
from typing import Optional
from .rag_service import RAGService
from ..core.config import settings


class AgentService:
    @classmethod
    def generate_advice(
        cls,
        disease_name: str,
        crop_info: str,
        weather_info: str
    ) -> str:
        reference_docs = cls._retrieve_reference(disease_name)

        prompt = cls._build_prompt(
            disease_name=disease_name,
            crop_info=crop_info,
            weather_info=weather_info,
            reference_docs=reference_docs
        )

        response = cls._call_llm(prompt)

        return response

    @classmethod
    def _retrieve_reference(cls, disease_name: str) -> list:
        try:
            query = f"{disease_name} 防治方法"
            results = RAGService.retrieve(query, top_k=3)
            return results
        except ValueError:
            return []

    @classmethod
    def _build_prompt(
        cls,
        disease_name: str,
        crop_info: str,
        weather_info: str,
        reference_docs: list
    ) -> str:
        reference_text = ""
        if reference_docs:
            reference_text = "\n\n参考资料：\n"
            for i, doc in enumerate(reference_docs):
                reference_text += f"{i+1}. 来源: {doc.get('source', '')}\n"
                reference_text += f"   内容: {doc.get('content', '')}\n"
                reference_text += f"   相似度: {doc.get('score', 0):.4f}\n\n"

        prompt = f"""你是一位资深的农业技术专家，精通各种农作物病害的诊断与防治。

请根据以下信息，为用户提供专业、详细的综合防治建议：

【作物信息】
{crop_info}

【诊断结果】
病害名称：{disease_name}

【天气情况】
{weather_info}

{reference_text}

请按照以下结构输出防治建议：
1. 病害分析：简要说明该病害的特点和对当前作物的影响
2. 农业防治：从栽培管理角度给出预防和控制措施
3. 物理防治：适合当前天气条件的物理防治方法
4. 化学防治：推荐合适的农药及使用方法（如需要）
5. 注意事项：针对当前情况的特别提醒

注意：
- 建议要结合当前天气情况给出
- 如果有参考资料，请优先参考资料内容
- 输出语言要通俗易懂，便于农户理解和操作
- 避免使用过于专业的术语，或在使用时给出解释
"""
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
            "max_tokens": 1500
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
        return """由于当前环境未配置大语言模型API，无法生成完整的智能防治建议。

建议您：
1. 在 .env 文件中配置 LLM_API_KEY、LLM_API_BASE 和 LLM_MODEL_NAME
2. 确保网络可以访问大语言模型服务
3. 重新调用此功能

您可以参考以下通用建议框架：
- 及时清除病株和病叶，减少病菌传播源
- 加强田间通风透光，降低湿度
- 根据病害类型选择合适的农药进行防治
- 严格按照农药使用说明操作，注意安全间隔期"""