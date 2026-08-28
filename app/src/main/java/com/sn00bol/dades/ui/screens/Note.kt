package com.sn00bol.dades.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.TextEditor.TextEditor
import com.sn00bol.dades.TextEditor.RichTextEditorState
import com.sn00bol.dades.TextEditor.RichTextStyle
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.model.Tag
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.ui.screens.components.CreateTagDialog
import com.sn00bol.dades.ui.screens.components.FloatingNotesToolBar
import com.sn00bol.dades.ui.screens.components.TagSelectionDialog
import com.sn00bol.dades.ui.screens.components.ColorSelectionDialog
import com.sn00bol.dades.ui.theme.LocalIsDark
import com.sn00bol.dades.TextEditor.TextFormatToolbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    id: Long?,
    noteRepository: NoteRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val noteFromRepo by if (id != null) {
        noteRepository.observeNoteById(id).collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }

    var currentNote by remember { mutableStateOf(PlainNote(title = "", body = "")) }
    val editorState = remember { RichTextEditorState() }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(noteFromRepo) {
        if (noteFromRepo != null && !isInitialized) {
            currentNote = noteFromRepo!!
            editorState.onValueChanged(TextEditor.parseToState(currentNote.body).textFieldValue)
            isInitialized = true
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var color by remember(currentNote.color) { mutableStateOf(currentNote.color) }
    var isLocked by remember(currentNote.isLocked) { mutableStateOf(currentNote.isLocked) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showTagSelectionDialog by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf<List<Tag>>(emptyList()) }

    LaunchedEffect(noteFromRepo) {
        noteFromRepo?.let {
            tags = it.tags
        }
    }

    var isInternalSearchActive by remember { mutableStateOf(false) }
    var internalSearchQuery by remember { mutableStateOf("") }
    val internalSearchFocusRequester = remember { FocusRequester() }

    val backgroundColor = if (color == null || color == 0L) MaterialTheme.colorScheme.surface else Color(color!!)

    if (id == null || isInitialized) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val fullText = editorState.textFieldValue.text
                                    val titleText = fullText.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: ""
                                    val bodyText = TextEditor.serializeState(editorState)
                                    
                                    if (titleText != currentNote.title || bodyText != currentNote.body || color != currentNote.color || isLocked != currentNote.isLocked) {
                                        noteRepository.saveNote(currentNote.copy(title = titleText, body = bodyText, color = color, isLocked = isLocked))
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
                    actions = { },
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
                        state = editorState,
                        onMoreClick = { showMenu = true },
                        moreMenu = {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 0.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Find in note") },
                                    onClick = {
                                        showMenu = false
                                        isInternalSearchActive = true
                                        scope.launch {
                                            delay(100.milliseconds)
                                            internalSearchFocusRequester.requestFocus()
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                if (!isLocked) {
                                    DropdownMenuItem(
                                        text = { Text("Change color") },
                                        onClick = {
                                            showMenu = false
                                            showColorPickerDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                                    )
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

                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                val isDark = LocalIsDark.current
                val checkedColor = MaterialTheme.colorScheme.primary
                val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                
                Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = editorState.textFieldValue,
                        onValueChange = { editorState.onValueChanged(it) },
                        enabled = !isLocked,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    textLayoutResult?.let { layoutResult ->
                                        val offset = layoutResult.getOffsetForPosition(down.position)
                                        if (editorState.handleTextClick(offset)) {
                                            down.consume()
                                        }
                                    }
                                }
                            },
                        onTextLayout = { textLayoutResult = it },
                        cursorBrush = SolidColor(if (isDark) Color.White else Color.Black),
                        visualTransformation = remember(isInternalSearchActive, internalSearchQuery, checkedColor, highlightColor) {
                            val base = TextEditor.ChecklistTransformation(checkedColor)
                            if (isInternalSearchActive && internalSearchQuery.isNotEmpty()) {
                                CombinedTransformation(base, SearchHighlightTransformation(internalSearchQuery, highlightColor))
                            } else {
                                base
                            }
                        },
                        decorationBox = { innerTextField ->
                            Box {
                                if (editorState.textFieldValue.text.isEmpty()) {
                                    Text("Start noting...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

class CombinedTransformation(
    private val first: VisualTransformation,
    private val second: VisualTransformation
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val firstResult = first.filter(text)
        val secondResult = second.filter(firstResult.text)
        return TransformedText(
            text = secondResult.text,
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    secondResult.offsetMapping.originalToTransformed(firstResult.offsetMapping.originalToTransformed(offset))
                override fun transformedToOriginal(offset: Int): Int =
                    firstResult.offsetMapping.transformedToOriginal(secondResult.offsetMapping.transformedToOriginal(offset))
            }
        )
    }
}

class SearchHighlightTransformation(
    private val query: String,
    private val highlightColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            append(text)
            val textString = text.text
            var startIndex = 0
            while (startIndex < textString.length) {
                val index = textString.indexOf(query, startIndex, ignoreCase = true)
                if (index == -1) break
                addStyle(style = SpanStyle(background = highlightColor), start = index, end = index + query.length)
                startIndex = index + query.length
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
