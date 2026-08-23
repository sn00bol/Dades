package com.sn00bol.dades.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sn00bol.dades.database.dao.NoteDao
import com.sn00bol.dades.database.dao.TagDao
import com.sn00bol.dades.database.dao.SearchHistoryDao
import com.sn00bol.dades.database.model.Note
import com.sn00bol.dades.database.model.NoteTagCrossRef
import com.sn00bol.dades.database.model.SearchHistory
import com.sn00bol.dades.database.model.Tag
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [Note::class, Tag::class, NoteTagCrossRef::class, SearchHistory::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher libs
                System.loadLibrary("sqlcipher")
                
                val securityManager = SecurityManager(context)
                val passphrase = securityManager.getDatabasePassphrase()
                val factory = SupportOpenHelperFactory(passphrase.toByteArray())
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dades_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
