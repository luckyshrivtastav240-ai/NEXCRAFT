package com.movtery.zalithlauncher.connector

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ConnectorPairing(
    val pairingId: String,
    val pairingCode: String,
    val deviceSecret: String,
    val expiresAt: String
)

data class ConnectorPairingStatus(val status: String, val deviceToken: String? = null)

/** HTTPS-only API client. It never embeds ChatGPT cookies, account tokens, or API keys. */
class ConnectorRelayClient(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("nexcraft_connector", Context.MODE_PRIVATE)
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun setRelayUrl(value: String) {
        val normalized = normalizeRelayUrl(value)
        check(prefs.edit().putString("relay_url", normalized).commit())
    }

    fun relayUrl(): String? = prefs.getString("relay_url", null)?.let {
        runCatching { normalizeRelayUrl(it) }.getOrNull()
    }

    suspend fun createPairing(deviceLabel: String): ConnectorPairing = withContext(Dispatchers.IO) {
        val base = relayUrl() ?: throw IllegalStateException("Set the HTTPS relay address first")
        val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
        val body = JSONObject().put("deviceId", deviceId).put("deviceLabel", deviceLabel)
            .toString().toRequestBody(jsonType)
        val request = Request.Builder().url("$base/v1/pairings").post(body).build()
        execute(request) { json ->
            ConnectorPairing(
                json.getString("pairingId"),
                json.getString("pairingCode"),
                json.getString("deviceSecret"),
                json.getString("expiresAt")
            )
        }
    }

    suspend fun pairingStatus(pairing: ConnectorPairing): ConnectorPairingStatus = withContext(Dispatchers.IO) {
        val base = relayUrl() ?: throw IllegalStateException("Relay address is missing")
        val request = Request.Builder()
            .url("$base/v1/pairings/status?pairingId=${Uri.encode(pairing.pairingId)}")
            .header("X-Device-Pairing-Secret", pairing.deviceSecret)
            .get().build()
        execute(request) { json ->
            val token = json.optString("deviceToken").takeIf { it.isNotBlank() }
            if (token != null) ConnectorTokenStore(appContext).save(token)
            ConnectorPairingStatus(json.optString("status", "unknown"), token)
        }
    }

    suspend fun uploadDiagnostic(kind: String, payload: JSONObject): Long = withContext(Dispatchers.IO) {
        require(kind in setOf("status", "log", "crash", "screen", "feature_snapshot"))
        val base = relayUrl() ?: throw IllegalStateException("Relay address is missing")
        val token = ConnectorTokenStore(appContext).read()
            ?: throw IllegalStateException("NEXCRAFT is not paired")
        val body = JSONObject().put("kind", kind).put("payload", payload)
            .toString().toRequestBody(jsonType)
        val request = Request.Builder().url("$base/v1/diagnostics")
            .header("Authorization", "Bearer $token").post(body).build()
        execute(request) { it.getLong("id") }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        val store = ConnectorTokenStore(appContext)
        val token = store.read()
        try {
            val base = relayUrl()
            if (base != null && token != null) {
                val request = Request.Builder().url("$base/v1/device/disconnect")
                    .header("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(jsonType)).build()
                execute(request) { Unit }
            }
        } finally {
            store.clear()
        }
    }

    private fun normalizeRelayUrl(value: String): String {
        val uri = Uri.parse(value.trim().trimEnd('/'))
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank()) {
            "Relay must use a valid HTTPS address"
        }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Relay address must not contain credentials, query parameters, or fragments"
        }
        return uri.toString().trimEnd('/')
    }

    private inline fun <T> execute(request: Request, parse: (JSONObject) -> T): T {
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val reason = runCatching { JSONObject(text).optString("error") }.getOrNull()
                throw IOException(reason?.takeIf { it.isNotBlank() } ?: "Relay request failed (${response.code})")
            }
            return parse(JSONObject(text))
        }
    }
}
