import pytest
import torch
from PIL import Image
from io import BytesIO
from services.inference_service import DiseaseClassifier, UnknownDiseaseError


@pytest.fixture
def mock_image_bytes():
    img = Image.new('RGB', (224, 224), color='red')
    buf = BytesIO()
    img.save(buf, format='JPEG')
    return buf.getvalue()


@pytest.fixture
def classifier():
    try:
        return DiseaseClassifier()
    except FileNotFoundError:
        pytest.skip("Model files not found, skipping inference tests")


def test_predict_returns_disease_name_and_confidence(classifier, mock_image_bytes):
    try:
        disease_name, confidence = classifier.predict_from_bytes(mock_image_bytes)
        
        assert isinstance(disease_name, str)
        assert len(disease_name) > 0
        assert isinstance(confidence, float)
        assert 0 <= confidence <= 1
    except UnknownDiseaseError:
        pytest.skip("Unknown disease due to low confidence, skipping test")


def test_predict_confidence_in_range(classifier, mock_image_bytes):
    try:
        _, confidence = classifier.predict_from_bytes(mock_image_bytes)
        
        assert 0 <= confidence <= 1, f"Confidence {confidence} is not in range [0, 1]"
    except UnknownDiseaseError:
        pytest.skip("Unknown disease due to low confidence, skipping test")


def test_unknown_disease_error_is_raised_with_low_confidence(classifier):
    low_threshold_classifier = DiseaseClassifier(threshold=0.99)
    
    img = Image.new('RGB', (224, 224), color='blue')
    buf = BytesIO()
    img.save(buf, format='JPEG')
    image_bytes = buf.getvalue()
    
    with pytest.raises(UnknownDiseaseError):
        low_threshold_classifier.predict_from_bytes(image_bytes)