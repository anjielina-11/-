from src.services.agent_service import AgentService


def test_fallback_advice_uses_rag_reference(monkeypatch):
    monkeypatch.setattr("src.services.agent_service.settings.LLM_API_KEY", "")
    monkeypatch.setattr("src.services.agent_service.settings.LLM_API_BASE", "")

    result = AgentService.generate_advice(
        disease_name="rice_blast",
        citations=[{
            "source": "水稻病害规范",
            "content": "穗颈瘟关键防治期为破口期和齐穗期，可使用三环唑防治。",
            "score": 0.1,
        }],
    )

    assert "破口期和齐穗期" in result["advice"]
    assert "水稻病害规范" in result["advice"]
