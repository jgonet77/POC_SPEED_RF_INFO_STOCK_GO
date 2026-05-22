# backend/models/schemas.py
from pydantic import BaseModel
from typing import Optional, Dict, Any

class HealthCheckResponse(BaseModel):
    """Response model for health check endpoint"""
    service: str
    database_status: str
    details: Dict[str, Any]

class ErrorResponse(BaseModel):
    """Error response model"""
    error: str
    message: str
    details: Optional[Dict[str, Any]] = None
