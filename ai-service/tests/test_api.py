import pytest
from fastapi.testclient import TestClient
from PIL import Image
from io import BytesIO
from src.main import app


client = TestClient(app)


@pytest.fixture
def mock_image():
    img = Image.new('RGB', (224, 224), color='red')
    buf = BytesIO()
    img.save(buf, format='JPEG')
    buf.seek(0)
    return ('test.jpg', buf, 'image/jpeg')


def test_health_endpoint():
    response = client.get("/health")
    
    assert response.status_code == 200
    data = response.json()
    assert data.get("status") in {"healthy", "degraded"}


def test_diagnosis_image_endpoint(mock_image):
    files = {"image": mock_image}
    
    response = client.post("/api/v1/diagnosis/image", files=files)
    
    assert response.status_code == 200
    data = response.json()
    
    assert "disease_name" in data or "status" in data
    
    if "disease_name" in data:
        assert isinstance(data["disease_name"], str)
        assert "confidence" in data
        assert 0 <= data["confidence"] <= 1
        assert "advice" in data
        assert isinstance(data["advice"], str)
    elif "status" in data:
        assert data["status"] == "pending_review"


def test_diagnosis_image_with_crop_and_weather(mock_image):
    files = {"image": mock_image}
    data = {
        "crop_info": "温室番茄",
        "weather_info": "气温25°C，湿度70%"
    }
    
    response = client.post("/api/v1/diagnosis/image", files=files, data=data)
    
    assert response.status_code == 200


def test_diagnosis_image_invalid_format():
    files = {"image": ("test.txt", b"not an image", "text/plain")}
    
    response = client.post("/api/v1/diagnosis/image", files=files)
    
    assert response.status_code == 400
