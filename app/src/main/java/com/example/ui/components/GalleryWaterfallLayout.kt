package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.MonthStampEntity
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.PhotoRepository
import com.example.ui.GalleryEntry
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.ObsidianOverlayBorder
import com.example.ui.theme.ObsidianOverlayGlass
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Progressive pinch-to-zoom density spectrum shared across all Gallery views
 * (Main Gallery & Flipbook Month Gallery):
 * 4-col compact grid <-> 3-col standard grid <-> justified natural-ratio waterfall <-> immersive 1-col stream.
 */
enum class GalleryZoomTier(
    val label: String,
    val shortChipLabel: String,
    val isWaterfall: Boolean
) {
    COMPACT_4(label = "4列紧凑网格", shortChipLabel = "4列", isWaterfall = false),
    STANDARD_3(label = "3列标准网格", shortChipLabel = "3列", isWaterfall = false),
    WATERFALL_2(label = "大图瀑布流", shortChipLabel = "瀑布流", isWaterfall = true),
    WATERFALL_1(label = "沉浸大图流", shortChipLabel = "大图", isWaterfall = true);

    fun zoomIn(): GalleryZoomTier = when (this) {
        COMPACT_4 -> STANDARD_3
        STANDARD_3 -> WATERFALL_2
        WATERFALL_2 -> WATERFALL_1
        WATERFALL_1 -> WATERFALL_1
    }

    fun zoomOut(): GalleryZoomTier = when (this) {
        WATERFALL_1 -> WATERFALL_2
        WATERFALL_2 -> STANDARD_3
        STANDARD_3 -> COMPACT_4
        COMPACT_4 -> COMPACT_4
    }

    fun gridColumns(): Int = when (this) {
        COMPACT_4 -> 4
        else -> 3
    }

    fun nextByTap(preferredColumns: Int): GalleryZoomTier = when (this) {
        COMPACT_4, STANDARD_3 -> WATERFALL_2
        WATERFALL_2 -> WATERFALL_1
        WATERFALL_1 -> defaultGalleryZoomTier(preferredColumns)
    }
}

fun defaultGalleryZoomTier(preferredColumns: Int): GalleryZoomTier =
    if (preferredColumns >= 4) GalleryZoomTier.COMPACT_4 else GalleryZoomTier.STANDARD_3

@Immutable
data class WaterfallCell(
    val entry: GalleryEntry,
    val aspectRatio: Float
)

@Immutable
sealed interface WaterfallRowItem {
    val stableKey: String

    @Immutable
    data class MonthHeader(
        val cluster: MonthlyClusterSummary
    ) : WaterfallRowItem {
        override val stableKey: String = "wf_month_${cluster.yearMonthKey}_${cluster.firstGridIndex}"
    }

    @Immutable
    data class DayHeader(
        val year: Int,
        val month: Int,
        val day: Int,
        val itemCount: Int,
        override val stableKey: String
    ) : WaterfallRowItem

    @Immutable
    data class JustifiedPhotos(
        val cells: List<WaterfallCell>,
        override val stableKey: String
    ) : WaterfallRowItem

    @Immutable
    data class ExpandedBurst(
        val burst: GalleryEntry.Burst,
        override val stableKey: String = "wf_burst_exp_${burst.groupId}"
    ) : WaterfallRowItem
}

@Immutable
data class WaterfallLayoutResult(
    val rows: List<WaterfallRowItem>,
    val clusterLayout: TimelineClusterLayout
)

internal fun PhotoItemEntity.safeWaterfallAspectRatio(): Float {
    if (widthPx > 0 && heightPx > 0) {
        return (widthPx.toFloat() / heightPx.toFloat()).coerceIn(0.48f, 2.20f)
    }
    return 0.88f
}

/**
 * Single linear O(N) justified-row waterfall packer preserving natural photo aspect ratios.
 * - Portrait + landscape shots pack side-by-side at equal row height without destructive cropping.
 * - Wide landscapes and leftover tall screenshots span full width for direct legibility.
 * - In [GalleryZoomTier.WATERFALL_1], every item spans full width at its natural aspect ratio.
 */
