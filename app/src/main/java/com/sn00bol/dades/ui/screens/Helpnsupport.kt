package com.sn00bol.dades.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sn00bol.dades.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HelpItem(
                icon = Icons.Default.BugReport,
                title = "Report issues",
                subtitle = "Found a bug? report it here",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sn00bol/Dades/issues"))
                    context.startActivity(intent)
                }
            )

            HelpItem(
                icon = Icons.Default.Email,
                title = "Email",
                subtitle = "trancongbinhan2016@gmail.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:trancongbinhan2016@gmail.com")
                    }
                    context.startActivity(intent)
                }
            )

            HelpItem(
                painter = painterResource(id = R.drawable.discord),
                title = "Discord",
                subtitle = "sn00bol",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com/users/870567726324805673"))
                    context.startActivity(intent)
                }
            )

            HelpItem(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "Telegram",
                subtitle = "@Snoobol",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sn00bol"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun HelpItem(
    icon: ImageVector? = null,
    painter: Painter? = null,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            when {
                icon != null -> Icon(icon, contentDescription = null)
                painter != null -> Icon(painter, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
