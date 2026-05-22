package com.example.stockapp.repositories

import android.util.Log
import com.example.stockapp.api.ApiClient
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.models.ApiHealthResponse
import com.example.stockapp.models.HealthCheckResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private class HealthCheckCallback(
    val endpoint: String,
    val onSuccess: (HealthCheckResponse) -> Unit,
    val onError: (String) -> Unit
) : Callback<HealthCheckResponse> {
    override fun onResponse(call: Call<HealthCheckResponse>, response: Response<HealthCheckResponse>) {
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            AppLogger.logApiResponse(endpoint, response.code(), body.toString())
            onSuccess(body)
        } else {
            val errorMsg = "API Error: ${response.code()} ${response.message()}"
            AppLogger.logApiError(endpoint, errorMsg)
            onError(errorMsg)
        }
    }

    override fun onFailure(call: Call<HealthCheckResponse>, t: Throwable) {
        val errorMsg = "Connection Error: ${t.message}"
        AppLogger.logApiError(endpoint, errorMsg)
        onError(errorMsg)
        Log.e("HealthRepository", "Database health check failed", t)
    }
}

private class ApiHealthCallback(
    val endpoint: String,
    val onSuccess: (ApiHealthResponse) -> Unit,
    val onError: (String) -> Unit
) : Callback<ApiHealthResponse> {
    override fun onResponse(call: Call<ApiHealthResponse>, response: Response<ApiHealthResponse>) {
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            AppLogger.logApiResponse(endpoint, response.code(), body.toString())
            onSuccess(body)
        } else {
            val errorMsg = "API Error: ${response.code()} ${response.message()}"
            AppLogger.logApiError(endpoint, errorMsg)
            onError(errorMsg)
        }
    }

    override fun onFailure(call: Call<ApiHealthResponse>, t: Throwable) {
        val errorMsg = "Connection Error: ${t.message}"
        AppLogger.logApiError(endpoint, errorMsg)
        onError(errorMsg)
        Log.e("HealthRepository", "API health check failed", t)
    }
}

class HealthRepository {

    fun checkDatabaseHealth(
        onSuccess: (HealthCheckResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val endpoint = "api/health/database"
        AppLogger.logApiCall(endpoint, "GET")
        val callback: Callback<HealthCheckResponse> = HealthCheckCallback(endpoint, onSuccess, onError)
        ApiClient.apiService.checkDatabaseHealth().enqueue(callback)
    }

    fun checkApiHealth(
        onSuccess: (ApiHealthResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val endpoint = "api/health/api"
        AppLogger.logApiCall(endpoint, "GET")
        val callback: Callback<ApiHealthResponse> = ApiHealthCallback(endpoint, onSuccess, onError)
        ApiClient.apiService.checkApiHealth().enqueue(callback)
    }
}
