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
import androidx.compose.ui.graphics.graphicsLayer
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

    val blurRadius by animateDpAsState(
        targetValue = if (isActive && useGaussian) maxBlurRadius else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "BlurRadiusAnimation"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isActive) {
            if (useGaussian) 0.25f else 0.75f
        } else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "OverlayAlphaAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        content(Modifier.adaptiveBlur(blurRadius, forceDisabled = !blurEnabled))

        if (overlayAlpha > 0.01f) {
            val tintColor = if (useGaussian) MaterialTheme.colorScheme.surface else Color.Black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = overlayAlpha
                    }
                    .background(tintColor)
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
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !blurEnabled) return

    val surfaceColor = MaterialTheme.colorScheme.surface
    val topGradient = remember(surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                surfaceColor.copy(alpha = 0.4f),
                surfaceColor.copy(alpha = 0f)
            )
        )
    }
    val bottomGradient = remember(surfaceColor) {
        Brush.verticalGradient(
            colors = listOf(
                surfaceColor.copy(alpha = 0f),
                surfaceColor.copy(alpha = 0.5f)
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(brush = topGradient)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(brush = bottomGradient)
        )
    }
}
