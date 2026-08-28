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
    private val decryptionCache = mutableMapOf<Long, Pair<Long, String>>() // noteId -> (updatedAt, decryptedBody)

    /**
     * Observes all notes and decrypts their bodies efficiently using a cache.
     */
    fun getAllNotes(): Flow<List<PlainNote>> {
        return noteDao.getAllNotesWithTags().map { list ->
            list.map { noteWithTags ->
                val cached = decryptionCache[noteWithTags.note.id]
                val decryptedBody = if (cached != null && cached.first == noteWithTags.note.updatedAt) {
                    cached.second
                } else {
                    securityManager.decryptData(noteWithTags.note.encryptedBody).also {
                        decryptionCache[noteWithTags.note.id] = noteWithTags.note.updatedAt to it
                    }
                }
                
                PlainNote(
                    id = noteWithTags.note.id,
                    title = noteWithTags.note.title,
                    body = decryptedBody,
                    color = noteWithTags.note.color,
                    tags = noteWithTags.tags,
                    isLocked = noteWithTags.note.isLocked,
                    metadata = noteWithTags.note.metadata,
                    deletedAt = noteWithTags.note.deletedAt,
                    createdAt = noteWithTags.note.createdAt,
                    updatedAt = noteWithTags.note.updatedAt
                )
            }
        }
    }

    /**
     * Observes notes in trash.
     */
    fun getTrashNotes(): Flow<List<PlainNote>> {
        return noteDao.getTrashNotesWithTags().map { list ->
            list.map { noteWithTags ->
                val cached = decryptionCache[noteWithTags.note.id]
                val decryptedBody = if (cached != null && cached.first == noteWithTags.note.updatedAt) {
                    cached.second
                } else {
                    securityManager.decryptData(noteWithTags.note.encryptedBody).also {
                        decryptionCache[noteWithTags.note.id] = noteWithTags.note.updatedAt to it
                    }
                }

                PlainNote(
                    id = noteWithTags.note.id,
                    title = noteWithTags.note.title,
                    body = decryptedBody,
                    color = noteWithTags.note.color,
                    tags = noteWithTags.tags,
                    isLocked = noteWithTags.note.isLocked,
                    metadata = noteWithTags.note.metadata,
                    deletedAt = noteWithTags.note.deletedAt,
                    createdAt = noteWithTags.note.createdAt,
                    updatedAt = noteWithTags.note.updatedAt
                )
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

    fun observeNoteById(id: Long): Flow<PlainNote?> {
        return noteDao.observeNoteById(id).map { it?.let { decryptNote(it) } }
    }

    /**
     * Observes note summaries (no decryption required).
     * Extremely fast for simple lists.
     */
    fun getAllNoteSummaries(): Flow<List<PlainNoteSummary>> {
        return noteDao.getAllNoteSummariesWithTags().map { list ->
            list.map { summaryWithTags ->
                PlainNoteSummary(
                    id = summaryWithTags.note.id,
                    title = summaryWithTags.note.title,
                    color = summaryWithTags.note.color,
                    tags = summaryWithTags.tags,
                    isLocked = summaryWithTags.note.isLocked,
                    metadata = summaryWithTags.note.metadata,
                    deletedAt = summaryWithTags.note.deletedAt,
                    createdAt = summaryWithTags.note.createdAt,
                    updatedAt = summaryWithTags.note.updatedAt
                )
            }
        }
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
            isLocked = plainNote.isLocked,
            metadata = plainNote.metadata,
            deletedAt = plainNote.deletedAt,
            createdAt = plainNote.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        
        val crossRefs = plainNote.tags.map { NoteTagCrossRef(note.id, it.tagId) }
        return noteDao.insertNoteWithTags(note, crossRefs)
    }

    /**
     * Moves a note to trash.
     */
    suspend fun moveNoteToTrash(noteId: Long) {
        noteDao.softDeleteNote(noteId, System.currentTimeMillis())
    }

    /**
     * Restores a note from trash.
     */
    suspend fun restoreNote(noteId: Long) {
        noteDao.restoreNote(noteId)
    }

    /**
     * Deletes a note permanently.
     */
    suspend fun deleteNotePermanently(noteId: Long) {
        val note = noteDao.getNoteById(noteId)?.note ?: return
        noteDao.deleteNote(note)
        noteDao.deleteTagsForNote(noteId)
    }

    /**
     * Deletes all notes in trash permanently.
     */
    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    /**
     * Clears old notes from trash based on days threshold.
     * @param days 0 or negative means never delete.
     */
    suspend fun clearOldTrash(days: Int) {
        if (days <= 0) return
        val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        noteDao.deleteOldNotesInTrash(threshold)
    }

    /**
     * Legacy delete - now moves to trash by default.
     */
    suspend fun deleteNote(plainNote: PlainNote) {
        moveNoteToTrash(plainNote.id)
    }

    suspend fun duplicateNote(noteId: Long): Long {
        val original = getNoteById(noteId) ?: return -1
        return saveNote(
            original.copy(
                id = 0,
                title = original.title + " (Copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun isFirstRun() = securityManager.isFirstRun()

    fun markFirstRunCompleted() = securityManager.setFirstRunCompleted()

    // --- Tag Operations ---
    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun saveTag(tag: Tag): Long {
        return tagDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag)
    }

    suspend fun addTagToNote(noteId: Long, tagId: Long) {
        noteDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    suspend fun removeTagFromNote(noteId: Long, tagId: Long) {
        noteDao.deleteNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

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
            isLocked = noteWithTags.note.isLocked,
            metadata = noteWithTags.note.metadata,
            deletedAt = noteWithTags.note.deletedAt,
            createdAt = noteWithTags.note.createdAt,
            updatedAt = noteWithTags.note.updatedAt
        )
    }
}
