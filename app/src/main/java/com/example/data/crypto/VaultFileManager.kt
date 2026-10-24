package com.example.data.crypto

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.db.AppDatabase
import com.example.data.db.VaultMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

object VaultFileManager {
    // 32-byte XOR obfuscation key to break file headers (PNG, JPG, MP4) and protect data on disk
    private val CIPHER_KEY = byteArrayOf(
        0x47, 0x68, 0x6F, 0x73, 0x74, 0x56, 0x61, 0x75,
        0x6C, 0x74, 0x53, 0x65, 0x63, 0x72, 0x65, 0x74,
        0x32, 0x30, 0x32, 0x36, 0x21, 0x40, 0x23, 0x24,
        0x25, 0x5E, 0x26, 0x2A, 0x28, 0x29, 0x5F, 0x2B
    )

    fun getVaultDirectory(context: Context): File {
        val vaultDir = File(context.filesDir, "secret_vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        // Create .nomedia file so media scanners completely ignore this folder
        val noMedia = File(vaultDir, ".nomedia")
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return vaultDir
    }

    /**
     * Imports a photo or video from a ContentResolver URI, scrambles bytes into internal storage,
     * registers it in the Room database, and returns the VaultMediaItem.
     */
    suspend fun importMedia(context: Context, uri: Uri): VaultMediaItem? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var originalName = "media_${System.currentTimeMillis()}"
        var mimeType = resolver.getType(uri) ?: "application/octet-stream"
        var sizeBytes: Long = 0

        // Extract metadata from source URI
        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        originalName = cursor.getString(nameIndex) ?: originalName
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val isVideo = mimeType.startsWith("video/") ||
                originalName.endsWith(".mp4", ignoreCase = true) ||
                originalName.endsWith(".mkv", ignoreCase = true) ||
                originalName.endsWith(".mov", ignoreCase = true) ||
                originalName.endsWith(".3gp", ignoreCase = true)

        val id = UUID.randomUUID().toString()
        val encryptedFileName = "$id.gvault"
        val destFile = File(getVaultDirectory(context), encryptedFileName)

        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    transformStream(input, output)
                }
            } ?: return@withContext null

            if (sizeBytes <= 0) {
                sizeBytes = destFile.length()
            }

            val item = VaultMediaItem(
                id = id,
                originalName = originalName,
                mimeType = mimeType,
                encryptedFileName = encryptedFileName,
                fileSizeBytes = sizeBytes,
                dateAdded = System.currentTimeMillis(),
                isVideo = isVideo
            )

            // Save to database
            val db = AppDatabase.getDatabase(context)
            db.vaultDao().insertMedia(item)
            item
        } catch (e: Exception) {
            e.printStackTrace()
            if (destFile.exists()) destFile.delete()
            null
        }
    }

    /**
     * Decrypts a vault media item to a temporary file in cacheDir so it can be previewed or played.
     */
    suspend fun getDecryptedTempFile(context: Context, item: VaultMediaItem): File? = withContext(Dispatchers.IO) {
        val srcFile = File(getVaultDirectory(context), item.encryptedFileName)
        if (!srcFile.exists()) return@withContext null

        val ext = when {
            item.isVideo -> ".mp4"
            item.mimeType.contains("png", true) -> ".png"
            else -> ".jpg"
        }
        val tempFile = File(context.cacheDir, "preview_${item.id}$ext")
        try {
            FileInputStream(srcFile).use { input ->
                FileOutputStream(tempFile).use { output ->
                    transformStream(input, output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Restores the hidden media back to the device's public Gallery (DCIM/Pictures/Movies)
     * using MediaStore, then removes it from the vault.
     */
    suspend fun restoreMediaToGallery(context: Context, item: VaultMediaItem): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(getVaultDirectory(context), item.encryptedFileName)
        if (!srcFile.exists()) return@withContext false

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.originalName)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val subDir = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$subDir/Restored_Vault")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val targetCollection = if (item.isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        try {
            val insertedUri = resolver.insert(targetCollection, contentValues) ?: return@withContext false

            resolver.openOutputStream(insertedUri)?.use { output ->
                FileInputStream(srcFile).use { input ->
                    transformStream(input, output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(insertedUri, contentValues, null, null)
            }

            // Remove from vault
            srcFile.delete()
            val db = AppDatabase.getDatabase(context)
            db.vaultDao().deleteMedia(item)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Permanently deletes a media file from the vault.
     */
    suspend fun deletePermanently(context: Context, item: VaultMediaItem): Boolean = withContext(Dispatchers.IO) {
        val file = File(getVaultDirectory(context), item.encryptedFileName)
        if (file.exists()) {
            file.delete()
        }
        val db = AppDatabase.getDatabase(context)
        db.vaultDao().deleteMedia(item)
        true
    }

    /**
     * XOR stream transformation: symmetric cipher for encryption and decryption.
     */
    private fun transformStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var keyIndex = 0
        val keyLen = CIPHER_KEY.size

        while (input.read(buffer).also { bytesRead = it } != -1) {
            for (i in 0 until bytesRead) {
                buffer[i] = (buffer[i].toInt() xor CIPHER_KEY[keyIndex].toInt()).toByte()
                keyIndex = (keyIndex + 1) % keyLen
            }
            output.write(buffer, 0, bytesRead)
        }
        output.flush()
    }
}
