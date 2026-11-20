package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SkylightChampagneGlow
import com.example.ui.theme.SkylightRoseGlow
import com.example.ui.theme.SkylightSapphireGlow
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.StudioCanvasBottom
import com.example.ui.theme.StudioCanvasTop

/**
 * Architectural gallery atmosphere canvas supporting both Alabaster Daylight and Nocturnal
 * Leica Darkroom themes. Uses cached radial skylight gradients + hairline registration marks
 * drawn in a single `drawWithCache` pass with zero offscreen full-screen blur surfaces.
 */
@Composable
fun ArtisticGalleryBackdrop(
    modifier: Modifier = Modifier,
    warmAccent: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val canvasTop = StudioCanvasTop
    val canvasMid = StudioCanvas
    val canvasBottom = StudioCanvasBottom
    val sapphireGlow = SkylightSapphireGlow
    val champagneGlow = SkylightChampagneGlow
    val roseGlow = SkylightRoseGlow
    val borderColor = HairlineBorder

    val backdropBrush = remember(canvasTop, canvasMid, canvasBottom) {
        Brush.verticalGradient(
            colors = listOf(canvasTop, canvasMid, canvasBottom)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdropBrush)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val topRightCenter = Offset(size.width * 0.84f, size.height * 0.14f)
                    val topRightRadius = size.width * 0.82f
                    val topRightBrush = Brush.radialGradient(
                        colors = listOf(
                            if (warmAccent) {
                                champagneGlow.copy(alpha = if (isDark) 0.24f else 0.22f)
                            } else {
                                sapphireGlow.copy(alpha = if (isDark) 0.22f else 0.16f)
                            },
                            Color.Transparent
                        ),
                        center = topRightCenter,
                        radius = topRightRadius
                    )

                    val midLeftCenter = Offset(size.width * 0.15f, size.height * 0.54f)
                    val midLeftRadius = size.width * 0.72f
                    val midLeftBrush = Brush.radialGradient(
                        colors = listOf(
                            if (warmAccent) {
                                roseGlow.copy(alpha = if (isDark) 0.18f else 0.14f)
                            } else {
                                champagneGlow.copy(alpha = if (isDark) 0.18f else 0.20f)
                            },
                            Color.Transparent
                        ),
                        center = midLeftCenter,
                        radius = midLeftRadius
                    )

                    val bottomCenter = Offset(size.width * 0.72f, size.height * 0.92f)
                    val bottomRadius = size.width * 0.62f
                    val bottomBrush = Brush.radialGradient(
                        colors = listOf(
                            sapphireGlow.copy(alpha = if (isDark) 0.18f else 0.13f),
                            Color.Transparent
                        ),
                        center = bottomCenter,
                        radius = bottomRadius
                    )

                    val tickColor = borderColor.copy(alpha = if (isDark) 0.62f else 0.55f)
                    val tickLen = 14.dp.toPx()
                    val inset = 14.dp.toPx()
                    val stroke = 0.8.dp.toPx()

                    onDrawBehind {
                        drawCircle(brush = topRightBrush, radius = topRightRadius, center = topRightCenter)
                        drawCircle(brush = midLeftBrush, radius = midLeftRadius, center = midLeftCenter)
                        drawCircle(brush = bottomBrush, radius = bottomRadius, center = bottomCenter)

                        drawLine(tickColor, Offset(inset, inset * 3.5f), Offset(inset + tickLen, inset * 3.5f), stroke)
                        drawLine(tickColor, Offset(inset, inset * 3.5f), Offset(inset, inset * 3.5f + tickLen), stroke)
                        drawLine(tickColor, Offset(size.width - inset - tickLen, inset * 3.5f), Offset(size.width - inset, inset * 3.5f), stroke)
                        drawLine(tickColor, Offset(size.width - inset, inset * 3.5f), Offset(size.width - inset, inset * 3.5f + tickLen), stroke)
                    }
                }
        )

        content()
    }
}
