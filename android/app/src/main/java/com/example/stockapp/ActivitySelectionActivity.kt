package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.api.ApiClient
import com.example.stockapp.databinding.ActivitySelectionBinding
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.ActivityManager
import com.example.stockapp.models.ActivityItem
import com.example.stockapp.models.ActivityListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for selecting which activity to work with.
 *
 * Loads available activities from API, displays them in a spinner,
 * and allows user to confirm selection before proceeding to MainActivity.
 */
class ActivitySelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectionBinding

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var isActivityAlive = true
    private var activities: List<ActivityItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize app-wide singletons
        AppLogger(this)
        ApiClient.init(this)

        // Setup button listeners
        binding.confirmButton.setOnClickListener { performActivitySelection() }
        binding.cancelButton.setOnClickListener { exitActivity() }

        // Load activities on startup
        loadActivities()
    }

    override fun onDestroy() {
        isActivityAlive = false
        super.onDestroy()
    }

    private fun loadActivities() {
        // Show loading state
        setLoadingState(true)
        binding.statusMessageTextView.text = getString(R.string.activity_selection_loading)
        binding.statusMessageTextView.setTextColor(getColor(R.color.black))

        // Log activity load attempt
        AppLogger.log(
            "[${getCurrentTimestamp()}] ACTIVITY_LOAD_REQUEST status=STARTED"
        )

        // Make API call
        val call = ApiClient.apiService.getActivities()
        call.enqueue(object : Callback<ActivityListResponse> {
            override fun onResponse(call: Call<ActivityListResponse>, response: Response<ActivityListResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val activityResponse = response.body()!!
                    handleLoadSuccess(activityResponse)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: getString(R.string.activity_selection_error_unknown)
                    handleLoadFailure(errorMsg)
                }
            }

            override fun onFailure(call: Call<ActivityListResponse>, t: Throwable) {
                handleLoadFailure(t.message ?: getString(R.string.activity_selection_error_network))
            }
        })
    }

    private fun handleLoadSuccess(response: ActivityListResponse) {
        if (!isActivityAlive) return

        runOnUiThread {
            if (!isActivityAlive) return@runOnUiThread

            setLoadingState(false)

            // Check if activities list is empty
            if (response.activities.isEmpty()) {
                showError(getString(R.string.activity_selection_error_empty))
                AppLogger.log(
                    "[${getCurrentTimestamp()}] ACTIVITY_LOAD_FAILED " +
                        "error=no_activities_returned"
                )
                return@runOnUiThread
            }

            // Store activities list
            activities = response.activities

            // Populate spinner with activities
            populateSpinner(activities)

            // Log successful load
            AppLogger.log(
                "[${getCurrentTimestamp()}] ACTIVITY_LOAD_SUCCESS " +
                    "activities_count=${activities.size}"
            )

            // Clear status message on success
            binding.statusMessageTextView.text = ""
        }
    }

    private fun handleLoadFailure(errorMsg: String) {
        if (!isActivityAlive) return

        runOnUiThread {
            if (!isActivityAlive) return@runOnUiThread

            setLoadingState(false)

            val displayError = when {
                errorMsg.contains("network", ignoreCase = true) ->
                    getString(R.string.activity_selection_error_network)
                else -> errorMsg.take(100) // Truncate long error messages
            }

            showError(displayError)

            // Log failed load
            AppLogger.log(
                "[${getCurrentTimestamp()}] ACTIVITY_LOAD_FAILED " +
                    "error=${errorMsg.take(50)}"
            )
        }
    }

    private fun populateSpinner(activityList: List<ActivityItem>) {
        // Create formatted display list: "ACT_CODE - ACT_LIB"
        val displayList = activityList.map { "${it.actCode} - ${it.actLib}" }

        // Create adapter
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            displayList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set adapter to spinner
        binding.activitySpinner.adapter = adapter

        // Set first item as selected by default
        if (activityList.isNotEmpty()) {
            binding.activitySpinner.setSelection(0)
        }
    }

    private fun performActivitySelection() {
        // Get selected position
        val selectedPosition = binding.activitySpinner.selectedItemPosition

        // Validate selection
        if (selectedPosition < 0 || selectedPosition >= activities.size) {
            showError(getString(R.string.activity_selection_error_select))
            return
        }

        // Get selected activity
        val selectedActivity = activities[selectedPosition]

        // Save activity
        ActivityManager.saveActivity(
            this,
            selectedActivity.actKeyu,
            selectedActivity.actCode,
            selectedActivity.actLib
        )

        // Log activity selection
        AppLogger.log(
            "[${getCurrentTimestamp()}] ACTIVITY_SELECTED " +
                "act_code=${selectedActivity.actCode} act_keyu=${selectedActivity.actKeyu}"
        )

        // Launch MainActivity and finish
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun exitActivity() {
        finish()
    }

    private fun showError(message: String) {
        binding.statusMessageTextView.text = message
        binding.statusMessageTextView.setTextColor(getColor(R.color.error_red))
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.confirmButton.isEnabled = !isLoading
        binding.cancelButton.isEnabled = !isLoading
        binding.activitySpinner.isEnabled = !isLoading
        binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }
}
