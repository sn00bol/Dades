package com.sn00bol.dades.ui.screens.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun FloatingNotesToolBar(
    onNewNoteClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit = {},
    onSearch: (String) -> Unit = {},
    showAddButton: Boolean = true,
    placeholder: String = "Search something...",
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    val internalFocusRequester = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: internalFocusRequester

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
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange,
            onSearch = onSearch,
            placeholder = placeholder,
            focusRequester = effectiveFocusRequester
        )

        AnimatedVisibility(
            visible = showAddButton && !isSearchActive,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut()
        ) {
            FloatingAddButton(onClick = onNewNoteClick)
        }
    }
}

@Composable
fun FloatingSearchBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit = {},
    placeholder: String = "Search...",
    focusRequester: FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current

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
                onSearchQueryChange(it)
            },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = {
                    if (searchQuery.isNotEmpty()) {
                        onSearchQueryChange("")
                    } else {
                        onSearchActiveChange(false)
                        focusManager.clearFocus()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = if (searchQuery.isNotEmpty()) "Clear search" else "Close search"
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focusState ->
                    // Only trigger active change when gaining focus.
                    // Let the parent screens handle deactivation via explicit actions (Back, Dismiss, etc.)
                    if (focusState.isFocused) {
                        onSearchActiveChange(true)
                    }
                },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
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
