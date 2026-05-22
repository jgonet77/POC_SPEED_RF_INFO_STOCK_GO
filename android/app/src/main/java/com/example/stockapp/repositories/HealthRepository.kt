package com.example.stockapp.repositories

import android.util.Log
import com.example.stockapp.api.ApiClient
import com.example.stockapp.models.HealthCheckResponse
import com.example.stockapp.models.ApiHealthResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HealthRepository {

    fun checkDatabaseHealth(
        onSuccess: (HealthCheckResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.checkDatabaseHealth().enqueue(object : Callback<HealthCheckResponse> {
            override fun onResponse(call: Call<HealthCheckResponse>, response: Response<HealthCheckResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    onError("API Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<HealthCheckResponse>, t: Throwable) {
                onError("Connection Error: ${t.message}")
                Log.e("HealthRepository", "Database health check failed", t)
            }
        })
    }

    fun checkApiHealth(
        onSuccess: (ApiHealthResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.checkApiHealth().enqueue(object : Callback<ApiHealthResponse> {
            override fun onResponse(call: Call<ApiHealthResponse>, response: Response<ApiHealthResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    onError("API Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ApiHealthResponse>, t: Throwable) {
                onError("Connection Error: ${t.message}")
                Log.e("HealthRepository", "API health check failed", t)
            }
        })
    }
}
