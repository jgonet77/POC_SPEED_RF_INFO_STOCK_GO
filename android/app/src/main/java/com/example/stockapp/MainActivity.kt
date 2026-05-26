package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.api.ApiClient
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.TokenManager
import com.example.stockapp.viewmodels.HealthViewModel
import com.example.stockapp.viewmodels.ConnectionStatus
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HealthViewModel

    private lateinit var connectionSpinner: ProgressBar
    private lateinit var connectionStatusIcon: ImageView
    private lateinit var connectionStatusText: TextView
    private lateinit var connectionDetailsText: TextView
    private lateinit var waitTimeText: TextView
    private lateinit var cancelConnectionButton: Button
    private lateinit var apiStatusText: TextView
    private lateinit var apiTestTimeText: TextView
    private lateinit var databaseStatusText: TextView
    private lateinit var databaseTestTimeText: TextView
    private lateinit var databaseVersionText: TextView
    private lateinit var databaseTimeText: TextView
    private lateinit var errorText: TextView
    private lateinit var testApiButton: Button
    private lateinit var testDatabaseButton: Button
    private lateinit var settingsButton: Button
    private lateinit var viewLogsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check token validity before showing MainActivity
        val token = TokenManager.getToken(this)
        if (token == null) {
            // Token is missing or expired, redirect to login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Initialize app-wide singletons in background thread to avoid ANR
        thread(isDaemon = true) {
            AppLogger(this@MainActivity)
            ApiClient.init(this@MainActivity)
        }

        // Initialize ViewModel
        viewModel = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HealthViewModel(this@MainActivity) as T
            }
        }.let { ViewModelProvider(this, it).get(HealthViewModel::class.java) }

        // Find views
        connectionSpinner = findViewById(R.id.connectionSpinner)
        connectionStatusIcon = findViewById(R.id.connectionStatusIcon)
        connectionStatusText = findViewById(R.id.connectionStatusText)
        connectionDetailsText = findViewById(R.id.connectionDetailsText)
        waitTimeText = findViewById(R.id.waitTimeText)
        cancelConnectionButton = findViewById(R.id.cancelConnectionButton)
        apiStatusText = findViewById(R.id.apiStatusText)
        apiTestTimeText = findViewById(R.id.apiTestTimeText)
        databaseStatusText = findViewById(R.id.databaseStatusText)
        databaseTestTimeText = findViewById(R.id.databaseTestTimeText)
        databaseVersionText = findViewById(R.id.databaseVersionText)
        databaseTimeText = findViewById(R.id.databaseTimeText)
        errorText = findViewById(R.id.errorText)
        testApiButton = findViewById(R.id.testApiButton)
        testDatabaseButton = findViewById(R.id.testDatabaseButton)
        settingsButton = findViewById(R.id.settingsButton)
        viewLogsButton = findViewById(R.id.viewLogsButton)

        // Setup connection status observers
        viewModel.connectionStatus.observe(this) { status ->
            updateConnectionUI(status)
        }

        viewModel.connectionDetails.observe(this) { details ->
            connectionDetailsText.text = details
        }

        viewModel.waitTime.observe(this) { time ->
            waitTimeText.text = time
        }

        // Setup health status observers
        viewModel.apiHealthStatus.observe(this) { status ->
            apiStatusText.text = status
        }

        viewModel.apiTestTime.observe(this) { time ->
            apiTestTimeText.text = time
        }

        viewModel.databaseHealthStatus.observe(this) { status ->
            databaseStatusText.text = status
        }

        viewModel.databaseTestTime.observe(this) { time ->
            databaseTestTimeText.text = time
        }

        viewModel.databaseVersion.observe(this) { version ->
            databaseVersionText.text = if (version.isNotEmpty()) "Version: $version" else ""
        }

        viewModel.databaseTime.observe(this) { time ->
            databaseTimeText.text = if (time.isNotEmpty()) "Server Time: $time" else ""
        }

        viewModel.errorMessage.observe(this) { error ->
            errorText.text = if (error.isNotEmpty()) "Error: $error" else ""
        }

        // Setup button listeners
        testApiButton.setOnClickListener {
            viewModel.checkApiHealth()
        }

        testDatabaseButton.setOnClickListener {
            viewModel.checkDatabaseHealth()
        }

        cancelConnectionButton.setOnClickListener {
            if (viewModel.connectionStatus.value == ConnectionStatus.ERROR) {
                viewModel.testConnection()
            } else {
                viewModel.cancelConnection()
            }
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        viewLogsButton.setOnClickListener {
            val intent = Intent(this, LogsViewerActivity::class.java)
            startActivity(intent)
        }

        // Start connection test on app launch
        viewModel.testConnection()
    }

    override fun onResume() {
        super.onResume()
        // Re-test connection when returning from Settings
        if (viewModel.connectionStatus.value == ConnectionStatus.DISCONNECTED ||
            viewModel.connectionStatus.value == ConnectionStatus.ERROR) {
            viewModel.testConnection()
        }
    }

    private fun updateConnectionUI(status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.DISCONNECTED -> {
                connectionStatusText.text = "Disconnected"
                connectionSpinner.visibility = View.GONE
                connectionStatusIcon.visibility = View.GONE
                cancelConnectionButton.visibility = View.GONE
                testApiButton.isEnabled = false
                testDatabaseButton.isEnabled = false
            }
            ConnectionStatus.CONNECTING -> {
                connectionStatusText.text = "Connecting..."
                connectionSpinner.visibility = View.VISIBLE
                connectionStatusIcon.visibility = View.GONE
                cancelConnectionButton.visibility = View.VISIBLE
                testApiButton.isEnabled = false
                testDatabaseButton.isEnabled = false
            }
            ConnectionStatus.CONNECTED -> {
                connectionStatusText.text = "Connected ✅"
                connectionSpinner.visibility = View.GONE
                connectionStatusIcon.visibility = View.VISIBLE
                connectionStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                cancelConnectionButton.visibility = View.GONE
                testApiButton.isEnabled = true
                testDatabaseButton.isEnabled = true
            }
            ConnectionStatus.ERROR -> {
                connectionStatusText.text = "Connection Failed ❌"
                connectionSpinner.visibility = View.GONE
                connectionStatusIcon.visibility = View.VISIBLE
                connectionStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                cancelConnectionButton.text = "Retry"
                cancelConnectionButton.visibility = View.VISIBLE
                testApiButton.isEnabled = false
                testDatabaseButton.isEnabled = false
            }
        }
    }
}