fun buildWaterfallLayout(
    entries: List<GalleryEntry>,
    expandedBurstIds: Set<String>,
    monthStamps: Map<String, MonthStampEntity>,
    showMonthHeaders: Boolean,
    singleColumnImmersive: Boolean
): WaterfallLayoutResult {
    if (entries.isEmpty()) {
        return WaterfallLayoutResult(emptyList(), TimelineClusterLayout(emptyList(), emptyList()))
    }

    val rows = ArrayList<WaterfallRowItem>(entries.size)
    val clusters = ArrayList<MonthlyClusterSummary>(16)

    var index = 0
    while (index < entries.size) {
        val anchor = entries[index].primaryPhoto
        val year = anchor.year
        val month = anchor.month
        val ymKey = PhotoRepository.yearMonthKey(year, month)

        var photoCount = 0
        var videoCount = 0
        var burstGroupCount = 0
        var totalBytes = 0L

        val monthStartIdx = index
        while (index < entries.size) {
            val p = entries[index].primaryPhoto
            if (showMonthHeaders && (p.year != year || p.month != month)) break
            when (val e = entries[index]) {
                is GalleryEntry.Single -> {
                    if (e.photo.isVideo) videoCount++ else photoCount++
                    totalBytes += e.photo.fileSizeBytes
                }
                is GalleryEntry.Burst -> {
                    burstGroupCount++
                    for (m in e.photos) {
                        if (m.isVideo) videoCount++ else photoCount++
                        totalBytes += m.fileSizeBytes
                    }
                }
            }
            index++
        }

        val firstRowIndex = rows.size
        if (showMonthHeaders) {
            val summary = MonthlyClusterSummary(
                yearMonthKey = ymKey,
                year = year,
                month = month,
                stampLabel = formatMonthSummaryStamp(year, month),
                photoCount = photoCount,
                videoCount = videoCount,
                burstGroupCount = burstGroupCount,
                totalBytes = totalBytes,
                firstGridIndex = firstRowIndex,
                isCleaned = monthStamps.containsKey(ymKey)
            )
            clusters.add(summary)
            rows.add(WaterfallRowItem.MonthHeader(summary))
        }

        // Sub-group inside the month by capture day for crisp "11月16日" editorial dividers
        var dayCursor = monthStartIdx
        while (dayCursor < index) {
            val dayAnchor = entries[dayCursor].primaryPhoto
            val dYear = dayAnchor.year
            val dMonth = dayAnchor.month
            val dDay = dayAnchor.day

            val dayStart = dayCursor
            var dayItemCount = 0
            while (dayCursor < index) {
                val p = entries[dayCursor].primaryPhoto
                if (p.year != dYear || p.month != dMonth || p.day != dDay) break
                dayItemCount += when (val e = entries[dayCursor]) {
                    is GalleryEntry.Single -> 1
                    is GalleryEntry.Burst -> e.photos.size
                }
                dayCursor++
            }

            rows.add(
                WaterfallRowItem.DayHeader(
                    year = dYear,
                    month = dMonth,
                    day = dDay,
                    itemCount = dayItemCount,
                    stableKey = "wf_day_${dYear}_${dMonth}_${dDay}_$dayStart"
                )
            )

            // Pack entries of this day into justified aspect-ratio rows
            val pending = ArrayList<WaterfallCell>(3)
            var sumAr = 0f

            fun flushPending() {
                if (pending.isEmpty()) return
                val snapshot = pending.toList()
                val key = "wf_row_${snapshot.first().entry.stableKey}_${snapshot.size}"
                rows.add(WaterfallRowItem.JustifiedPhotos(cells = snapshot, stableKey = key))
                pending.clear()
                sumAr = 0f
            }

            for (i in dayStart until dayCursor) {
                val entry = entries[i]
                if (entry is GalleryEntry.Burst && entry.groupId in expandedBurstIds) {
                    flushPending()
                    rows.add(WaterfallRowItem.ExpandedBurst(burst = entry))
                    continue
                }

                val ar = when (entry) {
                    is GalleryEntry.Single -> entry.photo.safeWaterfallAspectRatio()
                    is GalleryEntry.Burst -> entry.bestPhoto.safeWaterfallAspectRatio().coerceIn(0.65f, 1.80f)
                }

                if (singleColumnImmersive) {
                    flushPending()
                    rows.add(
                        WaterfallRowItem.JustifiedPhotos(
                            cells = listOf(WaterfallCell(entry = entry, aspectRatio = ar)),
                            stableKey = "wf_single_${entry.stableKey}"
                        )
                    )
                } else {
                    // Wide landscape or standalone square-tall photo gets full width cleanly when pending already has items
                    if (pending.isNotEmpty() && (pending.size >= 3 || (pending.size == 2 && sumAr + ar > 2.45f))) {
                        flushPending()
                    }
                    pending.add(WaterfallCell(entry = entry, aspectRatio = ar))
                    sumAr += ar

                    val shouldFlush = when {
                        pending.size >= 3 -> true
                        pending.size == 2 && sumAr >= 1.32f -> true
                        pending.size == 1 && ar >= 1.52f -> true
                        else -> false
                    }
                    if (shouldFlush) {
                        flushPending()
                    }
                }
            }
            flushPending()
        }
    }

    // Build synthetic TimelineClusterLayout whose gridItems.size matches rows.size for fast-scrub math
    val pseudoItems = rows.mapIndexed { idx, row ->
        when (row) {
            is WaterfallRowItem.MonthHeader -> TimelineGridItem.MonthDivider(row.cluster)
            else -> TimelineGridItem.MediaEntry(
                entries[idx.coerceIn(0, entries.lastIndex)]
            )
        }
    }
    return WaterfallLayoutResult(
        rows = rows,
        clusterLayout = TimelineClusterLayout(gridItems = pseudoItems, clusters = clusters)
    )
}

