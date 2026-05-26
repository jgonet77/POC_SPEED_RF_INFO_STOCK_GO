# backend/models/activity.py
from pydantic import BaseModel
from typing import List


class ActivityResponse(BaseModel):
    """Response model for a single activity"""
    act_keyu: int
    act_code: str
    act_lib: str


class ActivityListResponse(BaseModel):
    """Response model for activity list endpoint"""
    status: str  # "success" or "error"
    message: str
    activities: List[ActivityResponse]
