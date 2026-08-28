package com.sn00bol.dades.ui.screens.components

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.R
import com.sn00bol.dades.database.model.PlainNote
import com.sn00bol.dades.database.model.Tag
import com.sn00bol.dades.ui.components.BlurWrapper
import com.sn00bol.dades.ui.components.adaptiveBlur
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteGridPane(
    notes: List<PlainNote>,
    allTags: List<Tag> = emptyList(),
    isSearchActive: Boolean,
    blurEnabled: Boolean = true,
    currentTagId: Long? = null,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (PlainNote) -> Unit,
    onSaveTag: (Tag) -> Unit,
    onAddTagToNote: (Long, Long) -> Unit,
    onRemoveTagFromNote: (Long, Long) -> Unit,
    onDismissSearch: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToManageTags: (Boolean) -> Unit,
    onNavigateToTag: (Long) -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    title: String? = "Notes",
    moreVertActions: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit = { onDismiss ->
        DropdownMenuItem(
            text = { Text("Display settings") },
            onClick = { onDismiss() },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Select notes") },
            onClick = { onDismiss() },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
        )
    },
    floatingBar: @Composable () -> Unit = {},
    // Selection Actions
    onDeleteNotes: (Set<Long>) -> Unit = {},
    onDuplicateNotes: (Set<Long>) -> Unit = {},
    onUpdateNotesColor: (Set<Long>, Long?) -> Unit = { _, _ -> },
    onUpdateNotesTags: (Set<Long>) -> Unit = {}
) {
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isGaussianSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Selection state
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    // Tag selection state
    var noteIdForTagSelection by remember { mutableStateOf<Long?>(null) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Clear selection on back press
    androidx.activity.compose.BackHandler(enabled = isSelectionMode) {
        selectedIds = emptySet()
    }

    val noteForTagSelection = remember(noteIdForTagSelection, notes) {
        notes.find { it.id == noteIdForTagSelection }
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
                onNavigateToManageTags(true)
            }
        )
    }

    if (showBatchTagDialog) {
        TagSelectionDialog(
            noteId = -1,
            noteTags = emptyList(),
            allTags = allTags,
            onDismiss = { showBatchTagDialog = false },
            onTagToggle = { tag, isAdded ->
                selectedIds.forEach { id ->
                    if (isAdded) {
                        onAddTagToNote(id, tag.tagId)
                    } else {
                        onRemoveTagFromNote(id, tag.tagId)
                    }
                }
            },
            onCreateNewTag = {
                onNavigateToManageTags(true)
            }
        )
    }

    if (showColorPicker) {
        ColorSelectionDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                onUpdateNotesColor(selectedIds, color)
                selectedIds = emptySet()
                showColorPicker = false
            }
        )
    }

    val useGaussian = isGaussianSupported && blurEnabled

    val drawerBlurRadius by animateDpAsState(
        targetValue = if (drawerState.targetValue == DrawerValue.Open && useGaussian) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = if (useGaussian) 300 else 0),
        label = "DrawerBlur"
    )

    var showTopMenu by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val topBarHeight = 64.dp
    val topBarHeightPx = with(density) { topBarHeight.toPx() }
    var scrollOffsetPx by remember { mutableStateOf(topBarHeightPx) }

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()

    val animatedTranslationY by animateDpAsState(
        targetValue = if (isSelectionMode) 0.dp else with(density) { (scrollOffsetPx + statusBarHeightPx).toDp() },
        animationSpec = if (isSelectionMode) tween(300) else snap(),
        label = "OverlayTranslation"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isSelectionMode) 0.dp else 32.dp,
        animationSpec = tween(durationMillis = 300),
        label = "CornerRadius"
    )

    // Reset offset if notes are few
    LaunchedEffect(notes.size) {
        if (notes.size <= 6) {
            scrollOffsetPx = topBarHeightPx
        }
    }

    val nestedScrollConnection = remember(notes.size, topBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isSelectionMode) return Offset.Zero
                // Only allow pulling up if there are many notes
                if (available.y < 0 && notes.size <= 6) return Offset.Zero
                
                return if (available.y < 0) {
                    val delta = available.y
                    val newOffset = (scrollOffsetPx + delta).coerceIn(0f, topBarHeightPx)
                    val consumed = newOffset - scrollOffsetPx
                    scrollOffsetPx = newOffset
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isSelectionMode) return Offset.Zero
                return if (available.y > 0) {
                    val delta = available.y
                    val newOffset = (scrollOffsetPx + delta).coerceIn(0f, topBarHeightPx)
                    val consumedDelta = newOffset - scrollOffsetPx
                    scrollOffsetPx = newOffset
                    Offset(0f, consumedDelta)
                } else {
                    Offset.Zero
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isSelectionMode,
        scrimColor = if (useGaussian) Color.Black.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    NavigationDrawerItem(
                        label = { Text("Notes") },
                        selected = title == "Notes",
                        onClick = { 
                            scope.launch { 
                                drawerState.close() 
                                if (title != "Notes") {
                                    onNavigateToNotes()
                                }
                            } 
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.notes),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                    
                    if (allTags.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Editing",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.clickable { 
                                    scope.launch {
                                        drawerState.close()
                                        onNavigateToManageTags(false)
                                    }
                                }
                            )
                        }

                        allTags.forEach { tag ->
                            val isSelected = currentTagId == tag.tagId
                            NavigationDrawerItem(
                                label = { Text(tag.name) },
                                selected = isSelected,
                                onClick = { 
                                    scope.launch {
                                        drawerState.close()
                                        if (!isSelected) {
                                            onNavigateToTag(tag.tagId)
                                        }
                                    }
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        NavigationDrawerItem(
                            label = { Text("Create new tag") },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToManageTags(true)
                                }
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        NavigationDrawerItem(
                            label = { Text("Create new tag") },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToManageTags(true)
                                }
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                        )
                    }

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
                        icon = { Icon(Icons.Default.Help, contentDescription = null) }
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
            // Z=0: Fixed Title
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 18.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Z=1: Scrolling Overlay
            BlurWrapper(
                isActive = isSearchActive,
                blurEnabled = blurEnabled,
                onDismiss = onDismissSearch
            ) { searchBlurModifier ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = with(density) { animatedTranslationY.toPx() }
                        }
                        .adaptiveBlur(drawerBlurRadius, forceDisabled = !blurEnabled)
                        .nestedScroll(nestedScrollConnection)
                        .then(searchBlurModifier),
                    shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp
                ) {
                    Scaffold(
                        modifier = Modifier.statusBarsPadding(),
                        containerColor = Color.Transparent,
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
                                    LazyVerticalStaggeredGrid(
                                        columns = StaggeredGridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentPadding = PaddingValues(
                                            top = 24.dp,
                                            bottom = 120.dp + navigationBarsPadding
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalItemSpacing = 12.dp
                                    ) {
                                        items(notes, key = { it.id }) { note ->
                                            NoteItem(
                                                note = note,
                                                isSelected = selectedIds.contains(note.id),
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        selectedIds = if (selectedIds.contains(note.id)) {
                                                            selectedIds - note.id
                                                        } else {
                                                            selectedIds + note.id
                                                        }
                                                    } else {
                                                        onNoteClick(note.id)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!selectedIds.contains(note.id)) {
                                                        selectedIds = selectedIds + note.id
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Z=2: Fixed Buttons
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Actions",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            moreVertActions { showTopMenu = false }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AnimatedContent(
                    targetState = isSelectionMode,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                    },
                    label = "ToolBarTransition"
                ) { selectionMode ->
                    if (selectionMode) {
                        SelectionToolBar(
                            selectedCount = selectedIds.size,
                            onCancel = { selectedIds = emptySet() },
                            onPin = { /* Blank for now */ },
                            onDuplicate = { 
                                onDuplicateNotes(selectedIds)
                                selectedIds = emptySet()
                            },
                            onColor = { 
                                showColorPicker = true
                            },
                            onTags = { 
                                showBatchTagDialog = true
                            },
                            onDelete = {
                                onDeleteNotes(selectedIds)
                                selectedIds = emptySet()
                            }
                        )
                    } else {
                        floatingBar()
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItem(
    note: PlainNote,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    NoteCard(
        note = note,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        showTags = 2,
        showLock = true
    )
}
