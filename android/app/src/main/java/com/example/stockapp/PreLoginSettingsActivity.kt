package com.example.stockapp

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.api.ApiClient
import com.example.stockapp.config.ConfigManager
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.utils.TestConnectionCallback
import com.example.stockapp.utils.TestConnectionHelper

/**
 * Pre-Login Settings Activity for API configuration.
 *
 * Allows users to configure API host and port before logging in.
 * Provides test connection functionality and access to test connection logs.
 */
class PreLoginSettingsActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var testHelper: TestConnectionHelper
    private lateinit var appLogger: AppLogger

    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var saveButton: Button
    private lateinit var testButton: Button
    private lateinit var logsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_login_settings)

        // Initialize managers
        appLogger = AppLogger(this)
        configManager = ConfigManager(this)
        testHelper = TestConnectionHelper(this)

        // Find UI elements
        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        saveButton = findViewById(R.id.saveButton)
        testButton = findViewById(R.id.testButton)
        logsButton = findViewById(R.id.logsButton)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Load current configuration
        hostInput.setText(configManager.getApiHost())
        portInput.setText(configManager.getApiPort())

        // Setup button listeners
        saveButton.setOnClickListener { handleSaveClick() }
        testButton.setOnClickListener { handleTestClick() }
        logsButton.setOnClickListener { handleLogsClick() }
    }

    private fun handleSaveClick() {
        // Validate inputs
        if (!validateInputs()) {
            return
        }

        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        // Save configuration
        configManager.setApiConfig(host, port)
        ApiClient.refreshApiUrl()

        // Log success
        appLogger.info("PreLoginSettings: Configuration saved to $host:$port")

        // Show confirmation
        Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()

        // Close activity
        finish()
    }

    private fun handleTestClick() {
        // Validate inputs
        if (!validateInputs()) {
            return
        }

        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        // Set config temporarily for testing
        configManager.setApiConfig(host, port)
        ApiClient.refreshApiUrl()

        // Log test start
        appLogger.info("PreLoginSettings: Testing connection to $host:$port")

        // Show testing message
        Toast.makeText(this, "Testing connection...", Toast.LENGTH_SHORT).show()

        // Run connection test (async)
        testHelper.runConnectionTest(object : TestConnectionCallback {
            override fun onTestComplete(success: Boolean, summary: String) {
                runOnUiThread {
                    Toast.makeText(
                        this@PreLoginSettingsActivity,
                        if (success) "Connection successful: $summary"
                        else "Connection failed: $summary",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun handleLogsClick() {
        val logs = testHelper.getTestLogs()

        if (logs.isEmpty()) {
            Toast.makeText(
                this,
                "No test logs available - run a test first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Show logs dialog
        AlertDialog.Builder(this)
            .setTitle("Test Connection Logs")
            .setMessage(logs.joinToString("\n"))
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Clear Logs") { dialog, _ ->
                testHelper.clearTestLogs()
                Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun validateInputs(): Boolean {
        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        // Check host not empty
        if (host.isEmpty()) {
            appLogger.info("PreLoginSettings: Validation failed - host is empty")
            Toast.makeText(this, "Host cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check port not empty
        if (port.isEmpty()) {
            appLogger.info("PreLoginSettings: Validation failed - port is empty")
            Toast.makeText(this, "Port cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check port is valid
        if (!ConfigManager.isValidPort(port)) {
            appLogger.info("PreLoginSettings: Validation failed - invalid port: $port")
            Toast.makeText(this, "Port must be between 1 and 65535", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
