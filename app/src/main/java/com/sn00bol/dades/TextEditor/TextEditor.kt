package com.sn00bol.dades.TextEditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.sn00bol.dades.TextEditor.Alignment.AlignCenter
import com.sn00bol.dades.TextEditor.Alignment.AlignmentEngine
import com.sn00bol.dades.TextEditor.Alignment.AlignRight
import com.sn00bol.dades.TextEditor.TextFont.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object TextEditor {

    class ChecklistTransformation(
        private val checkedColor: Color = Color(0xFF4CAF50),
        private val fontSizeScale: Float = 1f,
        private val isStatic: Boolean = false
    ) : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val rawText = text.text
            
            val transformedToOriginal = mutableListOf<Int>()
            val originalToTransformed = IntArray(rawText.length + 1)
            
            val result = buildAnnotatedString {
                var i = 0
                while (i < rawText.length) {
                    // Handle normal character or checkbox prefix
                    val lineStart = rawText.lastIndexOf('\n', i - 1).let { if (it == -1) 0 else it + 1 }
                    val prefix = if (i == lineStart) TextList.findExistingPrefix(rawText.substring(lineStart)) else null
                    
                    if (prefix != null) {
                        val cleanPrefix = prefix.trim()
                        val displayPrefix = when (cleanPrefix) {
                            "[]", "[ ]" -> "☐ "
                            "[x]" -> "☑ "
                            else -> prefix
                        }
                        
                        val prefixStyle = when (cleanPrefix) {
                            "[]", "[ ]" -> SpanStyle(color = Color.Gray.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = (18 * fontSizeScale).sp)
                            "[x]" -> SpanStyle(color = checkedColor, fontWeight = FontWeight.Bold, fontSize = (18 * fontSizeScale).sp)
                            else -> SpanStyle()
                        }

                        val transformedStart = this.length
                        withStyle(prefixStyle) {
                            append(displayPrefix)
                        }
                        
                        repeat(prefix.length) { idx ->
                            originalToTransformed[i + idx] = transformedStart
                        }
                        repeat(displayPrefix.length) {
                            transformedToOriginal.add(i)
                        }
                        
                        i += prefix.length
                    } else {
                        originalToTransformed[i] = this.length
                        transformedToOriginal.add(i)
                        append(rawText[i])
                        i++
                    }
                }
                originalToTransformed[rawText.length] = this.length
                transformedToOriginal.add(rawText.length)
                
                text.spanStyles.forEach { range ->
                    val start = originalToTransformed[range.start.coerceIn(0, rawText.length)]
                    val end = originalToTransformed[range.end.coerceIn(0, rawText.length)]
                    if (start < end) {
                        addStyle(range.item, start, end)
                    }
                }
            }

            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = 
                    originalToTransformed.getOrElse(offset) { originalToTransformed.last() }

                override fun transformedToOriginal(offset: Int): Int = 
                    transformedToOriginal.getOrElse(offset) { transformedToOriginal.last() }
            }

            return TransformedText(result, offsetMapping)
        }
    }

    fun parseToState(rawText: String): RichTextEditorState {
        val matches = mutableListOf<ParsedMatch>()
        
        TextSize.REGEX.findAll(rawText).forEach { match ->
            val size = match.groupValues[1].toIntOrNull() ?: 16
            val contentGroup = match.groups[2]
            if (contentGroup != null) {
                val startLen = contentGroup.range.first - match.range.first
                val endLen = match.range.last - contentGroup.range.last
                matches.add(ParsedMatch(match.range, RichTextStyle.Size(size), startLen, endLen))
            }
        }

        fun collect(regex: Regex, style: RichTextStyle) {
            regex.findAll(rawText).forEach { match ->
                val contentGroup = match.groups[1]
                if (contentGroup != null) {
                    val startLen = contentGroup.range.first - match.range.first
                    val endLen = match.range.last - contentGroup.range.last
                    matches.add(ParsedMatch(match.range, style, startLen, endLen))
                }
            }
        }
        collect(Bold.REGEX, RichTextStyle.Bold)
        collect(Underline.REGEX, RichTextStyle.Underline)
        collect(Strikethrough.REGEX, RichTextStyle.Strikethrough)
        collect(Italic.REGEX, RichTextStyle.Italic)

        AlignmentEngine.findMatches(rawText).forEach { match ->
            val style = when (match.style.textAlign) {
                TextAlign.Center -> RichTextStyle.Alignment(TextAlign.Center)
                TextAlign.End -> RichTextStyle.Alignment(TextAlign.End)
                else -> RichTextStyle.Alignment(TextAlign.Start)
            }
            matches.add(ParsedMatch(match.range, style, match.symbolLen, 0))
        }

        val sortedMatches = matches.sortedWith(compareBy({ it.range.first }, { -it.range.last }))
        val processed = BooleanArray(rawText.length)
        val styleRanges = mutableListOf<StyleRange>()
        val symbolIndices = mutableSetOf<Int>()

        for (match in sortedMatches) {
            val startSymRange = match.range.first until match.range.first + match.startLen
            val endSymRange = if (match.endLen > 0) (match.range.last - match.endLen + 1 .. match.range.last) else IntRange.EMPTY

            val isStartFree = startSymRange.none { it < rawText.length && processed[it] }
            val isEndFree = endSymRange.isEmpty() || endSymRange.none { it < rawText.length && processed[it] }

            if (isStartFree && isEndFree) {
                startSymRange.forEach { if (it < rawText.length) { symbolIndices.add(it); processed[it] = true } }
                endSymRange.forEach { if (it < rawText.length) { symbolIndices.add(it); processed[it] = true } }
                styleRanges.add(StyleRange(match.range.first, match.range.last + 1, match.style))
            }
        }

        val cleanTextBuilder = StringBuilder()
        val originalToClean = IntArray(rawText.length + 1)
        for ((i, element) in rawText.withIndex()) {
            originalToClean[i] = cleanTextBuilder.length
            if (i !in symbolIndices) {
                cleanTextBuilder.append(element)
            }
        }
        originalToClean[rawText.length] = cleanTextBuilder.length

        val finalStyleRanges = mutableListOf<StyleRange>()
        styleRanges.forEach { range ->
            val newStart = originalToClean[range.start.coerceIn(0, rawText.length)]
            val newEnd = originalToClean[range.end.coerceIn(0, rawText.length)]
            finalStyleRanges.add(StyleRange(newStart, newEnd, range.style))
        }
        
        return RichTextEditorState(cleanTextBuilder.toString(), finalStyleRanges)
    }

    fun render(rawText: String, fontSizeScale: Float = 1f, checkedColor: Color = Color(0xFF4CAF50)): AnnotatedString {
        val state = parseToState(rawText)
        val annotatedString = buildAnnotatedString(state.textFieldValue.text, state.styleRanges, fontSizeScale)
        return ChecklistTransformation(checkedColor, fontSizeScale, isStatic = true).filter(annotatedString).text
    }

    fun buildAnnotatedString(text: String, ranges: List<StyleRange>, fontSizeScale: Float = 1f): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            ranges.forEach { range ->
                val start = range.start.coerceIn(0, text.length)
                val end = range.end.coerceIn(0, text.length)
                if (start < end) {
                    when (val s = range.style) {
                        RichTextStyle.Bold -> addStyle(Bold.STYLE, start, end)
                        RichTextStyle.Italic -> addStyle(Italic.STYLE, start, end)
                        RichTextStyle.Underline -> addStyle(Underline.STYLE, start, end)
                        RichTextStyle.Strikethrough -> addStyle(Strikethrough.STYLE, start, end)
                        is RichTextStyle.Size -> addStyle(SpanStyle(fontSize = (s.size * fontSizeScale).sp), start, end)
                        is RichTextStyle.Alignment -> addStyle(ParagraphStyle(textAlign = s.align), start, end)
                        is RichTextStyle.ListIndent -> addStyle(ParagraphStyle(textIndent = TextIndent(restLine = (s.indentSp * fontSizeScale).sp)), start, end)
                    }
                }
            }
        }
    }

    fun serializeState(state: RichTextEditorState): String {
        val text = state.textFieldValue.text

        class StyleEvent(val range: StyleRange, val tag: String, val offset: Int, val isStart: Boolean, val isAlignment: Boolean = false)

        val events = mutableListOf<StyleEvent>()
        state.styleRanges.forEach { range ->
            val startTag = when (val s = range.style) {
                RichTextStyle.Bold -> "**"
                RichTextStyle.Italic -> "*"
                RichTextStyle.Underline -> "__"
                RichTextStyle.Strikethrough -> "~~"
                is RichTextStyle.Size -> "[s:${s.size}]"
                is RichTextStyle.Alignment -> when (s.align) {
                    TextAlign.Center -> AlignCenter.SYMBOL
                    TextAlign.End -> AlignRight.SYMBOL
                    else -> ""
                }
                is RichTextStyle.ListIndent -> ""
            }
            val endTag = when (range.style) {
                RichTextStyle.Bold -> "**"
                RichTextStyle.Italic -> "*"
                RichTextStyle.Underline -> "__"
                RichTextStyle.Strikethrough -> "~~"
                is RichTextStyle.Size -> "[/s]"
                else -> ""
            }

            if (range.style is RichTextStyle.Alignment) {
                if (startTag.isNotEmpty()) {
                    val lineStart = text.lastIndexOf('\n', (range.start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                    events.add(StyleEvent(range, startTag, lineStart, isStart = true, isAlignment = true))
                }
            } else {
                if (startTag.isNotEmpty()) events.add(StyleEvent(range, startTag, range.start, isStart = true))
                if (endTag.isNotEmpty()) events.add(StyleEvent(range, endTag, range.end, isStart = false))
            }
        }

        val allEvents = events.groupBy { it.offset }
        val sortedOffsets = allEvents.keys.sortedDescending()

        var result = text
        sortedOffsets.forEach { offset ->
            val eventsAtOffset = allEvents[offset]!!.toMutableList()

            eventsAtOffset.sortWith { e1, e2 ->
                val type1 = if (e1.isAlignment) 2 else if (e1.isStart) 0 else 1
                val type2 = if (e2.isAlignment) 2 else if (e2.isStart) 0 else 1
                if (type1 != type2) return@sortWith type1.compareTo(type2)
                if (e1.isStart) e1.range.end.compareTo(e2.range.end) else e2.range.start.compareTo(e1.range.start)
            }

            eventsAtOffset.forEach { event ->
                if (offset in 0..result.length) {
                    result = result.substring(0, offset) + event.tag + result.substring(offset)
                }
            }
        }
        return result
    }

    private data class ParsedMatch(val range: IntRange, val style: RichTextStyle, val startLen: Int, val endLen: Int)
}
