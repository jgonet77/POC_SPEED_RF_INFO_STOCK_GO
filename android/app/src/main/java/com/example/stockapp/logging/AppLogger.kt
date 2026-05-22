package com.example.stockapp.logging

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
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

        /**
         * Static clearLogs helper to delete the log file.
         */
        fun clearLogs() {
            try {
                getLogFile().delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs: ${e.message}")
            }
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
        val truncatedBody = if (responseBody.length > 200) {
            responseBody.take(200) + "..."
        } else {
            responseBody
        }
        info("API RESPONSE: $endpoint - Status: $statusCode\nBody: $truncatedBody")
    }

    fun logApiError(endpoint: String, error: String) {
        this.error("API ERROR: $endpoint\n$error")
    }

    /**
     * Reads the last N lines from the log file in memory-efficient manner.
     * Avoids loading entire file which could cause OOM on large logs.
     * @param maxLines Maximum number of lines to return (default: 500)
     * @return The last N lines as a single string
     */
    fun getLogs(maxLines: Int = 500): String {
        if (!logFile.exists()) return "No logs yet"

        val lines = mutableListOf<String>()
        return try {
            BufferedReader(FileReader(logFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lines.add(line!!)
                    // Keep only last maxLines by removing the oldest
                    if (lines.size > maxLines) {
                        lines.removeAt(0)
                    }
                }
            }
            if (lines.isEmpty()) "No logs yet" else lines.joinToString("\n")
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }

    /**
     * Instance method to clear logs. Use AppLogger.clearLogs() for static access.
     */
    fun clearLogsInstance() {
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    /**
     * Count lines in the log file without loading entire content into memory.
     */
    private fun countLogLines(): Int {
        if (!logFile.exists()) return 0
        return try {
            BufferedReader(FileReader(logFile)).use { reader ->
                var count = 0
                while (reader.readLine() != null) {
                    count++
                }
                count
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Rotate log file if it exceeds the maximum line threshold.
     * When file has more than 2000 lines, it is deleted to restart fresh.
     */
    private fun rotateLogIfNeeded() {
        try {
            if (countLogLines() > 2000) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log: ${e.message}")
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

        // Append to file and check for rotation
        try {
            logFile.appendText(logLine)
            rotateLogIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log: ${e.message}")
        }
    }
}
