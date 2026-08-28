package com.sn00bol.dades.TextEditor

import androidx.compose.runtime.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.sn00bol.dades.TextEditor.TextFont.TextList
import com.sn00bol.dades.TextEditor.TextFont.TextSize
import kotlin.math.abs
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
    data class ListIndent(val indentSp: Int) : ParagraphStyle
}

data class StyleRange(val start: Int, val end: Int, val style: RichTextStyle)

class RichTextEditorState(
    initialAnnotatedString: AnnotatedString = AnnotatedString(""),
    initialStyles: List<StyleRange> = emptyList()
) {
    constructor(initialText: String, initialStyles: List<StyleRange> = emptyList()) : 
        this(TextEditor.buildAnnotatedString(initialText, initialStyles), initialStyles)

    var textFieldValue by mutableStateOf(
        TextFieldValue(
            annotatedString = initialAnnotatedString,
            selection = TextRange(initialAnnotatedString.text.length)
        )
    )
        private set

    var styleRanges = initialStyles.toMutableStateList()
        private set

    private val history = mutableStateListOf<Snapshot>()
    private var historyIndex by mutableIntStateOf(-1)

    data class Snapshot(
        val textFieldValue: TextFieldValue,
        val styleRanges: List<StyleRange>
    )

    init {
        takeSnapshot()
    }

    private fun takeSnapshot() {
        val current = Snapshot(textFieldValue.copy(annotatedString = AnnotatedString(textFieldValue.text)), styleRanges.toList())
        if (historyIndex >= 0 && history[historyIndex].textFieldValue.text == current.textFieldValue.text && history[historyIndex].styleRanges == current.styleRanges) return

        while (history.size > historyIndex + 1) {
            history.removeAt(history.size - 1)
        }

        history.add(current)
        if (history.size > 50) {
            history.removeAt(0)
        } else {
            historyIndex++
        }
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val snapshot = history[historyIndex]
            styleRanges.clear()
            styleRanges.addAll(snapshot.styleRanges)
            textFieldValue = snapshot.textFieldValue.copy(
                annotatedString = TextEditor.buildAnnotatedString(snapshot.textFieldValue.text, styleRanges)
            )
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val snapshot = history[historyIndex]
            styleRanges.clear()
            styleRanges.addAll(snapshot.styleRanges)
            textFieldValue = snapshot.textFieldValue.copy(
                annotatedString = TextEditor.buildAnnotatedString(snapshot.textFieldValue.text, styleRanges)
            )
        }
    }

    fun canUndo(): Boolean = historyIndex > 0
    fun canRedo(): Boolean = historyIndex < history.size - 1

    private val pendingStyles = mutableStateListOf<RichTextStyle>()

    fun onValueChanged(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        val newText = newValue.text
        val oldSelection = textFieldValue.selection
        val newSelection = newValue.selection

        if (oldText != newText) {
            val diff = newText.length - oldText.length
            val changePos = if (diff > 0) oldSelection.min else newSelection.min

            // Xử lý xóa checklist prefix
            if (diff == -1 && oldSelection.collapsed) {
                val lineStart = oldText.lastIndexOf('\n', (changePos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                val line = oldText.substring(lineStart)
                val prefix = TextList.findExistingPrefix(line)
                if (prefix != null && (prefix == "[ ] " || prefix == "[x] ") && changePos == lineStart + prefix.length - 1) {
                    val textWithoutPrefix = oldText.substring(0, lineStart) + oldText.substring(lineStart + prefix.length)
                    textFieldValue = TextFieldValue(textWithoutPrefix, TextRange(lineStart))
                    adjustRangesOnTextChange(lineStart, -prefix.length, oldText.length)
                    takeSnapshot()
                    return
                }
            }

            // Logic xử lý Enter cho List
            if (diff == 1 && newText.getOrNull(changePos) == '\n' && oldSelection.collapsed) {
                val prevLineStart = oldText.lastIndexOf('\n', (changePos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                val prevLine = oldText.substring(prevLineStart, changePos)
                val existingPrefix = TextList.findExistingPrefix(prevLine)

                if (existingPrefix != null) {
                    if (prevLine == existingPrefix) {
                        val textWithoutPrefix = oldText.substring(0, prevLineStart) + oldText.substring(changePos)
                        val cursorBase = prevLineStart
                        textFieldValue = TextFieldValue(textWithoutPrefix, TextRange(cursorBase))
                        adjustRangesOnTextChange(prevLineStart, -existingPrefix.length, oldText.length)
                        takeSnapshot()
                        return
                    } else {
                        val nextPrefix = if (existingPrefix.matches(Regex("""^\d+\.\s"""))) {
                            val curNum = existingPrefix.removeSuffix(". ").toIntOrNull() ?: 1
                            "${curNum + 1}. "
                        } else if (existingPrefix == TextList.CHECKED_SYMBOL) {
                            TextList.CHECKLIST_SYMBOL
                        } else existingPrefix

                        val textWithNextPrefix = newText.substring(0, changePos + 1) + nextPrefix + newText.substring(changePos + 1)
                        val newCursor = changePos + 1 + nextPrefix.length
                        textFieldValue = TextFieldValue(textWithNextPrefix, TextRange(newCursor))
                        adjustRangesOnTextChange(changePos, 1 + nextPrefix.length, oldText.length)

                        val fontSize = getCurrentSize()
                        val indentValue = (nextPrefix.length * fontSize * 0.7f).toInt()
                        styleRanges.add(StyleRange(changePos + 1, changePos + 1 + nextPrefix.length, RichTextStyle.ListIndent(indentValue)))

                        takeSnapshot()
                        return
                    }
                }
            }

            adjustRangesOnTextChange(changePos, diff, oldText.length)

            if (diff > 0 && oldSelection.collapsed && pendingStyles.isNotEmpty()) {
                val addedStart = oldSelection.start
                val addedEnd = oldSelection.start + diff

                pendingStyles.forEach { style ->
                    if (style is RichTextStyle.Alignment) {
                        val text = newText
                        val start = text.lastIndexOf('\n', (addedStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                        val end = text.indexOf('\n', addedEnd - 1).let { if (it == -1) text.length else it + 1 }
                        addStyleToRange(start, end, style)
                    } else {
                        addStyleToRange(addedStart, addedEnd, style)
                    }
                }
            }

            if (newText.endsWith(" ") || newText.endsWith("\n") || abs(newText.length - oldText.length) > 10) {
                takeSnapshot()
            }
        }

        if (newSelection != oldSelection && oldText == newText) {
            pendingStyles.clear()
        }

        textFieldValue = newValue.copy(annotatedString = TextEditor.buildAnnotatedString(newText, styleRanges))
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

                    val newEnd = (range.end + diff).coerceAtLeast(newStart)

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

        textFieldValue = textFieldValue.copy(annotatedString = TextEditor.buildAnnotatedString(textFieldValue.text, styleRanges))
        takeSnapshot()
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
        val alignStyle = styleRanges.find { it.start <= offset && it.end > offset && it.style is RichTextStyle.Alignment }
        return (alignStyle?.style as? RichTextStyle.Alignment)?.align ?: TextAlign.Start
    }

    fun applyTextPreset(size: Int, isBold: Boolean) {
        val selection = textFieldValue.selection

        if (selection.collapsed) {
            val existing = pendingStyles.filterIsInstance<RichTextStyle.Size>().firstOrNull()
            if (existing != null) pendingStyles.remove(existing)
            pendingStyles.add(RichTextStyle.Size(size))

            val hasBold = pendingStyles.contains(RichTextStyle.Bold)
            if (isBold && !hasBold) pendingStyles.add(RichTextStyle.Bold)
            else if (!isBold && hasBold) pendingStyles.remove(RichTextStyle.Bold)
        } else {
            val start = selection.min
            val end = selection.max

            addStyleToRange(start, end, RichTextStyle.Size(size))

            val isCurrentlyBold = isStyleFullyApplied(start, end, RichTextStyle.Bold)
            if (isBold && !isCurrentlyBold) {
                addStyleToRange(start, end, RichTextStyle.Bold)
            } else if (!isBold && isCurrentlyBold) {
                removeStyleFromRange(start, end, RichTextStyle.Bold)
            }

            textFieldValue = textFieldValue.copy(annotatedString = TextEditor.buildAnnotatedString(textFieldValue.text, styleRanges))
            takeSnapshot()
        }
    }

    fun isPresetActive(size: Int, isBold: Boolean): Boolean {
        return getCurrentSize() == size && hasStyle(RichTextStyle.Bold) == isBold
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
            val existing = pendingStyles.filterIsInstance<RichTextStyle.Size>().firstOrNull()
            if (existing != null) pendingStyles.remove(existing)
            pendingStyles.add(RichTextStyle.Size(nextSize))
            return
        }

        val start = selection.min
        val end = selection.max

        if (start < end) {
            addStyleToRange(start, end, RichTextStyle.Size(nextSize))
            textFieldValue = textFieldValue.copy(annotatedString = TextEditor.buildAnnotatedString(textFieldValue.text, styleRanges))
            takeSnapshot()
        }
    }

    fun applyAlignment(align: TextAlign) {
        val selection = textFieldValue.selection
        val text = textFieldValue.text

        val start = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val end = text.indexOf('\n', selection.max).let { if (it == -1) text.length else it + 1 }

        removeStyleClassFromRange(start, end, RichTextStyle.Alignment::class)

        if (align != TextAlign.Left && align != TextAlign.Start) {
            styleRanges.add(StyleRange(start, end, RichTextStyle.Alignment(align)))
        }

        if (selection.collapsed) {
            val existing = pendingStyles.filterIsInstance<RichTextStyle.Alignment>().firstOrNull()
            if (existing != null) pendingStyles.remove(existing)
            pendingStyles.add(RichTextStyle.Alignment(align))
        }

        compactRanges()
        textFieldValue = textFieldValue.copy(
            annotatedString = TextEditor.buildAnnotatedString(text, styleRanges)
        )
        takeSnapshot()
    }

    fun toggleBulletList() {
        val change = TextList.toggleList(textFieldValue, TextList.ListType.Bullet)
        applyMultiListChange(change)
    }

    fun toggleNumberList() {
        val change = TextList.toggleList(textFieldValue, TextList.ListType.Number)
        applyMultiListChange(change)
    }

    fun toggleChecklist() {
        val change = TextList.toggleList(textFieldValue, TextList.ListType.Checklist)
        applyMultiListChange(change)
    }

    /**
     * Xử lý Click vào vị trí offset chữ để đổi tick box trạng thái [] / [x]
     */
    fun handleTextClick(offset: Int): Boolean {
        val text = textFieldValue.text
        if (offset < 0 || offset > text.length) return false

        val lineStart = text.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val prefix = TextList.findExistingPrefix(line)
        if (prefix != null && (prefix == TextList.CHECKLIST_SYMBOL || prefix == TextList.CHECKED_SYMBOL || prefix.trim() == "[]")) {
            val prefixLen = prefix.length
            // Cho phép click vào biểu tượng checklist ở đầu dòng (hoặc trong bán kính 1-2 char kế bên)
            if (offset >= lineStart && offset <= lineStart + prefixLen + 1) {
                val isChecked = prefix == TextList.CHECKED_SYMBOL
                val newPrefix = if (isChecked) TextList.CHECKLIST_SYMBOL else TextList.CHECKED_SYMBOL

                val newText = text.substring(0, lineStart) + newPrefix + text.substring(lineStart + prefixLen)
                val diff = newPrefix.length - prefixLen

                adjustRangesOnTextChange(lineStart, diff, text.length)

                textFieldValue = TextFieldValue(
                    text = newText,
                    selection = textFieldValue.selection,
                    composition = textFieldValue.composition
                ).copy(annotatedString = TextEditor.buildAnnotatedString(newText, styleRanges))

                takeSnapshot()
                return true
            }
        }
        return false
    }

    fun toggleCustomBullet(symbol: String) {
        val change = TextList.toggleList(textFieldValue, TextList.ListType.Custom, symbol)
        applyMultiListChange(change)
    }

    private fun applyMultiListChange(change: TextList.ListChange) {
        val oldText = textFieldValue.text
        val firstLine = change.affectedLines.firstOrNull() ?: return
        val totalDiff = change.newValue.text.length - oldText.length

        adjustRangesOnTextChange(firstLine.start, totalDiff, oldText.length)

        change.affectedLines.forEach { line ->
            val lineEndInNewText = if (line.end < change.newValue.text.length && change.newValue.text[line.end] == '\n') {
                line.end + 1
            } else line.end

            removeStyleClassFromRange(line.start, lineEndInNewText, RichTextStyle.ListIndent::class)

            if (line.prefix != null) {
                val fontSize = getCurrentSize()
                val indentValue = (line.prefix.length * fontSize * 0.7f).toInt()
                styleRanges.add(StyleRange(line.start, lineEndInNewText, RichTextStyle.ListIndent(indentValue)))
            }
        }

        textFieldValue = change.newValue.copy(
            annotatedString = TextEditor.buildAnnotatedString(change.newValue.text, styleRanges)
        )
        compactRanges()
        takeSnapshot()
    }

    fun indent() {
        val selection = textFieldValue.selection
        val text = textFieldValue.text
        val indentStr = "    "

        val lineStart = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }

        val newText = text.substring(0, lineStart) + indentStr + text.substring(lineStart)
        val newSelection = TextRange(selection.start + indentStr.length, selection.end + indentStr.length)

        adjustRangesOnTextChange(lineStart, indentStr.length, text.length)
        textFieldValue = TextFieldValue(newText, newSelection.coerceIn(0, newText.length))
        takeSnapshot()
    }

    fun outdent() {
        val selection = textFieldValue.selection
        val text = textFieldValue.text
        val indentStr = "    "

        val lineStart = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val line = text.substring(lineStart)

        if (line.startsWith(indentStr)) {
            val newText = text.substring(0, lineStart) + line.substring(indentStr.length)
            val newSelection = TextRange((selection.start - indentStr.length).coerceAtLeast(lineStart), (selection.end - indentStr.length).coerceAtLeast(lineStart))

            adjustRangesOnTextChange(lineStart, -indentStr.length, text.length)
            textFieldValue = TextFieldValue(newText, newSelection.coerceIn(0, newText.length))
            takeSnapshot()
        }
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