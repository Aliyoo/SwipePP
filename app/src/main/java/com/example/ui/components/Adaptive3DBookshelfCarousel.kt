package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.example.data.PhotoItemEntity
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SkylightChampagneGlow
import com.example.ui.theme.SkylightSapphireGlow
import com.example.ui.theme.StudioCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Per-card optical & kinematic state provided by [Adaptive3DBookshelfCarousel].
 */
@Stable
data class CarouselItemKinematics(
    val pageOffset: Float,
    val absOffset: Float,
    val depthOfFieldFactor: Float,
    val scrollSpeedFactor: Float,
    val isCentered: Boolean
)

/**
 * Adaptive 3D Bookshelf Carousel that dynamically modulates:
 * 1. **Camera Perspective (`cameraDistance`, `rotationY`, `rotationX`)** based on real-time scroll speed
 *    and directional momentum (aero-banking + cinematic lens pull-in during fast flicks).
 * 2. **Atmospheric Depth of Field (DoF)** executed strictly in GPU `graphicsLayer` with zero
 *    per-frame recompositions and zero live RenderEffect blur stalls.
 * 3. **Responsive Stage Geometry** via [BoxWithConstraints] for phones, foldables, and tablets.
 */
@Composable
fun <T> Adaptive3DBookshelfCarousel(
    items: List<T>,
    pagerState: PagerState,
    settledCoverPhoto: PhotoItemEntity?,
    itemKey: (T) -> Any,
    itemShape: Shape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp, topEnd = 22.dp, bottomEnd = 22.dp),
    pagerTestTag: String = "bookshelf_3d_pager",
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, index: Int, kinematics: CarouselItemKinematics, cardModifier: Modifier) -> Unit
) {
    // Real-time smoothed scroll velocity kinematics (pages/sec normalized into 0f..1f & -1f..1f).
    // IMPORTANT: Never read `.value` in composition scope — read ONLY inside Canvas & graphicsLayer!
    val animatedSpeedFactor = remember { Animatable(0f) }
    val animatedSignedVelocity = remember { Animatable(0f) }
    var lastVelocitySampleNanos by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pagerState) {
        val initFrac = pagerState.currentPageOffsetFraction.let { if (it.isFinite()) it else 0f }
        var prevPosition = pagerState.currentPage + initFrac
        var prevTimeNanos = System.nanoTime()
        var smoothedSpeed = 0f
        var smoothedSigned = 0f

        snapshotFlow {
            val frac = pagerState.currentPageOffsetFraction
            pagerState.currentPage + (if (frac.isFinite()) frac else 0f)
        }.collect { currentPos ->
            val now = System.nanoTime()
            val dtSec = ((now - prevTimeNanos) / 1_000_000_000f).coerceIn(0.004f, 0.25f)
            val delta = currentPos - prevPosition
            prevPosition = currentPos
            prevTimeNanos = now

            if (delta.isFinite() && delta.absoluteValue > 0.0005f) {
                val pagesPerSec = (delta / dtSec).coerceIn(-6.5f, 6.5f)
                val normalizedSpeed = (pagesPerSec.absoluteValue / 4.2f).coerceIn(0f, 1f)
                val normalizedSigned = (pagesPerSec / 4.2f).coerceIn(-1f, 1f)

                // Low-pass filter to eliminate pointer jitter while staying super responsive
                smoothedSpeed = lerp(smoothedSpeed, normalizedSpeed, 0.42f)
                smoothedSigned = lerp(smoothedSigned, normalizedSigned, 0.38f)
                lastVelocitySampleNanos = now.toFloat()

                launch {
                    animatedSpeedFactor.snapTo(smoothedSpeed)
                    animatedSignedVelocity.snapTo(smoothedSigned)
                }
            }
        }
    }

    // Spring-decay perspective & depth-of-field gracefully back to calm gallery rest when scrolling stops
    LaunchedEffect(pagerState.isScrollInProgress, lastVelocitySampleNanos) {
        if (!pagerState.isScrollInProgress) {
            launch {
                animatedSpeedFactor.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                animatedSignedVelocity.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } else {
            // If user holds finger still mid-drag, smoothly settle velocity after 90ms idle
            delay(90L)
            launch {
                animatedSpeedFactor.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
            }
            launch {
                animatedSignedVelocity.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Adaptive book dimensions respecting both container width AND available height
        val isWideScreen = maxWidth >= 520.dp
        val maxCardWidthByHeight = ((maxHeight - 20.dp).coerceAtLeast(180.dp) * 0.70f)
        val widthFraction = if (isWideScreen) 0.46f else 0.72f
        val bookCardWidth = (maxWidth * widthFraction)
            .coerceAtMost(maxCardWidthByHeight)
            .coerceIn(210.dp, 312.dp)
        val horizontalPad = ((maxWidth - bookCardWidth) / 2f).coerceAtLeast(22.dp)
        val adaptiveSpacing = if (isWideScreen) 22.dp else 14.dp

        // 1) Whisper-soft full-bleed radial ambient spotlight (draw-phase only)
        val isDark = LocalIsDarkTheme.current
        val haloChampagne = SkylightChampagneGlow
        val haloSapphire = SkylightSapphireGlow
        val pedestalShadowInk = if (isDark) Color.Black else InkPrimary
        val veilColor = StudioCanvas
        val groundingBrush = remember(pedestalShadowInk, isDark) {
            Brush.radialGradient(
                colors = listOf(
                    pedestalShadowInk.copy(alpha = if (isDark) 0.46f else 0.16f),
                    Color.Transparent
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val speedFactor = animatedSpeedFactor.value.coerceIn(0f, 1f)
            val signedVelocity = animatedSignedVelocity.value.coerceIn(-1f, 1f)
            val haloRadius = (size.maxDimension * lerp(0.54f, 0.62f, speedFactor)).coerceAtLeast(1f)
            val centerPt = Offset(
                x = size.width * 0.5f - signedVelocity * size.width * 0.05f,
                y = size.height * 0.46f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        haloChampagne.copy(alpha = lerp(0.16f, 0.24f, speedFactor)),
                        haloSapphire.copy(alpha = lerp(0.07f, 0.14f, speedFactor)),
                        Color.Transparent
                    ),
                    center = centerPt,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = centerPt
            )
        }

        // 2) Subtle grounding contact shadow directly beneath the monograph spine (draw-phase animated)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .width(bookCardWidth * 0.88f)
                .height(14.dp)
                .graphicsLayer {
                    val speedFactor = animatedSpeedFactor.value.coerceIn(0f, 1f)
                    scaleX = lerp(0.96f, 1.09f, speedFactor)
                    alpha = lerp(1f, 0.48f, speedFactor)
                }
                .clip(CircleShape)
                .background(groundingBrush)
        )

        // 3) Adaptive 3D Horizontal Pager with Zero-Recomposition GPU Perspective & Atmospheric DoF
        val safeBeyondViewport = (items.size - 1).coerceIn(0, 2)
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(bookCardWidth),
            contentPadding = PaddingValues(horizontal = horizontalPad),
            pageSpacing = adaptiveSpacing,
            beyondViewportPageCount = safeBeyondViewport,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(pagerTestTag)
        ) { page ->
            val item = items.getOrNull(page) ?: return@HorizontalPager
            val isCurrentPage by remember(page, pagerState) {
                derivedStateOf { pagerState.currentPage == page }
            }
            val isSettledCenter by remember(page, pagerState) {
                derivedStateOf { pagerState.settledPage == page }
            }
            val kinematics = remember(isSettledCenter) {
                CarouselItemKinematics(
                    pageOffset = 0f,
                    absOffset = if (isSettledCenter) 0f else 1f,
                    depthOfFieldFactor = if (isSettledCenter) 0f else 0.45f,
                    scrollSpeedFactor = 0f,
                    isCentered = isSettledCenter
                )
            }

            key(itemKey(item)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.70f)
                        .zIndex(if (isCurrentPage) 2f else 1f)
                        .graphicsLayer {
                            val rawOffsetFraction = pagerState.currentPageOffsetFraction
                            val safeOffsetFraction = if (rawOffsetFraction.isFinite()) rawOffsetFraction else 0f
                            val pageOffset = (pagerState.currentPage - page) + safeOffsetFraction
                            val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1.5f)
                            val clampedOffset = absOffset.coerceIn(0f, 1f)
                            val speedFactor = animatedSpeedFactor.value.coerceIn(0f, 1f)
                            val signedVelocity = animatedSignedVelocity.value.coerceIn(-1f, 1f)

                            val dynamicCameraDistance = lerp(16.5f, 10.2f, speedFactor)
                            val baseYawDeg = pageOffset * lerp(-22f, -31f, speedFactor)
                            val velocityBankDeg = (-signedVelocity * 9.5f) * (0.45f + 0.55f * clampedOffset)
                            val dynamicRotationY = (baseYawDeg + velocityBankDeg).coerceIn(-42f, 42f)
                            val dynamicRotationX = (-speedFactor * 5.2f) * (1f - 0.35f * clampedOffset)

                            val flankingScaleTarget = lerp(0.86f, 0.77f, speedFactor)
                            val dynamicScale = lerp(1f, flankingScaleTarget, clampedOffset) * (1f - speedFactor * 0.025f * clampedOffset)
                            val dynamicTranslationY = lerp(-speedFactor * 10f, lerp(20f, 38f, speedFactor), clampedOffset)
                            val dynamicTranslationX = (-pageOffset.sign) * clampedOffset * speedFactor * 14f

                            val spatialDefocus = ((absOffset - 0.08f) / 0.92f).coerceIn(0f, 1f)
                            val dofFactor = (spatialDefocus * lerp(0.45f, 1.0f, speedFactor)).coerceIn(0f, 1f)
                            val dynamicAlpha = lerp(1f, lerp(0.78f, 0.60f, speedFactor), dofFactor)

                            scaleX = dynamicScale
                            scaleY = dynamicScale
                            alpha = dynamicAlpha
                            rotationY = dynamicRotationY
                            rotationX = dynamicRotationX
                            translationY = dynamicTranslationY
                            translationX = dynamicTranslationX
                            cameraDistance = dynamicCameraDistance * density
                        }
                ) {
                    itemContent(item, page, kinematics, Modifier.fillMaxSize())

                    // Atmospheric Depth-of-Field focal veil (GPU draw-phase alpha, zero recomposition, zero blur FBO)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val rawOffsetFraction = pagerState.currentPageOffsetFraction
                                val safeOffsetFraction = if (rawOffsetFraction.isFinite()) rawOffsetFraction else 0f
                                val absOffset = ((pagerState.currentPage - page) + safeOffsetFraction)
                                    .absoluteValue.coerceIn(0f, 1.5f)
                                val speedFactor = animatedSpeedFactor.value.coerceIn(0f, 1f)
                                val spatialDefocus = ((absOffset - 0.08f) / 0.92f).coerceIn(0f, 1f)
                                val dofFactor = (spatialDefocus * lerp(0.45f, 1.0f, speedFactor)).coerceIn(0f, 1f)
                                alpha = if (dofFactor > 0.05f) {
                                    (dofFactor * lerp(0.16f, 0.28f, speedFactor)).coerceIn(0f, 0.30f)
                                } else {
                                    0f
                                }
                            }
                            .clip(itemShape)
                            .background(veilColor)
                    )
                }
            }
        }
    }
}
