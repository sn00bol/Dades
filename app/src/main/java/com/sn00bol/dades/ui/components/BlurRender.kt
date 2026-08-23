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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.adaptiveBlur(blurRadius: Dp, forceDisabled: Boolean = false): Modifier = 
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !forceDisabled && blurRadius > 0.dp) {
        this.blur(
            radius = blurRadius,
            edgeTreatment = BlurredEdgeTreatment.Unbounded
        )
    } else {
        this
    }

@Composable
fun BlurWrapper(
    isActive: Boolean,
    onDismiss: () -> Unit,
    blurEnabled: Boolean = true,
    maxBlurRadius: Dp = 20.dp,
    content: @Composable (blurModifier: Modifier) -> Unit
) {
    val isGaussianSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useGaussian = isGaussianSupported && blurEnabled

    // 1. Animate Blur radius
    val blurRadius by animateDpAsState(
        targetValue = if (isActive && useGaussian) maxBlurRadius else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "BlurRadiusAnimation"
    )

    // 2. Animate Overlay alpha
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isActive) {
            if (useGaussian) 0.25f else 0.75f
        } else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "OverlayAlphaAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        content(Modifier.adaptiveBlur(blurRadius, forceDisabled = !blurEnabled))

        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (useGaussian) {
                            // Frosted glass tint for high-end
                            MaterialTheme.colorScheme.surface.copy(alpha = overlayAlpha)
                        } else {
                            // Solid dim for low-end
                            Color.Black.copy(alpha = overlayAlpha)
                        }
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


@Composable
fun EdgeFadeOverlay(blurEnabled: Boolean = true) {
    // Only show edge fades on devices that support Gaussian Blur (Android 12+)
    // because these gradients are designed to complement the glass/blur look.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !blurEnabled) return

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                )
        )
    }
}
