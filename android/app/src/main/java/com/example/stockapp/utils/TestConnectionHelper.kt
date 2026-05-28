package com.example.stockapp.utils

import android.content.Context
import com.example.stockapp.api.ApiClient
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.models.ApiHealthResponse
import com.example.stockapp.models.HealthCheckResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.Collections

/**
 * Test Connection Helper Interface for diagnostic callback.
 *
 * Provides functionality to test API connectivity and database health,
 * logging all operations for debugging purposes.
 */
interface TestConnectionCallback {
    fun onTestComplete(success: Boolean, summary: String)
}

/**
 * Diagnostic utility for testing API and database connectivity.
 *
 * Executes a 2-step health check:
 * 1. API health test (checks if server is reachable)
 * 2. Database health test (checks if database is accessible)
 *
 * All test operations are logged with timestamps for debugging.
 * Logs persist during the session and can be retrieved or cleared.
 *
 * @param context Android application context
 */
class TestConnectionHelper(private val context: Context) {

    private val testLogs = Collections.synchronizedList(mutableListOf<String>())

    /**
     * Executes a 2-step connection diagnostic test.
     *
     * Process:
     * 1. Starts overall test and clears logs
     * 2. Tests API health endpoint
     * 3. If API succeeds, tests database health endpoint
     * 4. If API fails, skips database test
     * 5. Calls callback with final summary
     *
     * @param callback Receives test completion status and summary
     */
    fun runConnectionTest(callback: TestConnectionCallback) {
        testLogs.clear()
        logTest("OVERALL_TEST_START", "Starting connection diagnostic")

        testApiHealth { apiSuccess ->
            if (apiSuccess) {
                testDatabaseHealth { dbSuccess ->
                    val summary = buildSummary(apiSuccess = true, dbSuccess = dbSuccess)
                    callback.onTestComplete(dbSuccess, summary)
                }
            } else {
                logTest("DB_TEST_SKIPPED", "Skipping database test due to API failure")
                val summary = buildSummary(apiSuccess = false, dbSuccess = false)
                callback.onTestComplete(false, summary)
            }
        }
    }

    /**
     * Tests API health endpoint.
     *
     * Makes a request to /health/api endpoint to verify the API server
     * is running and reachable.
     *
     * @param callback Returns true if API is healthy, false otherwise
     */
    private fun testApiHealth(callback: (Boolean) -> Unit) {
        logTest("API_TEST_START", "Testing API health endpoint")

        ApiClient.apiService.checkApiHealth().enqueue(
            object : Callback<ApiHealthResponse> {
                override fun onResponse(
                    call: Call<ApiHealthResponse>,
                    response: Response<ApiHealthResponse>
                ) {
                    if (response.isSuccessful) {
                        logTest("API_TEST_SUCCESS", "API health check passed with status ${response.code()}")
                        callback(true)
                    } else {
                        logTest("API_TEST_FAILURE", "API returned status ${response.code()}: ${response.message()}")
                        callback(false)
                    }
                }

                override fun onFailure(call: Call<ApiHealthResponse>, t: Throwable) {
                    logTest("API_TEST_FAILURE", "API test failed: ${t.message ?: "Unknown error"}")
                    callback(false)
                }
            }
        )
    }

    /**
     * Tests database health endpoint.
     *
     * Makes a request to /health/database endpoint to verify the database
     * connection and accessibility.
     *
     * @param callback Returns true if database is healthy, false otherwise
     */
    private fun testDatabaseHealth(callback: (Boolean) -> Unit) {
        logTest("DB_TEST_START", "Testing database health endpoint")

        ApiClient.apiService.checkDatabaseHealth().enqueue(
            object : Callback<HealthCheckResponse> {
                override fun onResponse(
                    call: Call<HealthCheckResponse>,
                    response: Response<HealthCheckResponse>
                ) {
                    if (response.isSuccessful) {
                        logTest("DB_TEST_SUCCESS", "Database health check passed with status ${response.code()}")
                        callback(true)
                    } else {
                        logTest("DB_TEST_FAILURE", "Database returned status ${response.code()}: ${response.message()}")
                        callback(false)
                    }
                }

                override fun onFailure(call: Call<HealthCheckResponse>, t: Throwable) {
                    logTest("DB_TEST_FAILURE", "Database test failed: ${t.message ?: "Unknown error"}")
                    callback(false)
                }
            }
        )
    }

    /**
     * Logs a test event with timestamp and details.
     *
     * Format: [timestamp] type details
     * Logs are stored in memory and also written to AppLogger for file persistence.
     *
     * @param type Event type identifier (e.g. "API_TEST_START")
     * @param details Description of the event
     */
    private fun logTest(type: String, details: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val logEntry = "[$timestamp] $type: $details"
        testLogs.add(logEntry)
        AppLogger.log("TestConnection: $logEntry")
    }

    /**
     * Retrieves all test logs generated during the current session.
     *
     * @return Immutable copy of the test logs list
     */
    fun getTestLogs(): List<String> {
        return testLogs.toList()
    }

    /**
     * Clears all test logs from memory.
     *
     * Note: Logs written to file via AppLogger are not affected.
     */
    fun clearTestLogs() {
        testLogs.clear()
        logTest("LOGS_CLEARED", "Test logs cleared from memory")
    }

    /**
     * Builds a summary string for the test results.
     *
     * @param apiSuccess Whether API health check passed
     * @param dbSuccess Whether database health check passed
     * @return Human-readable summary of test results
     */
    private fun buildSummary(apiSuccess: Boolean, dbSuccess: Boolean): String {
        return when {
            apiSuccess && dbSuccess -> "All systems operational (API and Database)"
            apiSuccess && !dbSuccess -> "API operational but Database issue detected"
            !apiSuccess && !dbSuccess -> "Connection failed - API unreachable"
            else -> "Unexpected test state"
        }
    }
}
