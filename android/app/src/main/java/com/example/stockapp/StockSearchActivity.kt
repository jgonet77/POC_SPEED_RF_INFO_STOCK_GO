package com.example.stockapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.databinding.ActivityStockSearchBinding
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.ActivityManager
import com.example.stockapp.models.StockItem
import com.example.stockapp.viewmodels.StockSearchViewModel

/**
 * ViewModel factory for StockSearchViewModel.
 *
 * Provides Context-dependent injection for StockSearchViewModel while
 * maintaining proper lifecycle management to avoid memory leaks.
 */
class StockSearchViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StockSearchViewModel(context) as T
    }
}

/**
 * Activity for searching stock items.
 *
 * Allows users to search by article code, storage location, or storage number.
 * Displays results in a ListView with proper state management for loading,
 * errors, and empty results.
 *
 * Uses MVVM pattern with StockSearchViewModel for state management and LiveData
 * for reactive UI updates.
 */
class StockSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockSearchBinding
    private lateinit var viewModel: StockSearchViewModel
    private lateinit var resultsAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStockSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel with named factory to avoid memory leaks
        viewModel = ViewModelProvider(
            this,
            StockSearchViewModelFactory(this)
        ).get(StockSearchViewModel::class.java)

        // Initialize results adapter
        resultsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>()
        )
        binding.resultsListView.adapter = resultsAdapter

        // Observe search results
        viewModel.searchResults.observe(this) { items ->
            displayResults(items)
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            updateLoadingState(isLoading)
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { error ->
            if (error.isNotEmpty()) {
                showError(error)
            }
        }

        // Setup button listeners
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        binding.clearButton.setOnClickListener {
            clearSearch()
        }

        // Populate connection info header
        populateConnectionHeader()

        // Log activity creation
        AppLogger.log("STOCK_SEARCH_ACTIVITY_CREATED")
    }

    /**
     * Performs a stock search with current form input values.
     *
     * Validates that at least one search criterion is provided, then calls
     * the ViewModel's searchStock method.
     */
    private fun performSearch() {
        val articleCode = binding.articleCodeInput.text.toString().trim()
        val location = binding.locationInput.text.toString().trim()
        val storageNumber = binding.storageNumberInput.text.toString().trim()

        // Client-side validation: check if all fields are empty
        if (articleCode.isEmpty() && location.isEmpty() && storageNumber.isEmpty()) {
            showError(getString(R.string.stock_search_empty_fields_error))
            return
        }

        AppLogger.log(
            "SEARCH_BUTTON_TAPPED article_code=$articleCode location=$location storage=$storageNumber"
        )

        viewModel.searchStock(
            artCode = articleCode.ifEmpty { null },
            stkLieu = location.ifEmpty { null },
            stkNosu = storageNumber.ifEmpty { null }
        )
    }

    /**
     * Clears the search form and resets the ViewModel.
     *
     * Clears all input fields and hides results.
     */
    private fun clearSearch() {
        AppLogger.log("CLEAR_BUTTON_TAPPED")

        binding.articleCodeInput.text.clear()
        binding.locationInput.text.clear()
        binding.storageNumberInput.text.clear()
        binding.statusMessage.text = ""

        viewModel.clearSearch()

        // Hide results and show empty state
        hideResults()
    }

    /**
     * Displays search results or empty state based on items list.
     *
     * @param items List of StockItem objects from the search
     */
    private fun displayResults(items: List<StockItem>) {
        if (items.isEmpty()) {
            hideResults()
            binding.emptyStateMessage.visibility = View.VISIBLE
            binding.emptyStateMessage.text = getString(R.string.stock_search_no_results)
            AppLogger.log("SEARCH_RESULTS_EMPTY")
        } else {
            // Format items for display
            val displayItems = items.map { item ->
                "${item.artCode} | ${item.stkLieu} | ${item.stkNosu} | ${item.quaCode} | Qty: ${item.stkQte}"
            }

            resultsAdapter.clear()
            resultsAdapter.addAll(displayItems)
            resultsAdapter.notifyDataSetChanged()

            binding.emptyStateMessage.visibility = View.GONE
            binding.resultsListView.visibility = View.VISIBLE

            AppLogger.log("SEARCH_RESULTS_DISPLAYED items_count=${items.size}")
        }
    }

    /**
     * Hides the results ListView.
     */
    private fun hideResults() {
        binding.resultsListView.visibility = View.GONE
        binding.emptyStateMessage.visibility = View.GONE
        resultsAdapter.clear()
        resultsAdapter.notifyDataSetChanged()
    }

    /**
     * Updates the UI loading state based on loading flag.
     *
     * Shows or hides the progress spinner and enables/disables search button.
     *
     * @param isLoading True if a search is in progress
     */
    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.loadingSpinner.visibility = View.VISIBLE
            binding.statusMessage.text = getString(R.string.stock_search_loading)
            binding.searchButton.isEnabled = false
            binding.clearButton.isEnabled = false

            AppLogger.log("LOADING_STATE_SHOWN")
        } else {
            binding.loadingSpinner.visibility = View.GONE
            binding.searchButton.isEnabled = true
            binding.clearButton.isEnabled = true

            AppLogger.log("LOADING_STATE_HIDDEN")
        }
    }

    /**
     * Displays an error message to the user.
     *
     * Shows the error in the status message TextView with truncation to 200 characters
     * to prevent layout overflow. Full message is logged.
     *
     * @param errorMessage The error message to display
     */
    private fun showError(errorMessage: String) {
        // Log full message for debugging
        AppLogger.log("ERROR_SHOWN error=$errorMessage")

        // Truncate message to 200 chars with ellipsis for display
        val displayMessage = if (errorMessage.length > 200) {
            errorMessage.substring(0, 200) + "…"
        } else {
            errorMessage
        }

        binding.statusMessage.text = displayMessage
        binding.loadingSpinner.visibility = View.GONE
        hideResults()
        binding.emptyStateMessage.visibility = View.VISIBLE
        binding.emptyStateMessage.text = displayMessage
    }

    /**
     * Populates the connection info header with database name, user login, and activity code.
     *
     * Reads user login from SharedPreferences (stored during login) and activity code
     * from ActivityManager. Database name is hardcoded as "DerreySpeed_Client".
     * Logs the population with AppLogger for debugging.
     */
    private fun populateConnectionHeader() {
        try {
            // Get header TextViews from included layout
            val headerDatabaseName = findViewById<android.widget.TextView>(R.id.headerDatabaseName)
            val headerUserLogin = findViewById<android.widget.TextView>(R.id.headerUserLogin)
            val headerActivityCode = findViewById<android.widget.TextView>(R.id.headerActivityCode)

            // Database name
            val databaseName = "DerreySpeed_Client"
            headerDatabaseName.text = databaseName

            // User login from SharedPreferences
            val prefs = getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
            val userLogin = prefs.getString("user_login", "Unknown") ?: "Unknown"
            headerUserLogin.text = userLogin

            // Activity code from ActivityManager
            val activityCode = ActivityManager.getActivityCode(this) ?: "N/A"
            headerActivityCode.text = activityCode

            // Log header population
            AppLogger.log("HEADER_POPULATED database=$databaseName user=$userLogin activity=$activityCode")
        } catch (e: Exception) {
            AppLogger.log("HEADER_POPULATION_ERROR error=${e.message}")
        }
    }
}
