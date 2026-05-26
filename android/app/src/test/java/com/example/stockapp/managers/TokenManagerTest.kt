package com.example.stockapp.managers

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for TokenManager.
 *
 * Covers:
 *  - saveToken() stores token and expiry correctly
 *  - getToken() returns token if not expired
 *  - getToken() returns null if expired
 *  - clearToken() removes token from SharedPreferences
 */
class TokenManagerTest {

    // --- mocks ---
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockEditor = mock()
        mockPrefs = mock()
        mockContext = mock()

        // Editor fluent chain: putString/putLong return the editor itself, apply is void
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.putLong(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        // getSharedPreferences always returns our mock prefs
        whenever(mockContext.getSharedPreferences(eq("auth"), any())).thenReturn(mockPrefs)

        // prefs.edit() returns the mock editor
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
    }

    // -------------------------------------------------------------------------
    // 1. Save token successfully
    // -------------------------------------------------------------------------

    @Test
    fun `saveToken stores token and expiry in SharedPreferences`() {
        // Arrange
        val token = "test_jwt_token_123"
        val expiresInSeconds = 86400 // 24 hours
        val currentTime = System.currentTimeMillis()

        // Act
        TokenManager.saveToken(mockContext, token, expiresInSeconds)

        // Assert
        verify(mockEditor).putString("auth_token", token)
        verify(mockEditor).putLong(eq("auth_token_expiry"), any())
        verify(mockEditor).apply()
    }

    // -------------------------------------------------------------------------
    // 2. Get token when valid (not expired)
    // -------------------------------------------------------------------------

    @Test
    fun `getToken returns token when it exists and is not expired`() {
        // Arrange
        val token = "valid_token"
        val futureTime = System.currentTimeMillis() + 3600000 // 1 hour in future

        whenever(mockPrefs.getString(eq("auth_token"), any())).thenReturn(token)
        whenever(mockPrefs.getLong(eq("auth_token_expiry"), eq(0L))).thenReturn(futureTime)

        // Act
        val result = TokenManager.getToken(mockContext)

        // Assert
        assert(result == token)
    }

    // -------------------------------------------------------------------------
    // 3. Get token returns null when expired
    // -------------------------------------------------------------------------

    @Test
    fun `getToken returns null and clears token when it is expired`() {
        // Arrange
        val token = "expired_token"
        val pastTime = System.currentTimeMillis() - 3600000 // 1 hour in past

        whenever(mockPrefs.getString(eq("auth_token"), any())).thenReturn(token)
        whenever(mockPrefs.getLong(eq("auth_token_expiry"), eq(0L))).thenReturn(pastTime)

        // Act
        val result = TokenManager.getToken(mockContext)

        // Assert
        assertNull(result)
        verify(mockEditor).remove("auth_token")
        verify(mockEditor).apply()
    }

    // -------------------------------------------------------------------------
    // 4. Get token returns null when no token stored
    // -------------------------------------------------------------------------

    @Test
    fun `getToken returns null when no token is stored`() {
        // Arrange
        whenever(mockPrefs.getString(eq("auth_token"), any())).thenReturn(null)
        whenever(mockPrefs.getLong(eq("auth_token_expiry"), eq(0L))).thenReturn(0L)

        // Act
        val result = TokenManager.getToken(mockContext)

        // Assert
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // 5. Clear token removes from SharedPreferences
    // -------------------------------------------------------------------------

    @Test
    fun `clearToken removes token from SharedPreferences`() {
        // Act
        TokenManager.clearToken(mockContext)

        // Assert
        verify(mockEditor).remove("auth_token")
        verify(mockEditor).apply()
    }
}
