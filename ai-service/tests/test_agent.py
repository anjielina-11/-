from src.core.config import settings
from src.models.schemas import AdviceRequest
from src.services.agent_service import AgentService


def test_fallback_advice_uses_real_context_and_agent_trace(monkeypatch):
    monkeypatch.setattr(settings, "LLM_API_KEY", "")
    monkeypatch.setattr(settings, "LLM_API_BASE", "")

    result = AgentService.generate_advice(
        AdviceRequest(
            disease_name="稻瘟病",
            confidence=0.92,
            crop={
                "name": "水稻",
                "variety": "滇粳验收",
                "planting_date": "2026-07-01",
                "growth_stage": "tillering",
            },
            field={"name": "A-01", "farm_name": "验收农场"},
            weather_forecast=[
                {
                    "date": "2026-07-26",
                    "weather": "阵雨",
                    "temperature": 28,
                    "humidity": 86,
                    "rainfall": 8.5,
                }
            ],
            citations=[
                {
                    "source": "水稻病害规范",
                    "content": "穗颈瘟关键防治期为破口期和齐穗期，可使用三环唑防治。",
                    "score": 0.1,
                }
            ],
        )
    )

    assert "分蘖期" in result.advice
    assert "阵雨" in result.advice
    assert "86" in result.advice
    assert "破口期和齐穗期" in result.advice
    assert [item.agent for item in result.agent_trace] == [
        "weather-risk",
        "growth-stage",
        "rag-evidence",
        "treatment",
    ]
    assert result.context_summary["crop_name"] == "水稻"


def test_missing_weather_and_rag_are_visible_in_trace(monkeypatch):
    monkeypatch.setattr(settings, "LLM_API_KEY", "")
    result = AgentService.generate_advice(
        AdviceRequest(
            disease_name="未知病害",
            confidence=0.65,
            crop={"name": "番茄", "growth_stage": "seedling"},
            field={"name": "B-02", "farm_name": "示范农场"},
            weather_forecast=[],
            citations=[],
        )
    )

    trace = {item.agent: item.status for item in result.agent_trace}
    assert trace["weather-risk"] == "no-data"
    assert trace["rag-evidence"] == "no-data"
    assert "农技人员复核" in result.advice