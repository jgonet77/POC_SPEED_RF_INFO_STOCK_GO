# Phase 7 Part 2: Advanced Stock Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement flexible stock search by article code, location, or storage number—filtering by selected activity and returning matching stock items with quantity and quality info.

**Architecture:**
- Backend: FastAPI `/api/stock/search` endpoint with pyodbc SQL Server query supporting OR-based criteria matching
- Android: MVVM pattern with StockSearchActivity, ViewModel, Repository, and Retrofit API client
- Data flow: User enters search criteria → API filters by activity + criteria → returns 0-N stock items → displayed in list
- Auth required for all stock endpoints (token validation via AuthInterceptor)

**Tech Stack:** FastAPI + pyodbc (backend), Kotlin + Retrofit + MVVM + LiveData (Android)

---

## Backend Tasks

### Task B1: Stock Models & Repository

**Files:**
- Create: `backend/models/stock.py`
- Create: `backend/repositories/stock_repository.py`
- Create: `backend/tests/test_stock_search.py`

- [ ] **Step 1: Write stock model test file**

Create `backend/tests/test_stock_search.py`:

```python
import pytest
from models.stock import StockSearchRequest, StockSearchResponse, StockItem


def test_stock_search_request_validation():
    """Validate StockSearchRequest with at least one criterion."""
    # Valid request with article code
    req = StockSearchRequest(art_code="ABC123", stk_lieu=None, stk_nosu=None)
    assert req.art_code == "ABC123"
    
    # Valid request with all criteria
    req = StockSearchRequest(art_code="ABC123", stk_lieu="A-01", stk_nosu="SUP-001")
    assert req.art_code == "ABC123"
    
    # Invalid: all None
    with pytest.raises(ValueError):
        StockSearchRequest(art_code=None, stk_lieu=None, stk_nosu=None)


def test_stock_item_response():
    """Verify StockItem has all required fields."""
    item = StockItem(
        art_code="ABC123",
        stk_lieu="A-01",
        stk_nosu="SUP-001",
        qua_code="GOOD",
        stk_qte=100
    )
    assert item.art_code == "ABC123"
    assert item.stk_lieu == "A-01"
    assert item.stk_nosu == "SUP-001"
    assert item.qua_code == "GOOD"
    assert item.stk_qte == 100


def test_stock_search_response():
    """Verify StockSearchResponse structure."""
    items = [
        StockItem(art_code="ABC", stk_lieu="A-01", stk_nosu="S1", qua_code="GOOD", stk_qte=50),
        StockItem(art_code="DEF", stk_lieu="B-02", stk_nosu="S2", qua_code="OK", stk_qte=30),
    ]
    resp = StockSearchResponse(status="success", message="Found 2 items", items=items)
    assert resp.status == "success"
    assert len(resp.items) == 2
    assert resp.items[0].art_code == "ABC"
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd D:\Projects\POC_SPEED_RF_INFO_STOCK
python -m pytest backend/tests/test_stock_search.py -v
```

Expected output: FAIL - models not found

- [ ] **Step 3: Create stock models**

Create `backend/models/stock.py`:

```python
from pydantic import BaseModel, validator
from typing import Optional, List


class StockSearchRequest(BaseModel):
    """Request model for stock search."""
    art_code: Optional[str] = None
    stk_lieu: Optional[str] = None
    stk_nosu: Optional[str] = None
    act_code: str  # Activity code (required, filtered from Android)
    
    @validator('art_code', 'stk_lieu', 'stk_nosu', pre=True, always=True)
    def trim_strings(cls, v):
        if isinstance(v, str):
            return v.strip()
        return v
    
    @validator('act_code')
    def act_code_required(cls, v):
        if not v or not v.strip():
            raise ValueError("Activity code (act_code) is required")
        return v.strip()
    
    def has_criteria(self) -> bool:
        """Ensure at least one search criterion is provided."""
        has_any = bool(self.art_code or self.stk_lieu or self.stk_nosu)
        if not has_any:
            raise ValueError("At least one of art_code, stk_lieu, or stk_nosu must be provided")
        return True


class StockItem(BaseModel):
    """Single stock item in search results."""
    art_code: str
    stk_lieu: str
    stk_nosu: str
    qua_code: str
    stk_qte: int
    
    class Config:
        json_schema_extra = {
            "example": {
                "art_code": "ARTICLE-001",
                "stk_lieu": "A-01-01",
                "stk_nosu": "SUPP-12345",
                "qua_code": "GOOD",
                "stk_qte": 150
            }
        }


class StockSearchResponse(BaseModel):
    """Response model for stock search."""
    status: str  # "success" or "error"
    message: str
    items: List[StockItem] = []
    
    class Config:
        json_schema_extra = {
            "example": {
                "status": "success",
                "message": "Found 3 matching items",
                "items": [
                    {
                        "art_code": "ARTICLE-001",
                        "stk_lieu": "A-01-01",
                        "stk_nosu": "SUPP-12345",
                        "qua_code": "GOOD",
                        "stk_qte": 150
                    }
                ]
            }
        }
```

