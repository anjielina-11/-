import csv
import re


class CtripHotelPipeline:
    def open_spider(self, spider):
        self.file = open('yunnan_university_hotels.csv', 'w', newline='', encoding='utf-8-sig')
        self.writer = csv.writer(self.file)
        self.writer.writerow([
            'hotel_id', 'name', 'star', 'rating',
            'review_count', 'price', 'address'
        ])

    def close_spider(self, spider):
        self.file.close()

    def process_item(self, item, spider):
        # 清洗价格 - 提取数字
        if item.get('price'):
            match = re.search(r'[\d,]+(?:\.\d+)?', item['price'])
            if match:
                item['price'] = match.group(0).replace(',', '')

        # 清洗评论数（如 16,720条点评 → 16720）
        if item.get('review_count'):
            text = item['review_count']
            match = re.search(r'[\d,]+', text)
            if match:
                item['review_count'] = match.group(0).replace(',', '')

        # 清洗评分
        if item.get('rating'):
            match = re.search(r'[\d.]+', item['rating'])
            if match:
                item['rating'] = match.group(0)

        self.writer.writerow([
            item.get('hotel_id', ''),
            item.get('name', ''),
            item.get('star', ''),
            item.get('rating', ''),
            item.get('review_count', ''),
            item.get('price', ''),
            item.get('address', ''),
        ])
        return item
