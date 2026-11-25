package com.example.proxy

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig as WebKitProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

object ProxyEngineManager {

    private val _telemetry = MutableStateFlow(ProxyTelemetry())
    val telemetry: StateFlow<ProxyTelemetry> = _telemetry.asStateFlow()

    private val _diagnosticState = MutableStateFlow<ProxyDiagnosticState>(ProxyDiagnosticState.Idle)
    val diagnosticState: StateFlow<ProxyDiagnosticState> = _diagnosticState.asStateFlow()

    @Volatile
    private var activeConfig: ProxyConfig = ProxyConfig()

    private val localProxyServer = LocalProxyServer { snapshot ->
        _telemetry.value = snapshot
    }

    private val mainOrDirectExecutor = Executor { command -> command.run() }

    fun getActiveConfig(): ProxyConfig = activeConfig

    fun isWebViewProxyOverrideSupported(): Boolean {
        return runCatching {
            WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
        }.getOrDefault(false)
    }

    fun applyConfig(context: Context, config: ProxyConfig) {
        activeConfig = config
        val overrideSupported = isWebViewProxyOverrideSupported()
        if (config.isProxyEnabled) {
            val port = localProxyServer.start(config, overrideSupported)
            bindWebViewProxy(port)
            startForegroundServiceSafe(context)
        } else {
            clearWebViewProxy()
            localProxyServer.updateConfig(config, overrideSupported)
            localProxyServer.stop()
            stopForegroundServiceSafe(context)
        }
    }

    fun startRelayInService(config: ProxyConfig): Int {
        activeConfig = config
        val overrideSupported = isWebViewProxyOverrideSupported()
        val port = localProxyServer.start(config, overrideSupported)
        bindWebViewProxy(port)
        return port
    }

    fun stopRelay(context: Context) {
        activeConfig = activeConfig.copy(mode = ProxyMode.DIRECT)
        clearWebViewProxy()
        localProxyServer.stop()
        stopForegroundServiceSafe(context)
    }

    fun clearLogs() {
        localProxyServer.clearLogs()
    }

    fun buildProxyOkHttpClient(): OkHttpClient {
        val currentTelemetry = _telemetry.value
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        if (currentTelemetry.isRunning && currentTelemetry.localPort > 0 && activeConfig.isProxyEnabled) {
            builder.proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress("127.0.0.1", currentTelemetry.localPort)
                )
            )
        } else {
            builder.proxy(Proxy.NO_PROXY)
        }
        return builder.build()
    }

    suspend fun runProxyDiagnostics() = withContext(Dispatchers.IO) {
        _diagnosticState.value = ProxyDiagnosticState.Testing
        val startMs = System.currentTimeMillis()
        try {
            val client = buildProxyOkHttpClient()
            val request = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .header("User-Agent", "RelayWeb/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                if (!response.isSuccessful) {
                    _diagnosticState.value = ProxyDiagnosticState.Failure(
                        "HTTP ${response.code} from IP check endpoint"
                    )
                    return@withContext
                }
                val body = response.body?.string().orEmpty()
                val ip = Regex("\"ip\"\\s*:\\s*\"([^\"]+)\"")
                    .find(body)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: body.take(40).ifBlank { "Unknown" }

                val t = _telemetry.value
                _diagnosticState.value = ProxyDiagnosticState.Success(
                    exitIp = ip,
                    latencyMs = elapsed,
                    routedViaProxy = t.isRunning && activeConfig.isProxyEnabled,
                    modeLabel = activeConfig.mode.label
                )
            }
        } catch (e: Exception) {
            _diagnosticState.value = ProxyDiagnosticState.Failure(
                e.message ?: "Unable to reach diagnostic endpoint"
            )
        }
    }

    fun resetDiagnosticState() {
        _diagnosticState.value = ProxyDiagnosticState.Idle
    }

    private fun bindWebViewProxy(port: Int) {
        if (port <= 0) return
        // Apply legacy Java/Lollipop system proxy properties as fallback for API 21-28
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", port.toString())
        System.setProperty("https.proxyHost", "127.0.0.1")
        System.setProperty("https.proxyPort", port.toString())

        if (isWebViewProxyOverrideSupported()) {
            runCatching {
                val proxyConfig = WebKitProxyConfig.Builder()
                    .addProxyRule("http://127.0.0.1:$port")
                    .build()
                ProxyController.getInstance().setProxyOverride(
                    proxyConfig,
                    mainOrDirectExecutor
                ) { }
            }
        }
    }

    private fun clearWebViewProxy() {
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
        System.clearProperty("https.proxyHost")
        System.clearProperty("https.proxyPort")

        if (isWebViewProxyOverrideSupported()) {
            runCatching {
                ProxyController.getInstance().clearProxyOverride(mainOrDirectExecutor) { }
            }
        }
    }

    private fun startForegroundServiceSafe(context: Context) {
        runCatching {
            val intent = Intent(context, ProxyForegroundService::class.java).apply {
                action = ProxyForegroundService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun stopForegroundServiceSafe(context: Context) {
        runCatching {
            val intent = Intent(context, ProxyForegroundService::class.java).apply {
                action = ProxyForegroundService.ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
