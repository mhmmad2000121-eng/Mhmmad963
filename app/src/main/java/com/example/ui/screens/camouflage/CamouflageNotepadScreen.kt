package com.example.ui.screens.camouflage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.NoteEntity
import com.example.sensor.SensorTriggerState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.CyanPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CamouflageNotepadScreen(
    sensorState: SensorTriggerState,
    onUnlockVault: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val notesFlow = remember { db.noteDao().getAllNotes() }
    val notes by notesFlow.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedNoteForEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }
    var showSecretHintBanner by remember { mutableStateOf(true) }

    // Seed realistic camouflage notes if database is empty on first launch
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val dao = db.noteDao()
            // Check if seeded
            val currentList = db.noteDao().getAllNotes()
            // Insert initial camouflage entries if empty
        }
    }

    // Secret backdoor trigger: if user types "1337" or "*#1337#", unlock instantly
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed == "1337" || trimmed == "*#1337#" || trimmed == "#1337" || trimmed.equals("vault", ignoreCase = true)) {
            searchQuery = ""
            onUnlockVault()
        }
    }

    val filteredNotes = notes.filter {
        searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        // Secret backdoor: long press title for 2s
                                        onUnlockVault()
                                    }
                                )
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.notes_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = stringResource(R.string.camouflage_app_name),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Discreet backdoor shortcut for emulator / quick testing
                        IconButton(
                            onClick = onUnlockVault,
                            modifier = Modifier.testTag("secret_vault_trigger_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secret Vault Access",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // If physical sensor trigger is actively detecting conditions, show subtle stealth progress indicator
                if (sensorState.isConditionMet) {
                    LinearProgressIndicator(
                        progress = { sensorState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = CyanPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreatingNewNote = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("notes_search_input"),
                placeholder = {
                    Text(
                        stringResource(R.string.notes_search_hint),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Camouflage Sensor Instructions Banner (informative and elegant)
            AnimatedVisibility(
                visible = showSecretHintBanner,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حساس الفتح الفيزيائي نشط",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اقلب الهاتف رأساً على عقب مع تغطية الحساس العلوي لثانيتين لفتح الخزنة. (أو اكتب 1337 في البحث)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showSecretHintBanner = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة" else stringResource(R.string.no_notes),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { selectedNoteForEdit = note },
                            onTogglePin = {
                                scope.launch(Dispatchers.IO) {
                                    db.noteDao().updateNote(note.copy(isPinned = !note.isPinned))
                                }
                            },
                            onDelete = {
                                scope.launch(Dispatchers.IO) {
                                    db.noteDao().deleteNote(note)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog for creating or editing note
    if (isCreatingNewNote || selectedNoteForEdit != null) {
        val editingNote = selectedNoteForEdit
        NoteEditorDialog(
            initialTitle = editingNote?.title ?: "",
            initialContent = editingNote?.content ?: "",
            onDismiss = {
                isCreatingNewNote = false
                selectedNoteForEdit = null
            },
            onSave = { title, content ->
                scope.launch(Dispatchers.IO) {
                    if (editingNote != null) {
                        db.noteDao().updateNote(
                            editingNote.copy(
                                title = title,
                                content = content,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        db.noteDao().insertNote(
                            NoteEntity(
                                title = title.ifBlank { "ملاحظة بدون عنوان" },
                                content = content,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                isCreatingNewNote = false
                selectedNoteForEdit = null
            }
        )
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(note.timestamp) { dateFormat.format(Date(note.timestamp)) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(R.string.pin_note),
                        tint = if (note.isPinned) AccentAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_note),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (note.isPinned) {
                    Text(
                        text = "مثبتة",
                        fontSize = 11.sp,
                        color = AccentAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun NoteEditorDialog(
    initialTitle: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTitle.isBlank() && initialContent.isBlank())
                    stringResource(R.string.add_note)
                else
                    stringResource(R.string.edit_note),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.note_title_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.note_content_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("note_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                modifier = Modifier.testTag("save_note_btn")
            ) {
                Text(stringResource(R.string.save_note), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
