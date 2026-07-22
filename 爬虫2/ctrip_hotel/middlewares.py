# -*- coding: utf-8 -*-
"""
Ctrip Hotel Spider - 中间件
================================
功能:
  1. CtripCookieMiddleware  - 将用户提供的Cookie注入到每个请求中
  2. CtripSeleniumMiddleware - 使用Selenium+Chrome渲染JS页面, 并注入Cookie绕过验证

Cookie策略: 所有Cookie作为会话Cookie(max-age=0)注入, 不设置过期时间,
          浏览器关闭即失效, 每次运行都使用最新提供的Cookie.
"""

import time
import logging
from scrapy import signals
from scrapy.http import HtmlResponse

logger = logging.getLogger(__name__)

# ============================================================
# 用户提供的Cookie字符串 (用于绕过携程反爬验证)
# max-age=0 含义: Cookie设置为会话级别, 浏览器关闭即失效
# ============================================================
AUTH_COOKIE_STRING = (
    "UBT_VID=1783923138329.26b82sgIuTIc; "
    "MKT_CKID=1783923138460.scnhs.gpg0; "
    "_ga=GA1.1.951555538.1783923139; "
    "GUID=09031055110347578714; "
    "_RGUID=284915d1-48c0-46a0-9153-fe19ac209835; "
    "MKT_Pagesource=PC; "
    "nfes_isSupportWebP=1; "
    "ibu_country=CN; "
    "ibulocale=zh_cn; "
    "cookiePricesDisplayed=CNY; "
    "oldCurrency=CNY; "
    "nfes_isSupportWebP=1; "
    "_abtest_userid=49056b4f-9d71-4366-9c78-0b0b84f12354; "
    "login_type=0; "
    "login_uid=B9E287B234BC7E968605A501D9F867783E229D5544A22615F980912DD30D9469; "
    "DUID=u=9600CB4E5FA7FCF7AAC0589C1C1AA657&v=0; "
    "IsNonUser=F; "
    "AHeadUserInfo=VipGrade=0&VipGradeName=%C6%D5%CD%A8%BB%E1%D4%B1&UserName=&NoReadMessageCount=0; "
    "_udl=708D70C2B179E2F91CC5ED1C2CCE362D; "
    "w_lid=016a54835fc1c516afa3; "
    "IBU_showtotalamt=2; "
    "intl_ht1=h4%3D34_132637897%2C34_134129881; "
    "ibulanguage=ZH-CN; "
    "Hm_lvt_a8d6737197d542432f4ff4abc6e06384=1783923138,1784009365; "
    "HMACCOUNT=65C317E04D6F090A; "
    "Session=smartlinkcode=U130727&smartlinklanguage=zh&SmartLinkKeyWord=&SmartLinkQuary=&SmartLinkHost=; "
    "Union=AllianceID=4902&SID=130727&OUID=&createtime=1784009366&Expires=1784614166090; "
    "manualclose=1; "
    "_ga_9BZF483VNQ=GS2.1.s1784009365$o3$g1$t1784010800$j60$l0$h0; "
    "_ga_5DVRDQD429=GS2.1.s1784009365$o3$g1$t1784010800$j60$l0$h357186698; "
    "_ga_B77BES1Z8Z=GS2.1.s1784009365$o3$g1$t1784010800$j60$l0$h0; "
    "cticket=F310E13A04FC4C6D72AEC997B1517740D0B66E8C3A4DC6A8FDECA241DB153EC5; "
    "Hm_lpvt_a8d6737197d542432f4ff4abc6e06384=1784010830; "
    "ibusite=CN; "
    "ibugroup=ctrip; "
    "Hm_lvt_4a51227696a44e11b0c61f6105dc4ee4=1783923231,1784010847; "
    "Hm_lpvt_4a51227696a44e11b0c61f6105dc4ee4=1784010847; "
    "_bfa=1.1783923138329.26b82sgIuTIc.1.1784010830266.1784010846927.3.4.10650171192; "
    "ibu_hotel_search_date=%7B%22checkIn%22%3A%222026-07-14%22%2C%22checkOut%22%3A%222026-07-15%22%2C%22isChoseFlexible%22%3Afalse%2C%22flexibleDate%22%3A%7B%22selectNight%22%3A0%7D%2C%22dayFlexibility%22%3A0%7D; "
    "ibu_hotel_search_target=%7B%22countryId%22%3A1%2C%22provinceId%22%3A25%2C%22searchWord%22%3A%22%E4%BA%91%E5%8D%97%E5%A4%A7%E5%AD%A6(%E5%91%88%E8%B4%A1%E6%A0%A1%E5%8C%BA)%22%2C%22cityId%22%3A34%2C%22searchType%22%3A%22%22%2C%22searchValue%22%3A%22%22%2C%22cityName%22%3A%22%E6%98%86%E6%98%8E%22%7D; "
    "ibu_hotel_search_crn_guest=%7B%22adult%22%3A1%2C%22children%22%3A0%2C%22ages%22%3A%22%22%2C%22crn%22%3A1%7D; "
    "_jzqco=%7C%7C%7C%7C1784010801608%7C1.623881195.1783923138463.1784010830317.1784010847543.1784010830317.1784010847543.undefined.0.0.21.21; "
    "w_tuid=qJKeAQvbHXdm1xTBeywAkIB+TCLhvQjCRv0Hpz+hlohc/ctSoaRIr9NAk/keXSeo2lUvCMd/k05hd2uuCMhO2cbsdfrJNqLHxttGPfIvGhVZdrxPI7MlsTc5GSatztFrNQGiCt0ScqalsnT5mCLda4MGnsRq+2yrrAVOKyWJ7I5n5AlqrQfLtEjzmLWv8raJtw==:1_1_1_1.fwpxoOVe/PQBiL6Jp9b8yf9KmjflSyuXuJTTSghl4Rc="
)


