package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.proxy.ProxyMode
import com.example.ui.BrowserOverlay
import com.example.ui.BrowserViewModel
import com.example.ui.components.BottomNavToolbar
import com.example.ui.components.HomeSpeedDial
import com.example.ui.components.OmniboxBar
import com.example.ui.screens.FullscreenVideoOverlay
import com.example.ui.screens.LibraryAndSettingsScreen
import com.example.ui.screens.MediaPlayerScreen
import com.example.ui.screens.ProxyHubScreen
import com.example.ui.screens.TabsSwitcherScreen
import com.example.ui.theme.RelayWebTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            intent?.dataString?.let { incomingUrl ->
                viewModel.handleIncomingExternalUrl(incomingUrl)
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            RelayWebTheme(darkTheme = settings.darkThemePreferred) {
                RelayWebApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let { incomingUrl ->
            viewModel.handleIncomingExternalUrl(incomingUrl)
        }
    }
}

@Composable
fun RelayWebApp(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeOverlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val proxyProfiles by viewModel.proxyProfiles.collectAsStateWithLifecycle()
    val proxyTelemetry by viewModel.proxyTelemetry.collectAsStateWithLifecycle()
    val proxyDiagnostics by viewModel.proxyDiagnostics.collectAsStateWithLifecycle()
    val mediaPlaybackState by viewModel.mediaPlaybackState.collectAsStateWithLifecycle()
    val fullscreenView by viewModel.fullscreenVideoView.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val activeTab = remember(tabs, activeTabId) {
        tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    }
    val isBookmarked = remember(bookmarks, activeTab.url) {
        bookmarks.any { it.url == activeTab.url }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun ensureNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    BackHandler(enabled = !activeTab.isHomePage || activeOverlay != BrowserOverlay.NONE || fullscreenView != null) {
        viewModel.goBackInActiveTab()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OmniboxBar(
                tab = activeTab,
                tabs = tabs,
                proxyTelemetry = proxyTelemetry,
                isBookmarked = isBookmarked,
                desktopMode = settings.desktopMode,
                onNavigate = viewModel::navigateToInput,
                onReloadOrStop = viewModel::reloadOrStopActiveTab,
                onGoHome = viewModel::goHomeInActiveTab,
                onToggleBookmark = viewModel::toggleCurrentBookmark,
                onToggleDesktopMode = viewModel::toggleDesktopMode,
                onToggleIncognito = viewModel::toggleIncognitoMode,
                onSelectTab = viewModel::selectTab,
                onCloseTab = viewModel::closeTab,
                onNewTab = { viewModel.createNewTab(isIncognito = false) },
                onOpenProxyHub = { viewModel.openOverlay(BrowserOverlay.PROXY_HUB) },
                onOpenMediaItem = viewModel::openMediaInPlayer,
                onOpenMediaPlayer = { viewModel.openOverlay(BrowserOverlay.MEDIA_PLAYER) }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeTab.isHomePage) {
                    HomeSpeedDial(
                        isIncognito = activeTab.isIncognito,
                        proxyTelemetry = proxyTelemetry,
                        bookmarks = bookmarks,
                        onNavigate = viewModel::navigateToInput,
                        onOpenProxyHub = { viewModel.openOverlay(BrowserOverlay.PROXY_HUB) },
                        onOpenMediaPlayer = { viewModel.openOverlay(BrowserOverlay.MEDIA_PLAYER) },
                        onToggleIncognito = viewModel::toggleIncognitoMode
                    )
                } else {
                    val webView = remember(activeTab.id, settings.desktopMode, settings.javascriptEnabled, settings.darkWebContent, settings.trackerShieldEnabled) {
                        viewModel.getOrCreateWebView(activeTab)
                    }
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                (webView.parent as? ViewGroup)?.removeView(webView)
                                addView(
                                    webView,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        update = { container ->
                            val currentChild = if (container.childCount > 0) container.getChildAt(0) else null
                            if (currentChild !== webView) {
                                container.removeAllViews()
                                (webView.parent as? ViewGroup)?.removeView(webView)
                                container.addView(
                                    webView,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (activeTab.lastError != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Connection note: ${activeTab.lastError}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            BottomNavToolbar(
                canGoBack = activeTab.canGoBack || !activeTab.isHomePage,
                canGoForward = activeTab.canGoForward,
                tabCount = tabs.size,
                detectedMediaCount = activeTab.detectedMedia.size,
                isMediaPlaying = mediaPlaybackState.isPlaying,
                proxyTelemetry = proxyTelemetry,
                onBack = { viewModel.goBackInActiveTab() },
                onForward = viewModel::goForwardInActiveTab,
                onOpenProxyHub = { viewModel.openOverlay(BrowserOverlay.PROXY_HUB) },
                onOpenMediaPlayer = { viewModel.openOverlay(BrowserOverlay.MEDIA_PLAYER) },
                onOpenTabs = { viewModel.openOverlay(BrowserOverlay.TABS) },
                onOpenMenu = { viewModel.openOverlay(BrowserOverlay.LIBRARY_SETTINGS) }
            )
        }

        // Secondary Screen Overlays
        AnimatedVisibility(
            visible = activeOverlay == BrowserOverlay.PROXY_HUB,
            enter = slideInVertically { it / 6 } + fadeIn(),
            exit = slideOutVertically { it / 6 } + fadeOut()
        ) {
            ProxyHubScreen(
                telemetry = proxyTelemetry,
                diagnostics = proxyDiagnostics,
                settings = settings,
                profiles = proxyProfiles,
                onBack = viewModel::closeOverlay,
                onSelectMode = { mode ->
                    if (mode != ProxyMode.DIRECT) ensureNotificationPermissionIfNeeded()
                    viewModel.selectProxyMode(mode)
                },
                onActivateProfile = { profile ->
                    ensureNotificationPermissionIfNeeded()
                    viewModel.activateProxyProfile(profile)
                },
                onSaveProfile = { name, mode, host, port, user, pass, blockTrackers, stripHeaders ->
                    ensureNotificationPermissionIfNeeded()
                    viewModel.saveAndActivateProxyProfile(
                        name = name,
                        mode = mode,
                        host = host,
                        port = port,
                        username = user,
                        password = pass,
                        blockTrackers = blockTrackers,
                        stripHeaders = stripHeaders
                    )
                },
                onDeleteProfile = viewModel::deleteProxyProfile,
                onRunDiagnostics = viewModel::runProxyDiagnostics,
                onClearLogs = viewModel::clearProxyLogs,
                onToggleTrackerShield = viewModel::setTrackerShield,
                onToggleStripHeaders = viewModel::setStripTrackingHeaders
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == BrowserOverlay.MEDIA_PLAYER,
            enter = slideInVertically { it / 6 } + fadeIn(),
            exit = slideOutVertically { it / 6 } + fadeOut()
        ) {
            MediaPlayerScreen(
                playbackState = mediaPlaybackState,
                mediaPlayerManager = viewModel.mediaPlayerManager,
                detectedPageMedia = activeTab.detectedMedia,
                proxyTelemetry = proxyTelemetry,
                onRescanPageMedia = viewModel::scanActiveTabMedia,
                onBack = viewModel::closeOverlay
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == BrowserOverlay.TABS,
            enter = slideInVertically { it / 6 } + fadeIn(),
            exit = slideOutVertically { it / 6 } + fadeOut()
        ) {
            TabsSwitcherScreen(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelectTab = viewModel::selectTab,
                onCloseTab = viewModel::closeTab,
                onDuplicateTab = viewModel::duplicateTab,
                onCloseOtherTabs = viewModel::closeOtherTabs,
                onCloseAllIncognito = viewModel::closeAllIncognitoTabs,
                onCloseAllTabs = viewModel::closeAllTabs,
                onNewTab = { incognito -> viewModel.createNewTab(isIncognito = incognito) },
                onBack = viewModel::closeOverlay
            )
        }

        AnimatedVisibility(
            visible = activeOverlay == BrowserOverlay.LIBRARY_SETTINGS,
            enter = slideInVertically { it / 6 } + fadeIn(),
            exit = slideOutVertically { it / 6 } + fadeOut()
        ) {
            LibraryAndSettingsScreen(
                bookmarks = bookmarks,
                history = history,
                settings = settings,
                onNavigate = viewModel::navigateToInput,
                onDeleteBookmark = viewModel::deleteBookmark,
                onDeleteHistoryItem = viewModel::deleteHistoryItem,
                onClearHistory = viewModel::clearAllHistory,
                onClearCacheAndCookies = viewModel::clearWebCacheAndCookies,
                onSelectSearchEngine = viewModel::setSearchEngine,
                onToggleJavascript = viewModel::setJavascriptEnabled,
                onToggleDarkWeb = viewModel::setDarkWebContent,
                onToggleDarkTheme = viewModel::setDarkThemePreferred,
                onToggleProxyAutoStart = viewModel::setProxyAutoStart,
                onBack = viewModel::closeOverlay
            )
        }

        // Fullscreen HTML5 <video> CustomView Overlay
        val customVideoView = fullscreenView
        if (customVideoView != null) {
            FullscreenVideoOverlay(
                customView = customVideoView,
                onExitFullscreen = viewModel::exitFullscreenVideo
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}
