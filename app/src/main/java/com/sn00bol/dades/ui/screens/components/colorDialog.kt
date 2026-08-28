package com.sn00bol.dades.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.ui.theme.LocalIsDark

@Composable
fun ColorSelectionDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Long?) -> Unit
) {
    val isDark = LocalIsDark.current
    val colors = if (isDark) {
        com.sn00bol.dades.ui.theme.DarkNoteColors
    } else {
        com.sn00bol.dades.ui.theme.LightNoteColors
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                itemsIndexed(colors) { index, colorValue ->
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                colorValue?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable {
                                onColorSelected(if (index == 0) null else index.toLong())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 0) {
                            Icon(Icons.Default.Block, contentDescription = "Default", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
