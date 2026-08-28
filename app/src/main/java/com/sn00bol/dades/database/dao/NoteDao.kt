package com.sn00bol.dades.database.dao

import androidx.room.*
import com.sn00bol.dades.database.model.Note
import com.sn00bol.dades.database.model.NoteTagCrossRef
import com.sn00bol.dades.database.model.NoteWithTags
import com.sn00bol.dades.database.model.NoteSummaryWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT id, title, color, isLocked, metadata, createdAt, updatedAt FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllNoteSummariesWithTags(): Flow<List<NoteSummaryWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteWithTags?

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNoteById(id: Long): Flow<NoteWithTags?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE id = :noteId")
    suspend fun deleteTagsForNote(noteId: Long)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("UPDATE notes SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteNote(id: Long, timestamp: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun deleteOldNotesInTrash(threshold: Long)

    @Query("DELETE FROM note_tag_cross_ref WHERE id IN (SELECT id FROM notes WHERE deletedAt IS NOT NULL)")
    suspend fun deleteTrashNoteTags()

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun deleteTrashNotes()

    @Transaction
    suspend fun emptyTrash() {
        deleteTrashNoteTags()
        deleteTrashNotes()
    }

    @Transaction
    suspend fun insertNoteWithTags(note: Note, tags: List<NoteTagCrossRef>): Long {
        val noteId = insertNote(note)
        val actualNoteId = if (note.id == 0L) noteId else note.id
        deleteTagsForNote(actualNoteId)
        tags.forEach { 
            insertNoteTagCrossRef(it.copy(id = actualNoteId))
        }
        return actualNoteId
    }
}
