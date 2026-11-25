package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.BrowserPresets
import com.example.browser.SearchEngine
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.preferences.BrowserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAndSettingsScreen(
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    settings: BrowserSettings,
    onNavigate: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onClearCacheAndCookies: () -> Unit,
    onSelectSearchEngine: (String) -> Unit,
    onToggleJavascript: (Boolean) -> Unit,
    onToggleDarkWeb: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleProxyAutoStart: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var historyQuery by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = { Text("Library & Settings", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("library_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { onNavigate(BrowserPresets.INTERNAL_HTML5_TEST_URL) },
                            modifier = Modifier.testTag("open_html5_lab_action")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Science,
                                contentDescription = "HTML5 Test Lab"
                            )
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Bookmarks") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("History") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (bookmarks.isEmpty()) {
                            item {
                                Text(
                                    text = "No bookmarks saved yet. Tap the star icon in the address bar on any page to save it here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(bookmarks, key = { it.id }) { bm ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate(bm.url) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bookmark,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bm.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(bm.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete bookmark")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val filtered = remember(history, historyQuery) {
                        if (historyQuery.isBlank()) history
                        else history.filter {
                            it.title.contains(historyQuery, ignoreCase = true) ||
                                it.url.contains(historyQuery, ignoreCase = true)
                        }
                    }
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = historyQuery,
                                    onValueChange = { historyQuery = it },
                                    label = { Text("Search history") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onClearHistory) {
                                    Text("Clear All")
                                }
                            }
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    text = "No browsing history recorded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate(item.url) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(item.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { onDeleteHistoryItem(item.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete history entry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Search Engine", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SearchEngine.entries.take(2).forEach { engine ->
                                    FilterChip(
                                        selected = settings.searchEngineId == engine.id,
                                        onClick = { onSelectSearchEngine(engine.id) },
                                        label = { Text(engine.displayName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SearchEngine.entries.drop(2).forEach { engine ->
                                    FilterChip(
                                        selected = settings.searchEngineId == engine.id,
                                        onClick = { onSelectSearchEngine(engine.id) },
                                        label = { Text(engine.displayName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SettingSwitchRow(
                                        title = "Dark App Theme",
                                        subtitle = "Use RelayWeb Obsidian & Cyan dark palette",
                                        checked = settings.darkThemePreferred,
                                        onCheckedChange = onToggleDarkTheme
                                    )
                                    SettingSwitchRow(
                                        title = "HTML5 JavaScript Engine",
                                        subtitle = "Required for HTML5 players, WebGL, and interactive apps",
                                        checked = settings.javascriptEnabled,
                                        onCheckedChange = onToggleJavascript
                                    )
                                    SettingSwitchRow(
                                        title = "Dark Web Page Rendering",
                                        subtitle = "Apply dark theme darkening to websites when supported",
                                        checked = settings.darkWebContent,
                                        onCheckedChange = onToggleDarkWeb
                                    )
                                    SettingSwitchRow(
                                        title = "Auto-Start Proxy Relay",
                                        subtitle = "Automatically restore your last proxy mode on launch",
                                        checked = settings.proxyAutoStart,
                                        onCheckedChange = onToggleProxyAutoStart
                                    )
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = onClearCacheAndCookies,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("clear_cache_cookies_button")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear Web Cache, Cookies & DOM Storage")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
