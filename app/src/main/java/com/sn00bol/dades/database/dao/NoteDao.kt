package com.sn00bol.dades.database.dao

import androidx.room.*
import com.sn00bol.dades.database.model.Note
import com.sn00bol.dades.database.model.NoteTagCrossRef
import com.sn00bol.dades.database.model.NoteWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteWithTags?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE id = :noteId")
    suspend fun deleteTagsForNote(noteId: Long)

    @Delete
    suspend fun deleteNote(note: Note)
}
