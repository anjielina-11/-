import scrapy

class CtripHotelItem(scrapy.Item):
    hotel_id = scrapy.Field()
    name = scrapy.Field()
    star = scrapy.Field()
    rating = scrapy.Field()
    review_count = scrapy.Field()
    price = scrapy.Field()
    address = scrapy.Field()