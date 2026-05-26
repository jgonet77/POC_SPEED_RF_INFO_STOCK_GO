# backend/repositories/auth_repository.py
from typing import Optional, Tuple

from repositories.base_repository import BaseRepository, DatabaseConnectionError, QueryError

AUTH_TABLE = "USW_DAT"


class AuthRepositoryError(Exception):
    """Raised when the auth repository encounters an error."""
    pass


class AuthRepository(BaseRepository):
    """Repository for user authentication data (table USW_DAT)."""

    def find_user_by_login(self, login: str) -> Optional[Tuple[str, str]]:
        """
        Return (USW_LOGN, USW_PASS) for the given login, or None if not found.

        Raises:
            AuthRepositoryError: on DB connection or query failure.
        """
        query = f"SELECT USW_LOGN, USW_PASS FROM {AUTH_TABLE} WHERE USW_LOGN = ?"
        try:
            rows = self.execute_query(query, (login,))
        except (DatabaseConnectionError, QueryError) as exc:
            raise AuthRepositoryError(str(exc)) from exc

        if not rows:
            return None

        row = rows[0]
        return (row[0], row[1])
