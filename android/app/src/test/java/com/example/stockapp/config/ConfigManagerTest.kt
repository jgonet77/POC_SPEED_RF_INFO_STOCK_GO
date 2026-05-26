package com.example.stockapp.config

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ConfigManager.
 *
 * Covers:
 *  - Default host/port returned when SharedPreferences has no stored value
 *  - isValidPort() boundary validation
 *  - resetToDefaults() delegates to SharedPreferences.Editor correctly
 */
class ConfigManagerTest {

    // --- mocks ---
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    // --- subject under test ---
    private lateinit var configManager: ConfigManager

    @Before
    fun setUp() {
        mockEditor = mock()
        mockPrefs = mock()
        mockContext = mock()

        // Editor fluent chain: putString returns the editor itself, apply is void
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)

        // getSharedPreferences always returns our mock prefs
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)

        // prefs.edit() returns the mock editor
        whenever(mockPrefs.edit()).thenReturn(mockEditor)

        configManager = ConfigManager(mockContext)
    }

    // -------------------------------------------------------------------------
    // 1. Default host
    // -------------------------------------------------------------------------

    @Test
    fun `getApiHost returns DEFAULT_HOST when not configured`() {
        // Arrange
        whenever(mockPrefs.getString(eq("api_host"), eq(ConfigManager.DEFAULT_HOST)))
            .thenReturn(ConfigManager.DEFAULT_HOST)

        // Act
        val host = configManager.getApiHost()

        // Assert
        assertEquals(ConfigManager.DEFAULT_HOST, host)
    }

    // -------------------------------------------------------------------------
    // 2. Default port
    // -------------------------------------------------------------------------

    @Test
    fun `getApiPort returns DEFAULT_PORT when not configured`() {
        // Arrange
        whenever(mockPrefs.getString(eq("api_port"), eq(ConfigManager.DEFAULT_PORT)))
            .thenReturn(ConfigManager.DEFAULT_PORT)

        // Act
        val port = configManager.getApiPort()

        // Assert
        assertEquals(ConfigManager.DEFAULT_PORT, port)
    }

    // -------------------------------------------------------------------------
    // 3. Valid port numbers
    // -------------------------------------------------------------------------

    @Test
    fun `isValidPort returns true for valid port numbers`() {
        assertTrue("Port 1 should be valid",     ConfigManager.isValidPort("1"))
        assertTrue("Port 80 should be valid",    ConfigManager.isValidPort("80"))
        assertTrue("Port 8000 should be valid",  ConfigManager.isValidPort("8000"))
        assertTrue("Port 65535 should be valid", ConfigManager.isValidPort("65535"))
    }

    // -------------------------------------------------------------------------
    // 4. Invalid port numbers
    // -------------------------------------------------------------------------

    @Test
    fun `isValidPort returns false for invalid port values`() {
        assertFalse("Port 0 should be invalid",     ConfigManager.isValidPort("0"))
        assertFalse("Port -1 should be invalid",    ConfigManager.isValidPort("-1"))
        assertFalse("Port 65536 should be invalid", ConfigManager.isValidPort("65536"))
        assertFalse("Port 99999 should be invalid", ConfigManager.isValidPort("99999"))
        assertFalse("'abc' should be invalid",      ConfigManager.isValidPort("abc"))
        assertFalse("Empty string should be invalid", ConfigManager.isValidPort(""))
    }

    // -------------------------------------------------------------------------
    // 5. resetToDefaults delegates to SharedPreferences.Editor
    // -------------------------------------------------------------------------

    @Test
    fun `resetToDefaults writes DEFAULT_HOST and DEFAULT_PORT then calls apply`() {
        // Act
        configManager.resetToDefaults()

        // Assert — editor must receive both putString calls and apply()
        verify(mockEditor).putString("api_host", ConfigManager.DEFAULT_HOST)
        verify(mockEditor).putString("api_port", ConfigManager.DEFAULT_PORT)
        verify(mockEditor).apply()
    }
}
