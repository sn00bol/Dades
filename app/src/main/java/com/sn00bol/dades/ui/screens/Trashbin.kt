package com.sn00bol.dades.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.R
import com.sn00bol.dades.TextEditor.TextEditorEngine
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.repository.NoteRepository
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
                            color = Color(0xFFE53935)
                        ) {
                            IconButton(onClick = { showEmptyDialog = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.trashbin),
                                    contentDescription = "Empty Trash",
                                    tint = Color.White,
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trashNotes, key = { it.id }) { note ->
                    TrashNoteItem(
                        note = note,
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
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            onClick = { showMenu = true },
            colors = CardDefaults.cardColors(
                containerColor = note.color?.let { Color(it).copy(alpha = 0.5f) } ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TextEditorEngine.render(note.body),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
