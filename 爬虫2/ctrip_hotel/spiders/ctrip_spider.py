# -*- coding: utf-8 -*-
"""
携程酒店爬虫 - 云南大学(呈贡校区)周边酒店信息采集
======================================================
基于 Scrapy + Selenium 框架, 使用Cookie绕过反爬验证.

功能:
  1. 访问携程酒店搜索页, 搜索云南大学(呈贡校区)周边酒店
  2. 从页面内嵌JSON数据中提取酒店列表
  3. 解析酒店名称、地址、评分、价格、星级、详情链接
  4. 通过Scrapy Item Pipeline 导出结构化CSV文件

数据字段:
  - hotel_name  : 酒店名称
  - address     : 酒店地址 (含距云南大学的距离)
  - rating      : 评分 / 点评数
  - price       : 参考价格
  - star_level  : 星级/档次
  - detail_url  : 酒店详情页链接
"""

import scrapy
import json
import re
import os
from urllib.parse import urlencode


class CtripHotelSpider(scrapy.Spider):
    """携程酒店列表爬虫"""

    name = "ctrip_hotel"
    allowed_domains = ["hotels.ctrip.com", "ctrip.com"]

    # ================================================================
    # 搜索参数 - 云南大学(呈贡校区)
    # cityId=34 对应昆明市, countryId=1 中国, provinceId=25 云南
    # ================================================================
    SEARCH_PARAMS = {
        "countryId": 1,
        "provinceId": 25,
        "cityId": 34,
        "searchWord": "云南大学(呈贡校区)",
        "cityName": "昆明",
        "searchType": "",
        "searchValue": "",
    }

    # 入住/离店日期
    CHECKIN = "2026-07-14"
    CHECKOUT = "2026-07-15"

    def start_requests(self):
        """
        构建携程酒店搜索URL并生成初始请求.
        URL格式: https://hotels.ctrip.com/hotel/kunming34?参数...
        """
        base_url = "https://hotels.ctrip.com/hotel/kunming34"
        params = {
            "countryId": self.SEARCH_PARAMS["countryId"],
            "provinceId": self.SEARCH_PARAMS["provinceId"],
            "cityId": self.SEARCH_PARAMS["cityId"],
            "searchWord": self.SEARCH_PARAMS["searchWord"],
            "cityName": self.SEARCH_PARAMS["cityName"],
            "checkIn": self.CHECKIN,
            "checkOut": self.CHECKOUT,
            "adult": 1,
            "children": 0,
        }
        url = base_url + "?" + urlencode(params)
        self.logger.info("搜索URL: %s", url)

        yield scrapy.Request(
            url=url,
            callback=self.parse,
            meta={"handle_httpstatus_all": True},
        )

    def parse(self, response):
        """
        解析携程酒店列表页.
        策略:
          1. 优先从页面内嵌JSON中提取 hotelList 数组
          2. 尝试 __NEXT_DATA__ (Next.js SSR数据)
          3. 尝试 __INITIAL_STATE__ / window.__NUXT__
          4. 回退到正则匹配酒店信息
        """
        page_text = response.text
        self.logger.info("页面内容长度: %d 字符", len(page_text))
        self.logger.info("当前URL: %s", response.url)

        # 策略1: 从页面中提取 hotelList JSON 数组
        hotels = list(self._extract_hotellist_json(page_text))
        if hotels:
            self.logger.info("✅ 成功从 hotelList JSON 提取 %d 条酒店数据", len(hotels))
            yield from hotels
            return

        # 策略2: 提取 __NEXT_DATA__ (Next.js)
        hotels = list(self._extract_next_data(page_text))
        if hotels:
            self.logger.info("✅ 成功从 __NEXT_DATA__ 提取 %d 条酒店数据", len(hotels))
            yield from hotels
            return

        # 策略3: 提取 window.__INITIAL_STATE__ / __NUXT__
        hotels = list(self._extract_init_state(page_text))
        if hotels:
            self.logger.info("✅ 成功从 __INITIAL_STATE__ 提取 %d 条酒店数据", len(hotels))
            yield from hotels
            return

        # 策略4: 正则全局搜索酒店信息
        hotels = list(self._extract_regex_fallback(page_text))
        if hotels:
            self.logger.info("✅ 成功通过正则匹配提取 %d 条酒店数据", len(hotels))
            yield from hotels
            return

        self.logger.error("❌ 所有提取策略均失败, 未找到酒店数据!")
        self.logger.error("请检查 output/debug_page.html 分析页面结构")

    # ================================================================
    # 提取策略
    # ================================================================

    def _extract_hotellist_json(self, text):
        """
        策略1: 提取内嵌的 hotelList / htlList JSON数组.
        携程页面通常在 script 标签中有类似:
          "hotelList":[{...},{...},...]  或  "htlList":[{...}]
        的结构.
        """
        # 查找所有可能的酒店列表标记
        markers = [
            '"hotelList":[', '"HotelList":[', '"htlList":[',
            'hotelList:[', 'htlList:[',
            '"hotelList": [', '"htlList": [',
        ]
        found_start = -1
        found_marker = ""
        for marker in markers:
            pos = text.find(marker)
            if pos >= 0:
                found_start = pos
                found_marker = marker
                break

        if found_start < 0:
            self.logger.info("hotelList/htlList 标记未找到")
            return

        # 找到 [ 的位置
        bracket_start = text.find("[", found_start)
        if bracket_start < 0:
            return

        # 计算匹配的 ] 位置 (处理嵌套)
        json_str = self._extract_json_array(text, bracket_start)
        if not json_str:
            return

        self.logger.info("%s JSON 长度: %d 字符", found_marker.strip('"'), len(json_str))

        try:
            hotels = json.loads(json_str)
        except json.JSONDecodeError as e:
            self.logger.warning("JSON 解析失败: %s, 尝试修复...", e)
            hotels = self._try_fix_and_parse(json_str)

        if not isinstance(hotels, list):
            self.logger.warning("酒店列表不是 list 类型, 而是: %s", type(hotels).__name__)
            return

        self.logger.info("解析到 %d 个酒店对象", len(hotels))
        for hotel in hotels:
            if isinstance(hotel, dict):
                item = self._parse_hotel(hotel)
                if item and item.get("hotel_name") and item["hotel_name"] != "N/A":
                    yield item

    def _extract_next_data(self, text):
        """
        策略2: 提取 __NEXT_DATA__ (Next.js SSR 数据).
        携程PC版可能使用Next.js, 数据内嵌在:
          <script id="__NEXT_DATA__" type="application/json">...</script>
        """
        match = re.search(
            r'<script[^>]*id="__NEXT_DATA__"[^>]*type="application/json"[^>]*>(.*?)</script>',
            text,
            re.DOTALL,
        )
        if not match:
            return

        try:
            data = json.loads(match.group(1))
        except json.JSONDecodeError:
            return

        # 深度搜索 hotelList
        hotels = self._deep_search(data, ["hotelList", "HotelList", "htlList", "hotels", "list"])
        if not hotels:
            hotels = self._deep_search(data, ["props", "pageProps", "initialState"])

        if isinstance(hotels, list):
            for h in hotels:
                if isinstance(h, dict):
                    item = self._parse_hotel(h)
                    if item and item.get("hotel_name") and item["hotel_name"] != "N/A":
                        yield item

    def _extract_init_state(self, text):
        """
        策略3: 提取 window.__INITIAL_STATE__ / window.__NUXT__.
        使用括号计数法提取完整JSON对象.
        页面可能有多个 __NUXT__ 赋值, 需要遍历所有出现位置.
        """
        # 尝试多种标记
        markers = [
            "window.__NUXT__=",
            "window.__INITIAL_STATE__=",
            "window.__NEXT_DATA__=",
        ]

        for marker in markers:
            search_from = 0
            occurrence = 0
            while True:
                start = text.find(marker, search_from)
                if start < 0:
                    break
                occurrence += 1

                # 跳过标记, 找到 { 的位置
                brace_start = text.find("{", start + len(marker))
                if brace_start < 0:
                    search_from = start + len(marker)
                    continue

                # 使用括号计数提取完整的JSON对象
                json_str = self._extract_json_object(text, brace_start)
                if not json_str:
                    self.logger.info("%s #%d JSON对象提取失败", marker, occurrence)
                    search_from = brace_start + 1
                    continue

                self.logger.info("%s #%d JSON长度: %d 字符", marker, occurrence, len(json_str))

                try:
                    data = json.loads(json_str)
                except json.JSONDecodeError as e:
                    self.logger.warning("%s #%d JSON解析失败: %s", marker, occurrence, e)
                    search_from = brace_start + len(json_str) + 1
                    continue

                # 深度搜索酒店列表
                hotels = self._deep_search(data, ["htlList", "hotelList", "HotelList", "hotels", "list"])
                if isinstance(hotels, list) and len(hotels) > 0:
                    self.logger.info("从 %s #%d 中找到 %d 个酒店", marker, occurrence, len(hotels))
                    for h in hotels:
                        if isinstance(h, dict):
                            item = self._parse_hotel(h)
                            if item and item.get("hotel_name") and item["hotel_name"] != "N/A":
                                yield item
                    return
                else:
                    self.logger.info("%s #%d 中未找到酒店列表", marker, occurrence)

                # 继续搜索下一个出现位置
                search_from = brace_start + len(json_str) + 1

    def _extract_json_object(self, text, start):
        """从 start 位置开始提取完整的JSON对象 (处理嵌套花括号)."""
        depth = 0
        end = start
        in_string = False
        escape = False

        while end < len(text):
            c = text[end]

            if escape:
                escape = False
                end += 1
                continue

            if c == "\\":
                escape = True
                end += 1
                continue

            if c == '"':
                in_string = not in_string
                end += 1
                continue

            if in_string:
                end += 1
                continue

            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    end += 1
                    return text[start:end]

            end += 1

        return None

    def _extract_regex_fallback(self, text):
        """
        策略4: 正则表达式回退方案.
        匹配类似 {"hotelName":"xxx","address":"xxx",...} 的JSON片段.
        """
        # 匹配包含 hotelName 的JSON对象
        pattern = r'\{[^{}]*"hotelName"\s*:\s*"[^"]*"[^{}]*\}'
        matches = re.findall(pattern, text)
        self.logger.info("正则匹配到 %d 个可能的酒店对象", len(matches))

        for match in matches:
            try:
                obj = json.loads(match)
                item = self._parse_hotel(obj)
                if item and item.get("hotel_name") and item["hotel_name"] != "N/A":
                    yield item
            except json.JSONDecodeError:
                # 尝试修复后再解析
                obj = self._try_fix_and_parse(match)
                if isinstance(obj, dict):
                    item = self._parse_hotel(obj)
                    if item and item.get("hotel_name") and item["hotel_name"] != "N/A":
                        yield item

    # ================================================================
    # 辅助方法
    # ================================================================

    def _extract_json_array(self, text, start):
        """从 start 位置开始提取完整的JSON数组 (处理嵌套括号)."""
        depth = 0
        end = start
        in_string = False
        escape = False

        while end < len(text):
            c = text[end]

            if escape:
                escape = False
                end += 1
                continue

            if c == "\\":
                escape = True
                end += 1
                continue

            if c == '"':
                in_string = not in_string
                end += 1
                continue

            if in_string:
                end += 1
                continue

            if c == "[":
                depth += 1
            elif c == "]":
                depth -= 1
                if depth == 0:
                    end += 1
                    return text[start:end]

            end += 1

        return None

    def _deep_search(self, obj, keys):
        """在嵌套的dict/list中递归搜索指定的key, 返回第一个匹配的值."""
        if isinstance(obj, dict):
            for k in keys:
                if k in obj:
                    return obj[k]
            for v in obj.values():
                result = self._deep_search(v, keys)
                if result is not None:
                    return result
        elif isinstance(obj, list):
            for item in obj:
                result = self._deep_search(item, keys)
                if result is not None:
                    return result
        return None

    def _try_fix_and_parse(self, json_str):
        """尝试修复并解析可能截断或有问题的JSON字符串."""
        # 方法1: 直接解析
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            pass

        # 方法2: 尝试补全截断的JSON
        fixed = json_str.rstrip()
        # 补全缺失的闭合符号
        open_braces = fixed.count("{") - fixed.count("}")
        open_brackets = fixed.count("[") - fixed.count("]")
        fixed += "}" * open_braces + "]" * open_brackets

        try:
            return json.loads(fixed)
        except json.JSONDecodeError:
            pass

        # 方法3: 使用正则提取散列字段
        result = {}
        fields = {
            "hotelName": "hotel_name",
            "hotelId": "hotel_id",
            "address": "address",
            "commentScore": "score",
            "price": "price",
            "hotelStarRating": "star",
        }
        for json_key, _ in fields.items():
            m = re.search(rf'"{json_key}"\s*:\s*"([^"]*)"', json_str)
            if m:
                result[json_key] = m.group(1)
            else:
                m = re.search(rf'"{json_key}"\s*:\s*([\d.]+)', json_str)
                if m:
                    try:
                        result[json_key] = int(m.group(1))
                    except ValueError:
                        result[json_key] = float(m.group(1))

        return result if result else None

    # ================================================================
    # 酒店对象解析
    # ================================================================

    def _parse_hotel(self, hotel):
        """
        将携程酒店JSON对象解析为结构化字典.

        携程酒店JSON结构大致如下:
        {
          "hotelInfo": {
            "summary": {
              "hotelId": 123456,
              "hotelName": "酒店名称",
              "hotelStarRating": "五星级",
              ...
            },
            "positionInfo": {
              "address": "详细地址",
              "distance": "距云南大学1.2公里",
              ...
            },
            "priceInfo": {
              "price": 299,
              ...
            }
          },
          "commentInfo": {
            "commentScore": 4.6,
            "commenterNumber": "1234条点评",
            ...
          },
          "hotelTags": { ... }
        }
        """
        # 兼容不同的数据来源结构
        # 有些来源直接是扁平结构, 有些有 hotelInfo 嵌套
        info = hotel.get("hotelInfo", hotel)
        if isinstance(info, dict):
            basic = info.get("summary", info.get("basicInfo", info))
            position = info.get("positionInfo", info.get("position", {}))
            price_info = info.get("priceInfo", info.get("price", {}))
        else:
            basic = hotel
            position = hotel
            price_info = hotel

        comment = hotel.get("commentInfo", hotel.get("comment", {}))
        tags = hotel.get("hotelTags", hotel.get("tags", {}))

        # --- 酒店名称 ---
        name = basic.get("hotelName", "")
        if not name:
            # 尝试从 tags 中获取
            name_tags = tags.get("hotelNameTags", [])
            if name_tags and isinstance(name_tags, list) and len(name_tags) > 0:
                tg = name_tags[0]
                name = tg.get("name", str(tg)) if isinstance(tg, dict) else str(tg)
        if not name:
            name = hotel.get("name", hotel.get("hotel_name", ""))

        # --- 地址 ---
        address = position.get("address", "")
        if not address:
            address = info.get("address", "") if isinstance(info, dict) else ""

        # 距离信息
        distance = position.get("distance", "")
        if not distance:
            distance = position.get("distanceDesc", "")
        if not distance:
            distance = hotel.get("distance", "")
        if not distance:
            distance = ""

        if address and distance:
            address = f"{address}（距离云南大学{distance}）"
        elif distance:
            address = f"距离云南大学{distance}"
        if not address:
            address = "N/A"

        # --- 评分 ---
        score = comment.get("commentScore", "")
        if not score:
            score = hotel.get("commentScore", hotel.get("score", ""))

        commenter = comment.get("commenterNumber", "")
        if not commenter:
            commenter = comment.get("commentCount", "")
        if not commenter:
            commenter = hotel.get("commenterNumber", hotel.get("commentCount", ""))

        rating_parts = []
        if score:
            rating_parts.append(f"评分{score}")
        if commenter:
            rating_parts.append(str(commenter))
        rating = " / ".join(rating_parts) if rating_parts else "N/A"

        # --- 价格 ---
        price = price_info.get("price", "")
        if not price:
            price = price_info.get("showPrice", "")
        if not price:
            price = price_info.get("lowestPrice", "")
        if not price:
            price = hotel.get("price", hotel.get("lowestPrice", ""))
        if isinstance(price, (int, float)):
            price = f"¥{price}"
        if not price:
            price = "N/A"

        # --- 星级 ---
        star = basic.get("hotelStarRating", "")
        if not star:
            star = basic.get("star", basic.get("starLevel", ""))
        if not star:
            star = hotel.get("hotelStarRating", hotel.get("star", hotel.get("starLevel", "")))
        if not star:
            star = "N/A"

        # --- 详情URL ---
        hotel_id = basic.get("hotelId", "")
        if not hotel_id:
            hotel_id = hotel.get("hotelId", hotel.get("id", ""))
        detail_url = ""
        if hotel_id:
            detail_url = f"https://hotels.ctrip.com/hotels/detail/?hotelId={hotel_id}"

        return {
            "hotel_name": str(name).strip(),
            "address": str(address).strip(),
            "rating": str(rating).strip(),
            "price": str(price).strip(),
            "star_level": str(star).strip(),
            "detail_url": detail_url,
        }

    def _save_debug(self, text):
        """保存调试HTML到 output/debug_page.html (仅首次)."""
        if getattr(self, "_debug_saved", False):
            return
        try:
            os.makedirs("output", exist_ok=True)
            filepath = os.path.join("output", "debug_page.html")
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(text)
            self._debug_saved = True
        except Exception:
            pass  # 静默忽略保存失败
