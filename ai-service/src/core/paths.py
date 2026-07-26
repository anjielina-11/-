from pathlib import Path
from typing import Union

PathValue = Union[str, Path]
AI_SERVICE_ROOT = Path(__file__).resolve().parents[2]


def resolve_service_path(value: PathValue) -> Path:
    """Resolve configured relative paths from the AI service root, never from cwd."""
    path = Path(value).expanduser()
    return path if path.is_absolute() else AI_SERVICE_ROOT / path
