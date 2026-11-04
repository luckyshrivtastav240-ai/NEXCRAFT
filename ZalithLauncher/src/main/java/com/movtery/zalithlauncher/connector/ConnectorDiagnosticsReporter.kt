package com.movtery.zalithlauncher.connector

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.movtery.zalithlauncher.path.PathManager
import org.json.JSONObject

/** Uploads bounded, redacted launcher diagnostics only after the device has been paired. */
class ConnectorDiagnosticsReporter(private val context: Context) {
    private val relay = ConnectorRelayClient(context.applicationContext)

    suspend fun uploadSnapshot() {
        if (ConnectorTokenStore(context).read() == null) return
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")
        relay.uploadDiagnostic(
            "status",
            JSONObject()
                .put("appVersion", version)
                .put("package", context.packageName)
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL)
                .put("androidRelease", Build.VERSION.RELEASE)
                .put("sdk", Build.VERSION.SDK_INT)
                .put("availableRamMb", memory.availMem / (1024 * 1024))
                .put("lowMemory", memory.lowMemory)
                .put("capturedAt", System.currentTimeMillis())
        )

        val latestLog = PathManager.DIR_LAUNCHER_LOGS.listFiles()
            ?.filter { it.isFile && it.extension.equals("log", true) }
            ?.maxByOrNull { it.lastModified() }
        latestLog?.let { file ->
            val excerpt = redact(runCatching { file.readText().takeLast(20_000) }.getOrDefault(""))
            if (excerpt.isNotBlank()) relay.uploadDiagnostic(
                "log",
                JSONObject().put("file", file.name).put("excerpt", excerpt).put("capturedAt", System.currentTimeMillis())
            )
        }
        val crash = PathManager.FILE_CRASH_REPORT.takeIf { it.isFile }
            ?.let { redact(runCatching { it.readText().takeLast(20_000) }.getOrDefault("")) }
        if (!crash.isNullOrBlank()) relay.uploadDiagnostic(
            "crash",
            JSONObject().put("excerpt", crash).put("capturedAt", System.currentTimeMillis())
        )
    }

    private fun redact(input: String): String {
        val bearer = Regex("(?i)(authorization\\\\s*[:=]\\\\s*bearer\\\\s+)[A-Za-z0-9._~+/-]+=*")
        val secrets = Regex(
            "(?i)((?:access[_-]?token|refresh[_-]?token|id[_-]?token|token|password|passwd|secret|api[_-]?key|client[_-]?secret|cookie|session|authorization|pairing[_-]?code)\\\\s*[:=]\\\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\\\s,;&]+)"
        )
        val email = Regex("(?i)\\\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\\\.[A-Z]{2,}\\\\b")
        val redacted = secrets.replace(bearer.replace(input, "$1[REDACTED]"), "$1[REDACTED]")
        return email.replace(redacted, "[EMAIL]")
    }
}
