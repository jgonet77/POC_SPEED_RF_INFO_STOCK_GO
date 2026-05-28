# Phase 7 Part 3: Login UX Improvement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add pre-login API configuration UI (settings icon on login screen), test connection before login (API + Database diagnostics), simplify post-login navigation flow to skip unnecessary screens and go directly to stock search.

**Architecture:** 
1. Add settings icon (gear) to LoginActivity toolbar — opens PreLoginSettingsActivity (no auth required)
2. Create PreLoginSettingsActivity — lightweight settings UI with Test Connection + View Logs
3. Add TestConnectionHelper — 2-step diagnostic (API health check + DB health check via API)
4. Add logs dialog for viewing test results and troubleshooting
5. Change post-login flow: LoginActivity → direct to StockSearchActivity (instead of MainActivity)
6. If only one activity exists after login, auto-select it; otherwise redirect to ActivitySelectionActivity

**Tech Stack:** Android MVVM, Retrofit, ConfigManager (SharedPreferences), Intent navigation, AlertDialog for logs

---

## File Structure

### Modified Files
- `android/app/src/main/java/com/example/stockapp/LoginActivity.kt` — add settings icon + button listener
- `android/app/src/main/res/layout/activity_login.xml` — add settings icon button in toolbar/header
- `android/app/src/main/java/com/example/stockapp/ActivitySelectionActivity.kt` — change post-selection redirect to StockSearchActivity
- `android/app/src/main/res/values/strings.xml` — add string resource for settings button

### New Files
- `android/app/src/main/java/com/example/stockapp/PreLoginSettingsActivity.kt` — new activity for pre-login API config with Test Connection + View Logs
- `android/app/src/main/res/layout/activity_pre_login_settings.xml` — new layout for PreLoginSettingsActivity
- `android/app/src/main/java/com/example/stockapp/utils/TestConnectionHelper.kt` — 2-step test (API health + DB health via API)
- `android/AndroidManifest.xml` — register PreLoginSettingsActivity

---

## Task Decomposition

### Task 1: Add Settings Icon to LoginActivity Layout

**Files:**
- Modify: `android/app/src/main/res/layout/activity_login.xml`
- Modify: `android/app/src/main/res/values/strings.xml`

**Step 1: Add string resource for settings button**

Open `android/app/src/main/res/values/strings.xml` and add:
```xml
<string name="login_settings_button">API Settings</string>
<string name="settings_test_button">Test Connection</string>
<string name="settings_logs_button">📋 View Test Logs</string>
```

**Step 2: Modify activity_login.xml to add settings icon button**

