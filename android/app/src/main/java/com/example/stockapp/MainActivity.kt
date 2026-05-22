package com.example.stockapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.viewmodels.HealthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HealthViewModel

    private lateinit var apiStatusText: TextView
    private lateinit var databaseStatusText: TextView
    private lateinit var databaseVersionText: TextView
    private lateinit var databaseTimeText: TextView
    private lateinit var errorText: TextView
    private lateinit var testApiButton: Button
    private lateinit var testDatabaseButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(HealthViewModel::class.java)

        // Find views
        apiStatusText = findViewById(R.id.apiStatusText)
        databaseStatusText = findViewById(R.id.databaseStatusText)
        databaseVersionText = findViewById(R.id.databaseVersionText)
        databaseTimeText = findViewById(R.id.databaseTimeText)
        errorText = findViewById(R.id.errorText)
        testApiButton = findViewById(R.id.testApiButton)
        testDatabaseButton = findViewById(R.id.testDatabaseButton)

        // Setup observers
        viewModel.apiHealthStatus.observe(this) { status ->
            apiStatusText.text = status
        }

        viewModel.databaseHealthStatus.observe(this) { status ->
            databaseStatusText.text = status
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
    }
}
