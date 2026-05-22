# backend/routes/health.py
from fastapi import APIRouter, HTTPException
from services.health_service import HealthService
from models.schemas import HealthCheckResponse, ErrorResponse

router = APIRouter()
health_service = HealthService()

@router.get("/health/database", response_model=HealthCheckResponse)
def check_database_health():
    """
    Test database connection
    Returns server version and current time if connected
    """
    try:
        result = health_service.get_database_health()

        if result["database_status"] == "error":
            raise HTTPException(
                status_code=500,
                detail=result["details"].get("error", "Unknown database error")
            )

        return HealthCheckResponse(**result)

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Health check failed: {str(e)}"
        )

@router.get("/health/api")
def check_api_health():
    """Check API health (always returns 200 if this endpoint is reachable)"""
    return {
        "status": "healthy",
        "message": "API is running"
    }
