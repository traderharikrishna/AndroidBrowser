package com.example.proxy

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class LocalProxyServer(
    private val onTelemetryChanged: (ProxyTelemetry) -> Unit
) {
    private var scope: CoroutineScope? = null
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    @Volatile
    private var currentConfig: ProxyConfig = ProxyConfig()

    @Volatile
    private var webViewOverrideSupported: Boolean = false

    private val activeConnections = AtomicInteger(0)
    private val totalConnections = AtomicLong(0L)
    private val blockedRequests = AtomicLong(0L)
    private val bytesSent = AtomicLong(0L)
    private val bytesReceived = AtomicLong(0L)
    private val logIdGen = AtomicLong(0L)
    private val recentLogs = ConcurrentLinkedDeque<ProxyLogEntry>()

    @Volatile
    private var lastError: String? = null

    val localPort: Int
        get() = serverSocket?.localPort ?: 0

    val isRunning: Boolean
        get() = serverSocket?.let { !it.isClosed } == true

    fun updateConfig(config: ProxyConfig, overrideSupported: Boolean) {
        currentConfig = config
        webViewOverrideSupported = overrideSupported
        emitTelemetry()
    }

    @Synchronized
    fun start(config: ProxyConfig, overrideSupported: Boolean): Int {
        currentConfig = config
        webViewOverrideSupported = overrideSupported
        if (isRunning) {
            emitTelemetry()
            return localPort
        }

        lastError = null
        val loopback = InetAddress.getByName("127.0.0.1")
        val socket = ServerSocket(0, 64, loopback)
        serverSocket = socket

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope

        acceptJob = newScope.launch {
            while (isActive && !socket.isClosed) {
                try {
                    val client = socket.accept()
                    launch {
                        handleClient(client)
                    }
                } catch (e: IOException) {
                    if (!socket.isClosed) {
                        lastError = e.message ?: "Proxy accept error"
                        emitTelemetry()
                    }
                    break
                }
            }
        }
        emitTelemetry()
        return socket.localPort
    }

    @Synchronized
    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        scope?.cancel()
        scope = null
        activeConnections.set(0)
        emitTelemetry()
    }

    fun clearLogs() {
        recentLogs.clear()
        emitTelemetry()
    }

    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        activeConnections.incrementAndGet()
        totalConnections.incrementAndGet()
        emitTelemetry()

        var targetSocket: Socket? = null
        try {
            client.soTimeout = 30_000
            client.tcpNoDelay = true
            val clientIn = BufferedInputStream(client.getInputStream(), 16 * 1024)
            val clientOut = client.getOutputStream()

            val headerBytes = readHttpHeaderBlock(clientIn) ?: return@withContext
            val headerText = String(headerBytes, StandardCharsets.ISO_8859_1)
            val lines = headerText.split("\r\n").filter { it.isNotEmpty() }
            if (lines.isEmpty()) return@withContext

            val requestLine = lines.first()
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val target = parts[1]
            val config = currentConfig

            if (method == "CONNECT") {
                val (host, port) = parseHostPort(target, 443)
                if (config.blockTrackers && TrackerBlocklist.isBlocked(host)) {
                    blockedRequests.incrementAndGet()
                    clientOut.write(
                        "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            .toByteArray(StandardCharsets.ISO_8859_1)
                    )
                    clientOut.flush()
                    appendLog(method, host, port, ProxyLogStatus.BLOCKED, 0L, "Tracker Shield")
                    return@withContext
                }

                targetSocket = openTunnelSocket(host, port, config)
                clientOut.write(
                    "HTTP/1.1 200 Connection Established\r\nProxy-Agent: RelayWeb/1.0\r\n\r\n"
                        .toByteArray(StandardCharsets.ISO_8859_1)
                )
                clientOut.flush()

                val transferred = relayBidirectional(clientIn, clientOut, targetSocket)
                appendLog(method, host, port, ProxyLogStatus.TUNNELED, transferred, config.mode.name)
            } else {
                val uri = runCatching { URI(target) }.getOrNull()
                val hostHeader = lines.drop(1)
                    .firstOrNull { it.startsWith("Host:", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()
                val rawHost = uri?.host ?: hostHeader ?: ""
                val (host, port) = if (uri?.host != null) {
                    uri.host to (if (uri.port > 0) uri.port else if (uri.scheme == "https") 443 else 80)
                } else {
                    parseHostPort(rawHost, 80)
                }

                if (host.isBlank()) {
                    clientOut.write(
                        "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            .toByteArray(StandardCharsets.ISO_8859_1)
                    )
                    clientOut.flush()
                    return@withContext
                }

                if (config.blockTrackers && TrackerBlocklist.isBlocked(host)) {
                    blockedRequests.incrementAndGet()
                    clientOut.write(
                        "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            .toByteArray(StandardCharsets.ISO_8859_1)
                    )
                    clientOut.flush()
                    appendLog(method, host, port, ProxyLogStatus.BLOCKED, 0L, "Tracker Shield")
                    return@withContext
                }

                val forwardedHeaders = buildForwardHttpHeaders(lines, uri, config)
                if (config.mode == ProxyMode.UPSTREAM_HTTP && config.hasValidUpstream) {
                    val upstream = Socket()
                    upstream.connect(InetSocketAddress(config.upstreamHost, config.upstreamPort), 12_000)
                    upstream.soTimeout = 30_000
                    targetSocket = upstream
                    val upstreamOut = upstream.getOutputStream()
                    upstreamOut.write(forwardedHeaders)
                    upstreamOut.flush()
                    val transferred = relayBidirectional(clientIn, clientOut, upstream)
                    appendLog(method, host, port, ProxyLogStatus.TUNNELED, transferred, "HTTP Upstream")
                } else {
                    targetSocket = openTunnelSocket(host, port, config)
                    val targetOut = targetSocket.getOutputStream()
                    targetOut.write(forwardedHeaders)
                    targetOut.flush()
                    val transferred = relayBidirectional(clientIn, clientOut, targetSocket)
                    appendLog(method, host, port, ProxyLogStatus.TUNNELED, transferred, config.mode.name)
                }
            }
        } catch (e: Exception) {
            lastError = e.message ?: "Tunnel error"
            appendLog("ERR", "tunnel", 0, ProxyLogStatus.ERROR, 0L, e.message ?: "IO failure")
            runCatching {
                client.getOutputStream().write(
                    "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                        .toByteArray(StandardCharsets.ISO_8859_1)
                )
            }
        } finally {
            runCatching { targetSocket?.close() }
            runCatching { client.close() }
            activeConnections.decrementAndGet()
            emitTelemetry()
        }
    }

    private fun openTunnelSocket(host: String, port: Int, config: ProxyConfig): Socket {
        return when (config.mode) {
            ProxyMode.UPSTREAM_HTTP -> {
                if (!config.hasValidUpstream) {
                    throw IOException("Upstream HTTP host/port not configured")
                }
                val socket = Socket()
                socket.connect(InetSocketAddress(config.upstreamHost, config.upstreamPort), 12_000)
                socket.soTimeout = 30_000
                socket.tcpNoDelay = true
                val sb = StringBuilder()
                sb.append("CONNECT ").append(host).append(":").append(port).append(" HTTP/1.1\r\n")
                sb.append("Host: ").append(host).append(":").append(port).append("\r\n")
                if (config.username.isNotBlank()) {
                    val token = Base64.encodeToString(
                        "${config.username}:${config.password}".toByteArray(StandardCharsets.UTF_8),
                        Base64.NO_WRAP
                    )
                    sb.append("Proxy-Authorization: Basic ").append(token).append("\r\n")
                }
                sb.append("\r\n")
                val out = socket.getOutputStream()
                out.write(sb.toString().toByteArray(StandardCharsets.ISO_8859_1))
                out.flush()

                val responseBytes = readHttpHeaderBlock(socket.getInputStream())
                    ?: throw IOException("Empty response from upstream HTTP proxy")
                val responseFirstLine = String(responseBytes, StandardCharsets.ISO_8859_1)
                    .lineSequence()
                    .firstOrNull()
                    .orEmpty()
                if (!responseFirstLine.contains(" 200")) {
                    socket.close()
                    throw IOException("Upstream HTTP proxy rejected CONNECT: $responseFirstLine")
                }
                socket
            }

            ProxyMode.UPSTREAM_SOCKS5 -> {
                if (!config.hasValidUpstream) {
                    throw IOException("Upstream SOCKS5 host/port not configured")
                }
                openSocks5Tunnel(host, port, config)
            }

            ProxyMode.LOCAL_RELAY, ProxyMode.DIRECT -> {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), 12_000)
                socket.soTimeout = 30_000
                socket.tcpNoDelay = true
                socket
            }
        }
    }

    private fun openSocks5Tunnel(targetHost: String, targetPort: Int, config: ProxyConfig): Socket {
        val socket = Socket()
        socket.connect(InetSocketAddress(config.upstreamHost, config.upstreamPort), 12_000)
        socket.soTimeout = 30_000
        socket.tcpNoDelay = true
        val input = socket.getInputStream()
        val output = socket.getOutputStream()

        val hasAuth = config.username.isNotBlank()
        // 1. Greeting
        val greeting = if (hasAuth) {
            byteArrayOf(0x05, 0x02, 0x00, 0x02)
        } else {
            byteArrayOf(0x05, 0x01, 0x00)
        }
        output.write(greeting)
        output.flush()

        val methodSelection = readExactBytes(input, 2)
        if (methodSelection[0] != 0x05.toByte()) {
            socket.close()
            throw IOException("Invalid SOCKS5 version reply")
        }
        val chosenMethod = methodSelection[1].toInt() and 0xFF
        if (chosenMethod == 0xFF) {
            socket.close()
            throw IOException("SOCKS5 server rejected authentication methods")
        }
        if (chosenMethod == 0x02) {
            // RFC 1929 Username/Password Auth
            val userBytes = config.username.toByteArray(StandardCharsets.UTF_8)
            val passBytes = config.password.toByteArray(StandardCharsets.UTF_8)
            val authPacket = ByteArrayOutputStream()
            authPacket.write(0x01)
            authPacket.write(userBytes.size.coerceAtMost(255))
            authPacket.write(userBytes, 0, userBytes.size.coerceAtMost(255))
            authPacket.write(passBytes.size.coerceAtMost(255))
            authPacket.write(passBytes, 0, passBytes.size.coerceAtMost(255))
            output.write(authPacket.toByteArray())
            output.flush()
            val authReply = readExactBytes(input, 2)
            if (authReply[1] != 0x00.toByte()) {
                socket.close()
                throw IOException("SOCKS5 authentication failed")
            }
        }

        // 2. CONNECT Request (ATYP = 0x03 Domain)
        val hostBytes = targetHost.toByteArray(StandardCharsets.UTF_8)
        if (hostBytes.size > 255) {
            socket.close()
            throw IOException("Host name too long for SOCKS5")
        }
        val connectPacket = ByteArrayOutputStream()
        connectPacket.write(0x05)
        connectPacket.write(0x01) // CONNECT
        connectPacket.write(0x00) // RSV
        connectPacket.write(0x03) // DOMAINNAME
        connectPacket.write(hostBytes.size)
        connectPacket.write(hostBytes)
        connectPacket.write((targetPort shr 8) and 0xFF)
        connectPacket.write(targetPort and 0xFF)
        output.write(connectPacket.toByteArray())
        output.flush()

        val replyHeader = readExactBytes(input, 4)
        if (replyHeader[1] != 0x00.toByte()) {
            val code = replyHeader[1].toInt() and 0xFF
            socket.close()
            throw IOException("SOCKS5 CONNECT failed with code $code")
        }
        when (replyHeader[3].toInt() and 0xFF) {
            0x01 -> readExactBytes(input, 6) // IPv4 + 2 byte port
            0x03 -> {
                val len = readExactBytes(input, 1)[0].toInt() and 0xFF
                readExactBytes(input, len + 2)
            }
            0x04 -> readExactBytes(input, 18) // IPv6 + 2 byte port
            else -> throw IOException("Unsupported SOCKS5 address type")
        }
        return socket
    }

    private fun buildForwardHttpHeaders(
        lines: List<String>,
        uri: URI?,
        config: ProxyConfig
    ): ByteArray {
        val parts = lines.first().split(" ")
        val method = parts.getOrElse(0) { "GET" }
        val target = parts.getOrElse(1) { "/" }
        val version = parts.getOrElse(2) { "HTTP/1.1" }

        val forwardTarget = if (config.mode == ProxyMode.UPSTREAM_HTTP) {
            target
        } else {
            val rawPath = uri?.rawPath?.takeIf { it.isNotEmpty() } ?: "/"
            val query = uri?.rawQuery?.let { "?$it" } ?: ""
            rawPath + query
        }

        val sb = StringBuilder()
        sb.append("$method $forwardTarget $version\r\n")
        for (line in lines.drop(1)) {
            val lower = line.lowercase()
            if (lower.startsWith("proxy-connection:")) continue
            if (config.mode != ProxyMode.UPSTREAM_HTTP && lower.startsWith("proxy-authorization:")) continue
            if (config.stripTrackingHeaders && (lower.startsWith("x-forwarded-for:") || lower.startsWith("via:"))) {
                continue
            }
            sb.append(line).append("\r\n")
        }
        if (config.mode == ProxyMode.UPSTREAM_HTTP && config.username.isNotBlank()) {
            val token = Base64.encodeToString(
                "${config.username}:${config.password}".toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
            sb.append("Proxy-Authorization: Basic ").append(token).append("\r\n")
        }
        sb.append("Connection: close\r\n\r\n")
        return sb.toString().toByteArray(StandardCharsets.ISO_8859_1)
    }

    private suspend fun relayBidirectional(
        clientIn: InputStream,
        clientOut: OutputStream,
        targetSocket: Socket
    ): Long = withContext(Dispatchers.IO) {
        val targetIn = targetSocket.getInputStream()
        val targetOut = targetSocket.getOutputStream()
        val upBytes = AtomicLong(0L)
        val downBytes = AtomicLong(0L)

        val upstreamJob = launch {
            try {
                val buffer = ByteArray(16 * 1024)
                while (isActive) {
                    val read = clientIn.read(buffer)
                    if (read <= 0) break
                    targetOut.write(buffer, 0, read)
                    targetOut.flush()
                    upBytes.addAndGet(read.toLong())
                    bytesSent.addAndGet(read.toLong())
                }
            } catch (_: IOException) {
            } finally {
                runCatching { targetSocket.shutdownOutput() }
            }
        }

        val downstreamJob = launch {
            try {
                val buffer = ByteArray(16 * 1024)
                while (isActive) {
                    val read = targetIn.read(buffer)
                    if (read <= 0) break
                    clientOut.write(buffer, 0, read)
                    clientOut.flush()
                    downBytes.addAndGet(read.toLong())
                    bytesReceived.addAndGet(read.toLong())
                }
            } catch (_: IOException) {
            } finally {
                runCatching { clientOut.flush() }
            }
        }

        upstreamJob.join()
        downstreamJob.join()
        upBytes.get() + downBytes.get()
    }

    private fun readHttpHeaderBlock(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream(1024)
        var prev3 = 0
        var prev2 = 0
        var prev1 = 0
        while (out.size() < 64 * 1024) {
            val b = input.read()
            if (b == -1) {
                return if (out.size() == 0) null else out.toByteArray()
            }
            out.write(b)
            if (prev3 == '\r'.code && prev2 == '\n'.code && prev1 == '\r'.code && b == '\n'.code) {
                return out.toByteArray()
            }
            prev3 = prev2
            prev2 = prev1
            prev1 = b
        }
        throw IOException("HTTP header block exceeded 64KB")
    }

    private fun readExactBytes(input: InputStream, count: Int): ByteArray {
        val buf = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buf, offset, count - offset)
            if (read == -1) throw IOException("Unexpected EOF during handshake")
            offset += read
        }
        return buf
    }

    private fun parseHostPort(hostPort: String, defaultPort: Int): Pair<String, Int> {
        val trimmed = hostPort.trim()
        val lastColon = trimmed.lastIndexOf(':')
        return if (lastColon > 0 && !trimmed.endsWith("]")) {
            val h = trimmed.substring(0, lastColon).trim('[', ']')
            val p = trimmed.substring(lastColon + 1).toIntOrNull() ?: defaultPort
            h to p
        } else {
            trimmed.trim('[', ']') to defaultPort
        }
    }

    private fun appendLog(
        method: String,
        host: String,
        port: Int,
        status: ProxyLogStatus,
        bytes: Long,
        note: String
    ) {
        val entry = ProxyLogEntry(
            id = logIdGen.incrementAndGet(),
            timestampMs = System.currentTimeMillis(),
            method = method,
            host = host,
            port = port,
            status = status,
            bytesTransferred = bytes,
            note = note
        )
        recentLogs.addFirst(entry)
        while (recentLogs.size > 80) {
            recentLogs.pollLast()
        }
        emitTelemetry()
    }

    private fun emitTelemetry() {
        val config = currentConfig
        onTelemetryChanged(
            ProxyTelemetry(
                isRunning = isRunning,
                localPort = localPort,
                activeMode = config.mode,
                activeProfileName = config.activeProfileName,
                activeConnections = activeConnections.get().coerceAtLeast(0),
                totalConnections = totalConnections.get(),
                blockedRequests = blockedRequests.get(),
                bytesSent = bytesSent.get(),
                bytesReceived = bytesReceived.get(),
                webViewOverrideSupported = webViewOverrideSupported,
                lastError = lastError,
                recentLogs = recentLogs.toList()
            )
        )
    }
}
