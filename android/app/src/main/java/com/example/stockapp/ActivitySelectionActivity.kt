package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.api.ApiClient
import com.example.stockapp.databinding.ActivitySelectionBinding
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.ActivityManager
import com.example.stockapp.models.ActivityItem
import com.example.stockapp.viewmodels.ActivitySelectionViewModel

/**
 * Activity for selecting which activity to work with.
 *
 * Loads available activities from API. If only 1 activity exists, it auto-selects
 * and navigates to StockSearchActivity. If multiple activities exist, user selects
 * from a spinner before navigating.
 */
class ActivitySelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectionBinding
    private lateinit var viewModel: ActivitySelectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize app-wide singletons
        AppLogger(this)
        ApiClient.init(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize ViewModel and observe activities
        viewModel = ViewModelProvider(this).get(ActivitySelectionViewModel::class.java)
        viewModel.activities.observe(this) { activities ->
            handleActivitiesLoaded(activities)
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                binding.statusMessageTextView.text = error
                binding.statusMessageTextView.setTextColor(getColor(R.color.error_red))
                binding.confirmButton.isEnabled = false
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.confirmButton.isEnabled = !isLoading
            binding.cancelButton.isEnabled = !isLoading
            binding.activitySpinner.isEnabled = !isLoading
            binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Setup cancel button
        binding.cancelButton.setOnClickListener { exitActivity() }

        // Load activities on startup
        viewModel.loadActivities()
    }

    private fun handleActivitiesLoaded(activities: List<ActivityItem>) {
        if (activities.isEmpty()) {
            binding.statusMessageTextView.text = "No activities available"
            binding.statusMessageTextView.setTextColor(getColor(R.color.error_red))
            binding.confirmButton.isEnabled = false
            AppLogger.log("ActivitySelectionActivity: No activities available")
        } else if (activities.size == 1) {
            // Auto-select single activity
            val selectedActivity = activities[0]
            ActivityManager.saveActivity(
                this,
                selectedActivity.actKeyu,
                selectedActivity.actCode,
                selectedActivity.actLib
            )
            AppLogger.log("ActivitySelectionActivity: Auto-selected activity ${selectedActivity.actLib}")

            // Navigate to StockSearchActivity
            val intent = Intent(this, StockSearchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            // Multiple activities: show spinner for selection
            val displayList = activities.map { "${it.actCode} - ${it.actLib}" }
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                displayList
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.activitySpinner.adapter = adapter

            // Pre-select BKS as default activity
            val bksIndex = activities.indexOfFirst { it.actCode.equals("BKS", ignoreCase = true) }
            if (bksIndex >= 0) {
                binding.activitySpinner.setSelection(bksIndex)
                AppLogger.log("ACTIVITY_DEFAULT BKS pre-selected at index=$bksIndex")
            } else {
                binding.activitySpinner.setSelection(0)
            }

            binding.confirmButton.setOnClickListener {
                val selectedIndex = binding.activitySpinner.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < activities.size) {
                    val selectedActivity = activities[selectedIndex]
                    ActivityManager.saveActivity(
                        this,
                        selectedActivity.actKeyu,
                        selectedActivity.actCode,
                        selectedActivity.actLib
                    )
                    AppLogger.log("ActivitySelectionActivity: User selected activity ${selectedActivity.actLib}")

                    // Navigate to StockSearchActivity
                    val intent = Intent(this, StockSearchActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }

            binding.statusMessageTextView.text = ""
        }
    }


    private fun exitActivity() {
        finish()
    }
}
