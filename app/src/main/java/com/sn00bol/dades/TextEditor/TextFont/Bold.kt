package com.sn00bol.dades.TextEditor.TextFont

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

object Bold {
    val REGEX = Regex("""(?s)\*\*(.*?)\*\*""")
    val STYLE = SpanStyle(fontWeight = FontWeight.Bold)
    const val SYMBOL_LEN = 2
}
