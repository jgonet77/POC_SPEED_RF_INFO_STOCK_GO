# backend/repositories/activity_repository.py
from typing import List, Dict
from repositories.base_repository import BaseRepository, DatabaseConnectionError, QueryError

ACTIVITY_TABLE = "ACT_PAR"


class ActivityRepositoryError(Exception):
    """Raised when the activity repository encounters an error."""
    pass


class ActivityRepository(BaseRepository):
    """Repository for activity data (table ACT_PAR)."""

    def get_all_activities(self) -> List[Dict]:
        """
        Return list of active activities from ACT_PAR table.

        Only returns activities where ACT_ACTF = 1 (active flag).
        Results are ordered by ACT_LIB (activity label).

        Returns:
            List[Dict]: List of dicts with keys: act_keyu, act_code, act_lib

        Raises:
            ActivityRepositoryError: on DB connection or query failure.
        """
        query = f"SELECT ACT_KEYU, ACT_CODE, ACT_LIB FROM {ACTIVITY_TABLE} WHERE ACT_ACTF = 1 ORDER BY ACT_LIB"
        try:
            rows = self.execute_query(query)
        except (DatabaseConnectionError, QueryError) as exc:
            raise ActivityRepositoryError(str(exc)) from exc

        # Convert rows (tuples) to list of dicts
        activities = []
        for row in rows:
            activities.append({
                "act_keyu": row[0],
                "act_code": row[1],
                "act_lib": row[2],
            })

        return activities
