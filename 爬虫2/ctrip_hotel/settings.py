# -*- coding: utf-8 -*-
"""
Scrapy 设置 - 携程酒店爬虫
==========================
基于 Scrapy + Selenium 框架, Cookie绕过验证.
"""

# 爬虫名称
BOT_NAME = "ctrip_hotel"

# 爬虫模块
SPIDER_MODULES = ["ctrip_hotel.spiders"]
NEWSPIDER_MODULE = "ctrip_hotel.spiders"

# ================================================================
# 爬取规则
# ================================================================

# 不遵守 robots.txt (反爬策略需要)
ROBOTSTXT_OBEY = False

# 并发请求数 (Selenium模式下设为1避免冲突)
CONCURRENT_REQUESTS = 1
CONCURRENT_REQUESTS_PER_DOMAIN = 1

# 下载延迟 (秒)
DOWNLOAD_DELAY = 0

# ================================================================
# Cookie 设置
# ================================================================

# 启用Cookie (由中间件注入)
COOKIES_ENABLED = True

# Cookie调试
COOKIES_DEBUG = False

# ================================================================
# 请求头
# ================================================================

DEFAULT_REQUEST_HEADERS = {
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
    "Accept-Encoding": "gzip, deflate, br",
    "Cache-Control": "max-age=0",
    "Connection": "keep-alive",
    "Upgrade-Insecure-Requests": "1",
}

# ================================================================
# 下载器中间件
# ================================================================

DOWNLOADER_MIDDLEWARES = {
    # Cookie注入中间件 (优先级100, 先执行)
    "ctrip_hotel.middlewares.CtripCookieMiddleware": 100,
    # Selenium渲染中间件 (优先级200, 后执行)
    "ctrip_hotel.middlewares.CtripSeleniumMiddleware": 200,
    # 禁用默认的Cookie中间件
    "scrapy.downloadermiddlewares.cookies.CookiesMiddleware": None,
}

# ================================================================
# 自动限速 (AutoThrottle)
# ================================================================

AUTOTHROTTLE_ENABLED = False  # Selenium模式下不需要

# ================================================================
# 重试设置
# ================================================================

RETRY_ENABLED = False  # Selenium请求失败不重试
RETRY_TIMES = 1
DOWNLOAD_TIMEOUT = 60  # Selenium渲染需要时间, 设置较长超时

# ================================================================
# 日志
# ================================================================

LOG_LEVEL = "INFO"
LOG_FILE = "output/spider.log"
LOG_STDOUT = False

# ================================================================
# Telnet
# ================================================================

TELNETCONSOLE_ENABLED = False

# ================================================================
# Pipeline (使用FEEDS导出, 不需要自定义Pipeline)
# ================================================================

ITEM_PIPELINES = {}

# ================================================================
# FEEDS - CSV 导出配置
# ================================================================

FEEDS = {
    "output/hotels_near_ynu.csv": {
        "format": "csv",
        "encoding": "utf-8-sig",
        "overwrite": True,
        "store_empty": False,
        "fields": [
            "hotel_name",
            "address",
            "rating",
            "price",
            "star_level",
            "detail_url",
        ],
    },
}

# CSV导出编码
FEED_EXPORT_ENCODING = "utf-8-sig"

# ================================================================
# 其他设置
# ================================================================

# 禁用不需要的扩展以提高性能
EXTENSIONS = {
    "scrapy.extensions.telnet.TelnetConsole": None,
    "scrapy.extensions.logstats.LogStats": None,
}

# 内存使用限制
MEMUSAGE_ENABLED = True
MEMUSAGE_LIMIT_MB = 1024