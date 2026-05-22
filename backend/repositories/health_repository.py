from .base_repository import BaseRepository


class HealthRepository(BaseRepository):
    """Repository for health check operations (DB connectivity tests)"""

    def check_database_connection(self) -> dict:
        """
        Test connection to SQL Server by executing a simple query
        Returns: Dict with connection status and server info
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
                return {"status": "error", "message": "Query executed but no results"}

        except Exception as e:
            return {
                "status": "error",
                "message": str(e)
            }
