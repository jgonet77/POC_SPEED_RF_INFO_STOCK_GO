package com.example.stockapp.api

import android.content.Context
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that handles authorization and 401 responses.
 *
 * Responsibilities:
 *  1. Adds "Authorization: Bearer <token>" header to protected endpoints
 *  2. Detects 401 Unauthorized responses and notifies listener
 *  3. Clears expired token on 401 response
 *
 * Protected endpoints (with auth required):
 *  - /api/search
 *  - /api/details
 *
 * Unprotected endpoints (no auth header):
 *  - /api/login
 *  - /api/health/*
 *
 * @param context Application context for token operations
 * @param listener Callback to handle 401 Unauthorized responses
 * @param getTokenFn Optional token retriever (for testing with mock)
 * @param clearTokenFn Optional token clearer (for testing with mock)
 */
class AuthInterceptor(
    private val context: Context,
    private val listener: OnUnauthorizedListener? = null,
    private val getTokenFn: ((Context) -> String?)? = null,
    private val clearTokenFn: ((Context) -> Unit)? = null
) : Interceptor {

    interface OnUnauthorizedListener {
        fun onUnauthorized()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestUrl = originalRequest.url.toString()
        val endpoint = originalRequest.url.encodedPath

        // Check if this endpoint requires authentication
        if (isProtectedEndpoint(requestUrl)) {
            // Retrieve token using injected function or default TokenManager
            val token = getTokenFn?.invoke(context) ?: TokenManager.getToken(context)

            // If token exists, add Authorization header
            if (!token.isNullOrBlank()) {
                val authenticatedRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                // Log API request with auth header present
                AppLogger.log("API_REQUEST endpoint=$endpoint auth_header=present")

                val response = chain.proceed(authenticatedRequest)

                // Check for 401 Unauthorized
                if (response.code == 401) {
                    AppLogger.log("AUTH_401 endpoint=$endpoint error=Token expired or invalid")
                    handleUnauthorized()
                }

                return response
            }
        }

        // Unprotected endpoint or no token available - proceed without auth header
        // Log API request without auth header
        AppLogger.log("API_REQUEST endpoint=$endpoint auth_header=absent")

        val response = chain.proceed(originalRequest)

        // Still check for 401 (e.g., if endpoint became protected on server side)
        if (response.code == 401) {
            AppLogger.log("AUTH_401 endpoint=$endpoint error=Token expired or invalid")
            handleUnauthorized()
        }

        return response
    }

    /**
     * Determines whether the given URL requires authentication.
     *
     * @param url The full request URL
     * @return true if the endpoint is protected, false otherwise
     */
    fun isProtectedEndpoint(url: String): Boolean {
        return url.contains("/api/search") || url.contains("/api/details")
    }

    /**
     * Handles 401 Unauthorized response by:
     *  1. Calling the listener callback (if set)
     *  2. Clearing the stored token
     */
    private fun handleUnauthorized() {
        // Notify listener (e.g., to redirect to LoginActivity)
        listener?.onUnauthorized()

        // Clear the expired token using injected function or default TokenManager
        clearTokenFn?.invoke(context) ?: TokenManager.clearToken(context)
    }
}
