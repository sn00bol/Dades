package com.sn00bol.dades.TextEditor.TextFont

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

object Underline {
    val REGEX = Regex("""(?s)__(.*?)__""")
    val STYLE = SpanStyle(textDecoration = TextDecoration.Underline)
    const val SYMBOL_LEN = 2
}
