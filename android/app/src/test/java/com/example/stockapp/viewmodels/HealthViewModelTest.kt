package com.example.stockapp.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.stockapp.repositories.HealthRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Unit tests for HealthViewModel.
 *
 * InstantTaskExecutorRule replaces the Architecture Components background executor
 * with a synchronous one, so LiveData.postValue() updates are immediately visible
 * in assertions without Thread.sleep() or await helpers.
 *
 * HealthRepository is injected as a mock to prevent any real network I/O.
 * android.util.Log calls are handled by returnDefaultValues = true in build.gradle.
 */
class HealthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockRepository: HealthRepository

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

        viewModel = HealthViewModel(mockContext, mockRepository)
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
    // 4. testConnection() transitions status to CONNECTING
    // -------------------------------------------------------------------------

    @Test
    fun `testConnection transitions connectionStatus to CONNECTING`() {
        // Act
        viewModel.testConnection()

        // Assert — postValue is synchronous thanks to InstantTaskExecutorRule
        assertEquals(ConnectionStatus.CONNECTING, viewModel.connectionStatus.value)
    }
}
