package com.example.data

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object CrashDiagnosticLogger {

    private const val LOG_FILE_NAME = "typegenius_diagnostics.log"
    private const val MAX_ENTRIES = 50
    private val lock = Any()
    private val handlerInstalled = AtomicBoolean(false)

    fun installUncaughtHandler(context: Context) {
        if (!handlerInstalled.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                logError(
                    context = appContext,
                    tag = "CRASH/${thread.name}",
                    message = "Uncaught exception in ${appContext.packageName} (SDK ${Build.VERSION.SDK_INT})",
                    throwable = throwable
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun logInfo(context: Context, tag: String, message: String) {
        appendEntry(context, "INFO", tag, message, null)
    }

    fun logError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        appendEntry(context, "ERROR", tag, message, throwable)
    }

    fun readLogs(context: Context): List<String> {
        synchronized(lock) {
            val file = File(context.applicationContext.filesDir, LOG_FILE_NAME)
            if (!file.exists()) return emptyList()
            return runCatching {
                file.readLines()
                    .filter { it.isNotBlank() }
                    .takeLast(MAX_ENTRIES)
            }.getOrDefault(emptyList())
        }
    }

    fun readLogsText(context: Context): String {
        val entries = readLogs(context)
        return if (entries.isEmpty()) {
            "কোনো এরর বা ক্র্যাশ লগ নেই — TypeGenius সুস্থভাবে চলছে ✓"
        } else {
            entries.asReversed().joinToString("\n")
        }
    }

    fun clearLogs(context: Context) {
        synchronized(lock) {
            runCatching {
                val file = File(context.applicationContext.filesDir, LOG_FILE_NAME)
                if (file.exists()) file.delete()
            }
        }
    }

    private fun appendEntry(
        context: Context,
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        synchronized(lock) {
            runCatching {
                val file = File(context.applicationContext.filesDir, LOG_FILE_NAME)
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val causeSummary = throwable?.let { t ->
                    val topFrame = t.stackTrace.firstOrNull()?.let { f ->
                        "${f.className.substringAfterLast('.')}:${f.lineNumber}"
                    } ?: "unknown"
                    " | ${t::class.java.simpleName}: ${t.message.orEmpty().take(120)} @ $topFrame"
                }.orEmpty()
                val singleLine = "[$ts] [$level/$tag] ${message.replace('\n', ' ').take(220)}$causeSummary"
                val existing = if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()
                val trimmed = (existing + singleLine).takeLast(MAX_ENTRIES)
                file.writeText(trimmed.joinToString("\n") + "\n")
            }
        }
    }
}
