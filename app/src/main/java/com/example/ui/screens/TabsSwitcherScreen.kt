package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.BrowserTab

private enum class TabSessionFilter {
    ALL,
    STANDARD,
    INCOGNITO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsSwitcherScreen(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onDuplicateTab: (String) -> Unit,
    onCloseOtherTabs: (String) -> Unit,
    onCloseAllIncognito: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onNewTab: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var filter by remember { mutableStateOf(TabSessionFilter.ALL) }
    var topMenuExpanded by remember { mutableStateOf(false) }

    val standardCount = remember(tabs) { tabs.count { !it.isIncognito } }
    val incognitoCount = remember(tabs) { tabs.count { it.isIncognito } }
    val displayedTabs = remember(tabs, filter) {
        when (filter) {
            TabSessionFilter.ALL -> tabs
            TabSessionFilter.STANDARD -> tabs.filter { !it.isIncognito }
            TabSessionFilter.INCOGNITO -> tabs.filter { it.isIncognito }
        }
    }

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
                    Column {
                        Text("Web Sessions (${tabs.size})", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "$standardCount standard • $incognitoCount incognito",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("tabs_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { topMenuExpanded = true },
                            modifier = Modifier.testTag("tabs_overflow_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Tab session actions"
                            )
                        }
                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            if (incognitoCount > 0) {
                                DropdownMenuItem(
                                    text = { Text("Close All Incognito Tabs") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                                    },
                                    onClick = {
                                        topMenuExpanded = false
                                        onCloseAllIncognito()
                                    }
                                )
                            }
                            if (tabs.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Close Other Tabs") },
                                    onClick = {
                                        topMenuExpanded = false
                                        onCloseOtherTabs(activeTabId)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Close All Tabs") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                                },
                                onClick = {
                                    topMenuExpanded = false
                                    onCloseAllTabs()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNewTab(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("new_tab_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Tab")
                    }
                    OutlinedButton(
                        onClick = { onNewTab(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("new_incognito_tab_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Incognito")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filter == TabSessionFilter.ALL,
                            onClick = { filter = TabSessionFilter.ALL },
                            label = { Text("All (${tabs.size})") },
                            modifier = Modifier.testTag("filter_tabs_all")
                        )
                        FilterChip(
                            selected = filter == TabSessionFilter.STANDARD,
                            onClick = { filter = TabSessionFilter.STANDARD },
                            label = { Text("Standard ($standardCount)") },
                            modifier = Modifier.testTag("filter_tabs_standard")
                        )
                        FilterChip(
                            selected = filter == TabSessionFilter.INCOGNITO,
                            onClick = { filter = TabSessionFilter.INCOGNITO },
                            label = { Text("Incognito ($incognitoCount)") },
                            modifier = Modifier.testTag("filter_tabs_incognito")
                        )
                    }
                }

                if (displayedTabs.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "No sessions match this filter. Tap New Tab or Incognito below to launch a session.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(displayedTabs, key = { it.id }) { tab ->
                        val isSelected = tab.id == activeTabId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectTab(tab.id) }
                                .testTag("tab_card_${tab.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected && tab.isIncognito -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = when {
                                    isSelected && tab.isIncognito -> MaterialTheme.colorScheme.tertiary
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (tab.isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Language,
                                        contentDescription = null,
                                        tint = if (tab.isIncognito) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = tab.title.ifBlank { "New Tab" },
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = tab.url.ifBlank { "Speed Dial Start Page" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (tab.detectedMedia.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "${tab.detectedMedia.size} media",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { onDuplicateTab(tab.id) },
                                        modifier = Modifier.testTag("duplicate_tab_${tab.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Duplicate session",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCloseTab(tab.id) },
                                        modifier = Modifier.testTag("close_tab_${tab.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close tab"
                                        )
                                    }
                                }

                                if (tab.isLoading) {
                                    LinearProgressIndicator(
                                        progress = { tab.progress.coerceIn(5, 100) / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