- [ ] **Step 4: Create stock repository**

Create `backend/repositories/stock_repository.py`:

```python
from typing import List, Dict, Optional
import pyodbc
from repositories.base_repository import BaseRepository, DatabaseConnectionError, QueryError
from models.stock import StockItem, StockSearchRequest


class StockRepository(BaseRepository):
    """Repository for stock data queries."""
    
    def search_by_activity(self, request: StockSearchRequest) -> List[Dict]:
        """
        Search stock by article code, location, or storage number within activity.
        
        Returns list of dicts with keys: art_code, stk_lieu, stk_nosu, qua_code, stk_qte
        
        Raises:
            QueryError: on database query failure
            DatabaseConnectionError: on connection failure
        """
        # Validate request
        request.has_criteria()
        
        # Build WHERE clause with OR logic
        conditions = [f"ACT_CODE = '{request.act_code}'"]
        
        if request.art_code:
            conditions.append(f"ART_CODE LIKE '%{request.art_code}%'")
        if request.stk_lieu:
            conditions.append(f"STK_LIEU LIKE '%{request.stk_lieu}%'")
        if request.stk_nosu:
            conditions.append(f"STK_NOSU LIKE '%{request.stk_nosu}%'")
        
        # First condition is activity filter (AND), rest are OR
        where_clause = conditions[0] + " AND (" + " OR ".join(conditions[1:]) + ")"
        
        query = f"""
            SELECT ART_CODE, STK_LIEU, STK_NOSU, QUA_CODE, STK_QTE
            FROM STK_PAR
            WHERE {where_clause}
            ORDER BY ART_CODE, STK_LIEU
        """
        
        try:
            rows = self.execute_query(query)
        except (DatabaseConnectionError, QueryError) as exc:
            raise QueryError(f"Stock search failed: {str(exc)}") from exc
        
        # Convert rows to dicts
        results = []
        for row in rows:
            results.append({
                "art_code": row[0],
                "stk_lieu": row[1],
                "stk_nosu": row[2],
                "qua_code": row[3],
                "stk_qte": row[4],
            })
        
        return results
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
python -m pytest backend/tests/test_stock_search.py -v
```

Expected output: PASS (3 passed)

- [ ] **Step 6: Commit**

```bash
cd D:\Projects\POC_SPEED_RF_INFO_STOCK
git add backend/models/stock.py backend/repositories/stock_repository.py backend/tests/test_stock_search.py
git commit -m "feat: add stock search models and repository with flexible criteria matching"
```

---

### Task B2: Stock Service & API Endpoint

**Files:**
- Create: `backend/services/stock_service.py`
- Modify: `backend/routes/stock.py` (or create new)
- Modify: `backend/main.py` (register routes)

- [ ] **Step 1: Create stock service**

Create `backend/services/stock_service.py`:

