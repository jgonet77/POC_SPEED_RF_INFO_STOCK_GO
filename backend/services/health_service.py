from repositories.health_repository import HealthRepository


class HealthService:
    """Service layer for health check operations"""

    def __init__(self):
        self.repository = HealthRepository()

    def get_database_health(self) -> dict:
        """
        Check database health and return formatted response

        Returns:
            Dict with service health info including DB status, version, and time

        Raises:
            DatabaseConnectionError if connection fails
            QueryError if query fails
        """
        db_check = self.repository.check_database_connection()

        return {
            "service": "Database Connection Test",
            "database_status": db_check.get("status"),
            "details": {
                "version": db_check.get("server_version"),
                "server_time": db_check.get("server_time")
            }
        }
