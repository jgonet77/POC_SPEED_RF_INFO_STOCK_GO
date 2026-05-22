package com.example.stockapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stockapp.repositories.HealthRepository
import com.example.stockapp.models.HealthCheckResponse
import com.example.stockapp.models.ApiHealthResponse

class HealthViewModel : ViewModel() {

    private val repository = HealthRepository()

    private val _apiHealthStatus = MutableLiveData<String>("Not tested")
    val apiHealthStatus: LiveData<String> = _apiHealthStatus

    private val _databaseHealthStatus = MutableLiveData<String>("Not tested")
    val databaseHealthStatus: LiveData<String> = _databaseHealthStatus

    private val _databaseVersion = MutableLiveData<String>("")
    val databaseVersion: LiveData<String> = _databaseVersion

    private val _databaseTime = MutableLiveData<String>("")
    val databaseTime: LiveData<String> = _databaseTime

    private val _errorMessage = MutableLiveData<String>("")
    val errorMessage: LiveData<String> = _errorMessage

    fun checkApiHealth() {
        _apiHealthStatus.value = "Testing..."
        repository.checkApiHealth(
            onSuccess = { response ->
                _apiHealthStatus.value = "✅ ${response.status}"
                _errorMessage.value = ""
            },
            onError = { error ->
                _apiHealthStatus.value = "❌ Failed"
                _errorMessage.value = error
            }
        )
    }

    fun checkDatabaseHealth() {
        _databaseHealthStatus.value = "Testing..."
        repository.checkDatabaseHealth(
            onSuccess = { response ->
                _databaseHealthStatus.value = "✅ ${response.database_status}"
                _databaseVersion.value = response.details["server_version"]?.toString() ?: "Unknown"
                _databaseTime.value = response.details["server_time"]?.toString() ?: "Unknown"
                _errorMessage.value = ""
            },
            onError = { error ->
                _databaseHealthStatus.value = "❌ Failed"
                _errorMessage.value = error
                _databaseVersion.value = ""
                _databaseTime.value = ""
            }
        )
    }
}
