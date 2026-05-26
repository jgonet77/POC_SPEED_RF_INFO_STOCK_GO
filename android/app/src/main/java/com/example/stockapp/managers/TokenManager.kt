package com.example.stockapp.managers

import android.content.Context

/**
 * Manages JWT token persistence and expiry validation.
 *
 * Stores token and expiry time in SharedPreferences under the "auth" namespace.
 * Automatically validates token expiry on retrieval.
 */
object TokenManager {

    private const val PREFS_NAME = "auth"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_AUTH_TOKEN_EXPIRY = "auth_token_expiry"

    /**
     * Retrieves the stored JWT token if it exists and has not expired.
     * If the token is expired, it is automatically cleared and null is returned.
     *
     * @param context Application context
     * @return The JWT token string, or null if not stored or expired
     */
    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        val expiry = prefs.getLong(KEY_AUTH_TOKEN_EXPIRY, 0)

        // Check if token is null first, then check if expired
        if (token == null || System.currentTimeMillis() >= expiry) {
            clearToken(context)
            return null
        }

        return token
    }

    /**
     * Saves a JWT token and its expiry time to SharedPreferences.
     * The expiry time is calculated as current time + (expiresIn * 1000) milliseconds.
     *
     * @param context Application context
     * @param token The JWT token string
     * @param expiresIn Expiry time in seconds (typically 86400 for 24 hours)
     */
    fun saveToken(context: Context, token: String, expiresIn: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putLong(KEY_AUTH_TOKEN_EXPIRY, System.currentTimeMillis() + (expiresIn * 1000L))
            apply()
        }
    }

    /**
     * Clears the stored token from SharedPreferences.
     * Used when token is expired or user logs out.
     *
     * @param context Application context
     */
    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_AUTH_TOKEN_EXPIRY)
            apply()
        }
    }
}
