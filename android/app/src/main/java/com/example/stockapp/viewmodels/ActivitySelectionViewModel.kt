package com.example.stockapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.stockapp.api.ApiClient
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.TokenManager
import com.example.stockapp.models.ActivityItem
import com.example.stockapp.models.ActivityListResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

class ActivitySelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _activities = MutableLiveData<List<ActivityItem>>()
    val activities: LiveData<List<ActivityItem>> = _activities

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadActivities() {
        // Validate token before making API call
        val token = TokenManager.getToken(getApplication())
        if (token == null) {
            _error.postValue("No valid authentication token")
            AppLogger.log("ActivitySelectionViewModel: Token validation failed")
            return
        }

        _loading.postValue(true)
        _error.postValue(null)

        AppLogger.log("ActivitySelectionViewModel: Starting activity load")

        val viewModelRef = WeakReference(this)
        val call = ApiClient.apiService.getActivities()
        call.enqueue(object : Callback<ActivityListResponse> {
            override fun onResponse(call: Call<ActivityListResponse>, response: Response<ActivityListResponse>) {
                viewModelRef.get() ?: return

                response.body()?.let { activityResponse ->
                    AppLogger.log("ActivitySelectionViewModel: Load successful, count=${activityResponse.activities.size}")
                    _activities.postValue(activityResponse.activities)
                    _loading.postValue(false)
                } ?: run {
                    val errorMsg = "Invalid response from server"
                    _error.postValue(errorMsg)
                    _loading.postValue(false)
                    AppLogger.log("ActivitySelectionViewModel: Load failed - invalid response")
                }
            }

            override fun onFailure(call: Call<ActivityListResponse>, t: Throwable) {
                val errorMsg = t.message ?: "Network error"
                _error.postValue(errorMsg)
                _loading.postValue(false)
                AppLogger.log("ActivitySelectionViewModel: Load failed - ${t.message}")
            }
        })
    }
}
