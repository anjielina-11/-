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
VERBOSE = True  # 是否在终端打印请求与响应头（仅第一页）

# UA池：随机切换 User-Agent，模仿不同浏览器，降低被封锁风险
UA_LIST = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64; rv:115.0) Gecko/20100101 Firefox/115.0"
]

# 创建全局 Session，自动管理 Cookie，提高效率
session = requests.Session()
# 配置重试策略：总重试3次，每次等待时间递增，遇到特定状态码自动重试
retry_config = Retry(
    total=3,
    backoff_factor=1.2,
    status_forcelist=[403, 429, 500, 502, 503, 504]
)
adapter = HTTPAdapter(max_retries=retry_config)
session.mount("https://", adapter)
session.mount("http://", adapter)


# ============================================================
# 1. 请求发送函数（含请求/响应头展示）
# ============================================================
def fetch_page(start, url, retries=3):
    """
    发送 GET 请求获取豆瓣 Top250 页面（电影或图书）
    参数：
        start : 起始偏移量（0, 25, 50 ...）
        url   : 目标 URL（电影或图书）
        retries : 失败重试次数
    返回：
        成功返回页面 HTML 字符串，失败返回空字符串
    """
    # 构造请求头，模拟真实浏览器访问
    headers = {
        "User-Agent": random.choice(UA_LIST),         # 随机 UA
        "Referer": url,                               # 来源页（防盗链）
        "Accept-Language": "zh-CN,zh;q=0.9",          # 优先中文
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    }
    params = {"start": start}  # 分页参数

    for attempt in range(1, retries + 1):
        try:
            # 发送 GET 请求
            resp = session.get(url, headers=headers, params=params, timeout=10)
            resp.encoding = "utf-8"  # 强制使用 UTF-8 解码，避免乱码

            # ----- 如果是第一页，打印请求和响应头（结构化展示） -----
            if start == 0 and VERBOSE:
                print("\n" + "=" * 60)
                print("【结构化爬虫数据分析】请求与响应头")
                print("=" * 60)
                print("\n--- 请求头 (Request Headers) ---")
                for key, value in resp.request.headers.items():
                    print(f"  {key}: {value}")
                print("\n--- 响应头 (Response Headers) (关键字段) ---")
                for key, value in resp.headers.items():
                    # 只显示重要字段，避免刷屏
                    if key.lower() in ['content-type', 'server', 'date', 'set-cookie', 'cache-control']:
                        print(f"  {key}: {value}")
                print("=" * 60 + "\n")

            # 判断是否正常返回（包含电影条目）
            if 'class="item"' in resp.text:
                # 随机延时，模仿人类浏览行为，避免触发反爬
                time.sleep(random.uniform(2.0, 3.5))
                return resp.text
            else:
                print(f"第{attempt}次尝试：页面可能被拦截，前200字符：{resp.text[:200]}")
                time.sleep(5)  # 等待更长时间再重试

        except Exception as e:
            print(f"第{attempt}次请求异常：{e}")
            time.sleep(3)
    return ""  # 所有重试失败


# ============================================================
# 2. 解析函数（使用 XPath 提取电影信息）
# ============================================================
def parse_movie_with_xpath(html):
    """
    从电影 Top250 的 HTML 中提取每部电影的信息
    返回列表，每个元素为一条记录（列表形式）
    """
    tree = etree.HTML(html)
    # 定位所有电影条目容器
    items = tree.xpath("//div[@class='item']")
    res = []

    for item in items:
        try:
            # ---- 排名 ----
            rank = item.xpath(".//div[@class='pic']/em/text()")
            rank = rank[0].strip() if rank else ""

            # ---- 电影名（中文名） ----
            title = item.xpath(".//div[@class='hd']/a/span[@class='title'][1]/text()")
            title = title[0].strip() if title else ""

            # ---- 导演、主演、年份、国家（从 <p> 文本中解析） ----
            info_parts = item.xpath(".//div[@class='bd']/p[1]/text()")
            full_info = "".join([t.strip() for t in info_parts if t.strip()])
            full_info = full_info.replace("\n", "").replace("\xa0", " ")

            director = actors = year = country = ""
            if "导演:" in full_info:
                if "主演:" in full_info:
                    # 分割导演和主演部分
                    dir_part = full_info.split("主演:")[0].replace("导演:", "").strip()
                    act_part = full_info.split("主演:")[1].strip()
                    # 从主演部分提取年份（4位数字）和国家
                    year_match = re.search(r"(\d{4})", act_part)
                    if year_match:
                        year = year_match.group(1)
                        rest = act_part.replace(year, "").strip()
                        if "/" in rest:
                            parts = rest.split("/")
                            country = parts[0].strip()
                        else:
                            country = rest
                        actors = act_part.replace(year, "").replace(country, "").strip("/").strip()
                    else:
                        actors = act_part
                    director = dir_part
                else:
                    director = full_info.replace("导演:", "").strip()

            # ---- 评分 ----
            rating = item.xpath(".//span[@class='rating_num']/text()")
            rating = rating[0].strip() if rating else ""

            # ---- 评价人数（定位包含“人评价”的 <span>） ----
            people_span = item.xpath(".//div[@class='bd']//span[contains(text(),'人评价')]")
            if people_span:
                people = people_span[0].text.strip().replace("人评价", "").strip()
            else:
                people = "0"

            # ---- 简介（一句话） ----
            quote = item.xpath(".//span[@class='inq']/text()")
            quote = quote[0].strip() if quote else ""

            # ---- 封面图片链接 ----
            cover = item.xpath(".//div[@class='pic']/a/img/@src")
            cover = cover[0] if cover else ""

            # 组装成一行
            row = [rank, title, director, actors, year, country, rating, people, quote, cover]
            res.append(row)

        except Exception as e:
            # 单条解析失败不影响其他条目
            print(f"解析单条电影异常：{e}")
            continue
    return res


# ============================================================
# 3. 数据存储（CSV）
# ============================================================
def save_csv(data, file="douban_movie_top100.csv"):
    """
    将数据写入 CSV 文件（覆盖写入）
    使用 utf-8-sig 编码以兼容 Excel 打开
    """
    headers = ["排名", "电影名", "导演", "主演", "年份", "国家/地区", "评分", "评价人数", "简介", "封面链接"]
    with open(file, "w", newline="", encoding="utf-8-sig") as f:
        wr = csv.writer(f)
        wr.writerow(headers)   # 写入表头
        wr.writerows(data)     # 写入所有行


# ============================================================
# 4. 主程序入口
# ============================================================
if __name__ == "__main__":
    url = "https://movie.douban.com/top250"
    total_movies = 100
    per_page = 25
    pages = total_movies // per_page   # 4页

    all_data = []

    for page in range(pages):
        start = page * per_page
        print(f"正在采集第{page+1}页（排名{start+1}-{start+per_page}）...")
        html = fetch_page(start, url, retries=3)

        if not html:
            print(f"⚠️ 第{page+1}页获取失败，跳过")
            continue

        page_data = parse_movie_with_xpath(html)
        if page_data:
            all_data.extend(page_data)
            print(f"✅ 第{page+1}页成功，获取{len(page_data)}条")
            if page_data:
                print(f"  示例：{page_data[0][1]} 评价人数={page_data[0][7]}")
        else:
            print(f"⚠️ 第{page+1}页解析结果为空")

        time.sleep(2)  # 页间间隔

    # 保存全部数据
    if all_data:
        save_csv(all_data)
        print(f"✅ 总共采集 {len(all_data)} 条，已保存至 douban_movie_top100.csv")
    else:
        print("❌ 未采集到任何数据")