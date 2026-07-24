import pytest
import os
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