```python
from typing import List
from models.stock import StockSearchRequest, StockItem, StockSearchResponse
from repositories.stock_repository import StockRepository
from repositories.base_repository import QueryError


class StockService:
    """Service layer for stock operations."""
    
    def __init__(self):
        self.repository = StockRepository()
    
    def search_stock(self, request: StockSearchRequest) -> StockSearchResponse:
        """
        Search stock by activity and flexible criteria.
        
        Returns:
            StockSearchResponse with items matching any criterion within activity
        """
        try:
            # Validate request
            request.has_criteria()
            
            # Query repository
            results = self.repository.search_by_activity(request)
            
            # Convert to StockItem objects
            items = [
                StockItem(
                    art_code=row["art_code"],
                    stk_lieu=row["stk_lieu"],
                    stk_nosu=row["stk_nosu"],
                    qua_code=row["qua_code"],
                    stk_qte=row["stk_qte"],
                )
                for row in results
            ]
            
            # Build response
            if len(items) == 0:
                message = "No stock items found matching criteria"
            else:
                message = f"Found {len(items)} item(s)"
            
            return StockSearchResponse(
                status="success",
                message=message,
                items=items
            )
        
        except ValueError as e:
            # Validation error
            return StockSearchResponse(
                status="error",
                message=f"Invalid search criteria: {str(e)}",
                items=[]
            )
        except QueryError as e:
            # Database error
            return StockSearchResponse(
                status="error",
                message=f"Database error: {str(e)}",
                items=[]
            )
        except Exception as e:
            # Unexpected error
            return StockSearchResponse(
                status="error",
                message=f"Unexpected error: {str(e)}",
                items=[]
            )
```

- [ ] **Step 2: Create stock routes**

Create `backend/routes/stock.py`:

```python
from fastapi import APIRouter, Header, Depends
from models.stock import StockSearchRequest, StockSearchResponse
from services.stock_service import StockService
from logging import getLogger

logger = getLogger(__name__)

router = APIRouter()
stock_service = StockService()


def verify_token(authorization: str = Header(None)) -> str:
    """Verify token from Authorization header."""
    if not authorization or not authorization.startswith("Bearer "):
        raise ValueError("Missing or invalid Authorization header")
    return authorization[7:]  # Remove "Bearer " prefix


@router.post("/api/stock/search")
async def search_stock(
    request: StockSearchRequest,
    token: str = Depends(verify_token)
) -> StockSearchResponse:
    """
    Search stock by article code, location, or storage number.
    
    Filters results by activity code (act_code).
    Returns 0-N results matching any criterion.
    
    Request:
    {
        "art_code": "ABC123",     (optional)
        "stk_lieu": "A-01",       (optional)
        "stk_nosu": "SUPP-001",   (optional)
        "act_code": "BKS"         (required)
    }
    """
    logger.info(
        f"STOCK_SEARCH_REQUEST act_code={request.act_code} "
        f"art_code={request.art_code} stk_lieu={request.stk_lieu} stk_nosu={request.stk_nosu}"
    )
    
    response = stock_service.search_stock(request)
    
    logger.info(
        f"STOCK_SEARCH_RESPONSE act_code={request.act_code} "
        f"status={response.status} items_found={len(response.items)}"
    )
    
    return response
```

- [ ] **Step 3: Register routes in main.py**

Modify `backend/main.py` to include stock routes:

```python
from fastapi import FastAPI
from routes import auth, stock  # Add stock import

app = FastAPI()

# Register routers
app.include_router(auth.router)
app.include_router(stock.router)

# ... rest of main.py
```

- [ ] **Step 4: Test endpoint manually**

```bash
# Start backend server
python backend/main.py

# In another terminal, test with curl
curl -X POST http://localhost:8000/api/stock/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"art_code": "ARTICLE", "stk_lieu": null, "stk_nosu": null, "act_code": "BKS"}'
```

Expected output: JSON response with status, message, and items array

- [ ] **Step 5: Commit**

```bash
git add backend/services/stock_service.py backend/routes/stock.py backend/main.py
git commit -m "feat: add stock search service and API endpoint with activity filtering"
```

---

## Android Tasks

### Task A1: Stock Models

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/models/StockModels.kt`

- [ ] **Step 1: Create stock models**

Create `android/app/src/main/java/com/example/stockapp/models/StockModels.kt`:

```kotlin
package com.example.stockapp.models

import com.google.gson.annotations.SerializedName


