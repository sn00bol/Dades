package com.sn00bol.dades.database.model

import androidx.room.Entity

@Entity(primaryKeys = ["id", "tagId"], tableName = "note_tag_cross_ref")
data class NoteTagCrossRef(
    val id: Long, // Note ID
    val tagId: Long
)
