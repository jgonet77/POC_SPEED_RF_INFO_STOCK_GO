from typing import List
from logging import getLogger
from models.stock import StockSearchRequest, StockItem, StockSearchResponse
from repositories.stock_repository import StockRepository
from repositories.base_repository import QueryError


logger = getLogger(__name__)


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
        logger.info(f"STOCK_SEARCH_INITIATED act_code={request.act_code}")

        try:
            # Request validation is handled by Pydantic model_validator

            # Query repository
            logger.debug(f"Querying repository with criteria: art_code={request.art_code}, stk_lieu={request.stk_lieu}, stk_nosu={request.stk_nosu}")
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

            logger.info(f"STOCK_SEARCH_COMPLETED act_code={request.act_code} items_count={len(items)}")

            return StockSearchResponse(
                status="success",
                items=items
            )

        except ValueError as e:
            # Validation error
            logger.warning(f"Validation error in stock search: {str(e)}")
            return StockSearchResponse(
                status="error",
                items=[]
            )
        except QueryError as e:
            # Database error
            logger.error(f"Query error in stock search: {str(e)}")
            return StockSearchResponse(
                status="error",
                items=[]
            )
