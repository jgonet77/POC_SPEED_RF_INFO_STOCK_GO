package com.example.stockapp.api

import android.content.Context
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val context: Context,
    private val listener: OnUnauthorizedListener? = null
) : Interceptor {

    interface OnUnauthorizedListener {
        fun onUnauthorized()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val endpoint = originalRequest.url.encodedPath

        // Check if this endpoint requires authentication
        if (isProtectedEndpoint(originalRequest.url.toString())) {
            val token = TokenManager.getToken(context)

            if (!token.isNullOrBlank()) {
                val authenticatedRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                AppLogger.log("API_REQUEST endpoint=$endpoint auth_header=present")
                val response = chain.proceed(authenticatedRequest)

                if (response.code == 401) {
                    AppLogger.log("AUTH_401 endpoint=$endpoint error=Token expired or invalid")
                    listener?.onUnauthorized()
                    TokenManager.clearToken(context)
                }

                return response
            }
        }

        AppLogger.log("API_REQUEST endpoint=$endpoint auth_header=absent")
        val response = chain.proceed(originalRequest)

        if (response.code == 401) {
            AppLogger.log("AUTH_401 endpoint=$endpoint error=Token expired or invalid")
            listener?.onUnauthorized()
            TokenManager.clearToken(context)
        }

        return response
    }

    private fun isProtectedEndpoint(url: String): Boolean {
        return url.contains("/api/search") || url.contains("/api/details")
    }
}
