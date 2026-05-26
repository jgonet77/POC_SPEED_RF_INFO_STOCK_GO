package com.example.stockapp.api

import com.example.stockapp.models.HealthCheckResponse
import com.example.stockapp.models.ApiHealthResponse
import com.example.stockapp.models.LoginRequest
import com.example.stockapp.models.LoginResponse
import com.example.stockapp.models.StockResponse
import com.example.stockapp.models.StockDetailsResponse
import com.example.stockapp.models.ActivityListResponse
import com.example.stockapp.models.StockSearchRequest
import com.example.stockapp.models.StockSearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query

interface StockApiService {

    @GET("api/health/database")
    fun checkDatabaseHealth(): Call<HealthCheckResponse>

    @GET("api/health/api")
    fun checkApiHealth(): Call<ApiHealthResponse>

    @POST("api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/search")
    fun searchStock(@Query("sku") sku: String): Call<StockResponse>

    @GET("api/details")
    fun getStockDetails(@Query("sku") sku: String): Call<StockDetailsResponse>

    @GET("api/activities")
    fun getActivities(): Call<ActivityListResponse>

    @POST("/api/stock/search")
    fun searchStockByActivity(@Body request: StockSearchRequest): Call<StockSearchResponse>
}
