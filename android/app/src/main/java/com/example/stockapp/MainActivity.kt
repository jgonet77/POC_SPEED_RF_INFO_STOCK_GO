package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.api.ApiClient
import com.example.stockapp.api.AuthInterceptor
import com.example.stockapp.databinding.ActivityMainBinding
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.TokenManager
import com.example.stockapp.managers.ActivityManager
import com.example.stockapp.viewmodels.HealthViewModel
import com.example.stockapp.viewmodels.ConnectionStatus
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), AuthInterceptor.OnUnauthorizedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: HealthViewModel

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

        // Check if activity has been selected
        if (!ActivityManager.hasActivity(this)) {
            // No activity selected, redirect to ActivitySelectionActivity
            val intent = Intent(this, ActivitySelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize app-wide singletons in background thread to avoid ANR
        thread(isDaemon = true) {
            AppLogger(this@MainActivity)
            // Pass this Activity as the 401 listener so AuthInterceptor can notify us
            ApiClient.init(this@MainActivity, this@MainActivity)
        }

        // Initialize ViewModel
        viewModel = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HealthViewModel(this@MainActivity) as T
            }
        }.let { ViewModelProvider(this, it).get(HealthViewModel::class.java) }

        // Setup connection status observers
        viewModel.connectionStatus.observe(this) { status ->
            updateConnectionUI(status)
        }

        viewModel.connectionDetails.observe(this) { details ->
            val activityLib = ActivityManager.getActivityLib(this)
            val activityInfo = if (activityLib != null) "Activity: $activityLib\n" else ""
            binding.connectionDetailsText.text = activityInfo + details
        }

        viewModel.waitTime.observe(this) { time ->
            binding.waitTimeText.text = time
        }

        // Setup health status observers
        viewModel.apiHealthStatus.observe(this) { status ->
            binding.apiStatusText.text = status
        }

        viewModel.apiTestTime.observe(this) { time ->
            binding.apiTestTimeText.text = time
        }

        viewModel.databaseHealthStatus.observe(this) { status ->
            binding.databaseStatusText.text = status
        }

        viewModel.databaseTestTime.observe(this) { time ->
            binding.databaseTestTimeText.text = time
        }

        viewModel.databaseVersion.observe(this) { version ->
            binding.databaseVersionText.text = if (version.isNotEmpty()) "Version: $version" else ""
        }

        viewModel.databaseTime.observe(this) { time ->
            binding.databaseTimeText.text = if (time.isNotEmpty()) "Server Time: $time" else ""
        }

        viewModel.errorMessage.observe(this) { error ->
            binding.errorText.text = if (error.isNotEmpty()) "Error: $error" else ""
        }

        // Setup button listeners
        binding.testApiButton.setOnClickListener {
            viewModel.checkApiHealth()
        }

        binding.testDatabaseButton.setOnClickListener {
            viewModel.checkDatabaseHealth()
        }

        binding.cancelConnectionButton.setOnClickListener {
            if (viewModel.connectionStatus.value == ConnectionStatus.ERROR) {
                viewModel.testConnection()
            } else {
                viewModel.cancelConnection()
            }
        }

        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.viewLogsButton.setOnClickListener {
            val intent = Intent(this, LogsViewerActivity::class.java)
            startActivity(intent)
        }

        binding.logoutButton.setOnClickListener {
            AppLogger.log("USER_LOGOUT button tapped")
            TokenManager.clearToken(this)
            ActivityManager.clearActivity(this)
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
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

    override fun onUnauthorized() {
        // Handle 401 Unauthorized response: redirect to login
        runOnUiThread {
            // Show toast notification to user
            Toast.makeText(
                this,
                "Session expired, please login again",
                Toast.LENGTH_LONG
            ).show()

            // Clear token (if not already cleared by interceptor)
            TokenManager.clearToken(this)

            // Redirect to LoginActivity
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun updateConnectionUI(status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.DISCONNECTED -> {
                binding.connectionStatusText.text = "Disconnected"
                binding.connectionSpinner.visibility = View.GONE
                binding.connectionStatusIcon.visibility = View.GONE
                binding.cancelConnectionButton.visibility = View.GONE
                binding.testApiButton.isEnabled = false
                binding.testDatabaseButton.isEnabled = false
            }
            ConnectionStatus.CONNECTING -> {
                binding.connectionStatusText.text = "Connecting..."
                binding.connectionSpinner.visibility = View.VISIBLE
                binding.connectionStatusIcon.visibility = View.GONE
                binding.cancelConnectionButton.visibility = View.VISIBLE
                binding.testApiButton.isEnabled = false
                binding.testDatabaseButton.isEnabled = false
            }
            ConnectionStatus.CONNECTED -> {
                binding.connectionStatusText.text = "Connected ✅"
                binding.connectionSpinner.visibility = View.GONE
                binding.connectionStatusIcon.visibility = View.VISIBLE
                binding.connectionStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                binding.cancelConnectionButton.visibility = View.GONE
                binding.testApiButton.isEnabled = true
                binding.testDatabaseButton.isEnabled = true
            }
            ConnectionStatus.ERROR -> {
                binding.connectionStatusText.text = "Connection Failed ❌"
                binding.connectionSpinner.visibility = View.GONE
                binding.connectionStatusIcon.visibility = View.VISIBLE
                binding.connectionStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                binding.cancelConnectionButton.text = "Retry"
                binding.cancelConnectionButton.visibility = View.VISIBLE
                binding.testApiButton.isEnabled = false
                binding.testDatabaseButton.isEnabled = false
            }
        }
    }
}
