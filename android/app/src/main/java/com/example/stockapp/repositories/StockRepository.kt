package com.example.stockapp.repositories

import android.content.Context
import com.example.stockapp.api.ApiClient
import com.example.stockapp.api.StockApiService
import com.example.stockapp.models.StockResponse
import com.example.stockapp.models.StockDetailsResponse
import retrofit2.Callback

/**
 * Repository for stock-related API calls.
 *
 * Abstracts the API layer and provides methods for:
 *  - Searching stock by SKU
 *  - Fetching detailed stock information
 *
 * Uses [ApiClient] singleton to access the Retrofit service.
 */
class StockRepository(private val context: Context) {

    private val apiService: StockApiService
        get() = ApiClient.apiService

    /**
     * Searches for stock information by SKU.
     *
     * Makes an async call to GET /api/search?sku=<sku>
     *
     * @param sku The product SKU to search for
     * @param callback Callback to handle response (success or failure)
     */
    fun searchStock(sku: String, callback: Callback<StockResponse>) {
        apiService.searchStock(sku).enqueue(callback)
    }

    /**
     * Fetches detailed stock information for a specific SKU.
     *
     * Makes an async call to GET /api/details?sku=<sku>
     *
     * @param sku The product SKU to fetch details for
     * @param callback Callback to handle response (success or failure)
     */
    fun getStockDetails(sku: String, callback: Callback<StockDetailsResponse>) {
        apiService.getStockDetails(sku).enqueue(callback)
    }
}
