package com.sn00bol.dades.TextEditor.TextFont

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

object Strikethrough {
    val REGEX = Regex("""(?s)~~(.*?)~~""")
    val STYLE = SpanStyle(textDecoration = TextDecoration.LineThrough)
    const val SYMBOL_LEN = 2
}
