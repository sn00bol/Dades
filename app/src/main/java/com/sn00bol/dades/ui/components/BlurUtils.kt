package com.sn00bol.dades.ui.components

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier that handles standard Gaussian Blur for Android 12+ (API 31+).
 * Android < 12 will ignore it to avoid crashes/UI errors.
 */
fun Modifier.adaptiveBlur(blurRadius: Dp): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this.blur(
        radius = blurRadius,
        edgeTreatment = BlurredEdgeTreatment.Unbounded
    )
} else {
    this
}

/**
 * Multi-platform blur management wrapper:
 * - Android 12+: Renders true Gaussian blur + slight color overlay.
 * - Android < 12: Semi-transparent dark/blur overlay (Dimming Fallback).
 */
@Composable
fun BlurWrapper(
    isActive: Boolean,
    onDismiss: () -> Unit,
    maxBlurRadius: Dp = 20.dp,
    content: @Composable (blurModifier: Modifier) -> Unit
) {
    // 1. Animate Blur radius
    val blurRadius by animateDpAsState(
        targetValue = if (isActive) maxBlurRadius else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "BlurRadiusAnimation"
    )

    // 2. Animate Overlay alpha (Android < 12 uses higher alpha to simulate blur effect)
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.3f else 0.85f
        } else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "OverlayAlphaAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Pass Modifier containing Gaussian Blur into Content
        content(Modifier.adaptiveBlur(blurRadius))

        // Overlay providing depth and capturing click events to close Search
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = overlayAlpha)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
    }
}

/**
 * Component creating a Fade effect at the top and bottom of the list.
 */
@Composable
fun EdgeFadeOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                    )
                )
        )
        // Bottom Fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )
                )
        )
    }
}