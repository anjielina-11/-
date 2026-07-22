import time
from scrapy import signals
from scrapy.http import HtmlResponse
from selenium import webdriver
from selenium.webdriver.edge.options import Options
from selenium.webdriver.edge.service import Service
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import TimeoutException
from webdriver_manager.microsoft import EdgeChromiumDriverManager


class SeleniumMiddleware:
    def __init__(self, cookie_str):
        self.driver = None
        self.cookie_str = cookie_str
        self._current_page = 0

    @classmethod
    def from_crawler(cls, crawler):
        cookie_str = crawler.settings.get('COOKIE_STRING')
        middleware = cls(cookie_str)
        crawler.signals.connect(middleware.spider_closed, signals.spider_closed)
        return middleware

    def spider_closed(self, spider):
        if self.driver:
            spider.logger.info('关闭 Selenium WebDriver')
            self.driver.quit()

    def _create_driver(self):
        edge_options = Options()
        edge_options.add_argument('--disable-gpu')
        edge_options.add_argument('--no-sandbox')
        edge_options.add_argument('--disable-dev-shm-usage')
        edge_options.add_argument('--disable-blink-features=AutomationControlled')
        edge_options.add_argument('--window-size=1920,1080')
        edge_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        edge_options.add_experimental_option('useAutomationExtension', False)
        edge_options.add_argument(
            '--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
            'AppleWebKit/537.36 (KHTML, like Gecko) '
            'Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0'
        )
        prefs = {
            'credentials_enable_service': False,
            'profile.password_manager_enabled': False,
        }
        edge_options.add_experimental_option('prefs', prefs)

        service = Service(EdgeChromiumDriverManager().install())
        driver = webdriver.Edge(service=service, options=edge_options)
        driver.set_page_load_timeout(60)
        driver.implicitly_wait(10)

        driver.execute_cdp_cmd(
            'Page.addScriptToEvaluateOnNewDocument',
            {'source': '''
                Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
                Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});
                Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh', 'en']});
                window.chrome = {runtime: {}};
            '''}
        )
        return driver

    def _inject_cookies_via_cdp(self, spider):
        if not self.cookie_str:
            spider.logger.warning('未配置 COOKIE_STRING，跳过 Cookie 注入')
            return
        cookies_added = 0
        for cookie_pair in self.cookie_str.split(';'):
            cookie_pair = cookie_pair.strip()
            if '=' not in cookie_pair:
                continue
            key, value = cookie_pair.split('=', 1)
            for domain in ['.ctrip.com', '.trip.com']:
                try:
                    self.driver.execute_cdp_cmd('Network.setCookie', {
                        'name': key, 'value': value, 'domain': domain,
                        'path': '/', 'secure': False, 'httpOnly': False,
                    })
                    cookies_added += 1
                    break
                except Exception:
                    continue
        spider.logger.info(f'通过 CDP 成功注入 {cookies_added} 个 Cookie')

    def process_request(self, request, spider):
        if not request.meta.get('use_selenium'):
            return None

        target_page = request.meta.get('page', 1)

        # ── 首次请求：创建 driver、注入 cookie、加载首页 ──
        if self.driver is None:
            spider.logger.info('首次请求：创建 Edge WebDriver...')
            self.driver = self._create_driver()
            self._inject_cookies_via_cdp(spider)
            self._current_page = 1

            spider.logger.info(f'加载首页: {request.url}')
            try:
                self.driver.get(request.url)
            except Exception as e:
                spider.logger.error(f'页面加载超时: {e}')

            # 首页：等待加载 + 多次滚动触发懒加载更多酒店
            self._wait_and_scroll(spider)
            self._scroll_to_load_more(spider, max_rounds=10)

        # ── 后续页面：通过无限滚动加载更多 ──
        else:
            spider.logger.info(f'尝试滚动加载更多酒店（第 {self._current_page} → {target_page} 页）...')
            # 持续滚动加载，直到酒店数量足够或没有新内容
            loaded = self._scroll_to_load_more(spider, max_rounds=3)
            self._current_page = target_page
            if not loaded:
                spider.logger.warning('滚动后未加载新酒店')

        body = self.driver.page_source
        current_url = self.driver.current_url
        spider.logger.info(f'第 {target_page} 页 | URL: {current_url} | 长度: {len(body)}')

        if 'login' in current_url.lower():
            spider.logger.error('⚠️ Cookie 已过期，需要更新！')

        return HtmlResponse(
            current_url,
            body=body,
            encoding='utf-8',
            request=request
        )

    def _scroll_to_load_more(self, spider, max_rounds=5):
        """多次滚动到底部，触发懒加载更多酒店"""
        loaded_any = False
        for rnd in range(max_rounds):
            prev_height = self.driver.execute_script('return document.body.scrollHeight;')
            # 滚动到底部
            self.driver.execute_script('window.scrollTo(0, document.body.scrollHeight);')
            time.sleep(3)
            new_height = self.driver.execute_script('return document.body.scrollHeight;')
            if new_height > prev_height:
                spider.logger.info(f'  第 {rnd+1} 轮滚动：页面增高 {new_height - prev_height}px（有新内容加载）')
                loaded_any = True
            else:
                spider.logger.info(f'  第 {rnd+1} 轮滚动：页面高度未变化（可能已到底）')
                break
        return loaded_any

    def _wait_and_scroll(self, spider):
        """等待页面加载并滚动触发懒加载"""
        wait = WebDriverWait(self.driver, 20)
        try:
            wait.until(EC.presence_of_element_located(
                (By.CSS_SELECTOR, '.hotel-card, .hotel-list, [data-exposure]')
            ))
        except TimeoutException:
            spider.logger.warning('等待页面元素超时')

        time.sleep(4)
        for pct in [33, 66]:
            self.driver.execute_script(f'window.scrollTo(0, document.body.scrollHeight * {pct} / 100);')
            time.sleep(1.5)

    def _go_to_next_page_js(self, spider):
        """使用 JavaScript 查找并点击分页的下一次按钮"""
        # 先滚动到底部，确保分页组件加载
        self.driver.execute_script('window.scrollTo(0, document.body.scrollHeight);')
        time.sleep(4)

        js_code = '''
        var all = document.querySelectorAll('*');

        // 方案1: 找包含 next/page 语义的 class 或 aria 属性
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            if (el.offsetParent === null) continue;
            var cls = (el.className || '').toString();
            var aria = (el.getAttribute('aria-label') || '');
            // 检查 class 或 aria 中的翻页语义
            var isNext = (
                cls.indexOf('next') >= 0 || cls.indexOf('Next') >= 0 ||
                cls.indexOf('btn-next') >= 0 || cls.indexOf('ant-pagination-next') >= 0 ||
                aria.indexOf('Next') >= 0 || aria.indexOf('下一页') >= 0 ||
                aria.indexOf('next page') >= 0
            );
            if (isNext) {
                el.click();
                return 'clicked:class-aria ' + el.tagName + ' cls=' + cls.substring(0,50) + ' aria=' + aria;
            }
        }

        // 方案2: 查找元素文本为纯箭头字符
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            if (el.offsetParent === null) continue;
            var txt = (el.innerText || '').trim();
            if (txt === '>' || txt === '›' || txt === '»' || txt === '→' || txt === '▶') {
                if (el.tagName === 'A' || el.tagName === 'BUTTON' || el.tagName === 'LI' || el.tagName === 'SPAN') {
                    el.click();
                    return 'clicked:arrow ' + el.tagName + ' text=' + txt;
                }
            }
        }

        // 方案3: 查找分页容器中的最后一个可点元素（通常是"下一页"）
        var pagers = document.querySelectorAll('[class*="pagin"], [class*="Pagin"], [class*="pager"], [class*="Pager"], [class*="page-nav"], [class*="PageNav"], [class*="list-pager"], [class*="ListPager"], nav[aria-label*="pagin"], nav[aria-label*="Pagin"]');
        for (var i = 0; i < pagers.length; i++) {
            var btns = pagers[i].querySelectorAll('a, button, li, [role="button"]');
            if (btns.length > 0) {
                // 点击最后一个按钮（通常是"下一页"）
                var lastBtn = btns[btns.length - 1];
                if (lastBtn.offsetParent !== null) {
                    lastBtn.click();
                    return 'clicked:last-in-pager ' + lastBtn.tagName + ' cls=' + (lastBtn.className || '').substring(0,50);
                }
            }
        }

        // 方案4: 查找所有 button/a 包含数字（页码），点击最大的数字旁边的下一项
        var numberElements = [];
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            if (el.offsetParent === null) continue;
            if (el.tagName !== 'A' && el.tagName !== 'BUTTON' && el.tagName !== 'LI') continue;
            var txt = (el.innerText || '').trim();
            if (/^\\d+$/.test(txt)) {
                numberElements.push({el: el, num: parseInt(txt), rect: el.getBoundingClientRect()});
            }
        }
        if (numberElements.length > 0) {
            // 找到最大页码，尝试点它后面的元素
            numberElements.sort(function(a,b) { return b.num - a.num; });
            var maxNumEl = numberElements[0].el;
            var parent = maxNumEl.parentElement;
            if (parent) {
                var siblings = parent.querySelectorAll('a, button, li');
                for (var k = 0; k < siblings.length; k++) {
                    if (siblings[k] === maxNumEl && k + 1 < siblings.length) {
                        siblings[k + 1].click();
                        return 'clicked:after-max-page-num ' + siblings[k+1].tagName;
                    }
                }
            }
            // 或者直接点最大页码，让它到最后一页
            // 再找最后一页后面的"下一页"按钮
        }

        // 方案5: 暴力搜索 - 收集底部所有可见元素用于调试
        var bodyH = document.body.scrollHeight;
        var bottomElements = [];
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            if (el.offsetParent === null) continue;
            var rect = el.getBoundingClientRect();
            // 看 viewport 底部的元素（不一定在整个 body 底部）
            var vh = window.innerHeight;
            if (rect.top > vh * 0.5 && rect.top < vh) {
                var t = (el.innerText || '').trim();
                var tag = el.tagName;
                var cls = (el.className || '').toString().substring(0, 30);
                if (t && t.length < 30 && (tag === 'A' || tag === 'BUTTON' || tag === 'LI' || tag === 'SPAN' || tag === 'DIV')) {
                    bottomElements.push(t + '|' + tag + '|' + cls);
                }
            }
        }

        return 'notfound debug:' + JSON.stringify(bottomElements.slice(-40));
        '''
        result = self.driver.execute_script(js_code)
        spider.logger.info(f'翻页 JS: {result[:500]}')
        return result.startswith('clicked:')
