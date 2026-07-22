# -*- coding: utf-8 -*-
"""
酒店数据分析与清洗脚本
======================
功能:
  1. 读取 output/hotels_near_ynu.csv 中的原始爬取数据
  2. 进行数据清洗: 提取价格数值、评分数值、距离公里数
  3. 生成统计分析报告 (价格分布、评分分布、星级分布、性价比排名)
  4. 导出清洗后的结构化CSV: output/hotels_cleaned.csv

用法:
    python analyze_hotel_data.py

依赖:
    需要先运行爬虫 python run_spider.py 生成 hotels_near_ynu.csv
"""

import csv
import re
import os
import sys
from collections import Counter


# 设置Windows控制台支持UTF-8输出
if sys.platform == "win32":
    try:
        import io
        sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
        sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")
    except Exception:
        pass


# ================================================================
# 配置
# ================================================================

RAW_CSV = os.path.join("output", "hotels_near_ynu.csv")
CLEANED_CSV = os.path.join("output", "hotels_cleaned.csv")


class HotelDataAnalyzer:
    """酒店数据分析器: 读取原始CSV, 清洗数据, 生成统计报告, 导出清洗后CSV."""

    def __init__(self, csv_path):
        self.csv_path = csv_path
        self.hotels = []  # 原始酒店数据列表

    # ================================================================
    # 数据加载
    # ================================================================

    def load(self):
        """加载CSV文件中的酒店数据."""
        if not os.path.exists(self.csv_path):
            print(f"[错误] 找不到文件: {self.csv_path}")
            print("请先运行: python run_spider.py 来采集酒店数据")
            return False

        with open(self.csv_path, "r", encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            headers = reader.fieldnames
            print(f"[加载] CSV列名: {headers}")

            for row in reader:
                self.hotels.append(row)

        print(f"[加载] 共读取 {len(self.hotels)} 条酒店记录")
        return True

    # ================================================================
    # 数据清洗辅助方法
    # ================================================================

    @staticmethod
    def extract_price_value(price_str):
        """
        从价格字符串中提取数值.
        支持格式: "¥299", "299", "¥1,234", "N/A"
        返回: int/float 或 None
        """
        if not price_str or price_str.strip() == "N/A":
            return None
        cleaned = str(price_str).replace("¥", "").replace(",", "").replace(" ", "").strip()
        match = re.search(r"(\d+(?:\.\d+)?)", cleaned)
        if match:
            try:
                val = float(match.group(1))
                return int(val) if val == int(val) else val
            except ValueError:
                return None
        return None

    @staticmethod
    def extract_rating_score(rating_str):
        """
        从评分字符串中提取评分值.
        支持格式: "评分4.6 / 1234条点评", "4.6", "N/A"
        返回: float 或 None
        """
        if not rating_str or rating_str.strip() == "N/A":
            return None
        match = re.search(r"(\d+\.?\d*)", str(rating_str))
        if match:
            try:
                return float(match.group(1))
            except ValueError:
                return None
        return None

    @staticmethod
    def extract_comment_count(rating_str):
        """
        从评分字符串中提取点评数.
        支持格式: "评分4.6 / 1234条点评", "1234条点评"
        返回: int 或 None
        """
        if not rating_str or rating_str.strip() == "N/A":
            return None
        match = re.search(r"(\d+)\s*条点评", str(rating_str))
        if match:
            try:
                return int(match.group(1))
            except ValueError:
                return None
        return None

    @staticmethod
    def extract_distance_km(address_str):
        """
        从地址字符串中提取距云南大学的距离(公里).
        支持格式: "xxx路xx号（距离云南大学1.2公里）", "距离云南大学500米"
        返回: float (公里) 或 None
        """
        if not address_str or address_str == "N/A":
            return None

        text = str(address_str)

        # 匹配: X.X公里 / X.Xkm
        km_match = re.search(r"([\d,.]+)\s*(?:公里|km|KM)", text)
        if km_match:
            try:
                return float(km_match.group(1).replace(",", ""))
            except ValueError:
                return None

        # 匹配: XXX米 / XXXm (转换为公里)
        m_match = re.search(r"([\d,]+)\s*(?:米|m|M)(?!.*(?:公里|km))", text)
        if m_match:
            try:
                meters = float(m_match.group(1).replace(",", ""))
                return round(meters / 1000, 2)
            except ValueError:
                return None

        return None

    @staticmethod
    def classify_star(star_str):
        """
        标准化星级分类.
        输入: "五星级", "四星级", "三星级", "二星级及以下", "N/A" 等
        输出: 标准化星级字符串
        """
        if not star_str or star_str == "N/A":
            return "未知"

        s = str(star_str).strip()
        # 统一映射
        star_map = {
            "五星级": "五星级",
            "五星": "五星级",
            "5星": "五星级",
            "四星级": "四星级",
            "四星": "四星级",
            "4星": "四星级",
            "三星级": "三星级",
            "三星": "三星级",
            "3星": "三星级",
            "二星级": "二星级及以下",
            "二星": "二星级及以下",
            "2星": "二星级及以下",
            "一星级": "二星级及以下",
            "一星": "二星级及以下",
            "1星": "二星级及以下",
            "经济型": "经济型",
            "舒适型": "舒适型",
            "舒适": "舒适型",
            "高档型": "高档型",
            "高档": "高档型",
            "豪华型": "豪华型",
            "豪华": "豪华型",
        }
        for key, val in star_map.items():
            if key in s:
                return val
        return s  # 保持原样

    # ================================================================
    # 数据分析与报告
    # ================================================================

    def analyze(self):
        """执行完整数据分析并打印报告."""
        if not self.hotels:
            print("[分析] 没有数据可供分析")
            return None

        n = len(self.hotels)

        print()
        print("=" * 80)
        print("                    云南大学(呈贡校区)周边酒店数据统计分析报告")
        print("=" * 80)

        # --- 一、基本统计 ---
        print(f"\n{' 一、基本统计 ':=^60}")
        print(f"  酒店总数: {n}")

        n_price = sum(1 for h in self.hotels if h.get("price", "N/A") != "N/A")
        n_rating = sum(1 for h in self.hotels if h.get("rating", "N/A") != "N/A")
        n_star = sum(1 for h in self.hotels if h.get("star_level", "N/A") != "N/A")
        print(f"  有价格信息: {n_price} ({n_price/n*100:.1f}%)")
        print(f"  有评分信息: {n_rating} ({n_rating/n*100:.1f}%)")
        print(f"  有星级信息: {n_star} ({n_star/n*100:.1f}%)")

        # --- 二、价格分析 ---
        print(f"\n{' 二、价格分析 ':=^60}")

        prices = []
        for h in self.hotels:
            p = self.extract_price_value(h.get("price", ""))
            if p is not None:
                prices.append(p)

        if prices:
            prices_sorted = sorted(prices)
            print(f"  有效价格数据: {len(prices)} 条")
            print(f"  最低价格: ¥{min(prices):.0f}")
            print(f"  最高价格: ¥{max(prices):.0f}")
            print(f"  平均价格: ¥{sum(prices)/len(prices):.0f}")

            # 中位数
            mid = len(prices_sorted) // 2
            if len(prices_sorted) % 2 == 1:
                median = prices_sorted[mid]
            else:
                median = (prices_sorted[mid - 1] + prices_sorted[mid]) / 2
            print(f"  中位数价格: ¥{median:.0f}")

            # 价格区间分布
            print(f"\n  价格区间分布:")
            ranges = [
                (0, 100, "¥0-100 (经济型)"),
                (100, 200, "¥100-200 (舒适型)"),
                (200, 350, "¥200-350 (高档型)"),
                (350, 500, "¥350-500 (豪华型)"),
                (500, float("inf"), "¥500+ (奢华型)"),
            ]
            for lo, hi, label in ranges:
                cnt = sum(1 for p in prices if lo <= p < hi)
                if cnt > 0:
                    bar = "█" * (cnt * 40 // len(prices))
                    print(f"    {label:<24} {cnt:>3} 家 ({cnt/len(prices)*100:5.1f}%) {bar}")
        else:
            print("  无有效价格数据")

        # --- 三、评分分析 ---
        print(f"\n{' 三、评分分析 ':=^60}")

        ratings = []
        for h in self.hotels:
            r = self.extract_rating_score(h.get("rating", ""))
            if r is not None:
                ratings.append(r)

        if ratings:
            print(f"  有效评分数据: {len(ratings)} 条")
            print(f"  最高评分: {max(ratings):.1f}")
            print(f"  最低评分: {min(ratings):.1f}")
            print(f"  平均评分: {sum(ratings)/len(ratings):.1f}")

            # 评分分布
            print(f"\n  评分分布:")
            rating_ranges = [
                (0, 3.5, "3.5分以下"),
                (3.5, 4.0, "3.5-4.0分"),
                (4.0, 4.5, "4.0-4.5分"),
                (4.5, 4.8, "4.5-4.8分"),
                (4.8, 5.1, "4.8分以上 (优质)"),
            ]
            for lo, hi, label in rating_ranges:
                cnt = sum(1 for r in ratings if lo <= r < hi)
                if cnt > 0:
                    bar = "█" * (cnt * 40 // len(ratings))
                    print(f"    {label:<20} {cnt:>3} 家 ({cnt/len(ratings)*100:5.1f}%) {bar}")
        else:
            print("  无有效评分数据")

        # --- 四、星级分布 ---
        print(f"\n{' 四、星级/档次分布 ':=^60}")

        star_counts = Counter()
        for h in self.hotels:
            star = self.classify_star(h.get("star_level", "N/A"))
            star_counts[star] += 1

        for star, cnt in star_counts.most_common():
            bar = "█" * (cnt * 40 // n)
            print(f"  {star:<14} {cnt:>3} 家 ({cnt/n*100:5.1f}%) {bar}")

        # --- 五、性价比排名 (评分高且价格低) ---
        print(f"\n{' 五、性价比 TOP 10 (高评分 低价格) ':=^60}")

        value_list = []
        for h in self.hotels:
            r = self.extract_rating_score(h.get("rating", ""))
            p = self.extract_price_value(h.get("price", ""))
            if r is not None and p is not None and p > 0:
                score = r / p * 100  # 性价比指数: 分/元
                value_list.append((h, score))

        value_list.sort(key=lambda x: x[1], reverse=True)

        if value_list:
            print(f"  {'排名':<5} {'酒店名称':<28} {'价格':<10} {'评分':<6} {'性价比':<8}")
            print(f"  {'-'*58}")
            for i, (hotel, val) in enumerate(value_list[:10], 1):
                name = hotel.get("hotel_name", "")[:27]
                price = hotel.get("price", "")
                rating = self.extract_rating_score(hotel.get("rating", ""))
                rating_str = f"{rating:.1f}" if rating else "N/A"
                print(f"  {i:<5} {name:<28} {str(price):<10} {rating_str:<6} {val:.1f}")
        else:
            print("  无足够数据计算性价比")

        # --- 六、距离分析 ---
        print(f"\n{' 六、距离分析 ':=^60}")

        distances = []
        for h in self.hotels:
            d = self.extract_distance_km(h.get("address", ""))
            if d is not None:
                distances.append(d)

        if distances:
            print(f"  可计算距离的酒店: {len(distances)} 家")
            print(f"  最近距离: {min(distances):.2f} 公里")
            print(f"  最远距离: {max(distances):.2f} 公里")
            print(f"  平均距离: {sum(distances)/len(distances):.2f} 公里")

            # 距离分布
            dist_ranges = [
                (0, 1, "1公里以内"),
                (1, 3, "1-3公里"),
                (3, 5, "3-5公里"),
                (5, 10, "5-10公里"),
                (10, float("inf"), "10公里以上"),
            ]
            print(f"\n  距离分布:")
            for lo, hi, label in dist_ranges:
                cnt = sum(1 for d in distances if lo <= d < hi)
                if cnt > 0:
                    bar = "█" * (cnt * 40 // len(distances))
                    print(f"    {label:<14} {cnt:>3} 家 ({cnt/len(distances)*100:5.1f}%) {bar}")
        else:
            print("  无距离数据可分析")

        print("=" * 80)

        # --- 导出清洗后数据 ---
        cleaned_path = self._export_cleaned()
        return cleaned_path

    # ================================================================
    # 导出清洗后的CSV
    # ================================================================

    def _export_cleaned(self):
        """
        导出清洗后的数据到 output/hotels_cleaned.csv.
        新增列:
          - price_value   : 价格数值
          - rating_score  : 评分数值
          - comment_count : 点评数
          - distance_km   : 距云南大学距离(公里)
          - star_category : 标准化星级分类
        """
        fieldnames = [
            "hotel_name",
            "address",
            "rating",
            "rating_score",
            "comment_count",
            "price",
            "price_value",
            "distance_km",
            "star_level",
            "star_category",
            "detail_url",
        ]

        with open(CLEANED_CSV, "w", encoding="utf-8-sig", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()

            for h in self.hotels:
                row = {
                    "hotel_name": h.get("hotel_name", ""),
                    "address": h.get("address", ""),
                    "rating": h.get("rating", ""),
                    "rating_score": self.extract_rating_score(h.get("rating", "")),
                    "comment_count": self.extract_comment_count(h.get("rating", "")),
                    "price": h.get("price", ""),
                    "price_value": self.extract_price_value(h.get("price", "")),
                    "distance_km": self.extract_distance_km(h.get("address", "")),
                    "star_level": h.get("star_level", ""),
                    "star_category": self.classify_star(h.get("star_level", "")),
                    "detail_url": h.get("detail_url", ""),
                }
                writer.writerow(row)

        print(f"\n[导出] 清洗后数据已保存到: {CLEANED_CSV}")
        print(f"[导出] 字段: {', '.join(fieldnames)}")
        return CLEANED_CSV


# ================================================================
# 主入口
# ================================================================

def main():
    analyzer = HotelDataAnalyzer(RAW_CSV)

    if not analyzer.load():
        print("\n分析失败: 无法加载数据文件")
        print("请先运行: python run_spider.py 来采集酒店数据")
        sys.exit(1)

    cleaned_file = analyzer.analyze()

    if cleaned_file:
        print(f"\n✅ 数据分析完毕!")
        print(f"  原始数据: {RAW_CSV}")
        print(f"  清洗数据: {cleaned_file}")
    else:
        print("\n⚠️ 分析完成但未生成清洗文件")


if __name__ == "__main__":
    main()