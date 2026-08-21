package com.sn00bol.dades.ui.screens.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.ui.screens.notes.components.FloatingNotesToolBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    id: Long?,
    noteRepository: NoteRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf<PlainNote?>(null) }
    
    // Load note data
    LaunchedEffect(id) {
        if (id != null) {
            note = noteRepository.getNoteById(id)
        }
    }

    if (note != null) {
        val currentNote = note!!
        var title by remember(currentNote.id) { mutableStateOf(currentNote.title) }
        var body by remember(currentNote.id) { mutableStateOf(currentNote.body) }
        var color by remember(currentNote.id) { mutableStateOf(currentNote.color) }
        var showMenu by remember { mutableStateOf(false) }
        
        // Internal search state
        var isInternalSearchActive by remember { mutableStateOf(false) }
        var internalSearchQuery by remember { mutableStateOf("") }
        val internalSearchFocusRequester = remember { FocusRequester() }

        // Request focus when opening search
        LaunchedEffect(isInternalSearchActive) {
            if (isInternalSearchActive) {
                delay(100) // Wait for UI to finish rendering
                internalSearchFocusRequester.requestFocus()
            }
        }

        // Save when system Back key is pressed
        BackHandler {
            if (isInternalSearchActive) {
                isInternalSearchActive = false
                internalSearchQuery = ""
            } else {
                scope.launch {
                    if (title != currentNote.title || body != currentNote.body || color != currentNote.color) {
                        noteRepository.saveNote(currentNote.copy(title = title, body = body, color = color))
                    }
                    onBack()
                }
            }
        }

        // Auto-save logic
        LaunchedEffect(title, body, color) {
            if (title != currentNote.title || body != currentNote.body || color != currentNote.color) {
                delay(500)
                noteRepository.saveNote(currentNote.copy(title = title, body = body, color = color))
            }
        }

        val backgroundColor = color?.let { Color(it) } ?: MaterialTheme.colorScheme.background

        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (title != currentNote.title || body != currentNote.body || color != currentNote.color) {
                                    noteRepository.saveNote(currentNote.copy(title = title, body = body, color = color))
                                }
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Find in note") },
                                    onClick = { 
                                        showMenu = false
                                        isInternalSearchActive = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    "Change color",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .width(200.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        null, 0xFFFFCDD2, 0xFFF8BBD0, 0xFFE1BEE7, 0xFFC5CAE9,
                                        0xFFB3E5FC, 0xFFB2DFDB, 0xFFDCEDC8, 0xFFFFF9C4, 0xFFFFE0B2
                                    ).forEach { colorValue ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(colorValue?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                                                .background(if (color == colorValue) Color.Black.copy(alpha = 0.2f) else Color.Transparent)
                                                .combinedClickable(
                                                    onClick = { color = colorValue }
                                                )
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = { Text("Lock") },
                                    onClick = { showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete note", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            noteRepository.deleteNote(currentNote)
                                            onBack()
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                if (isInternalSearchActive) {
                    FloatingNotesToolBar(
                        searchQuery = internalSearchQuery,
                        onSearchQueryChange = { internalSearchQuery = it },
                        onSearchActiveChange = { /* Ignore focus change here */ },
                        onClose = {
                            isInternalSearchActive = false
                            internalSearchQuery = ""
                        },
                        showAddButton = false,
                        placeholder = "Find in note...",
                        focusRequester = internalSearchFocusRequester
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall, color = Color.Gray) },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isInternalSearchActive && internalSearchQuery.isNotEmpty()) {
                        SearchHighlightTransformation(internalSearchQuery, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    } else {
                        VisualTransformation.None
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
                
                TextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Start noting...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxSize(),
                    visualTransformation = if (isInternalSearchActive && internalSearchQuery.isNotEmpty()) {
                        SearchHighlightTransformation(internalSearchQuery, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    } else {
                        VisualTransformation.None
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

class SearchHighlightTransformation(
    private val query: String,
    private val highlightColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            val textString = text.text
            var startIndex = 0
            while (startIndex < textString.length) {
                val index = textString.indexOf(query, startIndex, ignoreCase = true)
                if (index == -1) {
                    append(textString.substring(startIndex))
                    break
                }
                append(textString.substring(startIndex, index))
                withStyle(style = SpanStyle(background = highlightColor)) {
                    append(textString.substring(index, index + query.length))
                }
                startIndex = index + query.length
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
