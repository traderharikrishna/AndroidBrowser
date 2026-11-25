package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.proxy.ProxyEngineManager
import com.example.proxy.TrackerBlocklist
import org.json.JSONArray
import java.io.ByteArrayInputStream

interface WebViewCallbacks {
    fun onPageStarted(tabId: String, url: String)
    fun onPageFinished(tabId: String, url: String, title: String, canBack: Boolean, canForward: Boolean)
    fun onProgressChanged(tabId: String, progress: Int)
    fun onTitleChanged(tabId: String, title: String)
    fun onMediaDetected(tabId: String, items: List<DetectedMediaItem>)
    fun onOpenNewTab(url: String)
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
    fun onPageError(tabId: String, description: String)
}

object Html5WebViewFactory {

    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private const val MEDIA_SCANNER_JS = """
        (function() {
          try {
            var found = [];
            var seen = {};
            function pushItem(url, title, type, poster) {
              if (!url || url.indexOf('blob:') === 0 || url.indexOf('data:') === 0) return;
              if (seen[url]) return;
              seen[url] = true;
              found.push({
                url: url,
                title: title || document.title || 'HTML5 Media Stream',
                type: type,
                poster: poster || ''
              });
            }
            document.querySelectorAll('video').forEach(function(v, idx) {
              var src = v.currentSrc || v.src;
              if (!src) {
                var s = v.querySelector('source');
                if (s) src = s.src;
              }
              pushItem(src, v.getAttribute('title') || (document.title + ' • Video ' + (idx + 1)), 'VIDEO', v.poster);
            });
            document.querySelectorAll('audio').forEach(function(a, idx) {
              var src = a.currentSrc || a.src;
              if (!src) {
                var s = a.querySelector('source');
                if (s) src = s.src;
              }
              pushItem(src, a.getAttribute('title') || (document.title + ' • Audio ' + (idx + 1)), 'AUDIO', '');
            });
            document.querySelectorAll('a[href]').forEach(function(link) {
              var href = link.href || '';
              var lower = href.toLowerCase().split('?')[0];
              if (lower.endsWith('.mp4') || lower.endsWith('.webm') || lower.endsWith('.m3u8') || lower.endsWith('.mpd')) {
                pushItem(href, (link.textContent || '').trim() || 'Video Link', 'VIDEO', '');
              } else if (lower.endsWith('.mp3') || lower.endsWith('.aac') || lower.endsWith('.ogg') || lower.endsWith('.flac') || lower.endsWith('.wav')) {
                pushItem(href, (link.textContent || '').trim() || 'Audio Link', 'AUDIO', '');
              }
            });
            if (window.RelayMediaBridge) {
              window.RelayMediaBridge.onMediaFound(JSON.stringify(found));
            }
          } catch (e) {}
        })();
    """

    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        context: Context,
        tabId: String,
        isIncognito: Boolean,
        javascriptEnabled: Boolean,
        desktopMode: Boolean,
        trackerShieldEnabled: Boolean,
        darkWebContent: Boolean,
        callbacks: WebViewCallbacks
    ): WebView {
        val webView = WebView(context).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val defaultUa = WebSettings.getDefaultUserAgent(context)
        webView.tag = defaultUa

        applySettings(
            webView = webView,
            defaultUserAgent = defaultUa,
            isIncognito = isIncognito,
            javascriptEnabled = javascriptEnabled,
            desktopMode = desktopMode,
            darkWebContent = darkWebContent
        )

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(!isIncognito)
        cookieManager.setAcceptThirdPartyCookies(webView, !isIncognito)
        if (isIncognito) {
            cookieManager.removeSessionCookies(null)
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearHistory()
        }

        webView.addJavascriptInterface(
            MediaJsBridge(tabId, webView, callbacks),
            "RelayMediaBridge"
        )

        var currentTrackerShield = trackerShieldEnabled
        webView.setTag(com.example.R.id.webview_shield_tag, currentTrackerShield)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                callbacks.onProgressChanged(tabId, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    callbacks.onTitleChanged(tabId, title)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    callbacks.onShowCustomView(view, callback)
                }
            }

            override fun onHideCustomView() {
                callbacks.onHideCustomView()
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val hitTest = view?.hitTestResult
                val extra = hitTest?.extra
                if (!extra.isNullOrBlank() && (extra.startsWith("http://") || extra.startsWith("https://"))) {
                    callbacks.onOpenNewTab(extra)
                    return true
                }
                return false
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString().orEmpty()
                return handleCustomUrl(view, url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleCustomUrl(view, url.orEmpty())
            }

            private fun handleCustomUrl(view: WebView?, url: String): Boolean {
                if (url == BrowserPresets.INTERNAL_HTML5_TEST_URL) {
                    view?.loadDataWithBaseURL(
                        "https://relayweb.local/",
                        Html5LocalPages.buildHtml5TestPage(),
                        "text/html",
                        "UTF-8",
                        BrowserPresets.INTERNAL_HTML5_TEST_URL
                    )
                    return true
                }
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:")) {
                    detectDirectMediaUrl(tabId, url, callbacks)
                    return false
                }
                // Block non-HTTP intents like intent:// or market:// safely without crashing
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val shieldOn = (view?.getTag(com.example.R.id.webview_shield_tag) as? Boolean) ?: true
                val host = request?.url?.host.orEmpty()
                if (shieldOn && host.isNotEmpty() && TrackerBlocklist.isBlocked(host)) {
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        403,
                        "Blocked by RelayWeb Shield",
                        emptyMap(),
                        ByteArrayInputStream(ByteArray(0))
                    )
                }

                // Legacy Lollipop-to-Pie fallback when WebView ProxyController isn't supported
                val proxyConfig = ProxyEngineManager.getActiveConfig()
                val telemetry = ProxyEngineManager.telemetry.value
                if (proxyConfig.isProxyEnabled &&
                    telemetry.isRunning &&
                    !ProxyEngineManager.isWebViewProxyOverrideSupported() &&
                    request != null &&
                    request.method.equals("GET", ignoreCase = true) &&
                    (request.url.scheme == "http" || request.url.scheme == "https")
                ) {
                    return fetchViaOkHttpProxy(request)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                callbacks.onPageStarted(tabId, url.orEmpty())
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (view == null) return
                val currentUrl = url ?: view.url.orEmpty()
                val title = view.title ?: currentUrl
                callbacks.onPageFinished(
                    tabId = tabId,
                    url = currentUrl,
                    title = title,
                    canBack = view.canGoBack(),
                    canForward = view.canGoForward()
                )
                view.evaluateJavascript(MEDIA_SCANNER_JS, null)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    callbacks.onPageError(tabId, error?.description?.toString() ?: "Page failed to load")
                }
            }
        }

        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applySettings(
        webView: WebView,
        defaultUserAgent: String,
        isIncognito: Boolean,
        javascriptEnabled: Boolean,
        desktopMode: Boolean,
        darkWebContent: Boolean,
        trackerShieldEnabled: Boolean = true
    ) {
        webView.setTag(com.example.R.id.webview_shield_tag, trackerShieldEnabled)
        webView.settings.apply {
            this.javaScriptEnabled = javascriptEnabled
            domStorageEnabled = !isIncognito
            databaseEnabled = !isIncognito
            @Suppress("DEPRECATION")
            saveFormData = !isIncognito
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            userAgentString = if (desktopMode) DESKTOP_USER_AGENT else defaultUserAgent
        }
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(!isIncognito)
        cookieManager.setAcceptThirdPartyCookies(webView, !isIncognito)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, darkWebContent)
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(
                webView.settings,
                if (darkWebContent) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    }

    fun triggerMediaScan(webView: WebView) {
        webView.evaluateJavascript(MEDIA_SCANNER_JS, null)
    }

    fun wipeIncognitoWebView(webView: WebView) {
        webView.stopLoading()
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().flush()
    }

    private fun detectDirectMediaUrl(
        tabId: String,
        url: String,
        callbacks: WebViewCallbacks
    ) {
        val clean = url.lowercase().substringBefore('?')
        val type = when {
            clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".m3u8") || clean.endsWith(".mpd") ->
                MediaStreamType.VIDEO
            clean.endsWith(".mp3") || clean.endsWith(".aac") || clean.endsWith(".ogg") ||
                clean.endsWith(".flac") || clean.endsWith(".wav") ->
                MediaStreamType.AUDIO
            else -> null
        } ?: return

        val filename = url.substringAfterLast('/').substringBefore('?').ifBlank { "Stream" }
        callbacks.onMediaDetected(
            tabId,
            listOf(
                DetectedMediaItem(
                    url = url,
                    title = filename,
                    type = type,
                    sourcePageUrl = url
                )
            )
        )
    }

    private fun fetchViaOkHttpProxy(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val client = ProxyEngineManager.buildProxyOkHttpClient()
            val reqBuilder = okhttp3.Request.Builder().url(request.url.toString())
            request.requestHeaders.forEach { (k, v) ->
                if (!k.equals("Host", ignoreCase = true)) {
                    reqBuilder.header(k, v)
                }
            }
            val response = client.newCall(reqBuilder.build()).execute()
            val contentType = response.header("Content-Type") ?: "text/html"
            val mimeType = contentType.substringBefore(';').trim()
            val charset = contentType.substringAfter("charset=", "utf-8").substringBefore(';').trim()
            val bodyStream = response.body?.byteStream() ?: ByteArrayInputStream(ByteArray(0))
            val headers = response.headers.toMultimap().mapValues { it.value.joinToString(", ") }
            WebResourceResponse(
                mimeType,
                charset,
                response.code.coerceIn(100, 599),
                response.message.ifBlank { "OK" },
                headers,
                bodyStream
            )
        } catch (_: Exception) {
            null
        }
    }

    private class MediaJsBridge(
        private val tabId: String,
        private val webView: WebView,
        private val callbacks: WebViewCallbacks
    ) {
        @JavascriptInterface
        fun onMediaFound(jsonStr: String) {
            val items = mutableListOf<DetectedMediaItem>()
            runCatching {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val url = obj.optString("url")
                    if (url.isNotBlank()) {
                        val typeStr = obj.optString("type", "VIDEO")
                        val type = if (typeStr == "AUDIO") MediaStreamType.AUDIO else MediaStreamType.VIDEO
                        items.add(
                            DetectedMediaItem(
                                url = url,
                                title = obj.optString("title", "HTML5 Media"),
                                type = type,
                                posterUrl = obj.optString("poster", ""),
                                sourcePageUrl = ""
                            )
                        )
                    }
                }
            }
            webView.post {
                callbacks.onMediaDetected(tabId, items)
            }
        }
    }
}
