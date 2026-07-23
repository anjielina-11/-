from fastapi import APIRouter, HTTPException
from models.schemas import RetrieveRequest, RetrieveResponse, IngestRequest, IngestResponse
from services.rag_service import RAGService

router = APIRouter(prefix="/api/v1/rag", tags=["rag"])


@router.post(
    "/ingest",
    responses={
        200: {"model": IngestResponse, "description": "文档导入成功"},
        400: {"model": dict, "description": "参数错误或文档未找到"}
    },
    summary="导入文档到向量数据库",
    description="将指定目录下的文档导入到向量数据库，支持txt、md、pdf、docx格式。"
)
async def ingest_documents(request: IngestRequest):
    try:
        chunks_count = RAGService.ingest_documents(request.docs_dir)
        return IngestResponse(
            success=True,
            message=f"成功导入 {chunks_count} 个文档片段",
            chunks_count=chunks_count
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"导入失败: {str(e)}")


@router.post(
    "/retrieve",
    responses={
        200: {"model": RetrieveResponse, "description": "检索成功"},
        400: {"model": dict, "description": "参数错误或向量数据库未初始化"}
    },
    summary="检索相关文档",
    description="根据查询文本检索向量数据库中最相关的文档片段。"
)
async def retrieve_documents(request: RetrieveRequest):
    try:
        results = RAGService.retrieve(request.query, request.top_k)
        return RetrieveResponse(
            success=True,
            results=results
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"检索失败: {str(e)}")