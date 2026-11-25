package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.rememberHapticEngine
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// 摩登画报造型语法：旗签 = 金描边纸面小签，3dp 小圆角（弃 14~18dp 大圆角玻璃旗）
private val FootprintFlagExpandedShape = RoundedCornerShape(3.dp)
private val FootprintFlagIdleShape = RoundedCornerShape(3.dp)

/**
 * Unified stop entry for the right-edge [FootprintLocationScrubRail] and bottom-card stepper.
 * Covers both geotagged [com.example.ui.LocationCluster] entries and the optional trailing
 * unlocated-media bucket so every footprint remains reachable with one thumb.
 */
@Immutable
data class FootprintScrubStop(
    val id: String,
    val displayName: String,
    val mediaCount: Int,
    val unreviewedCount: Int,
    val totalBytes: Long,
    val latitude: Double,
    val longitude: Double,
    val isUnlocated: Boolean = false
) {
    val isCleaned: Boolean
        get() = !isUnlocated && unreviewedCount == 0

    val shortIdleLabel: String
        get() = when {
            isUnlocated -> "无定位"
            displayName.length <= 3 -> displayName
            else -> displayName.take(3)
        }
}

/**
 * One-handed right-edge location fast-scrub rail matching the tactile & visual grammar of
 * Gallery's [TimelineMonthStampOverlay].
 *
 * - Vertical drag previews locations live in a gold-edged paper flag anchored left of the thumb
 *   with crisp detent haptics while keeping the map camera calm.
 * - Releasing the finger (or tapping anywhere along the rail) commits the highlighted stop and
 *   triggers the smooth camera flight to that location.
 */
