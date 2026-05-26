package com.example.stockapp.logging

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Unit tests for AppLogger.
 *
 * TemporaryFolder provides real on-disk temp files so the actual File I/O
 * code paths inside AppLogger are exercised. Context is mocked so
 * getExternalFilesDir(null) resolves to the temp folder rather than Android's
 * external storage.
 *
 * android.util.Log calls are handled by returnDefaultValues = true in
 * build.gradle testOptions — they return 0 / null / false instead of throwing
 * "Method not mocked" RuntimeExceptions.
 */
class AppLoggerTest {

    // JUnit 4 rule that creates a fresh temporary directory for every test
    // and deletes it (including all contents) afterwards.
    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var logger: AppLogger

    @Before
    fun setUp() {
        // Reset companion state first (before any AppLogger construction) so
        // each test starts with a clean slate and never references a previous
        // test's (now-deleted) TemporaryFolder.
        val field = AppLogger::class.java.getDeclaredField("appContext")
        field.isAccessible = true
        field.set(null, null)

        mockContext = mock()

        // getExternalFilesDir(null) must return a File so AppLogger can build
        // the "stock_app_logs" sub-directory beneath it.
        whenever(mockContext.getExternalFilesDir(null)).thenReturn(tempFolder.root)

        // applicationContext must not return null — the companion object stores
        // it on first construction and throws if it is null later.
        whenever(mockContext.applicationContext).thenReturn(mockContext)

        // Construct a fresh AppLogger for every test.
        logger = AppLogger(mockContext)
    }

    // -------------------------------------------------------------------------
    // 1. getLogs returns the last N lines from a large file
    // -------------------------------------------------------------------------

    @Test
    fun `getLogs returns last N lines and excludes earlier lines`() {
        // Arrange — write 600 numbered lines directly into the log file that
        // AppLogger will read.  We bypass the logger API intentionally so we
        // can write a deterministic, large file quickly.
        val logDir = File(tempFolder.root, "stock_app_logs")
        val logFile = File(logDir, "debug.log")
        logDir.mkdirs()

        val content = buildString {
            for (i in 1..600) {
                appendLine("Line $i")
            }
        }
        logFile.writeText(content)

        // Act
        val result = logger.getLogs(maxLines = 100)

        // Assert — the result must be at most 100 lines
        val lines = result.lines().filter { it.isNotEmpty() }
        assertTrue(
            "Expected at most 100 lines but got ${lines.size}",
            lines.size <= 100
        )

        // The very last written line must appear in the result
        assertTrue(
            "Result should contain 'Line 600' (last line)",
            result.contains("Line 600")
        )

        // The very first written line must NOT appear (it is outside the last 100)
        assertFalse(
            "Result should NOT contain 'Line 1' (before last 100 lines)",
            result.contains("Line 1\n") || result.endsWith("Line 1") || result.startsWith("Line 1\n")
                    // broader guard: lines list should not start with "Line 1"
                    || lines.first() == "Line 1"
        )
    }

    // -------------------------------------------------------------------------
    // 2. getLogs returns "No logs yet" when the file does not exist
    // -------------------------------------------------------------------------

    @Test
    fun `getLogs returns 'No logs yet' when log file is absent`() {
        // Arrange — ensure the log file does not exist.  If AppLogger's init
        // created the directory already, remove any file it may have written.
        val logDir = File(tempFolder.root, "stock_app_logs")
        val logFile = File(logDir, "debug.log")
        logFile.delete()

        // Act
        val result = logger.getLogs()

        // Assert
        assertTrue(
            "Expected 'No logs yet' but got: $result",
            result == "No logs yet"
        )
    }

    // -------------------------------------------------------------------------
    // 3. logApiResponse truncates bodies longer than 200 characters
    // -------------------------------------------------------------------------

    @Test
    fun `logApiResponse truncates response body longer than 200 chars`() {
        // Arrange — build a body that is clearly over the 200-char limit.
        val longResponse = "X".repeat(300)

        // Act
        logger.logApiResponse("/api/test", 200, longResponse)

        val result = logger.getLogs()

        // Assert — truncation marker must be present
        assertTrue(
            "Expected '...' truncation marker in logs",
            result.contains("...")
        )

        // Assert — the full 300-char string must NOT appear verbatim
        assertFalse(
            "Full 300-char response body should not appear in logs (should be truncated)",
            result.contains(longResponse)
        )
    }

    // -------------------------------------------------------------------------
    // 4. info() writes a properly formatted log line
    // -------------------------------------------------------------------------

    @Test
    fun `info writes line containing level tag, message and timestamp`() {
        // Act
        logger.info("Test message")

        val result = logger.getLogs()

        // Assert — level tag
        assertTrue(
            "Log line should contain '[INFO]'",
            result.contains("[INFO]")
        )

        // Assert — message text
        assertTrue(
            "Log line should contain 'Test message'",
            result.contains("Test message")
        )

        // Assert — timestamp in the format [YYYY-MM-DD HH:mm:ss.SSS]
        // Example: [2026-05-22 14:30:00.123]
        val timestampPattern = Regex("""\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}]""")
        assertTrue(
            "Log line should contain a timestamp matching [YYYY-MM-DD HH:mm:ss.SSS]",
            timestampPattern.containsMatchIn(result)
        )
    }
}
