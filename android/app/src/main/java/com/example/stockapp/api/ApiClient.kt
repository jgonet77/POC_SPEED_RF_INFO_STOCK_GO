package com.example.stockapp.api

import android.content.Context
import com.example.stockapp.config.ConfigManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton Retrofit client.
 *
 * The base URL is read from [ConfigManager] at initialization time and can be
 * refreshed at runtime via [refreshApiUrl] (e.g. after the user changes the
 * host/port in Settings).
 *
 * Call [init] from your Application or main Activity's `onCreate` before any
 * use of [apiService]. A defensive fallback URL is used if [init] was never
 * called, so existing callers remain functional.
 */
object ApiClient {

    // Fallback URL used only if init() was never called (e.g. unit tests).
    private const val FALLBACK_BASE_URL = "http://10.0.2.2:8000/"

    private var appContext: Context? = null

    @Volatile
    private var _retrofit: Retrofit? = null

    @Volatile
    private var _apiService: StockApiService? = null

    /**
     * Initializes the client with an application context so that the base URL
     * can be resolved dynamically from [ConfigManager].
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        _retrofit = createRetrofit()
        _apiService = _retrofit!!.create(StockApiService::class.java)
    }

    /**
     * Recreates the underlying Retrofit instance using the latest URL from
     * [ConfigManager]. Call this after the user updates host/port in settings.
     */
    fun refreshApiUrl() {
        _retrofit = createRetrofit()
        _apiService = _retrofit!!.create(StockApiService::class.java)
    }

    private fun createRetrofit(): Retrofit {
        val baseUrl = resolveBaseUrl()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun resolveBaseUrl(): String {
        val ctx = appContext
        return if (ctx != null) {
            ConfigManager(ctx).getApiUrl()
        } else {
            FALLBACK_BASE_URL
        }
    }

    val retrofit: Retrofit
        get() {
            return _retrofit ?: synchronized(this) {
                _retrofit ?: createRetrofit().also { _retrofit = it }
            }
        }

    val apiService: StockApiService
        get() {
            return _apiService ?: synchronized(this) {
                _apiService ?: retrofit.create(StockApiService::class.java).also {
                    _apiService = it
                }
            }
        }
}