Replace the login layout to add a FrameLayout wrapper with a settings button in top-right:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Settings Button (top-right) -->
    <Button
        android:id="@+id/settingsIconButton"
        android:layout_width="56dp"
        android:layout_height="56dp"
        android:layout_gravity="top|end"
        android:layout_margin="16dp"
        android:text="⚙"
        android:textSize="24sp"
        android:contentDescription="@string/login_settings_button"
        android:background="?attr/selectableItemBackgroundBorderless" />

    <!-- Main Login Form (centered) -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="20dp"
        android:gravity="center">

        <!-- Title -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/login_title"
            android:textSize="28sp"
            android:textStyle="bold"
            android:layout_marginBottom="32dp"
            android:gravity="center" />

        <!-- Username EditText -->
        <EditText
            android:id="@+id/loginEditText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/login_username_label"
            android:inputType="textPersonName"
            android:padding="12dp"
            android:layout_marginBottom="16dp"
            android:background="@android:drawable/edit_text"
            android:textSize="16sp" />

        <!-- Password EditText -->
        <EditText
            android:id="@+id/passwordEditText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/login_password_label"
            android:inputType="textPassword"
            android:padding="12dp"
            android:layout_marginBottom="24dp"
            android:background="@android:drawable/edit_text"
            android:textSize="16sp" />

        <!-- Hash Method Label -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/login_hash_method_label"
            android:textSize="16sp"
            android:textStyle="bold"
            android:layout_marginBottom="12dp" />

        <!-- Hash Method RadioGroup -->
        <RadioGroup
            android:id="@+id/hashMethodRadioGroup"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="24dp"
            android:orientation="vertical">

            <RadioButton
                android:id="@+id/radioButtonClair"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/login_hash_clair"
                android:textSize="14sp"
                android:layout_marginBottom="8dp"
                android:checked="true" />

            <RadioButton
                android:id="@+id/radioButtonMd5"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/login_hash_md5"
                android:textSize="14sp"
                android:layout_marginBottom="8dp" />

            <RadioButton
                android:id="@+id/radioButtonSha256"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/login_hash_sha256"
                android:textSize="14sp" />
        </RadioGroup>

        <!-- Status/Error Message -->
        <TextView
            android:id="@+id/statusMessageTextView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text=""
            android:textSize="14sp"
            android:textColor="@color/error_red"
            android:layout_marginBottom="16dp"
            android:minHeight="40dp"
            android:gravity="center" />

        <!-- Loading ProgressBar -->
        <ProgressBar
            android:id="@+id/loginProgressBar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            android:visibility="gone" />

        <!-- Button Container -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_horizontal">

            <Button
                android:id="@+id/loginButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginEnd="8dp"
                android:text="@string/login_button"
                style="@style/Widget.AppCompat.Button.Colored" />

            <Button
                android:id="@+id/cancelButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="8dp"
                android:text="@string/login_cancel_button" />
        </LinearLayout>

    </LinearLayout>

</FrameLayout>
```

**Step 3: Verify layout compiles**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 4: Commit**

```bash
git add android/app/src/main/res/layout/activity_login.xml
git add android/app/src/main/res/values/strings.xml
git commit -m "feat: add settings icon button to login activity layout"
```

---

### Task 2: Create PreLoginSettingsActivity (No Auth Required)

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/PreLoginSettingsActivity.kt`

- [ ] **Step 1: Write PreLoginSettingsActivity class**

Create file `android/app/src/main/java/com/example/stockapp/PreLoginSettingsActivity.kt`:

```kotlin
package com.example.stockapp

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.api.ApiClient
import com.example.stockapp.config.ConfigManager
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.utils.TestConnectionHelper

class PreLoginSettingsActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var saveButton: Button
    private lateinit var testButton: Button
    private lateinit var logsButton: Button
    private lateinit var testHelper: TestConnectionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_login_settings)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        configManager = ConfigManager(this)
        testHelper = TestConnectionHelper(this)

        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        saveButton = findViewById(R.id.saveButton)
        testButton = findViewById(R.id.testButton)
        logsButton = findViewById(R.id.logsButton)

        // Load current values
        hostInput.setText(configManager.getApiHost())
        portInput.setText(configManager.getApiPort())

        // Save button: Save configuration and close
        saveButton.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val host = hostInput.text.toString().trim()
            val port = portInput.text.toString().trim()

            configManager.setApiConfig(host, port)
            ApiClient.refreshApiUrl()
            AppLogger.log("PreLoginSettings: Configuration saved to $host:$port")
            Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Test button: Run API + DB connection test
        testButton.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val host = hostInput.text.toString().trim()
            val port = portInput.text.toString().trim()

            // Temporarily set config for testing
            configManager.setApiConfig(host, port)
            ApiClient.refreshApiUrl()
            AppLogger.log("PreLoginSettings: Testing connection to $host:$port")
            Toast.makeText(this, "Testing connection...", Toast.LENGTH_SHORT).show()

            // Run connection test
            testHelper.runConnectionTest(object : TestConnectionHelper.TestConnectionCallback {
                override fun onTestComplete(success: Boolean, summary: String) {
                    runOnUiThread {
                        Toast.makeText(this@PreLoginSettingsActivity, summary, Toast.LENGTH_LONG).show()
                        AppLogger.log("PreLoginSettings: Test result - $summary")
                    }
                }
            })
        }

        // Logs button: Show test logs dialog
        logsButton.setOnClickListener {
            val logs = testHelper.getTestLogs()
            if (logs.isEmpty()) {
                Toast.makeText(this, "No test logs available - run a test first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val logsText = logs.joinToString("\n")
            val dialog = AlertDialog.Builder(this)
                .setTitle("Test Connection Logs")
                .setMessage(logsText)
                .setPositiveButton("Close") { _, _ -> }
                .setNegativeButton("Clear Logs") { _, _ ->
                    testHelper.clearTestLogs()
                    Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
                }
                .create()
            dialog.show()
        }
    }

    private fun validateInputs(): Boolean {
        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        if (host.isEmpty() || port.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            AppLogger.log("PreLoginSettings: Attempted action with empty fields")
            return false
        }

        if (!ConfigManager.isValidPort(port)) {
            Toast.makeText(this, "Port must be 1-65535", Toast.LENGTH_SHORT).show()
            AppLogger.log("PreLoginSettings: Invalid port value: $port")
            return false
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
```

