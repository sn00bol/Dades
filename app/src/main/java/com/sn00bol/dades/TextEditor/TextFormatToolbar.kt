package com.sn00bol.dades.TextEditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.BorderStroke
import com.sn00bol.dades.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TextFormatToolbar(
    state: RichTextEditorState,
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {},
    moreMenu: @Composable () -> Unit = {}
) {
    var showAlignmentMenu by remember { mutableStateOf(false) }
    var showFormattingOverlay by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    
    val activeAlign = state.getActiveAlignment()
    
    val alignIcon = when (activeAlign) {
        TextAlign.Center -> R.drawable.align_center
        TextAlign.End -> R.drawable.align_right
        else -> R.drawable.align_left
    }

    if (showFormattingOverlay) {
        TextFormattingOverlay(
            state = state,
            onDismiss = { showFormattingOverlay = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left group: Undo & Redo
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { state.undo() }, enabled = state.canUndo(), modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = { state.redo() }, enabled = state.canRedo(), modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                }

                // Middle group: Alignment & Text Formatting
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Alignment button with dropdown
                    val isAlignActive = activeAlign != TextAlign.Start && activeAlign != TextAlign.Left
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAlignActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) {
                        IconButton(onClick = { showAlignmentMenu = true }, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                painter = painterResource(id = alignIcon),
                                contentDescription = "Alignment",
                                modifier = Modifier.size(24.dp),
                                tint = if (isAlignActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showAlignmentMenu,
                            onDismissRequest = { showAlignmentMenu = false },
                            properties = PopupProperties(focusable = false),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AlignmentPopupItem(
                                    resId = R.drawable.align_left,
                                    isActive = activeAlign == TextAlign.Start || activeAlign == TextAlign.Left,
                                    onClick = { showAlignmentMenu = false; state.applyAlignment(TextAlign.Start) }
                                )
                                AlignmentPopupItem(
                                    resId = R.drawable.align_center,
                                    isActive = activeAlign == TextAlign.Center,
                                    onClick = { showAlignmentMenu = false; state.applyAlignment(TextAlign.Center) }
                                )
                                AlignmentPopupItem(
                                    resId = R.drawable.align_right,
                                    isActive = activeAlign == TextAlign.End,
                                    onClick = { showAlignmentMenu = false; state.applyAlignment(TextAlign.End) }
                                )
                            }
                        }
                    }
                    // Text formatting button
                    IconButton(onClick = { showFormattingOverlay = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.textedit),
                            contentDescription = "Text Formatting",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right group: More (Three dots) with rounded popup
                Box {
                    IconButton(onClick = { onMoreClick(); showMoreMenu = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    ) {
                        // Insert the original moreMenu composable content here
                        moreMenu()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFormattingOverlay(
    state: RichTextEditorState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showCustomizerBySymbol by remember { mutableStateOf<String?>(null) } // null = hide, string = initial symbol

    val presets = listOf(
        TextPreset("Title", 30, true),
        TextPreset("Heading", 24, true),
        TextPreset("Subheading", 20, true),
        TextPreset("Body", 16, false),
        TextPreset("Hint", 12, false)
    )

    if (showCustomizerBySymbol != null) {
        ListStyleDialog(
            initialSymbol = showCustomizerBySymbol!!,
            onDismiss = { showCustomizerBySymbol = null },
            onApply = { state.toggleCustomBullet(it) }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.45f) // Tương tự menu chính
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f) // Tăng nhẹ để chứa thêm hàng
                .navigationBarsPadding() // Giữ vị trí ở dưới thanh điều hướng
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Preset styles row
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(presets.size) { index ->
                    val preset = presets[index]
                    TextPresetItem(
                        preset = preset,
                        isActive = state.isPresetActive(preset.size, preset.isBold),
                        onClick = { state.applyTextPreset(preset.size, preset.isBold) }
                    )
                }
            }

            // Hàng 1: Bold, Italic, Underline, Strikethrough (Ô tròn dài)
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverlayFormatButton(
                        resId = R.drawable.bold,
                        isActive = state.hasStyle(RichTextStyle.Bold),
                        onClick = { state.toggleStyle(RichTextStyle.Bold) }
                    )
                    OverlayFormatButton(
                        resId = R.drawable.italics,
                        isActive = state.hasStyle(RichTextStyle.Italic),
                        onClick = { state.toggleStyle(RichTextStyle.Italic) }
                    )
                    OverlayFormatButton(
                        resId = R.drawable.underline,
                        isActive = state.hasStyle(RichTextStyle.Underline),
                        onClick = { state.toggleStyle(RichTextStyle.Underline) }
                    )
                    OverlayFormatButton(
                        resId = R.drawable.strikethrough,
                        isActive = state.hasStyle(RichTextStyle.Strikethrough),
                        onClick = { state.toggleStyle(RichTextStyle.Strikethrough) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hàng 2: Hai ô tách biệt (Lists & Indentation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ô bên trái: DotList, NumList và Mũi tên tùy chỉnh (3 nút)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { state.toggleBulletList() }, modifier = Modifier.size(40.dp)) {
                            Icon(painter = painterResource(id = R.drawable.dotlist), contentDescription = "Bullet List", modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = { state.toggleNumberList() }, modifier = Modifier.size(40.dp)) {
                            Icon(painter = painterResource(id = R.drawable.numlist), contentDescription = "Number List", modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = { state.toggleChecklist() }, modifier = Modifier.size(40.dp)) {
                            Icon(painter = painterResource(id = R.drawable.checklist), contentDescription = "Checklist", modifier = Modifier.size(24.dp))
                        }

                        VerticalDivider(
                            modifier = Modifier.height(24.dp).padding(horizontal = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        IconButton(onClick = { showCustomizerBySymbol = "•" }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.KeyboardArrowDown, 
                                contentDescription = "Custom Options", 
                                modifier = Modifier.size(20.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Ô bên phải: Indent & Outdent (Tab)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { state.outdent() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.indentationleft), 
                                contentDescription = "Outdent",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { state.indent() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.indentationright), 
                                contentDescription = "Indent",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListStyleDialog(
    initialSymbol: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var customSymbol by remember { mutableStateOf("") }
    val predefined = listOf("•", "◦", "▪", "-", "*", "1.", "a.", "i.", "✓", "→", "●", "◆")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize List Style") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Preview Area
                Text("Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val previewSymbol = customSymbol.ifEmpty { initialSymbol }
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (previewSymbol.endsWith(" ")) previewSymbol else "$previewSymbol ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sample text line",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Predefined grid
                Text("Suggested symbols", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefined.forEach { sym ->
                        SuggestionChip(
                            onClick = { customSymbol = sym },
                            label = { Text(sym) }
                        )
                    }
                }
                
                // Custom input
                OutlinedTextField(
                    value = customSymbol,
                    onValueChange = { if (it.length <= 5) customSymbol = it },
                    label = { Text("Enter custom symbol") },
                    placeholder = { Text("e.g. >>") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalSymbol = customSymbol.ifEmpty { initialSymbol }
                    onApply(finalSymbol)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun OverlayFormatButton(resId: Int, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun AlignmentPopupItem(resId: Int, isActive: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.size(24.dp), tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class TextPreset(val name: String, val size: Int, val isBold: Boolean)

@Composable
private fun TextPresetItem(
    preset: TextPreset,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .widthIn(min = 100.dp)
            .height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = preset.size.sp,
                    fontWeight = if (preset.isBold) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
