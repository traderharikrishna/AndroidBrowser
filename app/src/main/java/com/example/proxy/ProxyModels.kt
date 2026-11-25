package com.example.proxy

enum class ProxyMode(val label: String, val subtitle: String) {
    DIRECT(
        label = "Direct Connection",
        subtitle = "Bypass proxy relay and connect directly"
    ),
    LOCAL_RELAY(
        label = "Built-in Privacy Relay",
        subtitle = "Local HTTP/CONNECT relay with tracker shield & telemetry"
    ),
    UPSTREAM_HTTP(
        label = "Upstream HTTP Proxy",
        subtitle = "Chain local relay through an external HTTP/HTTPS proxy"
    ),
    UPSTREAM_SOCKS5(
        label = "Upstream SOCKS5 Proxy",
        subtitle = "Chain local relay through an external SOCKS5 tunnel"
    );

    companion object {
        fun fromString(value: String): ProxyMode =
            entries.firstOrNull { it.name == value } ?: LOCAL_RELAY
    }
}

data class ProxyConfig(
    val mode: ProxyMode = ProxyMode.LOCAL_RELAY,
    val upstreamHost: String = "",
    val upstreamPort: Int = 8080,
    val username: String = "",
    val password: String = "",
    val blockTrackers: Boolean = true,
    val stripTrackingHeaders: Boolean = true,
    val activeProfileName: String = "Built-in Privacy Relay"
) {
    val isProxyEnabled: Boolean
        get() = mode != ProxyMode.DIRECT

    val requiresUpstream: Boolean
        get() = mode == ProxyMode.UPSTREAM_HTTP || mode == ProxyMode.UPSTREAM_SOCKS5

    val hasValidUpstream: Boolean
        get() = !requiresUpstream || (upstreamHost.isNotBlank() && upstreamPort in 1..65535)
}

enum class ProxyLogStatus {
    TUNNELED,
    BLOCKED,
    ERROR
}

data class ProxyLogEntry(
    val id: Long,
    val timestampMs: Long,
    val method: String,
    val host: String,
    val port: Int,
    val status: ProxyLogStatus,
    val bytesTransferred: Long = 0L,
    val note: String = ""
)

data class ProxyTelemetry(
    val isRunning: Boolean = false,
    val localPort: Int = 0,
    val activeMode: ProxyMode = ProxyMode.DIRECT,
    val activeProfileName: String = "Direct",
    val activeConnections: Int = 0,
    val totalConnections: Long = 0L,
    val blockedRequests: Long = 0L,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val webViewOverrideSupported: Boolean = false,
    val lastError: String? = null,
    val recentLogs: List<ProxyLogEntry> = emptyList()
)

sealed interface ProxyDiagnosticState {
    data object Idle : ProxyDiagnosticState
    data object Testing : ProxyDiagnosticState
    data class Success(
        val exitIp: String,
        val latencyMs: Long,
        val routedViaProxy: Boolean,
        val modeLabel: String
    ) : ProxyDiagnosticState

    data class Failure(val message: String) : ProxyDiagnosticState
}