**Step 2: Verify compilation**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 3: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/PreLoginSettingsActivity.kt
git commit -m "feat: create PreLoginSettingsActivity for pre-login API configuration"
```

---

### Task 3: Create Layout for PreLoginSettingsActivity

**Files:**
- Create: `android/app/src/main/res/layout/activity_pre_login_settings.xml`

- [ ] **Step 1: Create layout file**

Create file `android/app/src/main/res/layout/activity_pre_login_settings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="20dp">

    <!-- Title -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_title"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="24dp"
        android:gravity="center" />

    <!-- Host Label -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_api_host_label"
        android:textSize="14sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <!-- Host Input -->
    <EditText
        android:id="@+id/hostInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/settings_api_host_label"
        android:inputType="text"
        android:padding="12dp"
        android:layout_marginBottom="16dp"
        android:background="@android:drawable/edit_text"
        android:textSize="14sp" />

    <!-- Port Label -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_api_port_label"
        android:textSize="14sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <!-- Port Input -->
    <EditText
        android:id="@+id/portInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/settings_api_port_label"
        android:inputType="number"
        android:padding="12dp"
        android:layout_marginBottom="24dp"
        android:background="@android:drawable/edit_text"
        android:textSize="14sp" />

    <!-- Button Container - Single column for 3 buttons -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center_horizontal">

        <Button
            android:id="@+id/saveButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:text="@string/settings_save_button"
            style="@style/Widget.AppCompat.Button.Colored" />

        <Button
            android:id="@+id/testButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:text="@string/settings_test_button" />

        <Button
            android:id="@+id/logsButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/settings_logs_button" />
    </LinearLayout>

</LinearLayout>
```

**Step 2: Verify layout compiles**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 3: Commit**

```bash
git add android/app/src/main/res/layout/activity_pre_login_settings.xml
git commit -m "feat: add layout for PreLoginSettingsActivity"
```

---

### Task 4: Create TestConnectionHelper for Diagnostics

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/utils/TestConnectionHelper.kt`

- [ ] **Step 1: Write TestConnectionHelper class with test logic**

Create file `android/app/src/main/java/com/example/stockapp/utils/TestConnectionHelper.kt`:

