import pyodbc
from config import settings
from typing import Optional


class BaseRepository:
    """Base class for database access"""

    def __init__(self):
        self.connection_string = settings.connection_string
        self._connection: Optional[pyodbc.Connection] = None

    def _get_connection(self) -> pyodbc.Connection:
        """Get or create a database connection"""
        if self._connection is None:
            try:
                self._connection = pyodbc.connect(self.connection_string)
            except pyodbc.Error as e:
                raise Exception(f"Database connection failed: {str(e)}")
        return self._connection

    def execute_query(self, query: str, params: tuple = ()) -> list:
        """Execute a SELECT query and return results"""
        try:
            connection = self._get_connection()
            cursor = connection.cursor()
            cursor.execute(query, params)
            return cursor.fetchall()
        except pyodbc.Error as e:
            raise Exception(f"Query execution failed: {str(e)}")

    def __del__(self):
        """Cleanup on object destruction"""
        self.close()

    def close(self):
        """Close database connection"""
        if self._connection:
            self._connection.close()
            self._connection = None
