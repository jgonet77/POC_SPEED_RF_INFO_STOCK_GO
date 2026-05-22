package com.example.stockapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.api.ApiClient
import com.example.stockapp.config.ConfigManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        configManager = ConfigManager(this)

        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        saveButton = findViewById(R.id.saveButton)

        // Load current values
        hostInput.setText(configManager.getApiHost())
        portInput.setText(configManager.getApiPort())

        saveButton.setOnClickListener {
            val host = hostInput.text.toString().trim()
            val port = portInput.text.toString().trim()

            if (host.isEmpty() || port.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate port is a number
            if (!port.matches(Regex("\\d+"))) {
                Toast.makeText(this, "Port must be a number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            configManager.setApiConfig(host, port)
            // Rebuild the Retrofit client so subsequent calls hit the new URL.
            ApiClient.refreshApiUrl()
            Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
