package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MonthStampEntity
import com.example.data.PhotoRepository
import com.example.ui.GalleryEntry
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfacePure
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val StampOverlayShape = RoundedCornerShape(18.dp)
private val StampInlineShape = RoundedCornerShape(12.dp)
private val JumpChipShape = RoundedCornerShape(50)
private val ScrollFlagShape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 6.dp, bottomEnd = 6.dp)

private val StampActiveFillBrush = Brush.verticalGradient(
    colors = listOf(Color(0xEB151822), Color(0xE00E1017))
)
private val StampDashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
private val StampInnerDarkRim = Color(0x38FFFFFF)
private val StampInnerLightRim = Color(0x522548D6)
private val StampShadowHalo = Color(0x26121316)
private val ScrollRailTrackBg = Color(0x2E121316)

/**
 * Aggregated monthly cluster metadata used both for inline section stamps and the
 * scroll-responsive floating timeline overlay (e.g., "Jan 2026").
 */
@Immutable
data class MonthlyClusterSummary(
    val yearMonthKey: String,
    val year: Int,
    val month: Int,
    val stampLabel: String,
    val photoCount: Int,
    val videoCount: Int,
    val burstGroupCount: Int,
    val totalBytes: Long,
    val firstGridIndex: Int,
    val isCleaned: Boolean
) {
    val totalMediaCount: Int
        get() = photoCount + videoCount

    val compactSummaryText: String
        get() = buildString {
            append("${totalMediaCount} 项")
            if (burstGroupCount > 0) {
                append(" · ${burstGroupCount} 连拍")
            }
            if (videoCount > 0 && photoCount > 0) {
                append(" · ${videoCount} 视频")
            }
            append(" · ${formatStorageSize(totalBytes)}")
        }
}

/**
 * Unified grid item model for chronological timelines displaying monthly cluster dividers
 * interspersed with photo & burst entries.
 */
@Immutable
sealed interface TimelineGridItem {
    val stableKey: String

    @Immutable
    data class MonthDivider(val cluster: MonthlyClusterSummary) : TimelineGridItem {
        override val stableKey: String = "month_divider_${cluster.yearMonthKey}_${cluster.firstGridIndex}"
    }

    @Immutable
    data class MediaEntry(val entry: GalleryEntry) : TimelineGridItem {
        override val stableKey: String = entry.stableKey
    }
}

/**
 * Pre-computed O(N) timeline cluster index supporting O(log M) binary search from any
 * visible [LazyGridState.firstVisibleItemIndex] to its owning [MonthlyClusterSummary].
 */
@Immutable
data class TimelineClusterLayout(
    val gridItems: List<TimelineGridItem>,
    val clusters: List<MonthlyClusterSummary>
) {
    fun clusterForVisibleIndex(firstVisibleIndex: Int): MonthlyClusterSummary? {
        if (clusters.isEmpty()) return null
        if (firstVisibleIndex <= clusters.first().firstGridIndex) return clusters.first()
        var low = 0
        var high = clusters.lastIndex
        var result = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (clusters[mid].firstGridIndex <= firstVisibleIndex) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return clusters[result]
    }
}

/**
 * Groups chronologically ordered [GalleryEntry] items into monthly clusters in a single linear pass.
 */
fun buildTimelineClusterLayout(
    entries: List<GalleryEntry>,
    monthStamps: Map<String, MonthStampEntity> = emptyMap()
): TimelineClusterLayout {
    if (entries.isEmpty()) {
        return TimelineClusterLayout(emptyList(), emptyList())
    }

    val gridItems = ArrayList<TimelineGridItem>(entries.size + 16)
    val clusters = ArrayList<MonthlyClusterSummary>(16)

    var index = 0
    while (index < entries.size) {
        val anchorPhoto = entries[index].primaryPhoto
        val year = anchorPhoto.year
        val month = anchorPhoto.month
        val ymKey = PhotoRepository.yearMonthKey(year, month)

        var photoCount = 0
        var videoCount = 0
        var burstGroupCount = 0
        var totalBytes = 0L

        val startEntryIndex = index
        while (index < entries.size) {
            val p = entries[index].primaryPhoto
            if (p.year != year || p.month != month) break
            when (val e = entries[index]) {
                is GalleryEntry.Single -> {
                    if (e.photo.isVideo) videoCount++ else photoCount++
                    totalBytes += e.photo.fileSizeBytes
                }
                is GalleryEntry.Burst -> {
                    burstGroupCount++
                    for (member in e.photos) {
                        if (member.isVideo) videoCount++ else photoCount++
                        totalBytes += member.fileSizeBytes
                    }
                }
            }
            index++
        }

        val firstGridIndex = gridItems.size
        val summary = MonthlyClusterSummary(
            yearMonthKey = ymKey,
            year = year,
            month = month,
            stampLabel = formatMonthSummaryStamp(year, month),
            photoCount = photoCount,
            videoCount = videoCount,
            burstGroupCount = burstGroupCount,
            totalBytes = totalBytes,
            firstGridIndex = firstGridIndex,
            isCleaned = monthStamps.containsKey(ymKey)
        )
        clusters.add(summary)
        gridItems.add(TimelineGridItem.MonthDivider(summary))

        for (i in startEntryIndex until index) {
            gridItems.add(TimelineGridItem.MediaEntry(entries[i]))
        }
    }

    return TimelineClusterLayout(gridItems = gridItems, clusters = clusters)
}

