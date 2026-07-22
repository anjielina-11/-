from app.clients.deepseek_client import deepseek_client
from app.clients.pubmed_client import pubmed_client
from app.utils.logger import logger


MOCK_LITERATURES = [
    {"pmid": "38892105", "title_en": "Targeted Therapies for EGFR Exon 20 Insertion Mutation in Non-Small-Cell Lung Cancer.", "title_cn": "针对非小细胞肺癌中EGFR外显子20插入突变的靶向治疗", "authors": ["Seo Donghyun", "Lim Jun Hyeok"], "pub_date": "2024 May", "abstract": "Non-small cell lung cancer with EGFR exon 20 insertion mutations is resistant to conventional TKIs. New agents such as amivantamab, mobocertinib, poziotinib, and zipalertinib show promising efficacy.", "abstract_summary": "EGFR外显子20插入突变NSCLC对传统TKI不敏感，新药如Amivantamab、Mobocertinib等显示疗效。"},
    {"pmid": "31564718", "title_en": "Resistance mechanisms to osimertinib in EGFR-mutated non-small cell lung cancer.", "title_cn": "EGFR突变非小细胞肺癌对奥希替尼的耐药机制", "authors": ["Leonetti Alessandro", "Sharma Sugandhi", "Minari Roberta", "Perego Paola", "Giovannetti Elisa", "Tiseo Marcello"], "pub_date": "2019 Oct", "abstract": "Osimertinib is effective in EGFR-mutated NSCLC but resistance inevitably develops through heterogeneous mechanisms including MET/HER2 amplification and RAS-MAPK/PI3K pathway activation.", "abstract_summary": "奥希替尼治疗EGFR突变NSCLC虽有效，但患者不可避免产生耐药，机制包括MET/HER2扩增、RAS-MAPK/PI3K通路激活等。"},
    {"pmid": "31562956", "title_en": "Rare epidermal growth factor receptor (EGFR) mutations in non-small cell lung cancer.", "title_cn": "非小细胞肺癌中罕见的表皮生长因子受体（EGFR）突变", "authors": ["Harrison Peter T", "Vyse Simon", "Huang Paul H"], "pub_date": "2020 Apr", "abstract": "EGFR mutations are the second most common oncogenic driver in NSCLC. Classical mutations respond well to EGFR inhibitors, while uncommon mutations show variable responses.", "abstract_summary": "EGFR突变是NSCLC第二大驱动事件，经典突变对EGFR抑制剂反应良好，而低频突变反应各异。"},
    {"pmid": "37937797", "title_en": "Combination Therapy for EGFR-Mutated Lung Cancer.", "title_cn": "EGFR突变肺癌的联合治疗", "authors": ["Wu Yi-Long", "Zhou Qing"], "pub_date": "2023 Nov", "abstract": "Combination strategies are being explored to overcome resistance and improve outcomes in EGFR-mutated lung cancer.", "abstract_summary": "EGFR突变肺癌的联合治疗策略正在探索中，以克服耐药并改善预后。"},
    {"pmid": "28017789", "title_en": "Driven by Mutations: The Predictive Value of Mutation Subtype in EGFR-Mutated Non-Small Cell Lung Cancer.", "title_cn": "由突变驱动：EGFR突变非小细胞肺癌中突变亚型的预测价值", "authors": ["Castellanos Emily", "Feld Emily", "Horn Leora"], "pub_date": "2017 Apr", "abstract": "EGFR-mutated NSCLC harbors genetic heterogeneity. Common mutations respond to TKIs while 10% are rare mutations with variable TKI responses.", "abstract_summary": "EGFR突变NSCLC具有高度遗传异质性，常见突变对TKI敏感，10%为罕见突变，对TKI反应各异。"},
    {"pmid": "40243603", "title_en": "Strategies to Overcome Resistance to Osimertinib in EGFR-Mutated Lung Cancer.", "title_cn": "克服EGFR突变肺癌中对奥希替尼耐药性的策略", "authors": ["Romaniello Donatella", "Morselli Alessandra", "Marrocco Ilaria"], "pub_date": "2025 Mar", "abstract": "Resistance to osimertinib involves C797S mutations, MET amplification and other mechanisms. Fourth-generation TKIs, PROTACs, bispecific antibodies and ADCs are being developed.", "abstract_summary": "奥希替尼耐药机制包括C797S突变、MET扩增等，第四代TKI、PROTACs、双特异性抗体等新药在研发中。"},
    {"pmid": "37196632", "title_en": "Air pollution, EGFR mutation, and cancer initiation.", "title_cn": "空气污染、EGFR突变与癌症发生", "authors": ["Han Si-Chong", "Wang Gui-Zhen", "Zhou Guang-Biao"], "pub_date": "2023 May", "abstract": "PM2.5 exposure is associated with EGFR/KRAS-driven lung cancer through interleukin-1 beta release from mesenchymal macrophages.", "abstract_summary": "PM2.5暴露与EGFR/KRAS驱动的肺癌相关，通过间质巨噬细胞分泌IL-1β促进肿瘤发生。"},
    {"pmid": "38696655", "title_en": "Brain Metastasis from EGFR-Mutated Non-Small Cell Lung Cancer: Secretion of IL11 from Astrocytes Up-Regulates PDL1 and Promotes Immune Escape.", "title_cn": "EGFR突变非小细胞肺癌脑转移：星形胶质细胞分泌IL11上调PDL1并促进免疫逃逸", "authors": ["Tang Mengyi", "Xu Mingxin", "Wang Jian", "Liu Ye", "Liang Kun", "Jin Yinuo", "Duan Wenzhe"], "pub_date": "2024 Jul", "abstract": "Reactive astrocytes secrete IL11 in EGFR-mutated NSCLC brain metastasis, upregulating PDL1 and promoting immune escape.", "abstract_summary": "EGFR突变NSCLC脑转移中，反应性星形胶质细胞分泌IL11上调PDL1，促进免疫逃逸。"},
    {"pmid": "41516316", "title_en": "Lineage Plasticity and Histologic Transformation in EGFR-TKI Resistant Lung Cancer.", "title_cn": "EGFR-TKI耐药肺癌中的谱系可塑性与组织学转化", "authors": ["Lau Li Yieng Eunice", "Skanderup Anders Jacobsen", "Tan Aaron C"], "pub_date": "2025 Dec", "abstract": "Lineage plasticity is a key mechanism of cancer therapy resistance, particularly in EGFR-mutated lung adenocarcinoma, leading to histologic transformation.", "abstract_summary": "谱系可塑性是癌症治疗耐药的关键机制，在EGFR突变肺腺癌中可导致小细胞癌等组织学转化。"},
]


