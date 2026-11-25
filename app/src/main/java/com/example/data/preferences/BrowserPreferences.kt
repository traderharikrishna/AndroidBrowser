package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserDataStore: DataStore<Preferences> by preferencesDataStore(name = "relayweb_settings")

data class BrowserSettings(
    val searchEngineId: String = "duckduckgo",
    val desktopMode: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val trackerShieldEnabled: Boolean = true,
    val stripTrackingHeaders: Boolean = true,
    val darkWebContent: Boolean = false,
    val blockPopups: Boolean = true,
    val activeProxyMode: String = "LOCAL_RELAY",
    val activeProxyHost: String = "",
    val activeProxyPort: Int = 8080,
    val activeProxyUser: String = "",
    val activeProxyPass: String = "",
    val activeProfileId: Long = -1L,
    val proxyAutoStart: Boolean = true,
    val darkThemePreferred: Boolean = true
)

class BrowserPreferences(private val context: Context) {

    private object Keys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        val JS_ENABLED = booleanPreferencesKey("js_enabled")
        val TRACKER_SHIELD = booleanPreferencesKey("tracker_shield")
        val STRIP_HEADERS = booleanPreferencesKey("strip_headers")
        val DARK_WEB = booleanPreferencesKey("dark_web")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val PROXY_MODE = stringPreferencesKey("proxy_mode")
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val PROXY_USER = stringPreferencesKey("proxy_user")
        val PROXY_PASS = stringPreferencesKey("proxy_pass")
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        val PROXY_AUTO_START = booleanPreferencesKey("proxy_auto_start")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val settingsFlow: Flow<BrowserSettings> = context.browserDataStore.data.map { prefs ->
        BrowserSettings(
            searchEngineId = prefs[Keys.SEARCH_ENGINE] ?: "duckduckgo",
            desktopMode = prefs[Keys.DESKTOP_MODE] ?: false,
            javascriptEnabled = prefs[Keys.JS_ENABLED] ?: true,
            trackerShieldEnabled = prefs[Keys.TRACKER_SHIELD] ?: true,
            stripTrackingHeaders = prefs[Keys.STRIP_HEADERS] ?: true,
            darkWebContent = prefs[Keys.DARK_WEB] ?: false,
            blockPopups = prefs[Keys.BLOCK_POPUPS] ?: true,
            activeProxyMode = prefs[Keys.PROXY_MODE] ?: "LOCAL_RELAY",
            activeProxyHost = prefs[Keys.PROXY_HOST] ?: "",
            activeProxyPort = prefs[Keys.PROXY_PORT] ?: 8080,
            activeProxyUser = prefs[Keys.PROXY_USER] ?: "",
            activeProxyPass = prefs[Keys.PROXY_PASS] ?: "",
            activeProfileId = prefs[Keys.ACTIVE_PROFILE_ID] ?: -1L,
            proxyAutoStart = prefs[Keys.PROXY_AUTO_START] ?: true,
            darkThemePreferred = prefs[Keys.DARK_THEME] ?: true
        )
    }

    suspend fun updateSearchEngine(engineId: String) {
        context.browserDataStore.edit { it[Keys.SEARCH_ENGINE] = engineId }
    }

    suspend fun setDesktopMode(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.DESKTOP_MODE] = enabled }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.JS_ENABLED] = enabled }
    }

    suspend fun setTrackerShield(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.TRACKER_SHIELD] = enabled }
    }

    suspend fun setStripTrackingHeaders(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.STRIP_HEADERS] = enabled }
    }

    suspend fun setDarkWebContent(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.DARK_WEB] = enabled }
    }

    suspend fun setBlockPopups(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.BLOCK_POPUPS] = enabled }
    }

    suspend fun setDarkThemePreferred(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setProxyAutoStart(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.PROXY_AUTO_START] = enabled }
    }

    suspend fun saveActiveProxyConfig(
        mode: String,
        host: String,
        port: Int,
        user: String,
        pass: String,
        profileId: Long = -1L
    ) {
        context.browserDataStore.edit {
            it[Keys.PROXY_MODE] = mode
            it[Keys.PROXY_HOST] = host
            it[Keys.PROXY_PORT] = port
            it[Keys.PROXY_USER] = user
            it[Keys.PROXY_PASS] = pass
            it[Keys.ACTIVE_PROFILE_ID] = profileId
        }
    }
}