data class StockSearchRequest(
    @SerializedName("art_code")
    val artCode: String?,
    @SerializedName("stk_lieu")
    val stkLieu: String?,
    @SerializedName("stk_nosu")
    val stkNosu: String?,
    @SerializedName("act_code")
    val actCode: String
)


data class StockItem(
    @SerializedName("art_code")
    val artCode: String,
    @SerializedName("stk_lieu")
    val stkLieu: String,
    @SerializedName("stk_nosu")
    val stkNosu: String,
    @SerializedName("qua_code")
    val quaCode: String,
    @SerializedName("stk_qte")
    val stkQte: Int
)


data class StockSearchResponse(
    val status: String,
    val message: String,
    val items: List<StockItem>
)
```

- [ ] **Step 2: Verify compilation**

```bash
cd D:\Projects\POC_SPEED_RF_INFO_STOCK\android
./gradlew build
```

Expected: Build succeeds (no compilation errors)

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/models/StockModels.kt
git commit -m "feat: add stock search data models with JSON serialization"
```

---

### Task A2: Stock Repository & API Interface

**Files:**
- Modify: `android/app/src/main/java/com/example/stockapp/api/StockApiService.kt` (create if missing)
- Create: `android/app/src/main/java/com/example/stockapp/repositories/StockRepository.kt`

- [ ] **Step 1: Create/update API service interface**

Create or modify `android/app/src/main/java/com/example/stockapp/api/StockApiService.kt`:

```kotlin
package com.example.stockapp.api

import com.example.stockapp.models.StockSearchRequest
import com.example.stockapp.models.StockSearchResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface StockApiService {
    
    @POST("/api/stock/search")
    fun searchStock(@Body request: StockSearchRequest): Call<StockSearchResponse>
}
```

- [ ] **Step 2: Create stock repository**

Create `android/app/src/main/java/com/example/stockapp/repositories/StockRepository.kt`:

```kotlin
package com.example.stockapp.repositories

import com.example.stockapp.api.ApiClient
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.managers.ActivityManager
import com.example.stockapp.models.StockItem
import com.example.stockapp.models.StockSearchRequest
import com.example.stockapp.models.StockSearchResponse
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class StockRepository {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    fun searchStock(
        context: android.content.Context,
        artCode: String?,
        stkLieu: String?,
        stkNosu: String?,
        callback: (Result<List<StockItem>>) -> Unit
    ) {
        // Get selected activity code
        val actCode = ActivityManager.getActivityCode(context)
        if (actCode.isNullOrEmpty()) {
            AppLogger.log("[${getCurrentTimestamp()}] STOCK_SEARCH_FAILED error=no_activity_selected")
            callback(Result.failure(Exception("No activity selected")))
            return
        }
        
        // Build request
        val request = StockSearchRequest(
            artCode = artCode?.trim(),
            stkLieu = stkLieu?.trim(),
            stkNosu = stkNosu?.trim(),
            actCode = actCode
        )
        
        // Log request
        AppLogger.log(
            "[${getCurrentTimestamp()}] STOCK_SEARCH_REQUEST " +
            "act_code=$actCode art_code=$artCode stk_lieu=$stkLieu stk_nosu=$stkNosu"
        )
        
        // Make API call
        val apiService = ApiClient.apiService
        val call = apiService.searchStock(request)
        
        call.enqueue(object : Callback<StockSearchResponse> {
            override fun onResponse(call: retrofit2.Call<StockSearchResponse>, response: Response<StockSearchResponse>) {
                response.body()?.let { body ->
                    AppLogger.log(
                        "[${getCurrentTimestamp()}] STOCK_SEARCH_RESPONSE " +
                        "status=${body.status} items_count=${body.items.size}"
                    )
                    
                    if (response.isSuccessful && body.status == "success") {
                        callback(Result.success(body.items))
                    } else {
                        callback(Result.failure(Exception(body.message)))
                    }
                } ?: run {
                    AppLogger.log("[${getCurrentTimestamp()}] STOCK_SEARCH_FAILED error=null_response")
                    callback(Result.failure(Exception("Empty response from server")))
                }
            }
            
            override fun onFailure(call: retrofit2.Call<StockSearchResponse>, t: Throwable) {
                AppLogger.log(
                    "[${getCurrentTimestamp()}] STOCK_SEARCH_FAILED " +
                    "error=${t.message?.take(100)}"
                )
                callback(Result.failure(t))
            }
        })
    }
    
    private fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd D:\Projects\POC_SPEED_RF_INFO_STOCK\android
./gradlew build
```

Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/api/StockApiService.kt \
        android/app/src/main/java/com/example/stockapp/repositories/StockRepository.kt
