package com.example.stockapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.databinding.ActivityStockSearchBinding
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.models.StockItem
import com.example.stockapp.viewmodels.StockSearchViewModel

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

        // Initialize ViewModel with factory
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return StockSearchViewModel(this@StockSearchActivity) as T
                }
            }
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
     * Shows the error in the status message TextView.
     *
     * @param errorMessage The error message to display
     */
    private fun showError(errorMessage: String) {
        binding.statusMessage.text = errorMessage
        binding.loadingSpinner.visibility = View.GONE
        hideResults()
        binding.emptyStateMessage.visibility = View.VISIBLE
        binding.emptyStateMessage.text = errorMessage

        AppLogger.log("ERROR_SHOWN error=$errorMessage")
    }
}