/**
 * Pure 2-finger pinch-to-zoom modifier that NEVER intercepts 1-finger scrolling, tapping,
 * long-pressing, or horizontal paging. Drives live GPU elastic scale feedback and triggers
 * discrete zoom-tier transitions with tactile haptics.
 */
internal fun Modifier.galleryPinchZoomGesture(
    scope: CoroutineScope,
    pinchScaleAnim: Animatable<Float, AnimationVector1D>,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
): Modifier = pointerInput(onZoomIn, onZoomOut) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var cumulativeZoom = 1f
        var pinchingActive = false

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val pressedCount = event.changes.count { it.pressed }
            if (pressedCount == 0) {
                if (pinchingActive) {
                    scope.launch {
                        pinchScaleAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
                break
            }

            if (pressedCount >= 2) {
                val zoomChange = event.calculateZoom()
                if (zoomChange.isFinite() && zoomChange > 0f && kotlin.math.abs(zoomChange - 1f) > 0.002f) {
                    pinchingActive = true
                    cumulativeZoom *= zoomChange
                    event.changes.forEach { it.consume() }

                    val previewScale = cumulativeZoom.coerceIn(0.92f, 1.08f)
                    scope.launch { pinchScaleAnim.snapTo(previewScale) }

                    if (cumulativeZoom >= 1.22f) {
                        onZoomIn()
                        cumulativeZoom = 1f
                        scope.launch {
                            pinchScaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    } else if (cumulativeZoom <= 0.80f) {
                        onZoomOut()
                        cumulativeZoom = 1f
                        scope.launch {
                            pinchScaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                }
            } else if (pinchingActive) {
                // One finger lifted while pinching; smoothly reset elastic feedback
                pinchingActive = false
                cumulativeZoom = 1f
                scope.launch {
                    pinchScaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }
}

private val WaterfallSingleShape = RoundedCornerShape(14.dp)
private val WaterfallMultiShape = RoundedCornerShape(10.dp)
private val WaterfallPillShape = RoundedCornerShape(50)
private val WaterfallBurstScrim = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xAD000000))
)

@Composable
internal fun GalleryJustifiedWaterfallList(
    layoutResult: WaterfallLayoutResult,
    listState: LazyListState,
    singleColumnImmersive: Boolean,
    showMonthHeaders: Boolean,
    contentPadding: PaddingValues,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<Long>,
    monthPhotosMap: Map<String, List<PhotoItemEntity>>,
    haptic: HapticFeedback,
    onToggleBurst: (String) -> Unit,
    onSelectBestInBurst: (groupId: String, photoId: Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onTrashSinglePhoto: (PhotoItemEntity) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    onToggleSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    onLongPressSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    onCleanMonth: ((year: Int, month: Int, isCleaned: Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("gallery_waterfall_list")
    ) {
        items(
            items = layoutResult.rows,
            key = { it.stableKey },
            contentType = { row ->
                when (row) {
                    is WaterfallRowItem.MonthHeader -> "wf_month"
                    is WaterfallRowItem.DayHeader -> "wf_day"
                    is WaterfallRowItem.JustifiedPhotos -> if (row.cells.size == 1) "wf_row_1" else "wf_row_n"
                    is WaterfallRowItem.ExpandedBurst -> "wf_burst_exp"
                }
            }
        ) { row ->
            when (row) {
                is WaterfallRowItem.MonthHeader -> {
                    val monthPhotos = monthPhotosMap[row.cluster.yearMonthKey].orEmpty()
                    val isMonthAllSelected = isSelectionMode && monthPhotos.isNotEmpty() &&
                        monthPhotos.all { it.id in selectedPhotoIds }
                    MonthlyClusterHeaderStamp(
                        cluster = row.cluster,
                        isSelectionMode = isSelectionMode,
                        isMonthAllSelected = isMonthAllSelected,
                        onToggleSelectMonth = if (isSelectionMode && monthPhotos.isNotEmpty()) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleSelectPhotos(monthPhotos)
                            }
                        } else null,
                        onCleanMonth = onCleanMonth?.let { cb ->
                            { cb(row.cluster.year, row.cluster.month, row.cluster.isCleaned) }
                        }
                    )
                }

                is WaterfallRowItem.DayHeader -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showMonthHeaders) "${row.month}月${row.day}日" else "${row.day}日",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = InkPrimary
                        )
                        Text(
                            text = "${row.itemCount} 项",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = InkSecondary
                        )
                    }
                }

                is WaterfallRowItem.ExpandedBurst -> {
                    ExpandedBurstStackTray(
                        burst = row.burst,
                        isSelectionMode = isSelectionMode,
                        selectedPhotoIds = selectedPhotoIds,
                        onCollapse = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleBurst(row.burst.groupId)
                        },
                        onSelectBest = { photo ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectBestInBurst(row.burst.groupId, photo.id)
                        },
                        onKeepBest = { chosen ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onKeepBestInBurst(row.burst, chosen)
                        },
                        onTrashPhoto = { photo ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTrashSinglePhoto(photo)
                        },
                        onPhotoClick = onPhotoClick,
                        onToggleSelectPhoto = { photo ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSelectPhotos(listOf(photo))
                        },
                        onLongPressSelectPhoto = { photo ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressSelectPhotos(listOf(photo))
                        }
                    )
                }

                is WaterfallRowItem.JustifiedPhotos -> {
                    JustifiedWaterfallRow(
                        cells = row.cells,
                        singleColumnImmersive = singleColumnImmersive,
                        isSelectionMode = isSelectionMode,
                        selectedPhotoIds = selectedPhotoIds,
                        haptic = haptic,
                        onToggleBurst = onToggleBurst,
                        onPhotoClick = onPhotoClick,
                        onToggleSelectPhotos = onToggleSelectPhotos,
                        onLongPressSelectPhotos = onLongPressSelectPhotos
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JustifiedWaterfallRow(
    cells: List<WaterfallCell>,
    singleColumnImmersive: Boolean,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<Long>,
    haptic: HapticFeedback,
    onToggleBurst: (String) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    onToggleSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    onLongPressSelectPhotos: (List<PhotoItemEntity>) -> Unit
) {
    val spacing: Dp = 6.dp
    val isSingle = cells.size == 1
    val shape = if (isSingle) WaterfallSingleShape else WaterfallMultiShape
    val totalAr = cells.sumOf { it.aspectRatio.toDouble() }.toFloat().coerceAtLeast(0.45f)
    val isDark = LocalIsDarkTheme.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalSpacing = spacing * (cells.size - 1)
        val usableWidth = (maxWidth - totalSpacing).coerceAtLeast(80.dp)
        val naturalHeight = usableWidth / totalAr
        val rowHeight = if (isSingle) {
            naturalHeight.coerceIn(164.dp, if (singleColumnImmersive) 490.dp else 430.dp)
        } else {
            naturalHeight.coerceIn(114.dp, 290.dp)
        }

        val decodeTier = if (isSingle) ImageDownsampleTier.HERO_CARD else ImageDownsampleTier.PREVIEW_CARD
        val decodePx = if (isSingle) 960 else 640

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            cells.forEach { cell ->
                when (val entry = cell.entry) {
                    is GalleryEntry.Single -> {
                        val photo = entry.photo
                        val isSelected = isSelectionMode && (photo.id in selectedPhotoIds)
                        val scaleMod = if (isSelected) {
                            Modifier.graphicsLayer { scaleX = 0.96f; scaleY = 0.96f }
                        } else {
                            Modifier
                        }
                        Box(
                            modifier = Modifier
                                .weight(cell.aspectRatio)
                                .fillMaxHeight()
                                .then(scaleMod)
                                .clip(shape)
                                .background(SurfaceMuted)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.85.dp,
                                    color = if (isSelected) CobaltPrimary else HairlineBorder,
                                    shape = shape
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onToggleSelectPhotos(listOf(photo))
                                        } else {
                                            onPhotoClick(photo)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPressSelectPhotos(listOf(photo))
                                    }
                                )
                                .testTag("gallery_single_${photo.id}")
                        ) {
                            PhotoArtwork(
                                photo = photo,
                                contentDescription = photo.caption,
                                downsampleTier = decodeTier,
                                targetSizePx = decodePx,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(CobaltPrimary.copy(alpha = 0.16f))
                                )
                            }

                            if (isSelectionMode) {
                                SelectionCheckIndicator(
                                    isSelected = isSelected,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(7.dp)
                                )
                            } else if (photo.photoStatus == PhotoStatus.KEPT) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(7.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldKeep),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "已保留",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (photo.isFavorite) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(7.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(SolarAmber),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "收藏",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    is GalleryEntry.Burst -> {
                        val selectedCount = if (!isSelectionMode) 0 else entry.photos.count { it.id in selectedPhotoIds }
                        val allSelected = isSelectionMode && selectedCount == entry.photos.size && entry.photos.isNotEmpty()
                        val anySelected = selectedCount > 0
                        val stackBackColor = if (isDark) Color(0xFF283146) else Color(0xFFE3E7F0)
                        val scaleMod = if (anySelected) {
                            Modifier.graphicsLayer { scaleX = 0.96f; scaleY = 0.96f }
                        } else {
                            Modifier
                        }
                        Box(
                            modifier = Modifier
                                .weight(cell.aspectRatio)
                                .fillMaxHeight()
                                .then(scaleMod)
                                .drawBehind {
                                    // Subtle top stacked deck rim signaling burst group in waterfall
                                    val insetX = 6.dp.toPx()
                                    val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                    drawRoundRect(
                                        color = stackBackColor,
                                        topLeft = Offset(insetX, 0f),
                                        size = Size(size.width - insetX * 2f, size.height - 4.dp.toPx()),
                                        cornerRadius = corner
                                    )
                                }
                                .padding(top = 5.dp)
                                .clip(shape)
                                .background(SurfacePure)
                                .border(
                                    width = if (anySelected) 2.5.dp else 1.2.dp,
                                    color = if (anySelected) CobaltPrimary else HairlineBorder,
                                    shape = shape
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onToggleSelectPhotos(entry.photos)
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onToggleBurst(entry.groupId)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPressSelectPhotos(entry.photos)
                                    }
                                )
                                .testTag("burst_collapsed_tile_${entry.groupId}")
                        ) {
                            PhotoArtwork(
                                photo = entry.bestPhoto,
                                contentDescription = "连拍组，共 ${entry.photos.size} 张，点击展开或长按多选",
                                showVideoBadge = false,
                                downsampleTier = decodeTier,
                                targetSizePx = decodePx,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (anySelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(CobaltPrimary.copy(alpha = 0.16f))
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(WaterfallBurstScrim)
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(7.dp)
                                    .clip(WaterfallPillShape)
                                    .background(ObsidianOverlayGlass)
                                    .border(0.85.dp, ObsidianOverlayBorder, WaterfallPillShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BurstMode,
                                    contentDescription = null,
                                    tint = SolarAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${entry.photos.size} 连拍",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }

                            if (isSelectionMode) {
                                SelectionCheckIndicator(
                                    isSelected = allSelected,
                                    partialCountText = if (anySelected && !allSelected) "$selectedCount/${entry.photos.size}" else null,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(7.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelectionMode) {
                                        if (allSelected) "已选整组" else "点击选整组"
                                    } else {
                                        "点击展开对比"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (!isSelectionMode) {
                                    Icon(
                                        imageVector = Icons.Filled.ExpandMore,
                                        contentDescription = stringResource(R.string.burst_expand),
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact, icon-only view density switch displaying the silhouette of the active [GalleryZoomTier].
 * Keeps layout switching visually distinct from content filters while preserving a 48dp accessible hit target.
 */
@Composable
fun GalleryZoomTierIconButton(
    currentTier: GalleryZoomTier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualSize: Dp = 36.dp
) {
    val isDark = LocalIsDarkTheme.current
    val isWaterfall = currentTier.isWaterfall
    val containerColor = when {
        isWaterfall && isDark -> CobaltPrimary.copy(alpha = 0.24f)
        isWaterfall -> CobaltPrimary.copy(alpha = 0.12f)
        isDark -> SurfacePure.copy(alpha = 0.80f)
        else -> SurfacePure.copy(alpha = 0.94f)
    }
    val borderColor = when {
        isWaterfall -> CobaltPrimary.copy(alpha = if (isDark) 0.56f else 0.42f)
        isDark -> HairlineBorder
        else -> HairlineBorder.copy(alpha = 0.90f)
    }
    val iconTint = if (isWaterfall) CobaltPrimary else InkPrimary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(visualSize)
                .clip(CircleShape)
                .background(containerColor)
                .border(width = 0.95.dp, color = borderColor, shape = CircleShape)
        ) {
            AnimatedContent(
                targetState = currentTier,
                transitionSpec = {
                    (fadeIn(tween(140)) + scaleIn(initialScale = 0.82f, animationSpec = tween(140))) togetherWith
                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.82f, animationSpec = tween(100)))
                },
                label = "gallery_zoom_tier_icon"
            ) { tier ->
                val icon = when (tier) {
                    GalleryZoomTier.COMPACT_4 -> Icons.Filled.Apps
                    GalleryZoomTier.STANDARD_3 -> Icons.Filled.GridView
                    GalleryZoomTier.WATERFALL_2 -> Icons.Filled.Dashboard
                    GalleryZoomTier.WATERFALL_1 -> Icons.Filled.ViewAgenda
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "当前视图：${tier.label}，点按切换布局",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