def parse_cookie_string(cookie_str):
    """将Cookie字符串解析为字典 {name: value}"""
    cookies = {}
    for item in cookie_str.split("; "):
        if "=" in item:
            key, value = item.split("=", 1)
            cookies[key.strip()] = value.strip()
    return cookies


class CtripCookieMiddleware:
    """
    Scrapy请求Cookie注入中间件
    ---------------------------
    优先级: 100 (在Selenium中间件之前执行)
    功能: 将用户提供的Cookie注入到每个Scrapy Request中,
          确保请求携带完整的登录态和搜索参数Cookie.
    """

    def __init__(self):
        self.cookies = parse_cookie_string(AUTH_COOKIE_STRING)

    @classmethod
    def from_crawler(cls, crawler):
        return cls()

    def process_request(self, request, spider):
        """
        处理每个请求: 合并Cookie到request.cookies中.
        策略: 先设置我们的Cookie作为基础, 再叠加请求自带的Cookie.
              (请求自带的Cookie优先级更高, 但通常为空)
        """
        existing = {}
        if isinstance(request.cookies, dict):
            existing = request.cookies
        elif isinstance(request.cookies, list):
            for c in request.cookies:
                name = c.get("name", c.get("key", ""))
                value = c.get("value", "")
                if name:
                    existing[name] = value

        # 合并: 我们的Cookie作为基础, 请求自带的覆盖
        merged = dict(self.cookies)
        merged.update(existing)
        request.cookies = merged

        return None


