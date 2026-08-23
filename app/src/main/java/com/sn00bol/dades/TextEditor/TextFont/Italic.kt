package com.sn00bol.dades.TextEditor.TextFont

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle

object Italic {
    val REGEX = Regex("""(?s)\*(.*?)\*""")
    val STYLE = SpanStyle(fontStyle = FontStyle.Italic)
    const val SYMBOL_LEN = 1
}
