# backend/routes/stock.py
"""
Stock information endpoints (search and details).
Protected endpoints requiring JWT token verification.
"""
from fastapi import APIRouter, Depends
from logging import getLogger

from models.stock import StockSearchRequest, StockSearchResponse
from services.stock_service import StockService
from middleware.auth import verify_token


logger = getLogger(__name__)

router = APIRouter()
stock_service = StockService()


@router.post("/search")
async def search_stock(
    request: StockSearchRequest,
    current_user: dict = Depends(verify_token)
) -> StockSearchResponse:
    """
    Search stock by article code, location, or storage number.

    Filters results by activity code (act_code).
    Returns 0-N results matching any criterion.

    Request:
    {
        "art_code": "ABC123",     (optional)
        "stk_lieu": "A-01",       (optional)
        "stk_nosu": "SUPP-001",   (optional)
        "act_code": "BKS"         (required)
    }
    """
    logger.info(
        f"STOCK_SEARCH_REQUEST act_code={request.act_code} "
        f"art_code={request.art_code} stk_lieu={request.stk_lieu} stk_nosu={request.stk_nosu} "
        f"user={current_user.get('login')}"
    )

    response = stock_service.search_stock(request)

    logger.info(
        f"STOCK_SEARCH_RESPONSE act_code={request.act_code} "
        f"status={response.status} items_found={len(response.items)} "
        f"user={current_user.get('login')}"
    )

    return response


@router.get("/details/{sku}")
async def get_stock_details(
    sku: str,
    current_user: dict = Depends(verify_token)
) -> dict:
    """
    Get detailed stock information for a specific SKU.

    Path Parameters:
        sku: Product SKU

    Returns:
        dict: Detailed stock information
    """
    # TODO: Implement stock details logic
    return {
        "status": "success",
        "message": "Details endpoint (not yet implemented)",
        "sku": sku,
        "user": current_user.get("login"),
    }
