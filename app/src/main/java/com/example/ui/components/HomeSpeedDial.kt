package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.browser.BrowserPresets
import com.example.browser.SpeedDialSite
import com.example.data.local.BookmarkEntity
import com.example.proxy.ProxyMode
import com.example.proxy.ProxyTelemetry

@Composable
fun HomeSpeedDial(
    isIncognito: Boolean,
    proxyTelemetry: ProxyTelemetry,
    bookmarks: List<BookmarkEntity>,
    onNavigate: (String) -> Unit,
    onOpenProxyHub: () -> Unit,
    onOpenMediaPlayer: () -> Unit,
    onToggleIncognito: () -> Unit,
    modifier: Modifier = Modifier
) {
    val proxyActive = proxyTelemetry.isRunning && proxyTelemetry.activeMode != ProxyMode.DIRECT

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner with live Proxy Telemetry Pill
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_browser_home_banner),
                        contentDescription = "RelayWeb network horizon banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x66090D14),
                                        Color(0xEE090D14)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (proxyActive) Color(0xCC064E3B) else Color(0xCC1E293B),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onOpenProxyHub)
                                    .testTag("home_proxy_status_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (proxyActive) Color(0xFF10B981) else Color(0xFF94A3B8)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (proxyActive) {
                                            "${proxyTelemetry.activeMode.label} • :${proxyTelemetry.localPort}"
                                        } else {
                                            "Direct Mode • Tap to Enable Proxy"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFF1F5F9)
                                    )
                                }
                            }

                            if (isIncognito) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xCC543300)
                                ) {
                                    Text(
                                        text = "INCOGNITO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFDE68A),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = if (isIncognito) "Incognito Browsing" else "RelayWeb Browser",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isIncognito) {
                                    "History, cookies, form data & disk cache are disabled in this tab"
                                } else {
                                    "HTML5 Web Engine • Media3 Audio/Video • Inbuilt HTTP & SOCKS5 Proxy"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // Quick Action Cards: Proxy Hub + HTML5 Media Player + Incognito
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenProxyHub,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("home_open_proxy_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VpnLock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Proxy", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onOpenMediaPlayer,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("home_open_media_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Media", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onToggleIncognito,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("home_incognito_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = if (isIncognito) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isIncognito) "Exit" else "Incognito", maxLines = 1)
                    }
                }
            }

            // Live Proxy Telemetry Mini Strip
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenProxyHub),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryMiniStat(
                            icon = Icons.Rounded.Speed,
                            label = "Connections",
                            value = proxyTelemetry.totalConnections.toString()
                        )
                        TelemetryMiniStat(
                            icon = Icons.Rounded.Shield,
                            label = "Trackers Blocked",
                            value = proxyTelemetry.blockedRequests.toString()
                        )
                        TelemetryMiniStat(
                            icon = Icons.Rounded.Language,
                            label = "Traffic",
                            value = formatBytesCompact(proxyTelemetry.bytesSent + proxyTelemetry.bytesReceived)
                        )
                    }
                }
            }

            // Speed Dial Grid
            item {
                Text(
                    text = "Speed Dial",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            val rows = BrowserPresets.speedDialSites.chunked(2)
            items(rows.size) { rowIndex ->
                val pair = rows[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { site ->
                        SpeedDialCard(
                            site = site,
                            onClick = { onNavigate(site.url) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Saved Bookmarks Section
            if (bookmarks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Bookmarks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(bookmarks.take(6).size) { idx ->
                    val bm = bookmarks[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(bm.url) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bm.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bm.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpeedDialCard(
    site: SpeedDialSite,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("speed_dial_${site.badge.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = site.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Icon(
                    imageVector = if (site.badge == "HTML5") Icons.Rounded.Science else Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = site.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = site.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatBytesCompact(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1fKB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1fMB", mb)
    val gb = mb / 1024.0
    return String.format("%.2fGB", gb)
}
