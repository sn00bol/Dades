package com.sn00bol.dades.database.model

/**
 * Domain model representing a Note with decrypted content.
 * This is what the UI will interact with.
 */
data class PlainNote(
    val id: Long = 0,
    val title: String,
    val body: String,
    val color: Long? = null,
    val tags: List<Tag> = emptyList(),
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
