package com.example.stockapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.api.ApiClient
import com.example.stockapp.config.ConfigManager
import com.example.stockapp.logging.AppLogger

class SettingsActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var saveButton: Button
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)

        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        saveButton = findViewById(R.id.saveButton)
        retryButton = findViewById(R.id.retryButton)

        // Load current values
        hostInput.setText(configManager.getApiHost())
        portInput.setText(configManager.getApiPort())

        saveButton.setOnClickListener {
            val host = hostInput.text.toString().trim()
            val port = portInput.text.toString().trim()

            if (host.isEmpty() || port.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                AppLogger.log("Settings: Attempted to save with empty fields")
                return@setOnClickListener
            }

            if (!ConfigManager.isValidPort(port)) {
                Toast.makeText(this, "Port must be 1-65535", Toast.LENGTH_SHORT).show()
                AppLogger.log("Settings: Invalid port value: $port")
                return@setOnClickListener
            }

            configManager.setApiConfig(host, port)
            ApiClient.refreshApiUrl()
            AppLogger.log("Settings: Configuration saved to $host:$port")
            Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        retryButton.setOnClickListener {
            val host = hostInput.text.toString().trim()
            val port = portInput.text.toString().trim()

            if (host.isEmpty() || port.isEmpty()) {
                Toast.makeText(this, "Please fill in IP and port", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!ConfigManager.isValidPort(port)) {
                Toast.makeText(this, "Port must be 1-65535", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            configManager.setApiConfig(host, port)
            ApiClient.refreshApiUrl()
            AppLogger.log("Connection: Retrying connection to $host:$port")
            Toast.makeText(this, "Testing connection to $host:$port", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
