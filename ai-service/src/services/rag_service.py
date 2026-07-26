import os
import ssl

os.environ.setdefault("ANONYMIZED_TELEMETRY", "False")

from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from langchain_community.document_loaders import DirectoryLoader, TextLoader, PyPDFLoader, Docx2txtLoader
from langchain_community.vectorstores import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from ..core.config import settings
from ..core.paths import resolve_service_path

ssl._create_default_https_context = ssl._create_unverified_context


class RAGService:
    COLLECTION_NAME = "yunong_rag_v3"
    DISEASE_ALIASES = {
        "citrus_canker": "柑橘溃疡病",
        "citrus_red_spider": "柑橘红蜘蛛",
        "corn_borer": "玉米螟",
        "corn_leaf_blight": "玉米大斑病",
        "corn_smut": "玉米黑粉病",
        "cotton_verticillium": "棉花黄萎病",
        "cucumber_downy_mildew": "黄瓜霜霉病",
        "cucumber_powdery_mildew": "黄瓜白粉病",
        "pepper_anthracnose": "辣椒炭疽病",
        "potato_late_blight": "马铃薯晚疫病",
        "rice_blast": "水稻稻瘟病",
        "rice_sheath_blight": "水稻纹枯病",
        "rice_stem_maggot": "水稻秆蝇",
        "soybean_pod_borer": "大豆食心虫",
        "tomato_gray_mold": "番茄灰霉病",
        "tomato_late_blight": "番茄晚疫病",
        "wheat_rust": "小麦锈病",
        "wheat_scab": "小麦赤霉病",
    }
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
            elif resolve_service_path(model_name).exists():
                model_path = str(resolve_service_path(model_name))
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
            vector_db_path = resolve_service_path(settings.RAG_VECTOR_DB_PATH)
            if vector_db_path.exists():
                cls._vector_store = Chroma(
                    collection_name=cls.COLLECTION_NAME,
                    persist_directory=str(vector_db_path),
                    embedding_function=cls._get_embeddings()
                )
        return cls._vector_store

    @classmethod
    def _prepare_documents_for_chunking(cls, documents: list) -> list:
        prepared = []
        for document in documents:
            source = str(document.metadata.get("source", ""))
            if not source.lower().endswith(".md"):
                prepared.append(document)
                continue

            sections = []
            heading = None
            section_lines = []
            for line in document.page_content.splitlines():
                if line.startswith("## "):
                    if heading and section_lines:
                        sections.append((heading, section_lines))
                    heading = line[3:].strip()
                    section_lines = [line]
                elif heading:
                    section_lines.append(line)
            if heading and section_lines:
                sections.append((heading, section_lines))

            if not sections:
                prepared.append(document)
                continue

            for section_heading, lines in sections:
                metadata = dict(document.metadata)
                metadata["section"] = section_heading
                disease = next((name for name in cls.DISEASE_ALIASES.values() if name in section_heading), None)
                if disease:
                    metadata["disease"] = disease
                prepared.append(Document(page_content="\n".join(lines).strip(), metadata=metadata))

        return prepared

    @classmethod
    def ingest_documents(cls, docs_dir):
        docs_path = resolve_service_path(docs_dir)
        loaders = [
            DirectoryLoader(str(docs_path), glob="*.txt", loader_cls=TextLoader,
                            loader_kwargs={"encoding": "utf-8"}),
            DirectoryLoader(str(docs_path), glob="*.md", loader_cls=TextLoader,
                            loader_kwargs={"encoding": "utf-8"}),
            DirectoryLoader(str(docs_path), glob="*.pdf", loader_cls=PyPDFLoader),
            DirectoryLoader(str(docs_path), glob="*.docx", loader_cls=Docx2txtLoader),
            DirectoryLoader(str(docs_path), glob="*.doc", loader_cls=Docx2txtLoader),
        ]

        documents = []
        for loader in loaders:
            try:
                docs = loader.load()
                documents.extend(docs)
            except Exception:
                pass

        if not documents:
            raise ValueError(f"未在目录 {docs_path} 中找到任何支持的文档文件")

        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.RAG_CHUNK_SIZE,
            chunk_overlap=settings.RAG_CHUNK_OVERLAP,
            length_function=len,
            separators=["\n\n", "\n", "。", "！", "？", ".", "!", "?", "；", ";", ""]
        )

        prepared_documents = cls._prepare_documents_for_chunking(documents)
        split_docs = text_splitter.split_documents(prepared_documents)
        for document in split_docs:
            disease = document.metadata.get("disease")
            section = document.metadata.get("section")
            if disease and disease not in document.page_content:
                document.page_content = f"## {section}\n\n{document.page_content}"

        if cls._vector_store is not None and cls._vector_store._collection.count() > 0:
            cls._vector_store.delete_collection()
            cls._vector_store = None

        cls._vector_store = Chroma.from_documents(
            documents=split_docs,
            embedding=cls._get_embeddings(),
            collection_name=cls.COLLECTION_NAME,
            persist_directory=str(resolve_service_path(settings.RAG_VECTOR_DB_PATH))
        )

        return len(split_docs)

    @classmethod
    def ensure_initialized(cls, docs_dir=None) -> int:
        vector_store = cls._get_vector_store()
        if vector_store is not None and vector_store._collection.count() > 0:
            return 0
        return cls.ingest_documents(docs_dir or settings.RAG_KNOWLEDGE_DOCS_PATH)

    @classmethod
    def retrieve(cls, query: str, top_k: int = None) -> list:
        k = top_k if top_k else settings.RAG_TOP_K
        vector_store = cls._get_vector_store()

        if vector_store is None:
            raise ValueError("向量数据库未初始化，请先调用 ingest_documents 方法")

        normalized_query = query
        target_terms = []
        target_disease = None
        lowered_query = query.lower()
        for code, chinese_name in cls.DISEASE_ALIASES.items():
            if code in lowered_query or chinese_name in query:
                target_terms = [code, chinese_name]
                target_disease = chinese_name
                normalized_query = query.replace(code, chinese_name)
                break

        # Over-fetch before lexical reranking so a semantically close but wrong disease
        # does not crowd out the exact disease document.
        search_k = max(k * 4, 8)
        if target_disease:
            try:
                results = vector_store.similarity_search_with_score(
                    normalized_query, k=search_k, filter={"disease": target_disease}
                )
            except TypeError:
                results = vector_store.similarity_search_with_score(normalized_query, k=search_k)
            if not results:
                results = vector_store.similarity_search_with_score(normalized_query, k=search_k)
        else:
            results = vector_store.similarity_search_with_score(normalized_query, k=search_k)
        ranked_results = []
        for doc, score in results:
            metadata_text = " ".join(str(value) for value in doc.metadata.values())
            haystack = f"{doc.page_content} {metadata_text}".lower()
            exact_match = any(term.lower() in haystack for term in target_terms) if target_terms else False
            ranked_results.append((not exact_match, score, doc))

        if target_terms and any(not item[0] for item in ranked_results):
            ranked_results = [item for item in ranked_results if not item[0]]
        ranked_results.sort(key=lambda item: (item[0], item[1]))

        return [{
            "content": doc.page_content,
            "source": doc.metadata.get("source", ""),
            "score": float(score),
            "metadata": doc.metadata
        } for _, score, doc in ranked_results[:k]]
