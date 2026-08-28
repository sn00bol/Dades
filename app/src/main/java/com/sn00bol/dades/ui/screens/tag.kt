package com.sn00bol.dades.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.database.SettingsManager
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.model.Tag
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.ui.screens.components.FloatingNotesToolBar
import com.sn00bol.dades.ui.screens.components.NoteGridPane
import com.sn00bol.dades.ui.screens.components.RenameTagDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    noteRepository: NoteRepository,
    onBack: () -> Unit,
    startCreatingInitially: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val allTags by noteRepository.getAllTags().collectAsState(initial = emptyList())
    
    var newTagName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(startCreatingInitially) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var editTagName by remember { mutableStateOf("") }
    
    val createFocusRequester = remember { FocusRequester() }
    val editFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isCreating) {
        if (isCreating) {
            createFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(editingTag) {
        if (editingTag != null) {
            editFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Managing tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (!isCreating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCreating = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create new tag",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Text(
                    text = "Create new tag",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        newTagName = ""
                        isCreating = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                    
                    TextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("Tag name...") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(createFocusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    
                    IconButton(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                scope.launch {
                                    noteRepository.saveTag(Tag(name = newTagName, color = 0))
                                    newTagName = ""
                                    isCreating = false
                                }
                            }
                        },
                        enabled = newTagName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Existing Tags",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allTags, key = { it.tagId }) { tag ->
                    if (editingTag?.tagId == tag.tagId) {
                        // Editing mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        noteRepository.deleteTag(tag)
                                        editingTag = null
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                            
                            TextField(
                                value = editTagName,
                                onValueChange = { editTagName = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(editFocusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            
                            IconButton(
                                onClick = {
                                    if (editTagName.isNotBlank()) {
                                        scope.launch {
                                            noteRepository.saveTag(tag.copy(name = editTagName))
                                            editingTag = null
                                        }
                                    }
                                },
                                enabled = editTagName.isNotBlank()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        // Normal mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable { 
                                    editingTag = tag
                                    editTagName = tag.name
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagNotesScreen(
    tagId: Long,
    noteRepository: NoteRepository,
    settingsManager: SettingsManager,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToManageTags: (Boolean) -> Unit,
    onNavigateToTag: (Long) -> Unit,
    onNavigateToNotes: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allTags by noteRepository.getAllTags().collectAsState(initial = emptyList())
    val tag = allTags.find { it.tagId == tagId }
    
    val notes by noteRepository.getAllNotes().collectAsState(initial = null)
    val filteredNotes = remember(notes, tagId) {
        notes?.filter { note -> note.tags.any { it.tagId == tagId } } ?: emptyList()
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showRenameDialog && tag != null) {
        RenameTagDialog(
            tag = tag,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                scope.launch {
                    noteRepository.saveTag(tag.copy(name = newName))
                    showRenameDialog = false
                }
            }
        )
    }

    if (showDeleteConfirm && tag != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete tag?") },
            text = { Text("Are you sure you want to permanently delete tag? The notes will not deleted") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            noteRepository.deleteTag(tag)
                            showDeleteConfirm = false
                            onNavigateToNotes()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val blurEnabled by settingsManager.blurEnabled.collectAsState(initial = true)

    NoteGridPane(
        notes = filteredNotes,
        allTags = allTags,
        isSearchActive = false,
        blurEnabled = blurEnabled,
        currentTagId = tagId,
        title = null,
        onNoteClick = onNavigateToDetail,
        onDeleteNote = { note ->
            scope.launch { noteRepository.deleteNote(note) }
        },
        onSaveTag = { /* Not used here */ },
        onAddTagToNote = { noteId, tId ->
            scope.launch { noteRepository.addTagToNote(noteId, tId) }
        },
        onRemoveTagFromNote = { noteId, tId ->
            scope.launch { noteRepository.removeTagFromNote(noteId, tId) }
        },
        onDismissSearch = { },
        onNavigateToTrash = onNavigateToTrash,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToHelp = onNavigateToHelp,
        onNavigateToManageTags = onNavigateToManageTags,
        onNavigateToTag = onNavigateToTag,
        onNavigateToNotes = onNavigateToNotes,
        onDeleteNotes = { ids ->
            scope.launch {
                ids.forEach { id ->
                    noteRepository.moveNoteToTrash(id)
                }
            }
        },
        onDuplicateNotes = { ids ->
            scope.launch {
                ids.forEach { id ->
                    noteRepository.duplicateNote(id)
                }
            }
        },
        onUpdateNotesColor = { ids, color ->
            scope.launch {
                ids.forEach { id ->
                    val note = noteRepository.getNoteById(id)
                    if (note != null) {
                        noteRepository.saveNote(note.copy(color = color))
                    }
                }
            }
        },
        moreVertActions = { onDismiss ->
            DropdownMenuItem(
                text = { Text("Change tag name") },
                onClick = {
                    onDismiss()
                    showRenameDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete tag") },
                onClick = {
                    onDismiss()
                    showDeleteConfirm = true
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        },
        floatingBar = {
            FloatingNotesToolBar(
                showSearchBar = false,
                onSearchQueryChange = {},
                onNewNoteClick = {
                    scope.launch {
                        val newId = noteRepository.saveNote(
                            PlainNote(
                                title = "New note",
                                body = ""
                            )
                        )
                        noteRepository.addTagToNote(newId, tagId)
                        onNavigateToDetail(newId)
                    }
                }
            )
        }
    )
}
