package com.sn00bol.dades.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Relation model for NoteSummary and its associated Tags.
 */
data class NoteSummaryWithTags(
    @Embedded val note: NoteSummary,
    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(NoteTagCrossRef::class)
    )
    val tags: List<Tag>
)
