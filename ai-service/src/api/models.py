from fastapi import APIRouter, HTTPException, Request

from ..models.schemas import ModelActivateRequest, RuntimeInfo

router = APIRouter(prefix="/api/v1/models", tags=["models"])


@router.get("/runtime", response_model=RuntimeInfo, summary="获取当前模型 Runtime")
async def get_model_runtime(request: Request):
    try:
        return request.app.state.model_runtime.get_runtime()
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@router.post("/activate", response_model=RuntimeInfo, summary="安全激活模型")
async def activate_model(payload: ModelActivateRequest, request: Request):
    try:
        return request.app.state.model_runtime.activate(payload)
    except (ValueError, FileNotFoundError) as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"模型加载失败: {exc}") from exc