package com.example.stockapp.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.models.StockItem
import com.example.stockapp.repositories.StockRepository
import com.example.stockapp.repositories.StockSearchCallback

/**
 * ViewModel for stock search operations.
 *
 * Manages state for stock search UI with:
 *  - Search results (LiveData list of StockItem)
 *  - Loading state
 *  - Error messages
 *  - Search execution flag
 *
 * Uses [StockRepository] for API calls and implements [StockSearchCallback] for result handling.
 *
 * @param context Android context for repository and logging
 */
class StockSearchViewModel(private val context: Context) : ViewModel() {

    private val repository = StockRepository(context)

    // Search results
    private val _searchResults = MutableLiveData<List<StockItem>>(emptyList())
    val searchResults: LiveData<List<StockItem>> = _searchResults

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error message
    private val _errorMessage = MutableLiveData<String>("")
    val errorMessage: LiveData<String> = _errorMessage

    // Flag to indicate if a search has been executed
    private val _hasSearched = MutableLiveData<Boolean>(false)
    val hasSearched: LiveData<Boolean> = _hasSearched

    /**
     * Searches for stock by article code, storage location, or storage number.
     *
     * At least one search criterion must be provided. Sets loading state, clears error,
     * and calls the repository with a callback to handle results.
     *
     * @param artCode Article code (optional)
     * @param stkLieu Storage location (optional)
     * @param stkNosu Storage number (optional)
     */
    fun searchStock(artCode: String?, stkLieu: String?, stkNosu: String?) {
        // Validate at least one criterion provided
        if ((artCode?.trim().isNullOrEmpty()) &&
            (stkLieu?.trim().isNullOrEmpty()) &&
            (stkNosu?.trim().isNullOrEmpty())
        ) {
            AppLogger.log("SEARCH_VALIDATION_FAILED error=all_criteria_empty")
            _errorMessage.value = "Please provide at least one search criterion"
            _hasSearched.value = true
            return
        }

        // Set loading state
        _isLoading.value = true
        _errorMessage.value = ""
        _hasSearched.value = true

        // Log search request
        AppLogger.log(
            "SEARCH_INITIATED art_code=$artCode stk_lieu=$stkLieu stk_nosu=$stkNosu"
        )

        // Call repository with callback
        repository.searchStock(
            artCode = artCode?.trim(),
            stkLieu = stkLieu?.trim(),
            stkNosu = stkNosu?.trim(),
            callback = object : StockSearchCallback {
                override fun onSuccess(items: List<StockItem>) {
                    AppLogger.log("SEARCH_SUCCESS items_count=${items.size}")
                    _searchResults.value = items
                    _errorMessage.value = ""
                    _isLoading.value = false
                }

                override fun onError(error: String) {
                    AppLogger.log("SEARCH_ERROR error=$error")
                    _searchResults.value = emptyList()
                    _errorMessage.value = error
                    _isLoading.value = false
                }
            }
        )
    }

    /**
     * Clears search results, error messages, and flags.
     *
     * Resets the ViewModel to its initial state.
     */
    fun clearSearch() {
        _searchResults.value = emptyList()
        _errorMessage.value = ""
        _isLoading.value = false
        _hasSearched.value = false
        AppLogger.log("SEARCH_CLEARED")
    }
}
