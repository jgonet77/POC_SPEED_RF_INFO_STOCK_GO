package com.example.stockapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Change this to your backend server IP/hostname
    private const val BASE_URL = "http://10.0.2.2:8000/"  // 10.0.2.2 for Android emulator

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: StockApiService by lazy {
        retrofit.create(StockApiService::class.java)
    }
}
