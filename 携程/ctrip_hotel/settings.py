BOT_NAME = "ctrip_hotel"

SPIDER_MODULES = ["ctrip_hotel.spiders"]
NEWSPIDER_MODULE = "ctrip_hotel.spiders"

# 启用 Selenium 中间件
DOWNLOADER_MIDDLEWARES = {
    "ctrip_hotel.middlewares.SeleniumMiddleware": 543,
}

# 启用 Pipeline
ITEM_PIPELINES = {
    "ctrip_hotel.pipelines.CtripHotelPipeline": 300,
}

# ============================================================
# Cookie 配置说明
# ============================================================
# Cookie 是跳过登录的关键，但会过期（通常 1-7 天）
# 如果爬虫获取到的是上海酒店而非昆明酒店，或页面重定向到登录页
# 说明 Cookie 已过期，需要重新从浏览器复制
#
# 获取方法：
# 1. 用浏览器手动登录 https://hotels.ctrip.com
# 2. 搜索"云南大学(呈贡校区)"
# 3. F12 → Application → Cookies → 复制所有 Cookie
# 4. 粘贴到下方 COOKIE_STRING（以 ; 分隔）
# ============================================================

# 1. Cookie 字符串（从浏览器复制，以 ; 分隔）
COOKIE_STRING = "UBT_VID=1783923138329.26b82sgIuTIc; MKT_CKID=1783923138460.scnhs.gpg0; _ga=GA1.1.951555538.1783923139; GUID=09031055110347578714; _RGUID=284915d1-48c0-46a0-9153-fe19ac209835; MKT_Pagesource=PC; nfes_isSupportWebP=1; ibu_country=CN; ibulocale=zh_cn; cookiePricesDisplayed=CNY; oldCurrency=CNY; nfes_isSupportWebP=1; _abtest_userid=49056b4f-9d71-4366-9c78-0b0b84f12354; login_type=0; login_uid=B9E287B234BC7E968605A501D9F867783E229D5544A22615F980912DD30D9469; DUID=u=9600CB4E5FA7FCF7AAC0589C1C1AA657&v=0; IsNonUser=F; AHeadUserInfo=VipGrade=0&VipGradeName=%C6%D5%CD%A8%BB%E1%D4%B1&UserName=&NoReadMessageCount=0; _udl=708D70C2B179E2F91CC5ED1C2CCE362D; w_lid=016a54835fc1c516afa3; IBU_showtotalamt=2; intl_ht1=h4%3D34_132637897%2C34_134129881; cticket=F310E13A04FC4C6D72AEC997B1517740D0B66E8C3A4DC6A8FDECA241DB153EC5; ibu_hotel_search_date=%7B%22checkIn%22%3A%222026-07-14%22%2C%22checkOut%22%3A%222026-07-15%22%2C%22isChoseFlexible%22%3Afalse%2C%22flexibleDate%22%3A%7B%22selectNight%22%3A0%7D%2C%22dayFlexibility%22%3A0%7D; ibu_hotel_search_target=%7B%22countryId%22%3A1%2C%22provinceId%22%3A25%2C%22searchWord%22%3A%22%E4%BA%91%E5%8D%97%E5%A4%A7%E5%AD%A6(%E5%91%88%E8%B4%A1%E6%A0%A1%E5%8C%BA)%22%2C%22cityId%22%3A34%2C%22searchType%22%3A%22%22%2C%22searchValue%22%3A%22%22%2C%22cityName%22%3A%22%E6%98%86%E6%98%8E%22%7D; ibu_hotel_search_crn_guest=%7B%22adult%22%3A1%2C%22children%22%3A0%2C%22ages%22%3A%22%22%2C%22crn%22%3A1%7D; ibulanguage=ZH-CN; w_tuid=cj/dfpPQi9HKb/3z88WZHjEm+OCl14grbBtDg7nBmyoFZM9eZAF5iO5LtoovdoAJA22Hol42mSrtXN4F80wiHPhI0ZRJi5Mt8L0a1CBzdj7jJL+KOfQRnOXPxw34ziZ3AJBxHM5z74Uv5QEx/53t3IkAxvy2zCkMiLiq2Z/Vrq551GybasVZ5G+XRvy+AW6wQA==:1_1_1_1.P0wx1owPuU5HpDbGoAmFG8D2owybyAvcYgHPUMjOtqA=; Hm_lvt_a8d6737197d542432f4ff4abc6e06384=1783923138,1784009365,1784021736; HMACCOUNT=65C317E04D6F090A; Session=smartlinkcode=U130727&smartlinklanguage=zh&SmartLinkKeyWord=&SmartLinkQuary=&SmartLinkHost=; Union=AllianceID=4902&SID=130727&OUID=&createtime=1784021756&Expires=1784626555937; Hm_lpvt_a8d6737197d542432f4ff4abc6e06384=1784021756; ibusite=CN; ibugroup=ctrip; _ga_5DVRDQD429=GS2.1.s1784021755$o4$g0$t1784021786$j29$l0$h1381947125; _ga_B77BES1Z8Z=GS2.1.s1784021755$o4$g0$t1784021786$j29$l0$h0; _ga_9BZF483VNQ=GS2.1.s1784021756$o4$g0$t1784021786$j30$l0$h0; Hm_lvt_4a51227696a44e11b0c61f6105dc4ee4=1783923231,1784010847,1784021786; Hm_lpvt_4a51227696a44e11b0c61f6105dc4ee4=1784021786; _bfa=1.1783923138329.26b82sgIuTIc.1.1784021755737.1784021786524.6.3.10650171192; _jzqco=%7C%7C%7C%7C1784010801608%7C1.623881195.1783923138463.1784021756004.1784021787167.1784021756004.1784021787167.undefined.0.0.31.31"

# 其他设置
USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
ROBOTSTXT_OBEY = False
DOWNLOAD_DELAY = 3