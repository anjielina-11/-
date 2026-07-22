from app.clients.deepseek_client import deepseek_client
from app.utils.logger import logger


class SummaryService:
    def generate_summary(self, pmid, title_en, abstract):
        prompt = f"""请根据以下医学文献信息，分别从三个维度进行专业总结。请严格按照以下格式输出，每个维度标题独占一行，内容紧跟其后：

关键发现：
[请总结该文献的核心研究结论与数据发现]

研究趋势：
[请描述该文献反映的学术研究方向与发展趋势]

临床意义：
[请描述该文献对临床实践的应用价值与指导意义]

文献标题：{title_en}
文献摘要：{abstract if abstract else '暂无摘要'}"""

        try:
            result = deepseek_client.chat(prompt)
            key_findings, research_trends, clinical_significance = self._parse_summary(result)

            return {
                'pmid': pmid,
                'key_findings': key_findings,
                'research_trends': research_trends,
                'clinical_significance': clinical_significance,
            }
        except Exception as e:
            logger.error(f"Generate summary error for pmid={pmid}: {str(e)}")
            raise

    def _parse_summary(self, text):
        key_findings = '暂无数据'
        research_trends = '暂无数据'
        clinical_significance = '暂无数据'

        sections = text.split('关键发现')
        if len(sections) > 1:
            rest = '关键发现' + sections[1]

            parts = rest.split('研究趋势')
            if len(parts) > 1:
                kf_text = parts[0].replace('关键发现', '').strip().lstrip('：:').strip()
                if kf_text:
                    key_findings = kf_text

                rest2 = '研究趋势' + parts[1]
                parts2 = rest2.split('临床意义')
                if len(parts2) > 1:
                    rt_text = parts2[0].replace('研究趋势', '').strip().lstrip('：:').strip()
                    if rt_text:
                        research_trends = rt_text

                    cs_text = parts2[1].replace('临床意义', '').strip().lstrip('：:').strip()
                    if cs_text:
                        clinical_significance = cs_text
                else:
                    rt_text = rest2.replace('研究趋势', '').strip().lstrip('：:').strip()
                    if rt_text:
                        research_trends = rt_text
            else:
                kf_text = rest.replace('关键发现', '').strip().lstrip('：:').strip()
                if kf_text:
                    key_findings = kf_text

        return key_findings, research_trends, clinical_significance


summary_service = SummaryService()
