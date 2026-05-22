package com.example.stockapp.api

import retrofit2.Call
import retrofit2.http.GET

interface StockApiService {

    @GET("api/health/database")
    fun checkDatabaseHealth(): Call<HealthCheckResponse>

    @GET("api/health/api")
    fun checkApiHealth(): Call<ApiHealthResponse>
}

data class HealthCheckResponse(
    val service: String,
    val database_status: String,
    val details: Map<String, Any>
)

data class ApiHealthResponse(
    val status: String,
    val message: String
)