git commit -m "feat: add stock API interface and repository with activity filtering"
```

---

### Task A3: Stock Search ViewModel

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/viewmodels/StockSearchViewModel.kt`

- [ ] **Step 1: Create ViewModel**

Create `android/app/src/main/java/com/example/stockapp/viewmodels/StockSearchViewModel.kt`:

```kotlin
package com.example.stockapp.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.models.StockItem
import com.example.stockapp.repositories.StockRepository


class StockSearchViewModel(private val context: Context) : ViewModel() {
    
    private val repository = StockRepository()
    
    private val _searchResults = MutableLiveData<List<StockItem>>()
    val searchResults: LiveData<List<StockItem>> = _searchResults
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData("")
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _hasSearched = MutableLiveData(false)
    val hasSearched: LiveData<Boolean> = _hasSearched
    
    
    fun searchStock(artCode: String?, stkLieu: String?, stkNosu: String?) {
        // Validate at least one criterion is provided
        if (artCode.isNullOrBlank() && stkLieu.isNullOrBlank() && stkNosu.isNullOrBlank()) {
            _errorMessage.value = "Please enter at least one search criterion"
            AppLogger.log("STOCK_SEARCH_VALIDATION_ERROR: no criteria provided")
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = ""
        
        repository.searchStock(
            context,
            artCode,
            stkLieu,
            stkNosu
        ) { result ->
            _isLoading.value = false
            _hasSearched.value = true
            
            result.onSuccess { items ->
                _searchResults.value = items
                _errorMessage.value = ""
                
                if (items.isEmpty()) {
                    AppLogger.log("STOCK_SEARCH_SUCCESS: no items found")
                } else {
                    AppLogger.log("STOCK_SEARCH_SUCCESS: found ${items.size} items")
                }
            }
            
            result.onFailure { error ->
                _searchResults.value = emptyList()
                _errorMessage.value = error.message ?: "Unknown error occurred"
                AppLogger.log("STOCK_SEARCH_ERROR: ${error.message}")
            }
        }
    }
    
    fun clearSearch() {
        _searchResults.value = emptyList()
        _errorMessage.value = ""
        _hasSearched.value = false
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd D:\Projects\POC_SPEED_RF_INFO_STOCK\android
./gradlew build
```

