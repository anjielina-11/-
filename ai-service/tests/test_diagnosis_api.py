from fastapi.testclient import TestClient

from src.main import app


client = TestClient(app)


def test_advice_accepts_structured_context_without_reuploading_image(monkeypatch):
    monkeypatch.setattr(
        "src.api.diagnosis.AgentService.generate_advice",
        lambda request: {
            "advice": "建议文本",
            "references": request.citations,
            "context_summary": {"crop_name": request.crop.name},
            "agent_trace": [{"agent": "treatment", "status": "completed", "summary": "已生成建议"}],
        },
    )

    response = client.post(
        "/api/v1/diagnosis/advice",
        json={
            "disease_name": "rice_blast",
            "confidence": 0.9,
            "crop": {"name": "水稻", "growth_stage": "tillering"},
            "field": {"name": "A-01", "farm_name": "验收农场"},
            "weather_forecast": [
                {
                    "date": "2026-07-26",
                    "weather": "高湿",
                    "temperature": 25,
                    "humidity": 82,
                    "rainfall": 2.5
                }
            ],
            "citations": [],
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["advice"] == "建议文本"
    assert body["context_summary"]["crop_name"] == "水稻"
    assert body["agent_trace"][0]["agent"] == "treatment"

def test_low_confidence_result_preserves_model_score_for_manual_review():
    from src.services.diagnosis_service import DiagnosisService
    from src.services.inference_service import UnknownDiseaseError

    class RejectingClassifier:
        def predict_from_bytes(self, _image_bytes):
            raise UnknownDiseaseError(
                disease_name="potato_late_blight",
                confidence=0.176073,
                threshold=0.6,
            )

    result = DiagnosisService.analyze_image(b"image", "leaf.png", RejectingClassifier())[0]

    assert result.disease_name == "未知病害"
    assert result.confidence == 0.176073
    assert "potato_late_blight" in result.description
    assert "0.1761" in result.description
