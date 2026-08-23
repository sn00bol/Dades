package com.sn00bol.dades.TextEditor

import androidx.compose.runtime.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.sn00bol.dades.TextEditor.TextFont.TextSize
import kotlin.reflect.KClass

sealed interface RichTextStyle {
    sealed interface CharacterStyle : RichTextStyle
    sealed interface ParagraphStyle : RichTextStyle

    object Bold : CharacterStyle
    object Italic : CharacterStyle
    object Underline : CharacterStyle
    object Strikethrough : CharacterStyle
    data class Size(val size: Int) : CharacterStyle
    data class Alignment(val align: TextAlign) : ParagraphStyle
}

data class StyleRange(val start: Int, val end: Int, val style: RichTextStyle)

class RichTextEditorState(
    initialText: String = "",
    initialStyles: List<StyleRange> = emptyList()
) {
    var textFieldValue by mutableStateOf(
        TextFieldValue(
            annotatedString = TextEditorEngine.buildAnnotatedString(initialText, initialStyles),
            selection = TextRange(initialText.length)
        )
    )
        private set

    var styleRanges = initialStyles.toMutableStateList()
        private set

    private val pendingStyles = mutableStateListOf<RichTextStyle>()

    fun onValueChanged(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        val newText = newValue.text
        val oldSelection = textFieldValue.selection
        val newSelection = newValue.selection

        if (oldText != newText) {
            val diff = newText.length - oldText.length
            val changePos = if (diff > 0) oldSelection.min else newSelection.min

            adjustRangesOnTextChange(changePos, diff, oldText.length)

            if (diff > 0 && oldSelection.collapsed && pendingStyles.isNotEmpty()) {
                val addedStart = oldSelection.start
                val addedEnd = oldSelection.start + diff

                pendingStyles.forEach { style ->
                    addStyleToRange(addedStart, addedEnd, style)
                }
            }
        }

        if (newSelection != oldSelection && oldText == newText) {
            pendingStyles.clear()
        }

        textFieldValue = newValue.copy(annotatedString = TextEditorEngine.buildAnnotatedString(newText, styleRanges))
    }

    private fun adjustRangesOnTextChange(changePos: Int, diff: Int, oldLength: Int) {
        val updatedRanges = mutableListOf<StyleRange>()

        styleRanges.forEach { range ->
            when {
                range.end <= changePos -> updatedRanges.add(range)

                range.start >= changePos + (if (diff < 0) -diff else 0) -> {
                    val newStart = (range.start + diff).coerceAtLeast(0)
                    val newEnd = (range.end + diff).coerceAtLeast(0)
                    if (newStart < newEnd) updatedRanges.add(range.copy(start = newStart, end = newEnd))
                }

                else -> {
                    val newStart = if (range.start > changePos) {
                        (range.start + diff).coerceAtLeast(changePos)
                    } else {
                        range.start
                    }

                    val newEnd = if (range.end > changePos) {
                        (range.end + diff).coerceAtLeast(newStart)
                    } else {
                        range.end
                    }

                    if (newStart < newEnd) updatedRanges.add(range.copy(start = newStart, end = newEnd))
                }
            }
        }

        styleRanges.clear()
        styleRanges.addAll(updatedRanges)
    }

    fun toggleStyle(style: RichTextStyle) {
        val selection = textFieldValue.selection

        if (selection.collapsed) {
            val existing = pendingStyles.find {
                if (style is RichTextStyle.Size) it is RichTextStyle.Size
                else if (style is RichTextStyle.Alignment) it is RichTextStyle.Alignment
                else it == style
            }

            if (existing != null) {
                pendingStyles.remove(existing)
                if (existing != style) pendingStyles.add(style)
            } else {
                pendingStyles.add(style)
            }
            return
        }

        val start = selection.min
        val end = selection.max
        val isCurrentlyApplied = isStyleFullyApplied(start, end, style)

        if (isCurrentlyApplied) {
            removeStyleFromRange(start, end, style)
        } else {
            addStyleToRange(start, end, style)
        }

        textFieldValue = textFieldValue.copy(annotatedString = TextEditorEngine.buildAnnotatedString(textFieldValue.text, styleRanges))
    }

    private fun addStyleToRange(start: Int, end: Int, style: RichTextStyle) {
        if (start >= end) return

        splitRangesAt(start)
        splitRangesAt(end)

        if (style is RichTextStyle.Size || style is RichTextStyle.Alignment) {
            removeStyleClassFromRange(start, end, style::class)
        } else {
            removeStyleFromRange(start, end, style)
        }

        styleRanges.add(StyleRange(start, end, style))
        compactRanges()
    }

    private fun removeStyleFromRange(start: Int, end: Int, style: RichTextStyle) {
        if (start >= end) return
        splitRangesAt(start)
        splitRangesAt(end)

        val remaining = styleRanges.filter { range ->
            range.style != style || range.end <= start || range.start >= end
        }
        styleRanges.clear()
        styleRanges.addAll(remaining)
        compactRanges()
    }

    private fun removeStyleClassFromRange(start: Int, end: Int, styleClass: KClass<out RichTextStyle>) {
        if (start >= end) return
        splitRangesAt(start)
        splitRangesAt(end)

        val remaining = styleRanges.filter { range ->
            range.style::class != styleClass || range.end <= start || range.start >= end
        }
        styleRanges.clear()
        styleRanges.addAll(remaining)
        compactRanges()
    }

    private fun splitRangesAt(offset: Int) {
        if (offset <= 0) return
        val toAdd = mutableListOf<StyleRange>()
        val toRemove = mutableListOf<StyleRange>()

        styleRanges.forEach { range ->
            if (offset > range.start && offset < range.end) {
                toRemove.add(range)
                toAdd.add(range.copy(end = offset))
                toAdd.add(range.copy(start = offset))
            }
        }
        styleRanges.removeAll(toRemove)
        styleRanges.addAll(toAdd)
    }

    private fun isStyleFullyApplied(start: Int, end: Int, style: RichTextStyle): Boolean {
        var coverage = start
        val relevantRanges = styleRanges
            .filter { it.style == style && it.end > start && it.start < end }
            .sortedBy { it.start }

        for (range in relevantRanges) {
            if (range.start > coverage) return false
            coverage = maxOf(coverage, range.end)
            if (coverage >= end) return true
        }
        return coverage >= end
    }

    fun hasStyle(style: RichTextStyle): Boolean {
        val selection = textFieldValue.selection
        val isPending = when (style) {
            is RichTextStyle.Size -> pendingStyles.any { it is RichTextStyle.Size && it.size == style.size }
            is RichTextStyle.Alignment -> pendingStyles.any { it is RichTextStyle.Alignment && it.align == style.align }
            else -> pendingStyles.contains(style)
        }
        if (isPending) return true

        if (selection.collapsed) {
            val offset = selection.min
            return styleRanges.any { it.start <= offset && it.end > offset && it.style == style }
        }
        return isStyleFullyApplied(selection.min, selection.max, style)
    }

    fun getActiveAlignment(): TextAlign {
        val selection = textFieldValue.selection
        val pendingAlign = pendingStyles.filterIsInstance<RichTextStyle.Alignment>().lastOrNull()
        if (pendingAlign != null) return pendingAlign.align

        val offset = selection.min
        val alignStyle = styleRanges.find { it.start <= offset && it.end >= offset && it.style is RichTextStyle.Alignment }
        return (alignStyle?.style as? RichTextStyle.Alignment)?.align ?: TextAlign.Start
    }

    fun getCurrentSize(): Int {
        val selection = textFieldValue.selection
        val pendingSize = pendingStyles.filterIsInstance<RichTextStyle.Size>().lastOrNull()
        if (pendingSize != null) return pendingSize.size

        val offset = selection.min
        val sizeStyle = styleRanges.find { it.start <= offset && it.end > offset && it.style is RichTextStyle.Size }
        return (sizeStyle?.style as? RichTextStyle.Size)?.size ?: 16
    }

    fun changeSize(increase: Boolean) {
        val selection = textFieldValue.selection
        val currentSize = getCurrentSize()
        val nextSize = if (increase) TextSize.getNextSize(currentSize) else TextSize.getPrevSize(currentSize)

        if (selection.collapsed) {
            // Behavioral change: update pending size instead of entire text
            val existing = pendingStyles.filterIsInstance<RichTextStyle.Size>().firstOrNull()
            if (existing != null) pendingStyles.remove(existing)
            pendingStyles.add(RichTextStyle.Size(nextSize))
            return
        }

        val start = selection.min
        val end = selection.max

        if (start < end) {
            addStyleToRange(start, end, RichTextStyle.Size(nextSize))
            textFieldValue = textFieldValue.copy(annotatedString = TextEditorEngine.buildAnnotatedString(textFieldValue.text, styleRanges))
        }
    }

    fun applyAlignment(align: TextAlign) {
        val selection = textFieldValue.selection
        val text = textFieldValue.text

        val start = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val end = text.indexOf('\n', selection.max).let { if (it == -1) text.length else it }

        removeStyleClassFromRange(start, end, RichTextStyle.Alignment::class)

        if (align != TextAlign.Left && align != TextAlign.Start) {
            val selectedBlock = text.substring(start, end)
            var currentPos = start
            selectedBlock.split('\n').forEach { line ->
                val lineEnd = currentPos + line.length
                styleRanges.add(StyleRange(currentPos, lineEnd, RichTextStyle.Alignment(align)))
                currentPos = lineEnd + 1
            }
        }

        textFieldValue = textFieldValue.copy(annotatedString = TextEditorEngine.buildAnnotatedString(text, styleRanges))
        compactRanges()
    }

    private fun compactRanges() {
        val sorted = styleRanges.sortedWith(compareBy({ it.style.javaClass.name }, { it.start }))
        val compacted = mutableListOf<StyleRange>()

        for (range in sorted) {
            val last = compacted.lastOrNull()
            if (last != null && last.style == range.style && last.end >= range.start) {
                compacted[compacted.lastIndex] = last.copy(end = maxOf(last.end, range.end))
            } else {
                compacted.add(range)
            }
        }

        styleRanges.clear()
        styleRanges.addAll(compacted)
    }
}
