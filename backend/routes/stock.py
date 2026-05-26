# backend/routes/stock.py
"""
Stock information endpoints (search and details).
Protected endpoints requiring JWT token verification.
"""
from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse

from middleware.auth import verify_token


router = APIRouter()


@router.get("/search")
async def search_stock(
    sku: str,
    current_user: dict = Depends(verify_token)
) -> dict:
    """
    Search for stock by SKU.

    Query Parameters:
        sku: Product SKU to search for

    Returns:
        dict: Stock search results
    """
    # TODO: Implement stock search logic
    return {
        "status": "success",
        "message": "Search endpoint (not yet implemented)",
        "sku": sku,
        "user": current_user.get("login"),
    }


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
