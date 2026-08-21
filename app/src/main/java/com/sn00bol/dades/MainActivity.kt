package com.sn00bol.dades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sn00bol.dades.database.AppDatabase
import com.sn00bol.dades.database.SecurityManager
import com.sn00bol.dades.database.repository.NoteRepository
import com.sn00bol.dades.ui.layout.DadesApp
import com.sn00bol.dades.ui.theme.DadesProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val securityManager = SecurityManager(this)
        val database = AppDatabase.getDatabase(this)
        val noteRepository = NoteRepository(
            database.noteDao(),
            database.tagDao(),
            database.searchHistoryDao(),
            securityManager
        )

        enableEdgeToEdge()
        setContent {
            DadesProjectTheme {
                DadesApp(noteRepository)
            }
        }
    }
}
