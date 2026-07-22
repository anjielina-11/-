import requests
import csv
import time
import json
from datetime import datetime

# ========== 配置区（每次更新Cookie和签名时，只改这里） ==========
CONFIG = {
    "cookies": {
        'UBT_VID': '1783923138329.26b82sgIuTIc',
    'Hm_lvt_a8d6737197d542432f4ff4abc6e06384': '1783923138',
    'HMACCOUNT': '65C317E04D6F090A',
    'Session': 'smartlinkcode=U130727&smartlinklanguage=zh&SmartLinkKeyWord=&SmartLinkQuary=&SmartLinkHost=',
    'Union': 'AllianceID=4902&SID=130727&OUID=&createtime=1783923138&Expires=1784527938409',
    'MKT_CKID': '1783923138460.scnhs.gpg0',
    '_ga': 'GA1.1.951555538.1783923139',
    'GUID': '09031055110347578714',
    '_RGUID': '284915d1-48c0-46a0-9153-fe19ac209835',
    'MKT_Pagesource': 'PC',
    'manualclose': '1',
    'Hm_lpvt_a8d6737197d542432f4ff4abc6e06384': '1783923153',
    'ibulocale': 'zh_cn',
    'cookiePricesDisplayed': 'CNY',
    '_ga_5DVRDQD429': 'GS2.1.s1783923138$o1$g1$t1783923230$j60$l0$h560453016',
    '_ga_B77BES1Z8Z': 'GS2.1.s1783923138$o1$g1$t1783923230$j60$l0$h0',
    '_ga_9BZF483VNQ': 'GS2.1.s1783923138$o1$g1$t1783923230$j60$l0$h0',
    'nfes_isSupportWebP': '1',
    '_abtest_userid': '49056b4f-9d71-4366-9c78-0b0b84f12354',
    'cticket': 'F310E13A04FC4C6D72AEC997B151774059F556B73FF193420935D91AE92C54BD',
    'login_type': '0',
    'login_uid': 'B9E287B234BC7E968605A501D9F867783E229D5544A22615F980912DD30D9469',
    'DUID': 'u=9600CB4E5FA7FCF7AAC0589C1C1AA657&v=0',
    'IsNonUser': 'F',
    'AHeadUserInfo': 'VipGrade=0&VipGradeName=%C6%D5%CD%A8%BB%E1%D4%B1&UserName=&NoReadMessageCount=0',
    '_udl': '708D70C2B179E2F91CC5ED1C2CCE362D',
    'ibulanguage': 'ZH-CN',
    'IBU_showtotalamt': '2',
    '_jzqco': '%7C%7C%7C%7C1783923138695%7C1.623881195.1783923138463.1783925604081.1783925612034.1783925604081.1783925612034.undefined.0.0.12.12',
    '_bfa': '1.1783923138329.26b82sgIuTIc.1.1783925604053.1783925611938.1.13.10650171192',
        # ... 省略其他，请替换为你最新的完整cookies
        # 提示：从curlconverter复制时，把cookies部分全部粘贴到这里
    },
    "headers": {
        'accept': 'application/json',
    'accept-language': 'zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6',
    'content-type': 'application/json',
    'cookieorigin': 'https://hotels.ctrip.com',
    'origin': 'https://hotels.ctrip.com',
    'phantom-token': '1006-common-Of6x5mJDQJ8ZW79xO3y8lvdMWg0JXdrhpw1bJ9cwnFv1tW0gJo9wzOwtbygHJb8iomj6Sy5nynfwp0j5fRcDwNzJ3pRbpjZ1ElNJDTw9zvGEHtjklEkYQ9jfpRNSyp0Rf6yzZjL0iSzvN9YMhJmdwcLR0TEfGE9XRTSJDprUpyggKgYnAjS1yN7JGkK3srMUjBtEHFyHE4siHZYLE3oYZ9ySEmGYTlytLK4kiFdjMgj10WZ7yGoI3DwX5jz0wTDepFEcUImawtQiFhRFEtkY5OyU0KfliGAjLbjbmWlZylEBlYMpykdWHOelAymTjmEpgi1NyGhvXnWsTj3ceP4wbPITUE5Lj5E9LiXAJ5lRZEZLYkdwd5wXseSgjMOIsSxNtyNE4LisnJz9iMGETEnAYdawsnEbfwBdiqDxH1eknYGSw5OjkOxtmytEaQi9QJl1WMESoYT1wnoy31e68yp5jTOwmQeLpwBZj9UxZoyZEZpiUmJSTWXEknYsmwMZwPciqzvBnYDkj18wQbITGxDdyDEO6i58J1lRsEmhYDZwz0wsgeUPjQNIDgWfFEH8RNojNE8XiqHJzEN6YbOJsGwpGwXETBYhcJLcv5DjbE71Y3pJXoj0TwFEQZYP9J1XwUAw9EGAY8kJqavdpj4Eq7YhoJ8sj9OwtEUpYcTw4tybLjo4y7fj0TW5lEDGRUojUEBoiFXJPEfhY0pJQnwlNwHE5AYplJSsvNojkE0UYUsJl6jcqwdEO4Y4GJ1bwfAwOEklYzQJp1vlDjsEhQY3ZJUfjG1w7EpkYfzwUFyGDjPqEBQwqQwDLKGmwSGIGdELsjLEfDihqJ5HEF0wAE8bYzQwsqildEN8j4ge9LiQMYcOxOsyOEm1iodJMqiTtEOElOYGzwm9wk9KOUjMkwNXrbUecGYcEb1iokJ8FY05Wp4Y3nwbXeAPWPfyqte6nR1qiDEmgYSdwTQwSUK7bjLdEASWFHJFYfJsgYHwtnR87yOcj35imqvLfYAQibsR50iXbj71is0Y0AvDQIpMY39wBTJbUjfaY8QylMxgfxBzv9awX1xtZwP0jU4r7dv7YamxZAYfqehDjs4JMdygOj7pjaMJ67iqAR38e0nRNDYbAv5BwPFeg0E46YBswT1vBzen4j5QRHTyX0ySGed5JU0EF1idOWSpjhOJDNv54ETtvdhwhZW3gvXaJhgr7Mi7Yptv1GvlHJhswMzyGBYs6wa9jGqwn5YsqwDAW4XrdayaY3Lj4GvSnyNLYqMEZFKNArMfjMYGXwPOJ3ovmBv8FezgY8LikNYFLJhdx0XjfY5Xi36eN4wTHe67ESTjMZWZniUfybfvbYkkxmgJoQIG0rgBKp0ecsE7cWmbIB7ITfjTYFGehSrNqEHBRQnycZWtZy3ke6mvPXwdSWzjQniMcxGYo4voEUoYasRTHytdWb8yo0e3AvbXRMnY8MWg9jQ9RzYXFrZ8jHmypUE63wNSEtAJFwOSrOY10yFdwlvX0YZliPQw46RfgE9LW45ETtigyBYAHebAW1bWcsJmgR0mITy9YHPjbHYSbWHojQfwOfvb7jqseQky5Ov5Yq0E0bJX0WozYbSvh4ra1vaY9lESpiaURQ3j9awzpvPGjPEq6WTojBYgDY5pv37w47ROUWq4WcQyDOxL9IbYS9R3kJfGJqOxUzIpydYpLRZOi1My3FRhmWFZwDZJ6aWmcRzAy4oRNmRHkvTGYqXE3gwGsy0Ya4wQXr9MwdkRsmR8dJfzjUnek8jTDY3Fj4hizhrlfilYZXeoUKNTKpoRSqy3fWN6y3tes5vQXR3NY85ePjUk',
    'priority': 'u=1, i',
    'referer': 'https://hotels.ctrip.com/',
    'sec-ch-ua': '"Not;A=Brand";v="8", "Chromium";v="150", "Microsoft Edge";v="150"',
    'sec-ch-ua-mobile': '?0',
    'sec-ch-ua-platform': '"Windows"',
    'sec-fetch-dest': 'empty',
    'sec-fetch-mode': 'cors',
    'sec-fetch-site': 'same-site',
    'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0',
    'w-payload-source': '1.0.9@102!mXdrh/VSFB4bOE9HKPAIOSVZKEb/O6FaKlNVOr4I9PqLK2qIG6NPKSkbOrK2+ET5+rApbbbpOSkS+r4/OlALOEKLbEkbKtb5+rbSKrb/OrApKEKLOSqZ+ETpObbpOSkS+r4SKrKRKS4IQl42GlknT2hjhFNjG5b=',
    'x-ctx-country': 'CN',
    'x-ctx-currency': 'CNY',
    'x-ctx-locale': 'zh-CN',
    'x-ctx-ubt-pageid': '10650171192',
    'x-ctx-ubt-pvid': '13',
    'x-ctx-ubt-sid': '1',
    'x-ctx-ubt-vid': '1783923138329.26b82sgIuTIc',
    'x-ctx-wclient-req': 'dc28fe66527f095e5fc3d635e7d3a409',
    # 'cookie': 'UBT_VID=1783923138329.26b82sgIuTIc; Hm_lvt_a8d6737197d542432f4ff4abc6e06384=1783923138; HMACCOUNT=65C317E04D6F090A; Session=smartlinkcode=U130727&smartlinklanguage=zh&SmartLinkKeyWord=&SmartLinkQuary=&SmartLinkHost=; Union=AllianceID=4902&SID=130727&OUID=&createtime=1783923138&Expires=1784527938409; MKT_CKID=1783923138460.scnhs.gpg0; _ga=GA1.1.951555538.1783923139; GUID=09031055110347578714; _RGUID=284915d1-48c0-46a0-9153-fe19ac209835; MKT_Pagesource=PC; manualclose=1; Hm_lpvt_a8d6737197d542432f4ff4abc6e06384=1783923153; ibulocale=zh_cn; cookiePricesDisplayed=CNY; _ga_5DVRDQD429=GS2.1.s1783923138$o1$g1$t1783923230$j60$l0$h560453016; _ga_B77BES1Z8Z=GS2.1.s1783923138$o1$g1$t1783923230$j60$l0$h0; _ga_9BZF483VNQ=GS2.1.s1783923138$o1$g1$t1783923230$j60$l0$h0; nfes_isSupportWebP=1; _abtest_userid=49056b4f-9d71-4366-9c78-0b0b84f12354; cticket=F310E13A04FC4C6D72AEC997B151774059F556B73FF193420935D91AE92C54BD; login_type=0; login_uid=B9E287B234BC7E968605A501D9F867783E229D5544A22615F980912DD30D9469; DUID=u=9600CB4E5FA7FCF7AAC0589C1C1AA657&v=0; IsNonUser=F; AHeadUserInfo=VipGrade=0&VipGradeName=%C6%D5%CD%A8%BB%E1%D4%B1&UserName=&NoReadMessageCount=0; _udl=708D70C2B179E2F91CC5ED1C2CCE362D; ibulanguage=ZH-CN; IBU_showtotalamt=2; _jzqco=%7C%7C%7C%7C1783923138695%7C1.623881195.1783923138463.1783925604081.1783925612034.1783925604081.1783925612034.undefined.0.0.12.12; _bfa=1.1783923138329.26b82sgIuTIc.1.1783925604053.1783925611938.1.13.10650171192',

    }
}
# ===========================================================

