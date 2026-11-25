package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proxy.ProxyMode
import com.example.proxy.ProxyTelemetry

@Composable
fun BottomNavToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    detectedMediaCount: Int,
    isMediaPlaying: Boolean,
    proxyTelemetry: ProxyTelemetry,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onOpenProxyHub: () -> Unit,
    onOpenMediaPlayer: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val proxyActive = proxyTelemetry.isRunning && proxyTelemetry.activeMode != ProxyMode.DIRECT

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }

                // Glowing Proxy Pill
                Row(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (proxyActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.dp,
                            color = if (proxyActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onOpenProxyHub)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("nav_proxy_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (proxyActive) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.VpnLock,
                        contentDescription = "Proxy Hub",
                        tint = if (proxyActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (proxyActive) "PROXY" else "DIRECT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (proxyActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // HTML5 Media Player button with badge
                IconButton(
                    onClick = onOpenMediaPlayer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_media_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (detectedMediaCount > 0 || isMediaPlaying) {
                                Badge(
                                    containerColor = if (isMediaPlaying) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = if (detectedMediaCount > 0) detectedMediaCount.toString() else "▶",
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = "HTML5 Media Player",
                            tint = if (isMediaPlaying || detectedMediaCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Tab switcher counter button
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTabs)
                        .padding(8.dp)
                        .testTag("nav_tabs_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(
                                width = 1.8.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabCount.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Bookmarks, History & Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
