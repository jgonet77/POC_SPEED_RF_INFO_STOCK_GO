package com.example.stockapp.models

/**
 * Represents a single activity available to the user.
 */
data class ActivityItem(
    val actKeyu: Int,
    val actCode: String,
    val actLib: String
)

/**
 * API response for GET /api/activities
 */
data class ActivityListResponse(
    val status: String,           // "success" or "error"
    val message: String,
    val activities: List<ActivityItem>
)
