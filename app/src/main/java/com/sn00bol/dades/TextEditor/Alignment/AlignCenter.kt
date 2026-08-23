package com.sn00bol.dades.TextEditor.Alignment

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign

object AlignCenter {
    const val SYMBOL = ":--:"
    val REGEX = Regex("""(?m)^$SYMBOL(.*)$""")
    val STYLE = ParagraphStyle(textAlign = TextAlign.Center)
    const val SYMBOL_LEN = 4
}
