package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.stockapp.logging.AppLogger
import java.io.File

class LogsViewerActivity : AppCompatActivity() {

    private lateinit var logsTextView: TextView
    private lateinit var clearButton: Button
    private lateinit var exportButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs_viewer)

        // Ensure AppLogger's application context is initialized so
        // AppLogger.getLogFile() / AppLogger.log() can resolve the log path
        // even if no other component has instantiated AppLogger yet.
        AppLogger(this)

        logsTextView = findViewById(R.id.logsTextView)
        clearButton = findViewById(R.id.clearLogsButton)
        exportButton = findViewById(R.id.exportLogsButton)

        clearButton.setOnClickListener { clearLogs() }
        exportButton.setOnClickListener { exportLogs() }

        loadLogs()
    }

    private fun loadLogs() {
        try {
            val logFile: File = AppLogger.getLogFile()
            if (logFile.exists() && logFile.length() > 0) {
                val logsContent = logFile.readText()
                logsTextView.text = logsContent
            } else {
                logsTextView.text = getString(R.string.no_logs_available)
            }
        } catch (e: Exception) {
            logsTextView.text = "Error loading logs: ${e.message}"
        }
    }

    private fun clearLogs() {
        try {
            val logFile: File = AppLogger.getLogFile()
            if (logFile.exists()) {
                logFile.delete()
                logsTextView.text = getString(R.string.no_logs_available)
                AppLogger.log("Logs cleared by user")
            }
        } catch (e: Exception) {
            logsTextView.text = "Error clearing logs: ${e.message}"
        }
    }

    private fun exportLogs() {
        try {
            val logFile: File = AppLogger.getLogFile()
            if (logFile.exists() && logFile.length() > 0) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Stock App Debug Logs")
                    putExtra(Intent.EXTRA_TEXT, logFile.readText())
                }
                startActivity(Intent.createChooser(intent, getString(R.string.export_logs)))
            } else {
                logsTextView.text = getString(R.string.no_logs_available)
            }
        } catch (e: Exception) {
            logsTextView.text = "Error exporting logs: ${e.message}"
        }
    }
}