/**
 * Lightweight inline archival divider stamp placed at the start of every monthly photo cluster
 * in the timeline grid. Uses zero blur and zero hardware shadows to stay 60/120fps locked.
 */
@Composable
fun MonthlyClusterHeaderStamp(
    cluster: MonthlyClusterSummary,
    isSelectionMode: Boolean = false,
    isMonthAllSelected: Boolean = false,
    onToggleSelectMonth: (() -> Unit)? = null,
    onCleanMonth: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val stampInnerRim = if (isDark) Color(0x666B8FFF) else StampInnerLightRim

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
            .testTag("timeline_cluster_header_${cluster.yearMonthKey}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Editorial archival postage-style month badge ("2026年1月")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(StampInlineShape)
                .background(SurfacePure)
                .drawBehind {
                    val inset = 2.5.dp.toPx()
                    drawRoundRect(
                        color = stampInnerRim,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2f, size.height - inset * 2f),
                        cornerRadius = CornerRadius(9.5.dp.toPx(), 9.5.dp.toPx()),
                        style = Stroke(width = 0.85.dp.toPx(), pathEffect = StampDashedEffect)
                    )
                }
                .border(1.dp, HairlineBorder, StampInlineShape)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (cluster.isCleaned) EmeraldKeep else CobaltPrimary)
            )
            Text(
                text = cluster.stampLabel,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                letterSpacing = 0.4.sp,
                color = InkPrimary
            )
        }

        // Hairline editorial separator rule
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(HairlineBorder)
        )

        // Compact cluster metrics + optional cleaned seal / contextual action pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = cluster.compactSummaryText,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelectionMode && onToggleSelectMonth != null) {
                androidx.compose.material3.Surface(
                    onClick = onToggleSelectMonth,
                    shape = JumpChipShape,
                    color = if (isMonthAllSelected) CobaltPrimary else CobaltSoft,
                    modifier = Modifier.testTag("timeline_select_month_${cluster.yearMonthKey}")
                ) {
                    Text(
                        text = if (isMonthAllSelected) "已选本月" else "选本月",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMonthAllSelected) Color.White else CobaltDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (!isSelectionMode && onCleanMonth != null) {
                androidx.compose.material3.Surface(
                    onClick = onCleanMonth,
                    shape = JumpChipShape,
                    color = if (cluster.isCleaned) EmeraldSoft else CobaltSoft,
                    modifier = Modifier.testTag("timeline_clean_month_${cluster.yearMonthKey}")
                ) {
                    Text(
                        text = if (cluster.isCleaned) "重温" else "整理",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cluster.isCleaned) EmeraldKeep else CobaltDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (cluster.isCleaned) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.timeline_month_cleaned_tag),
                    tint = EmeraldKeep,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Scroll-responsive visual overlay component that floats above the photo timeline and stamps
 * the active monthly photo cluster (e.g., "Jan 2026" + photo/burst/storage summary) as the
 * user scrolls.
 *
 * Performance Architecture:
 * - Reads [LazyGridState.firstVisibleItemIndex] and [LazyGridState.isScrollInProgress] strictly
 *   inside `derivedStateOf` in a sibling overlay layer — zero recomposition leaks into grid cells.
 * - Uses O(log M) binary search on pre-computed [TimelineClusterLayout].
 * - Draws archival collector-stamp double borders in a single GPU-cheap `drawBehind` pass.
 */
@Composable
fun TimelineMonthStampOverlay(
    gridState: LazyGridState,
    clusterLayout: TimelineClusterLayout,
    modifier: Modifier = Modifier
) {
    if (clusterLayout.clusters.isEmpty()) return

    val activeCluster by remember(gridState, clusterLayout) {
        derivedStateOf {
            clusterLayout.clusterForVisibleIndex(gridState.firstVisibleItemIndex)
        }
    }
    val isScrolling by remember(gridState) {
        derivedStateOf { gridState.isScrollInProgress }
    }
    val scrollProgressFraction by remember(gridState, clusterLayout) {
        derivedStateOf {
            val maxIndex = (clusterLayout.gridItems.size - 1).coerceAtLeast(1)
            (gridState.firstVisibleItemIndex.toFloat() / maxIndex.toFloat()).coerceIn(0f, 1f)
        }
    }

    var lingerVisible by remember { mutableStateOf(false) }

    // Fast-scrub state: the rail flag doubles as a draggable scrollbar thumb.
    // While dragging, dragFraction drives both the flag position and the grid scroll.
    var isDraggingFlag by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Keep the elevated active scroll stamp visible briefly after a fling or a
    // flag scrub settles — re-armed on drag end so the overlay lingers ~2s.
    LaunchedEffect(isScrolling, isDraggingFlag) {
        if (isScrolling || isDraggingFlag) {
            lingerVisible = true
        } else {
            delay(2200L)
            lingerVisible = false
        }
    }

    val currentCluster = activeCluster ?: return
    // Stamp visibility follows the scroll-rail flag: while scrolling/dragging +
    // a short post-fling linger — never parked at top.
    val showFloatingStamp = isScrolling || lingerVisible || isDraggingFlag
    val isElevatedDarkMode = showFloatingStamp
    val stampAccessibilityDesc = "${currentCluster.stampLabel}，${currentCluster.compactSummaryText}"

    Box(modifier = modifier.fillMaxSize()) {
        // Top-Center Floating Archival Monthly Summary Stamp + Tap-to-Jump Month Ribbon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            AnimatedVisibility(
                visible = showFloatingStamp,
                enter = fadeIn(tween(170)) +
                    slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(170)),
                exit = fadeOut(tween(220)) +
                    slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(200)) +
                    scaleOut(targetScale = 0.95f, animationSpec = tween(200))
            ) {
                val stampScale by animateFloatAsState(
                    targetValue = if (isScrolling) 1.03f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "timeline_stamp_scale"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = stampScale
                            scaleY = stampScale
                        }
                        .widthIn(max = 356.dp)
                        .heightIn(min = 48.dp)
                        .drawBehind {
                            // Subtle soft grounded drop halo without RenderNode elevation shadow cost
                            drawRoundRect(
                                color = StampShadowHalo,
                                topLeft = Offset(1.dp.toPx(), 3.dp.toPx()),
                                size = Size(size.width - 2.dp.toPx(), size.height),
                                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                            )
                        }
                        .clip(StampOverlayShape)
                        .background(StampActiveFillBrush)
                        .drawBehind {
                            // Archival perforated collector stamp inner dashed frame + corner ticks
                            val inset = 3.5.dp.toPx()
                            drawRoundRect(
                                color = StampInnerDarkRim,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                                cornerRadius = CornerRadius(14.5.dp.toPx(), 14.5.dp.toPx()),
                                style = Stroke(width = 0.9.dp.toPx(), pathEffect = StampDashedEffect)
                            )
                        }
                        .border(
                            width = 1.dp,
                            color = if (isElevatedDarkMode) Color(0x47FFFFFF) else HairlineBorder,
                            shape = StampOverlayShape
                        )
                        .semantics { contentDescription = stampAccessibilityDesc }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("timeline_month_stamp_overlay")
                ) {
                    AnimatedContent(
                        targetState = currentCluster,
                        transitionSpec = {
                            val movingOlder = (targetState.year * 12 + targetState.month) <
                                (initialState.year * 12 + initialState.month)
                            if (movingOlder) {
                                (slideInVertically(tween(180)) { it / 2 } + fadeIn(tween(150))) togetherWith
                                    (slideOutVertically(tween(150)) { -it / 2 } + fadeOut(tween(120)))
                            } else {
                                (slideInVertically(tween(180)) { -it / 2 } + fadeIn(tween(150))) togetherWith
                                    (slideOutVertically(tween(150)) { it / 2 } + fadeOut(tween(120)))
                            }
                        },
                        contentKey = { it.yearMonthKey },
                        label = "timeline_month_stamp_roll"
                    ) { cluster ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Collector Stamp Emblem
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isElevatedDarkMode) CobaltPrimary.copy(alpha = 0.28f)
                                        else CobaltSoft
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isElevatedDarkMode) SolarAmber else CobaltPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = cluster.stampLabel,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.5.sp,
                                        color = if (isElevatedDarkMode) Color.White else InkPrimary,
                                        modifier = Modifier.testTag("timeline_month_stamp_label")
                                    )
                                    if (cluster.isCleaned) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier
                                                .clip(JumpChipShape)
                                                .background(
                                                    if (isElevatedDarkMode) EmeraldKeep.copy(alpha = 0.24f)
                                                    else EmeraldSoft
                                                )
                                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isElevatedDarkMode) Color(0xFF5EEAD4) else EmeraldKeep,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.timeline_month_cleaned_tag),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isElevatedDarkMode) Color(0xFF5EEAD4) else EmeraldKeep
                                            )
                                        }
                                    }
                                }

                                // Monthly cluster summary telemetry row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (cluster.photoCount > 0) {
                                        StampMetricTag(
                                            icon = Icons.Filled.PhotoLibrary,
                                            text = "${cluster.photoCount}",
                                            darkMode = isElevatedDarkMode
                                        )
                                    }
                                    if (cluster.videoCount > 0) {
                                        StampMetricTag(
                                            icon = Icons.Filled.Videocam,
                                            text = "${cluster.videoCount}",
                                            darkMode = isElevatedDarkMode
                                        )
                                    }
                                    if (cluster.burstGroupCount > 0) {
                                        StampMetricTag(
                                            icon = Icons.Filled.BurstMode,
                                            text = "${cluster.burstGroupCount}组",
                                            darkMode = isElevatedDarkMode,
                                            tintOverride = SolarAmber
                                        )
                                    }
                                    Text(
                                        text = formatStorageSize(cluster.totalBytes),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isElevatedDarkMode) Color(0xCCFFFFFF) else InkSecondary
                                    )
                                }
                            }

                        }
                    }
                }
            }

        }

        // Persistent Right-Edge Fast-Scrub Rail — always mounted so TalkBack can discover
        // and control timeline jumping even when the list is idle; expands into the dark
        // month flag while scrolling or dragging.
        if (clusterLayout.clusters.size >= 2) {
            val railActive = isScrolling || lingerVisible || isDraggingFlag
            val railAlpha by animateFloatAsState(
                targetValue = if (railActive) 1f else 0.45f,
                animationSpec = tween(220),
                label = "timeline_rail_idle_alpha"
            )

            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 74.dp, bottom = 128.dp, end = 4.dp)
                    .graphicsLayer { alpha = railAlpha },
                contentAlignment = Alignment.TopEnd
            ) {
                val trackHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val travelPx = (trackHeightPx - 120f).coerceAtLeast(1f)
                val displayFraction = if (isDraggingFlag) dragFraction else scrollProgressFraction
                val indicatorOffsetY = (displayFraction * travelPx).roundToInt()

                val scrollRailTrackColor = if (LocalIsDarkTheme.current) Color(0x47FFFFFF) else ScrollRailTrackBg
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 3.dp)
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(scrollRailTrackColor)
                )

                // Always-discoverable 48dp-high touch & TalkBack target
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = indicatorOffsetY) }
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "时间轴快速滚动条，当前${currentCluster.stampLabel}"
                            progressBarRangeInfo = ProgressBarRangeInfo(displayFraction, 0f..1f)
                            setProgress { target ->
                                dragFraction = target.coerceIn(0f, 1f)
                                val targetIndex =
                                    (dragFraction * (clusterLayout.gridItems.size - 1))
                                        .roundToInt()
                                        .coerceAtLeast(0)
                                scope.launch { gridState.scrollToItem(targetIndex) }
                                true
                            }
                        }
                        .pointerInput(travelPx, clusterLayout) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDraggingFlag = true
                                    dragFraction = scrollProgressFraction
                                },
                                onDragEnd = { isDraggingFlag = false },
                                onDragCancel = { isDraggingFlag = false }
                            ) { change, dragAmount ->
                                change.consume()
                                dragFraction =
                                    (dragFraction + dragAmount / travelPx).coerceIn(0f, 1f)
                                val targetIndex =
                                    (dragFraction * (clusterLayout.gridItems.size - 1))
                                        .roundToInt()
                                        .coerceAtLeast(0)
                                scope.launch { gridState.scrollToItem(targetIndex) }
                            }
                        }
                        .testTag("timeline_scroll_rail_flag")
                ) {
                    if (railActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .clip(ScrollFlagShape)
                                .background(if (isDraggingFlag) Color(0xF21A1E29) else Color(0xDE12141C))
                                .border(0.9.dp, if (isDraggingFlag) Color(0x7AFFFFFF) else Color(0x45FFFFFF), ScrollFlagShape)
                                .padding(start = 9.dp, end = 7.dp)
                        ) {
                            Text(
                                text = currentCluster.stampLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (currentCluster.isCleaned) EmeraldKeep else SolarAmber)
                            )
                        }
                    } else {
                        // Subtle resting pill thumb so users & TalkBack always have a visible anchor
                        Box(
                            modifier = Modifier
                                .padding(end = 1.dp)
                                .width(10.dp)
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(CobaltPrimary.copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StampMetricTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    darkMode: Boolean,
    tintOverride: Color? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintOverride ?: if (darkMode) Color(0xB8FFFFFF) else InkTertiary,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (darkMode) Color(0xDBFFFFFF) else InkSecondary
        )
    }
}
