package com.example.data.repository

import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.local.ProxyProfileDao
import com.example.data.local.ProxyProfileEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.BrowserSettings
import kotlinx.coroutines.flow.Flow

class BrowserRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val proxyProfileDao: ProxyProfileDao,
    val preferences: BrowserPreferences
) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val history: Flow<List<HistoryEntity>> = historyDao.getRecentHistory()
    val proxyProfiles: Flow<List<ProxyProfileEntity>> = proxyProfileDao.getAllProfiles()
    val settings: Flow<BrowserSettings> = preferences.settingsFlow

    fun isBookmarked(url: String): Flow<Boolean> = bookmarkDao.isBookmarked(url)

    fun searchHistory(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)

    suspend fun toggleBookmark(title: String, url: String, currentlyBookmarked: Boolean) {
        if (currentlyBookmarked) {
            bookmarkDao.deleteByUrl(url)
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(title = title.ifBlank { url }, url = url))
        }
    }

    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteById(id)

    suspend fun addHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("relayweb://") || url.startsWith("about:")) return
        historyDao.insertHistory(HistoryEntity(title = title.ifBlank { url }, url = url))
    }

    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)

    suspend fun clearHistory() = historyDao.clearAll()

    suspend fun saveProxyProfile(profile: ProxyProfileEntity): Long {
        return if (profile.id == 0L) {
            proxyProfileDao.insertProfile(profile)
        } else {
            proxyProfileDao.updateProfile(profile)
            profile.id
        }
    }

    suspend fun deleteProxyProfile(id: Long) = proxyProfileDao.deleteById(id)
}
