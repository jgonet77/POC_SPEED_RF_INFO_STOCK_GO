import pytest
from unittest.mock import patch, MagicMock


@pytest.fixture(autouse=True)
def set_required_env(monkeypatch):
    """Ensure required env vars are set for all tests in this module"""
    monkeypatch.setenv("SQL_SERVER_PASSWORD", "test_password")


def test_health_service_initialization():
    """Test that service can be instantiated"""
    from services.health_service import HealthService
    service = HealthService()
    assert service is not None
    assert service.repository is not None


def test_get_database_health_structure():
    """Test that health check returns proper structure"""
    from services.health_service import HealthService

    service = HealthService()

    mock_db_result = {
        "status": "connected",
        "server_version": "Microsoft SQL Server 2019",
        "server_time": "2026-01-01 00:00:00"
    }
    with patch.object(service.repository, "check_database_connection", return_value=mock_db_result):
        result = service.get_database_health()

    assert "service" in result
    assert "database_status" in result
    assert "details" in result
    assert result["service"] == "Database Connection Test"


def test_get_database_health_maps_db_fields():
    """Test that health service maps repository fields to response correctly"""
    from services.health_service import HealthService

    service = HealthService()

    mock_db_result = {
        "status": "connected",
        "server_version": "SQL Server 2019 RTM",
        "server_time": "2026-05-22 08:00:00"
    }
    with patch.object(service.repository, "check_database_connection", return_value=mock_db_result):
        result = service.get_database_health()

    assert result["database_status"] == "connected"
    assert result["details"]["version"] == "SQL Server 2019 RTM"
    assert result["details"]["server_time"] == "2026-05-22 08:00:00"


def test_get_database_health_propagates_connection_error():
    """Test that DatabaseConnectionError from repository propagates through service"""
    from services.health_service import HealthService
    from repositories.base_repository import DatabaseConnectionError

    service = HealthService()

    with patch.object(service.repository, "check_database_connection",
                      side_effect=DatabaseConnectionError("Cannot connect")):
        with pytest.raises(DatabaseConnectionError):
            service.get_database_health()
