import pytest
import os
import shutil
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from src.services.rag_service import RAGService


@pytest.fixture(autouse=True)
def setup_and_teardown():
    test_docs_dir = "tests/test_docs"
    os.makedirs(test_docs_dir, exist_ok=True)
    
    with open(os.path.join(test_docs_dir, "test_disease.txt"), "w", encoding="utf-8") as f:
        f.write("番茄晚疫病是由疫霉菌引起的病害。\n")
        f.write("防治方法：轮作倒茬、及时清除病株。\n")
    
    RAGService._vector_store = None
    RAGService._embeddings = None
    
    yield
    
    shutil.rmtree(test_docs_dir, ignore_errors=True)
    if os.path.exists("chroma_db"):
        shutil.rmtree("chroma_db", ignore_errors=True)


def test_ingest_documents():
    chunk_count = RAGService.ingest_documents("tests/test_docs")
    
    assert isinstance(chunk_count, int)
    assert chunk_count > 0, f"Expected at least 1 chunk, got {chunk_count}"


def test_retrieve_returns_non_empty_list():
    RAGService.ingest_documents("tests/test_docs")
    
    results = RAGService.retrieve("番茄晚疫病防治")
    
    assert isinstance(results, list)
    assert len(results) > 0, "Expected at least 1 result"


def test_retrieve_results_have_required_fields():
    RAGService.ingest_documents("tests/test_docs")
    
    results = RAGService.retrieve("番茄晚疫病防治")
    
    for result in results:
        assert "content" in result
        assert isinstance(result["content"], str)
        assert len(result["content"]) > 0
        
        assert "source" in result
        assert isinstance(result["source"], str)
        
        assert "score" in result
        assert isinstance(result["score"], float)


def test_retrieve_returns_top_k_results():
    RAGService.ingest_documents("tests/test_docs")
    
    results = RAGService.retrieve("番茄晚疫病防治", top_k=2)
    
    assert len(results) <= 2