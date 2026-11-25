package com.example.browser

enum class SearchEngine(
    val id: String,
    val displayName: String,
    val searchUrlTemplate: String
) {
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    STARTPAGE("startpage", "Startpage", "https://www.startpage.com/do/dsearch?query=%s"),
    BRAVE("brave", "Brave Search", "https://search.brave.com/search?q=%s"),
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s");

    companion object {
        fun fromId(id: String): SearchEngine =
            entries.firstOrNull { it.id == id } ?: DUCKDUCKGO
    }
}

enum class MediaStreamType {
    VIDEO,
    AUDIO
}

data class DetectedMediaItem(
    val url: String,
    val title: String,
    val type: MediaStreamType,
    val mimeType: String = "",
    val posterUrl: String = "",
    val sourcePageUrl: String = ""
)

data class BrowserTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = "",
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = false,
    val detectedMedia: List<DetectedMediaItem> = emptyList(),
    val lastError: String? = null
) {
    val isHomePage: Boolean
        get() = url.isBlank()
}

data class SpeedDialSite(
    val title: String,
    val subtitle: String,
    val url: String,
    val badge: String
)

object BrowserPresets {
    const val INTERNAL_HTML5_TEST_URL = "relayweb://html5-test"

    val speedDialSites = listOf(
        SpeedDialSite(
            title = "DuckDuckGo",
            subtitle = "Private search",
            url = "https://duckduckgo.com",
            badge = "SEARCH"
        ),
        SpeedDialSite(
            title = "Wikipedia",
            subtitle = "Free encyclopedia",
            url = "https://en.m.wikipedia.org",
            badge = "WIKI"
        ),
        SpeedDialSite(
            title = "Internet Archive",
            subtitle = "Open media library",
            url = "https://archive.org",
            badge = "MEDIA"
        ),
        SpeedDialSite(
            title = "MDN Web Docs",
            subtitle = "HTML5 & Web APIs",
            url = "https://developer.mozilla.org",
            badge = "DOCS"
        ),
        SpeedDialSite(
            title = "HTML5 Test Lab",
            subtitle = "Built-in audio/video test",
            url = INTERNAL_HTML5_TEST_URL,
            badge = "HTML5"
        ),
        SpeedDialSite(
            title = "IP Check",
            subtitle = "Verify proxy exit IP",
            url = "https://api.ipify.org",
            badge = "PROXY"
        )
    )

    val openTestMediaStreams = listOf(
        DetectedMediaItem(
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            title = "Big Buck Bunny (MP4 720p)",
            type = MediaStreamType.VIDEO,
            mimeType = "video/mp4"
        ),
        DetectedMediaItem(
            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            title = "Mux HLS Adaptive Stream (.m3u8)",
            type = MediaStreamType.VIDEO,
            mimeType = "application/x-mpegURL"
        ),
        DetectedMediaItem(
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            title = "Elephants Dream (MP4)",
            type = MediaStreamType.VIDEO,
            mimeType = "video/mp4"
        ),
        DetectedMediaItem(
            url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            title = "SoundHelix Synth Track #1 (MP3)",
            type = MediaStreamType.AUDIO,
            mimeType = "audio/mpeg"
        )
    )
}
