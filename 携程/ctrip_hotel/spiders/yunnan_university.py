import re
import json
import codecs
from scrapy import Spider, Request
from ctrip_hotel.items import CtripHotelItem


class YunnanHotelSpider(Spider):
    name = "yunnan_hotel"
    allowed_domains = ["ctrip.com", "trip.com", "ctripcorp.com"]

    # 目标酒店数量
    TARGET_COUNT = 100
    # 每页大约酒店数
    PER_PAGE = 13
    # 最大翻页数（防止无限循环）
    MAX_PAGES = 10

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.base_url = "https://hotels.ctrip.com/hotels/list?keyword=云南大学(呈贡校区)"
        self.total_scraped = 0
        self.seen_ids = set()  # 用于去重

    async def start(self):
        """异步入口 - 第1页"""
        url = self._build_url(1)
        self.logger.info(f"开始爬取第 1 页: {url}")
        yield Request(
            url,
            meta={'use_selenium': True, 'page': 1},
            callback=self.parse,
            errback=self.errback_httpbin,
            dont_filter=True,
        )

    def _build_url(self, page_num):
        """构建分页 URL - 首页带 v2 参数"""
        if page_num <= 1:
            return f"{self.base_url}&v2_mod=51&v2_version=E"
        # 后续页面用相同 URL，由 middleware 点击翻页
        return f"{self.base_url}&v2_mod=51&v2_version=E"

    def errback_httpbin(self, failure):
        self.logger.error(f"请求失败: {failure}")

    # ── 主解析方法 ──────────────────────────────────────────────────

    def parse(self, response):
        """解析每一页的酒店数据"""
        page = response.meta.get('page', 1)
        self.logger.info(f"===== 解析第 {page} 页: {response.url} =====")
        self.logger.info(f"页面长度: {len(response.text)} 字符")

        # 保存最新页面用于调试
        with open('debug_page.html', 'w', encoding='utf-8') as f:
            f.write(response.text)

        # 提取酒店数据：优先 DOM（含滚动加载的酒店），RSC 做补充
        hotels_from_rsc = self._extract_from_rsc_payload(response)
        hotels_from_nfes = self._extract_from_nfes_data(response)
        hotels_from_dom = self._extract_from_dom(response)

        self.logger.info(
            f"提取结果: RSC={len(hotels_from_rsc)}家, "
            f"NFES={len(hotels_from_nfes)}家, DOM={len(hotels_from_dom)}家"
        )

        # 以 DOM 为主（含滚动加载的酒店），RSC 补充详细信息
        if hotels_from_dom:
            hotels = hotels_from_dom
            # 用 RSC 的数据补充 DOM 中缺失的字段
            rsc_map = {h.get('hotel_id', ''): h for h in hotels_from_rsc if h.get('hotel_id')}
            for h in hotels:
                hid = h.get('hotel_id', '')
                if hid and hid in rsc_map and not h.get('price'):
                    h['price'] = rsc_map[hid].get('price', '')
                    if not h.get('rating'):
                        h['rating'] = rsc_map[hid].get('rating', '')
                    if not h.get('star'):
                        h['star'] = rsc_map[hid].get('star', '')
        elif hotels_from_rsc:
            hotels = hotels_from_rsc
        elif hotels_from_nfes:
            hotels = hotels_from_nfes
        else:
            self.logger.warning("⚠️ 所有方法均未提取到酒店数据")
            self._diagnose_page(response)
            return

        # 从 DOM 补充价格
        prices_from_dom = self._extract_prices_from_dom(response)
        for hotel in hotels:
            hid = hotel.get('hotel_id', '')
            if hid and hid in prices_from_dom and not hotel.get('price'):
                hotel['price'] = prices_from_dom[hid]

        # 输出本页酒店，去重
        new_count = 0
        for h in hotels:
            hid = h.get('hotel_id', '')
            if hid and hid in self.seen_ids:
                continue  # 跳过重复
            self.seen_ids.add(hid)
            self.total_scraped += 1
            new_count += 1
            yield h

        self.logger.info(
            f"第 {page} 页提取 {len(hotels)} 家酒店（新增 {new_count} 家），"
            f"累计 {self.total_scraped}/{self.TARGET_COUNT}"
        )

        # ── 判断是否需要翻页 ──
        if self.total_scraped >= self.TARGET_COUNT:
            self.logger.info(f"✅ 已到达目标数量 {self.TARGET_COUNT}，停止")
            return

        if page >= self.MAX_PAGES:
            self.logger.info(f"已达到最大翻页数 {self.MAX_PAGES}，停止")
            return

        # 如果本页全是重复数据，说明 URL 分页参数无效，停止
        if new_count == 0:
            self.logger.warning("本页无新酒店，URL 分页参数可能无效，停止翻页")
            return

        # 生成下一页请求（使用 pageIndex URL 参数）
        next_page = page + 1
        next_url = self._build_url(next_page)
        self.logger.info(f"→ 请求第 {next_page} 页: {next_url}")
        yield Request(
            next_url,
            meta={'use_selenium': True, 'page': next_page},
            callback=self.parse,
            errback=self.errback_httpbin,
            dont_filter=True,
        )

    # ── 数据提取方法 ─────────────────────────────────────────────────

    def _extract_from_rsc_payload(self, response):
        """从 self.__next_f.push 载荷中提取酒店数据"""
        pushes = re.findall(r'self\.__next_f\.push\(\[1,"(.*?)"\]\)', response.text)
        if not pushes:
            self.logger.info('未找到 __next_f.push 数据')
            return []

        combined = ''.join(pushes)

        try:
            unescaped = codecs.decode(combined, 'unicode_escape')
            unescaped = unescaped.encode('latin-1').decode('utf-8')
        except Exception as e:
            self.logger.error(f'解码 RSC 载荷失败: {e}')
            return []

        hl_idx = unescaped.find('"hotelList"')
        if hl_idx == -1:
            self.logger.info('RSC 载荷中未找到 hotelList')
            return []

        arr_start = unescaped.find('[', hl_idx)
        if arr_start == -1:
            return []

        arr_end = self._find_json_array_end(unescaped, arr_start)
        if arr_end == -1:
            return []

        try:
            raw_hotels = json.loads(unescaped[arr_start:arr_end])
        except json.JSONDecodeError as e:
            self.logger.error(f'解析 hotelList JSON 失败: {e}')
            return []

        return self._parse_hotel_objects(raw_hotels)

    def _extract_from_nfes_data(self, response):
        """从 window.__NFES_DATA__ 中提取酒店数据"""
        idx = response.text.find('window.__NFES_DATA__')
        if idx == -1:
            return []

        json_start = response.text.find('{', idx)
        if json_start == -1:
            return []

        json_end = self._find_json_object_end(response.text, json_start)
        if json_end == -1:
            return []

        try:
            data = json.loads(response.text[json_start:json_end])
        except json.JSONDecodeError:
            return []

        hotel_list = data.get('initListData', {}).get('hotelList', [])
        if not hotel_list:
            hotel_list = data.get('props', {}).get('initListData', {}).get('hotelList', [])

        return self._parse_hotel_objects(hotel_list)

    def _extract_from_dom(self, response):
        """从 DOM 中提取酒店数据"""
        hotel_cards = response.css('.hotel-card')
        if not hotel_cards:
            hotel_cards = response.xpath('//div[contains(@class, "hotel-card")]')

        self.logger.info(f'DOM 中找到 {len(hotel_cards)} 个 hotel-card 元素')

        items = []
        for card in hotel_cards:
            item = CtripHotelItem()

            # 酒店 ID
            hotel_id = card.xpath('./@id').get() or card.css('::attr(id)').get()
            item['hotel_id'] = hotel_id or ''

            # 酒店名：优先 .hotelName
            name = (card.css('.hotelName::text').get()
                    or card.css('.hotel-title .hotelName::text').get())
            if not name:
                # 回退：从 data-exposure 属性中提取
                exp = card.xpath('./@data-exposure').get()
                if exp:
                    try:
                        exp_data = json.loads(exp)
                        name = exp_data.get('data', {}).get('name', '')
                    except Exception:
                        pass
            item['name'] = name.strip() if name else ''

            # 星级：从 .hotelStar 的 aria-label 提取（如 "4 out of 5 stars"）
            star_aria = card.css('.hotelStar::attr(aria-label)').get()
            if star_aria:
                star_match = re.search(r'(\d+)', star_aria)
                if star_match:
                    item['star'] = star_match.group(1)

            # 评分
            rating = (card.css('.score::text').get()
                      or card.css('.comment-score .score::text').get())
            if rating:
                item['rating'] = rating.strip()

            # 评论数
            review_count = card.css('.comment-num::text').get()
            if review_count:
                item['review_count'] = review_count.strip()

            # 价格
            price = card.css('.sale::text').get()
            if not price:
                price = card.css('.price-line .sale::text').get()
            if not price:
                price = card.css('.room-price .sale::text').get()
            if price:
                item['price'] = price.strip()

            # 地址
            address = card.css('.position-desc::text').get()
            if address:
                item['address'] = address.strip()

            # 区域
            zone_els = card.css('.position-desc::text').getall()
            if len(zone_els) >= 2:
                item['zone'] = zone_els[1].strip()

            if item['name'] or item['hotel_id']:
                items.append(item)

        return items

    def _extract_prices_from_dom(self, response):
        """从 DOM 中提取每个酒店的价格"""
        prices = {}
        cards = response.css('.hotel-card')
        if not cards:
            cards = response.xpath('//div[contains(@class, "hotel-card")]')

        for card in cards:
            hotel_id = card.xpath('./@id').get() or card.css('::attr(id)').get()
            if not hotel_id:
                continue

            card_html = card.get()
            price_match = re.findall(r'¥\s*([\d,]+)', card_html)
            if price_match:
                numeric = []
                for p in price_match:
                    p_clean = p.replace(',', '')
                    if p_clean.isdigit():
                        numeric.append(int(p_clean))
                if numeric:
                    prices[hotel_id] = str(min(numeric))

        self.logger.info(f'从 DOM 提取到 {len(prices)} 家酒店的价格')
        return prices

    def _get_total_count(self, response):
        """从响应中提取酒店总数"""
        match = re.search(r'"hotelTotalCount":(\d+)', response.text)
        if match:
            return int(match.group(1))
        return None

    # ── 辅助方法 ─────────────────────────────────────────────────────

    def _find_json_array_end(self, text, start):
        depth = 0
        in_string = False
        escape_next = False
        for i in range(start, len(text)):
            c = text[i]
            if escape_next:
                escape_next = False
                continue
            if c == '\\':
                escape_next = True
                continue
            if c == '"':
                in_string = not in_string
                continue
            if in_string:
                continue
            if c == '[':
                depth += 1
            elif c == ']':
                depth -= 1
                if depth == 0:
                    return i + 1
        return -1

    def _find_json_object_end(self, text, start):
        depth = 0
        in_string = False
        escape_next = False
        for i in range(start, len(text)):
            c = text[i]
            if escape_next:
                escape_next = False
                continue
            if c == '\\':
                escape_next = True
                continue
            if c == '"':
                in_string = not in_string
                continue
            if in_string:
                continue
            if c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    return i + 1
        return -1

    def _parse_hotel_objects(self, raw_hotels):
        """将原始酒店 JSON 列表转换为 CtripHotelItem"""
        items = []
        for hotel in raw_hotels:
            item = CtripHotelItem()
            info = hotel.get('hotelInfo', {})

            summary = info.get('summary', {})
            item['hotel_id'] = summary.get('hotelId', '')

            name_info = info.get('nameInfo', {})
            item['name'] = name_info.get('name', '')

            hotel_star = info.get('hotelStar', {})
            item['star'] = str(hotel_star.get('star', ''))
            item['star_type'] = str(hotel_star.get('starType', ''))

            comment_info = info.get('commentInfo', {})
            item['rating'] = comment_info.get('commentScore', '')
            item['review_count'] = comment_info.get('commenterNumber', '')

            position_info = info.get('positionInfo', {})
            item['address'] = position_info.get('address', '')
            item['city'] = position_info.get('cityName', '')
            zones = position_info.get('zoneNames', [])
            item['zone'] = ', '.join(zones) if zones else ''

            # 价格从 roomInfo 提取
            room_infos = hotel.get('roomInfo', [])
            prices = []
            for room in room_infos:
                pay_info = room.get('payInfo', {})
                price = pay_info.get('price') or pay_info.get('salePrice') or pay_info.get('displayPrice')
                if price:
                    prices.append(str(price))
                rsum = room.get('summary', {})
                rprice = rsum.get('price') or rsum.get('salePrice')
                if rprice:
                    prices.append(str(rprice))

            if prices:
                try:
                    numeric_prices = []
                    for p in prices:
                        p_clean = p.replace(',', '')
                        if p_clean.replace('.', '').isdigit():
                            numeric_prices.append(float(p_clean))
                    if numeric_prices:
                        item['price'] = str(min(numeric_prices))
                except Exception:
                    item['price'] = ''
            else:
                item['price'] = ''

            item['price_delete'] = ''
            items.append(item)

        return items

    # ── 页面诊断 ─────────────────────────────────────────────────────

    def _diagnose_page(self, response):
        text = response.text
        markers = {
            'hotel-card': '酒店卡片',
            'hotelList': '酒店列表数据',
            'login': '登录相关',
            'captcha': '验证码',
            'spider-defence': '反爬虫脚本',
            '__NFES_DATA__': 'NFES 数据',
            '__next_f.push': 'Next.js RSC 数据',
        }
        for marker, desc in markers.items():
            count = text.count(marker)
            if count > 0:
                self.logger.info(f'  📋 {desc} ({marker}): 出现 {count} 次')
            else:
                self.logger.info(f'  ❌ {desc} ({marker}): 未找到')

        title_match = re.search(r'<title>(.*?)</title>', text)
        if title_match:
            self.logger.info(f'  📄 页面标题: {title_match.group(1)}')
