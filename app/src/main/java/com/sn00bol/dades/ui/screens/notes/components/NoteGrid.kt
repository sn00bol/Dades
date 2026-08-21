package com.sn00bol.dades.ui.screens.notes.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.ui.components.BlurWrapper
import com.sn00bol.dades.ui.components.EdgeFadeOverlay
import com.sn00bol.dades.ui.components.adaptiveBlur
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteGridPane(
    notes: List<PlainNote>,
    isSearchActive: Boolean,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (PlainNote) -> Unit,
    onDismissSearch: () -> Unit,
    floatingBar: @Composable () -> Unit = {}
) {
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Blur radius when Drawer is open
    val drawerBlurRadius by animateDpAsState(
        targetValue = if (drawerState.targetValue == DrawerValue.Open) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "DrawerBlur"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 24.dp)
                ) {
                    NavigationDrawerItem(
                        label = { Text("Notes") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Create new tag") },
                        selected = false,
                        onClick = { /* TODO */ },
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Trash") },
                        selected = false,
                        onClick = { /* TODO */ },
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = false,
                        onClick = { /* TODO */ },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Help & Support") },
                        selected = false,
                        onClick = { /* TODO */ },
                        icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) }
                    )
                }
            }
        }
    ) {
        // Main container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Only apply Blur to main content, keep Drawer and Floating Bar clear
            BlurWrapper(
                isActive = isSearchActive,
                onDismiss = onDismissSearch
            ) { searchBlurModifier ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .adaptiveBlur(drawerBlurRadius) // Blur when Drawer open
                        .then(searchBlurModifier),      // Blur when searching
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 2.dp
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            TopAppBar(
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                title = {
                                    Text(
                                        text = "Notes",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                actions = {
                                    Box {
                                        var showTopMenu by remember { mutableStateOf(false) }
                                        IconButton(
                                            onClick = { showTopMenu = true },
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                                        }

                                        DropdownMenu(
                                            expanded = showTopMenu,
                                            onDismissRequest = { showTopMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Display settings") },
                                                onClick = { showTopMenu = false },
                                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Select notes") },
                                                onClick = { showTopMenu = false },
                                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        },
                        content = { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(
                                        top = 16.dp,
                                        bottom = 120.dp + navigationBarsPadding
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(notes, key = { it.id }) { note ->
                                        NoteItem(
                                            note = note,
                                            onClick = { onNoteClick(note.id) },
                                            onDelete = { onDeleteNote(note) }
                                        )
                                    }
                                }
                                EdgeFadeOverlay()
                            }
                        }
                    )
                }
            }

            // Floating Bar: Placed on top of Surface and below Drawer (in terms of Box hierarchy)
            // Do not apply blur here so it stays clear
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                floatingBar()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: PlainNote,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = note.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.width(160.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Add Tag") },
                onClick = { showMenu = false },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}
