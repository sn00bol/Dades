package com.sn00bol.dades.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.R
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.ui.screens.components.NoteCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    noteRepository: NoteRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val trashNotes by noteRepository.getTrashNotes().collectAsState(initial = emptyList())
    var showEmptyDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("Are you sure you want to permanently delete all notes in the trash? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { noteRepository.emptyTrash() }
                        showEmptyDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Delete Selected?") },
            text = { Text("Are you sure you want to permanently delete ${selectedIds.size} selected notes? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            selectedIds.forEach { id ->
                                noteRepository.deleteNotePermanently(id)
                            }
                            selectedIds = emptySet()
                            showDeleteSelectedDialog = false
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (trashNotes.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp),
                            shape = CircleShape,
                            color = if (isSelectionMode) MaterialTheme.colorScheme.primary else Color(0xFFE53935)
                        ) {
                            IconButton(onClick = { 
                                if (isSelectionMode) {
                                    showDeleteSelectedDialog = true
                                } else {
                                    showEmptyDialog = true 
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = if (isSelectionMode) R.drawable.trashbin else R.drawable.trashbin),
                                    contentDescription = if (isSelectionMode) "Delete Selected" else "Empty Trash",
                                    tint = if (isSelectionMode) Color.White else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (trashNotes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.trashbin),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    alpha = 0.5f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "There no deleted notes here...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp
            ) {
                items(trashNotes, key = { it.id }) { note ->
                    TrashNoteItem(
                        note = note,
                        isSelected = selectedIds.contains(note.id),
                        onClick = {
                            if (isSelectionMode) {
                                selectedIds = if (selectedIds.contains(note.id)) {
                                    selectedIds - note.id
                                } else {
                                    selectedIds + note.id
                                }
                            } else {
                                // Normal click could be handled here, 
                                // but we want long click to enter selection mode
                            }
                        },
                        onLongClick = {
                            if (!selectedIds.contains(note.id)) {
                                selectedIds = selectedIds + note.id
                            }
                        },
                        onRestore = {
                            scope.launch { noteRepository.restoreNote(note.id) }
                        },
                        onDeleteForever = {
                            scope.launch { noteRepository.deleteNotePermanently(note.id) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrashNoteItem(
    note: PlainNote,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    NoteCard(
        note = note,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        showTags = 0,
        showLock = false,
        overlayContent = {
            if (!isSelected) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Restore") },
                        onClick = {
                            onRestore()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Forever", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDeleteForever()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    )
}
