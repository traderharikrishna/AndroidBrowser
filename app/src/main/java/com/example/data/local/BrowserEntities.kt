package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "proxy_profiles")
data class ProxyProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val modeName: String,
    val host: String,
    val port: Int,
    val username: String = "",
    val password: String = "",
    val blockTrackers: Boolean = true,
    val stripTrackingHeaders: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
