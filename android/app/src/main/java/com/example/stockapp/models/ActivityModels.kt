package com.example.stockapp.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a single activity available to the user.
 */
data class ActivityItem(
    @SerializedName("act_keyu")
    val actKeyu: Int,
    @SerializedName("act_code")
    val actCode: String,
    @SerializedName("act_lib")
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