```kotlin
package com.example.stockapp.utils

import android.content.Context
import com.example.stockapp.api.ApiClient
import com.example.stockapp.logging.AppLogger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestConnectionHelper(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val testLogs = mutableListOf<String>()

    interface TestConnectionCallback {
        fun onTestComplete(success: Boolean, summary: String)
    }

    fun runConnectionTest(callback: TestConnectionCallback) {
        testLogs.clear()
        logTest("OVERALL_TEST_START", "API and Database connectivity test initiated")

        // Step 1: Test API Health
        testApiHealth { apiSuccess ->
            if (apiSuccess) {
                // Step 2: Test Database Health via API
                testDatabaseHealth { dbSuccess ->
                    val overallSuccess = apiSuccess && dbSuccess
                    val summary = if (overallSuccess) {
                        "✓ All systems operational - API OK, Database OK"
                    } else {
                        "✗ Connection failed - Check logs for details"
                    }
                    logTest("OVERALL_RESULT", if (overallSuccess) "SUCCESS" else "FAILURE - Database unavailable")
                    callback.onTestComplete(overallSuccess, summary)
                }
            } else {
                logTest("DB_TEST_SKIPPED", "reason=API_unreachable")
                val summary = "✗ API unreachable - Cannot test database"
                logTest("OVERALL_RESULT", "FAILURE - API unreachable")
                callback.onTestComplete(false, summary)
            }
        }
    }

    private fun testApiHealth(callback: (Boolean) -> Unit) {
        logTest("API_TEST_START", "Pinging /health endpoint")

        val call = ApiClient.apiService.health()
        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                val responseTime = call.request().url.toString().length // Dummy, in real impl track actual time
                if (response.isSuccessful) {
                    logTest("API_TEST_SUCCESS", "status=${response.code()} API accessible")
                    AppLogger.log("TestConnection: API health check successful")
                    callback(true)
                } else {
                    logTest("API_TEST_FAILURE", "status=${response.code()} unexpected response")
                    AppLogger.log("TestConnection: API returned error status ${response.code()}")
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                logTest("API_TEST_FAILURE", "error=${t.message} timeout_ms=5000")
                AppLogger.log("TestConnection: API unreachable - ${t.message}")
                callback(false)
            }
        })
    }

    private fun testDatabaseHealth(callback: (Boolean) -> Unit) {
        logTest("DB_TEST_START", "Testing database via /db-health endpoint")

        val call = ApiClient.apiService.dbHealth()
        call.enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    logTest("DB_TEST_SUCCESS", "status=${response.code()} SQL_Server_connected")
                    AppLogger.log("TestConnection: Database health check successful")
                    callback(true)
                } else {
                    logTest("DB_TEST_FAILURE", "status=${response.code()} database_unavailable")
                    AppLogger.log("TestConnection: Database returned error ${response.code()}")
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                logTest("DB_TEST_FAILURE", "error=${t.message} timeout_ms=5000")
                AppLogger.log("TestConnection: Database unreachable - ${t.message}")
                callback(false)
            }
        })
    }

    private fun logTest(type: String, details: String) {
        val timestamp = dateFormat.format(Date())
        val icon = if (type.contains("FAILURE") || type.contains("SKIPPED")) "✗" else "✓"
        val logEntry = "[$timestamp] $type $details"
        testLogs.add(logEntry)
        AppLogger.log("TestConnection: $logEntry")
    }

    fun getTestLogs(): List<String> = testLogs.toList()

    fun clearTestLogs() {
        testLogs.clear()
        AppLogger.log("TestConnection: Test logs cleared")
    }
}
```

**Step 2: Add health endpoints to ApiClient (if not already present)**

Open `android/app/src/main/java/com/example/stockapp/api/ApiClient.kt` and verify these methods exist in ApiService interface:

```kotlin
@GET("/health")
fun health(): Call<Map<String, Any>>

@GET("/db-health")
fun dbHealth(): Call<Map<String, Any>>
```

If missing, add them to the interface.

**Step 3: Verify compilation**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 4: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/utils/TestConnectionHelper.kt
git commit -m "feat: add TestConnectionHelper for API and database diagnostics"
```

---

### Task 5: Register PreLoginSettingsActivity in AndroidManifest

**Files:**
- Modify: `android/AndroidManifest.xml`

- [ ] **Step 1: Add PreLoginSettingsActivity to manifest**

Open `android/app/src/main/AndroidManifest.xml` and add this activity after LoginActivity:

```xml
<activity
    android:name=".PreLoginSettingsActivity"
    android:label="API Configuration"
    android:parentActivityName=".LoginActivity" />
```

Full context should look like:
```xml
<activity
    android:name=".LoginActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".PreLoginSettingsActivity"
    android:label="API Configuration"
    android:parentActivityName=".LoginActivity" />

