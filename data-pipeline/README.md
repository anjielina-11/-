# 数据管道

## 功能
1. 天气数据定时采集（省级气象 API → 数据库）
2. 市场价格定时采集（农产品批发市场 API → 数据库）
3. 农业通报爬取（政策文件、病虫害预警）
4. 知识文档预处理和向量化（文档 → embedding → pgvector）
5. 模型训练数据预处理

## 技术栈
- Python + SQLAlchemy / psycopg2
- APScheduler / Celery Beat（定时任务）
- requests / httpx（API 调用）
- BeautifulSoup / lxml（网页解析）
- LangChain document loaders

## 目录结构
```
data-pipeline/
├── collectors/      # 数据采集脚本
│   ├── weather.py
│   ├── market_price.py
│   └── agriculture_news.py
├── processors/      # 数据处理脚本
│   ├── embedding.py
│   └── data_clean.py
└── README.md
```

## 启动
```bash
cd data-pipeline
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python -m collectors.weather  # 单独运行
# 或通过 Celery Beat 定时调度
```
