# backend/middleware/auth.py
"""
JWT token verification middleware for protected endpoints.
"""
import hashlib
import os
from datetime import datetime
from typing import Optional

import jwt
from fastapi import Header, HTTPException, status
from fastapi.responses import JSONResponse

from config import settings
from services.token_store import active_tokens

LOG_PATH = os.getenv("LOG_PATH", "logs/auth.log")
TOKEN_PREVIEW_LENGTH = 16


def _get_token_hash_preview(token: str) -> str:
    """
    Generate a safe token hash preview for logging.

    Returns first 16 hex chars of SHA256 hash instead of raw token prefix.
    This prevents token leakage in logs while maintaining traceability.
    """
    if not token:
        return "MISSING"
    token_hash = hashlib.sha256(token.encode()).hexdigest()
    return token_hash[:TOKEN_PREVIEW_LENGTH]


def _log_auth_failure(endpoint: str, token: str, reason: str) -> None:
    """
    Log authentication failure to logs/auth.log and stdout.

    Uses token hash instead of raw token prefix for security.
    """
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    token_display = _get_token_hash_preview(token)
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
        dict: Decoded JWT payload containing:
            - login (str): The authenticated user's login name
            - exp (int): Token expiration timestamp (Unix time)
            - Any other claims in the JWT payload

    Raises:
        HTTPException: 401 if token is missing, invalid, expired, or not in active_tokens
            - MISSING: Authorization header not present
            - MALFORMED_HEADER: Header doesn't match "Bearer {token}" format
            - EXPIRED: JWT signature is valid but token has expired
            - INVALID_SIGNATURE: JWT signature verification failed
            - NOT_IN_ACTIVE_TOKENS: Valid JWT but not found in active token store (revoked)
    """
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
        _log_auth_failure("protected", authorization, "MALFORMED_HEADER")
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
        _log_auth_failure("protected", token, "EXPIRED")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError:
        _log_auth_failure("protected", token, "INVALID_SIGNATURE")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # Check if token is in active_tokens (revocation check)
    if token not in active_tokens:
        _log_auth_failure("protected", token, "NOT_IN_ACTIVE_TOKENS")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return payload
