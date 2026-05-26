# backend/config.py
from dotenv import load_dotenv
import os

load_dotenv()  # Charger automatiquement le fichier .env


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

        # Auth settings
        self.secret_key = os.getenv("SECRET_KEY", "dev-secret-key-change-in-prod")

    @staticmethod
    def _env_int(name: str, default: int) -> int:
        raw = os.getenv(name, str(default))
        try:
            return int(raw)
        except ValueError:
            raise RuntimeError(f"Env var {name} must be an integer, got {raw!r}")

    @property
    def connection_string(self) -> str:
        # For named instances (e.g., SERVERNAME\INSTANCENAME), don't add port
        # For default instance, add port
        if "\\" in self.sql_server_host:
            # Named instance - don't add port
            server_part = f"Server={self.sql_server_host};"
        else:
            # Default instance - add port
            server_part = f"Server={self.sql_server_host},{self.sql_server_port};"

        return (
            f"Driver={{ODBC Driver 17 for SQL Server}};"
            f"{server_part}"
            f"Database={self.sql_server_db};"
            f"UID={self.sql_server_user};"
            f"PWD={self.sql_server_password};"
        )


settings = Settings()
