package com.example.ui

import android.app.Application
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.browser.BrowserPresets
import com.example.browser.BrowserTab
import com.example.browser.DetectedMediaItem
import com.example.browser.Html5LocalPages
import com.example.browser.Html5WebViewFactory
import com.example.browser.SearchEngine
import com.example.browser.WebViewCallbacks
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserDatabase
import com.example.data.local.HistoryEntity
import com.example.data.local.ProxyProfileEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.BrowserSettings
import com.example.data.repository.BrowserRepository
import com.example.media.MediaPlaybackState
import com.example.media.MediaPlayerManager
import com.example.proxy.ProxyConfig
import com.example.proxy.ProxyDiagnosticState
import com.example.proxy.ProxyEngineManager
import com.example.proxy.ProxyMode
import com.example.proxy.ProxyTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

enum class BrowserOverlay {
    NONE,
    TABS,
    PROXY_HUB,
    MEDIA_PLAYER,
    LIBRARY_SETTINGS
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BrowserDatabase.getInstance(application)
    private val preferences = BrowserPreferences(application)
    private val repository = BrowserRepository(
        bookmarkDao = database.bookmarkDao(),
        historyDao = database.historyDao(),
        proxyProfileDao = database.proxyProfileDao(),
        preferences = preferences
    )

    val mediaPlayerManager = MediaPlayerManager(application)

