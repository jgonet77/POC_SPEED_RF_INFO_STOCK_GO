package com.example.stockapp.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stockapp.config.ConfigManager
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.repositories.HealthRepository
import com.example.stockapp.models.HealthCheckResponse
import com.example.stockapp.models.ApiHealthResponse
import kotlin.concurrent.thread

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

class HealthViewModel(
    val context: Context? = null,
    private val repository: HealthRepository = HealthRepository()
) : ViewModel() {
    private val configManager = context?.let { ConfigManager(it) }

    private val _connectionStatus = MutableLiveData<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val _connectionDetails = MutableLiveData<String>("")
    val connectionDetails: LiveData<String> = _connectionDetails

    private val _waitTime = MutableLiveData<String>("")
    val waitTime: LiveData<String> = _waitTime

    private val _apiHealthStatus = MutableLiveData<String>("Not tested")
    val apiHealthStatus: LiveData<String> = _apiHealthStatus

    private val _databaseHealthStatus = MutableLiveData<String>("Not tested")
    val databaseHealthStatus: LiveData<String> = _databaseHealthStatus

    private val _databaseVersion = MutableLiveData<String>("")
    val databaseVersion: LiveData<String> = _databaseVersion

    private val _databaseTime = MutableLiveData<String>("")
    val databaseTime: LiveData<String> = _databaseTime

    private val _apiTestTime = MutableLiveData<String>("")
    val apiTestTime: LiveData<String> = _apiTestTime

    private val _databaseTestTime = MutableLiveData<String>("")
    val databaseTestTime: LiveData<String> = _databaseTestTime

    private val _errorMessage = MutableLiveData<String>("")
    val errorMessage: LiveData<String> = _errorMessage

    private var connectionCheckThread: Thread? = null
    private var isConnectionCheckRunning = false

    fun testConnection(apiHost: String = "", apiPort: Int = 0) {
        if (_connectionStatus.value == ConnectionStatus.CONNECTING) return

        val host = if (apiHost.isEmpty()) configManager?.getApiHost() ?: "192.168.1.20" else apiHost
        val port = if (apiPort == 0) (configManager?.getApiPort()?.toIntOrNull() ?: 8000) else apiPort

        _connectionStatus.postValue(ConnectionStatus.CONNECTING)
        _connectionDetails.postValue("Connecting to $host:$port")
        _waitTime.postValue("Testing connection...")
        isConnectionCheckRunning = true

        AppLogger.log("Connection: Testing connection to $host:$port")

        connectionCheckThread = thread(isDaemon = true) {
            val startTime = System.currentTimeMillis()
            repository.checkApiHealth(
                onSuccess = { response ->
                    val elapsed = System.currentTimeMillis() - startTime
                    _connectionStatus.postValue(ConnectionStatus.CONNECTED)
                    _connectionDetails.postValue("API: $host:$port")
                    _waitTime.postValue("Connected in ${elapsed}ms")
                    _errorMessage.postValue("")
                    AppLogger.log("Connection: Successfully connected to $host:$port after ${elapsed}ms")
                    isConnectionCheckRunning = false
                },
                onError = { error ->
                    _connectionStatus.postValue(ConnectionStatus.ERROR)
                    _connectionDetails.postValue("API: $host:$port")
                    _waitTime.postValue("")
                    _errorMessage.postValue(error)
                    AppLogger.log("Connection: Failed to connect to $host:$port - $error")
                    isConnectionCheckRunning = false
                }
            )
        }
    }

    fun cancelConnection() {
        isConnectionCheckRunning = false
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _connectionDetails.value = ""
        _waitTime.value = ""
    }

    fun checkApiHealth() {
        _apiHealthStatus.value = "Testing..."
        _apiTestTime.value = "Testing..."
        val startTime = System.currentTimeMillis()
        repository.checkApiHealth(
            onSuccess = { response ->
                val elapsed = System.currentTimeMillis() - startTime
                _apiHealthStatus.value = "✅ ${response.status}"
                _apiTestTime.value = "Completed in ${elapsed}ms"
                _errorMessage.value = ""
            },
            onError = { error ->
                val elapsed = System.currentTimeMillis() - startTime
                _apiHealthStatus.value = "❌ Failed"
                _apiTestTime.value = "Failed after ${elapsed}ms"
                _errorMessage.value = error
            }
        )
    }

    fun checkDatabaseHealth() {
        _databaseHealthStatus.value = "Testing..."
        _databaseTestTime.value = "Testing..."
        val startTime = System.currentTimeMillis()
        repository.checkDatabaseHealth(
            onSuccess = { response ->
                val elapsed = System.currentTimeMillis() - startTime
                _databaseHealthStatus.value = "✅ ${response.database_status}"
                _databaseVersion.value = response.details["version"]?.toString() ?: "Unknown"
                _databaseTime.value = response.details["server_time"]?.toString() ?: "Unknown"
                _databaseTestTime.value = "Completed in ${elapsed}ms"
                _errorMessage.value = ""
            },
            onError = { error ->
                val elapsed = System.currentTimeMillis() - startTime
                _databaseHealthStatus.value = "❌ Failed"
                _databaseTestTime.value = "Failed after ${elapsed}ms"
                _errorMessage.value = error
                _databaseVersion.value = ""
                _databaseTime.value = ""
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        cancelConnection()
    }
}
