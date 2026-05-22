# backend/config.py
import os


class Settings:
    """Database and app configuration"""

    def __init__(self):
        # SQL Server connection
        self.sql_server_host = os.getenv("SQL_SERVER_HOST", "localhost")
        self.sql_server_port = self._env_int("SQL_SERVER_PORT", 1433)
        self.sql_server_db = os.getenv("SQL_SERVER_DB", "WMS_SPEED")
        self.sql_server_user = os.getenv("SQL_SERVER_USER", "sa")
        self.sql_server_password = os.environ["SQL_SERVER_PASSWORD"]  # required

        # API settings
        self.api_host = os.getenv("API_HOST", "0.0.0.0")
        self.api_port = self._env_int("API_PORT", 8000)

    @staticmethod
    def _env_int(name: str, default: int) -> int:
        raw = os.getenv(name, str(default))
        try:
            return int(raw)
        except ValueError:
            raise RuntimeError(f"Env var {name} must be an integer, got {raw!r}")

    @property
    def connection_string(self) -> str:
        return (
            f"Driver={{ODBC Driver 17 for SQL Server}};"
            f"Server={self.sql_server_host},{self.sql_server_port};"
            f"Database={self.sql_server_db};"
            f"UID={self.sql_server_user};"
            f"PWD={self.sql_server_password};"
        )


settings = Settings()