class LiteratureService:
    def search_literatures(self, keywords, page=1, page_size=9):
        from app.services.keyword_service import keyword_service

        translated = keyword_service.translate_keywords(keywords)

        try:
            return self._search_pubmed(translated, page, page_size)
        except Exception as e:
            logger.warning(f"PubMed search failed, using mock data: {str(e)}")
            return self._search_mock(keywords, page, page_size)

    def _search_pubmed(self, translated, page, page_size):
        search_result = pubmed_client.search(translated, page, page_size)
        total_count = search_result['total_count']
        pmids = search_result['pmids']

        if not pmids:
            return {
                'total_count': 0,
                'filtered_count': 0,
                'page': page,
                'page_size': page_size,
                'total_pages': 0,
                'literatures': [],
            }

        articles = pubmed_client.fetch_details(pmids)

        literatures = []
        for article in articles:
            title_cn = self._translate_title(article['title_en'])
            abstract_summary = self._summarize_abstract(article['abstract'])

            literatures.append({
                'pmid': article['pmid'],
                'title_cn': title_cn,
                'title_en': article['title_en'],
                'authors': article['authors'],
                'pub_date': article['pub_date'],
                'abstract': article['abstract'],
                'abstract_summary': abstract_summary,
            })

        total_pages = (total_count + page_size - 1) // page_size
        filtered_count = len(literatures)

        return {
            'total_count': total_count,
            'filtered_count': filtered_count,
            'page': page,
            'page_size': page_size,
            'total_pages': total_pages,
            'literatures': literatures,
        }

    def _search_mock(self, keywords, page, page_size):
        logger.info(f"Using mock literature data for keywords: {keywords}")
        start = (page - 1) * page_size
        end = start + page_size
        items = MOCK_LITERATURES[start:end]
        total_count = len(MOCK_LITERATURES)
        total_pages = (total_count + page_size - 1) // page_size

        return {
            'total_count': total_count,
            'filtered_count': len(items),
            'page': page,
            'page_size': page_size,
            'total_pages': total_pages,
            'literatures': items,
        }

    def _translate_title(self, title_en):
        if not title_en:
            return ''

        prompt = f"""请将以下英文医学文献标题翻译为中文，仅输出翻译结果，不要其他解释：

{title_en}"""

        try:
            result = deepseek_client.chat(prompt)
            translated = result.strip()
            if translated:
                return translated
            return title_en
        except Exception as e:
            logger.warning(f"Translate title failed: {str(e)}")
            return title_en

    def _summarize_abstract(self, abstract):
        if not abstract:
            return '暂无摘要'

        prompt = f"""请用不超过100字的中文总结以下医学文献摘要的核心内容：

{abstract}"""

        try:
            result = deepseek_client.chat(prompt)
            summary = result.strip()
            if len(summary) > 100:
                summary = summary[:100]
            return summary
        except Exception as e:
            logger.warning(f"Summarize abstract failed: {str(e)}")
            return '摘要总结生成失败'


literature_service = LiteratureService()
