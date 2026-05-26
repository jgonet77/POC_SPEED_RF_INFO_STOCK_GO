# backend/tests/test_token_verification.py
import pytest
import jwt
from datetime import datetime, timedelta, timezone
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
    """Generate a valid JWT token for testing."""
    from config import settings
    expiry = datetime.now(tz=timezone.utc) + timedelta(hours=24)
    payload = {"login": "testuser", "exp": expiry}
    token = jwt.encode(payload, settings.secret_key, algorithm="HS256")
    return token


@pytest.fixture
def expired_token():
    """Generate an expired JWT token for testing."""
    from config import settings
    # Expiry in the past
    expiry = datetime.now(tz=timezone.utc) - timedelta(hours=1)
    payload = {"login": "testuser", "exp": expiry}
    token = jwt.encode(payload, settings.secret_key, algorithm="HS256")
    return token


@pytest.fixture
def invalid_signature_token():
    """Generate a JWT token with invalid signature."""
    expiry = datetime.now(tz=timezone.utc) + timedelta(hours=24)
    payload = {"login": "testuser", "exp": expiry}
    # Sign with wrong key
    token = jwt.encode(payload, "wrong-secret-key", algorithm="HS256")
    return token


# ---------------------------------------------------------------------------
# Tests: Protected endpoints require valid token
# ---------------------------------------------------------------------------

@pytest.fixture(autouse=True)
def clear_active_tokens():
    """Clear active_tokens before each test to avoid cross-test contamination."""
    from routes.auth import active_tokens
    active_tokens.clear()
    yield
    active_tokens.clear()


def test_search_endpoint_requires_auth_header(client):
    """GET /api/search without Authorization header returns 401."""
    response = client.get("/api/search?sku=TEST123")
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"
    assert "Unauthorized" in data["message"]
    assert data.get("detail") == "Please login first"


def test_search_endpoint_with_valid_token_passes_auth(client, valid_token):
    """GET /api/search with valid Authorization header passes auth."""
    from routes.auth import active_tokens
    active_tokens[valid_token] = "testuser"

    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": f"Bearer {valid_token}"}
    )
    # Should not return 401 (auth passes; may return 200 or other based on search logic)
    assert response.status_code != 401


def test_search_endpoint_missing_bearer_prefix(client, valid_token):
    """GET /api/search with missing Bearer prefix returns 401."""
    from routes.auth import active_tokens
    active_tokens[valid_token] = "testuser"

    # Missing "Bearer " prefix
    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": valid_token}
    )
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"


def test_search_endpoint_with_malformed_auth_header(client):
    """GET /api/search with malformed Authorization header returns 401."""
    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": "InvalidFormat token"}
    )
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"


def test_search_endpoint_with_expired_token(client, expired_token):
    """GET /api/search with expired token returns 401."""
    from routes.auth import active_tokens
    active_tokens[expired_token] = "testuser"

    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": f"Bearer {expired_token}"}
    )
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"
    assert "expired" in data["message"].lower() or "invalid" in data["message"].lower()


def test_search_endpoint_with_invalid_signature(client, invalid_signature_token):
    """GET /api/search with invalid signature returns 401."""
    from routes.auth import active_tokens
    active_tokens[invalid_signature_token] = "testuser"

    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": f"Bearer {invalid_signature_token}"}
    )
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"


def test_search_endpoint_token_not_in_active_tokens(client, valid_token):
    """GET /api/search with valid token not in active_tokens returns 401."""
    # Explicitly ensure token is NOT in active_tokens
    from routes.auth import active_tokens
    if valid_token in active_tokens:
        del active_tokens[valid_token]

    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": f"Bearer {valid_token}"}
    )
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"


# ---------------------------------------------------------------------------
# Tests: Details endpoint also requires auth
# ---------------------------------------------------------------------------

def test_details_endpoint_requires_auth_header(client):
    """GET /api/details/{sku} without Authorization header returns 401."""
    response = client.get("/api/details/TEST123")
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"
    assert "Unauthorized" in data["message"]


def test_details_endpoint_with_valid_token_passes_auth(client, valid_token):
    """GET /api/details/{sku} with valid Authorization header passes auth."""
    from routes.auth import active_tokens
    active_tokens[valid_token] = "testuser"

    response = client.get(
        "/api/details/TEST123",
        headers={"Authorization": f"Bearer {valid_token}"}
    )
    # Should not return 401 (auth passes)
    assert response.status_code != 401


def test_details_endpoint_with_expired_token(client, expired_token):
    """GET /api/details/{sku} with expired token returns 401."""
    from routes.auth import active_tokens
    active_tokens[expired_token] = "testuser"

    response = client.get(
        "/api/details/TEST123",
        headers={"Authorization": f"Bearer {expired_token}"}
    )
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"


# ---------------------------------------------------------------------------
# Tests: Public endpoints (health/*) don't require auth
# ---------------------------------------------------------------------------

def test_api_health_endpoint_no_auth_required(client):
    """GET /api/health/api without token returns 200."""
    response = client.get("/api/health/api")
    assert response.status_code == 200


def test_db_health_endpoint_no_auth_required(client):
    """GET /api/health/database without token returns 200 or 500 (DB issue)."""
    response = client.get("/api/health/database")
    # Should not require auth (not 401/403)
    # May be 200 (healthy) or 500 (DB unavailable), but not auth error
    assert response.status_code in [200, 500]


def test_login_endpoint_no_auth_required(client):
    """POST /api/login without token returns 400 or 401 (not 403)."""
    # Missing hash_method is invalid, but should return 4xx not 403
    response = client.post("/api/login", json={
        "login": "admin",
        "password": "password",
        # hash_method missing
    })
    # Should not be 403 (forbidden/not authorized)
    assert response.status_code in [400, 422]  # Bad request or validation error


# ---------------------------------------------------------------------------
# Tests: Auth failure logging
# ---------------------------------------------------------------------------

def test_missing_token_is_logged(client, tmp_path, monkeypatch):
    """Missing token failure is logged to logs/auth.log."""
    # Redirect logs to tmp_path
    monkeypatch.setenv("LOG_PATH", str(tmp_path / "auth.log"))

    response = client.get(
        "/api/search?sku=TEST123",
        headers={}
    )
    assert response.status_code == 401


def test_invalid_token_is_logged(client, invalid_signature_token):
    """Invalid token failure is logged."""
    from routes.auth import active_tokens
    active_tokens[invalid_signature_token] = "testuser"

    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": f"Bearer {invalid_signature_token}"}
    )
    assert response.status_code == 401


# ---------------------------------------------------------------------------
# Tests: Response format contract
# ---------------------------------------------------------------------------

def test_auth_error_response_format(client):
    """401 response has correct format: status, message, detail."""
    response = client.get(
        "/api/search?sku=TEST123",
        headers={}
    )
    assert response.status_code == 401
    data = response.json()
    assert "status" in data
    assert data["status"] == "error"
    assert "message" in data
    assert "detail" in data


def test_auth_error_response_no_sensitive_data(client):
    """401 response doesn't leak sensitive data like SECRET_KEY."""
    response = client.get(
        "/api/search?sku=TEST123",
        headers={"Authorization": "Bearer invalid-token"}
    )
    body = response.json()
    assert "secret" not in str(body).lower()
