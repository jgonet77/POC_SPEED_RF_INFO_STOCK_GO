# POC Stock Mobile - Backend & Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a working backend (Python FastAPI with 3-layer architecture) and Android frontend that can test the connection to SQL Server WMS database.

**Architecture:** 
- **Backend:** Python FastAPI with Repository → Service → API layer pattern. Initial endpoint tests DB connectivity.
- **Frontend:** Android Kotlin (MVVM) with Retrofit HTTP client. Simple UI to test backend connection.
- **Database:** Microsoft SQL Server (WMS SPEED) - connection only, no data manipulation yet.

**Tech Stack:** 
- Backend: Python 3.10+, FastAPI, pyodbc (SQL Server), Pydantic
- Frontend: Kotlin, Android Studio, Retrofit, ViewModel, LiveData
- Database: Microsoft SQL Server

---

## File Structure

### Backend
```
backend/
├── main.py                      # FastAPI app entry point
├── config.py                    # Configuration (DB connection strings)
├── requirements.txt             # Python dependencies
├── models/
│   └── schemas.py              # Pydantic models (request/response DTOs)
├── repositories/
│   ├── __init__.py
│   ├── base_repository.py       # Base class with DB connection
│   └── health_repository.py     # Health check repo (test DB connection)
├── services/
│   ├── __init__.py
│   └── health_service.py        # Health check service (business logic)
├── routes/
│   ├── __init__.py
│   └── health.py                # Health check endpoints
└── tests/
    ├── test_health_repository.py
    ├── test_health_service.py
    └── test_health_routes.py
```

### Frontend
```
android/
├── app/build.gradle             # Project config, dependencies
├── app/src/main/
│   ├── AndroidManifest.xml      # Permissions, activities
│   ├── java/com/example/stockapp/
│   │   ├── MainActivity.kt       # Main activity (launcher)
│   │   ├── api/
│   │   │   └── ApiClient.kt      # Retrofit HTTP client singleton
│   │   ├── models/
│   │   │   └── HealthCheckResponse.kt  # Data class for API response
│   │   ├── repositories/
│   │   │   └── HealthRepository.kt     # Data repository (local + remote)
│   │   ├── viewmodels/
│   │   │   └── HealthViewModel.kt      # ViewModel for main screen
│   │   └── ui/
│   │       └── HealthCheckScreen.kt    # UI composables or Activities
│   └── res/
│       ├── layout/
│       │   └── activity_main.xml       # Layout for main screen
│       └── values/
│           ├── strings.xml             # String resources
│           └── colors.xml              # Color resources
└── tests/
    └── HealthViewModelTest.kt
```

---

## Task 1: Backend - Initialize Python Project

**Files:**
- Create: `backend/requirements.txt`
- Create: `backend/config.py`
- Create: `backend/main.py`

- [ ] **Step 1: Create backend directory structure**

```bash
mkdir -p backend/models backend/repositories backend/services backend/routes backend/tests
cd backend
```

- [ ] **Step 2: Create requirements.txt with dependencies**

```bash
cat > requirements.txt << 'EOF'
fastapi==0.104.1
uvicorn==0.24.0
pydantic==2.5.0
pyodbc==5.1.0
pytest==7.4.3
httpx==0.25.1
EOF
```

- [ ] **Step 3: Install dependencies**

```bash
pip install -r requirements.txt
```

Expected: All packages installed without errors.

- [ ] **Step 4: Create config.py for database configuration**

```python
# backend/config.py
import os
from typing import Optional

class Settings:
    """Database and app configuration"""
    
    # SQL Server connection
    SQL_SERVER_HOST: str = os.getenv("SQL_SERVER_HOST", "localhost")
    SQL_SERVER_PORT: int = int(os.getenv("SQL_SERVER_PORT", "1433"))
    SQL_SERVER_DB: str = os.getenv("SQL_SERVER_DB", "WMS_SPEED")
    SQL_SERVER_USER: str = os.getenv("SQL_SERVER_USER", "sa")
    SQL_SERVER_PASSWORD: str = os.getenv("SQL_SERVER_PASSWORD", "")
    
    # Connection string for pyodbc
    @property
    def connection_string(self) -> str:
        return (
            f"Driver={{ODBC Driver 17 for SQL Server}};"
            f"Server={self.SQL_SERVER_HOST},{self.SQL_SERVER_PORT};"
            f"Database={self.SQL_SERVER_DB};"
            f"UID={self.SQL_SERVER_USER};"
            f"PWD={self.SQL_SERVER_PASSWORD};"
        )
    
    # API settings
    API_HOST: str = os.getenv("API_HOST", "0.0.0.0")
    API_PORT: int = int(os.getenv("API_PORT", "8000"))

settings = Settings()
```