    val settings: StateFlow<BrowserSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BrowserSettings()
    )

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history: StateFlow<List<HistoryEntity>> = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val proxyProfiles: StateFlow<List<ProxyProfileEntity>> = repository.proxyProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val proxyTelemetry: StateFlow<ProxyTelemetry> = ProxyEngineManager.telemetry
    val proxyDiagnostics: StateFlow<ProxyDiagnosticState> = ProxyEngineManager.diagnosticState
    val mediaPlaybackState: StateFlow<MediaPlaybackState> = mediaPlayerManager.state

    private val initialTab = BrowserTab(id = UUID.randomUUID().toString())
    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _activeOverlay = MutableStateFlow(BrowserOverlay.NONE)
    val activeOverlay: StateFlow<BrowserOverlay> = _activeOverlay.asStateFlow()

    private val _fullscreenVideoView = MutableStateFlow<View?>(null)
    val fullscreenVideoView: StateFlow<View?> = _fullscreenVideoView.asStateFlow()
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val webViewPool = mutableMapOf<String, WebView>()

    private val webViewCallbacks = object : WebViewCallbacks {
        override fun onPageStarted(tabId: String, url: String) {
            updateTab(tabId) {
                it.copy(
                    url = url,
                    isLoading = true,
                    progress = 10,
                    lastError = null,
                    isSecure = url.startsWith("https://") || url == BrowserPresets.INTERNAL_HTML5_TEST_URL
                )
            }
        }

        override fun onPageFinished(
            tabId: String,
            url: String,
            title: String,
            canBack: Boolean,
            canForward: Boolean
        ) {
            val tab = _tabs.value.firstOrNull { it.id == tabId }
            val resolvedTitle = title.ifBlank { url.ifBlank { "Page" } }
            updateTab(tabId) {
                it.copy(
                    url = if (url == "https://relayweb.local/") BrowserPresets.INTERNAL_HTML5_TEST_URL else url,
                    title = resolvedTitle,
                    isLoading = false,
                    progress = 100,
                    canGoBack = canBack,
                    canGoForward = canForward,
                    isSecure = url.startsWith("https://") || url == BrowserPresets.INTERNAL_HTML5_TEST_URL
                )
            }
            if (tab != null && !tab.isIncognito && url.startsWith("http") && url != "https://relayweb.local/") {
                viewModelScope.launch {
                    repository.addHistory(resolvedTitle, url)
                }
            }
        }

        override fun onProgressChanged(tabId: String, progress: Int) {
            updateTab(tabId) {
                it.copy(progress = progress, isLoading = progress in 1..99)
            }
        }

        override fun onTitleChanged(tabId: String, title: String) {
            updateTab(tabId) { it.copy(title = title) }
        }

        override fun onMediaDetected(tabId: String, items: List<DetectedMediaItem>) {
            updateTab(tabId) { tab ->
                val merged = (tab.detectedMedia + items).distinctBy { it.url }
                tab.copy(detectedMedia = merged)
            }
        }

        override fun onOpenNewTab(url: String) {
            val activeIsIncognito = _tabs.value.firstOrNull { it.id == _activeTabId.value }?.isIncognito == true
            createNewTab(url = url, isIncognito = activeIsIncognito)
        }

        override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
            customViewCallback = callback
            _fullscreenVideoView.value = view
        }

        override fun onHideCustomView() {
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
            _fullscreenVideoView.value = null
        }

        override fun onPageError(tabId: String, description: String) {
            updateTab(tabId) {
                it.copy(isLoading = false, lastError = description)
            }
        }
    }

    init {
        viewModelScope.launch {
            val initialSettings = repository.settings.first()
            if (initialSettings.proxyAutoStart && initialSettings.activeProxyMode != ProxyMode.DIRECT.name) {
                applyProxyFromSettings(initialSettings)
            } else {
                ProxyEngineManager.applyConfig(
                    getApplication(),
                    ProxyConfig(mode = ProxyMode.DIRECT)
                )
            }
        }
    }

    fun getOrCreateWebView(tab: BrowserTab): WebView {
        val currentSettings = settings.value
        val existing = webViewPool[tab.id]
        if (existing != null) {
            val defaultUa = existing.tag as? String ?: ""
            Html5WebViewFactory.applySettings(
                webView = existing,
                defaultUserAgent = defaultUa,
                isIncognito = tab.isIncognito,
                javascriptEnabled = currentSettings.javascriptEnabled,
                desktopMode = currentSettings.desktopMode,
                darkWebContent = currentSettings.darkWebContent,
                trackerShieldEnabled = currentSettings.trackerShieldEnabled
            )
            return existing
        }

        val created = Html5WebViewFactory.createWebView(
            context = getApplication(),
            tabId = tab.id,
            isIncognito = tab.isIncognito,
            javascriptEnabled = currentSettings.javascriptEnabled,
            desktopMode = currentSettings.desktopMode,
            trackerShieldEnabled = currentSettings.trackerShieldEnabled,
            darkWebContent = currentSettings.darkWebContent,
            callbacks = webViewCallbacks
        )
        webViewPool[tab.id] = created
        if (tab.url.isNotBlank()) {
            loadUrlIntoWebView(created, tab.url)
        }
        return created
    }

    fun openOverlay(overlay: BrowserOverlay) {
        _activeOverlay.value = overlay
    }

    fun closeOverlay() {
        _activeOverlay.value = BrowserOverlay.NONE
    }

    fun navigateToInput(rawInput: String) {
        val resolvedUrl = resolveInputToUrl(rawInput, settings.value.searchEngineId)
        val tabId = _activeTabId.value
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        updateTab(tabId) {
            it.copy(
                url = resolvedUrl,
                title = resolvedUrl,
                detectedMedia = emptyList(),
                lastError = null
            )
        }
        val webView = getOrCreateWebView(tab.copy(url = resolvedUrl))
        loadUrlIntoWebView(webView, resolvedUrl)
        _activeOverlay.value = BrowserOverlay.NONE
    }

    fun handleIncomingExternalUrl(url: String) {
        if (url.isBlank()) return
        val current = _tabs.value.firstOrNull { it.id == _activeTabId.value }
        if (current != null && current.isHomePage) {
            navigateToInput(url)
        } else {
            createNewTab(url = url)
        }
    }

    private fun loadUrlIntoWebView(webView: WebView, url: String) {
        if (url == BrowserPresets.INTERNAL_HTML5_TEST_URL) {
            webView.loadDataWithBaseURL(
                "https://relayweb.local/",
                Html5LocalPages.buildHtml5TestPage(),
                "text/html",
                "UTF-8",
                BrowserPresets.INTERNAL_HTML5_TEST_URL
            )
        } else {
            webView.loadUrl(url)
        }
    }

    fun reloadOrStopActiveTab() {
        val tab = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: return
        if (tab.isHomePage) return
        val wv = webViewPool[tab.id] ?: return
        if (tab.isLoading) {
            wv.stopLoading()
            updateTab(tab.id) { it.copy(isLoading = false) }
        } else {
            if (tab.url == BrowserPresets.INTERNAL_HTML5_TEST_URL) {
                loadUrlIntoWebView(wv, tab.url)
            } else {
                wv.reload()
            }
        }
    }

    fun goBackInActiveTab(): Boolean {
        if (_fullscreenVideoView.value != null) {
            exitFullscreenVideo()
            return true
        }
        if (_activeOverlay.value != BrowserOverlay.NONE) {
            _activeOverlay.value = BrowserOverlay.NONE
            return true
        }
        val tab = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: return false
        val wv = webViewPool[tab.id]
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
            updateTab(tab.id) {
                it.copy(canGoBack = wv.canGoBack(), canGoForward = wv.canGoForward())
            }
            return true
        }
        if (!tab.isHomePage) {
            // Return to Speed Dial home
            updateTab(tab.id) {
                it.copy(url = "", title = "New Tab", detectedMedia = emptyList(), isLoading = false, canGoBack = false, canGoForward = false)
            }
            return true
        }
        return false
    }

    fun goForwardInActiveTab() {
        val tab = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: return
        val wv = webViewPool[tab.id] ?: return
        if (wv.canGoForward()) {
            wv.goForward()
            updateTab(tab.id) {
                it.copy(canGoBack = wv.canGoBack(), canGoForward = wv.canGoForward())
            }
        }
    }

    fun goHomeInActiveTab() {
        val tabId = _activeTabId.value
        updateTab(tabId) {
            it.copy(
                url = "",
                title = "New Tab",
                isLoading = false,
                progress = 0,
                detectedMedia = emptyList(),
                lastError = null
            )
        }
    }

    fun toggleIncognitoMode() {
        val current = _tabs.value.firstOrNull { it.id == _activeTabId.value }
        if (current != null && current.isIncognito) {
            // Exit incognito: wipe incognito tab data and switch to or create normal tab
            val normalTab = _tabs.value.firstOrNull { !it.isIncognito }
            closeTab(current.id)
            if (normalTab != null) {
                _activeTabId.value = normalTab.id
                syncCookiePolicyForTab(normalTab)
            }
            showBanner("Exited Incognito • Session cache & cookies wiped")
        } else {
            createNewTab(isIncognito = true)
            showBanner("Incognito Mode active • History, cookies & cache disabled")
        }
    }

    fun createNewTab(url: String = "", isIncognito: Boolean = false) {
        val newTab = BrowserTab(
            id = UUID.randomUUID().toString(),
            title = if (isIncognito) "Incognito Tab" else "New Tab",
            url = url,
            isIncognito = isIncognito
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        _activeOverlay.value = BrowserOverlay.NONE
        syncCookiePolicyForTab(newTab)
        if (url.isNotBlank()) {
            val wv = getOrCreateWebView(newTab)
            loadUrlIntoWebView(wv, url)
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        _activeOverlay.value = BrowserOverlay.NONE
        _tabs.value.firstOrNull { it.id == tabId }?.let { syncCookiePolicyForTab(it) }
    }

    fun closeTab(tabId: String) {
        val closingTab = _tabs.value.firstOrNull { it.id == tabId }
        val currentList = _tabs.value
        webViewPool.remove(tabId)?.apply {
            if (closingTab?.isIncognito == true) {
                Html5WebViewFactory.wipeIncognitoWebView(this)
            } else {
                stopLoading()
            }
            destroy()
        }
        if (currentList.size <= 1) {
            val replacement = BrowserTab(id = UUID.randomUUID().toString())
            _tabs.value = listOf(replacement)
            _activeTabId.value = replacement.id
            syncCookiePolicyForTab(replacement)
            return
        }
        val index = currentList.indexOfFirst { it.id == tabId }
        val updated = currentList.filterNot { it.id == tabId }
        _tabs.value = updated
        if (_activeTabId.value == tabId) {
            val nextIdx = (index - 1).coerceIn(0, updated.lastIndex)
            val nextTab = updated[nextIdx]
            _activeTabId.value = nextTab.id
            syncCookiePolicyForTab(nextTab)
        }
    }

    fun duplicateTab(tabId: String) {
        val source = _tabs.value.firstOrNull { it.id == tabId } ?: return
        createNewTab(url = source.url, isIncognito = source.isIncognito)
        showBanner("Opened duplicate session")
    }

    fun closeOtherTabs(keepTabId: String) {
        val keep = _tabs.value.firstOrNull { it.id == keepTabId } ?: return
        _tabs.value.filter { it.id != keepTabId }.forEach { tab ->
            webViewPool.remove(tab.id)?.apply {
                if (tab.isIncognito) Html5WebViewFactory.wipeIncognitoWebView(this) else stopLoading()
                destroy()
            }
        }
        _tabs.value = listOf(keep)
        _activeTabId.value = keep.id
        syncCookiePolicyForTab(keep)
        showBanner("Closed other tabs")
    }

    fun closeAllIncognitoTabs() {
        val incognitoTabs = _tabs.value.filter { it.isIncognito }
        if (incognitoTabs.isEmpty()) return
        incognitoTabs.forEach { tab ->
            webViewPool.remove(tab.id)?.apply {
                Html5WebViewFactory.wipeIncognitoWebView(this)
                destroy()
            }
        }
        val remaining = _tabs.value.filterNot { it.isIncognito }
        if (remaining.isEmpty()) {
            val fresh = BrowserTab(id = UUID.randomUUID().toString())
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            syncCookiePolicyForTab(fresh)
        } else {
            _tabs.value = remaining
            if (remaining.none { it.id == _activeTabId.value }) {
                val target = remaining.first()
                _activeTabId.value = target.id
                syncCookiePolicyForTab(target)
            }
        }
        showBanner("Closed all Incognito tabs")
    }

    fun closeAllTabs() {
        _tabs.value.forEach { tab ->
            webViewPool.remove(tab.id)?.apply {
                if (tab.isIncognito) Html5WebViewFactory.wipeIncognitoWebView(this) else stopLoading()
                destroy()
            }
        }
        val fresh = BrowserTab(id = UUID.randomUUID().toString())
        _tabs.value = listOf(fresh)
        _activeTabId.value = fresh.id
        _activeOverlay.value = BrowserOverlay.NONE
        syncCookiePolicyForTab(fresh)
        showBanner("All tabs closed")
    }

    private fun syncCookiePolicyForTab(tab: BrowserTab) {
        runCatching {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(!tab.isIncognito)
            webViewPool[tab.id]?.let { wv ->
                cookieManager.setAcceptThirdPartyCookies(wv, !tab.isIncognito)
            }
        }
    }

    fun exitFullscreenVideo() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        _fullscreenVideoView.value = null
    }

    fun toggleCurrentBookmark() {
        val tab = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: return
        if (tab.isHomePage) return
        val alreadyBookmarked = bookmarks.value.any { it.url == tab.url }
        viewModelScope.launch {
            repository.toggleBookmark(tab.title, tab.url, alreadyBookmarked)
            showBanner(if (alreadyBookmarked) "Removed bookmark" else "Bookmarked ${tab.title}")
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            showBanner("Browsing history cleared")
        }
    }

    fun clearWebCacheAndCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        webViewPool.values.forEach { it.clearCache(true) }
        showBanner("Cache, cookies, and web storage cleared")
    }

    fun toggleDesktopMode() {
        val next = !settings.value.desktopMode
        viewModelScope.launch {
            preferences.setDesktopMode(next)
            val activeTab = _tabs.value.firstOrNull { it.id == _activeTabId.value }
            if (activeTab != null && !activeTab.isHomePage) {
                val wv = getOrCreateWebView(activeTab)
                wv.reload()
            }
            showBanner(if (next) "Desktop site mode enabled" else "Mobile site mode enabled")
        }
    }

    fun setSearchEngine(engineId: String) {
        viewModelScope.launch { preferences.updateSearchEngine(engineId) }
    }

    fun setJavascriptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setJavascriptEnabled(enabled)
            refreshActiveWebViewSettings()
        }
    }

    fun setTrackerShield(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setTrackerShield(enabled)
            applyProxyFromSettings(settings.value.copy(trackerShieldEnabled = enabled))
            refreshActiveWebViewSettings()
        }
    }

    fun setStripTrackingHeaders(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setStripTrackingHeaders(enabled)
            applyProxyFromSettings(settings.value.copy(stripTrackingHeaders = enabled))
        }
    }

    fun setDarkWebContent(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDarkWebContent(enabled)
            refreshActiveWebViewSettings()
        }
    }

    fun setDarkThemePreferred(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkThemePreferred(enabled) }
    }

    fun setProxyAutoStart(enabled: Boolean) {
        viewModelScope.launch { preferences.setProxyAutoStart(enabled) }
    }

    fun selectProxyMode(mode: ProxyMode) {
        val s = settings.value
        if (mode.let { it == ProxyMode.UPSTREAM_HTTP || it == ProxyMode.UPSTREAM_SOCKS5 } &&
            s.activeProxyHost.isBlank()
        ) {
            showBanner("Select or add an upstream proxy server first")
            return
        }
        viewModelScope.launch {
            preferences.saveActiveProxyConfig(
                mode = mode.name,
                host = s.activeProxyHost,
                port = s.activeProxyPort,
                user = s.activeProxyUser,
                pass = s.activeProxyPass,
                profileId = s.activeProfileId
            )
            applyProxyFromSettings(s.copy(activeProxyMode = mode.name))
            mediaPlayerManager.rebuildForCurrentProxy()
        }
    }

    fun activateProxyProfile(profile: ProxyProfileEntity) {
        viewModelScope.launch {
            preferences.saveActiveProxyConfig(
                mode = profile.modeName,
                host = profile.host,
                port = profile.port,
                user = profile.username,
                pass = profile.password,
                profileId = profile.id
            )
            preferences.setTrackerShield(profile.blockTrackers)
            preferences.setStripTrackingHeaders(profile.stripTrackingHeaders)

            val config = ProxyConfig(
                mode = ProxyMode.fromString(profile.modeName),
                upstreamHost = profile.host,
                upstreamPort = profile.port,
                username = profile.username,
                password = profile.password,
                blockTrackers = profile.blockTrackers,
                stripTrackingHeaders = profile.stripTrackingHeaders,
                activeProfileName = profile.name
            )
            ProxyEngineManager.applyConfig(getApplication(), config)
            mediaPlayerManager.rebuildForCurrentProxy()
            showBanner("Activated ${profile.name}")
        }
    }

    fun saveAndActivateProxyProfile(
        id: Long = 0L,
        name: String,
        mode: ProxyMode,
        host: String,
        port: Int,
        username: String,
        password: String,
        blockTrackers: Boolean,
        stripHeaders: Boolean
    ) {
        viewModelScope.launch {
            val entity = ProxyProfileEntity(
                id = id,
                name = name.ifBlank { "${mode.label} ($host:$port)" },
                modeName = mode.name,
                host = host.trim(),
                port = port.coerceIn(1, 65535),
                username = username.trim(),
                password = password,
                blockTrackers = blockTrackers,
                stripTrackingHeaders = stripHeaders
            )
            val savedId = repository.saveProxyProfile(entity)
            activateProxyProfile(entity.copy(id = savedId))
        }
    }

    fun deleteProxyProfile(id: Long) {
        viewModelScope.launch { repository.deleteProxyProfile(id) }
    }

    fun runProxyDiagnostics() {
        viewModelScope.launch {
            ProxyEngineManager.runProxyDiagnostics()
        }
    }

    fun clearProxyLogs() {
        ProxyEngineManager.clearLogs()
    }

    fun scanActiveTabMedia() {
        val tab = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: return
        val wv = webViewPool[tab.id] ?: return
        Html5WebViewFactory.triggerMediaScan(wv)
    }

    fun openMediaInPlayer(item: DetectedMediaItem) {
        mediaPlayerManager.playItem(item)
        _activeOverlay.value = BrowserOverlay.MEDIA_PLAYER
    }

    fun playCustomMediaUrl(url: String, title: String = "Stream") {
        mediaPlayerManager.playUrl(url, title)
    }

    private fun applyProxyFromSettings(s: BrowserSettings) {
        val mode = ProxyMode.fromString(s.activeProxyMode)
        val effectiveMode = if ((mode == ProxyMode.UPSTREAM_HTTP || mode == ProxyMode.UPSTREAM_SOCKS5) &&
            s.activeProxyHost.isBlank()
        ) {
            ProxyMode.LOCAL_RELAY
        } else {
            mode
        }
        val profileName = when (effectiveMode) {
            ProxyMode.DIRECT -> "Direct"
            ProxyMode.LOCAL_RELAY -> "Built-in Privacy Relay"
            ProxyMode.UPSTREAM_HTTP -> "HTTP ${s.activeProxyHost}:${s.activeProxyPort}"
            ProxyMode.UPSTREAM_SOCKS5 -> "SOCKS5 ${s.activeProxyHost}:${s.activeProxyPort}"
        }
        ProxyEngineManager.applyConfig(
            getApplication(),
            ProxyConfig(
                mode = effectiveMode,
                upstreamHost = s.activeProxyHost,
                upstreamPort = s.activeProxyPort,
                username = s.activeProxyUser,
                password = s.activeProxyPass,
                blockTrackers = s.trackerShieldEnabled,
                stripTrackingHeaders = s.stripTrackingHeaders,
                activeProfileName = profileName
            )
        )
    }

    private fun refreshActiveWebViewSettings() {
        val s = settings.value
        _tabs.value.forEach { tab ->
            val wv = webViewPool[tab.id] ?: return@forEach
            val defaultUa = wv.tag as? String ?: ""
            Html5WebViewFactory.applySettings(
                webView = wv,
                defaultUserAgent = defaultUa,
                isIncognito = tab.isIncognito,
                javascriptEnabled = s.javascriptEnabled,
                desktopMode = s.desktopMode,
                darkWebContent = s.darkWebContent,
                trackerShieldEnabled = s.trackerShieldEnabled
            )
        }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    private fun showBanner(msg: String) {
        _statusBanner.value = msg
    }

    private fun resolveInputToUrl(rawInput: String, searchEngineId: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.equals(BrowserPresets.INTERNAL_HTML5_TEST_URL, ignoreCase = true) ||
            trimmed.equals("html5-test", ignoreCase = true)
        ) {
            return BrowserPresets.INTERNAL_HTML5_TEST_URL
        }
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        val looksLikeIpOrLocal = trimmed.startsWith("localhost", ignoreCase = true) ||
            Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$").matches(trimmed)
        if (looksLikeIpOrLocal) {
            return "http://$trimmed"
        }
        val hasNoSpacesAndDot = !trimmed.contains(' ') && trimmed.contains('.')
        if (hasNoSpacesAndDot) {
            return "https://$trimmed"
        }
        val engine = SearchEngine.fromId(searchEngineId)
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        return engine.searchUrlTemplate.replace("%s", encoded)
    }

    override fun onCleared() {
        super.onCleared()
        webViewPool.values.forEach { wv ->
            wv.stopLoading()
            wv.destroy()
        }
        webViewPool.clear()
        mediaPlayerManager.release()
    }
}
