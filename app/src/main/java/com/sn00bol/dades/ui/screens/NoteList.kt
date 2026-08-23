package com.sn00bol.dades.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.database.SettingsManager
import com.sn00bol.dades.ui.screens.components.NoteGridPane
import com.sn00bol.dades.ui.screens.components.FloatingNotesToolBar as FloatingNotesToolBarComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListDetailScreen(
    noteRepository: NoteRepository,
    settingsManager: SettingsManager,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    val blurEnabled by settingsManager.blurEnabled.collectAsState(initial = true)
    val trashAutoDeleteDays by settingsManager.trashAutoDeleteDays.collectAsState(initial = 30)

    // Clear old trash notes on start
    LaunchedEffect(trashAutoDeleteDays) {
        noteRepository.clearOldTrash(trashAutoDeleteDays)
    }
    
    // Centralized search state management
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // Search history from Database
    val searchHistory by noteRepository.getRecentSearchHistory().collectAsState(initial = emptyList())
    // Tags from Database
    val allTags by noteRepository.getAllTags().collectAsState(initial = emptyList())
    // Filters
    var selectedColor by remember { mutableStateOf<Long?>(null) }
    var selectedTagId by remember { mutableStateOf<Long?>(null) }

    // Auto-dismiss focus and disable blur when keyboard hides
    val isImeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible) {
            delay(150)
            wasImeVisible = false
            // User requested to keep search bar active when keyboard hides in main menu
            if (isSearchActive) {
                focusManager.clearFocus(force = true)
            }
        }
    }

    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            selectedColor = null
            selectedTagId = null
        }
    }

    // Prioritize exiting Search before going back to previous screen
    BackHandler(enabled = isSearchActive) {
        focusManager.clearFocus(force = true)
        isSearchActive = false
    }

    val notes by noteRepository.getAllNotes().collectAsState(initial = null)

    val filteredNotes = remember(notes, searchQuery, selectedColor, selectedTagId) {
        notes?.filter { note ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                note.title.contains(searchQuery, ignoreCase = true) ||
                        note.body.contains(searchQuery, ignoreCase = true) ||
                        note.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
            }
            
            val matchesColor = selectedColor == null || note.color == selectedColor
            
            val matchesTag = if (selectedTagId == null) true else {
                note.tags.any { it.tagId == selectedTagId }
            }
            
            matchesQuery && matchesColor && matchesTag
        }
    }

    LaunchedEffect(Unit) {
        if (noteRepository.isFirstRun()) {
            noteRepository.saveNote(
                PlainNote(
                    title = "Welcome to Dades Notes!",
                    body = "This is your first notes, you can change note color, add tags,.. bla bla, browse some link there:\n\nCHANGELOG: https://github.com/sn00bol/Dades/blob/main/docs/CHANGELOG.md\n KNOWN ISSUE: https://github.com/sn00bol/Dades/blob/main/docs/ISSUES.md"
                )
            )
            noteRepository.markFirstRunCompleted()
        }
    }

    val currentNotes = notes
    if (currentNotes == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            NoteGridPane(
                notes = filteredNotes ?: emptyList(),
                allTags = allTags,
                isSearchActive = isSearchActive,
                blurEnabled = blurEnabled,
                onNoteClick = { id ->
                    onNavigateToDetail(id)
                },
                onDeleteNote = { note ->
                    scope.launch {
                        noteRepository.deleteNote(note)
                    }
                },
                onSaveTag = { tag ->
                    scope.launch {
                        noteRepository.saveTag(tag)
                    }
                },
                onAddTagToNote = { noteId, tagId ->
                    scope.launch {
                        noteRepository.addTagToNote(noteId, tagId)
                    }
                },
                onRemoveTagFromNote = { noteId, tagId ->
                    scope.launch {
                        noteRepository.removeTagFromNote(noteId, tagId)
                    }
                },
                onDismissSearch = {
                    if (searchQuery.isNotBlank()) {
                        scope.launch {
                            noteRepository.addSearchQuery(searchQuery)
                        }
                    }
                    focusManager.clearFocus(force = true)
                    isSearchActive = false
                },
                onNavigateToTrash = onNavigateToTrash,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToHelp = onNavigateToHelp,
                floatingBar = {
                    FloatingNotesToolBar(
                        searchQuery = searchQuery,
                        onNewNoteClick = {
                            scope.launch {
                                val newId = noteRepository.saveNote(
                                    PlainNote(
                                        title = "New note",
                                        body = ""
                                    )
                                )
                                onNavigateToDetail(newId)
                            }
                        },
                        onSearchQueryChange = { query ->
                            searchQuery = query
                        },
                        onSearchActiveChange = { isSearchActive = it },
                        isSearchActive = isSearchActive,
                        onSearch = { query ->
                            if (query.isNotBlank()) {
                                scope.launch {
                                    noteRepository.addSearchQuery(query)
                                }
                            }
                        }
                    )
                }
            )
            // Show Search Screen as a full-screen overlay
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                SearchScreen(
                    notes = notes ?: emptyList(),
                    history = searchHistory,
                    allTags = allTags,
                    searchQuery = searchQuery,
                    selectedColor = selectedColor,
                    selectedTagId = selectedTagId,
                    onSearchQueryChange = { searchQuery = it },
                    onColorSelect = { selectedColor = it },
                    onTagSelect = { selectedTagId = it },
                    onHistorySelect = { selected ->
                        searchQuery = selected
                        scope.launch {
                            noteRepository.addSearchQuery(selected)
                        }
                    },
                    onDeleteHistory = { query ->
                        scope.launch {
                            noteRepository.deleteSearchQuery(query)
                        }
                    },
                    onClearHistory = {
                        scope.launch {
                            noteRepository.clearSearchHistory()
                        }
                    },
                    onNoteClick = { id ->
                        onNavigateToDetail(id)
                    },
                    onDeleteNote = { note ->
                        scope.launch {
                            noteRepository.deleteNote(note)
                        }
                    },
                    onAddTagToNote = { _ ->
                        // Reuse the logic from NoteList if needed, 
                        // but for now just a callback
                    },
                    onDismiss = {
                        if (searchQuery.isNotBlank()) {
                            scope.launch {
                                noteRepository.addSearchQuery(searchQuery)
                            }
                        }
                        focusManager.clearFocus(force = true)
                        isSearchActive = false
                    }
                )
            }
        }
    }
}

@Composable
fun FloatingNotesToolBar(
    modifier: Modifier = Modifier,
    onNewNoteClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    isSearchActive: Boolean = false,
    onSearch: (String) -> Unit = {},
    showAddButton: Boolean = true,
    searchQuery: String = ""
) {
    Box(modifier = modifier) {
        FloatingNotesToolBarComponent(
            onNewNoteClick = onNewNoteClick,
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange,
            isSearchActive = isSearchActive,
            onSearch = onSearch,
            showAddButton = showAddButton,
            searchQuery = searchQuery
        )
    }
}
