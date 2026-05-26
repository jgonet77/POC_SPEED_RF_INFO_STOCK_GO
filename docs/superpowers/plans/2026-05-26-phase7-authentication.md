# Phase 7 Part 1: Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development with **two parallel subagents** (Backend team + Android team). Each team has independent tasks with two-stage review. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement JWT-based authentication with 24h token expiry, password hashing options (CLAIR/MD5/SHA256), and token storage in SharedPreferences.

**Architecture:**
- Backend: FastAPI /login endpoint, JWT token generation, 24h expiry, in-memory token store
- Android: Login screen (before MainActivity), hashing radio buttons, token persistence, API header integration
- Auth required for: /search, /details endpoints only
- No auth required for: /health, /db-health, /login

**Tech Stack:** FastAPI + PyJWT for backend, Kotlin + JWT library for Android

---

## Backend Team: FastAPI Login Endpoint

### Task B1: Create /login Endpoint

**Files:**
- Create: `backend/routes/auth.py` (new file)
- Modify: `backend/main.py` (import and register auth routes)
- Create: `backend/models/auth.py` (login request/response models)

**Requirements:**

1. **Login endpoint:** `POST /login`
   - Request body:
     ```json
     {
       "login": "string",
       "password": "string",
       "hash_method": "CLAIR|MD5|SHA256"
     }
     ```
   - Response on success:
     ```json
     {
       "status": "success",
       "message": "Login successful",
       "token": "eyJ0eXAiOiJKV1QiLC...",
       "expires_in": 86400
     }
     ```
   - Response on failure:
     ```json
     {
       "status": "error",
       "message": "Invalid login or password",
       "log_location": "/path/to/logs"
     }
     ```

2. **Implementation details:**
   - Connect to SQL Server using existing `pyodbc` connection
   - Query table `USW_DAT` with fields `USW_LOGN`, `USW_PASS`
   - Support three hashing methods:
     - **CLAIR:** Compare password directly (password == USW_PASS)
     - **MD5:** `hashlib.md5(password.encode()).hexdigest()` == USW_PASS
     - **SHA256:** `hashlib.sha256(password.encode()).hexdigest()` == USW_PASS
   - Generate JWT token:
     - Payload: `{"login": login, "exp": datetime.utcnow() + timedelta(hours=24)}`
     - Secret key: use `config.SECRET_KEY` or `"dev-secret-key"` for POC
     - Algorithm: `HS256`
   - Store token in memory dict: `active_tokens = {token: login}` (use thread-safe dict if needed)
   - Log all attempts (success/failure) with timestamp, login, hash method, error details

3. **Error handling:**
   - Login not found: log "User not found: {login}" → return 401
   - Password mismatch: log "Password mismatch for {login} with method {hash_method}" → return 401
   - DB connection error: log full exception → return 500
   - JWT generation error: log full exception → return 500

4. **Logging:**
   - Use existing `AppLogger` pattern or simple logging
   - Log file: `backend/logs/auth.log`
   - Include: timestamp, login, hash_method, success/failure, full error details
   - Example: `[2026-05-26 14:30:45] LOGIN_ATTEMPT login=admin hash=SHA256 status=SUCCESS token=eyJ...`

**After implementation:**
- Write unit tests: `test_auth.py` (mock DB, test all 3 hash methods, test JWT expiry)
- Run: `pytest backend/tests/test_auth.py`
- Commit: "feat: add JWT authentication endpoint with 24h expiry and hashing options"

---

### Task B2: Protect /search and /details Endpoints

**Files:**
- Modify: `backend/routes/stock.py` (or wherever /search, /details are)
- Create: `backend/middleware/auth.py` (token verification middleware)

**Requirements:**

1. **Create token verification function:**
   ```python
   def verify_token(token: str) -> bool:
       try:
           payload = jwt.decode(token, config.SECRET_KEY, algorithms=["HS256"])
           return token in active_tokens  # Check if token is still valid
       except jwt.ExpiredSignatureError:
           return False
       except jwt.InvalidTokenError:
           return False
   ```

2. **Protect endpoints:**
   - `/search`: Require `Authorization: Bearer {token}` header
   - `/details`: Require `Authorization: Bearer {token}` header
   - `/health`, `/db-health`: NO auth required
   - `/login`: NO auth required

3. **Error response when token missing/invalid:**
   ```json
   {
     "status": "error",
     "message": "Unauthorized: invalid or missing token",
     "detail": "Please login first"
   }
   ```

4. **Implementation approach:**
   - Use FastAPI dependency injection: `def get_current_user(token: str = Header(None))`
   - Or create middleware for all protected routes
   - Log all auth failures: `[timestamp] AUTH_FAILED endpoint=/search token={token[:20]}... reason={reason}`

**After implementation:**
- Write tests: mock token header, test valid/invalid/expired tokens
- Commit: "feat: add JWT token verification for protected endpoints"

---

## Android Team: Login Screen & Token Storage

### Task A1: Create Login Activity

**Files:**
- Create: `app/src/main/java/com/example/stockapp/LoginActivity.kt`
- Create: `app/src/main/res/layout/activity_login.xml`
- Modify: `app/src/main/AndroidManifest.xml` (set LoginActivity as launcher)
- Modify: `app/src/main/java/com/example/stockapp/MainActivity.kt` (check token on startup)

**Requirements:**

1. **Login UI Layout:**
   - EditText: Login (email/username)
   - EditText: Password (inputType=password)
   - RadioGroup: Hash method
     - RadioButton: CLAIR
     - RadioButton: MD5
     - RadioButton: SHA256
   - Button: LOGIN
   - Button: CANCEL
   - TextView: Status message (error/loading)
   - ProgressBar: Loading spinner

