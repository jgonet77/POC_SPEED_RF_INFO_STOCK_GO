package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.api.ApiClient
import com.example.stockapp.databinding.ActivityLoginBinding
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.TokenManager
import com.example.stockapp.models.LoginRequest
import com.example.stockapp.models.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Login Activity for user authentication.
 *
 * Presents login UI with username, password, and hash method selection.
 * On successful login, stores JWT token via TokenManager and launches MainActivity.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var isActivityAlive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize app-wide singletons
        AppLogger(this)
        ApiClient.init(this)

        // Setup button listeners
        binding.loginButton.setOnClickListener { performLogin() }
        binding.cancelButton.setOnClickListener { exitApp() }

        // Settings button (accessible without login)
        val settingsButton = findViewById<android.widget.Button>(R.id.settingsIconButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, PreLoginSettingsActivity::class.java)
            startActivity(intent)
            AppLogger.log("LoginActivity: Opened PreLoginSettingsActivity")
        }
    }

    override fun onDestroy() {
        isActivityAlive = false
        super.onDestroy()
    }

    private fun performLogin() {
        // Validate inputs
        val login = binding.loginEditText.text.toString().trim()
        if (login.isEmpty()) {
            showError(getString(R.string.login_error_empty_login))
            return
        }

        val password = binding.passwordEditText.text.toString()
        if (password.isEmpty()) {
            showError(getString(R.string.login_error_empty_password))
            return
        }

        // Get selected hash method
        val hashMethod = getSelectedHashMethod()

        // Show loading state
        setLoadingState(true)
        binding.statusMessageTextView.text = getString(R.string.login_loading)
        binding.statusMessageTextView.setTextColor(getColor(R.color.black))

        // Log login attempt
        AppLogger.log(
            "[${getCurrentTimestamp()}] LOGIN_REQUEST " +
                "login=$login hash=$hashMethod status=STARTED"
        )

        // Prepare request
        val request = LoginRequest(
            login = login,
            password = password,
            hash_method = hashMethod
        )

        // Make API call
        val call = ApiClient.apiService.login(request)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    handleLoginSuccess(login, hashMethod, loginResponse)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: getString(R.string.login_error_unknown)
                    handleLoginFailure(login, hashMethod, errorMsg)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                handleLoginFailure(login, hashMethod, t.message ?: getString(R.string.login_error_network))
            }
        })
    }

    private fun handleLoginSuccess(login: String, hashMethod: String, response: LoginResponse) {
        if (!isActivityAlive) return

        runOnUiThread {
            if (!isActivityAlive) return@runOnUiThread

            setLoadingState(false)

            // Store token
            TokenManager.saveToken(this, response.token, response.expires_in)

            // Log successful login
            AppLogger.log(
                "[${getCurrentTimestamp()}] LOGIN_SUCCESS " +
                    "login=$login hash=$hashMethod token_saved=true expires_in=${response.expires_in}"
            )

            // Show success message briefly before launching ActivitySelectionActivity
            binding.statusMessageTextView.text = response.message
            binding.statusMessageTextView.setTextColor(getColor(R.color.success_green))

            // Launch ActivitySelectionActivity and finish LoginActivity
            val intent = Intent(this, ActivitySelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun handleLoginFailure(login: String, hashMethod: String, errorMsg: String) {
        if (!isActivityAlive) return

        runOnUiThread {
            if (!isActivityAlive) return@runOnUiThread

            setLoadingState(false)

            val displayError = when {
                errorMsg.contains("Invalid", ignoreCase = true) -> getString(R.string.login_error_invalid_credentials)
                errorMsg.contains("network", ignoreCase = true) -> getString(R.string.login_error_network)
                else -> errorMsg.take(100) // Truncate long error messages
            }

            showError(displayError)

            // Log failed login
            AppLogger.log(
                "[${getCurrentTimestamp()}] LOGIN_FAILED " +
                    "login=$login hash=$hashMethod error=${errorMsg.take(50)}"
            )
        }
    }

    private fun showError(message: String) {
        binding.statusMessageTextView.text = message
        binding.statusMessageTextView.setTextColor(getColor(R.color.error_red))
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.passwordEditText.isEnabled = !isLoading
        binding.loginEditText.isEnabled = !isLoading
        binding.hashMethodRadioGroup.isEnabled = !isLoading
        binding.loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun getSelectedHashMethod(): String {
        return when (binding.hashMethodRadioGroup.checkedRadioButtonId) {
            R.id.radioButtonClair -> "CLAIR"
            R.id.radioButtonMd5 -> "MD5"
            R.id.radioButtonSha256 -> "SHA256"
            else -> "CLAIR"
        }
    }

    private fun exitApp() {
        finish()
    }

    private fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }
}
