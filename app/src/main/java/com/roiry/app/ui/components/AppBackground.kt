package com.roiry.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    
    // Animation states for 4 different blobs
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = Math.PI.toFloat() * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val primaryColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    val accentColor = Color(0xFFD1FAE5).copy(alpha = 0.5f) // Mint accent
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f)) {
            val width = size.width
            val height = size.height

            // Blob 1: Moving around top-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor, Color.Transparent),
                    center = Offset(
                        x = width * 0.2f + (width * 0.1f * sin(time)),
                        y = height * 0.2f + (height * 0.1f * cos(time))
                    ),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(
                    x = width * 0.2f + (width * 0.1f * sin(time)),
                    y = height * 0.2f + (height * 0.1f * cos(time))
                )
            )

            // Blob 2: Moving around bottom-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor, Color.Transparent),
                    center = Offset(
                        x = width * 0.8f + (width * 0.15f * cos(time * 0.8f)),
                        y = height * 0.8f + (height * 0.15f * sin(time * 0.8f))
                    ),
                    radius = width * 0.9f
                ),
                radius = width * 0.9f,
                center = Offset(
                    x = width * 0.8f + (width * 0.15f * cos(time * 0.8f)),
                    y = height * 0.8f + (height * 0.15f * sin(time * 0.8f))
                )
            )

            // Blob 3: Moving middle-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor, Color.Transparent),
                    center = Offset(
                        x = width * 0.1f + (width * 0.2f * sin(time * 0.5f)),
                        y = height * 0.5f + (height * 0.2f * cos(time * 0.5f))
                    ),
                    radius = width * 0.7f
                ),
                radius = width * 0.7f,
                center = Offset(
                    x = width * 0.1f + (width * 0.2f * sin(time * 0.5f)),
                    y = height * 0.5f + (height * 0.2f * cos(time * 0.5f))
                )
            )

            // Blob 4: Top-right accent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(
                        x = width * 0.9f + (width * 0.1f * sin(time * 1.2f)),
                        y = height * 0.1f + (height * 0.1f * cos(time * 1.2f))
                    ),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(
                    x = width * 0.9f + (width * 0.1f * sin(time * 1.2f)),
                    y = height * 0.1f + (height * 0.1f * cos(time * 1.2f))
                )
            )
        }

        // Overlay Noise/Texture effect (simulated with a very subtle gradient or fine dots if we had a shader)
        // For now, a subtle overall gradient to blend everything
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.02f)
                        )
                    )
                )
        )

        content()
    }
}
