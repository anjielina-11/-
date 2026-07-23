import os
import ssl
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import DirectoryLoader, TextLoader, PyPDFLoader, Docx2txtLoader
from langchain_community.vectorstores import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from ..core.config import settings

ssl._create_default_https_context = ssl._create_unverified_context


class RAGService:
    _vector_store = None
    _embeddings = None

    @classmethod
    def _find_local_model_cache(cls, model_name: str) -> str:
        cache_dir = os.path.join(os.path.expanduser("~"), ".cache", "huggingface", "hub")
        if not os.path.exists(cache_dir):
            return None
        
        possible_names = [model_name]
        
        if "/" not in model_name:
            possible_names.append(f"sentence-transformers/{model_name}")
        
        for name in possible_names:
            model_cache_dir = f"models--{name.replace('/', '--')}"
            full_path = os.path.join(cache_dir, model_cache_dir)
            if os.path.exists(full_path):
                snapshots_dir = os.path.join(full_path, "snapshots")
                if os.path.exists(snapshots_dir):
                    snapshots = [d for d in os.listdir(snapshots_dir) if os.path.isdir(os.path.join(snapshots_dir, d))]
                    if snapshots:
                        return os.path.join(snapshots_dir, snapshots[0])
        
        return None

    @classmethod
    def _get_embeddings(cls):
        if cls._embeddings is None:
            model_name = settings.RAG_EMBEDDING_MODEL
            
            local_cache = cls._find_local_model_cache(model_name)
            if local_cache and os.path.exists(local_cache):
                model_path = local_cache
                model_kwargs = {"device": "cpu", "local_files_only": True}
            elif os.path.exists(model_name):
                model_path = model_name
                model_kwargs = {"device": "cpu", "local_files_only": True}
            else:
                model_path = model_name
                model_kwargs = {"device": "cpu"}
            
            try:
                cls._embeddings = HuggingFaceEmbeddings(
                    model_name=model_path,
                    model_kwargs=model_kwargs,
                    encode_kwargs={"normalize_embeddings": True}
                )
            except Exception as e:
                raise RuntimeError(
                    f"无法加载嵌入模型: {model_path}\n"
                    f"错误: {str(e)}\n"
                    f"请确保模型已正确下载，或设置 RAG_EMBEDDING_MODEL 为本地模型路径"
                )
        return cls._embeddings

    @classmethod
    def _get_vector_store(cls):
        if cls._vector_store is None:
            if os.path.exists(settings.RAG_VECTOR_DB_PATH):
                cls._vector_store = Chroma(
                    persist_directory=settings.RAG_VECTOR_DB_PATH,
                    embedding_function=cls._get_embeddings()
                )
        return cls._vector_store

    @classmethod
    def ingest_documents(cls, docs_dir: str):
        loaders = [
            DirectoryLoader(docs_dir, glob="*.txt", loader_cls=TextLoader),
            DirectoryLoader(docs_dir, glob="*.md", loader_cls=TextLoader),
            DirectoryLoader(docs_dir, glob="*.pdf", loader_cls=PyPDFLoader),
            DirectoryLoader(docs_dir, glob="*.docx", loader_cls=Docx2txtLoader),
            DirectoryLoader(docs_dir, glob="*.doc", loader_cls=Docx2txtLoader),
        ]

        documents = []
        for loader in loaders:
            try:
                docs = loader.load()
                documents.extend(docs)
            except Exception:
                pass

        if not documents:
            raise ValueError(f"未在目录 {docs_dir} 中找到任何支持的文档文件")

        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.RAG_CHUNK_SIZE,
            chunk_overlap=settings.RAG_CHUNK_OVERLAP,
            length_function=len,
            separators=["\n\n", "\n", "。", "！", "？", ".", "!", "?", "；", ";", ""]
        )

        split_docs = text_splitter.split_documents(documents)

        cls._vector_store = Chroma.from_documents(
            documents=split_docs,
            embedding=cls._get_embeddings(),
            persist_directory=settings.RAG_VECTOR_DB_PATH
        )
        cls._vector_store.persist()

        return len(split_docs)

    @classmethod
    def retrieve(cls, query: str, top_k: int = None) -> list:
        k = top_k if top_k else settings.RAG_TOP_K
        vector_store = cls._get_vector_store()

        if vector_store is None:
            raise ValueError("向量数据库未初始化，请先调用 ingest_documents 方法")

        results = vector_store.similarity_search_with_score(query, k=k)

        retrieved_snippets = []
        for doc, score in results:
            retrieved_snippets.append({
                "content": doc.page_content,
                "source": doc.metadata.get("source", ""),
                "score": score,
                "metadata": doc.metadata
            })

        return retrieved_snippets