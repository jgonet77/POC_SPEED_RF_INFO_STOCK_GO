package com.example.stockapp.models

data class HealthCheckResponse(
    val service: String,
    val database_status: String,
    val details: Map<String, Any>
)

data class ApiHealthResponse(
    val status: String,
    val message: String
)
