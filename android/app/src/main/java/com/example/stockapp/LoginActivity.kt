package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.api.ApiClient
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

    private lateinit var loginEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var hashMethodRadioGroup: RadioGroup
    private lateinit var statusMessageTextView: TextView
    private lateinit var loginProgressBar: ProgressBar
    private lateinit var loginButton: Button
    private lateinit var cancelButton: Button

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize app-wide singletons
        AppLogger(this)
        ApiClient.init(this)

        // Find views
        loginEditText = findViewById(R.id.loginEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        hashMethodRadioGroup = findViewById(R.id.hashMethodRadioGroup)
        statusMessageTextView = findViewById(R.id.statusMessageTextView)
        loginProgressBar = findViewById(R.id.loginProgressBar)
        loginButton = findViewById(R.id.loginButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Setup button listeners
        loginButton.setOnClickListener { performLogin() }
        cancelButton.setOnClickListener { exitApp() }
    }

    private fun performLogin() {
        // Validate inputs
        val login = loginEditText.text.toString().trim()
        if (login.isEmpty()) {
            showError(getString(R.string.login_error_empty_login))
            return
        }

        val password = passwordEditText.text.toString()
        if (password.isEmpty()) {
            showError(getString(R.string.login_error_empty_password))
            return
        }

        // Get selected hash method
        val hashMethod = getSelectedHashMethod()

        // Show loading state
        setLoadingState(true)
        statusMessageTextView.text = getString(R.string.login_loading)
        statusMessageTextView.setTextColor(getColor(android.R.color.black))

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
        setLoadingState(false)

        // Store token
        TokenManager.saveToken(this, response.token, response.expires_in)

        // Log successful login
        AppLogger.log(
            "[${getCurrentTimestamp()}] LOGIN_SUCCESS " +
                "login=$login hash=$hashMethod token_saved=true expires_in=${response.expires_in}"
        )

        // Show success message briefly before launching MainActivity
        statusMessageTextView.text = response.message
        statusMessageTextView.setTextColor(getColor(android.R.color.holo_green_dark))

        // Launch MainActivity and finish LoginActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun handleLoginFailure(login: String, hashMethod: String, errorMsg: String) {
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

    private fun showError(message: String) {
        statusMessageTextView.text = message
        statusMessageTextView.setTextColor(getColor(android.R.color.holo_red_dark))
    }

    private fun setLoadingState(isLoading: Boolean) {
        loginButton.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        loginEditText.isEnabled = !isLoading
        hashMethodRadioGroup.isEnabled = !isLoading
        loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun getSelectedHashMethod(): String {
        return when (hashMethodRadioGroup.checkedRadioButtonId) {
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
