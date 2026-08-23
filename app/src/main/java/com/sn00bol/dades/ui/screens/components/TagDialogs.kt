package com.sn00bol.dades.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.database.model.Tag

@Composable
fun CreateTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    val colors = listOf(
        0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF673AB7.toInt(),
        0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF00BCD4.toInt(),
        0xFF009688.toInt(), 0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(),
        0xFFFFC107.toInt(), 0xFFFF9800.toInt(), 0xFFFF5722.toInt()
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create new tag") },
        text = {
            Column {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select color", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Display first 6 colors for brevity in this dialog, or scrollable row
                    colors.take(6).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onConfirm(tagName, selectedColor)
                    }
                },
                enabled = tagName.isNotBlank()
            ) {
                Text("Create")
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
fun TagSelectionDialog(
    noteId: Long,
    noteTags: List<Tag>,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onTagToggle: (Tag, Boolean) -> Unit,
    onCreateNewTag: () -> Unit
) {
    val selectedTagIds = remember(noteTags) { 
        mutableStateListOf<Long>().apply { 
            addAll(noteTags.map { it.tagId }) 
        } 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                if (allTags.isEmpty()) {
                    Text(
                        "No tags created yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allTags) { tag ->
                            val isSelected = selectedTagIds.contains(tag.tagId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { 
                                        val newState = !isSelected
                                        if (newState) {
                                            selectedTagIds.add(tag.tagId)
                                        } else {
                                            selectedTagIds.remove(tag.tagId)
                                        }
                                        onTagToggle(tag, newState) 
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            selectedTagIds.add(tag.tagId)
                                        } else {
                                            selectedTagIds.remove(tag.tagId)
                                        }
                                        onTagToggle(tag, isChecked)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(tag.color))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tag.name)
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onCreateNewTag,
                    modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create new tag")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
