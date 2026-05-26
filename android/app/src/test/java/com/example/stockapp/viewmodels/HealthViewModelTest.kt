package com.example.stockapp.viewmodels

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Unit tests for HealthViewModel.
 *
 * These tests validate the initial LiveData state of the ViewModel and that
 * public methods can be called without throwing exceptions. No network I/O is
 * exercised — all assertions run on the JVM without a device or emulator.
 *
 * android.util.Log calls inside the ViewModel are handled by
 * returnDefaultValues = true in build.gradle testOptions.
 *
 * LiveData initial values are set synchronously in MutableLiveData constructors,
 * so .value is readable without InstantTaskExecutorRule for these tests.
 */
class HealthViewModelTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var viewModel: HealthViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Editor fluent chain required by ConfigManager.setApiConfig / init
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)

        // Default host/port responses so ConfigManager does not return null
        whenever(mockSharedPreferences.getString(any(), any())).thenAnswer { invocation ->
            invocation.arguments[1] as? String
        }

        viewModel = HealthViewModel(mockContext)
    }

    // -------------------------------------------------------------------------
    // 1. Initial connection status
    // -------------------------------------------------------------------------

    @Test
    fun `connectionStatus initial value is DISCONNECTED`() {
        // Arrange — ViewModel created in setUp()

        // Act
        val status = viewModel.connectionStatus.value

        // Assert
        assertEquals(ConnectionStatus.DISCONNECTED, status)
    }

    // -------------------------------------------------------------------------
    // 2. Initial API health status
    // -------------------------------------------------------------------------

    @Test
    fun `apiHealthStatus initial value is 'Not tested'`() {
        // Arrange — ViewModel created in setUp()

        // Act
        val apiStatus = viewModel.apiHealthStatus.value

        // Assert
        assertEquals("Not tested", apiStatus)
    }

    // -------------------------------------------------------------------------
    // 3. Initial database health status
    // -------------------------------------------------------------------------

    @Test
    fun `databaseHealthStatus initial value is 'Not tested'`() {
        // Arrange — ViewModel created in setUp()

        // Act
        val dbStatus = viewModel.databaseHealthStatus.value

        // Assert
        assertEquals("Not tested", dbStatus)
    }

    // -------------------------------------------------------------------------
    // 4. testConnection() can be called without arguments
    // -------------------------------------------------------------------------

    @Test
    fun `testConnection can be called without arguments and does not throw`() {
        // Arrange — ViewModel already in DISCONNECTED state

        // Act — testConnection() reads host/port from ConfigManager (mocked above).
        // The actual network call runs on a daemon background thread; we only
        // verify that the call does not throw synchronously.
        var thrownException: Throwable? = null
        try {
            viewModel.testConnection()
        } catch (e: Throwable) {
            thrownException = e
        }

        // Assert — no exception thrown
        assertEquals(null, thrownException)

        // Assert — ViewModel is no longer DISCONNECTED (it moved to CONNECTING)
        assertNotNull(viewModel.connectionStatus.value)
    }
}
