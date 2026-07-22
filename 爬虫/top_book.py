# ============================================================
# 模块导入
# ============================================================
import requests
import random
import time
import csv
import re
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from lxml import etree

# ============================================================
# 全局配置
# ============================================================
VERBOSE = True   # 打印请求/响应头（仅第一页）

UA_LIST = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64; rv:115.0) Gecko/20100101 Firefox/115.0"
]

session = requests.Session()
retry_config = Retry(total=3, backoff_factor=1.2, status_forcelist=[403, 429, 500, 502, 503, 504])
adapter = HTTPAdapter(max_retries=retry_config)
session.mount("https://", adapter)
session.mount("http://", adapter)


# ============================================================
# 1. 请求发送函数（含请求/响应头展示）
# ============================================================
def fetch_page(start, url, retries=3):
    """
    发送 GET 请求获取豆瓣图书 Top250 页面
    参数：start（偏移量），url（图书地址），retries（重试次数）
    返回：HTML 字符串或空字符串
    """
    headers = {
        "User-Agent": random.choice(UA_LIST),
        "Referer": url,
        "Accept-Language": "zh-CN,zh;q=0.9",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    }
    params = {"start": start}

    for attempt in range(1, retries + 1):
        try:
            resp = session.get(url, headers=headers, params=params, timeout=10)
            resp.encoding = "utf-8"

            # 第一页打印结构化头信息
            if start == 0 and VERBOSE:
                print("\n" + "=" * 60)
                print("【结构化爬虫数据分析】请求与响应头")
                print("=" * 60)
                print("\n--- 请求头 (Request Headers) ---")
                for key, value in resp.request.headers.items():
                    print(f"  {key}: {value}")
                print("\n--- 响应头 (Response Headers) (关键字段) ---")
                for key, value in resp.headers.items():
                    if key.lower() in ['content-type', 'server', 'date', 'set-cookie', 'cache-control']:
                        print(f"  {key}: {value}")
                print("=" * 60 + "\n")

            # 图书页面容器是 <tr class="item">
            if 'class="item"' in resp.text or 'tr class="item"' in resp.text:
                time.sleep(random.uniform(2.0, 3.5))
                return resp.text
            else:
                print(f"第{attempt}次尝试：页面可能被拦截，前200字符：{resp.text[:200]}")
                time.sleep(5)
        except Exception as e:
            print(f"第{attempt}次请求异常：{e}")
            time.sleep(3)
    return ""


# ============================================================
# 2. 解析函数（XPath 提取图书信息）
# ============================================================
def parse_books_with_xpath(html):
    """
    从图书 Top250 的 HTML 中提取每本书的信息
    返回列表，每项为一行记录
    """
    tree = etree.HTML(html)
    # 图书条目是表格行 <tr class="item">
    items = tree.xpath("//tr[@class='item']")
    res = []

    for item in items:
        try:
            # ---- 排名（可能包含空格，拼接所有文本） ----
            rank_parts = item.xpath(".//td[@class='td_number']//text()")
            rank = "".join(rank_parts).strip() if rank_parts else ""

            # ---- 书名（取 <a> 的文本） ----
            title_a = item.xpath(".//div[@class='pl2']/a")
            if title_a:
                title = title_a[0].xpath("string()").strip()
                if not title:
                    title = title_a[0].text_content().strip()
            else:
                title = ""

            # ---- 作者、出版社、出版时间（格式：作者 / 出版社 / 出版时间） ----
            pub_info = item.xpath(".//p[@class='pl']/text()")
            pub_text = pub_info[0].strip() if pub_info else ""
            author = publisher = pub_date = ""

            if pub_text:
                parts = [p.strip() for p in pub_text.split('/') if p.strip()]
                # 根据部分数量智能解析
                if len(parts) >= 3:
                    author = parts[0]
                    publisher = parts[-2] if len(parts) >= 2 else ""
                    pub_date = parts[-1] if len(parts) >= 3 else ""
                    # 如果最后是价格（如“59.70元”），调整顺序
                    if re.search(r'\d+\.\d+元', pub_date):
                        if len(parts) >= 4:
                            pub_date = parts[-2]
                            publisher = parts[-3]
                        elif len(parts) == 3:
                            pub_date = parts[1]
                            publisher = parts[0]
                    # 若出版社含有“-”而出版时间没有，则交换（兼容不同格式）
                    if '-' in publisher and '/' not in pub_date:
                        publisher, pub_date = pub_date, publisher
                elif len(parts) == 2:
                    author = parts[0]
                    publisher = parts[1]
                elif len(parts) == 1:
                    author = parts[0]

                author = author.strip()
                publisher = publisher.strip()
                pub_date = pub_date.strip()

            # ---- 评分 ----
            rating = item.xpath(".//span[@class='rating_nums']/text()")
            rating = rating[0].strip() if rating else ""

            # ---- 评价人数（提取数字） ----
            people_span = item.xpath(".//span[@class='pl']/text()")
            people = "0"
            if people_span:
                text = people_span[0].strip()
                match = re.search(r'(\d+)', text)
                if match:
                    people = match.group(1)

            # ---- 简介（一句话） ----
            quote = item.xpath(".//span[@class='inq']/text()")
            quote = quote[0].strip() if quote else ""

            # ---- 封面链接 ----
            cover = item.xpath(".//td[@class='td_thumb']/a/img/@src")
            cover = cover[0] if cover else ""

            row = [rank, title, author, publisher, pub_date, rating, people, quote, cover]
            res.append(row)

        except Exception as e:
            print(f"解析单条图书异常：{e}")
            continue
    return res


# ============================================================
# 3. 数据存储（CSV）
# ============================================================
def save_csv(data, file="douban_book_top100.csv"):
    headers = ["排名", "书名", "作者", "出版社", "出版时间", "评分", "评价人数", "简介", "封面链接"]
    with open(file, "w", newline="", encoding="utf-8-sig") as f:
        wr = csv.writer(f)
        wr.writerow(headers)
        wr.writerows(data)


# ============================================================
# 4. 主程序入口
# ============================================================
if __name__ == "__main__":
    url = "https://book.douban.com/top250"
    total_books = 100
    per_page = 25
    pages = total_books // per_page   # 4页

    all_data = []

    for page in range(pages):
        start = page * per_page
        print(f"正在采集第{page+1}页（排名{start+1}-{start+per_page}）...")
        html = fetch_page(start, url, retries=3)

        if not html:
            print(f"⚠️ 第{page+1}页获取失败，跳过")
            continue

        page_data = parse_books_with_xpath(html)
        if page_data:
            all_data.extend(page_data)
            print(f"✅ 第{page+1}页成功，获取{len(page_data)}条")
            if page_data:
                print(f"  示例：{page_data[0][1]} 评分={page_data[0][5]} 评价人数={page_data[0][6]}")
        else:
            print(f"⚠️ 第{page+1}页解析结果为空")

        time.sleep(2)

    if all_data:
        save_csv(all_data)
        print(f"✅ 总共采集 {len(all_data)} 条，已保存至 douban_book_top100.csv")
    else:
        print("❌ 未采集到任何数据")