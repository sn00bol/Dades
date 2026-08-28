package com.sn00bol.dades.TextEditor.TextFont

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object TextList {
    const val BULLET_SYMBOL = "• "
    const val CHECKLIST_SYMBOL = "[ ] "
    const val CHECKED_SYMBOL = "[x] "

    data class ListChange(
        val newValue: TextFieldValue,
        val affectedLines: List<AffectedLine>
    )

    data class AffectedLine(
        val start: Int,
        val end: Int,
        val prefix: String?
    )

    /**
     * Nhận diện thông minh tiền tố của danh sách
     */
    fun findExistingPrefix(line: String): String? {
        val patterns = listOf(
            """\[x\]\s?""",
            """\[\s?\]\s?""",
            """\d+\.\s""",
            """[a-zA-Z]\.\s""",
            """\(\d+\)\s""",
            """[^\w\s]+\s"""
        )
        for (p in patterns) {
            val r = Regex("^$p")
            val match = r.find(line)
            if (match != null) return match.value
        }
        return null
    }

    fun toggleList(value: TextFieldValue, type: ListType, customSymbol: String? = null): ListChange {
        val text = value.text
        val selection = value.selection

        val startIdx = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val endIdx = text.indexOf('\n', selection.max).let { if (it == -1) text.length else it }

        val lines = text.substring(startIdx, endIdx).split('\n')
        var currentOffset = startIdx

        val affectedLines = mutableListOf<AffectedLine>()
        var newFullText = text.substring(0, startIdx)

        val targetPrefixProvider: (Int, String) -> String? = { index, line ->
            when (type) {
                ListType.Bullet -> BULLET_SYMBOL
                ListType.Number -> {
                    val prevLineNum = if (index == 0) {
                        val pStart = text.lastIndexOf('\n', (startIdx - 2).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                        if (startIdx > 0) {
                            val pLine = text.substring(pStart, startIdx - 1)
                            Regex("""^(\d+)\.\s""").find(pLine)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        } else 0
                    } else {
                        affectedLines.lastOrNull()?.prefix?.removeSuffix(". ")?.toIntOrNull() ?: 0
                    }
                    "${prevLineNum + 1}. "
                }
                ListType.Custom -> if (customSymbol?.endsWith(" ") == true) customSymbol else "$customSymbol "
                ListType.Checklist -> CHECKLIST_SYMBOL
            }
        }

        val firstLinePrefix = findExistingPrefix(lines[0])
        val isRemoving = when(type) {
            ListType.Bullet -> firstLinePrefix == BULLET_SYMBOL
            ListType.Number -> firstLinePrefix?.matches(Regex("""^\d+\.\s""")) == true
            ListType.Custom -> firstLinePrefix == (if (customSymbol?.endsWith(" ") == true) customSymbol else "$customSymbol ")
            ListType.Checklist -> firstLinePrefix == CHECKLIST_SYMBOL || firstLinePrefix == CHECKED_SYMBOL
        }

        lines.forEachIndexed { index, line ->
            val existing = findExistingPrefix(line)
            val prefixToApply = targetPrefixProvider(index, line)

            val newLineContent: String
            val finalPrefix: String?

            if (isRemoving) {
                newLineContent = if (existing != null) line.substring(existing.length) else line
                finalPrefix = null
            } else {
                newLineContent = if (existing != null) prefixToApply!! + line.substring(existing.length) else prefixToApply!! + line
                finalPrefix = prefixToApply
            }

            newFullText += newLineContent
            if (index < lines.size - 1) newFullText += "\n"

            affectedLines.add(AffectedLine(currentOffset, currentOffset + newLineContent.length, finalPrefix))
            currentOffset += newLineContent.length + 1
        }

        newFullText += text.substring(endIdx)

        val newSelection = if (selection.collapsed) {
            val prefixLen = if (isRemoving) 0 else (targetPrefixProvider(0, lines[0])?.length ?: 0)
            TextRange((startIdx + prefixLen).coerceIn(0, newFullText.length))
        } else {
            val totalAddedLen = lines.sumOf { line ->
                val existing = findExistingPrefix(line)
                val target = targetPrefixProvider(0, line) ?: ""
                (target.length - (existing?.length ?: 0))
            }
            TextRange(selection.start, (selection.end + totalAddedLen).coerceIn(0, newFullText.length))
        }

        return ListChange(value.copy(text = newFullText, selection = newSelection), affectedLines)
    }

    enum class ListType { Bullet, Number, Custom, Checklist }
}