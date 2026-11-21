package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CleanupCardItem
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SurfacePure
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ArcTimeGranularity {
    HOURS,  // Single day span
    DAYS,   // Single month / multi-day span
    MONTHS  // Multi-month / full-year span
}

/**
 * Stage 2: "Arc as Time Scale" — replaces the decorative background arc with an interactive
 * quadratic-Bézier timeline scrubber.
 *
 * - Range adapts automatically to the active queue span (hours for 1 day, days for 1 month,
 *   months for full year / multi-month spans).
 * - A live cursor thumb reflects the current card's position along the arc.
 * - Dragging or tapping along the arc jumps to the card nearest that timestamp and triggers
 *   a light haptic tick on release.
 */
@Composable
fun ArcTimeScaleScrubber(
    queue: List<CleanupCardItem>,
    currentIndex: Int,
    onJumpToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isDark = LocalIsDarkTheme.current

    val timestamps = remember(queue) {
        queue.map { it.primaryPhoto.takenAtMillis }
    }
    val minTs = remember(timestamps) { timestamps.minOrNull() ?: 0L }
    val maxTs = remember(timestamps) { timestamps.maxOrNull() ?: minTs }
    val spanMs = (maxTs - minTs).coerceAtLeast(1L)

    val granularity = remember(spanMs) {
        when {
            spanMs <= 36L * 3_600_000L -> ArcTimeGranularity.HOURS
            spanMs <= 45L * 86_400_000L -> ArcTimeGranularity.DAYS
            else -> ArcTimeGranularity.MONTHS
        }
    }

    val safeIndex = currentIndex.coerceIn(0, queue.lastIndex)
    val currentTs = timestamps[safeIndex]

    val currentFractionFromQueue = remember(safeIndex, timestamps, minTs, maxTs, queue.size) {
        if (queue.size <= 1) {
            0.5f
        } else if (maxTs > minTs) {
            ((currentTs - minTs).toDouble() / (maxTs - minTs).toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            (safeIndex.toFloat() / queue.lastIndex.toFloat()).coerceIn(0f, 1f)
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(currentFractionFromQueue) }
    var lastJumpedIndex by remember { mutableIntStateOf(safeIndex) }
    var widthPx by remember { mutableIntStateOf(1) }

    val activeFraction = if (isDragging) dragFraction else currentFractionFromQueue
    val activeTimestamp = if (isDragging && maxTs > minTs) {
        minTs + (spanMs * dragFraction).toLong()
    } else {
        currentTs
    }

    val latestTimestamps by rememberUpdatedState(timestamps)
    val latestOnJump by rememberUpdatedState(onJumpToIndex)

    fun nearestIndexForFraction(t: Float): Int {
        val tsList = latestTimestamps
        if (tsList.size <= 1) return 0
        val clamped = t.coerceIn(0f, 1f)
        if (maxTs <= minTs) {
            return (clamped * tsList.lastIndex).roundToInt().coerceIn(0, tsList.lastIndex)
        }
        val targetTs = minTs + (spanMs * clamped).toLong()
        var bestIdx = 0
        var bestDiff = Long.MAX_VALUE
        for (i in tsList.indices) {
            val diff = abs(tsList[i] - targetTs)
            if (diff < bestDiff) {
                bestDiff = diff
                bestIdx = i
            }
        }
        return bestIdx
    }

    val startLabel = remember(minTs, granularity) { formatArcTickLabel(minTs, granularity) }
    val midLabel = remember(minTs, maxTs, granularity) {
        formatArcTickLabel(minTs + (maxTs - minTs) / 2L, granularity)
    }
    val endLabel = remember(maxTs, granularity) { formatArcTickLabel(maxTs, granularity) }
    val currentLabel = remember(activeTimestamp, granularity) {
        formatArcCursorLabel(activeTimestamp, granularity)
    }

    val trackBaseColor = if (isDark) CobaltPrimary.copy(alpha = 0.24f) else CobaltPrimary.copy(alpha = 0.20f)
    val activeArcColor = CobaltPrimary
    val tickColor = if (isDark) Color.White.copy(alpha = 0.28f) else InkSecondary.copy(alpha = 0.34f)
    val thumbHalo = if (isDark) CobaltPrimary.copy(alpha = 0.34f) else CobaltSoft

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 18.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .semantics {
                contentDescription = "时间刻度弧，当前 $currentLabel，第 ${safeIndex + 1} 项，共 ${queue.size} 项"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = activeFraction,
                    range = 0f..1f
                )
                setProgress { targetFraction ->
                    val idx = nearestIndexForFraction(targetFraction)
                    latestOnJump(idx)
                    true
                }
            }
            .pointerInput(queue.size, minTs, maxTs) {
                detectTapGestures { offset ->
                    val padPx = with(density) { 14.dp.toPx() }
                    val usable = (size.width - padPx * 2f).coerceAtLeast(1f)
                    val t = ((offset.x - padPx) / usable).coerceIn(0f, 1f)
                    val idx = nearestIndexForFraction(t)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    latestOnJump(idx)
                }
            }
            .pointerInput(queue.size, minTs, maxTs) {
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        isDragging = true
                        val padPx = with(density) { 14.dp.toPx() }
                        val usable = (size.width - padPx * 2f).coerceAtLeast(1f)
                        dragFraction = ((startOffset.x - padPx) / usable).coerceIn(0f, 1f)
                        val idx = nearestIndexForFraction(dragFraction)
                        lastJumpedIndex = idx
                        latestOnJump(idx)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val padPx = with(density) { 14.dp.toPx() }
                        val usable = (size.width - padPx * 2f).coerceAtLeast(1f)
                        dragFraction = ((change.position.x - padPx) / usable).coerceIn(0f, 1f)
                        val idx = nearestIndexForFraction(dragFraction)
                        if (idx != lastJumpedIndex) {
                            lastJumpedIndex = idx
                            latestOnJump(idx)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
            .testTag("arc_time_scale_scrubber")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padX = 14.dp.toPx()
            val startX = padX
            val endX = (size.width - padX).coerceAtLeast(startX + 1f)
            val baseY = size.height * 0.68f
            val peakY = size.height * 0.22f
            val controlX = (startX + endX) * 0.5f
            // Quadratic Bézier control Y so midpoint (t = 0.5) lands at peakY
            val controlY = 2f * peakY - baseY

            fun pointOnArc(t: Float): Offset {
                val u = 1f - t
                val x = u * u * startX + 2f * u * t * controlX + t * t * endX
                val y = u * u * baseY + 2f * u * t * controlY + t * t * baseY
                return Offset(x, y)
            }

            // Full arc track
            val fullPath = Path().apply {
                moveTo(startX, baseY)
                quadraticTo(controlX, controlY, endX, baseY)
            }
            drawPath(
                path = fullPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        trackBaseColor.copy(alpha = 0.14f),
                        trackBaseColor,
                        trackBaseColor.copy(alpha = 0.14f)
                    )
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active progress segment along arc (sampled smoothly)
            val steps = 28
            val activeSteps = (steps * activeFraction).roundToInt().coerceIn(0, steps)
            if (activeSteps > 0) {
                val activePath = Path().apply {
                    val p0 = pointOnArc(0f)
                    moveTo(p0.x, p0.y)
                    for (s in 1..activeSteps) {
                        val t = s.toFloat() / steps.toFloat()
                        val pt = pointOnArc(t)
                        lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = activePath,
                    color = activeArcColor,
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Subtle time tick marks along the curve
            val tickCount = 9
            for (i in 0 until tickCount) {
                val t = i.toFloat() / (tickCount - 1).toFloat()
                val pt = pointOnArc(t)
                val isMajor = i == 0 || i == tickCount / 2 || i == tickCount - 1
                val halfLen = if (isMajor) 4.dp.toPx() else 2.2.dp.toPx()
                drawLine(
                    color = if (t <= activeFraction) activeArcColor.copy(alpha = 0.72f) else tickColor,
                    start = Offset(pt.x, pt.y - halfLen),
                    end = Offset(pt.x, pt.y + halfLen),
                    strokeWidth = if (isMajor) 1.6.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draggable cursor thumb on arc
            val thumbCenter = pointOnArc(activeFraction)
            val haloRadius = if (isDragging) 14.dp.toPx() else 10.dp.toPx()
            val coreRadius = if (isDragging) 7.dp.toPx() else 5.5.dp.toPx()
            drawCircle(color = thumbHalo, radius = haloRadius, center = thumbCenter)
            drawCircle(color = activeArcColor, radius = coreRadius, center = thumbCenter)
            drawCircle(color = Color.White, radius = coreRadius * 0.42f, center = thumbCenter)
        }

        // Start / Mid / End labels anchored neatly below the arc
        Text(
            text = startLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = InkSecondary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 0.dp)
        )
        Text(
            text = if (isDragging) currentLabel else midLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium,
            color = if (isDragging) CobaltPrimary else InkSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
        )
        Text(
            text = endLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = InkSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 0.dp)
        )

        // Floating live timestamp callout while scrubbing
        if (isDragging) {
            val padPx = with(density) { 14.dp.toPx() }
            val usable = (widthPx - padPx * 2f).coerceAtLeast(1f)
            val xPx = (padPx + usable * activeFraction).roundToInt()
            val halfPillPx = with(density) { 44.dp.toPx().roundToInt() }
            val clampedX = (xPx - halfPillPx).coerceIn(0, (widthPx - halfPillPx * 2).coerceAtLeast(0))
            Surface(
                shape = RoundedCornerShape(50),
                color = SurfacePure,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                modifier = Modifier
                    .offset { IntOffset(clampedX, -with(density) { 10.dp.roundToPx() }) }
            ) {
                Text(
                    text = currentLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private fun formatArcTickLabel(millis: Long, granularity: ArcTimeGranularity): String {
    if (millis <= 0L) return ""
    val pattern = when (granularity) {
        ArcTimeGranularity.HOURS -> "HH:mm"
        ArcTimeGranularity.DAYS -> "M/d"
        ArcTimeGranularity.MONTHS -> "M月"
    }
    return SimpleDateFormat(pattern, Locale.CHINA).format(Date(millis))
}

private fun formatArcCursorLabel(millis: Long, granularity: ArcTimeGranularity): String {
    if (millis <= 0L) return ""
    val pattern = when (granularity) {
        ArcTimeGranularity.HOURS -> "M月d日 HH:mm"
        ArcTimeGranularity.DAYS -> "M月d日 HH:mm"
        ArcTimeGranularity.MONTHS -> "yyyy年M月d日"
    }
    return SimpleDateFormat(pattern, Locale.CHINA).format(Date(millis))
}
