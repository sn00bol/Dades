@file:Suppress("DEPRECATION")

package com.sn00bol.dades.ui.components

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Provides standard spacing parameters based on WindowSizeClass.
 * Uses @Suppress("DEPRECATION") because current Google Adaptive APIs 
 * are still in the process of transitioning between Window Core libraries.
 */
object AdaptiveSpacing {
    val Compact = 16.dp
    val Medium = 24.dp
    val Expanded = 32.dp

    @Composable
    fun current(): Dp {
        val adaptiveInfo = currentWindowAdaptiveInfo()
        return when {
            adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT -> Compact
            adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM -> Medium
            else -> Expanded
        }
    }
}

/**
 * Checks if the device is a large screen (Tablet/Desktop).
 */
@Composable
fun isExpandedScreen(): Boolean {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    return adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
}

/**
 * Checks if the device is a foldable or medium-sized tablet.
 */
@Composable
fun isMediumScreen(): Boolean {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    return adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM
}
