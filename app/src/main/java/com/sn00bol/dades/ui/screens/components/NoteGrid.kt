package com.sn00bol.dades.ui.screens.components

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.R
import com.sn00bol.dades.TextEditor.TextEditorEngine
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.model.Tag
import com.sn00bol.dades.ui.components.BlurWrapper
import com.sn00bol.dades.ui.components.EdgeFadeOverlay
import com.sn00bol.dades.ui.components.adaptiveBlur
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteGridPane(
    notes: List<PlainNote>,
    allTags: List<Tag> = emptyList(),
    isSearchActive: Boolean,
    blurEnabled: Boolean = true,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (PlainNote) -> Unit,
    onSaveTag: (Tag) -> Unit,
    onAddTagToNote: (Long, Long) -> Unit,
    onRemoveTagFromNote: (Long, Long) -> Unit,
    onDismissSearch: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    floatingBar: @Composable () -> Unit = {}
) {
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isGaussianSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Tag dialog states
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var noteIdForTagSelection by remember { mutableStateOf<Long?>(null) }

    val noteForTagSelection = remember(noteIdForTagSelection, notes) {
        notes.find { it.id == noteIdForTagSelection }
    }

    if (showCreateTagDialog) {
        CreateTagDialog(
            onDismiss = { showCreateTagDialog = false },
            onConfirm = { name, color ->
                onSaveTag(Tag(name = name, color = color))
                showCreateTagDialog = false
            }
        )
    }

    if (noteForTagSelection != null) {
        TagSelectionDialog(
            noteId = noteForTagSelection.id,
            noteTags = noteForTagSelection.tags,
            allTags = allTags,
            onDismiss = { noteIdForTagSelection = null },
            onTagToggle = { tag, isAdded ->
                if (isAdded) {
                    onAddTagToNote(noteForTagSelection.id, tag.tagId)
                } else {
                    onRemoveTagFromNote(noteForTagSelection.id, tag.tagId)
                }
            },
            onCreateNewTag = {
                showCreateTagDialog = true
            }
        )
    }

    val isBlurEnabled = blurEnabled
    val useGaussian = isGaussianSupported && isBlurEnabled

    val drawerBlurRadius by animateDpAsState(
        targetValue = if (drawerState.targetValue == DrawerValue.Open && useGaussian) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = if (useGaussian) 300 else 0),
        label = "DrawerBlur"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = if (useGaussian) Color.Black.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.6f),
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
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.notes),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Create new tag") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                showCreateTagDialog = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Trash") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToTrash()
                            }
                        },
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToSettings()
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Help & Support") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToHelp()
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) }
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            BlurWrapper(
                isActive = isSearchActive,
                blurEnabled = isBlurEnabled,
                onDismiss = onDismissSearch
            ) { searchBlurModifier ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .adaptiveBlur(drawerBlurRadius, forceDisabled = !isBlurEnabled)
                        .then(searchBlurModifier),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 2.dp
                ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
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
                                            .clip(CircleShape)
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
                            if (notes.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.notes),
                                        contentDescription = null,
                                        modifier = Modifier.size(120.dp),
                                        alpha = 0.5f
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "The note you just added will appear here",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
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
                                            onDelete = { onDeleteNote(note) },
                                            onAddTag = { noteIdForTagSelection = note.id }
                                        )
                                    }
                                }
                                EdgeFadeOverlay(blurEnabled = isBlurEnabled)
                            }
                        }
                    }
                )
            }
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
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
    onDelete: () -> Unit,
    onAddTag: () -> Unit
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TextEditorEngine.render(note.body),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        note.tags.take(2).forEach { tag ->
                            Surface(
                                modifier = Modifier.padding(start = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.width(160.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Add Tag") },
                onClick = {
                    showMenu = false
                    onAddTag()
                },
                enabled = !note.isLocked,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = if (note.isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                enabled = !note.isLocked,
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = if (note.isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error) }
            )
        }
    }
}