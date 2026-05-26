# backend/routes/activity.py
"""
Activity endpoints (list of available activities).
Protected endpoint requiring JWT token verification.
"""
from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse

from middleware.auth import verify_token
from models.activity import ActivityResponse, ActivityListResponse
from repositories.activity_repository import ActivityRepository, ActivityRepositoryError


router = APIRouter()


@router.get("/activities", response_model=ActivityListResponse)
async def get_activities(
    current_user: dict = Depends(verify_token)
) -> dict:
    """
    Get list of all active activities from ACT_PAR table.

    Returns:
        ActivityListResponse: List of available activities with their keys and codes

    Authentication:
        Requires valid JWT token in Authorization header (Bearer {token})

    Raises:
        HTTPException: 401 if token is invalid or missing
    """
    repo = ActivityRepository()
    try:
        activities_data = repo.get_all_activities()
    except ActivityRepositoryError as exc:
        return JSONResponse(
            status_code=500,
            content=ActivityListResponse(
                status="error",
                message="Failed to fetch activities",
                activities=[],
            ).model_dump(),
        )
    finally:
        repo.close()

    # Convert list of dicts to ActivityResponse objects
    activities = [
        ActivityResponse(
            act_keyu=item["act_keyu"],
            act_code=item["act_code"],
            act_lib=item["act_lib"],
        )
        for item in activities_data
    ]

    return ActivityListResponse(
        status="success",
        message="Activities retrieved successfully",
        activities=activities,
    )
