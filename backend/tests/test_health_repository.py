import pytest
import os
from unittest.mock import patch, MagicMock


@pytest.fixture(autouse=True)
def set_required_env(monkeypatch):
    """Ensure required env vars are set for all tests in this module"""
    monkeypatch.setenv("SQL_SERVER_PASSWORD", "test_password")


def test_health_repository_initialization():
    """Test that repository can be instantiated"""
    from repositories.health_repository import HealthRepository
    repo = HealthRepository()
    assert repo is not None
    assert repo.connection_string is not None


def test_health_repository_connection_string_format():
    """Test that connection string is properly formatted"""
    from repositories.health_repository import HealthRepository
    repo = HealthRepository()
    conn_str = repo.connection_string
    assert "ODBC Driver 17 for SQL Server" in conn_str
    assert "Server=" in conn_str
    assert "Database=" in conn_str


def test_health_repository_check_database_connection_success():
    """Test check_database_connection returns expected structure on success"""
    from repositories.health_repository import HealthRepository
    from repositories.base_repository import DatabaseConnectionError

    repo = HealthRepository()

    mock_row = ("Microsoft SQL Server 2019", "2026-01-01 00:00:00")
    with patch.object(repo, "execute_query", return_value=[mock_row]):
        result = repo.check_database_connection()

    assert result["status"] == "connected"
    assert result["server_version"] == "Microsoft SQL Server 2019"
    assert result["server_time"] == "2026-01-01 00:00:00"


def test_health_repository_check_database_connection_raises_on_db_error():
    """Test that DatabaseConnectionError propagates from check_database_connection"""
    from repositories.health_repository import HealthRepository
    from repositories.base_repository import DatabaseConnectionError

    repo = HealthRepository()

    with patch.object(repo, "execute_query", side_effect=DatabaseConnectionError("Connection refused")):
        with pytest.raises(DatabaseConnectionError):
            repo.check_database_connection()


def test_health_repository_check_database_connection_raises_on_empty_result():
    """Test that QueryError is raised when query returns no rows"""
    from repositories.health_repository import HealthRepository
    from repositories.base_repository import QueryError

    repo = HealthRepository()

    with patch.object(repo, "execute_query", return_value=[]):
        with pytest.raises(QueryError):
            repo.check_database_connection()