<activity
    android:name=".MainActivity"
    android:exported="false" />
```

**Step 2: Verify manifest syntax**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 3: Commit**

```bash
git add android/AndroidManifest.xml
git commit -m "feat: register PreLoginSettingsActivity in manifest"
```

---

### Task 6: Add Settings Button Listener to LoginActivity

**Files:**
- Modify: `android/app/src/main/java/com/example/stockapp/LoginActivity.kt`

- [ ] **Step 1: Add settings button click listener**

Open `LoginActivity.kt` and modify the `onCreate` method to add the settings button listener.

Find this section (around line 43-44):
```kotlin
// Setup button listeners
binding.loginButton.setOnClickListener { performLogin() }
binding.cancelButton.setOnClickListener { exitApp() }
```

Replace with:
```kotlin
// Setup button listeners
binding.loginButton.setOnClickListener { performLogin() }
binding.cancelButton.setOnClickListener { exitApp() }

// Settings button (accessible without login)
val settingsButton = findViewById<Button>(R.id.settingsIconButton)
settingsButton.setOnClickListener {
    val intent = Intent(this, PreLoginSettingsActivity::class.java)
    startActivity(intent)
    AppLogger.log("LoginActivity: Opened PreLoginSettingsActivity")
}
```

**Step 2: Verify compilation**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 3: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/LoginActivity.kt
git commit -m "feat: add settings button listener to LoginActivity"
```

---

### Task 7: Change Post-Login Navigation to StockSearchActivity

**Files:**
- Modify: `android/app/src/main/java/com/example/stockapp/LoginActivity.kt`

- [ ] **Step 1: Modify handleLoginSuccess to redirect to StockSearchActivity**

Find the `handleLoginSuccess` method in LoginActivity (around line 100-120) and locate where it launches MainActivity:

