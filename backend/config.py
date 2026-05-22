# backend/config.py
import os
from typing import Optional

class Settings:
    """Database and app configuration"""

    # SQL Server connection
    SQL_SERVER_HOST: str = os.getenv("SQL_SERVER_HOST", "localhost")
    SQL_SERVER_PORT: int = int(os.getenv("SQL_SERVER_PORT", "1433"))
    SQL_SERVER_DB: str = os.getenv("SQL_SERVER_DB", "WMS_SPEED")
    SQL_SERVER_USER: str = os.getenv("SQL_SERVER_USER", "sa")
    SQL_SERVER_PASSWORD: str = os.getenv("SQL_SERVER_PASSWORD", "")

    # Connection string for pyodbc
    @property
    def connection_string(self) -> str:
        return (
            f"Driver={{ODBC Driver 17 for SQL Server}};"
            f"Server={self.SQL_SERVER_HOST},{self.SQL_SERVER_PORT};"
            f"Database={self.SQL_SERVER_DB};"
            f"UID={self.SQL_SERVER_USER};"
            f"PWD={self.SQL_SERVER_PASSWORD};"
        )

    # API settings
    API_HOST: str = os.getenv("API_HOST", "0.0.0.0")
    API_PORT: int = int(os.getenv("API_PORT", "8000"))

settings = Settings()
