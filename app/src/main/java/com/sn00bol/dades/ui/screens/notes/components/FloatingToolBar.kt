package com.sn00bol.dades.ui.screens.notes.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun FloatingNotesToolBar(
    onNewNoteClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit = {},
    onClose: (() -> Unit)? = null,
    showAddButton: Boolean = true,
    placeholder: String = "Search...",
    focusRequester: FocusRequester? = null,
    searchQuery: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FloatingSearchBar(
            modifier = Modifier.weight(1f),
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange,
            onSearch = onSearch,
            onClose = onClose,
            placeholder = placeholder,
            focusRequester = focusRequester,
            showCloseButton = !showAddButton,
            initialQuery = searchQuery
        )
        
        if (showAddButton) {
            FloatingAddButton(onClick = onNewNoteClick)
        }
    }
}

@Composable
fun FloatingSearchBar(
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit = {},
    onClose: (() -> Unit)? = null,
    placeholder: String = "Search...",
    focusRequester: FocusRequester? = null,
    showCloseButton: Boolean = false,
    initialQuery: String = ""
) {
    var searchQuery by remember(initialQuery) { mutableStateOf(initialQuery) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Surface(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 3.dp
    ) {
        TextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                onSearchQueryChange(it)
            },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        onSearchQueryChange("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                } else if (showCloseButton) {
                    IconButton(onClick = {
                        onClose?.invoke()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focusState ->
                    // Only activate Blur when actually typing
                    onSearchActiveChange(focusState.isFocused)
                },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    onSearch(searchQuery)
                    focusManager.clearFocus()
                }
            )
        )
    }
}

@Composable
fun FloatingAddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add new", modifier = Modifier.size(32.dp))
    }
}
