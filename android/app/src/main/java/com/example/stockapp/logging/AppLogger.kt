package com.example.stockapp.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AppLogger(context: Context) {

    companion object {
        private const val TAG = "StockApp"
        private const val LOG_DIR = "stock_app_logs"
        private const val LOG_FILE_NAME = "debug.log"

        // Application context held for static helpers. Initialized on first
        // AppLogger construction. Using applicationContext avoids leaks.
        @Volatile
        private var appContext: Context? = null

        /**
         * Returns the log file used by AppLogger. Requires that an AppLogger
         * instance has been constructed at least once so the application
         * context is available.
         */
        fun getLogFile(): File {
            val ctx = appContext
                ?: throw IllegalStateException(
                    "AppLogger has not been initialized. " +
                        "Construct AppLogger(context) at app startup before calling getLogFile()."
                )
            val logsDir = File(ctx.getExternalFilesDir(null), LOG_DIR)
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }
            return File(logsDir, LOG_FILE_NAME)
        }

        /**
         * Convenience static logger that writes an INFO line via a fresh
         * AppLogger backed by the stored application context.
         */
        fun log(message: String) {
            val ctx = appContext ?: return
            AppLogger(ctx).info(message)
        }

        /**
         * Static API-call logging helpers. They forward to an AppLogger
         * instance backed by the stored application context. No-ops if
         * AppLogger has never been constructed.
         */
        fun logApiCall(endpoint: String, method: String = "GET") {
            val ctx = appContext ?: return
            AppLogger(ctx).logApiCall(endpoint, method)
        }

        fun logApiResponse(endpoint: String, statusCode: Int, responseBody: String) {
            val ctx = appContext ?: return
            AppLogger(ctx).logApiResponse(endpoint, statusCode, responseBody)
        }

        fun logApiError(endpoint: String, error: String) {
            val ctx = appContext ?: return
            AppLogger(ctx).logApiError(endpoint, error)
        }
    }

    private val logFile: File
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        // Store application context so static helpers can resolve the log path.
        if (appContext == null) {
            appContext = context.applicationContext
        }
        val logsDir = File(context.getExternalFilesDir(null), LOG_DIR)
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        logFile = File(logsDir, LOG_FILE_NAME)
    }

    fun info(message: String) {
        log("INFO", message)
    }

    fun error(message: String, exception: Exception? = null) {
        val fullMessage = if (exception != null) {
            "$message\n${exception.stackTraceToString()}"
        } else {
            message
        }
        log("ERROR", fullMessage)
    }

    fun debug(message: String) {
        log("DEBUG", message)
    }

    fun logApiCall(endpoint: String, method: String = "GET") {
        info("API CALL: $method $endpoint")
    }

    fun logApiResponse(endpoint: String, statusCode: Int, responseBody: String) {
        info("API RESPONSE: $endpoint - Status: $statusCode\nBody: $responseBody")
    }

    fun logApiError(endpoint: String, error: String) {
        this.error("API ERROR: $endpoint\n$error")
    }

    fun getLogs(): String {
        return if (logFile.exists()) {
            logFile.readText()
        } else {
            "No logs yet"
        }
    }

    fun clearLogs() {
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    private fun log(level: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] [$level] $message\n"

        // Also log to Android Logcat
        Log.println(
            when (level) {
                "INFO" -> Log.INFO
                "ERROR" -> Log.ERROR
                "DEBUG" -> Log.DEBUG
                else -> Log.DEBUG
            },
            TAG,
            message
        )

        // Append to file
        try {
            logFile.appendText(logLine)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log: ${e.message}")
        }
    }
}