# 基础请求参数（不变的部分）
BASE_PAYLOAD = {
    "date": {"dateType": 1, "dateInfo": {"checkInDate": "20260713", "checkOutDate": "20260714"}},
    "extension": [],
    "destination": {"type": 1, "geo": {"cityId": 34, "countryId": 1}, "keyword": {"word": ""}},
    "extraFilter": {
        "childInfoItems": [],
        "ctripMainLandBDCoordinate": True,
        "sessionId": "a707e52d914141e59eb91cfbe293a0a4",
        "extendableParams": {"tripWalkDriveSwitch": "T", "isUgcSentenceB": ""},
    },
    "filters": [
        {"type": "17", "title": "欢迎度排序", "value": "1", "filterId": "17|1"},
        {"type": "80", "title": "", "value": "2", "filterId": "80|2"},
        {"filterId": "29|1", "type": "29", "value": "1|1"},
    ],
    "roomQuantity": 1,
    "marketInfo": {},
    "hotelIdFilter": {"hotelAldyShown": []},
    "head": {
        "platform": "PC",
        "cver": "0",
        "cid": "1783923138329.26b82sgIuTIc",
        "bu": "HBU",
        "group": "ctrip",
        "aid": "4902",
        "sid": "130727",
        "ouid": "",
        "locale": "zh-CN",
        "timezone": "8",
        "currency": "CNY",
        "pageId": "10650171192",
        "vid": "1783923138329.26b82sgIuTIc",
        "guid": "",
        "isSSR": False,
        "extension": [
            {"name": "cityId", "value": ""},
            {"name": "checkIn", "value": "2026-07-13"},
            {"name": "checkOut", "value": "2026-07-14"},
            {"name": "region", "value": "CN"},
        ],
    },
}

