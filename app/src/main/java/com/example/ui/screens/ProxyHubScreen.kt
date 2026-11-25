package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProxyProfileEntity
import com.example.data.preferences.BrowserSettings
import com.example.proxy.ProxyDiagnosticState
import com.example.proxy.ProxyLogEntry
import com.example.proxy.ProxyLogStatus
import com.example.proxy.ProxyMode
import com.example.proxy.ProxyTelemetry
import com.example.ui.components.formatBytesCompact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyHubScreen(
    telemetry: ProxyTelemetry,
    diagnostics: ProxyDiagnosticState,
    settings: BrowserSettings,
    profiles: List<ProxyProfileEntity>,
    onBack: () -> Unit,
    onSelectMode: (ProxyMode) -> Unit,
    onActivateProfile: (ProxyProfileEntity) -> Unit,
    onSaveProfile: (
        name: String,
        mode: ProxyMode,
        host: String,
        port: Int,
        user: String,
        pass: String,
        blockTrackers: Boolean,
        stripHeaders: Boolean
    ) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onRunDiagnostics: () -> Unit,
    onClearLogs: () -> Unit,
    onToggleTrackerShield: (Boolean) -> Unit,
    onToggleStripHeaders: (Boolean) -> Unit
) {
    BackHandler(onBack = onBack)

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Text(
                        text = "Inbuilt Proxy Hub",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("proxy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Browser"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_proxy_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Upstream Proxy"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Active Tunnel Status Card
                item {
                    val active = telemetry.isRunning && telemetry.activeMode != ProxyMode.DIRECT
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) MaterialTheme.colorScheme.secondary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (active) "TUNNEL ACTIVE" else "DIRECT BYPASS",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = if (active) "127.0.0.1:${telemetry.localPort}" else "Port Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = telemetry.activeProfileName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = if (telemetry.webViewOverrideSupported) {
                                    "WebView ProxyController API active • Media3 OkHttp routed"
                                } else {
                                    "Lollipop Legacy Relay + OkHttp proxy fallback active"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricChip("Active", telemetry.activeConnections.toString())
                                MetricChip("Total", telemetry.totalConnections.toString())
                                MetricChip("Blocked", telemetry.blockedRequests.toString())
                                MetricChip(
                                    "Data",
                                    formatBytesCompact(telemetry.bytesSent + telemetry.bytesReceived)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (active) onSelectMode(ProxyMode.DIRECT)
                                        else onSelectMode(ProxyMode.LOCAL_RELAY)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("proxy_master_toggle"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (active) "Disconnect" else "Start Relay")
                                }

                                OutlinedButton(
                                    onClick = onRunDiagnostics,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("proxy_test_ip_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NetworkCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check Exit IP")
                                }
                            }
                        }
                    }
                }

                // Diagnostics Result Banner
                if (diagnostics !is ProxyDiagnosticState.Idle) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (diagnostics) {
                                    is ProxyDiagnosticState.Testing -> {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Probing exit IP & latency through active route...")
                                    }
                                    is ProxyDiagnosticState.Success -> {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Exit IP: ${diagnostics.exitIp} (${diagnostics.latencyMs} ms)",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "${diagnostics.modeLabel} • Routed via proxy: ${diagnostics.routedViaProxy}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    is ProxyDiagnosticState.Failure -> {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Diagnostic Check Failed", style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = diagnostics.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    ProxyDiagnosticState.Idle -> Unit
                                }
                            }
                        }
                    }
                }

                // Routing Modes
                item {
                    Text("Routing Mode", style = MaterialTheme.typography.titleMedium)
                }

                items(ProxyMode.entries) { mode ->
                    val selected = telemetry.activeMode == mode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (mode.let { it == ProxyMode.UPSTREAM_HTTP || it == ProxyMode.UPSTREAM_SOCKS5 } &&
                                    settings.activeProxyHost.isBlank()
                                ) {
                                    showAddDialog = true
                                } else {
                                    onSelectMode(mode)
                                }
                            }
                            .testTag("proxy_mode_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (mode == ProxyMode.LOCAL_RELAY) Icons.Rounded.Shield else Icons.Rounded.VpnLock,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if ((mode == ProxyMode.UPSTREAM_HTTP || mode == ProxyMode.UPSTREAM_SOCKS5) &&
                                        settings.activeProxyHost.isNotBlank()
                                    ) {
                                        "${settings.activeProxyHost}:${settings.activeProxyPort}"
                                    } else {
                                        mode.subtitle
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Privacy & Shield Toggles
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tracker & Ad Domain Shield", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Blocks known tracker/ad hostnames inside proxy & WebView",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.trackerShieldEnabled,
                                    onCheckedChange = onToggleTrackerShield,
                                    modifier = Modifier.testTag("switch_tracker_shield")
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Strip Proxy Forwarding Headers", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Removes X-Forwarded-For and Via headers on HTTP requests",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.stripTrackingHeaders,
                                    onCheckedChange = onToggleStripHeaders,
                                    modifier = Modifier.testTag("switch_strip_headers")
                                )
                            }
                        }
                    }
                }

                // Saved Upstream Proxy Profiles
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Upstream Proxies", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_proxy_inline_button")
                        ) {
                            Text("+ Add Server")
                        }
                    }
                }

                if (profiles.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "No custom upstream HTTP or SOCKS5 servers saved yet. Tap '+ Add Server' to chain traffic through an external proxy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(profiles, key = { it.id }) { profile ->
                        val isActive = settings.activeProfileId == profile.id &&
                            telemetry.activeMode.name == profile.modeName
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onActivateProfile(profile) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(profile.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${profile.modeName.removePrefix("UPSTREAM_")} • ${profile.host}:${profile.port}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteProfile(profile.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete proxy profile",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Connection Logs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Tunnel Telemetry", style = MaterialTheme.typography.titleMedium)
                        if (telemetry.recentLogs.isNotEmpty()) {
                            TextButton(onClick = onClearLogs) {
                                Text("Clear")
                            }
                        }
                    }
                }

                if (telemetry.recentLogs.isEmpty()) {
                    item {
                        Text(
                            text = "Start browsing with the relay active to inspect live tunneled hosts and blocked trackers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(telemetry.recentLogs.take(30), key = { it.id }) { entry ->
                        ProxyLogRow(entry)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProxyProfileDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, mode, host, port, user, pass, blockTrackers, stripHeaders ->
                onSaveProfile(name, mode, host, port, user, pass, blockTrackers, stripHeaders)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProxyLogRow(entry: ProxyLogEntry) {
    val timeStr = remember(entry.timestampMs) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestampMs))
    }
    val statusColor = when (entry.status) {
        ProxyLogStatus.TUNNELED -> MaterialTheme.colorScheme.secondary
        ProxyLogStatus.BLOCKED -> MaterialTheme.colorScheme.tertiary
        ProxyLogStatus.ERROR -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.method} ${entry.host}:${entry.port}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$timeStr • ${entry.note} • ${formatBytesCompact(entry.bytesTransferred)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.status.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = statusColor
            )
        }
    }
}

@Composable
private fun AddProxyProfileDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        mode: ProxyMode,
        host: String,
        port: Int,
        user: String,
        pass: String,
        blockTrackers: Boolean,
        stripHeaders: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(ProxyMode.UPSTREAM_SOCKS5) }
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("1080") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var blockTrackers by remember { mutableStateOf(true) }
    var stripHeaders by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Upstream Proxy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMode == ProxyMode.UPSTREAM_SOCKS5,
                        onClick = {
                            selectedMode = ProxyMode.UPSTREAM_SOCKS5
                            if (portText == "8080") portText = "1080"
                        },
                        label = { Text("SOCKS5") }
                    )
                    FilterChip(
                        selected = selectedMode == ProxyMode.UPSTREAM_HTTP,
                        onClick = {
                            selectedMode = ProxyMode.UPSTREAM_HTTP
                            if (portText == "1080") portText = "8080"
                        },
                        label = { Text("HTTP / HTTPS") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Office Relay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host / IP") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull()?.coerceIn(1, 65535) ?: 1080
                    if (host.isNotBlank()) {
                        onSave(name, selectedMode, host, port, username, password, blockTrackers, stripHeaders)
                    }
                },
                enabled = host.isNotBlank()
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