Expected: Build succeeds

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/viewmodels/StockSearchViewModel.kt
git commit -m "feat: add stock search ViewModel with LiveData observables"
```

---

### Task A4: Stock Search Activity & Layout

**Files:**
- Create: `android/app/src/main/java/com/example/stockapp/StockSearchActivity.kt`
- Create: `android/app/src/main/res/layout/activity_stock_search.xml`
- Modify: `android/app/src/main/res/values/strings.xml` (add stock search strings)
- Modify: `android/app/src/main/AndroidManifest.xml` (register activity)

- [ ] **Step 1: Add string resources**

Add to `android/app/src/main/res/values/strings.xml`:

```xml
<string name="stock_search_title">Stock Search</string>
<string name="stock_search_article_label">Article Code</string>
<string name="stock_search_location_label">Location (Lieu)</string>
<string name="stock_search_storage_label">Storage Number (Nosu)</string>
<string name="stock_search_button">Search</string>
<string name="stock_search_clear_button">Clear</string>
<string name="stock_search_no_results">No stock items found</string>
<string name="stock_search_error_criteria">Enter at least one search criterion</string>
<string name="stock_search_column_article">Article</string>
<string name="stock_search_column_location">Location</string>
<string name="stock_search_column_storage">Storage #</string>
<string name="stock_search_column_quality">Quality</string>
<string name="stock_search_column_quantity">Qty</string>
<string name="stock_search_loading">Searching...</string>
```

- [ ] **Step 2: Create layout XML**

Create `android/app/src/main/res/layout/activity_stock_search.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleTextView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/stock_search_title"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <!-- Search Criteria Section -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp"
        android:background="@android:color/darker_gray"
        android:layout_marginBottom="16dp">

        <EditText
            android:id="@+id/articleCodeInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/stock_search_article_label"
            android:inputType="text"
            android:layout_marginBottom="8dp" />

        <EditText
            android:id="@+id/locationInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/stock_search_location_label"
            android:inputType="text"
            android:layout_marginBottom="8dp" />

        <EditText
            android:id="@+id/storageNumberInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/stock_search_storage_label"
            android:inputType="text"
            android:layout_marginBottom="12dp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:spacing="8dp">

            <Button
                android:id="@+id/searchButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/stock_search_button" />

            <Button
                android:id="@+id/clearButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/stock_search_clear_button" />
        </LinearLayout>
    </LinearLayout>

    <!-- Status and Error Messages -->
    <TextView
        android:id="@+id/statusMessageTextView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@android:color/holo_red_dark"
        android:layout_marginBottom="12dp"
        android:visibility="gone" />

    <ProgressBar
        android:id="@+id/loadingProgressBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:indeterminate="true"
        android:visibility="gone"
        android:layout_marginBottom="12dp" />

    <!-- Results List -->
    <ListView
        android:id="@+id/resultsListView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="8dp" />

    <!-- Empty State -->
    <TextView
        android:id="@+id/emptyStateTextView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:text="@string/stock_search_no_results"
        android:textAlignment="center"
        android:textSize="16sp"
        android:gravity="center"
        android:visibility="gone" />
</LinearLayout>
```

- [ ] **Step 3: Create Activity**

Create `android/app/src/main/java/com/example/stockapp/StockSearchActivity.kt`:

```kotlin
package com.example.stockapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.stockapp.databinding.ActivityStockSearchBinding
import com.example.stockapp.logging.AppLogger
import com.example.stockapp.models.StockItem
import com.example.stockapp.viewmodels.StockSearchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class StockSearchActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStockSearchBinding
    private lateinit var viewModel: StockSearchViewModel
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityStockSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider.AndroidViewModelFactory(application)
            .create(StockSearchViewModel::class.java)
        
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
            } else {
                binding.statusMessageTextView.visibility = View.GONE
            }
        }
        
        // Setup button listeners
        binding.searchButton.setOnClickListener {
            performSearch()
        }
        
        binding.clearButton.setOnClickListener {
            clearSearch()
        }
        
        AppLogger.log("[${getCurrentTimestamp()}] STOCK_SEARCH_ACTIVITY_CREATED")
    }
    
    private fun performSearch() {
        val artCode = binding.articleCodeInput.text.toString().trim()
        val stkLieu = binding.locationInput.text.toString().trim()
        val stkNosu = binding.storageNumberInput.text.toString().trim()
        
        AppLogger.log(
            "[${getCurrentTimestamp()}] STOCK_SEARCH_START " +
            "art_code=$artCode stk_lieu=$stkLieu stk_nosu=$stkNosu"
        )
        
        viewModel.searchStock(artCode.takeIf { it.isNotEmpty() },
                              stkLieu.takeIf { it.isNotEmpty() },
                              stkNosu.takeIf { it.isNotEmpty() })
    }
    
    private fun clearSearch() {
        binding.articleCodeInput.text.clear()
        binding.locationInput.text.clear()
        binding.storageNumberInput.text.clear()
        viewModel.clearSearch()
        binding.emptyStateTextView.visibility = View.GONE
        binding.resultsListView.visibility = View.GONE
    }
    
    private fun displayResults(items: List<StockItem>) {
        if (items.isEmpty()) {
            binding.emptyStateTextView.visibility = View.VISIBLE
            binding.resultsListView.visibility = View.GONE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
            binding.resultsListView.visibility = View.VISIBLE
            
            val displayList = items.map {
                "${it.artCode} | ${it.stkLieu} | ${it.stkNosu} | ${it.quaCode} | Qty:${it.stkQte}"
            }
            
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                displayList
            )
            binding.resultsListView.adapter = adapter
        }
    }
    
    private fun updateLoadingState(isLoading: Boolean) {
        binding.searchButton.isEnabled = !isLoading
        binding.clearButton.isEnabled = !isLoading
        binding.articleCodeInput.isEnabled = !isLoading
        binding.locationInput.isEnabled = !isLoading
        binding.storageNumberInput.isEnabled = !isLoading
        
        if (isLoading) {
            binding.loadingProgressBar.visibility = View.VISIBLE
            binding.statusMessageTextView.text = getString(R.string.stock_search_loading)
            binding.statusMessageTextView.setTextColor(getColor(android.R.color.holo_blue_light))
            binding.statusMessageTextView.visibility = View.VISIBLE
        } else {
            binding.loadingProgressBar.visibility = View.GONE
        }
    }
    
    private fun showError(message: String) {
        binding.statusMessageTextView.text = message
        binding.statusMessageTextView.setTextColor(getColor(android.R.color.holo_red_dark))
        binding.statusMessageTextView.visibility = View.VISIBLE
    }
    
    private fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }
}
```

- [ ] **Step 4: Register Activity in Manifest**

Modify `android/app/src/main/AndroidManifest.xml` to add:

```xml
<activity
    android:name=".StockSearchActivity"
    android:exported="false" />
