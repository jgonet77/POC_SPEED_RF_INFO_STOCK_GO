package com.example.stockapp.api

import com.example.stockapp.models.HealthCheckResponse
import com.example.stockapp.models.ApiHealthResponse
import com.example.stockapp.models.LoginRequest
import com.example.stockapp.models.LoginResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

interface StockApiService {

    @GET("api/health/database")
    fun checkDatabaseHealth(): Call<HealthCheckResponse>

    @GET("api/health/api")
    fun checkApiHealth(): Call<ApiHealthResponse>

    @POST("api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}
