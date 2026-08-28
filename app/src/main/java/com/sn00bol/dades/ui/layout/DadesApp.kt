package com.sn00bol.dades.ui.layout

import android.annotation.SuppressLint
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.database.SettingsManager
import com.sn00bol.dades.ui.screens.NoteListDetailScreen
import com.sn00bol.dades.ui.screens.NoteDetailScreen
import com.sn00bol.dades.ui.screens.TrashScreen
import com.sn00bol.dades.ui.screens.SettingsScreen
import com.sn00bol.dades.ui.screens.HelpSupportScreen
import com.sn00bol.dades.ui.screens.TagManagementScreen
import com.sn00bol.dades.ui.screens.TagNotesScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DadesApp(noteRepository: NoteRepository, settingsManager: SettingsManager) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = "notes_grid",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable("notes_grid") {
                NoteListDetailScreen(
                    noteRepository = noteRepository,
                    settingsManager = settingsManager,
                    onNavigateToDetail = { noteId ->
                        navController.navigate("note_detail/$noteId")
                    },
                    onNavigateToTrash = {
                        navController.navigate("trash")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToHelp = {
                        navController.navigate("help_support")
                    },
                    onNavigateToManageTags = { startCreating ->
                        navController.navigate("manage_tags/$startCreating")
                    },
                    onNavigateToTag = { tagId ->
                        navController.navigate("tag_notes/$tagId")
                    },
                    onNavigateToNotes = {
                        navController.navigate("notes_grid") {
                            popUpTo("notes_grid") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "note_detail/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId")
                NoteDetailScreen(
                    id = noteId,
                    noteRepository = noteRepository,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("trash") {
                TrashScreen(
                    noteRepository = noteRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    settingsManager = settingsManager,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("help_support") {
                HelpSupportScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "manage_tags/{startCreating}",
                arguments = listOf(navArgument("startCreating") { type = NavType.BoolType })
            ) { backStackEntry ->
                val startCreating = backStackEntry.arguments?.getBoolean("startCreating") ?: false
                TagManagementScreen(
                    noteRepository = noteRepository,
                    onBack = { navController.popBackStack() },
                    startCreatingInitially = startCreating
                )
            }
            composable(
                route = "tag_notes/{tagId}",
                arguments = listOf(navArgument("tagId") { type = NavType.LongType })
            ) { backStackEntry ->
                val tagId = backStackEntry.arguments?.getLong("tagId") ?: 0L
                TagNotesScreen(
                    tagId = tagId,
                    noteRepository = noteRepository,
                    settingsManager = settingsManager,
                    onNavigateToDetail = { noteId ->
                        navController.navigate("note_detail/$noteId")
                    },
                    onNavigateToTrash = { navController.navigate("trash") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToHelp = { navController.navigate("help_support") },
                    onNavigateToManageTags = { startCreating ->
                        navController.navigate("manage_tags/$startCreating")
                    },
                    onNavigateToTag = { newTagId ->
                        navController.navigate("tag_notes/$newTagId")
                    },
                    onNavigateToNotes = {
                        navController.navigate("notes_grid") {
                            popUpTo("notes_grid") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
