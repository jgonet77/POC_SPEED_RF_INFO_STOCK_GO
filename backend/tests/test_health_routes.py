import pytest
import os
from unittest.mock import patch


@pytest.fixture(autouse=True)
def set_required_env(monkeypatch):
    """Ensure required env vars are set before app import"""
    monkeypatch.setenv("SQL_SERVER_PASSWORD", "test_password")


@pytest.fixture
def client(set_required_env):
    """Create a TestClient after env vars are set"""
    from fastapi.testclient import TestClient
    from main import app
    return TestClient(app)


def test_api_health_endpoint(client):
    """Test that API health endpoint returns 200 and healthy status"""
    response = client.get("/api/health/api")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"


def test_root_endpoint(client):
    """Test root endpoint returns 200 and message field"""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "message" in data


def test_database_health_endpoint_success(client):
    """Test database health endpoint returns 200 when DB is reachable (mocked)"""
    mock_result = {
        "service": "Database Connection Test",
        "database_status": "connected",
        "details": {
            "version": "Microsoft SQL Server 2019",
            "server_time": "2026-05-22 08:00:00"
        }
    }
    with patch("routes.health.health_service.get_database_health", return_value=mock_result):
        response = client.get("/api/health/database")
    assert response.status_code == 200
    data = response.json()
    assert data["service"] == "Database Connection Test"
    assert data["database_status"] == "connected"


def test_database_health_endpoint_db_unavailable(client):
    """Test database health endpoint returns 500 when DB is unreachable"""
    from repositories.base_repository import DatabaseConnectionError

    with patch("routes.health.health_service.get_database_health",
               side_effect=DatabaseConnectionError("Cannot reach SQL Server")):
        response = client.get("/api/health/database")
    assert response.status_code == 500