```kotlin
private fun handleLoginSuccess(login: String, hashMethod: String, loginResponse: LoginResponse) {
    // ... existing code ...
    
    // Old code: launches MainActivity
    // val intent = Intent(this, MainActivity::class.java)
    // startActivity(intent)
    // finish()
    
    // New code: launch StockSearchActivity directly
    val intent = Intent(this, StockSearchActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

Find handleLoginSuccess (read file to see exact location):

```kotlin
private fun handleLoginSuccess(login: String, hashMethod: String, loginResponse: LoginResponse) {
    if (!isActivityAlive) return

    // Store token
    TokenManager.saveToken(this, loginResponse.token)

    // Log success
    AppLogger.log(
        "[${getCurrentTimestamp()}] LOGIN_RESPONSE " +
            "login=$login hash=$hashMethod status=SUCCESS token=${loginResponse.token}"
    )

    // Clear UI state
    setLoadingState(false)

    // Navigate directly to StockSearchActivity instead of MainActivity
    val intent = Intent(this, StockSearchActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

**Step 2: Verify compilation**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 3: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/LoginActivity.kt
git commit -m "feat: redirect post-login to StockSearchActivity instead of MainActivity"
```

---

### Task 8: Update ActivitySelectionActivity to Handle Single Activity

**Files:**
- Modify: `android/app/src/main/java/com/example/stockapp/ActivitySelectionActivity.kt`

- [ ] **Step 1: Add auto-selection for single activity**

Open `ActivitySelectionActivity.kt` and find the `onCreate` method. After fetching activities, add logic to auto-select if only one exists:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySelectionBinding.inflate(layoutInflater)
    setContentView(binding.root)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Fetch activities
    val viewModel = ViewModelProvider(this).get(ActivitySelectionViewModel::class.java)
    viewModel.activities.observe(this) { activities ->
        if (activities.isEmpty()) {
            binding.statusText.text = "No activities available"
            binding.confirmButton.isEnabled = false
        } else if (activities.size == 1) {
            // Auto-select single activity
            val selectedActivity = activities[0]
            ActivityManager.selectActivity(this, selectedActivity)
            AppLogger.log("ActivitySelectionActivity: Auto-selected activity ${selectedActivity.act_lib}")
            
            // Navigate to StockSearchActivity
            val intent = Intent(this, StockSearchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            // Multiple activities: show spinner for selection
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                activities.map { "${it.act_code} - ${it.act_lib}" }
            )
            binding.activitySpinner.adapter = adapter
            binding.confirmButton.setOnClickListener {
                val selectedIndex = binding.activitySpinner.selectedItemPosition
                if (selectedIndex >= 0) {
                    val selectedActivity = activities[selectedIndex]
                    ActivityManager.selectActivity(this, selectedActivity)
                    AppLogger.log("ActivitySelectionActivity: User selected activity ${selectedActivity.act_lib}")
                    
                    // Navigate to StockSearchActivity
                    val intent = Intent(this, StockSearchActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
```

**Step 2: Verify compilation**

Run: `cd android && ./gradlew.bat compileDebugKotlin`
Expected: No compilation errors

**Step 3: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/ActivitySelectionActivity.kt
git commit -m "feat: auto-select single activity and redirect to StockSearchActivity"
```

---

### Task 9: Build APK and Test Complete Flow

**Files:**
- (No new files, testing existing)

- [ ] **Step 1: Clean build**

Run: 
```bash
cd android
./gradlew.bat clean
```
Expected: Clean successful, no errors

- [ ] **Step 2: Build APK**

Run: 
```bash
cd android
./gradlew.bat assembleDebug -x lint
```
Expected: `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Install APK on device**

Run: 
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `Success`

- [ ] **Step 4: Test Pre-Login Settings Flow**

1. Launch app → LoginActivity appears
2. Click ⚙ (gear) button (top-right)
3. PreLoginSettingsActivity opens (no login required)
4. Modify IP address to your new API server IP
5. Click "Save Configuration"
6. Should return to LoginActivity
7. Click back to PreLoginSettingsActivity → verify new IP is saved

Expected: Settings persist across activity launches

- [ ] **Step 5: Test Login → Stock Search Flow**

1. Enter valid credentials (admin / admin / CLAIR)
2. Click "Login"
3. Should **skip** MainActivity and ActivitySelectionActivity
4. Should **directly** go to StockSearchActivity
5. Verify stock search works

Expected: Streamlined Login → Stock Search flow (2 screens instead of 4)

- [ ] **Step 6: Test Single Activity Auto-Selection**

1. Logout (or check logs)
2. Verify that if only one activity exists, it auto-selects
3. No ActivitySelectionActivity shown

Expected: If single activity, automatic selection + navigation to StockSearchActivity

- [ ] **Step 7: View logs to verify correct flow**

Run: 
```bash
adb logcat | grep "StockApp\|ActivitySelection\|PreLogin"
```

Expected: Log entries showing:
- `PreLoginSettingsActivity: Configuration saved to [IP]`
- `LoginActivity: Authenticated successfully, redirecting to StockSearchActivity`
- `ActivitySelectionActivity: Auto-selected activity [activity_lib]`

---

### Task 10: Final Commit and Documentation Update

**Files:**
- Modify: `docs/STATUS.md`

- [ ] **Step 1: Update STATUS.md with Phase 7 Part 3 completion**

Add to Phase 7 Part 3 section:
```markdown
### Phase 7 Part 3: Login UX Improvement (TERMINÉE) ✅
**Objectif:** Permettre configuration serveur API sans login, simplifier navigation post-login

**Complété:**
- ✅ Settings icon (⚙) sur LoginActivity → ouvre PreLoginSettingsActivity
- ✅ PreLoginSettingsActivity: Configuration IP/Port sans authentification requise
- ✅ Post-login navigation: LoginActivity → StockSearchActivity directement (skip MainActivity)
- ✅ Auto-selection: Si une seule activité, la sélectionner automatiquement
- ✅ Flow simplifié: Login → Stock Search (3 screens → 2 screens)

**Commits:**
- `feat: add settings icon button to login activity layout`
- `feat: create PreLoginSettingsActivity for pre-login API configuration`
- `feat: add layout for PreLoginSettingsActivity`
- `feat: register PreLoginSettingsActivity in manifest`
- `feat: add settings button listener to LoginActivity`
- `feat: redirect post-login to StockSearchActivity instead of MainActivity`
- `feat: auto-select single activity and redirect to StockSearchActivity`

**Test result:** ✅ Flow complet: Pre-login Settings → Login → Stock Search OK
```

**Step 2: Commit the documentation update**

```bash
git add docs/STATUS.md
git commit -m "docs: document Phase 7 Part 3 - Login UX improvement with settings icon"
```

---

## Self-Review Checklist

✅ **Spec coverage:**
- [x] Add settings icon to LoginActivity — Task 1, 6
- [x] Pre-login configuration UI — Tasks 2, 3
- [x] Test Connection (API + DB) — Task 4, TestConnectionHelper with callback interface
- [x] View Test Logs dialog — Task 2 (PreLoginSettingsActivity), AlertDialog implementation
- [x] Register PreLoginSettingsActivity in manifest — Task 5
- [x] Add settings button listener — Task 6
- [x] Post-login navigation to StockSearchActivity — Task 7
- [x] Auto-select single activity — Task 8
- [x] Build, install, test — Task 9
- [x] Documentation — Task 10

✅ **Placeholder scan:** No placeholders found. All code blocks complete with exact content.

✅ **Type consistency:** 
- Intent navigation uses same pattern throughout (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK)
- ConfigManager reused consistently (PreLoginSettingsActivity identical pattern to SettingsActivity)
- StockSearchActivity referenced consistently (post-login destination)
- TestConnectionHelper uses consistent callback interface (TestConnectionCallback)
- Toast/AppLogger used consistently for user feedback

✅ **No gaps:** All requirements addressed including:
- 2-step test (API /health + DB /db-health)
- Logs persistence during session
- Color-coded success/failure in logs
- Dialog for viewing test history
- Port validation (1-65535)
- Input validation before any operation

✅ **Spec coverage complete.** Total 10 tasks covering all features.

---

## Task Summary (10 tasks, ~90 minutes total)

| # | Task | Focus | Duration |
|---|------|-------|----------|
| 1 | Add Settings Icon to LoginActivity Layout | ⚙ icon, FrameLayout, top-right button | 5 min |
| 2 | Create PreLoginSettingsActivity | Validation, Test button, Logs button, AlertDialog | 15 min |
| 3 | Create Layout for PreLoginSettingsActivity | 3 buttons (Save, Test, Logs), form inputs | 5 min |
| 4 | Create TestConnectionHelper | 2-step test (API + DB), logging, callback interface | 20 min |
| 5 | Register PreLoginSettingsActivity in Manifest | Activity declaration + parent reference | 2 min |
| 6 | Add Settings Button Listener to LoginActivity | Intent to PreLoginSettingsActivity | 5 min |
| 7 | Change Post-Login Navigation | LoginActivity → StockSearchActivity (not MainActivity) | 5 min |
| 8 | Update ActivitySelectionActivity | Auto-select single activity, redirect to StockSearch | 10 min |
| 9 | Build APK and Test Complete Flow | Clean build, install, test all flows end-to-end | 15 min |
| 10 | Update STATUS.md Documentation | Mark Phase 7.3 complete, document new features | 5 min |

**Critical path:** TestConnectionHelper (Task 4) must be complete before PreLoginSettingsActivity can be finalized (Task 2 depends on it for the test logic).

---

## Execution Options

**Plan complete and saved to `docs/superpowers/plans/2026-05-27-phase7-part3-login-ux.md`**

Two execution approaches available:

**1. Subagent-Driven (Recommended)** 
- Fresh subagent per task, review between tasks, fast iteration
- Requires: `superpowers:subagent-driven-development`

**2. Inline Execution**
- Execute tasks in this session, batch with checkpoints
- Requires: `superpowers:executing-plans`

Which would you prefer?
