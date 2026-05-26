# backend/models/auth.py
from pydantic import BaseModel
from typing import Optional


class LoginRequest(BaseModel):
    """Request model for login endpoint"""
    login: str
    password: str
    hash_method: str  # "CLAIR", "MD5", or "SHA256"


class LoginResponse(BaseModel):
    """Response model for login endpoint"""
    status: str  # "success" or "error"
    message: str
    token: Optional[str] = None       # Only on success
    expires_in: Optional[int] = None  # Only on success (86400 for 24h)
    log_location: Optional[str] = None  # Only on error
