import xml.etree.ElementTree as ET
import requests
from flask import current_app
from app.utils.logger import logger
from app.utils.rate_limiter import rate_limiter


class PubMedClient:
    def __init__(self, app=None):
        self.app = app
        if app is not None:
            self.init_app(app)

    def init_app(self, app):
        self.app = app

    def _get_proxies(self):
        if self.app is None:
            self.app = current_app._get_current_object()
        proxy = self.app.config.get('HTTP_PROXY', '')
        if proxy:
            return {'http': proxy, 'https': proxy}
        return None

    def search(self, keywords, page=1, page_size=9):
        if self.app is None:
            self.app = current_app._get_current_object()

        base_url = self.app.config['PUBMED_BASE_URL']
        api_key = self.app.config.get('PUBMED_API_KEY', '')
        timeout = 15
        proxies = self._get_proxies()

        query = ' AND '.join(keywords)
        retstart = (page - 1) * page_size

        params = {
            'db': 'pubmed',
            'term': query,
            'retmode': 'json',
            'retstart': retstart,
            'retmax': page_size,
            'sort': 'relevance',
        }
        if api_key:
            params['api_key'] = api_key

        rate_limiter.acquire()
        logger.info(f"PubMed ESearch: query={query}, page={page}, page_size={page_size}")

        try:
            response = requests.get(
                f"{base_url}/esearch.fcgi",
                params=params,
                timeout=timeout,
                proxies=proxies,
            )
            response.raise_for_status()
            data = response.json()

            result = data.get('esearchresult', {})
            total_count = int(result.get('count', 0))
            id_list = result.get('idlist', [])

            logger.info(f"PubMed ESearch result: total={total_count}, ids={len(id_list)}")
            return {
                'total_count': total_count,
                'pmids': id_list,
            }

        except requests.exceptions.Timeout:
            logger.error("PubMed ESearch timeout")
            raise Exception("PubMed检索超时，请稍后重试")
        except requests.exceptions.ConnectionError:
            logger.error("PubMed ESearch connection error")
            raise Exception("PubMed服务连接失败，请稍后重试")
        except Exception as e:
            logger.error(f"PubMed ESearch error: {str(e)}")
            raise Exception("PubMed检索服务不可用，请稍后重试")

    def fetch_details(self, pmids):
        if self.app is None:
            self.app = current_app._get_current_object()

        if not pmids:
            return []

        base_url = self.app.config['PUBMED_BASE_URL']
        api_key = self.app.config.get('PUBMED_API_KEY', '')
        timeout = 15
        proxies = self._get_proxies()

        params = {
            'db': 'pubmed',
            'id': ','.join(pmids),
            'retmode': 'xml',
        }
        if api_key:
            params['api_key'] = api_key

        rate_limiter.acquire()
        logger.info(f"PubMed EFetch: pmids={len(pmids)}")

        try:
            response = requests.get(
                f"{base_url}/efetch.fcgi",
                params=params,
                timeout=timeout,
                proxies=proxies,
            )
            response.raise_for_status()
            return self._parse_xml(response.text)

        except requests.exceptions.Timeout:
            logger.error("PubMed EFetch timeout")
            raise Exception("PubMed文献详情获取超时")
        except requests.exceptions.ConnectionError:
            logger.error("PubMed EFetch connection error")
            raise Exception("PubMed服务连接失败")
        except Exception as e:
            logger.error(f"PubMed EFetch error: {str(e)}")
            raise Exception("PubMed文献详情获取失败")

    def _parse_xml(self, xml_text):
        articles = []
        try:
            root = ET.fromstring(xml_text)
            for article_elem in root.findall('.//PubmedArticle'):
                article = self._parse_article(article_elem)
                if article:
                    articles.append(article)
        except ET.ParseError as e:
            logger.error(f"PubMed XML parse error: {str(e)}")
        return articles

    def _parse_article(self, article_elem):
        try:
            medline = article_elem.find('.//MedlineCitation')
            if medline is None:
                return None

            pmid_elem = medline.find('.//PMID')
            pmid = pmid_elem.text if pmid_elem is not None else ''

            article = medline.find('.//Article')
            if article is None:
                return None

            title_elem = article.find('.//ArticleTitle')
            title = ''.join(title_elem.itertext()) if title_elem is not None else ''

            authors = []
            author_list = article.find('.//AuthorList')
            if author_list is not None:
                for author_elem in author_list.findall('Author'):
                    last = author_elem.find('LastName')
                    fore = author_elem.find('ForeName')
                    if last is not None and fore is not None:
                        authors.append(f"{last.text} {fore.text}")
                    elif last is not None:
                        authors.append(last.text)

            pub_date_elem = article.find('.//PubDate')
            pub_date = ''
            if pub_date_elem is not None:
                year = pub_date_elem.find('Year')
                month = pub_date_elem.find('Month')
                if year is not None:
                    pub_date = year.text
                    if month is not None:
                        pub_date = f"{year.text} {month.text}"

            abstract_parts = []
            abstract_elem = article.find('.//Abstract')
            if abstract_elem is not None:
                for text_elem in abstract_elem.findall('.//AbstractText'):
                    text = ''.join(text_elem.itertext())
                    if text:
                        abstract_parts.append(text)
            abstract_text = ' '.join(abstract_parts)

            return {
                'pmid': pmid,
                'title_en': title,
                'authors': authors,
                'pub_date': pub_date,
                'abstract': abstract_text,
            }
        except Exception as e:
            logger.error(f"PubMed article parse error: {str(e)}")
            return None


pubmed_client = PubMedClient()
