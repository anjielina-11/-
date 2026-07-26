import pytest
import os
from src.models.schemas import KnowledgeSyncDocument
from src.services.rag_service import RAGService
from src.core.config import settings
from langchain_community.vectorstores import Chroma


@pytest.fixture(autouse=True)
def setup_and_teardown(tmp_path, monkeypatch):
    test_docs_dir = tmp_path / "test_docs"
    os.makedirs(test_docs_dir, exist_ok=True)
    monkeypatch.setattr(settings, "RAG_VECTOR_DB_PATH", str(tmp_path / "chroma_db"))
    
    with open(os.path.join(test_docs_dir, "test_disease.txt"), "w", encoding="utf-8") as f:
        f.write("番茄晚疫病是由疫霉菌引起的病害。\n")
        f.write("防治方法：轮作倒茬、及时清除病株。\n")
    
    RAGService._vector_store = None
    RAGService._embeddings = None
    
    yield str(test_docs_dir)

    RAGService._vector_store = None
    RAGService._embeddings = None


def test_ingest_documents(setup_and_teardown):
    chunk_count = RAGService.ingest_documents(setup_and_teardown)
    
    assert isinstance(chunk_count, int)
    assert chunk_count > 0, f"Expected at least 1 chunk, got {chunk_count}"


def test_ingest_uses_versioned_collection(setup_and_teardown):
    RAGService.ingest_documents(setup_and_teardown)

    assert RAGService._vector_store._collection.name == RAGService.COLLECTION_NAME


def test_retrieve_returns_non_empty_list(setup_and_teardown):
    RAGService.ingest_documents(setup_and_teardown)
    
    results = RAGService.retrieve("番茄晚疫病防治")
    
    assert isinstance(results, list)
    assert len(results) > 0, "Expected at least 1 result"


def test_retrieve_results_have_required_fields(setup_and_teardown):
    RAGService.ingest_documents(setup_and_teardown)
    
    results = RAGService.retrieve("番茄晚疫病防治")
    
    for result in results:
        assert "content" in result
        assert isinstance(result["content"], str)
        assert len(result["content"]) > 0
        
        assert "source" in result
        assert isinstance(result["source"], str)
        
        assert "score" in result
        assert isinstance(result["score"], float)


def test_retrieve_returns_top_k_results(setup_and_teardown):
    RAGService.ingest_documents(setup_and_teardown)
    
    results = RAGService.retrieve("番茄晚疫病防治", top_k=2)
    
    assert len(results) <= 2


def test_ensure_initialized_builds_missing_vector_database(setup_and_teardown):
    chunk_count = RAGService.ensure_initialized(setup_and_teardown)

    assert chunk_count > 0
    assert RAGService.retrieve("番茄晚疫病防治")


def test_ensure_initialized_populates_empty_vector_database(tmp_path, monkeypatch, setup_and_teardown):
    vector_db_path = str(tmp_path / "empty_chroma_db")
    monkeypatch.setattr(settings, "RAG_VECTOR_DB_PATH", vector_db_path)
    RAGService._vector_store = Chroma(
        persist_directory=vector_db_path,
        embedding_function=RAGService._get_embeddings(),
    )

    chunk_count = RAGService.ensure_initialized(setup_and_teardown)

    assert chunk_count > 0
    assert RAGService.retrieve("番茄晚疫病防治")


def test_retrieve_prioritizes_same_disease_documents(monkeypatch):
    class FakeDoc:
        def __init__(self, content, source):
            self.page_content = content
            self.metadata = {"source": source}

    class FakeStore:
        last_filter = None

        def similarity_search_with_score(self, query, k, filter=None):
            self.last_filter = filter
            return [
                (FakeDoc("\u5927\u8c46\u98df\u5fc3\u866b\u9632\u6cbb", "soybean.txt"), 0.05),
                (FakeDoc("\u67d1\u6a58\u6e83\u75a1\u75c5\u9632\u6cbb", "citrus.txt"), 0.18),
                (FakeDoc("\u67d1\u6a58\u7ea2\u8718\u86db\u9632\u6cbb", "spider.txt"), 0.10),
            ]

    monkeypatch.setattr(RAGService, "_vector_store", FakeStore())

    results = RAGService.retrieve("\u67d1\u6a58\u6e83\u75a1\u75c5\u9632\u6cbb", top_k=2)

    assert RAGService._vector_store.last_filter == {"disease": "\u67d1\u6a58\u6e83\u75a1\u75c5"}
    assert results[0]["source"] == "citrus.txt"
    assert all("\u5927\u8c46\u98df\u5fc3\u866b" not in item["content"] for item in results)


def test_markdown_ingest_preserves_disease_heading_on_all_chunks(setup_and_teardown):
    docs_dir = setup_and_teardown
    disease = "\u67d1\u6a58\u6e83\u75a1\u75c5"
    treatment = "\u9632\u6cbb\u65b9\u6cd5\uff1a\u6e05\u9664\u75c5\u679d\uff0c\u5408\u7406\u65bd\u80a5\u3002"
    markdown = f"# Test\n\n## {disease} (Citrus Canker)\n\n" + treatment * 80
    with open(os.path.join(docs_dir, "citrus.md"), "w", encoding="utf-8") as file:
        file.write(markdown)

    RAGService.ingest_documents(docs_dir)
    stored = RAGService._vector_store.get(
        where={"disease": disease},
        include=["documents", "metadatas"],
    )

    assert stored["documents"]
    assert all(disease in content for content in stored["documents"])
    assert all(item.get("disease") == disease for item in stored["metadatas"])


def test_replace_documents_rebuilds_collection_and_clears_on_empty(setup_and_teardown):
    count = RAGService.replace_documents([
        KnowledgeSyncDocument(
            id="k1",
            title="唯一验收知识",
            category="disease",
            version=2,
            content="稻瘟病连续降雨后加强巡田",
            tags=["水稻", "稻瘟病"],
        )
    ])

    assert count > 0
    result = RAGService.retrieve("连续降雨 巡田", top_k=1)
    assert result[0]["metadata"]["document_id"] == "k1"
    assert result[0]["metadata"]["title"] == "唯一验收知识"
    assert result[0]["metadata"]["version"] == 2

    assert RAGService.replace_documents([]) == 0
    assert RAGService.retrieve("连续降雨", top_k=1) == []


def test_replace_documents_keeps_previous_collection_when_build_fails(setup_and_teardown, monkeypatch):
    RAGService.replace_documents([
        KnowledgeSyncDocument(
            id="stable",
            title="稳定知识",
            category="disease",
            version=1,
            content="稳定内容用于验证回滚",
        )
    ])
    original_from_documents = Chroma.from_documents

    def fail_build(*args, **kwargs):
        raise RuntimeError("embedding failed")

    monkeypatch.setattr(Chroma, "from_documents", fail_build)
    with pytest.raises(RuntimeError, match="embedding failed"):
        RAGService.replace_documents([
            KnowledgeSyncDocument(
                id="broken",
                title="失败知识",
                category="disease",
                version=1,
                content="不应覆盖旧知识库",
            )
        ])

    monkeypatch.setattr(Chroma, "from_documents", original_from_documents)
    result = RAGService.retrieve("稳定内容", top_k=1)
    assert result[0]["metadata"]["document_id"] == "stable"