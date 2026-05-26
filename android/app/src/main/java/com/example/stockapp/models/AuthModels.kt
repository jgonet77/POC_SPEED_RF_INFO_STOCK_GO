package com.example.stockapp.models

/**
 * Request body for POST /api/login
 */
data class LoginRequest(
    val login: String,
    val password: String,
    val hash_method: String  // CLAIR, MD5, or SHA256
)

/**
 * Success response for POST /api/login
 */
data class LoginResponse(
    val status: String,      // "success"
    val message: String,
    val token: String,
    val expires_in: Int,     // seconds
    val log_location: String?
)

/**
 * Error response for POST /api/login
 */
data class LoginErrorResponse(
    val status: String,      // "error"
    val message: String,
    val log_location: String?
)
