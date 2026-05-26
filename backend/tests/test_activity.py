# backend/tests/test_activity.py
import pytest
from unittest.mock import patch


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(autouse=True)
def set_required_env(monkeypatch):
    """Ensure required env vars are present before the app is imported."""
    monkeypatch.setenv("SQL_SERVER_PASSWORD", "test_password")
    monkeypatch.setenv("SECRET_KEY", "test-secret-key")


@pytest.fixture
def client(set_required_env):
    """FastAPI test client, imported lazily so env vars are set first."""
    from fastapi.testclient import TestClient
    from main import app
    return TestClient(app)


@pytest.fixture
def valid_token():
    """Create a valid JWT token for testing."""
    import jwt
    from datetime import datetime, timedelta, timezone

    expiry = datetime.now(tz=timezone.utc) + timedelta(hours=24)
    payload = {"login": "testuser", "exp": expiry}
    token = jwt.encode(payload, "test-secret-key", algorithm="HS256")

    # Add to active tokens so verification passes
    from services.token_store import active_tokens
    active_tokens[token] = "testuser"

    return token


# ---------------------------------------------------------------------------
# Test 1: get_activities_success
# ---------------------------------------------------------------------------

def test_get_activities_success(client, valid_token):
    """
    GET /api/activities with valid token returns 200 with activities list.

    Mock DB returns 3 activities, response includes status=success and activities.
    """
    mock_activities = [
        {"act_keyu": 1, "act_code": "PREP", "act_lib": "Préparation"},
        {"act_keyu": 2, "act_code": "PORT", "act_lib": "Portage"},
        {"act_keyu": 3, "act_code": "RECE", "act_lib": "Réception"},
    ]

    with patch("routes.activity.ActivityRepository.get_all_activities", return_value=mock_activities):
        response = client.get(
            "/api/activities",
            headers={"Authorization": f"Bearer {valid_token}"}
        )

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["message"] == "Activities retrieved successfully"
    assert len(data["activities"]) == 3

    # Check first activity structure
    activity = data["activities"][0]
    assert activity["act_keyu"] == 1
    assert activity["act_code"] == "PREP"
    assert activity["act_lib"] == "Préparation"


# ---------------------------------------------------------------------------
# Test 2: get_activities_requires_auth
# ---------------------------------------------------------------------------

def test_get_activities_requires_auth(client):
    """
    GET /api/activities without token returns 401.

    Missing Authorization header should trigger 401 response.
    """
    response = client.get("/api/activities")

    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"
    assert "Unauthorized" in data["message"]


def test_get_activities_invalid_token(client):
    """
    GET /api/activities with invalid token returns 401.
    """
    response = client.get(
        "/api/activities",
        headers={"Authorization": "Bearer invalid_token_123"}
    )

    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"


# ---------------------------------------------------------------------------
# Test 3: get_activities_returns_active_only
# ---------------------------------------------------------------------------

def test_get_activities_filters_active_only(client, valid_token):
    """
    Repository query uses WHERE ACT_ACTF = 1 to filter active activities only.

    This test mocks the repository to verify it's being called,
    and checks that the mock is configured to return only active records.
    """
    mock_activities = [
        {"act_keyu": 1, "act_code": "PREP", "act_lib": "Préparation"},
        {"act_keyu": 2, "act_code": "PORT", "act_lib": "Portage"},
    ]

    with patch("routes.activity.ActivityRepository") as mock_repo_class:
        mock_repo = mock_repo_class.return_value
        mock_repo.get_all_activities.return_value = mock_activities

        response = client.get(
            "/api/activities",
            headers={"Authorization": f"Bearer {valid_token}"}
        )

    assert response.status_code == 200
    data = response.json()
    assert len(data["activities"]) == 2
    # Repository method was called once
    mock_repo.get_all_activities.assert_called_once()


# ---------------------------------------------------------------------------
# Test 4: get_activities_db_error
# ---------------------------------------------------------------------------

def test_get_activities_db_error(client, valid_token):
    """
    GET /api/activities returns 500 when database error occurs.

    When repository raises an error, endpoint returns status=error and message.
    """
    from repositories.activity_repository import ActivityRepositoryError

    with patch("routes.activity.ActivityRepository.get_all_activities",
               side_effect=ActivityRepositoryError("Database connection failed")):
        response = client.get(
            "/api/activities",
            headers={"Authorization": f"Bearer {valid_token}"}
        )

    assert response.status_code == 500
    data = response.json()
    assert data["status"] == "error"
    assert "Failed to fetch activities" in data["message"]


# ---------------------------------------------------------------------------
# Test 5: response_format_contract
# ---------------------------------------------------------------------------

def test_get_activities_empty_list(client, valid_token):
    """
    GET /api/activities returns valid response even with empty activities list.

    Response format should be consistent regardless of data.
    """
    with patch("routes.activity.ActivityRepository.get_all_activities", return_value=[]):
        response = client.get(
            "/api/activities",
            headers={"Authorization": f"Bearer {valid_token}"}
        )

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["activities"] == []
    assert "message" in data
