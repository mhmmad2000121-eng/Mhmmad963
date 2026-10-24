package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for real camouflage daily notes.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val colorTag: Int = 0,
    val isPinned: Boolean = false
)

/**
 * Entity for encrypted hidden media items (photos and videos).
 */
@Entity(tableName = "vault_media")
data class VaultMediaItem(
    @PrimaryKey
    val id: String, // UUID
    val originalName: String,
    val mimeType: String,
    val encryptedFileName: String,
    val fileSizeBytes: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val isVideo: Boolean = false
)

/**
 * Entity for blocked / hidden apps.
 */
@Entity(tableName = "blocked_apps")
data class BlockedAppConfig(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val dateBlocked: Long = System.currentTimeMillis()
)
