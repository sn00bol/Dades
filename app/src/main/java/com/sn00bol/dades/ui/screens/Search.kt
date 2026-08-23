package com.sn00bol.dades.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.sn00bol.dades.TextEditor.TextEditorEngine
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.model.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    notes: List<PlainNote>,
    history: List<String>,
    allTags: List<Tag>,
    searchQuery: String,
    selectedColor: Long?,
    selectedTagId: Long?,
    onSearchQueryChange: (String) -> Unit,
    onColorSelect: (Long?) -> Unit,
    onTagSelect: (Long?) -> Unit,
    onHistorySelect: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (PlainNote) -> Unit,
    onAddTagToNote: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var searchFocused by remember {
        mutableStateOf(false)
    }
    val filteredNotes = remember(
        notes,
        searchQuery,
        selectedColor,
        selectedTagId
    ) {
        notes.filter { note ->

            val matchesQuery =
                if (searchQuery.isBlank()) {
                    true
                } else {
                    note.title.contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                            note.body.contains(
                                searchQuery,
                                ignoreCase = true
                            ) ||
                            note.tags.any {
                                it.name.contains(
                                    searchQuery,
                                    ignoreCase = true
                                )
                            }
                }

            val matchesColor =
                selectedColor == null ||
                        note.color == selectedColor

            val matchesTag =
                if (selectedTagId == null) {
                    true
                } else {
                    note.tags.any {
                        it.tagId == selectedTagId
                    }
                }

            matchesQuery &&
                    matchesColor &&
                    matchesTag
        }
    }

    BackHandler {
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val activeColors = remember(notes) {
        notes.mapNotNull { it.color }.distinct()
    }

    val showResults = searchQuery.isNotBlank() || selectedColor != null || selectedTagId != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onDismiss()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector =
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search something...",
                                color = MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                                style = MaterialTheme
                                    .typography
                                    .titleLarge,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                onSearchQueryChange(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(
                                    focusRequester
                                )
                                .onFocusChanged {
                                    searchFocused =
                                        it.isFocused
                                },
                            singleLine = true,
                            textStyle =
                            MaterialTheme
                                .typography
                                .titleLarge
                                .copy(
                                    color = MaterialTheme
                                        .colorScheme
                                        .onSurface,
                                    fontWeight = FontWeight.Normal
                                ),
                            cursorBrush = SolidColor(
                                MaterialTheme
                                    .colorScheme
                                    .primary
                            ),
                            keyboardOptions =
                            KeyboardOptions(
                                imeAction =
                                ImeAction.Search
                            ),
                            keyboardActions =
                            KeyboardActions(
                                onSearch = {
                                    focusManager
                                        .clearFocus()
                                }
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty() || selectedColor != null || selectedTagId != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    onSearchQueryChange("")
                                }
                                onColorSelect(null)
                                onTagSelect(null)
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector =
                                Icons.Rounded.Clear,
                                contentDescription =
                                "Clear search"
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            /*
             * Search content
             */
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 32.dp
                ),
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {

                if (!showResults) {
                    
                    // Tags Section
                    if (allTags.isNotEmpty()) {
                        item {
                            SuggestionSection(title = "Tags") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(allTags) { tag ->
                                        FilterChip(
                                            selected = false,
                                            onClick = { onTagSelect(tag.tagId) },
                                            label = { Text(tag.name) },
                                            leadingIcon = { Icon(Icons.Rounded.Tag, null, modifier = Modifier.size(18.dp)) },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Colors Section
                    if (activeColors.isNotEmpty()) {
                        item {
                            SuggestionSection(title = "Colors") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(activeColors) { colorVal ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(colorVal), shape = RoundedCornerShape(12.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.Black.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { onColorSelect(colorVal) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent Searches Section
                    if (history.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 16.dp,
                                        bottom = 8.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent searches",
                                    style = MaterialTheme
                                        .typography
                                        .titleMedium,
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    modifier =
                                        Modifier.weight(1f)
                                )

                                TextButton(
                                    onClick =
                                        onClearHistory
                                ) {
                                    Text("Clear")
                                }
                            }
                        }

                        items(
                            items = history,
                            key = {
                                "history_$it"
                            }
                        ) { query ->

                            SearchHistoryItem(
                                query = query,
                                onClick = {
                                    onHistorySelect(
                                        query
                                    )
                                },
                                onDelete = {
                                    onDeleteHistory(
                                        query
                                    )
                                }
                            )
                        }
                    }

                    if (history.isEmpty() && allTags.isEmpty() && activeColors.isEmpty()) {
                        item {
                            EmptySearchState()
                        }
                    }
                }

                if (showResults) {

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (filteredNotes.isEmpty()) "No results" else "Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            if (selectedColor != null || selectedTagId != null) {
                                TextButton(onClick = {
                                    onColorSelect(null)
                                    onTagSelect(null)
                                }) {
                                    Text("Clear filters")
                                }
                            }
                        }
                    }

                    items(
                        items = filteredNotes,
                        key = {
                            "note_${it.id}"
                        }
                    ) { note ->

                        SearchResultItem(
                            note = note,
                            query = searchQuery,
                            onClick = {
                                onNoteClick(note.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SearchHistoryItem(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme
                .colorScheme
                .onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(
            modifier = Modifier.width(16.dp)
        )

        Text(
            text = query,
            style = MaterialTheme
                .typography
                .bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        IconButton(
            onClick = onDelete
        ) {
            Icon(
                imageVector =
                    Icons.Rounded.DeleteOutline,
                contentDescription =
                    "Remove search"
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    note: PlainNote,
    query: String,
    onClick: () -> Unit
) {
    val annotatedBody = remember(note.body, query) {
        val base = TextEditorEngine.render(note.body)
        if (query.isBlank()) base
        else buildAnnotatedString {
            append(base)
            val text = base.text
            var startIndex = 0
            while (startIndex < text.length) {
                val index = text.indexOf(query, startIndex, ignoreCase = true)
                if (index == -1) break
                addStyle(
                    SpanStyle(background = Color.Yellow.copy(alpha = 0.3f)),
                    index,
                    index + query.length
                )
                startIndex = index + query.length
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(20.dp)
            )
            .background(
                MaterialTheme
                    .colorScheme
                    .surfaceContainer
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {

        if (note.title.isNotBlank()) {
            Text(
                text = note.title,
                style = MaterialTheme
                    .typography
                    .titleMedium,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )
        }

        if (note.body.isNotBlank()) {
            Text(
                text = annotatedBody,
                style = MaterialTheme
                    .typography
                    .bodyMedium,
                color = MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
                maxLines = 3
            )
        }

        if (note.tags.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                note.tags
                    .take(3)
                    .forEach { tag ->

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = tag.name,
                                    maxLines = 1
                                )
                            }
                        )
                    }
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 80.dp,
                start = 32.dp,
                end = 32.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector =
                Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme
                .colorScheme
                .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Search your notes",
            style = MaterialTheme
                .typography
                .titleMedium,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "Search by title, content, or tag.",
            style = MaterialTheme
                .typography
                .bodyMedium,
            color = MaterialTheme
                .colorScheme
                .onSurfaceVariant
        )
    }
}
