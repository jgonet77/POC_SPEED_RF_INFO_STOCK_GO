from typing import List
from models.stock import StockSearchRequest, StockItem, StockSearchResponse
from repositories.stock_repository import StockRepository
from repositories.base_repository import QueryError


class StockService:
    """Service layer for stock operations."""

    def __init__(self):
        self.repository = StockRepository()

    def search_stock(self, request: StockSearchRequest) -> StockSearchResponse:
        """
        Search stock by activity and flexible criteria.

        Returns:
            StockSearchResponse with items matching any criterion within activity
        """
        try:
            # Request validation is handled by Pydantic model_validator

            # Query repository
            results = self.repository.search_by_activity(request)

            # Convert to StockItem objects
            items = [
                StockItem(
                    art_code=row["art_code"],
                    stk_lieu=row["stk_lieu"],
                    stk_nosu=row["stk_nosu"],
                    qua_code=row["qua_code"],
                    stk_qte=row["stk_qte"],
                )
                for row in results
            ]

            # Build response
            if len(items) == 0:
                message = "No stock items found matching criteria"
            else:
                message = f"Found {len(items)} item(s)"

            return StockSearchResponse(
                status="success",
                message=message,
                items=items
            )

        except ValueError as e:
            # Validation error
            return StockSearchResponse(
                status="error",
                message=f"Invalid search criteria: {str(e)}",
                items=[]
            )
        except QueryError as e:
            # Database error
            return StockSearchResponse(
                status="error",
                message=f"Database error: {str(e)}",
                items=[]
            )
        except Exception as e:
            # Unexpected error
            return StockSearchResponse(
                status="error",
                message=f"Unexpected error: {str(e)}",
                items=[]
            )
