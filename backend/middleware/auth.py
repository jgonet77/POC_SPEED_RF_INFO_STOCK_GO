# backend/middleware/auth.py
"""
JWT token verification middleware for protected endpoints.
"""
import os
from datetime import datetime
from typing import Optional

import jwt
from fastapi import Header, HTTPException, status
from fastapi.responses import JSONResponse

from config import settings


LOG_PATH = "logs/auth.log"


def _log_auth_failure(endpoint: str, token_preview: str, reason: str) -> None:
    """Log authentication failure to logs/auth.log and stdout."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    token_display = token_preview[:20] + "..." if token_preview else "MISSING"
    line = f"[{timestamp}] AUTH_FAILED endpoint={endpoint} token={token_display} reason={reason}"

    os.makedirs("logs", exist_ok=True)
    with open(LOG_PATH, "a", encoding="utf-8") as f:
        f.write(line + "\n")

    print(line)


def verify_token(authorization: Optional[str] = Header(None)) -> dict:
    """
    FastAPI dependency to verify JWT token from Authorization header.

    Expected header format: "Authorization: Bearer {token}"

    Returns:
        dict: Decoded JWT payload (contains 'login' key)

    Raises:
        HTTPException: 401 if token is missing, invalid, expired, or not in active_tokens
    """
    from routes.auth import active_tokens

    # Check if Authorization header exists
    if not authorization:
        _log_auth_failure("protected", "", "MISSING")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # Parse "Bearer {token}" format
    parts = authorization.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        _log_auth_failure("protected", authorization[:20], "MALFORMED_HEADER")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = parts[1]

    # Decode JWT
    try:
        payload = jwt.decode(token, settings.secret_key, algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        _log_auth_failure("protected", token[:20], "EXPIRED")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError:
        _log_auth_failure("protected", token[:20], "INVALID_SIGNATURE")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # Check if token is in active_tokens (revocation check)
    if token not in active_tokens:
        _log_auth_failure("protected", token[:20], "NOT_IN_ACTIVE_TOKENS")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return payload
