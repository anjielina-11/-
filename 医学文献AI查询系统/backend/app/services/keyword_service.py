import re
from app.clients.deepseek_client import deepseek_client
from app.utils.logger import logger


class KeywordService:
    def generate_keywords(self, description):
        prompt = f"""请根据以下疾病描述，提取3个最核心的中文医学关键词。每个关键词单独一行输出，不要编号，不要其他解释。

疾病描述：{description}

请输出3个中文关键词："""

        try:
            result = deepseek_client.chat(prompt)
            keywords = self._parse_keywords(result)
            if len(keywords) > 3:
                keywords = keywords[:3]
            logger.info(f"Generated keywords: {keywords}")
            return keywords
        except Exception as e:
            logger.error(f"Generate keywords error: {str(e)}")
            raise

    def translate_keywords(self, keywords):
        keywords_str = '、'.join(keywords)
        prompt = f"""请将以下中文医学关键词翻译为对应的英文医学术语。每个翻译结果单独一行输出，与中文关键词一一对应，不要编号，不要其他解释。

中文关键词：{keywords_str}

请输出对应的英文翻译："""

        try:
            result = deepseek_client.chat(prompt)
            translated = self._parse_keywords(result)
            logger.info(f"Translated keywords: {translated}")
            return translated
        except Exception as e:
            logger.error(f"Translate keywords error: {str(e)}")
            raise

    def _parse_keywords(self, text):
        lines = text.strip().split('\n')
        keywords = []
        for line in lines:
            cleaned = re.sub(r'^[\d\.\-\*\)\（\(]+\s*', '', line.strip())
            cleaned = cleaned.strip()
            if cleaned:
                keywords.append(cleaned)
        return keywords


keyword_service = KeywordService()
