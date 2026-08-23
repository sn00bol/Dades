package com.sn00bol.dades.TextEditor.TextFont

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp

object TextSize {
    // Standardize Regex: Bắt cả dạng [s16], [s:16]
    val REGEX = Regex("""(?s)\[s:?(\d+)\](.*?)\[/s\]""")
    val START_TAG_REGEX = Regex("""\[s:?\d+\]""")
    val END_TAG_REGEX = Regex("""\[/s\]""")

    fun getStyle(size: Int): SpanStyle = SpanStyle(fontSize = size.sp)

    fun getNextSize(current: Int): Int {
        if (current >= 100) return 100
        var size = 16
        var step = 1
        while (size <= current) {
            val increment = 2 * ((step + 1) / 2)
            size += increment
            step++
            if (size > 100) break
        }
        return size.coerceAtMost(100)
    }

    fun getPrevSize(current: Int): Int {
        if (current <= 12) return 12
        val sizes = mutableListOf(12, 14, 16)
        var lastSize = 16
        var step = 1
        while (lastSize < 100) {
            val increment = 2 * ((step + 1) / 2)
            lastSize += increment
            sizes.add(lastSize)
            step++
        }
        return sizes.filter { it < current }.lastOrNull() ?: 12
    }

    /**
     * Áp dụng size mới cho TextFieldValue: Tự động loại bỏ thẻ size cũ bị bọc trùng
     */
    fun applySize(value: TextFieldValue, newSize: Int): TextFieldValue {
        val text = value.text
        val selection = value.selection

        // Nếu không bôi đen văn bản, tìm từ/khối nằm trong thẻ hiện tại
        var start = selection.start
        var end = selection.end

        if (selection.collapsed) {
            val activeMatch = findActiveMatch(text, start)
            if (activeMatch != null) {
                // Lấy phần text bên trong thẻ cũ
                val innerText = activeMatch.groupValues[2]
                val newTag = "[s:$newSize]$innerText[/s]"
                val newText = text.replaceRange(activeMatch.range, newTag)

                val newCursor = activeMatch.range.first + newTag.length
                return value.copy(
                    text = newText,
                    selection = TextRange(newCursor)
                )
            } else {
                return value
            }
        }

        // Trường hợp bôi đen đoạn văn bản: Xóa sạch các thẻ size cũ bên trong vùng chọn trước khi bọc thẻ mới
        val selectedText = text.substring(start, end)
        val cleanedSelectedText = selectedText
            .replace(START_TAG_REGEX, "")
            .replace(END_TAG_REGEX, "")

        val wrappedText = "[s:$newSize]$cleanedSelectedText[/s]"
        val newFullText = text.replaceRange(start, end, wrappedText)

        return value.copy(
            text = newFullText,
            selection = TextRange(start + wrappedText.length)
        )
    }

    fun findActiveMatch(text: String, offset: Int): MatchResult? {
        return REGEX.findAll(text).toList().reversed().firstOrNull {
            offset >= it.range.first && offset <= it.range.last
        }
    }
}