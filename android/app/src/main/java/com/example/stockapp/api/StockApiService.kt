package com.example.stockapp.api

import com.example.stockapp.models.HealthCheckResponse
import com.example.stockapp.models.ApiHealthResponse
import retrofit2.Call
import retrofit2.http.GET

interface StockApiService {

    @GET("api/health/database")
    fun checkDatabaseHealth(): Call<HealthCheckResponse>

    @GET("api/health/api")
    fun checkApiHealth(): Call<ApiHealthResponse>
}
