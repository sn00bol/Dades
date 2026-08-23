package com.sn00bol.dades.TextEditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.sn00bol.dades.R

@Composable
fun TextFormatToolbar(
    state: RichTextEditorState,
    modifier: Modifier = Modifier
) {
    var showAlignmentMenu by remember { mutableStateOf(false) }
    val activeAlign = state.getActiveAlignment()
    
    val alignIcon = when (activeAlign) {
        TextAlign.Center -> R.drawable.align_center
        TextAlign.End -> R.drawable.align_right
        else -> R.drawable.align_left
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Alignment
                val isAlignActive = activeAlign != TextAlign.Start && activeAlign != TextAlign.Left
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isAlignActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)) {
                    IconButton(onClick = { showAlignmentMenu = true }, modifier = Modifier.fillMaxSize()) {
                        Icon(painter = painterResource(id = alignIcon), contentDescription = "Alignment", modifier = Modifier.size(24.dp), tint = if (isAlignActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showAlignmentMenu, onDismissRequest = { showAlignmentMenu = false }, properties = PopupProperties(focusable = false), modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AlignmentPopupItem(resId = R.drawable.align_left, isActive = activeAlign == TextAlign.Start || activeAlign == TextAlign.Left, onClick = { showAlignmentMenu = false; state.applyAlignment(TextAlign.Start) })
                            AlignmentPopupItem(resId = R.drawable.align_center, isActive = activeAlign == TextAlign.Center, onClick = { showAlignmentMenu = false; state.applyAlignment(TextAlign.Center) })
                            AlignmentPopupItem(resId = R.drawable.align_right, isActive = activeAlign == TextAlign.End, onClick = { showAlignmentMenu = false; state.applyAlignment(TextAlign.End) })
                        }
                    }
                }

                VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))

                // 2. Text Size
                IconButton(onClick = { state.changeSize(false) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Size", modifier = Modifier.size(20.dp))
                }
                Text(
                    text = state.getCurrentSize().toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.widthIn(min = 20.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { state.changeSize(true) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Size", modifier = Modifier.size(20.dp))
                }

                VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))

                // 3. Formats
                FormatButton(icon = Icons.Default.FormatBold, isActive = state.hasStyle(RichTextStyle.Bold), onClick = { state.toggleStyle(RichTextStyle.Bold) })
                FormatButton(icon = Icons.Default.FormatItalic, isActive = state.hasStyle(RichTextStyle.Italic), onClick = { state.toggleStyle(RichTextStyle.Italic) })
                FormatButton(icon = Icons.Default.FormatUnderlined, isActive = state.hasStyle(RichTextStyle.Underline), onClick = { state.toggleStyle(RichTextStyle.Underline) })
                
                FormatButton(
                    painter = painterResource(id = R.drawable.strikethrough),
                    isActive = state.hasStyle(RichTextStyle.Strikethrough),
                    onClick = { state.toggleStyle(RichTextStyle.Strikethrough) }
                )
            }
        }
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

@Composable
private fun FormatButton(icon: ImageVector? = null, painter: androidx.compose.ui.graphics.painter.Painter? = null, isActive: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (painter != null) {
                Icon(painter = painter, contentDescription = null, tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
        }
    }
}
