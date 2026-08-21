package com.sn00bol.dades.database.repository

import com.sn00bol.dades.database.SecurityManager
import com.sn00bol.dades.database.dao.NoteDao
import com.sn00bol.dades.database.dao.SearchHistoryDao
import com.sn00bol.dades.database.dao.TagDao
import com.sn00bol.dades.database.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository that handles all note-related operations.
 * It automatically encrypts the body before saving and decrypts it when reading.
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val securityManager: SecurityManager
) {
    /**
     * Observes all notes and decrypts their bodies.
     */
    fun getAllNotes(): Flow<List<PlainNote>> {
        return noteDao.getAllNotesWithTags().map { list ->
            list.map { noteWithTags ->
                decryptNote(noteWithTags)
            }
        }
    }

    /**
     * Gets a single note by ID and decrypts it.
     */
    suspend fun getNoteById(id: Long): PlainNote? {
        val noteWithTags = noteDao.getNoteById(id)
        return noteWithTags?.let { decryptNote(it) }
    }

    /**
     * Saves a note (Insert or Update).
     * Encrypts the body before storing in the database.
     * Returns the ID of the saved note.
     */
    suspend fun saveNote(plainNote: PlainNote): Long {
        val encryptedBody = securityManager.encryptData(plainNote.body)
        val note = Note(
            id = plainNote.id,
            title = plainNote.title,
            encryptedBody = encryptedBody,
            color = plainNote.color,
            metadata = plainNote.metadata,
            createdAt = plainNote.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        
        val noteId = noteDao.insertNote(note)
        
        // Update tags association
        val actualNoteId = if (plainNote.id == 0L) noteId else plainNote.id
        noteDao.deleteTagsForNote(actualNoteId)
        plainNote.tags.forEach { tag ->
            noteDao.insertNoteTagCrossRef(NoteTagCrossRef(actualNoteId, tag.tagId))
        }
        return actualNoteId
    }

    /**
     * Deletes a note.
     */
    suspend fun deleteNote(plainNote: PlainNote) {
        val note = Note(
            id = plainNote.id,
            title = plainNote.title,
            encryptedBody = "", // Body not needed for deletion
            metadata = plainNote.metadata,
            createdAt = plainNote.createdAt,
            updatedAt = plainNote.updatedAt
        )
        noteDao.deleteNote(note)
        noteDao.deleteTagsForNote(plainNote.id)
    }

    fun isFirstRun() = securityManager.isFirstRun()

    fun markFirstRunCompleted() = securityManager.setFirstRunCompleted()

    // --- Search History Operations ---
    fun getRecentSearchHistory(): Flow<List<String>> {
        return searchHistoryDao.getRecentHistory().map { list ->
            list.map { it.query }
        }
    }

    suspend fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        // Delete existing to move it to the top
        searchHistoryDao.deleteByQuery(query)
        searchHistoryDao.insert(SearchHistory(query = query))
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.deleteByQuery(query)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }

    private fun decryptNote(noteWithTags: NoteWithTags): PlainNote {
        val decryptedBody = securityManager.decryptData(noteWithTags.note.encryptedBody)
        return PlainNote(
            id = noteWithTags.note.id,
            title = noteWithTags.note.title,
            body = decryptedBody,
            color = noteWithTags.note.color,
            tags = noteWithTags.tags,
            metadata = noteWithTags.note.metadata,
            createdAt = noteWithTags.note.createdAt,
            updatedAt = noteWithTags.note.updatedAt
        )
    }
}
