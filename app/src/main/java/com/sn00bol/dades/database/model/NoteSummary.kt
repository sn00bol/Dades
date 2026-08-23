package com.sn00bol.dades.database.model

/**
 * A summary of a Note, excluding the large encrypted body.
 * Used for listing notes efficiently.
 */
data class NoteSummary(
    val id: Long,
    val title: String,
    val color: Long?,
    val isLocked: Boolean,
    val metadata: String?,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
