package com.sn00bol.dades.TextEditor.Alignment

import androidx.compose.ui.text.ParagraphStyle

object AlignmentEngine {
    data class AlignmentMatch(val range: IntRange, val style: ParagraphStyle, val symbolLen: Int)

    fun findMatches(rawText: String): List<AlignmentMatch> {
        val matches = mutableListOf<AlignmentMatch>()
        
        AlignCenter.REGEX.findAll(rawText).forEach {
            matches.add(AlignmentMatch(it.range, AlignCenter.STYLE, AlignCenter.SYMBOL_LEN))
        }
        
        AlignRight.REGEX.findAll(rawText).forEach {
            matches.add(AlignmentMatch(it.range, AlignRight.STYLE, AlignRight.SYMBOL_LEN))
        }
        
        return matches
    }
}
