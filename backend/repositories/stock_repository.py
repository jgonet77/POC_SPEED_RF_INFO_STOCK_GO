from typing import List, Dict, Optional
import pyodbc
from repositories.base_repository import BaseRepository, DatabaseConnectionError, QueryError
from models.stock import StockItem, StockSearchRequest


class StockRepository(BaseRepository):
    """Repository for stock data queries."""

    def search_by_activity(self, request: StockSearchRequest) -> List[Dict]:
        """
        Search stock by article code, location, or storage number within activity.

        Returns list of dicts with keys: art_code, stk_lieu, stk_nosu, qua_code, stk_qte

        Raises:
            QueryError: on database query failure
            DatabaseConnectionError: on connection failure
        """
        # Validation is handled by Pydantic model_validator

        # Build WHERE clause with parameterized query (activity filter is AND, rest are OR)
        conditions = ["ACT_CODE = ?"]
        params = [request.act_code]

        if request.art_code:
            conditions.append("ART_CODE LIKE ?")
            params.append(f"%{request.art_code}%")
        if request.stk_lieu:
            conditions.append("STK_LIEU LIKE ?")
            params.append(f"%{request.stk_lieu}%")
        if request.stk_nosu:
            conditions.append("STK_NOSU LIKE ?")
            params.append(f"%{request.stk_nosu}%")

        # First condition is activity filter (AND), rest are OR
        where_clause = conditions[0] + " AND (" + " OR ".join(conditions[1:]) + ")"

        query = f"""
            SELECT ART_CODE, STK_LIEU, STK_NOSU, QUA_CODE, STK_QTE
            FROM STK_DAT
            WHERE {where_clause}
            ORDER BY ART_CODE, STK_LIEU
        """

        try:
            rows = self.execute_query(query, tuple(params))
        except (DatabaseConnectionError, QueryError) as exc:
            raise QueryError(f"Stock search failed: {str(exc)}") from exc

        # Convert rows to dicts
        results = []
        for row in rows:
            results.append({
                "art_code": row[0],
                "stk_lieu": row[1],
                "stk_nosu": row[2],
                "qua_code": row[3],
                "stk_qte": row[4],
            })

        return results