```

- [ ] **Step 5: Verify compilation and build APK**

```bash
cd D:\Projects\POC_SPEED_RF_INFO_STOCK\android
./gradlew build
```

Expected: Build succeeds

- [ ] **Step 6: Test manually**

- Launch app on device
- Login successfully
- Select activity
- Navigate to Stock Search (via menu or button in MainActivity)
- Enter search criteria (at least one field)
- Click Search
- Verify results display correctly or error message shows

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/java/com/example/stockapp/StockSearchActivity.kt \
        android/app/src/main/res/layout/activity_stock_search.xml \
        android/app/src/main/res/values/strings.xml \
        android/app/src/main/AndroidManifest.xml
git commit -m "feat: add stock search activity with layout and string resources"
```

---

## Verification Checklist

### Backend (B1 + B2)
- [ ] Stock models validate input (art_code, stk_lieu, stk_nosu with at least one required)
- [ ] Repository builds correct SQL query with OR logic for criteria
- [ ] Service returns StockSearchResponse with 0-N items
- [ ] API endpoint `/api/stock/search` requires Bearer token
- [ ] Endpoint filters by act_code (activity code)
- [ ] All search attempts logged with details
- [ ] Endpoint returns proper JSON response
- [ ] Tests pass for models, repository, service

### Android (A1 + A4)
- [ ] StockSearchActivity launches with 3 input fields
- [ ] Search button triggers viewModel.searchStock()
- [ ] Results display in ListView with all 5 fields (art_code, stk_lieu, stk_nosu, qua_code, stk_qte)
- [ ] Empty state shows "No results found" when no items
- [ ] Loading spinner shows during search
- [ ] Error messages display properly
- [ ] Clear button resets form and clears results
- [ ] Activity code from ActivityManager automatically included in request
- [ ] Logs show search attempt, response, and results count

### Integration (Backend + Android)
- [ ] Login → Activity Selection → Stock Search flow works end-to-end
- [ ] Search with all 3 criteria types works
- [ ] Search with multiple criteria returns OR results
- [ ] 401 response handled (user redirected to login)
- [ ] Network errors handled gracefully
- [ ] Results sorted by article code, location

---

## Test Plan

### Backend Manual Test
```bash
# Start backend
python backend/main.py

# In another terminal, test search
curl -X POST http://localhost:8000/api/stock/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{"art_code": "ARTICLE", "stk_lieu": null, "stk_nosu": null, "act_code": "BKS"}'
```

### Android Manual Test
1. Install and launch app
2. Login (Activity Selection should work from Phase 7 Part 1)
3. Navigate to Stock Search
4. Enter article code (e.g., "ARTICLE")
5. Click Search
6. Verify results display
7. Clear and try with location
8. Verify OR logic works
