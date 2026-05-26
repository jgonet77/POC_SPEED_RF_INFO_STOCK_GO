# backend/routes/auth.py
import hashlib
import os
from datetime import datetime, timedelta, timezone

import jwt
from fastapi import APIRouter
from fastapi.responses import JSONResponse

from config import settings
from models.auth import LoginRequest, LoginResponse
from repositories.auth_repository import AuthRepository, AuthRepositoryError

router = APIRouter()

# In-memory token store — maps token string to login
active_tokens: dict[str, str] = {}

LOG_PATH = "logs/auth.log"
VALID_HASH_METHODS = {"CLAIR", "MD5", "SHA256"}


def _hash_password(password: str, method: str) -> str:
    """Return password hashed with the specified method."""
    if method == "CLAIR":
        return password
    if method == "MD5":
        return hashlib.md5(password.encode()).hexdigest()
    # SHA256 — already validated upstream
    return hashlib.sha256(password.encode()).hexdigest()


def _log_auth_event(login: str, hash_method: str, success: bool, error_msg: str = None) -> None:
    """Append an auth event line to logs/auth.log and stdout."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    status = "SUCCESS" if success else "FAILED"
    line = f"[{timestamp}] LOGIN_ATTEMPT login={login} hash={hash_method} status={status}"
    if error_msg:
        line += f" error={error_msg}"

    os.makedirs("logs", exist_ok=True)
    with open(LOG_PATH, "a", encoding="utf-8") as f:
        f.write(line + "\n")

    print(line)


@router.post("/login", response_model=LoginResponse)
async def login(request: LoginRequest) -> LoginResponse:
    """
    Authenticate a user and return a JWT token valid for 24 h.

    Request body: {login, password, hash_method}
    Success response: {status, message, token, expires_in}
    Error response:   {status, message, log_location}
    """
    # Validate hash method before touching the DB
    if request.hash_method not in VALID_HASH_METHODS:
        _log_auth_event(request.login, request.hash_method, False, "Invalid hash method")
        return JSONResponse(
            status_code=400,
            content=LoginResponse(
                status="error",
                message="Invalid hash_method",
                log_location=LOG_PATH,
            ).model_dump(),
        )

    # Fetch user record from DB
    repo = AuthRepository()
    try:
        row = repo.find_user_by_login(request.login)
    except AuthRepositoryError as exc:
        error_msg = f"DB_ERROR: {exc}"
        _log_auth_event(request.login, request.hash_method, False, error_msg)
        return JSONResponse(
            status_code=500,
            content=LoginResponse(
                status="error",
                message="Database error",
                log_location=LOG_PATH,
            ).model_dump(),
        )
    finally:
        repo.close()

    if row is None:
        _log_auth_event(request.login, request.hash_method, False, "User not found")
        return JSONResponse(
            status_code=401,
            content=LoginResponse(
                status="error",
                message="Invalid login or password",
                log_location=LOG_PATH,
            ).model_dump(),
        )

    db_login, db_password = row

    # Compare hashed/plain password against stored value
    provided_hash = _hash_password(request.password, request.hash_method)
    if provided_hash != db_password:
        _log_auth_event(request.login, request.hash_method, False, "Password mismatch")
        return JSONResponse(
            status_code=401,
            content=LoginResponse(
                status="error",
                message="Invalid login or password",
                log_location=LOG_PATH,
            ).model_dump(),
        )

    # Generate JWT with 24 h expiry (UTC)
    expiry = datetime.now(tz=timezone.utc) + timedelta(hours=24)
    payload = {"login": db_login, "exp": expiry}
    try:
        token = jwt.encode(payload, settings.secret_key, algorithm="HS256")
    except Exception as exc:
        _log_auth_event(request.login, request.hash_method, False, f"JWT_ERROR: {exc}")
        return JSONResponse(
            status_code=500,
            content=LoginResponse(
                status="error",
                message="Token generation error",
                log_location=LOG_PATH,
            ).model_dump(),
        )

    active_tokens[token] = db_login
    _log_auth_event(request.login, request.hash_method, True)

    return LoginResponse(
        status="success",
        message="Login successful",
        token=token,
        expires_in=86400,
    )
