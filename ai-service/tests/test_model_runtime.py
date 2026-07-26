from io import BytesIO
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from src.main import app
from src.models.schemas import ModelActivateRequest, RuntimeInfo
from src.services.model_runtime_service import ModelRuntimeService


class FakeClassifier:
    def __init__(
        self,
        model_path=None,
        class_to_idx_path=None,
        num_classes=None,
        threshold=None,
    ):
        self.model_path = Path(model_path)
        self.class_to_idx_path = Path(class_to_idx_path)
        self.num_classes = num_classes
        self.threshold = threshold
        self.class_names = ["rice_blast"]

    def predict_from_bytes(self, _image_bytes):
        return "rice_blast", 0.91


def create_model_files(root: Path, prefix: str = "model") -> tuple[Path, Path]:
    model_path = root / f"{prefix}.pth"
    mapping_path = root / f"{prefix}-classes.pth"
    model_path.write_bytes(b"model")
    mapping_path.write_bytes(b"mapping")
    return model_path, mapping_path


def activation_request(model_path: Path, mapping_path: Path, version: str = "v1"):
    return ModelActivateRequest(
        model_id="model-1",
        model_name="test-model",
        version=version,
        model_path=str(model_path),
        class_to_idx_path=str(mapping_path),
        num_classes=18,
        confidence_threshold=0.65,
    )


def png_bytes() -> bytes:
    buffer = BytesIO()
    Image.new("RGB", (2, 2), "green").save(buffer, format="PNG")
    return buffer.getvalue()


def test_activate_valid_model_atomically_switches_runtime(tmp_path):
    model_path, mapping_path = create_model_files(tmp_path)
    runtime = ModelRuntimeService(
        allowed_roots=[tmp_path],
        classifier_factory=FakeClassifier,
    )

    info = runtime.activate(activation_request(model_path, mapping_path))

    assert info.loaded is True
    assert info.version == "v1"
    assert runtime.get_runtime() == info
    assert runtime.get_classifier().model_path == model_path.resolve()
    assert runtime.get_classifier().threshold == 0.65


def test_activate_rejects_path_outside_allowed_roots(tmp_path):
    allowed_root = tmp_path / "allowed"
    outside_root = tmp_path / "outside"
    allowed_root.mkdir()
    outside_root.mkdir()
    model_path, mapping_path = create_model_files(outside_root)
    runtime = ModelRuntimeService(
        allowed_roots=[allowed_root],
        classifier_factory=FakeClassifier,
    )

    with pytest.raises(ValueError, match="允许目录"):
        runtime.activate(activation_request(model_path, mapping_path))


def test_activate_rejects_missing_model_file(tmp_path):
    mapping_path = tmp_path / "classes.pth"
    mapping_path.write_bytes(b"mapping")
    runtime = ModelRuntimeService(
        allowed_roots=[tmp_path],
        classifier_factory=FakeClassifier,
    )

    with pytest.raises(FileNotFoundError, match="模型文件不存在"):
        runtime.activate(activation_request(tmp_path / "missing.pth", mapping_path))


def test_failed_activation_keeps_previous_classifier_and_runtime(tmp_path):
    model_path, mapping_path = create_model_files(tmp_path, "stable")

    class SometimesFailingClassifier(FakeClassifier):
        def __init__(self, *args, **kwargs):
            if Path(kwargs["model_path"]).name.startswith("broken"):
                raise RuntimeError("load failed")
            super().__init__(*args, **kwargs)

    runtime = ModelRuntimeService(
        allowed_roots=[tmp_path],
        classifier_factory=SometimesFailingClassifier,
    )
    stable_info = runtime.activate(activation_request(model_path, mapping_path, "v1"))
    stable_classifier = runtime.get_classifier()
    broken_model, broken_mapping = create_model_files(tmp_path, "broken")

    with pytest.raises(RuntimeError, match="load failed"):
        runtime.activate(activation_request(broken_model, broken_mapping, "v2"))

    assert runtime.get_classifier() is stable_classifier
    assert runtime.get_runtime() == stable_info


def test_model_runtime_api_delegates_to_app_runtime(monkeypatch):
    expected = RuntimeInfo(
        model_id="model-1",
        model_name="runtime-model",
        version="v2",
        model_path="/app/model.pth",
        class_to_idx_path="/app/classes.pth",
        num_classes=18,
        confidence_threshold=0.6,
        loaded=True,
    )

    class FakeRuntime:
        def __init__(self):
            self.request = None

        def get_runtime(self):
            return expected

        def activate(self, request):
            self.request = request
            return expected

    runtime = FakeRuntime()
    monkeypatch.setattr(app.state, "model_runtime", runtime, raising=False)
    client = TestClient(app)

    get_response = client.get("/api/v1/models/runtime")
    post_response = client.post(
        "/api/v1/models/activate",
        json={
            "model_id": "model-1",
            "model_name": "runtime-model",
            "version": "v2",
            "model_path": "/app/model.pth",
            "class_to_idx_path": "/app/classes.pth",
            "num_classes": 18,
            "confidence_threshold": 0.6,
        },
    )

    assert get_response.status_code == 200
    assert post_response.status_code == 200
    assert get_response.json()["version"] == "v2"
    assert runtime.request.model_name == "runtime-model"


def test_simple_diagnosis_uses_classifier_from_runtime(monkeypatch):
    classifier = FakeClassifier("model.pth", "classes.pth", 18, 0.6)

    class FakeRuntime:
        def __init__(self):
            self.calls = 0

        def get_classifier(self):
            self.calls += 1
            return classifier

    runtime = FakeRuntime()
    monkeypatch.setattr(app.state, "model_runtime", runtime, raising=False)
    client = TestClient(app)

    response = client.post(
        "/api/v1/diagnosis/simple",
        files={"image": ("leaf.png", png_bytes(), "image/png")},
    )

    assert response.status_code == 200
    assert response.json()["disease_name"] == "rice_blast"
    assert runtime.calls == 1

def test_classifier_accepts_zero_confidence_threshold(monkeypatch, tmp_path):
    from src.services.inference_service import DiseaseClassifier

    monkeypatch.setattr(DiseaseClassifier, "_load_class_mapping", lambda self: setattr(self, "idx_to_class", {0: "rice_blast"}))
    monkeypatch.setattr(DiseaseClassifier, "_load_model", lambda self: None)

    classifier = DiseaseClassifier(
        model_path=tmp_path / "model.pth",
        class_to_idx_path=tmp_path / "classes.pth",
        num_classes=1,
        threshold=0.0,
    )

    assert classifier.threshold == 0.0

def test_disease_list_returns_503_when_runtime_is_unavailable(monkeypatch):
    class UnavailableRuntime:
        def get_classifier(self):
            raise RuntimeError("模型尚未加载")

    monkeypatch.setattr(app.state, "model_runtime", UnavailableRuntime(), raising=False)
    response = TestClient(app).get("/api/v1/diagnosis/diseases")

    assert response.status_code == 503
