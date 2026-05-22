from .base_repository import BaseRepository, DatabaseConnectionError, QueryError


class HealthRepository(BaseRepository):
    """Repository for health check operations (DB connectivity tests)"""

    def check_database_connection(self) -> dict:
        """
        Test connection to SQL Server
        Returns: Dict with status, server_version, and server_time
        Raises: DatabaseConnectionError on connection failure
                QueryError on query failure
        """
        try:
            query = "SELECT @@VERSION as version, GETDATE() as server_time"
            result = self.execute_query(query)

            if result:
                row = result[0]
                return {
                    "status": "connected",
                    "server_version": row[0],
                    "server_time": str(row[1])
                }
            else:
                raise QueryError("Query executed but returned no results")

        except (DatabaseConnectionError, QueryError):
            raise  # re-raise our custom exceptions
        except Exception as e:
            raise QueryError(f"Unexpected error: {str(e)}") from e
