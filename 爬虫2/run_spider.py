# -*- coding: utf-8 -*-
r"""
携程酒店爬虫 - 启动脚本
=======================
用法:
    python run_spider.py           # 仅爬取数据
    python run_spider.py --full    # 爬取数据 + 数据分析

说明:
  - 该脚本启动 Scrapy 爬虫, 采集云南大学(呈贡校区)周边酒店信息
  - 使用 Selenium + Chrome 无头浏览器加载页面
  - Cookie 注入绕过携程反爬验证 (max-age=0, 会话Cookie)
  - 结果导出到 output/hotels_near_ynu.csv
  - 可选运行数据分析脚本 output/hotels_cleaned.csv
"""

import os
import sys
import subprocess


def run_spider():
    """运行 Scrapy 爬虫."""
    base_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(base_dir, "output")
    os.makedirs(output_dir, exist_ok=True)

    print("=" * 70)
    print("  携程酒店爬虫 - 云南大学(呈贡校区)周边酒店信息采集")
    print("=" * 70)
    print(f"  工作目录: {base_dir}")
    print(f"  输出目录: {output_dir}")
    print(f"  框架: Scrapy + Selenium (Chrome 无头浏览器)")
    print(f"  认证: Cookie 注入 (max-age=0 会话Cookie)")
    print("=" * 70)
    print()

    cmd = [
        sys.executable, "-m", "scrapy", "crawl", "ctrip_hotel",
        "-s", "LOG_LEVEL=INFO",
    ]

    print("正在启动爬虫...")
    print(f"命令: {' '.join(cmd)}")
    print()

    try:
        result = subprocess.run(cmd, cwd=base_dir)
        print()
        print("=" * 70)
        if result.returncode == 0:
            csv_path = os.path.join(output_dir, "hotels_near_ynu.csv")
            if os.path.exists(csv_path):
                with open(csv_path, "r", encoding="utf-8-sig") as f:
                    lines = f.readlines()
                hotel_count = max(0, len(lines) - 1)
                print(f"  OK 爬取完成! 共采集 {hotel_count} 条酒店数据")
                print(f"  输出文件: output/hotels_near_ynu.csv")
            else:
                print("  WARNING 爬虫运行完成但未生成CSV文件")
                print("  请检查 output/spider.log 查看日志")
        else:
            print(f"  ERROR 爬取过程中出现错误 (exit code: {result.returncode})")
            print("  请检查 output/spider.log 查看详细日志")
        print("=" * 70)
    except KeyboardInterrupt:
        print()
        print("用户中断爬取")
    except Exception as e:
        print(f"运行出错: {e}")
        import traceback
        traceback.print_exc()


def run_analysis():
    """运行数据分析脚本."""
    print()
    print("=" * 70)
    print("  正在运行数据分析...")
    print("=" * 70)

    base_dir = os.path.dirname(os.path.abspath(__file__))
    analysis_script = os.path.join(base_dir, "analyze_hotel_data.py")

    try:
        result = subprocess.run([sys.executable, analysis_script], cwd=base_dir)
        if result.returncode == 0:
            print()
            print("  OK 数据分析完成!")
            print("  清洗后数据: output/hotels_cleaned.csv")
        else:
            print("  WARNING 数据分析时出现警告, 请检查输出")
    except Exception as e:
        print(f"数据分析出错: {e}")


def main():
    full_mode = "--full" in sys.argv or "--analyze" in sys.argv
    run_spider()
    if full_mode:
        run_analysis()
    print()
    print("所有任务完成.")


if __name__ == "__main__":
    main()