from fastapi.testclient import TestClient

from src.main import app


client = TestClient(app)


def test_advice_accepts_json_without_reuploading_image(monkeypatch):
    monkeypatch.setattr(
        "src.api.diagnosis.AgentService.generate_advice",
        lambda **kwargs: {
            "advice": "建议文本",
            "references": kwargs.get("citations", []),
            "weather_info": kwargs.get("weather_info", "未知天气"),
        },
    )

    response = client.post(
        "/api/v1/diagnosis/advice",
        json={
            "disease_name": "rice_blast",
            "confidence": 0.9,
            "crop_info": "水稻",
            "weather_info": "高湿",
            "citations": [],
        },
    )

    assert response.status_code == 200
    assert response.json()["advice"] == "建议文本"
