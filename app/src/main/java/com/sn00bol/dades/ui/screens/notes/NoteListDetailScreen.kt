package com.sn00bol.dades.ui.screens.notes

import androidx.activity.compose.BackHandler
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
import com.sn00bol.dades.ui.screens.notes.components.FloatingNotesToolBar as FloatingNotesToolBarComponent
import com.sn00bol.dades.ui.screens.notes.components.SearchResultsPopup
import com.sn00bol.dades.ui.screens.notes.components.NoteGridPane
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListDetailScreen(
    noteRepository: NoteRepository,
    onNavigateToDetail: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    // Centralized search state management
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // Search history from Database
    val searchHistory by noteRepository.getRecentSearchHistory().collectAsState(initial = emptyList())
    // Color filter
    var selectedColor by remember { mutableStateOf<Long?>(null) }

    // Auto-dismiss focus and disable blur when keyboard hides
    val isImeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible && isSearchActive) {
            wasImeVisible = false
            focusManager.clearFocus(force = true)
            isSearchActive = false
        }
    }

    // Prioritize exiting Search before going back to previous screen
    BackHandler(enabled = isSearchActive) {
        focusManager.clearFocus(force = true)
        isSearchActive = false
    }

    val notes by noteRepository.getAllNotes().collectAsState(initial = null)

    val filteredNotes = remember(notes, searchQuery, selectedColor) {
        notes?.filter { note ->
            val matchesQuery = note.title.contains(searchQuery, ignoreCase = true) ||
                    note.body.contains(searchQuery, ignoreCase = true)
            val matchesColor = selectedColor == null || note.color == selectedColor
            matchesQuery && matchesColor
        }
    }

    LaunchedEffect(Unit) {
        if (noteRepository.isFirstRun()) {
            noteRepository.saveNote(
                PlainNote(
                    title = "Welcome to Dades Notes!",
                    body = "v2026.0.1 ALPHA - 8.21.2026\n check out at these link!\nCHANGELOG: https://github.com/sn00bol/Dades/blob/main/docs/CHANGELOG.md\n KNOWN ISSUE: https://github.com/sn00bol/Dades/blob/main/docs/ISSUES.md\n\nThank for read, your snow ball (lol)"
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
                isSearchActive = isSearchActive,
                onNoteClick = { id ->
                    onNavigateToDetail(id)
                },
                onDeleteNote = { note ->
                    scope.launch {
                        noteRepository.deleteNote(note)
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
                floatingBar = {
                    FloatingNotesToolBar(
                        searchQuery = searchQuery,
                        onNewNoteClick = { 
                            scope.launch {
                                val newId = noteRepository.saveNote(PlainNote(title = "New note", body = ""))
                                onNavigateToDetail(newId)
                            }
                        },
                        onSearchQueryChange = { query ->
                            searchQuery = query
                        },
                        onSearchActiveChange = { isSearchActive = it },
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
            // Show popup when search active
            if (isSearchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = 96.dp) // Moved up a bit more (from 80dp to 96dp)
                ) {
                    SearchResultsPopup(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        searchQuery = searchQuery,
                        notes = notes ?: emptyList(),
                        history = searchHistory,
                        selectedColor = selectedColor,
                        onColorSelect = { color ->
                            selectedColor = color
                        },
                        onHistorySelect = { selected: String ->
                            searchQuery = selected
                            scope.launch {
                                noteRepository.addSearchQuery(selected)
                            }
                            focusManager.clearFocus()
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
                        onDismiss = {
                            focusManager.clearFocus(force = true)
                            isSearchActive = false
                        }
                    )
                }
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
    onSearch: (String) -> Unit = {},
    searchQuery: String = ""
) {
    Box(modifier = modifier) {
        FloatingNotesToolBarComponent(
            onNewNoteClick = onNewNoteClick,
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange,
            onSearch = onSearch,
            searchQuery = searchQuery
        )
    }
}
