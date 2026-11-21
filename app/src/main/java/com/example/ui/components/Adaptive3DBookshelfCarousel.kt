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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SkylightChampagneGlow
import com.example.ui.theme.SkylightSapphireGlow
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StudioCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
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
 * 2. **Dynamic Depth of Field (DoF)** combining hardware-accelerated quantized blur (API 31+) with
 *    atmospheric focal haze & opacity roll-off across all API levels (24-36), keeping the focused
 *    center monograph razor sharp (`0.dp` blur, zero offscreen blur overhead).
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
    // Real-time smoothed scroll velocity kinematics (pages/sec normalized into 0f..1f & -1f..1f)
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

    val speedFactor = animatedSpeedFactor.value.coerceIn(0f, 1f)
    val signedVelocity = animatedSignedVelocity.value.coerceIn(-1f, 1f)

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

        // 1) Whisper-soft full-bleed radial ambient spotlight (no hard circular edge)
        val isDark = LocalIsDarkTheme.current
        val haloChampagne = SkylightChampagneGlow
        val haloSapphire = SkylightSapphireGlow
        val pedestalShadowInk = if (isDark) Color.Black else InkPrimary

        Canvas(modifier = Modifier.fillMaxSize()) {
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

        // 2) Subtle grounding contact shadow directly beneath the monograph spine
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .width(bookCardWidth * lerp(0.84f, 0.96f, speedFactor))
                .height(14.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            pedestalShadowInk.copy(
                                alpha = lerp(
                                    if (isDark) 0.46f else 0.16f,
                                    if (isDark) 0.22f else 0.08f,
                                    speedFactor
                                )
                            ),
                            Color.Transparent
                        )
                    )
                )
        )

        // 4) Adaptive 3D Horizontal Pager with Scroll-Velocity Perspective & Optical Depth of Field
        // Avoid HorizontalPager(key = ...) which triggers a Compose Foundation 1.7 crash in
        // NearestRangeKeyIndexMap when pageCount grows across filter transitions (e.g. Videos -> Photos)
        // before latestKey updates; key the inner card composable instead with strict bounds checks.
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
            val rawOffsetFraction = pagerState.currentPageOffsetFraction
            val safeOffsetFraction = if (rawOffsetFraction.isFinite()) rawOffsetFraction else 0f
            val pageOffset = (pagerState.currentPage - page) + safeOffsetFraction
            val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1.5f)
            val clampedOffset = absOffset.coerceIn(0f, 1f)

            // Dynamic Perspective: tightens camera lens distance & deepens 3D rotation as scroll speed rises
            val dynamicCameraDistance = lerp(16.5f, 10.2f, speedFactor)
            val baseYawDeg = pageOffset * lerp(-22f, -31f, speedFactor)
            val velocityBankDeg = (-signedVelocity * 9.5f) * (0.45f + 0.55f * clampedOffset)
            val dynamicRotationY = (baseYawDeg + velocityBankDeg).coerceIn(-42f, 42f)
            val dynamicRotationX = (-speedFactor * 5.2f) * (1f - 0.35f * clampedOffset)

            // Parabolic Depth Arc & Dynamic Elevation
            val flankingScaleTarget = lerp(0.86f, 0.77f, speedFactor)
            val dynamicScale = lerp(1f, flankingScaleTarget, clampedOffset) * (1f - speedFactor * 0.025f * clampedOffset)
            val dynamicTranslationY = lerp(-speedFactor * 10f, lerp(20f, 38f, speedFactor), clampedOffset)
            val dynamicTranslationX = (-pageOffset.sign) * clampedOffset * speedFactor * 14f

            // Dynamic Depth of Field (DoF):
            // Center focused card stays razor sharp (0.dp blur) at rest; flanking cards defocus
            // proportionally to both spatial depth (absOffset) and scroll speed (speedFactor).
            val spatialDefocus = ((absOffset - 0.08f) / 0.92f).coerceIn(0f, 1f)
            val dofFactor = (spatialDefocus * lerp(0.45f, 1.0f, speedFactor)).coerceIn(0f, 1f)
            val dynamicAlpha = lerp(1f, lerp(0.76f, 0.58f, speedFactor), dofFactor)

            // Quantize GPU blur radius to 0.5dp increments & bypass completely below 0.5dp for zero overhead on focused card
            val rawBlurDp = if (supportsHardwareBlur && dofFactor > 0.06f) {
                val maxBlur = lerp(3.5f, 8.5f, speedFactor)
                (((dofFactor * maxBlur) * 2f).roundToInt() / 2f).coerceIn(0f, 8.5f)
            } else {
                0f
            }

            val kinematics = CarouselItemKinematics(
                pageOffset = pageOffset,
                absOffset = absOffset,
                depthOfFieldFactor = dofFactor,
                scrollSpeedFactor = speedFactor,
                isCentered = absOffset < 0.12f
            )

            key(itemKey(item)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.70f)
                        .zIndex(1f - clampedOffset)
                        .graphicsLayer {
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
                    // Apply hardware RenderEffect DoF blur ONLY to defocused flanking cards on API 31+
                    val dofModifier = if (rawBlurDp >= 0.5f) {
                        Modifier
                            .fillMaxSize()
                            .blur(radius = rawBlurDp.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    itemContent(item, page, kinematics, dofModifier)

                    // Atmospheric Depth-of-Field veil (provides focal-plane separation across ALL Android API 24-36!)
                    if (dofFactor > 0.05f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(itemShape)
                                .background(
                                    StudioCanvas.copy(alpha = (dofFactor * lerp(0.16f, 0.28f, speedFactor)).coerceIn(0f, 0.30f))
                                )
                        )
                    }
                }
            }
        }
    }
}
