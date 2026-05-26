package com.example.stockapp.repositories

import android.content.Context
import com.example.stockapp.api.ApiClient
import com.example.stockapp.api.StockApiService
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.ActivityManager
import com.example.stockapp.models.StockResponse
import com.example.stockapp.models.StockDetailsResponse
import com.example.stockapp.models.StockSearchRequest
import com.example.stockapp.models.StockItem
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for stock-related API calls.
 *
 * Abstracts the API layer and provides methods for:
 *  - Searching stock by SKU (legacy)
 *  - Fetching detailed stock information (legacy)
 *  - Searching stock by activity with flexible criteria (Phase 7 Part 2)
 *
 * Uses [ApiClient] singleton to access the Retrofit service.
 */
class StockRepository(private val context: Context? = null) {

    private val apiService: StockApiService
        get() = ApiClient.apiService

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * Searches for stock information by SKU (legacy).
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
     * Fetches detailed stock information for a specific SKU (legacy).
     *
     * Makes an async call to GET /api/details?sku=<sku>
     *
     * @param sku The product SKU to fetch details for
     * @param callback Callback to handle response (success or failure)
     */
    fun getStockDetails(sku: String, callback: Callback<StockDetailsResponse>) {
        apiService.getStockDetails(sku).enqueue(callback)
    }

    /**
     * Searches stock by activity and flexible criteria (article code, location, storage number).
     *
     * Makes an async call to POST /api/stock/search with:
     *  - Activity code from ActivityManager
     *  - Search criteria (at least one required)
     *
     * @param artCode Article code (optional)
     * @param stkLieu Storage location (optional)
     * @param stkNosu Storage number (optional)
     * @param callback Callback with Result<List<StockItem>>
     */
    fun searchStock(
        artCode: String?,
        stkLieu: String?,
        stkNosu: String?,
        callback: (Result<List<StockItem>>) -> Unit
    ) {
        // Get selected activity code
        val actCode = context?.let { ActivityManager.getActivityCode(it) }
        if (actCode.isNullOrEmpty()) {
            AppLogger.log("[${getCurrentTimestamp()}] STOCK_SEARCH_FAILED error=no_activity_selected")
            callback(Result.failure(Exception("No activity selected")))
            return
        }

        // Build request
        val request = StockSearchRequest(
            artCode = artCode?.trim(),
            stkLieu = stkLieu?.trim(),
            stkNosu = stkNosu?.trim(),
            actCode = actCode
        )

        // Log request
        AppLogger.log(
            "[${getCurrentTimestamp()}] STOCK_SEARCH_REQUEST " +
            "act_code=$actCode art_code=$artCode stk_lieu=$stkLieu stk_nosu=$stkNosu"
        )

        // Make API call
        val call = apiService.searchStockByActivity(request)

        call.enqueue(object : retrofit2.Callback<com.example.stockapp.models.StockSearchResponse> {
            override fun onResponse(
                call: retrofit2.Call<com.example.stockapp.models.StockSearchResponse>,
                response: Response<com.example.stockapp.models.StockSearchResponse>
            ) {
                response.body()?.let { body ->
                    AppLogger.log(
                        "[${getCurrentTimestamp()}] STOCK_SEARCH_RESPONSE " +
                        "status=${body.status} items_count=${body.items.size}"
                    )

                    if (response.isSuccessful && body.status == "success") {
                        callback(Result.success(body.items))
                    } else {
                        callback(Result.failure(Exception(body.message)))
                    }
                } ?: run {
                    AppLogger.log("[${getCurrentTimestamp()}] STOCK_SEARCH_FAILED error=null_response")
                    callback(Result.failure(Exception("Empty response from server")))
                }
            }

            override fun onFailure(
                call: retrofit2.Call<com.example.stockapp.models.StockSearchResponse>,
                t: Throwable
            ) {
                AppLogger.log(
                    "[${getCurrentTimestamp()}] STOCK_SEARCH_FAILED " +
                    "error=${t.message?.take(100)}"
                )
                callback(Result.failure(t))
            }
        })
    }

    private fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }
}
