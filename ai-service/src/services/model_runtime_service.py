from pathlib import Path
from threading import RLock
from typing import Callable, Iterable, Optional

from ..models.schemas import ModelActivateRequest, RuntimeInfo
from .inference_service import DiseaseClassifier


class ModelRuntimeService:
    """Owns the active classifier and switches it only after a candidate loads."""

    def __init__(
        self,
        allowed_roots: Iterable[Path],
        classifier_factory: Callable = DiseaseClassifier,
        initial_request: Optional[ModelActivateRequest] = None,
    ):
        self._lock = RLock()
        self._allowed_roots = tuple(Path(root).resolve() for root in allowed_roots)
        self._classifier_factory = classifier_factory
        self._classifier = None
        self._runtime = self._to_runtime(initial_request, loaded=False) if initial_request else None

    def activate(self, request: ModelActivateRequest) -> RuntimeInfo:
        model_path = self._validate_file(request.model_path, "模型文件")
        mapping_path = self._validate_file(request.class_to_idx_path, "类别映射文件")

        candidate = self._classifier_factory(
            model_path=model_path,
            class_to_idx_path=mapping_path,
            num_classes=request.num_classes,
            threshold=request.confidence_threshold,
        )
        runtime = self._to_runtime(
            request.model_copy(
                update={
                    "model_path": str(model_path),
                    "class_to_idx_path": str(mapping_path),
                }
            ),
            loaded=True,
        )

        with self._lock:
            self._classifier = candidate
            self._runtime = runtime
        return runtime

    def get_classifier(self):
        with self._lock:
            if self._classifier is None:
                raise RuntimeError("模型尚未加载")
            return self._classifier

    def get_runtime(self) -> RuntimeInfo:
        with self._lock:
            if self._runtime is None:
                raise RuntimeError("模型 Runtime 尚未配置")
            return self._runtime.model_copy(deep=True)

    def _validate_file(self, value: str, label: str) -> Path:
        path = Path(value).expanduser().resolve()
        if not any(path == root or root in path.parents for root in self._allowed_roots):
            raise ValueError(f"{label}不在允许目录内")
        if not path.exists() or not path.is_file():
            raise FileNotFoundError(f"{label}不存在: {path}")
        return path

    @staticmethod
    def _to_runtime(request: ModelActivateRequest, loaded: bool) -> RuntimeInfo:
        return RuntimeInfo(**request.model_dump(), loaded=loaded)