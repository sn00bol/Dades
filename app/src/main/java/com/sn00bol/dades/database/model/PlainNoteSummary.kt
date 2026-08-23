package com.sn00bol.dades.database.model

/**
 * Domain model representing a Note summary (no body).
 */
data class PlainNoteSummary(
    val id: Long = 0,
    val title: String,
    val color: Long? = null,
    val tags: List<Tag> = emptyList(),
    val isLocked: Boolean = false,
    val metadata: String? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
