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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// =====================================================================
// 摩登画报造型令牌（design-demos/deco-app.html：3~4dp 画片圆角、金双线画框、
// 朱红小签、金边跳签）。色彩一律走 ui/theme Deco 令牌（日光画廊/暗夜暗房双套），
// 仅朱红小签的纸黄受光边为浅暗同值不变量（纸黄只压照片暗部 / 朱红底 —— 可读性红线）。
// =====================================================================
private val StampOverlayShape = RoundedCornerShape(3.dp)
private val JumpChipShape = RoundedCornerShape(3.dp)
private val ScrollFlagShape = RoundedCornerShape(3.dp)

// 朱红小签的纸黄受光边（原型朱红徽语法 border 1px rgba(239,227,200,0.25)）
private val VermTagCreamRim = Color(0x40EFE3C8)
private val VermTagCreamRimActive = Color(0x70EFE3C8)

// 干支纪年（通式推算，同 BurstGalleryGrid 画报月头语法：2026 丙午 / 2025 乙巳 …）
private val StampGanZhiStems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val StampGanZhiBranches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

private fun stampGanZhi(year: Int): String =
    StampGanZhiStems[(year - 4).mod(10)] + StampGanZhiBranches[(year - 4).mod(12)]

private fun stampChineseMonth(month: Int): String = when (month) {
    1 -> "一月"
    2 -> "二月"
    3 -> "三月"
    4 -> "四月"
    5 -> "五月"
    6 -> "六月"
    7 -> "七月"
    8 -> "八月"
    9 -> "九月"
    10 -> "十月"
    11 -> "十一月"
    else -> "十二月"
}

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
 * 摩登画报 · 月头画报栏目线（deco-app.html 画廊屏月头语法，与 BurstGalleryGrid.DecoMonthClusterHeader
 * 同构）：衬线月名 20sp + 金色干支纪年副标 12sp + 弱金通栏细线 + 计数与金边动作跳签。
 * 供瀑布流模式（GalleryWaterfallLayout）使用；选本月 / 整理·重温 / 已整理标记与 testTag 原样保留。
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
    val deco = LocalFlipYearPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
            .testTag("timeline_cluster_header_${cluster.yearMonthKey}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 画报月名 + 干支纪年副标
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stampChineseMonth(cluster.month),
                fontFamily = DecoDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.6.sp,
                color = deco.decoInkOnGround
            )
            Text(
                text = "${stampGanZhi(cluster.year)}年",
                fontSize = 12.sp,
                fontFamily = DecoDisplayFontFamily,
                letterSpacing = 1.2.sp,
                color = deco.decoGold
            )
        }

        // 栏目细线（弱金发丝）
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(deco.decoGold.copy(alpha = 0.45f))
        )

        // 计数 + 金边动作跳签（选本月 / 整理·重温 / 已整理金章）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = cluster.compactSummaryText,
                fontSize = 12.sp,
                fontFamily = DecoDisplayFontFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = deco.decoInkOnGround.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelectionMode && onToggleSelectMonth != null) {
                Surface(
                    onClick = onToggleSelectMonth,
                    shape = JumpChipShape,
                    color = if (isMonthAllSelected) deco.decoGold else Color.Transparent,
                    border = BorderStroke(1.dp, deco.decoGold.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("timeline_select_month_${cluster.yearMonthKey}")
                ) {
                    Text(
                        text = if (isMonthAllSelected) "已选本月" else "选本月",
                        fontSize = 12.sp,
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = if (isMonthAllSelected) deco.decoMat else deco.decoGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (!isSelectionMode && onCleanMonth != null) {
                Surface(
                    onClick = onCleanMonth,
                    shape = JumpChipShape,
                    color = if (cluster.isCleaned) deco.decoGold else Color.Transparent,
                    border = BorderStroke(1.dp, deco.decoGold.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("timeline_clean_month_${cluster.yearMonthKey}")
                ) {
                    Text(
                        text = if (cluster.isCleaned) "重温" else "整理",
                        fontSize = 12.sp,
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = if (cluster.isCleaned) deco.decoMat else deco.decoGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (cluster.isCleaned) {
                DecoIcon(
                    kind = DecoIconKind.CHECK_CIRCLE,
                    color = deco.decoGold,
                    iconSize = 13.dp,
                    contentDescription = stringResource(R.string.timeline_month_cleaned_tag)
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
 * 摩登画报重绘：浮动月戳 = DecoMat 纸面章 + 金双线画框（1.5dp 金描边 + 3dp 内衬弱金发丝线），
 * 左侧朱红小方章日历徽（纸黄字），衬线月名 + 金干支纪年副标；滚动快速签 = 朱红小签（纸黄字）。
 *
 * Performance Architecture:
 * - Reads [LazyGridState.firstVisibleItemIndex] and [LazyGridState.isScrollInProgress] strictly
 *   inside `derivedStateOf` in a sibling overlay layer — zero recomposition leaks into grid cells.
 * - Uses O(log M) binary search on pre-computed [TimelineClusterLayout].
 * - Draws the gold double-line frame in a single GPU-cheap `drawBehind` pass.
 */
@Composable
fun TimelineMonthStampOverlay(
    gridState: LazyGridState,
    clusterLayout: TimelineClusterLayout,
    modifier: Modifier = Modifier
) {
    TimelineMonthStampOverlayCore(
        firstVisibleIndexProvider = { gridState.firstVisibleItemIndex },
        isScrollingProvider = { gridState.isScrollInProgress },
        onScrollToIndex = { targetIndex -> gridState.scrollToItem(targetIndex) },
        clusterLayout = clusterLayout,
        modifier = modifier
    )
}

@Composable
fun TimelineMonthStampOverlay(
    listState: LazyListState,
    clusterLayout: TimelineClusterLayout,
    modifier: Modifier = Modifier
) {
    TimelineMonthStampOverlayCore(
        firstVisibleIndexProvider = { listState.firstVisibleItemIndex },
        isScrollingProvider = { listState.isScrollInProgress },
        onScrollToIndex = { targetIndex -> listState.scrollToItem(targetIndex) },
        clusterLayout = clusterLayout,
        modifier = modifier
    )
}

@Composable
private fun TimelineMonthStampOverlayCore(
    firstVisibleIndexProvider: () -> Int,
    isScrollingProvider: () -> Boolean,
    onScrollToIndex: suspend (Int) -> Unit,
    clusterLayout: TimelineClusterLayout,
    modifier: Modifier = Modifier
) {
    if (clusterLayout.clusters.isEmpty()) return

    val gold = DecoGold
    val verm = DecoVerm
    val cream = DecoPaperCream
    val mat = DecoMat
    val ink = DecoInkOnGround

    val activeCluster by remember(clusterLayout) {
        derivedStateOf {
            clusterLayout.clusterForVisibleIndex(firstVisibleIndexProvider())
        }
    }
    val isScrolling by remember {
        derivedStateOf { isScrollingProvider() }
    }
    val scrollProgressFraction by remember(clusterLayout) {
        derivedStateOf {
            val maxIndex = (clusterLayout.gridItems.size - 1).coerceAtLeast(1)
            (firstVisibleIndexProvider().toFloat() / maxIndex.toFloat()).coerceIn(0f, 1f)
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
    val stampAccessibilityDesc = "${currentCluster.stampLabel}，${currentCluster.compactSummaryText}"

    Box(modifier = modifier.fillMaxSize()) {
        // Top-Center Floating Deco Monthly Summary Stamp + Tap-to-Jump Month Ribbon
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
                                color = DecoScrimInk.copy(alpha = 0.26f),
                                topLeft = Offset(1.dp.toPx(), 3.dp.toPx()),
                                size = Size(size.width - 2.dp.toPx(), size.height),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                        .clip(StampOverlayShape)
                        .background(mat)
                        .drawBehind {
                            // 金双线画框第二线：3dp 内衬处的 1dp 弱金发丝环
                            val inset = 3.dp.toPx()
                            drawRoundRect(
                                color = gold.copy(alpha = 0.40f),
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        .border(width = 1.5.dp, color = gold, shape = StampOverlayShape)
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
                            // 朱红小方章日历徽（纸黄只压朱红底 —— 可读性红线允许组合）
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(verm)
                                    .border(1.dp, VermTagCreamRim, RoundedCornerShape(3.dp))
                            ) {
                                DecoIcon(
                                    kind = DecoIconKind.CALENDAR,
                                    color = cream,
                                    iconSize = 15.dp
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = cluster.stampLabel,
                                        fontFamily = DecoDisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        letterSpacing = 0.5.sp,
                                        color = ink,
                                        modifier = Modifier.testTag("timeline_month_stamp_label")
                                    )
                                    Text(
                                        text = "${stampGanZhi(cluster.year)}年",
                                        fontSize = 12.sp,
                                        fontFamily = DecoDisplayFontFamily,
                                        letterSpacing = 1.2.sp,
                                        color = gold
                                    )
                                    if (cluster.isCleaned) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier
                                                .clip(JumpChipShape)
                                                .background(gold.copy(alpha = 0.16f))
                                                .border(1.dp, gold.copy(alpha = 0.45f), JumpChipShape)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            DecoIcon(
                                                kind = DecoIconKind.CHECK_CIRCLE,
                                                color = gold,
                                                iconSize = 12.dp
                                            )
                                            Text(
                                                text = stringResource(R.string.timeline_month_cleaned_tag),
                                                fontSize = 12.sp,
                                                fontFamily = DecoDisplayFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp,
                                                color = gold
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
                                            iconKind = DecoIconKind.PHOTO_LIBRARY,
                                            text = "${cluster.photoCount}"
                                        )
                                    }
                                    if (cluster.videoCount > 0) {
                                        StampMetricTag(
                                            iconKind = DecoIconKind.VIDEOCAM,
                                            text = "${cluster.videoCount}"
                                        )
                                    }
                                    if (cluster.burstGroupCount > 0) {
                                        StampMetricTag(
                                            iconKind = DecoIconKind.BURST,
                                            text = "${cluster.burstGroupCount}组"
                                        )
                                    }
                                    Text(
                                        text = formatStorageSize(cluster.totalBytes),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                        color = ink.copy(alpha = 0.66f)
                                    )
                                }
                            }

                        }
                    }
                }
            }

        }

        // Persistent Right-Edge Fast-Scrub Rail — always mounted so TalkBack can discover
        // and control timeline jumping even when the list is idle; expands into the vermilion
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

                // 轨道：压照片暗部的 scrim 墨细线（可读性红线允许组合）
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 3.dp)
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(DecoScrimInk.copy(alpha = 0.32f))
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
                                scope.launch { onScrollToIndex(targetIndex) }
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
                                scope.launch { onScrollToIndex(targetIndex) }
                            }
                        }
                        .testTag("timeline_scroll_rail_flag")
                ) {
                    if (railActive) {
                        // 滚动月签：朱红小签（纸黄字 + 纸黄受光边），拖拽时受光边更亮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .clip(ScrollFlagShape)
                                .background(verm)
                                .border(
                                    0.9.dp,
                                    if (isDraggingFlag) VermTagCreamRimActive else VermTagCreamRim,
                                    ScrollFlagShape
                                )
                                .padding(start = 9.dp, end = 7.dp)
                        ) {
                            Text(
                                text = currentCluster.stampLabel,
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.4.sp,
                                color = cream
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (currentCluster.isCleaned) cream else gold)
                            )
                        }
                    } else {
                        // Subtle resting thumb so users & TalkBack always have a visible anchor
                        Box(
                            modifier = Modifier
                                .padding(end = 1.dp)
                                .width(10.dp)
                                .height(34.dp)
                                .clip(ScrollFlagShape)
                                .background(verm.copy(alpha = 0.9f))
                                .border(1.dp, VermTagCreamRim, ScrollFlagShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StampMetricTag(
    iconKind: DecoIconKind,
    text: String,
    tintOverride: Color? = null
) {
    val gold = DecoGold
    val ink = DecoInkOnGround
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DecoIcon(
            kind = iconKind,
            color = tintOverride ?: gold,
            iconSize = 13.dp
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = DecoDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = ink.copy(alpha = 0.66f)
        )
    }
}
