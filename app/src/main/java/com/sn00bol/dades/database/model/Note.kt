package com.sn00bol.dades.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [androidx.room.Index(value = ["updatedAt"])]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val encryptedBody: String,
    val color: Long? = null,
    val isLocked: Boolean = false,
    val metadata: String? = null,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
