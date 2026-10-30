package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiagnosticLog(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

object CrashDiagnostics {
    private val _logs = MutableStateFlow<List<DiagnosticLog>>(emptyList())
    val logs: StateFlow<List<DiagnosticLog>> = _logs.asStateFlow()

    private val _lastFatalError = MutableStateFlow<Throwable?>(null)
    val lastFatalError: StateFlow<Throwable?> = _lastFatalError.asStateFlow()

    fun recordLog(tag: String, message: String, throwable: Throwable? = null) {
        val entry = DiagnosticLog(tag = tag, message = message, throwable = throwable)
        _logs.value = (_logs.value + entry).takeLast(100)
    }

    fun recordFatalError(t: Throwable) {
        _lastFatalError.value = t
        recordLog("FATAL", t.message ?: "Unknown fatal error", t)
    }

    fun clearFatalError() {
        _lastFatalError.value = null
    }

    fun getDiagnosticReport(): String {
        val sb = StringBuilder()
        sb.append("=== Household CFO Diagnostic Report ===\n")
        sb.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})\n")
        sb.append("Timestamp: ${System.currentTimeMillis()}\n\n")

        val fatal = _lastFatalError.value
        if (fatal != null) {
            sb.append("--- LAST FATAL ERROR ---\n")
            sb.append("${fatal.javaClass.simpleName}: ${fatal.message}\n")
            sb.append(fatal.stackTraceToString().lines().take(20).joinToString("\n"))
            sb.append("\n\n")
        }

        sb.append("--- RECENT DIAGNOSTIC LOGS (${_logs.value.size}) ---\n")
        _logs.value.takeLast(25).forEach { log ->
            sb.append("[${log.tag}] ${log.message}\n")
            log.throwable?.let {
                sb.append("   Caused by: ${it.javaClass.simpleName}: ${it.message}\n")
            }
        }
        return sb.toString()
    }
}

@Composable
fun DebugCrashReporterBanner(
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.DEBUG) return

    val context = LocalContext.current
    val fatalError by CrashDiagnostics.lastFatalError.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (fatalError != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Diagnostic Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Debug Crash Detected",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp
                    )
                    Text(
                        text = fatalError?.message ?: "Uncaught exception",
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("View", fontSize = 12.sp)
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diagnostic Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val report = remember { CrashDiagnostics.getDiagnosticReport() }
                    val vScroll = rememberScrollState()
                    val hScroll = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                    ) {
                        Text(
                            text = report,
                            color = Color(0xFF4AF626),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Household CFO Diagnostic Report", report)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied diagnostic report", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                CrashDiagnostics.clearFatalError()
                                showDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dismiss", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
