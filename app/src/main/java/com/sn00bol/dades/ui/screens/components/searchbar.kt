package com.sn00bol.dades.ui.screens.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun SelectionToolBar(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    onCancel: () -> Unit,
    onPin: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onColor: () -> Unit = {},
    onTags: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
            
            Text(
                text = selectedCount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onPin) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pin")
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                }
                IconButton(onClick = onColor) {
                    Icon(Icons.Default.Palette, contentDescription = "Change color")
                }
                IconButton(onClick = onTags) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tags")
                }
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

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
    focusRequester: FocusRequester? = null,
    showSearchBar: Boolean = true
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
        horizontalArrangement = if (showSearchBar) Arrangement.spacedBy(12.dp) else Arrangement.End
    ) {
        if (showSearchBar) {
            FloatingSearchBar(
                modifier = Modifier.weight(1f),
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchActiveChange = onSearchActiveChange,
                onSearch = onSearch,
                placeholder = placeholder,
                focusRequester = effectiveFocusRequester
            )
        }

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