- [ ] **Step 5: Create main.py with FastAPI app**

```python
# backend/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes.health import router as health_router
from config import settings

app = FastAPI(
    title="Stock API",
    description="API for stock management POC",
    version="0.1.0"
)

# Enable CORS for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all origins (POC only, not production!)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(health_router, prefix="/api", tags=["health"])

@app.get("/")
def read_root():
    """Root endpoint - check if API is running"""
    return {"message": "Stock API is running"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app,
        host=settings.API_HOST,
        port=settings.API_PORT,
        reload=True
    )
```

- [ ] **Step 6: Create __init__.py files for modules**

```bash
touch backend/models/__init__.py
touch backend/repositories/__init__.py
touch backend/services/__init__.py
touch backend/routes/__init__.py
touch backend/tests/__init__.py
```

- [ ] **Step 7: Test the API runs (will fail on DB connection, that's ok)**

```bash
cd backend
python main.py
```

Expected: Server starts on `http://localhost:8000` (will error if routes are not yet defined, that's fine)

- [ ] **Step 8: Commit**

```bash
git add backend/requirements.txt backend/config.py backend/main.py backend/models/__init__.py backend/repositories/__init__.py backend/services/__init__.py backend/routes/__init__.py
git commit -m "feat: initialize FastAPI backend project structure"
```

---

## Task 2: Backend - Create Repository Layer (DB Access)

**Files:**
- Create: `backend/repositories/base_repository.py`
- Create: `backend/repositories/health_repository.py`

- [ ] **Step 1: Create base repository class**

```python
# backend/repositories/base_repository.py
import pyodbc
from config import settings
from typing import Optional

class BaseRepository:
    """Base class for database access"""
    
    def __init__(self):
        self.connection_string = settings.connection_string
        self._connection: Optional[pyodbc.Connection] = None
    
    def _get_connection(self) -> pyodbc.Connection:
        """Get or create a database connection"""
        if self._connection is None:
            try:
                self._connection = pyodbc.connect(self.connection_string)
            except pyodbc.Error as e:
                raise Exception(f"Database connection failed: {str(e)}")
        return self._connection
    
    def close(self):
        """Close database connection"""
        if self._connection:
            self._connection.close()
            self._connection = None
    
    def execute_query(self, query: str, params: tuple = ()) -> list:
        """Execute a SELECT query and return results"""
        try:
            connection = self._get_connection()
            cursor = connection.cursor()
            cursor.execute(query, params)
            return cursor.fetchall()
        except pyodbc.Error as e:
            raise Exception(f"Query execution failed: {str(e)}")
    
    def __del__(self):
        """Cleanup on object destruction"""
        self.close()
```

- [ ] **Step 2: Create health check repository**

```python
# backend/repositories/health_repository.py
from base_repository import BaseRepository

class HealthRepository(BaseRepository):
    """Repository for health check operations (DB connectivity tests)"""
    
    def check_database_connection(self) -> dict:
        """
        Test connection to SQL Server by executing a simple query
        Returns: Dict with connection status and server info
        """
        try:
            query = "SELECT @@VERSION as version, GETDATE() as server_time"
            result = self.execute_query(query)
            
            if result:
                row = result[0]
                return {
                    "status": "connected",
                    "server_version": row[0],
                    "server_time": str(row[1])
                }
            else:
                return {"status": "error", "message": "Query executed but no results"}
        
        except Exception as e:
            return {
                "status": "error",
                "message": str(e)
            }
```

- [ ] **Step 3: Commit**

```bash
git add backend/repositories/base_repository.py backend/repositories/health_repository.py
git commit -m "feat: add repository layer for database access"
```

---

## Task 3: Backend - Create Service Layer (Business Logic)

**Files:**
- Create: `backend/services/health_service.py`

- [ ] **Step 1: Create health service**

```python
# backend/services/health_service.py
from repositories.health_repository import HealthRepository

class HealthService:
    """Service layer for health check operations"""
    
    def __init__(self):
        self.repository = HealthRepository()
    
    def get_database_health(self) -> dict:
        """
        Check database health and return formatted response
        """
        db_check = self.repository.check_database_connection()
        
        return {
            "service": "Database Connection Test",
            "database_status": db_check.get("status"),
            "details": {
                "version": db_check.get("server_version"),
                "server_time": db_check.get("server_time"),
                "error": db_check.get("message")
            } if db_check.get("status") == "error" else {
                "version": db_check.get("server_version"),
                "server_time": db_check.get("server_time")
            }
        }
```

- [ ] **Step 2: Commit**

```bash
git add backend/services/health_service.py
git commit -m "feat: add service layer with health check logic"
```

---

## Task 4: Backend - Create Pydantic Models (Schemas)

**Files:**
- Create: `backend/models/schemas.py`

- [ ] **Step 1: Create response schemas**

```python
# backend/models/schemas.py
from pydantic import BaseModel
from typing import Optional, Dict, Any

class HealthCheckResponse(BaseModel):
    """Response model for health check endpoint"""
    service: str
    database_status: str
    details: Dict[str, Any]

class ErrorResponse(BaseModel):
    """Error response model"""
    error: str
    message: str
    details: Optional[Dict[str, Any]] = None
```

- [ ] **Step 2: Commit**

```bash
git add backend/models/schemas.py
git commit -m "feat: add pydantic schemas for API responses"
```

---

## Task 5: Backend - Create API Routes (Endpoints)

**Files:**
- Create: `backend/routes/health.py`

- [ ] **Step 1: Create health endpoints**

```python
# backend/routes/health.py
from fastapi import APIRouter, HTTPException
from services.health_service import HealthService
from models.schemas import HealthCheckResponse, ErrorResponse

router = APIRouter()
health_service = HealthService()

@router.get("/health/database", response_model=HealthCheckResponse)
def check_database_health():
    """
    Test database connection
    Returns server version and current time if connected
    """
    try:
        result = health_service.get_database_health()
        
        if result["database_status"] == "error":
            raise HTTPException(
                status_code=500,
                detail=result["details"].get("error", "Unknown database error")
            )
        
        return HealthCheckResponse(**result)
    
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Health check failed: {str(e)}"
        )

@router.get("/health/api")
def check_api_health():
    """Check API health (always returns 200 if this endpoint is reachable)"""
    return {
        "status": "healthy",
        "message": "API is running"
    }
```

- [ ] **Step 2: Commit**

```bash
git add backend/routes/health.py
git commit -m "feat: add health check endpoints"
```

---

## Task 6: Backend - Write Tests

**Files:**
- Create: `backend/tests/test_health_repository.py`
- Create: `backend/tests/test_health_service.py`
- Create: `backend/tests/test_health_routes.py`

- [ ] **Step 1: Write repository tests**

```python
# backend/tests/test_health_repository.py
import pytest
from repositories.health_repository import HealthRepository

def test_health_repository_initialization():
    """Test that repository can be instantiated"""
    repo = HealthRepository()
    assert repo is not None
    assert repo.connection_string is not None

def test_health_repository_connection_string_format():
    """Test that connection string is properly formatted"""
    repo = HealthRepository()
    conn_str = repo.connection_string
    assert "ODBC Driver 17 for SQL Server" in conn_str
    assert "Server=" in conn_str
    assert "Database=" in conn_str
```

- [ ] **Step 2: Write service tests**

```python
# backend/tests/test_health_service.py
import pytest
from services.health_service import HealthService

def test_health_service_initialization():
    """Test that service can be instantiated"""
    service = HealthService()
    assert service is not None
    assert service.repository is not None

def test_get_database_health_structure():
    """Test that health check returns proper structure"""
    service = HealthService()
    result = service.get_database_health()
    
    # Verify response structure
    assert "service" in result
    assert "database_status" in result
    assert "details" in result
    assert result["service"] == "Database Connection Test"
```

- [ ] **Step 3: Write route tests (mock DB)**

```python
# backend/tests/test_health_routes.py
import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_api_health_endpoint():
    """Test that API health endpoint works"""
    response = client.get("/api/health/api")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"

def test_root_endpoint():
    """Test root endpoint"""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "message" in data
```

- [ ] **Step 4: Run tests**

```bash
cd backend
pytest tests/ -v
```

Expected: Tests pass (except database connection test if SQL Server not available)

- [ ] **Step 5: Commit**

```bash
git add backend/tests/
git commit -m "test: add unit tests for repository, service, and routes"
```

---

## Task 7: Frontend - Initialize Android Project

**Files:**
- Create: `android/build.gradle` (root)
- Create: `android/app/build.gradle`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/local.properties`

- [ ] **Step 1: Create Android project directories**

```bash
mkdir -p android/app/src/main/java/com/example/stockapp/api
mkdir -p android/app/src/main/java/com/example/stockapp/models
mkdir -p android/app/src/main/java/com/example/stockapp/repositories
mkdir -p android/app/src/main/java/com/example/stockapp/viewmodels
mkdir -p android/app/src/main/res/layout
mkdir -p android/app/src/main/res/values
mkdir -p android/app/src/test/java/com/example/stockapp
```

- [ ] **Step 2: Create root build.gradle**

```gradle
// android/build.gradle
plugins {
    id 'com.android.application' version '8.2.0' apply false
}

ext {
    minSdkVersion = 24
    targetSdkVersion = 34
    compileSdkVersion = 34
}
```

- [ ] **Step 3: Create app/build.gradle**

```gradle
// android/app/build.gradle
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}

android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.example.stockapp"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "0.1.0"
    }
    
    buildTypes {
        release {
            minifyEnabled false
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
}

dependencies {
    // Core Android
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Kotlin
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.21'
    
    // Lifecycle & ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
    
    // Retrofit for HTTP
    implementation 'com.squareup.retrofit2:retrofit:2.10.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.10.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

- [ ] **Step 4: Create AndroidManifest.xml**

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Internet permission for API calls -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:allowBackup="true"
        android:label="Stock App POC"
        android:theme="@android:style/Theme.Material.Light.DarkActionBar">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
```

- [ ] **Step 5: Create local.properties**

```properties
# android/local.properties
sdk.dir=/path/to/android/sdk
# Update this path to your Android SDK location
```

- [ ] **Step 6: Commit**

```bash
git add android/
git commit -m "feat: initialize Android project structure"
```

---

## Task 8: Frontend - Create API Client (Retrofit)

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/api/ApiClient.kt`
- Create: `android/app/src/main/java/com/example/stockapp/api/StockApiService.kt`

- [ ] **Step 1: Create API Client singleton**

```kotlin
// android/app/src/main/java/com/example/stockapp/api/ApiClient.kt
package com.example.stockapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Change this to your backend server IP/hostname
    private const val BASE_URL = "http://10.0.2.2:8000/"  // 10.0.2.2 for Android emulator
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val apiService: StockApiService by lazy {
        retrofit.create(StockApiService::class.java)
    }
}
```

Note: `10.0.2.2` is special address for Android emulator to access host machine. For physical device, use your PC's local IP (e.g., `192.168.1.100`).

- [ ] **Step 2: Create Retrofit service interface**

```kotlin
// android/app/src/main/java/com/example/stockapp/api/StockApiService.kt
package com.example.stockapp.api

import retrofit2.Call
import retrofit2.http.GET

interface StockApiService {
    
    @GET("api/health/database")
    fun checkDatabaseHealth(): Call<HealthCheckResponse>
    
    @GET("api/health/api")
    fun checkApiHealth(): Call<ApiHealthResponse>
}

data class HealthCheckResponse(
    val service: String,
    val database_status: String,
    val details: Map<String, Any>
)

data class ApiHealthResponse(
    val status: String,
    val message: String
)
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/api/
git commit -m "feat: add Retrofit API client"
```

---

## Task 9: Frontend - Create Data Models

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/models/HealthCheckResponse.kt`

- [ ] **Step 1: Create data classes**

```kotlin
// android/app/src/main/java/com/example/stockapp/models/HealthCheckResponse.kt
package com.example.stockapp.models

data class HealthCheckResponse(
    val service: String,
    val database_status: String,
    val details: Map<String, Any>
)

data class ApiHealthResponse(
    val status: String,
    val message: String
)
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/models/
git commit -m "feat: add data models for API responses"
```

---

## Task 10: Frontend - Create Repository Layer

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/repositories/HealthRepository.kt`

- [ ] **Step 1: Create repository for health checks**

```kotlin
// android/app/src/main/java/com/example/stockapp/repositories/HealthRepository.kt
package com.example.stockapp.repositories

import android.util.Log
import com.example.stockapp.api.ApiClient
import com.example.stockapp.models.HealthCheckResponse
import com.example.stockapp.models.ApiHealthResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HealthRepository {
    
    fun checkDatabaseHealth(
        onSuccess: (HealthCheckResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.checkDatabaseHealth().enqueue(object : Callback<HealthCheckResponse> {
            override fun onResponse(call: Call<HealthCheckResponse>, response: Response<HealthCheckResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    onError("API Error: ${response.code()} ${response.message()}")
                }
            }
            
            override fun onFailure(call: Call<HealthCheckResponse>, t: Throwable) {
                onError("Connection Error: ${t.message}")
                Log.e("HealthRepository", "Database health check failed", t)
            }
        })
    }
    
    fun checkApiHealth(
        onSuccess: (ApiHealthResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.checkApiHealth().enqueue(object : Callback<ApiHealthResponse> {
            override fun onResponse(call: Call<ApiHealthResponse>, response: Response<ApiHealthResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    onSuccess(response.body()!!)
                } else {
                    onError("API Error: ${response.code()} ${response.message()}")
                }
            }
            
            override fun onFailure(call: Call<ApiHealthResponse>, t: Throwable) {
                onError("Connection Error: ${t.message}")
                Log.e("HealthRepository", "API health check failed", t)
            }
        })
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/repositories/
git commit -m "feat: add health repository for API communication"
```

---

## Task 11: Frontend - Create ViewModel

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/viewmodels/HealthViewModel.kt`

- [ ] **Step 1: Create ViewModel for health checks**

```kotlin
// android/app/src/main/java/com/example/stockapp/viewmodels/HealthViewModel.kt
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
                _databaseVersion.value = response.details["version"]?.toString() ?: "Unknown"
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
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/viewmodels/
git commit -m "feat: add HealthViewModel for business logic"
```

---

## Task 12: Frontend - Create Main Activity & Layouts

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/MainActivity.kt`
- Create: `android/app/src/main/res/layout/activity_main.xml`
- Create: `android/app/src/main/res/values/strings.xml`
- Create: `android/app/src/main/res/values/colors.xml`

- [ ] **Step 1: Create strings.xml**

```xml
<!-- android/app/src/main/res/values/strings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Stock App POC</string>
    <string name="api_health_title">API Health</string>
    <string name="database_health_title">Database Health</string>
    <string name="test_api_button">Test API Connection</string>
    <string name="test_database_button">Test Database Connection</string>
    <string name="status_label">Status: </string>
    <string name="version_label">Server Version: </string>
    <string name="time_label">Server Time: </string>
    <string name="error_label">Error: </string>
</resources>
```

- [ ] **Step 2: Create colors.xml**

```xml
<!-- android/app/src/main/res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#BB86FC</color>
    <color name="purple_500">#6200EE</color>
    <color name="teal_200">#03DAC5</color>
    <color name="black">#000000</color>
    <color name="white">#FFFFFF</color>
    <color name="error_red">#FF5252</color>
    <color name="success_green">#4CAF50</color>
</resources>
```

- [ ] **Step 3: Create activity_main.xml layout**

```xml
<!-- android/app/src/main/res/layout/activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Stock App - Connection Tester"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="24dp" />
    
    <!-- API Health Section -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:layout_marginBottom="20dp"
        android:padding="12dp"
        android:background="@android:color/darker_gray"
        android:alpha="0.1">
        
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/api_health_title"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />
        
        <TextView
            android:id="@+id/apiStatusText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Not tested"
            android:textSize="16sp"
            android:layout_marginBottom="12dp" />
        
        <Button
            android:id="@+id/testApiButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/test_api_button" />
    </LinearLayout>
    
    <!-- Database Health Section -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:layout_marginBottom="20dp"
        android:padding="12dp"
        android:background="@android:color/darker_gray"
        android:alpha="0.1">
        
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/database_health_title"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />
        
        <TextView
            android:id="@+id/databaseStatusText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Not tested"
            android:textSize="16sp"
            android:layout_marginBottom="8dp" />
        
        <TextView
            android:id="@+id/databaseVersionText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text=""
            android:textSize="14sp"
            android:layout_marginBottom="4dp" />
        
        <TextView
            android:id="@+id/databaseTimeText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text=""
            android:textSize="14sp"
            android:layout_marginBottom="12dp" />
        
        <Button
            android:id="@+id/testDatabaseButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/test_database_button" />
    </LinearLayout>
    
    <!-- Error Section -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/error_label"
        android:textSize="14sp"
        android:layout_marginBottom="8dp" />
    
    <TextView
        android:id="@+id/errorText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text=""
        android:textSize="12sp"
        android:textColor="@color/error_red"
        android:scrollbars="vertical"
        android:maxHeight="80dp" />
    
</LinearLayout>
```

- [ ] **Step 4: Create MainActivity.kt**

```kotlin
// android/app/src/main/java/com/example/stockapp/MainActivity.kt
package com.example.stockapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.viewmodels.HealthViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: HealthViewModel
    
    private lateinit var apiStatusText: TextView
    private lateinit var databaseStatusText: TextView
    private lateinit var databaseVersionText: TextView
    private lateinit var databaseTimeText: TextView
    private lateinit var errorText: TextView
    private lateinit var testApiButton: Button
    private lateinit var testDatabaseButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(HealthViewModel::class.java)
        
        // Find views
        apiStatusText = findViewById(R.id.apiStatusText)
        databaseStatusText = findViewById(R.id.databaseStatusText)
        databaseVersionText = findViewById(R.id.databaseVersionText)
        databaseTimeText = findViewById(R.id.databaseTimeText)
        errorText = findViewById(R.id.errorText)
        testApiButton = findViewById(R.id.testApiButton)
        testDatabaseButton = findViewById(R.id.testDatabaseButton)
        
        // Setup observers
        viewModel.apiHealthStatus.observe(this) { status ->
            apiStatusText.text = status
        }
        
        viewModel.databaseHealthStatus.observe(this) { status ->
            databaseStatusText.text = status
        }
        
        viewModel.databaseVersion.observe(this) { version ->
            databaseVersionText.text = if (version.isNotEmpty()) "Version: $version" else ""
        }
        
        viewModel.databaseTime.observe(this) { time ->
            databaseTimeText.text = if (time.isNotEmpty()) "Server Time: $time" else ""
        }
        
        viewModel.errorMessage.observe(this) { error ->
            errorText.text = if (error.isNotEmpty()) "Error: $error" else ""
        }
        
        // Setup button listeners
        testApiButton.setOnClickListener {
            viewModel.checkApiHealth()
        }
        
        testDatabaseButton.setOnClickListener {
            viewModel.checkDatabaseHealth()
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/MainActivity.kt
git add android/app/src/main/res/
git commit -m "feat: add MainActivity and UI layouts"
```

---

## Task 13: Create Mode Opératoire (Setup Guide)

**Files:**
- Create: `docs/MODE_OPERATOIRE.html`

- [ ] **Step 1: Write setup guide**

Create the file with complete instructions for:
- Installing Python and dependencies
- Configuring SQL Server connection
- Running the backend
- Setting up Android environment
- Running the frontend
- Testing both on PC and physical device

(See next section for complete content)

- [ ] **Step 2: Commit**

```bash
git add docs/MODE_OPERATOIRE.html
git commit -m "docs: add complete setup guide for POC testing"
```

---

## Summary

**Backend (Python FastAPI):** 
- ✅ 3-layer architecture (API → Service → Repository)
- ✅ SQL Server connection test endpoint
- ✅ Health check endpoints
- ✅ Proper error handling

**Frontend (Android Kotlin):**
- ✅ MVVM architecture (ViewModel + Repository)
- ✅ Retrofit HTTP client
- ✅ Simple UI with two test buttons
- ✅ LiveData for reactive updates

**Testing:**
- ✅ Both endpoints testable from Android app
- ✅ Connection errors clearly displayed
- ✅ Database info displayed if connection succeeds

**Next Steps:**
- After validation, add login/authentication endpoint
- Add stock search endpoint
- Add stock details endpoint
- Expand Android UI with search and details screens

---