class CtripSeleniumMiddleware:
    """
    Selenium + Chrome 渲染中间件
    ----------------------------
    优先级: 200 (在Cookie中间件之后执行)
    功能:
      1. 创建Chrome无头浏览器实例
      2. 先访问ctrip.com域名, 将Cookie注入浏览器 (max-age=0, 会话级别)
      3. 导航到目标URL
      4. 等待页面JS渲染完成, 滚动加载懒加载内容
      5. 将渲染后的HTML返回给Scrapy解析
    """

    def __init__(self):
        self.driver = None
        self._parsed_cookies = parse_cookie_string(AUTH_COOKIE_STRING)

    @classmethod
    def from_crawler(cls, crawler):
        middleware = cls()
        crawler.signals.connect(middleware.spider_closed, signal=signals.spider_closed)
        return middleware

    def _create_driver(self):
        """
        创建并配置Chrome WebDriver.
        - 无头模式 (headless=new)
        - 隐藏自动化特征
        - 设置合理的窗口大小
        """
        from selenium import webdriver
        from selenium.webdriver.chrome.options import Options

        chrome_options = Options()
        chrome_options.add_argument("--headless=new")      # 新版无头模式
        chrome_options.add_argument("--no-sandbox")
        chrome_options.add_argument("--disable-dev-shm-usage")
        chrome_options.add_argument("--disable-gpu")
        chrome_options.add_argument("--window-size=1920,1080")
        chrome_options.add_argument("--disable-blink-features=AutomationControlled")
        chrome_options.add_argument("--disable-infobars")

        # 隐藏 webdriver 特征
        chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        chrome_options.add_experimental_option("useAutomationExtension", False)

        # 设置User-Agent
        chrome_options.add_argument(
            "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/126.0.0.0 Safari/537.36"
        )

        driver = webdriver.Chrome(options=chrome_options)

        # 通过CDP隐藏webdriver属性
        driver.execute_cdp_cmd(
            "Page.addScriptToEvaluateOnNewDocument",
            {
                "source": """
                    Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
                    Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3,4,5]});
                    Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN','zh','en']});
                """
            },
        )

        return driver

    def _inject_cookies_into_browser(self, driver):
        """
        将Cookie注入到Selenium浏览器中.
        策略:
          1. 先访问 hotels.ctrip.com 域名以建立域上下文
          2. 用JS直接设置 document.cookie (绕过Selenium add_cookie的域限制)
          3. 刷新页面使Cookie生效
        """
        logger.info("正在注入 %d 个Cookie到浏览器...", len(self._parsed_cookies))

        # Step 1: 访问 hotels.ctrip.com 建立域上下文
        driver.get("https://hotels.ctrip.com/")
        time.sleep(3)

        # Step 2: 使用 JavaScript 直接设置所有 Cookie
        # 这个方法比 Selenium add_cookie 更可靠, 不受域名限制
        cookie_js_parts = []
        for name, value in self._parsed_cookies.items():
            # 转义特殊字符
            safe_value = value.replace("'", "\\'").replace("\n", "").replace("\r", "")
            cookie_js_parts.append(
                f"document.cookie = '{name}={safe_value}; path=/; domain=.ctrip.com';"
            )

        # 分批执行, 避免JS过长
        batch_size = 20
        for i in range(0, len(cookie_js_parts), batch_size):
            batch = cookie_js_parts[i:i + batch_size]
            js_code = "\n".join(batch)
            try:
                driver.execute_script(js_code)
            except Exception as e:
                logger.warning("批量Cookie注入警告 (batch %d): %s", i // batch_size, e)

        logger.info("Cookie JS注入完成, 共 %d 个", len(cookie_js_parts))

        # Step 3: 刷新页面让Cookie生效
        driver.get("https://hotels.ctrip.com/")
        time.sleep(2)
        logger.info("Cookie注入流程完毕, 已就绪")

    def _scroll_page(self, driver, scroll_count=5):
        """
        滚动页面以触发懒加载内容.
        分多次滚动, 每次滚动后等待渲染.
        """
        for i in range(1, scroll_count + 1):
            driver.execute_script(
                f"window.scrollTo(0, document.body.scrollHeight * {i} / {scroll_count});"
            )
            time.sleep(0.8)

        # 最后滚回顶部
        driver.execute_script("window.scrollTo(0, 0);")
        time.sleep(0.5)

    def _save_debug_html(self, body, url=""):
        """保存调试用HTML, 便于分析页面结构."""
        import os
        try:
            os.makedirs("output", exist_ok=True)
            filepath = os.path.join("output", "debug_page.html")
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(f"<!-- URL: {url} -->\n")
                f.write(body)
            logger.info("调试HTML已保存到: %s (%d 字符)", filepath, len(body))
        except Exception as e:
            logger.warning("保存调试HTML失败: %s", e)

    def process_request(self, request, spider):
        """
        核心方法: 使用Selenium获取渲染后的页面.
        每次请求:
          1. 如果driver未初始化, 创建并注入Cookie
          2. 导航到目标URL
          3. 等待页面加载
          4. 滚动页面触发懒加载
          5. 获取渲染后的HTML, 返回HtmlResponse
        """
        # 延迟初始化driver (首次请求时创建)
        if self.driver is None:
            logger.info("首次请求: 创建Chrome WebDriver并注入Cookie...")
            self.driver = self._create_driver()
            self._inject_cookies_into_browser(self.driver)

        target_url = request.url
        logger.info("Selenium 正在加载: %s", target_url)

        try:
            # 导航到目标URL
            self.driver.get(target_url)

            # 等待页面核心内容加载 (酒店列表需要较长时间)
            time.sleep(6)

            # 尝试等待酒店列表容器出现
            try:
                from selenium.webdriver.support.ui import WebDriverWait
                from selenium.webdriver.support import expected_conditions as EC
                from selenium.webdriver.common.by import By

                WebDriverWait(self.driver, 10).until(
                    EC.presence_of_element_located((By.TAG_NAME, "body"))
                )
            except Exception:
                pass

            # 额外等待JS渲染
            time.sleep(2)

            # 滚动页面以加载懒加载内容
            self._scroll_page(self.driver)

            # 获取最终页面源码
            body = self.driver.page_source
            current_url = self.driver.current_url

            logger.info("页面加载完成, URL: %s, 内容长度: %d", current_url, len(body))

            # 保存调试HTML (仅在第一个请求时保存)
            if not getattr(self, "_debug_saved", False):
                self._save_debug_html(body, current_url)
                self._debug_saved = True

            # 返回Scrapy HtmlResponse供Spider解析
            return HtmlResponse(
                url=current_url,
                body=body.encode("utf-8"),
                encoding="utf-8",
                request=request,
            )

        except Exception as e:
            logger.error("Selenium 请求失败: %s", e)
            import traceback
            traceback.print_exc()
            return None

    def spider_closed(self, spider):
        """爬虫关闭时清理WebDriver资源."""
        if self.driver:
            logger.info("关闭 Chrome WebDriver...")
            try:
                self.driver.quit()
            except Exception:
                pass
            self.driver = None