2. **Logic:**
   - On LOGIN click:
     - Validate inputs (not empty)
     - Get selected hash method from RadioGroup
     - Show loading spinner
     - Call API: `POST /login` with {login, password, hash_method}
     - On success: Save token to SharedPreferences, start MainActivity
     - On error: Show error message + log details
   - On CANCEL: Exit app
   - Token storage: SharedPreferences key `"auth_token"`
   - Expiry storage: SharedPreferences key `"auth_token_expiry"` (Unix timestamp)

3. **Token validation on app start:**
   - In MainActivity.onCreate(): Check if token exists and not expired
   - If expired or missing: Launch LoginActivity
   - If valid: Show MainActivity normally

4. **Logging:**
   - Use AppLogger to log all login attempts
   - Include: login, hash_method, success/failure, error details
   - Example: `[timestamp] LOGIN_REQUEST login=admin hash=SHA256 status=SUCCESS`

**After implementation:**
- Write UI tests: Espresso or manual (launch app, enter login, verify token saved)
- Commit: "feat: add login screen with hashing options and token storage"

---

### Task A2: Add Authorization Header to API Calls

**Files:**
- Modify: `app/src/main/java/com/example/stockapp/api/ApiClient.kt`
- Modify: `app/src/main/java/com/example/stockapp/repositories/HealthRepository.kt`
- Modify: `app/src/main/java/com/example/stockapp/repositories/StockRepository.kt` (if exists)

**Requirements:**

1. **Create token manager:**
   ```kotlin
   object TokenManager {
       fun getToken(context: Context): String? {
           val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
           val token = prefs.getString("auth_token", null)
           val expiry = prefs.getLong("auth_token_expiry", 0)
           
           // Check if expired
           if (System.currentTimeMillis() > expiry) {
               clearToken(context)
               return null
           }
           return token
       }
       
       fun saveToken(context: Context, token: String, expiresIn: Int) {
           val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
           prefs.edit().apply {
               putString("auth_token", token)
               putLong("auth_token_expiry", System.currentTimeMillis() + (expiresIn * 1000))
               apply()
           }
       }
       
       fun clearToken(context: Context) {
           val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
           prefs.edit().remove("auth_token").apply()
       }
   }
   ```

2. **Add Retrofit interceptor:**
   - Create `AuthInterceptor` that adds `Authorization: Bearer {token}` header to /search and /details requests
   - Log all requests with token info

3. **Handle 401 responses:**
   - If API returns 401 (Unauthorized):
     - Clear stored token
     - Launch LoginActivity
     - Show message: "Session expired, please login again"

4. **Logging:**
   - Log all API calls with auth header presence
   - Log all 401 responses

**After implementation:**
- Write tests: mock API with 401, verify token cleared and LoginActivity launched
- Commit: "feat: add authorization header to protected API endpoints"

---

## Verification Checklist

### Backend (B1 + B2)
- [ ] POST /login works with all 3 hash methods
- [ ] JWT token generated with 24h expiry
- [ ] Token stored in-memory and verified on protected endpoints
- [ ] /search and /details require token
- [ ] /health, /db-health, /login don't require token
- [ ] All login attempts logged with details
- [ ] Invalid login shows clear error message
- [ ] Tests: unit tests for login, token verification, hash methods

### Android (A1 + A2)
- [ ] LoginActivity shows before MainActivity on first launch
- [ ] Radio buttons for 3 hash methods functional
- [ ] Login request sent with correct hash method
- [ ] Token saved to SharedPreferences on success
- [ ] Token expiry calculated and stored
- [ ] Token checked on app startup
- [ ] Authorization header added to /search, /details requests
- [ ] 401 response triggers logout and back to LoginActivity
- [ ] All attempts logged in AppLogger

---

## Test Plan (Both Teams)

### Backend Tests (`test_auth.py`)
```python
def test_login_clair_success():
    # Mock DB with user
    # Send login request with CLAIR method
    # Verify token returned

def test_login_md5_success():
    # Mock DB with MD5-hashed password
    # Send login with MD5 method
    # Verify token returned

def test_login_sha256_success():
    # Mock DB with SHA256-hashed password
    # Send login with SHA256 method
    # Verify token returned

def test_login_invalid_user():
    # Send login with non-existent user
    # Verify 401 response

def test_login_wrong_password():
    # Send login with correct user, wrong password
    # Verify 401 response

def test_token_verification_valid():
    # Generate valid JWT
    # Verify token passes verification

def test_token_verification_expired():
    # Generate JWT with past expiry
    # Verify token fails verification

def test_protected_endpoint_no_token():
    # Call /search without Authorization header
    # Verify 401 response

def test_protected_endpoint_invalid_token():
    # Call /search with invalid token
    # Verify 401 response
```

### Android Tests (LoginActivityTest.kt + integration)
```kotlin
// UI tests
- Verify LoginActivity launches on app start if no token
- Verify radio buttons work
- Verify login button calls API
- Verify token saved on success
- Verify error shown on failure

// Integration tests
- Verify MainActivity checks token on startup
- Verify 401 response logs out user
- Verify expired token triggers re-login
```

---

## Next Steps After Approval

1. **Backend team** implements B1 + B2, runs tests, commits
2. **Android team** implements A1 + A2, runs tests, commits
3. **Integration test:** Login on Android → calls /search → success
4. **Manual test:** Full login flow on device

---

## Success Criteria

✅ User can login with CLAIR/MD5/SHA256 password hashing
✅ Token stored locally and used for /search, /details
✅ 24h expiry enforced
✅ All login attempts logged for debugging
✅ Tests automated for both backend and Android
✅ Clear error messages guide user if login fails

