package com.sn00bol.dades.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.database.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val themeMode by settingsManager.themeMode.collectAsState(initial = "System")
    val blurEnabled by settingsManager.blurEnabled.collectAsState(initial = true)
    val trashDays by settingsManager.trashAutoDeleteDays.collectAsState(initial = 30)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }

    val appVersion = remember {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "Unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            SettingsCategoryGroup(
                title = "Appearance"
            ) {
                SettingsClickableItem(
                    icon = Icons.Default.ColorLens,
                    title = "Theme",
                    subtitle = themeMode,
                    onClick = {
                        showThemeDialog = true
                    }
                )

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Icons.Default.BlurOn,
                    title = "Blur Effect",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Turn off if you experience lag" else "Not supported on this device",
                    checked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurEnabled else false,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            settingsManager.setBlurEnabled(enabled)
                        }
                    }
                )
            }
            SettingsCategoryGroup(
                title = "Notes & Data"
            ) {
                SettingsClickableItem(
                    icon = Icons.Default.Delete,
                    title = "Auto-delete Trash",
                    subtitle = when (trashDays) {
                        0 -> "Never"
                        1 -> "1 day"
                        else -> "$trashDays days"
                    },
                    onClick = {
                        showTrashDialog = true
                    }
                )
            }
            SettingsCategoryGroup(
                title = "About"
            ) {
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = appVersion,
                    onClick = {}
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
    }
    if (showThemeDialog) {
        SettingsSelectionDialog(
            title = "Select Theme",
            options = listOf(
                "System",
                "Light",
                "Dark"
            ),
            selectedOption = themeMode,
            onOptionSelected = { mode ->
                scope.launch {
                    settingsManager.setThemeMode(mode)
                }

                showThemeDialog = false
            },
            onDismiss = {
                showThemeDialog = false
            }
        )
    }

    if (showTrashDialog) {
        val trashOptions = listOf(
            30 to "30 days",
            60 to "60 days",
            90 to "90 days",
            0 to "Never"
        )

        AlertDialog(
            onDismissRequest = {
                showTrashDialog = false
            },

            title = {
                Text(
                    text = "Auto-delete Trash",
                    style = MaterialTheme.typography.headlineSmall
                )
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    trashOptions.forEach { (days, label) ->

                        SettingsSelectionRow(
                            label = label,
                            selected = trashDays == days,
                            onClick = {
                                scope.launch {
                                    settingsManager.setTrashAutoDeleteDays(days)
                                }

                                showTrashDialog = false
                            }
                        )
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showTrashDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = 4.dp
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}
@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },

        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },

        leadingContent = {
            SettingsIcon(
                icon = icon
            )
        },

        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),

        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },

        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },

        leadingContent = {
            SettingsIcon(
                icon = icon
            )
        },

        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },

        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),

        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled
            ) {
                onCheckedChange(!checked)
            }
    )
}

@Composable
private fun SettingsIcon(
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            horizontal = 16.dp
        ),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingsSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->

                    SettingsSelectionRow(
                        label = option,
                        selected = selectedOption == option,
                        onClick = {
                            onOptionSelected(option)
                        }
                    )
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsSelectionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Spacer(
            modifier = Modifier.size(8.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}