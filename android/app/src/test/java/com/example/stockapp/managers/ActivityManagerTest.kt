package com.example.stockapp.managers

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ActivityManager.
 *
 * Covers:
 *  - saveActivity() stores activity correctly
 *  - getActivityCode() returns stored code
 *  - getActivityKeyu() returns stored keyu
 *  - getActivityLib() returns stored lib
 *  - clearActivity() removes activity from SharedPreferences
 *  - hasActivity() returns true/false correctly
 */
class ActivityManagerTest {

    // --- mocks ---
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockEditor = mock()
        mockPrefs = mock()
        mockContext = mock()

        // Editor fluent chain: putInt/putString return the editor itself, apply is void
        whenever(mockEditor.putInt(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        // getSharedPreferences always returns our mock prefs
        whenever(mockContext.getSharedPreferences(eq("activity"), any())).thenReturn(mockPrefs)

        // prefs.edit() returns the mock editor
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
    }

    // -------------------------------------------------------------------------
    // 1. Save activity successfully
    // -------------------------------------------------------------------------

    @Test
    fun `saveActivity stores activity code, keyu, and lib in SharedPreferences`() {
        // Arrange
        val actKeyu = 123
        val actCode = "ACT001"
        val actLib = "Activity 1"

        // Act
        ActivityManager.saveActivity(mockContext, actKeyu, actCode, actLib)

        // Assert
        verify(mockEditor).putInt("selected_act_keyu", actKeyu)
        verify(mockEditor).putString("selected_act_code", actCode)
        verify(mockEditor).putString("selected_act_lib", actLib)
        verify(mockEditor).apply()
    }

    // -------------------------------------------------------------------------
    // 2. Get activity code when stored
    // -------------------------------------------------------------------------

    @Test
    fun `getActivityCode returns code when it is stored`() {
        // Arrange
        val actCode = "ACT001"

        whenever(mockPrefs.getString(eq("selected_act_code"), any())).thenReturn(actCode)

        // Act
        val result = ActivityManager.getActivityCode(mockContext)

        // Assert
        assertEquals(actCode, result)
    }

    // -------------------------------------------------------------------------
    // 3. Get activity code returns null when not stored
    // -------------------------------------------------------------------------

    @Test
    fun `getActivityCode returns null when code is not stored`() {
        // Arrange
        whenever(mockPrefs.getString(eq("selected_act_code"), any())).thenReturn(null)

        // Act
        val result = ActivityManager.getActivityCode(mockContext)

        // Assert
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // 4. Get activity keyu when stored
    // -------------------------------------------------------------------------

    @Test
    fun `getActivityKeyu returns keyu when it is stored`() {
        // Arrange
        val actKeyu = 123

        whenever(mockPrefs.getInt(eq("selected_act_keyu"), any())).thenReturn(actKeyu)

        // Act
        val result = ActivityManager.getActivityKeyu(mockContext)

        // Assert
        assertEquals(actKeyu, result)
    }

    // -------------------------------------------------------------------------
    // 5. Get activity keyu returns -1 when not stored
    // -------------------------------------------------------------------------

    @Test
    fun `getActivityKeyu returns -1 when keyu is not stored`() {
        // Arrange
        whenever(mockPrefs.getInt(eq("selected_act_keyu"), any())).thenReturn(-1)

        // Act
        val result = ActivityManager.getActivityKeyu(mockContext)

        // Assert
        assertEquals(-1, result)
    }

    // -------------------------------------------------------------------------
    // 6. Get activity lib when stored
    // -------------------------------------------------------------------------

    @Test
    fun `getActivityLib returns lib when it is stored`() {
        // Arrange
        val actLib = "Activity 1"

        whenever(mockPrefs.getString(eq("selected_act_lib"), any())).thenReturn(actLib)

        // Act
        val result = ActivityManager.getActivityLib(mockContext)

        // Assert
        assertEquals(actLib, result)
    }

    // -------------------------------------------------------------------------
    // 7. Get activity lib returns null when not stored
    // -------------------------------------------------------------------------

    @Test
    fun `getActivityLib returns null when lib is not stored`() {
        // Arrange
        whenever(mockPrefs.getString(eq("selected_act_lib"), any())).thenReturn(null)

        // Act
        val result = ActivityManager.getActivityLib(mockContext)

        // Assert
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // 8. Clear activity removes from SharedPreferences
    // -------------------------------------------------------------------------

    @Test
    fun `clearActivity removes code, keyu, and lib from SharedPreferences`() {
        // Act
        ActivityManager.clearActivity(mockContext)

        // Assert
        verify(mockEditor).remove("selected_act_code")
        verify(mockEditor).remove("selected_act_keyu")
        verify(mockEditor).remove("selected_act_lib")
        verify(mockEditor).apply()
    }

    // -------------------------------------------------------------------------
    // 9. Has activity returns true when activity is stored
    // -------------------------------------------------------------------------

    @Test
    fun `hasActivity returns true when both code and keyu are stored`() {
        // Arrange
        whenever(mockPrefs.contains("selected_act_code")).thenReturn(true)
        whenever(mockPrefs.contains("selected_act_keyu")).thenReturn(true)

        // Act
        val result = ActivityManager.hasActivity(mockContext)

        // Assert
        assertTrue(result)
    }

    // -------------------------------------------------------------------------
    // 10. Has activity returns false when code is missing
    // -------------------------------------------------------------------------

    @Test
    fun `hasActivity returns false when code is missing`() {
        // Arrange
        whenever(mockPrefs.contains("selected_act_code")).thenReturn(false)
        whenever(mockPrefs.contains("selected_act_keyu")).thenReturn(true)

        // Act
        val result = ActivityManager.hasActivity(mockContext)

        // Assert
        assertFalse(result)
    }

    // -------------------------------------------------------------------------
    // 11. Has activity returns false when keyu is missing
    // -------------------------------------------------------------------------

    @Test
    fun `hasActivity returns false when keyu is missing`() {
        // Arrange
        whenever(mockPrefs.contains("selected_act_code")).thenReturn(true)
        whenever(mockPrefs.contains("selected_act_keyu")).thenReturn(false)

        // Act
        val result = ActivityManager.hasActivity(mockContext)

        // Assert
        assertFalse(result)
    }

    // -------------------------------------------------------------------------
    // 12. Has activity returns false when both are missing
    // -------------------------------------------------------------------------

    @Test
    fun `hasActivity returns false when neither code nor keyu are stored`() {
        // Arrange
        whenever(mockPrefs.contains("selected_act_code")).thenReturn(false)
        whenever(mockPrefs.contains("selected_act_keyu")).thenReturn(false)

        // Act
        val result = ActivityManager.hasActivity(mockContext)

        // Assert
        assertFalse(result)
    }
}