def fetch_hotel_list(page_index=1, max_retries=3):
    """带重试的请求函数"""
    url = "https://m.ctrip.com/restapi/soa2/34951/fetchHotelList"
    payload = BASE_PAYLOAD.copy()
    payload["paging"] = {"pageIndex": page_index, "pageSize": 20, "pageCode": "10650171192"}

    for attempt in range(1, max_retries + 1):
        try:
            resp = requests.post(url, cookies=CONFIG["cookies"], headers=CONFIG["headers"], json=payload, timeout=15)
            if resp.status_code != 200:
                print(f"  第{attempt}次请求状态码 {resp.status_code}，重试...")
                time.sleep(2)
                continue
            data = resp.json()
            if data.get("ResponseStatus", {}).get("Ack") == "Success":
                return data
            else:
                print(f"  第{attempt}次请求返回失败: {data.get('ResponseStatus', {}).get('Errors', '未知错误')}")
                time.sleep(2)
        except Exception as e:
            print(f"  第{attempt}次请求异常: {e}")
            time.sleep(2)
    return None

def parse_hotel_info(data):
    """解析酒店数据（支持多房型展开）"""
    hotel_list = data.get("data", {}).get("hotelList", [])
    rows = []
    for hotel in hotel_list:
        hotel_info = hotel.get("hotelInfo", {})
        # 基础信息
        name = hotel_info.get("nameInfo", {}).get("name", "")
        hotel_id = hotel_info.get("summary", {}).get("hotelId", "")
        star = hotel_info.get("hotelStar", {}).get("star", 0)
        comment_info = hotel_info.get("commentInfo", {})
        score = comment_info.get("commentScore", "")
        comment_num = comment_info.get("commenterNumber", "")
        comment_desc = comment_info.get("commentDescription", "")
        images = hotel_info.get("hotelImages", {}).get("multiImgs", [])
        cover = images[0].get("url", "") if images else ""
        address = hotel_info.get("positionInfo", {}).get("address", "")

        # 房型列表（每个房型单独一行）
        room_list = hotel.get("roomInfo", [])
        if not room_list:
            # 没有房型信息，用一行空数据占位
            rows.append({
                "酒店名": name, "星级": star, "酒店ID": hotel_id,
                "评分": score, "点评数": comment_num, "评语": comment_desc,
                "价格": "", "原价": "", "房型名": "暂无房型", "房型描述": "",
                "封面图": cover, "地址": address
            })
        else:
            for room in room_list:
                price_info = room.get("priceInfo", {})
                display_price = price_info.get("displayPrice", "")
                delete_price = price_info.get("deleteDisplayPrice", "")
                room_name = room.get("summary", {}).get("saleRoomName", "")
                bed_info = room.get("bedInfo", {})
                bed_content = "; ".join(bed_info.get("contentList", [])) if bed_info.get("contentList") else ""
                room_desc = f"{room_name} {bed_content}".strip()
                # 额外标签（如免费取消、早餐）
                tags = room.get("roomTags", {})
                advantage = "; ".join([t.get("tagTitle", "") for t in tags.get("advantageTags", [])])
                room_desc = f"{room_desc} [{advantage}]" if advantage else room_desc

                rows.append({
                    "酒店名": name,
                    "星级": star,
                    "酒店ID": hotel_id,
                    "评分": score,
                    "点评数": comment_num,
                    "评语": comment_desc,
                    "价格": display_price,
                    "原价": delete_price,
                    "房型名": room_name,
                    "房型描述": room_desc,
                    "封面图": cover,
                    "地址": address,
                })
    return rows

