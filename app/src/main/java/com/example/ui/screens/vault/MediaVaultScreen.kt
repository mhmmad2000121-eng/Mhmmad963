package com.example.ui.screens.vault

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.crypto.VaultFileManager
import com.example.data.db.AppDatabase
import com.example.data.db.VaultMediaItem
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaVaultScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val mediaItemsFlow = remember { db.vaultDao().getAllVaultMedia() }
    val mediaItems by mediaItemsFlow.collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("all") } // "all", "images", "videos"
    var isImporting by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var selectedMediaItem by remember { mutableStateOf<VaultMediaItem?>(null) }

    // MediaStore delete intent launcher for Android 10+ (API 29+)
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "تم حذف الملف الأصلي من معرض الهاتف بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    // Media picker launcher: Select multiple images and videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isImporting = true
                importStatusMessage = "جارٍ تشفير وحفظ ${uris.size} ملف في الخزنة السرية..."
                var importedCount = 0
                val urisToDelete = mutableListOf<Uri>()

                for (uri in uris) {
                    val item = VaultFileManager.importMedia(context, uri)
                    if (item != null) {
                        importedCount++
                        urisToDelete.add(uri)
                    }
                }

                isImporting = false
                importStatusMessage = null
                Toast.makeText(
                    context,
                    "تم تشفير وحفظ $importedCount ملف داخل الخزنة بنجاح!",
                    Toast.LENGTH_LONG
                ).show()

                // Request deletion of original files from MediaStore so they vanish from the public gallery
                if (urisToDelete.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val deleteIntentSender = MediaStore.createDeleteRequest(context.contentResolver, urisToDelete).intentSender
                        deleteLauncher.launch(IntentSenderRequest.Builder(deleteIntentSender).build())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val filteredList = remember(mediaItems, selectedFilter) {
        when (selectedFilter) {
            "images" -> mediaItems.filter { !it.isVideo }
            "videos" -> mediaItems.filter { it.isVideo }
            else -> mediaItems
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mediaPickerLauncher.launch("*/*") },
                containerColor = CyanPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("import_media_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إخفاء وتشفير وسائط", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header stats
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${mediaItems.size} ملف مشفر في الخزنة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val totalBytes = mediaItems.sumOf { it.fileSizeBytes }
                        Text(
                            text = "المساحة المحمية: ${formatFileSize(totalBytes)} • مجلد مشفر (.nomedia)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("الكل (${mediaItems.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedFilter == "images",
                    onClick = { selectedFilter = "images" },
                    label = { Text("الصور (${mediaItems.count { !it.isVideo }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedFilter == "videos",
                    onClick = { selectedFilter = "videos" },
                    label = { Text("الفيديوهات (${mediaItems.count { it.isVideo }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            if (isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = importStatusMessage ?: "جارٍ المعالجة والتشفير...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredList.isEmpty() && !isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_media),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        VaultMediaGridItem(
                            item = item,
                            onClick = { selectedMediaItem = item }
                        )
                    }
                }
            }
        }
    }

    // Media Viewer Dialog (Full Photo or Video Player with Restore & Delete)
    selectedMediaItem?.let { item ->
        MediaViewerDialog(
            item = item,
            onDismiss = { selectedMediaItem = null },
            onRestored = {
                selectedMediaItem = null
                Toast.makeText(context, "تمت استعادة الملف إلى معرض الهاتف العام بنجاح", Toast.LENGTH_SHORT).show()
            },
            onDeleted = {
                selectedMediaItem = null
                Toast.makeText(context, "تم حذف الملف نهائياً من الخزنة", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun VaultMediaGridItem(
    item: VaultMediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var previewFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            val file = VaultFileManager.getDecryptedTempFile(context, item)
            previewFile = file
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .testTag("vault_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (previewFile != null && !item.isVideo) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(previewFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.originalName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.Image,
                        contentDescription = null,
                        tint = CyanPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Video overlay badge
            if (item.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom name/size badge
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = formatFileSize(item.fileSizeBytes),
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MediaViewerDialog(
    item: VaultMediaItem,
    onDismiss: () -> Unit,
    onRestored: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tempFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessingAction by remember { mutableStateOf(false) }

    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            val file = VaultFileManager.getDecryptedTempFile(context, item)
            tempFile = file
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.originalName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatFileSize(item.fileSizeBytes)} • مشفر",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = Color.Black.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Restore Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessingAction = true
                                    val success = VaultFileManager.restoreMediaToGallery(context, item)
                                    isProcessingAction = false
                                    if (success) onRestored()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            enabled = !isProcessingAction,
                            modifier = Modifier.testTag("restore_media_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استعادة للمعرض العام", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Delete Button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isProcessingAction = true
                                    VaultFileManager.deletePermanently(context, item)
                                    isProcessingAction = false
                                    onDeleted()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            enabled = !isProcessingAction,
                            modifier = Modifier.testTag("delete_media_btn")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حذف نهائي", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading || isProcessingAction) {
                    CircularProgressIndicator(color = CyanPrimary)
                } else if (tempFile != null) {
                    if (item.isVideo) {
                        // Video Player using native VideoView with MediaController
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val mediaController = MediaController(ctx)
                                    mediaController.setAnchorView(this)
                                    setMediaController(mediaController)
                                    setVideoPath(tempFile!!.absolutePath)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // High-res Image preview
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(tempFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.originalName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Text("تعذر فك تشفير المعاينة", color = Color.Red)
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
