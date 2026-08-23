package com.sn00bol.dades.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import com.sn00bol.dades.TextEditor.RichTextEditorState
import com.sn00bol.dades.TextEditor.TextEditorEngine
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.model.Tag
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.ui.screens.components.CreateTagDialog
import com.sn00bol.dades.ui.screens.components.FloatingNotesToolBar
import com.sn00bol.dades.ui.screens.components.TagSelectionDialog
import com.sn00bol.dades.TextEditor.TextFormatToolbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
        val editorState = remember(currentNote.id) { TextEditorEngine.parseToState(currentNote.body) }
        var color by remember(currentNote.id) { mutableStateOf(currentNote.color) }
        var tags by remember(currentNote.id) { mutableStateOf(currentNote.tags) }
        var isLocked by remember(currentNote.id) { mutableStateOf(currentNote.isLocked) }
        var showMenu by remember { mutableStateOf(false) }
        
        val allTags by noteRepository.getAllTags().collectAsState(initial = emptyList())
        var showCreateTagDialog by remember { mutableStateOf(false) }
        var showTagSelectionDialog by remember { mutableStateOf(false) }

        if (showCreateTagDialog) {
            CreateTagDialog(
                onDismiss = { showCreateTagDialog = false },
                onConfirm = { name, tagColor ->
                    scope.launch {
                        noteRepository.saveTag(Tag(name = name, color = tagColor))
                        showCreateTagDialog = false
                    }
                }
            )
        }

        if (showTagSelectionDialog) {
            TagSelectionDialog(
                noteId = currentNote.id,
                noteTags = tags,
                allTags = allTags,
                onDismiss = { showTagSelectionDialog = false },
                onTagToggle = { tag, isAdded ->
                    scope.launch {
                        if (isAdded) {
                            noteRepository.addTagToNote(currentNote.id, tag.tagId)
                            tags = tags + tag
                        } else {
                            noteRepository.removeTagFromNote(currentNote.id, tag.tagId)
                            tags = tags.filter { it.tagId != tag.tagId }
                        }
                    }
                },
                onCreateNewTag = {
                    showCreateTagDialog = true
                }
            )
        }
        
        // Internal search state
        var isInternalSearchActive by remember { mutableStateOf(false) }
        var internalSearchQuery by remember { mutableStateOf("") }
        val internalSearchFocusRequester = remember { FocusRequester() }

        // Auto-dismiss internal search when keyboard hides
        val isImeVisible = WindowInsets.isImeVisible
        var wasImeVisible by remember { mutableStateOf(false) }

        LaunchedEffect(isImeVisible) {
            if (isImeVisible) {
                wasImeVisible = true
            } else if (wasImeVisible && isInternalSearchActive) {
                // Add a small delay to handle UI flicker during animations
                delay(150)
                wasImeVisible = false
                isInternalSearchActive = false
                internalSearchQuery = ""
            }
        }

        BackHandler {
            if (isInternalSearchActive) {
                isInternalSearchActive = false
                internalSearchQuery = ""
            } else {
                scope.launch {
                    val bodyText = TextEditorEngine.serializeState(editorState)
                    if (title != currentNote.title || bodyText != currentNote.body || color != currentNote.color || isLocked != currentNote.isLocked) {
                        noteRepository.saveNote(currentNote.copy(title = title, body = bodyText, color = color, isLocked = isLocked))
                    }
                    onBack()
                }
            }
        }

        // Auto-save logic
        LaunchedEffect(title, editorState.textFieldValue.text, editorState.styleRanges.size, color, isLocked) {
            val bodyText = TextEditorEngine.serializeState(editorState)
            if (title != currentNote.title || bodyText != currentNote.body || color != currentNote.color || isLocked != currentNote.isLocked) {
                delay(500)
                noteRepository.saveNote(currentNote.copy(title = title, body = bodyText, color = color, isLocked = isLocked))
            }
        }

        val backgroundColor = color?.let { Color(it) } ?: MaterialTheme.colorScheme.background

        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val bodyText = TextEditorEngine.serializeState(editorState)
                                    if (title != currentNote.title || bodyText != currentNote.body || color != currentNote.color || isLocked != currentNote.isLocked) {
                                        noteRepository.saveNote(currentNote.copy(title = title, body = bodyText, color = color, isLocked = isLocked))
                                    }
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
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
                                        // Request focus for search field
                                        scope.launch {
                                            delay(100)
                                            internalSearchFocusRequester.requestFocus()
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                if (!isLocked) {
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
                                        text = { Text("Add Tag") },
                                        onClick = { 
                                            showMenu = false
                                            showTagSelectionDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                                    )
                                }
                                
                                DropdownMenuItem(
                                    text = { Text(if (isLocked) "Unlock" else "Lock") },
                                    onClick = { 
                                        showMenu = false
                                        isLocked = !isLocked
                                    },
                                    leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null) }
                                )
                                if (!isLocked) {
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
                        onSearchActiveChange = { isInternalSearchActive = it },
                        showAddButton = false,
                        placeholder = "Find in note...",
                        focusRequester = internalSearchFocusRequester
                    )
                } else if (!isLocked) {
                    TextFormatToolbar(
                        state = editorState
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
                    enabled = !isLocked,
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

                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                
                TextField(
                    value = editorState.textFieldValue,
                    onValueChange = { newValue ->
                        editorState.onValueChanged(newValue)
                    },
                    enabled = !isLocked,
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
            // Bảo toàn toàn bộ text và style cũ
            append(text)
            
            val textString = text.text
            var startIndex = 0
            while (startIndex < textString.length) {
                val index = textString.indexOf(query, startIndex, ignoreCase = true)
                if (index == -1) break
                
                // Thêm highlight đè lên trên style cũ
                addStyle(
                    style = SpanStyle(background = highlightColor),
                    start = index,
                    end = index + query.length
                )
                startIndex = index + query.length
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
