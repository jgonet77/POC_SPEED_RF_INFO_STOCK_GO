# backend/tests/test_auth.py
import hashlib
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


def _make_mock_auth_repo(login_result):
    """
    Return a context manager that patches AuthRepository.find_user_by_login
    to return login_result (a tuple or None).
    """
    return patch(
        "routes.auth.AuthRepository.find_user_by_login",
        return_value=login_result,
    )


# ---------------------------------------------------------------------------
# Success cases
# ---------------------------------------------------------------------------

def test_login_clair_success(client):
    """CLAIR password match returns 200 with token and expires_in."""
    with _make_mock_auth_repo(("admin", "password")):
        response = client.post("/api/login", json={
            "login": "admin",
            "password": "password",
            "hash_method": "CLAIR",
        })
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["message"] == "Login successful"
    assert data["token"] is not None
    assert data["expires_in"] == 86400


def test_login_md5_success(client):
    """MD5-hashed password match returns 200 with token."""
    md5_hash = hashlib.md5("mysecret".encode()).hexdigest()
    with _make_mock_auth_repo(("user1", md5_hash)):
        response = client.post("/api/login", json={
            "login": "user1",
            "password": "mysecret",
            "hash_method": "MD5",
        })
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["token"] is not None


def test_login_sha256_success(client):
    """SHA256-hashed password match returns 200 with token."""
    sha256_hash = hashlib.sha256("secret123".encode()).hexdigest()
    with _make_mock_auth_repo(("user2", sha256_hash)):
        response = client.post("/api/login", json={
            "login": "user2",
            "password": "secret123",
            "hash_method": "SHA256",
        })
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["token"] is not None


# ---------------------------------------------------------------------------
# Failure cases — 401
# ---------------------------------------------------------------------------

def test_login_invalid_user(client):
    """Non-existent login returns 401 with proper error shape."""
    with _make_mock_auth_repo(None):
        response = client.post("/api/login", json={
            "login": "nonexistent",
            "password": "password",
            "hash_method": "CLAIR",
        })
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"
    assert "log_location" in data
    assert data["log_location"] == "logs/auth.log"


def test_login_wrong_password(client):
    """Correct login but wrong password returns 401 with proper error shape."""
    with _make_mock_auth_repo(("admin", "correctpassword")):
        response = client.post("/api/login", json={
            "login": "admin",
            "password": "wrongpass",
            "hash_method": "CLAIR",
        })
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"
    assert "log_location" in data
    assert data["log_location"] == "logs/auth.log"


def test_login_wrong_md5_password(client):
    """MD5 hash mismatch returns 401 with proper error shape."""
    correct_hash = hashlib.md5("correct".encode()).hexdigest()
    with _make_mock_auth_repo(("admin", correct_hash)):
        response = client.post("/api/login", json={
            "login": "admin",
            "password": "wrong",
            "hash_method": "MD5",
        })
    assert response.status_code == 401
    data = response.json()
    assert data["status"] == "error"
    assert "log_location" in data


# ---------------------------------------------------------------------------
# Failure cases — 400
# ---------------------------------------------------------------------------

def test_login_invalid_hash_method(client):
    """Unknown hash_method returns 400 with proper error shape."""
    with _make_mock_auth_repo(("admin", "password")):
        response = client.post("/api/login", json={
            "login": "admin",
            "password": "password",
            "hash_method": "INVALID",
        })
    assert response.status_code == 400
    data = response.json()
    assert data["status"] == "error"
    assert "log_location" in data
    assert data["log_location"] == "logs/auth.log"


# ---------------------------------------------------------------------------
# Failure cases — 500
# ---------------------------------------------------------------------------

def test_login_db_error_returns_500(client):
    """DB failure returns 500 with proper error shape."""
    from repositories.auth_repository import AuthRepositoryError
    with patch(
        "routes.auth.AuthRepository.find_user_by_login",
        side_effect=AuthRepositoryError("Connection refused"),
    ):
        response = client.post("/api/login", json={
            "login": "admin",
            "password": "password",
            "hash_method": "CLAIR",
        })
    assert response.status_code == 500
    data = response.json()
    assert data["status"] == "error"
    assert "log_location" in data
    assert data["log_location"] == "logs/auth.log"


# ---------------------------------------------------------------------------
# Response shape contract (for Android team)
# ---------------------------------------------------------------------------

def test_success_response_has_no_log_location(client):
    """Successful response must not expose log_location."""
    with _make_mock_auth_repo(("admin", "password")):
        response = client.post("/api/login", json={
            "login": "admin",
            "password": "password",
            "hash_method": "CLAIR",
        })
    data = response.json()
    assert data.get("log_location") is None


def test_token_is_valid_jwt(client):
    """Token returned on success must be a decodable JWT."""
    import jwt as pyjwt
    with _make_mock_auth_repo(("admin", "password")):
        response = client.post("/api/login", json={
            "login": "admin",
            "password": "password",
            "hash_method": "CLAIR",
        })
    token = response.json()["token"]
    payload = pyjwt.decode(token, "test-secret-key", algorithms=["HS256"])
    assert payload["login"] == "admin"
