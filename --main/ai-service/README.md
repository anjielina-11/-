# AI 服务 (Python + FastAPI)

## 技术栈
- Python 3.11 + FastAPI
- LangChain / LlamaIndex
- pgvector (向量检索)
- Redis + Celery (异步任务队列)
- OpenCV / Pillow (图像预处理)

## 功能模块
1. 病害图像识别（分类模型推理）
2. RAG 检索增强生成（农技规范知识库）
3. 多 Agent 协作（诊断 + 防治建议 + 农事推荐）
4. 模型版本管理和性能监控
5. 未知病害拒识和人工审核触发
6. 天气数据定时采集
7. 市场价格定时采集

## 模型
- 图像分类：ResNet / EfficientNet / ViT
- RAG Embedding: text2vec / bge-large-zh
- LLM: 通过 API 调用或本地部署

## 启动
```bash
cd ai-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn src.main:app --reload --port 8000
```
