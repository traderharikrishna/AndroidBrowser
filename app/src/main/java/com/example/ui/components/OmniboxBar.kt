package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.BrowserTab
import com.example.browser.DetectedMediaItem
import com.example.proxy.ProxyTelemetry

@Composable
fun OmniboxBar(
    tab: BrowserTab,
    tabs: List<BrowserTab>,
    proxyTelemetry: ProxyTelemetry,
    isBookmarked: Boolean,
    desktopMode: Boolean,
    onNavigate: (String) -> Unit,
    onReloadOrStop: () -> Unit,
    onGoHome: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleIncognito: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onOpenProxyHub: () -> Unit,
    onOpenMediaItem: (DetectedMediaItem) -> Unit,
    onOpenMediaPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(TextFieldValue(tab.url)) }

    LaunchedEffect(tab.id, tab.url, isFocused) {
        if (!isFocused) {
            textValue = TextFieldValue(
                text = tab.url,
                selection = TextRange(tab.url.length)
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!tab.isHomePage) {
                    IconButton(
                        onClick = onGoHome,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Home Speed Dial",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Proxy Shield Status Indicator Pill
                val proxyActive = proxyTelemetry.isRunning && proxyTelemetry.activeMode.name != "DIRECT"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (proxyActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable(onClick = onOpenProxyHub)
                        .testTag("omnibox_proxy_indicator"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = if (proxyActive) "Proxy active" else "Proxy direct",
                        tint = if (proxyActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Editable URL / Search Input Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (tab.isIncognito) MaterialTheme.colorScheme.surfaceContainerHigh
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            tab.isIncognito -> Icons.Rounded.VisibilityOff
                            tab.isHomePage -> Icons.Rounded.Security
                            tab.isSecure -> Icons.Rounded.Lock
                            else -> Icons.Rounded.LockOpen
                        },
                        contentDescription = null,
                        tint = when {
                            tab.isIncognito -> MaterialTheme.colorScheme.tertiary
                            tab.isSecure -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textValue.text.isEmpty()) {
                            Text(
                                text = "Search or enter URL...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val target = textValue.text.trim()
                                    if (target.isNotEmpty()) {
                                        onNavigate(target)
                                    }
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                    if (focusState.isFocused && textValue.text.isNotEmpty()) {
                                        textValue = textValue.copy(
                                            selection = TextRange(0, textValue.text.length)
                                        )
                                    }
                                }
                                .testTag("omnibox_input")
                        )
                    }

                    if (isFocused && textValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = { textValue = TextFieldValue("") },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("omnibox_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear URL",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (!tab.isHomePage) {
                        IconButton(
                            onClick = onReloadOrStop,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("reload_stop_button")
                        ) {
                            Icon(
                                imageVector = if (tab.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                contentDescription = if (tab.isLoading) "Stop loading" else "Reload page",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (!tab.isHomePage) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("bookmark_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark page",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleIncognito,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("incognito_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = if (tab.isIncognito) "Exit Incognito Mode" else "Enter Incognito Mode",
                        tint = if (tab.isIncognito) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleDesktopMode,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("desktop_mode_button")
                ) {
                    Icon(
                        imageVector = if (desktopMode) Icons.Rounded.DesktopWindows else Icons.Rounded.Smartphone,
                        contentDescription = if (desktopMode) "Desktop site mode active" else "Mobile site mode active",
                        tint = if (desktopMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Horizontal Tab Strip when multiple sessions are open
            AnimatedVisibility(
                visible = tabs.size > 1,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .testTag("quick_tab_strip"),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(tabs, key = { it.id }) { sessionTab ->
                        val selected = sessionTab.id == tab.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                selected && sessionTab.isIncognito -> MaterialTheme.colorScheme.tertiaryContainer
                                selected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = when {
                                    selected && sessionTab.isIncognito -> MaterialTheme.colorScheme.tertiary
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }
                            ),
                            modifier = Modifier
                                .widthIn(min = 108.dp, max = 168.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectTab(sessionTab.id) }
                                .testTag("strip_tab_${sessionTab.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (sessionTab.isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = if (sessionTab.isIncognito) MaterialTheme.colorScheme.tertiary
                                    else if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = sessionTab.title.ifBlank { "New Tab" },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = { onCloseTab(sessionTab.id) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("strip_close_tab_${sessionTab.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close tab",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onNewTab)
                                .testTag("strip_new_tab_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Open new tab",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Incognito Stealth Mode Banner
            AnimatedVisibility(visible = tab.isIncognito) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f))
                        .clickable(onClick = onToggleIncognito)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .testTag("incognito_active_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Incognito • No history, cookies, or cache stored",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "EXIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Loading progress indicator
            AnimatedVisibility(visible = tab.isLoading) {
                LinearProgressIndicator(
                    progress = { (tab.progress.coerceIn(5, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Detected HTML5 Media Banner
            AnimatedVisibility(
                visible = tab.detectedMedia.isNotEmpty() && !tab.isHomePage,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val first = tab.detectedMedia.firstOrNull()
                if (first != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                            .clickable {
                                if (tab.detectedMedia.size == 1) onOpenMediaItem(first) else onOpenMediaPlayer()
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("detected_media_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (tab.detectedMedia.size == 1) {
                                    "HTML5 ${first.type.name.lowercase()}: ${first.title}"
                                } else {
                                    "${tab.detectedMedia.size} HTML5 media streams detected"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPEN PLAYER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