@Composable
fun FootprintLocationScrubRail(
    stops: List<FootprintScrubStop>,
    activeIndex: Int,
    onCommitStop: (FootprintScrubStop) -> Unit,
    topPadding: Dp = 84.dp,
    bottomPadding: Dp = 152.dp,
    modifier: Modifier = Modifier
) {
    if (stops.size < 2) return

    val haptics = rememberHapticEngine()
    val density = LocalDensity.current

    val safeActiveIndex = activeIndex.coerceIn(0, stops.lastIndex)
    val latestStops by rememberUpdatedState(stops)
    val latestOnCommitStop by rememberUpdatedState(onCommitStop)

    var isDragging by remember { mutableStateOf(false) }
    var lingerVisible by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var previewIndex by remember { mutableIntStateOf(safeActiveIndex) }
    var lastVisualIndex by remember { mutableIntStateOf(safeActiveIndex) }

    // Keep preview synced whenever external selection changes (map pin tap or bottom card stepper)
    LaunchedEffect(safeActiveIndex, stops.size) {
        if (!isDragging) {
            val changedExternally = lastVisualIndex != safeActiveIndex
            previewIndex = safeActiveIndex
            lastVisualIndex = safeActiveIndex
            if (changedExternally) {
                lingerVisible = true
            }
        }
    }

    // Brief confirmation linger after release or external jump, then smoothly contract to idle tab
    LaunchedEffect(isDragging, lingerVisible, previewIndex) {
        if (!isDragging && lingerVisible) {
            delay(950L)
            lingerVisible = false
        }
    }

    val lastStopIndex = (stops.size - 1).coerceAtLeast(1)
    val committedFraction = (safeActiveIndex.toFloat() / lastStopIndex.toFloat()).coerceIn(0f, 1f)
    val smoothCommittedFraction by animateFloatAsState(
        targetValue = committedFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "footprint_rail_committed_fraction"
    )

    val displayFraction = if (isDragging) dragFraction else smoothCommittedFraction
    val displayIndex = (if (isDragging) previewIndex else safeActiveIndex).coerceIn(0, stops.lastIndex)
    val displayStop = stops[displayIndex]
    val flagExpanded = isDragging || lingerVisible

    val railAlpha by animateFloatAsState(
        targetValue = if (flagExpanded) 1f else 0.86f,
        animationSpec = tween(200),
        label = "footprint_rail_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = topPadding, bottom = bottomPadding, end = 4.dp)
            .graphicsLayer { alpha = railAlpha },
        contentAlignment = Alignment.TopEnd
    ) {
        val trackHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val handleHeightPx = with(density) { 48.dp.toPx() }
        val handleHalfPx = handleHeightPx * 0.5f
        val travelPx = (trackHeightPx - handleHeightPx).coerceAtLeast(1f)
        val indicatorOffsetY = (displayFraction * travelPx).roundToInt()

        // 摩登画报取色：轨道 = 压照片暗 scrim 细轨（同 TimelineMonthStampOverlay 语法）；
        // 停留点 = 金(已整理/当前) / 朱红(待理) / 墨(无定位)，拖动指示 = 金发丝
        val trackScrimColor = DecoScrimInk.copy(alpha = 0.32f)
        val unlocatedDotColor = DecoInkOnGround.copy(alpha = 0.55f)
        val cleanedDotColor = DecoGold
        val activeDotColor = DecoGold
        val unreviewedDotColor = DecoVerm.copy(alpha = 0.85f)
        val railGold = DecoGold

        // Full-height 48dp-wide touch zone on the right edge (handles immediate drag + direct tap
        // while consuming pointer events so the underlying map never pans during location scrubbing)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(48.dp)
                .fillMaxHeight()
                .semantics {
                    contentDescription = "足迹地点快滑轨，当前${displayStop.displayName}，第${displayIndex + 1}项，共${stops.size}项"
                    progressBarRangeInfo = ProgressBarRangeInfo(displayFraction, 0f..1f)
                    setProgress { target ->
                        val clamped = target.coerceIn(0f, 1f)
                        val idx = (clamped * lastStopIndex).roundToInt().coerceIn(0, latestStops.lastIndex)
                        previewIndex = idx
                        dragFraction = clamped
                        lingerVisible = true
                        latestOnCommitStop(latestStops[idx])
                        true
                    }
                }
                .pointerInput(stops.size, travelPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val currentStops = latestStops
                        if (currentStops.size < 2) return@awaitEachGesture
                        val maxIdx = currentStops.lastIndex

                        fun fractionForY(y: Float): Float =
                            ((y - handleHalfPx) / travelPx).coerceIn(0f, 1f)

                        var localFraction = fractionForY(down.position.y)
                        var localIndex = (localFraction * maxIdx).roundToInt().coerceIn(0, maxIdx)

                        isDragging = true
                        lingerVisible = true
                        dragFraction = localFraction
                        if (localIndex != previewIndex) {
                            previewIndex = localIndex
                            haptics.bookshelfDetent()
                        } else {
                            previewIndex = localIndex
                        }

                        var cancelled = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointer = event.changes.firstOrNull { it.pressed }
                            if (activePointer == null) {
                                // Check whether gesture ended normally or was cancelled
                                cancelled = event.changes.any { it.isConsumed && !it.changedToUpIgnoreConsumed() }
                                event.changes.forEach { it.consume() }
                                break
                            }
                            activePointer.consume()
                            localFraction = fractionForY(activePointer.position.y)
                            val nextIdx = (localFraction * maxIdx).roundToInt().coerceIn(0, maxIdx)
                            dragFraction = localFraction
                            if (nextIdx != localIndex) {
                                localIndex = nextIdx
                                previewIndex = nextIdx
                                haptics.bookshelfDetent()
                            }
                        }

                        isDragging = false
                        if (!cancelled && localIndex in currentStops.indices) {
                            lastVisualIndex = localIndex
                            haptics.pageFlipSettle()
                            latestOnCommitStop(currentStops[localIndex])
                        }
                    }
                }
                .drawBehind {
                    // Slender translucent vertical track along the far right edge
                    val trackWidth = 3.dp.toPx()
                    val trackX = size.width - 6.dp.toPx()
                    drawRoundRect(
                        color = trackScrimColor,
                        topLeft = Offset(trackX - trackWidth * 0.5f, handleHalfPx),
                        size = androidx.compose.ui.geometry.Size(trackWidth, travelPx),
                        cornerRadius = CornerRadius(trackWidth, trackWidth)
                    )

                    // 拖动/已选进度指示 = 金发丝（deco 语法 0.8~1.5dp 金线）
                    drawLine(
                        color = railGold.copy(alpha = 0.9f),
                        start = Offset(trackX, handleHalfPx),
                        end = Offset(trackX, handleHalfPx + displayFraction * travelPx),
                        strokeWidth = 1.2.dp.toPx()
                    )

                    // Precision stop dots distributed evenly along the track
                    val maxDots = stops.size.coerceAtMost(24)
                    for (dotIdx in 0 until maxDots) {
                        val stopIndex = if (maxDots == stops.size) {
                            dotIdx
                        } else {
                            ((dotIdx.toFloat() / (maxDots - 1).coerceAtLeast(1)) * lastStopIndex)
                                .roundToInt()
                                .coerceIn(0, stops.lastIndex)
                        }
                        val stop = stops[stopIndex]
                        val dotFraction = stopIndex.toFloat() / lastStopIndex.toFloat()
                        val cy = handleHalfPx + dotFraction * travelPx
                        val isHighlighted = stopIndex == displayIndex

                        val dotColor = when {
                            stop.isUnlocated -> unlocatedDotColor
                            stop.isCleaned -> cleanedDotColor
                            isHighlighted -> activeDotColor
                            else -> unreviewedDotColor
                        }

                        if (isHighlighted) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.28f),
                                radius = 7.dp.toPx(),
                                center = Offset(trackX, cy)
                            )
                            drawCircle(
                                color = dotColor,
                                radius = 4.dp.toPx(),
                                center = Offset(trackX, cy)
                            )
                        } else {
                            drawCircle(
                                color = dotColor.copy(alpha = if (flagExpanded) 0.92f else 0.58f),
                                radius = 2.2.dp.toPx(),
                                center = Offset(trackX, cy)
                            )
                        }
                    }
                }
                .testTag("footprint_scrub_rail")
        )

        // Floating Location Flag anchored to the active/previewed Y offset (zero parent relayout;
        // also directly draggable so grabbing any part of the visible flag tab initiates scrubbing)
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .offset { IntOffset(x = 0, y = indicatorOffsetY) }
                .heightIn(min = 48.dp)
                .padding(end = if (flagExpanded) 22.dp else 12.dp)
                .pointerInput(stops.size, travelPx) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            val currentStops = latestStops
                            if (currentStops.size < 2) return@detectVerticalDragGestures
                            isDragging = true
                            lingerVisible = true
                            dragFraction = displayFraction
                            previewIndex = displayIndex
                        },
                        onDragEnd = {
                            val currentStops = latestStops
                            isDragging = false
                            if (previewIndex in currentStops.indices) {
                                lastVisualIndex = previewIndex
                                haptics.pageFlipSettle()
                                latestOnCommitStop(currentStops[previewIndex])
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val currentStops = latestStops
                            if (currentStops.size < 2) return@detectVerticalDragGestures
                            val maxIdx = currentStops.lastIndex
                            dragFraction = (dragFraction + dragAmount / travelPx).coerceIn(0f, 1f)
                            val nextIdx = (dragFraction * maxIdx).roundToInt().coerceIn(0, maxIdx)
                            if (nextIdx != previewIndex) {
                                previewIndex = nextIdx
                                haptics.bookshelfDetent()
                            }
                        }
                    )
                }
                .testTag("footprint_scrub_flag")
        ) {
            AnimatedContent(
                targetState = flagExpanded,
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.90f, animationSpec = tween(160))) togetherWith
                        (fadeOut(tween(110)) + scaleOut(targetScale = 0.92f, animationSpec = tween(120)))
                },
                label = "footprint_flag_expand_state"
            ) { expanded ->
                if (expanded) {
                    ExpandedFootprintScrubFlag(
                        stop = displayStop,
                        stopIndex = displayIndex,
                        totalStops = stops.size
                    )
                } else {
                    CompactIdleFootprintFlag(
                        stop = displayStop
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactIdleFootprintFlag(
    stop: FootprintScrubStop
) {
    val gold = DecoGold
    val verm = DecoVerm
    val mat = DecoMat
    val ink = DecoInkOnGround
    val iconTint = when {
        stop.isUnlocated -> ink.copy(alpha = 0.55f)
        stop.isCleaned -> gold
        else -> verm
    }

    // 金描边纸面小签（3dp 圆角）：旗签本体 = DecoMat 纸面 + 弱金描边，压地图仍清晰
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(FootprintFlagIdleShape)
            .background(mat)
            .border(1.dp, gold.copy(alpha = 0.8f), FootprintFlagIdleShape)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        DecoIcon(
            kind = if (stop.isUnlocated) DecoIconKind.LOCATION_OFF else DecoIconKind.LOCATION,
            color = iconTint,
            iconSize = 13.dp
        )
        Text(
            text = stop.shortIdleLabel,
            fontFamily = DecoDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = ink,
            maxLines = 1
        )
    }
}

@Composable
private fun ExpandedFootprintScrubFlag(
    stop: FootprintScrubStop,
    stopIndex: Int,
    totalStops: Int
) {
    val gold = DecoGold
    val verm = DecoVerm
    val cream = DecoPaperCream
    val mat = DecoMat
    val ink = DecoInkOnGround

    Box(
        modifier = Modifier
            .widthIn(min = 148.dp, max = 240.dp)
            .clip(FootprintFlagExpandedShape)
            .background(mat)
            .border(1.5.dp, gold, FootprintFlagExpandedShape)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("footprint_scrub_preview_banner")
    ) {
        AnimatedContent(
            targetState = stopIndex to stop,
            transitionSpec = {
                val movingDown = targetState.first >= initialState.first
                if (movingDown) {
                    (slideInVertically(tween(145)) { it / 2 } + fadeIn(tween(120))) togetherWith
                        (slideOutVertically(tween(110)) { -it / 2 } + fadeOut(tween(90)))
                } else {
                    (slideInVertically(tween(145)) { -it / 2 } + fadeIn(tween(120))) togetherWith
                        (slideOutVertically(tween(110)) { it / 2 } + fadeOut(tween(90)))
                }
            },
            contentKey = { it.second.id },
            label = "footprint_flag_stop_roll"
        ) { (idx, activeStop) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // 位置方章：纸地金描边小章（3dp 圆角）
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(gold.copy(alpha = 0.14f))
                        .border(1.dp, gold.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                ) {
                    DecoIcon(
                        kind = if (activeStop.isUnlocated) DecoIconKind.LOCATION_OFF else DecoIconKind.LOCATION,
                        color = if (activeStop.isUnlocated) verm else gold,
                        iconSize = 16.dp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activeStop.displayName,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            letterSpacing = 1.2.sp,
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 132.dp)
                        )
                        Text(
                            text = "${idx + 1}/$totalStops",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = gold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${activeStop.mediaCount} 项 · ${formatStorageSize(activeStop.totalBytes)}",
                            fontFamily = DecoDisplayFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = ink.copy(alpha = 0.62f),
                            maxLines = 1
                        )
                        if (activeStop.isCleaned) {
                            // 已整理 = 金底徽（内衬字）
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(gold)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                DecoIcon(
                                    kind = DecoIconKind.CHECK_CIRCLE,
                                    color = mat,
                                    iconSize = 11.dp
                                )
                                Text(
                                    text = "已整理",
                                    fontSize = 12.sp,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = mat
                                )
                            }
                        } else if (activeStop.unreviewedCount > 0) {
                            // 朱红计数徽（朱底 + 纸黄字 + 纸黄受光边）
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(verm)
                                    .border(0.75.dp, cream.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${activeStop.unreviewedCount} 待清",
                                    fontFamily = DecoDisplayFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = cream
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
