package com.example.stockapp.config

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "stock_app_config"
        private const val KEY_API_HOST = "api_host"
        private const val KEY_API_PORT = "api_port"
        const val DEFAULT_HOST = "192.168.1.20"
        const val DEFAULT_PORT = "8000"

        fun isValidPort(port: String): Boolean {
            val n = port.toIntOrNull() ?: return false
            return n in 1..65535
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiUrl(): String {
        val host = getApiHost()
        val port = getApiPort()
        return "http://$host:$port/"
    }

    fun getApiHost(): String {
        return prefs.getString(KEY_API_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
    }

    fun getApiPort(): String {
        return prefs.getString(KEY_API_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
    }

    fun resetToDefaults() {
        setApiConfig(DEFAULT_HOST, DEFAULT_PORT)
    }

    fun setApiConfig(host: String, port: String) {
        prefs.edit().apply {
            putString(KEY_API_HOST, host)
            putString(KEY_API_PORT, port)
            apply()
        }
    }
}
