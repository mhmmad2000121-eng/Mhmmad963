package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    // Hidden Media Items
    @Query("SELECT * FROM vault_media ORDER BY dateAdded DESC")
    fun getAllVaultMedia(): Flow<List<VaultMediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: VaultMediaItem)

    @Delete
    suspend fun deleteMedia(item: VaultMediaItem)

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: String): VaultMediaItem?

    // Blocked Apps
    @Query("SELECT * FROM blocked_apps ORDER BY dateBlocked DESC")
    fun getAllBlockedApps(): Flow<List<BlockedAppConfig>>

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    fun getActiveBlockedApps(): Flow<List<BlockedAppConfig>>

    @Query("SELECT isBlocked FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun isAppBlocked(packageName: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppBlocked(config: BlockedAppConfig)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun removeAppBlock(packageName: String)
}