def save_to_csv(rows, filename="kunming_hotels_expanded.csv"):
    """保存为CSV，自动添加时间戳避免覆盖"""
    if not rows:
        print("没有数据可保存")
        return
    # 添加时间戳
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"kunming_hotels_{timestamp}.csv"
    fieldnames = ["酒店名", "星级", "酒店ID", "评分", "点评数", "评语", "价格", "原价", "房型名", "房型描述", "封面图", "地址"]
    with open(filename, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    print(f"✅ 保存成功: {filename} (共{len(rows)}行)")

def main():
    print("🚀 开始采集昆明酒店数据 (多房型展开版)")
    all_rows = []
    page = 1
    max_pages = 30  # 安全上限
    while page <= max_pages:
        print(f"\n📄 正在请求第 {page} 页...")
        data = fetch_hotel_list(page)
        if not data:
            print("❌ 请求失败，停止爬取")
            break

        rows = parse_hotel_info(data)
        if not rows:
            print("🏁 当前页无数据，停止分页")
            break

        all_rows.extend(rows)
        print(f"✅ 第 {page} 页提取 {len(rows)} 个房型记录")

        # 检查是否最后一页
        paging = data.get("data", {}).get("pagingInfo", {})
        is_last = paging.get("isLastPage", False)
        if is_last:
            print("📌 已到达最后一页")
            break

        page += 1
        time.sleep(2)  # 延时

    if all_rows:
        save_to_csv(all_rows)
        print(f"\n🎉 总计提取 {len(all_rows)} 条房型记录")
    else:
        print("😞 未获取到任何数据，请检查 Cookie 是否过期")

if __name__ == "__main__":
    main()
with open('kunming_hotels_20260713_145731.csv', 'r', encoding='utf-8-sig') as f:
    lines = f.readlines()
    print(len(lines))