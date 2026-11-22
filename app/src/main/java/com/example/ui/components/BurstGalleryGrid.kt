package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BurstMode
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.MonthStampEntity
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.GalleryEntry
import kotlinx.coroutines.launch
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme

// Hoisted static shapes & invariant overlay colors to guarantee zero per-tile allocations during high-FPS scrolling
// 摩登画报造型语法：画片圆角 2~4dp（弃 10~22dp 大圆角）
private val TileShape = RoundedCornerShape(3.dp)
private val FrameCardShape = RoundedCornerShape(4.dp)
private val FrameImageShape = RoundedCornerShape(2.dp)
private val TrayShape = RoundedCornerShape(4.dp)
private val SmallButtonShape = RoundedCornerShape(3.dp)
private val PillShape = RoundedCornerShape(50)

private val BurstBottomScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xAD000000))
)
// 朱红小签边（纸黄 25%）与深墨序号签底：纸黄只压照片暗部 / 朱红底（可读性红线）
private val DecoBadgeBorder = Color(0x40EFE3C8)
private val SequencePillBg = Color(0x8C000000)
private val UnselectedCheckBg = Color(0x47000000)
private val UnselectedCheckBorder = Color(0xEBEFE3C8)
// 最优签边框：主题金 40%（与相邻 decoGold 派生编码一致，弃 0x66C9A24A 固定暗金写法）
private val BestActiveBorder: Color
    @Composable get() = DecoGold.copy(alpha = 0.40f)

// 干支纪年（2026 丙午 / 2025 乙巳 / 2024 甲辰 / 2023 癸卯 / 2022 壬寅 —— 通式推算）
private val DecoStems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val DecoBranches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

private fun decoGanZhi(year: Int): String =
    DecoStems[(year - 4).mod(10)] + DecoBranches[(year - 4).mod(12)]

private fun decoChineseMonth(month: Int): String = when (month) {
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
 * High-FPS interactive Gallery Grid supporting collapsible/expandable Burst Stacks.
 *
 * Rendering & Scroll Architecture:
 * - Zero per-scroll `isScrolling` recomposition toggles or per-tile `LaunchedEffect` animations.
 * - Collapsed burst stacks render their signature 3D fanned print edges in a single GPU-cheap
 *   `drawBehind` canvas pass (1 image decode per burst instead of 3, 0 RenderNode hardware shadows).
 * - Expanded burst trays disable offscreen RenderEffect blur so scrolling past expanded groups
 *   stays locked at 60/120fps.
 */
@Composable
fun BurstGalleryGrid(
    entries: List<GalleryEntry>,
    expandedBurstIds: Set<String>,
    onToggleBurst: (String) -> Unit,
    onSelectBestInBurst: (groupId: String, photoId: Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onTrashSinglePhoto: (PhotoItemEntity) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    columns: Int = 3,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    isSelectionMode: Boolean = false,
    selectedPhotoIds: Set<Long> = emptySet(),
    onToggleSelectPhotos: (List<PhotoItemEntity>) -> Unit = {},
    onLongPressSelectPhotos: (List<PhotoItemEntity>) -> Unit = {},
    onCleanMonth: ((year: Int, month: Int, isCleaned: Boolean) -> Unit)? = null,
    monthStamps: Map<String, MonthStampEntity> = emptyMap(),
    showMonthStampOverlay: Boolean = false,
    precomputedClusterLayout: TimelineClusterLayout? = null,
    zoomTier: GalleryZoomTier? = null,
    onZoomTierChanged: ((GalleryZoomTier) -> Unit)? = null,
    enablePinchZoom: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val waterfallListState = rememberLazyListState()

    var internalZoomTier by rememberSaveable(columns) {
        mutableStateOf(defaultGalleryZoomTier(columns))
    }
    val activeZoomTier = zoomTier ?: internalZoomTier
    val effectiveGridColumns = if (activeZoomTier.isWaterfall) columns else activeZoomTier.gridColumns()

    var zoomHudPulseToken by remember { mutableIntStateOf(0) }
    var showZoomHud by remember { mutableStateOf(false) }
    var pendingAnchorEntryKey by remember { mutableStateOf<String?>(null) }
    val pinchScaleAnim = remember { Animatable(1f) }

    LaunchedEffect(zoomHudPulseToken) {
        if (zoomHudPulseToken > 0) {
            showZoomHud = true
            delay(1300L)
            showZoomHud = false
        }
    }

    val clusterLayout = if (showMonthStampOverlay) {
        precomputedClusterLayout ?: remember(entries, monthStamps) {
            buildTimelineClusterLayout(entries, monthStamps)
        }
    } else {
        null
    }

    val waterfallLayout = remember(
        entries,
        expandedBurstIds,
        monthStamps,
        showMonthStampOverlay,
        activeZoomTier
    ) {
        if (activeZoomTier.isWaterfall) {
            buildWaterfallLayout(
                entries = entries,
                expandedBurstIds = expandedBurstIds,
                monthStamps = monthStamps,
                showMonthHeaders = showMonthStampOverlay,
                singleColumnImmersive = activeZoomTier == GalleryZoomTier.WATERFALL_1
            )
        } else {
            null
        }
    }

    fun captureCurrentAnchorKey(): String? {
        return if (activeZoomTier.isWaterfall) {
            val rows = waterfallLayout?.rows.orEmpty()
            val startIdx = waterfallListState.firstVisibleItemIndex.coerceIn(0, (rows.lastIndex).coerceAtLeast(0))
            for (i in startIdx until rows.size) {
                when (val r = rows[i]) {
                    is WaterfallRowItem.JustifiedPhotos -> return r.cells.firstOrNull()?.entry?.stableKey
                    is WaterfallRowItem.ExpandedBurst -> return r.burst.stableKey
                    else -> Unit
                }
            }
            null
        } else if (clusterLayout != null) {
            val items = clusterLayout.gridItems
            val startIdx = gridState.firstVisibleItemIndex.coerceIn(0, (items.lastIndex).coerceAtLeast(0))
            for (i in startIdx until items.size) {
                val it = items[i]
                if (it is TimelineGridItem.MediaEntry) return it.entry.stableKey
            }
            null
        } else {
            entries.getOrNull(gridState.firstVisibleItemIndex)?.stableKey
        }
    }

    fun updateZoomTier(next: GalleryZoomTier) {
        if (next == activeZoomTier) return
        pendingAnchorEntryKey = captureCurrentAnchorKey()
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (onZoomTierChanged != null) {
            onZoomTierChanged(next)
        } else {
            internalZoomTier = next
        }
        zoomHudPulseToken++
    }

    // Preserve visual scroll anchor cleanly across Grid <-> Waterfall transitions
    LaunchedEffect(activeZoomTier.isWaterfall, pendingAnchorEntryKey) {
        val anchorKey = pendingAnchorEntryKey ?: return@LaunchedEffect
        if (activeZoomTier.isWaterfall) {
            val rows = waterfallLayout?.rows.orEmpty()
            val targetRowIdx = rows.indexOfFirst { row ->
                when (row) {
                    is WaterfallRowItem.JustifiedPhotos -> row.cells.any { it.entry.stableKey == anchorKey }
                    is WaterfallRowItem.ExpandedBurst -> row.burst.stableKey == anchorKey
                    else -> false
                }
            }
            if (targetRowIdx >= 0) {
                runCatching { waterfallListState.scrollToItem(targetRowIdx) }
            }
        } else {
            val targetGridIdx = if (clusterLayout != null) {
                clusterLayout.gridItems.indexOfFirst { it is TimelineGridItem.MediaEntry && it.entry.stableKey == anchorKey }
            } else {
                entries.indexOfFirst { it.stableKey == anchorKey }
            }
            if (targetGridIdx >= 0) {
                runCatching { gridState.scrollToItem(targetGridIdx) }
            }
        }
        pendingAnchorEntryKey = null
    }

    val monthPhotosMap = remember(entries, isSelectionMode, showMonthStampOverlay) {
        if (!isSelectionMode || !showMonthStampOverlay) {
            emptyMap()
        } else {
            val map = HashMap<String, ArrayList<PhotoItemEntity>>()
            for (entry in entries) {
                val key = "${entry.primaryPhoto.year}-${entry.primaryPhoto.month.toString().padStart(2, '0')}"
                val list = map.getOrPut(key) { ArrayList() }
                when (entry) {
                    is GalleryEntry.Single -> list.add(entry.photo)
                    is GalleryEntry.Burst -> list.addAll(entry.photos)
                }
            }
            map
        }
    }

    // Density-calibrated thumbnail decode size capped tightly to keep memory & texture upload fast
    val gridThumbPx = remember(density, effectiveGridColumns) {
        val approxTileDp = (360f / effectiveGridColumns.coerceAtLeast(1))
        with(density) { (approxTileDp * 0.88f).dp.toPx().toInt() }
            .coerceIn(180, ImageDownsampleTier.GRID_THUMB.maxSizePx)
    }

    val pinchModifier = if (enablePinchZoom) {
        Modifier.galleryPinchZoomGesture(
            scope = coroutineScope,
            pinchScaleAnim = pinchScaleAnim,
            onZoomIn = { updateZoomTier(activeZoomTier.zoomIn()) },
            onZoomOut = { updateZoomTier(activeZoomTier.zoomOut()) }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(pinchModifier)
            .graphicsLayer {
                val s = pinchScaleAnim.value
                scaleX = s
                scaleY = s
            }
    ) {
        AnimatedContent(
            targetState = activeZoomTier.isWaterfall,
            transitionSpec = {
                (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.97f, animationSpec = tween(190, easing = FastOutSlowInEasing))) togetherWith
                    fadeOut(tween(120))
            },
            label = "gallery_grid_waterfall_mode",
            modifier = Modifier.fillMaxSize()
        ) { isWaterfall ->
            if (isWaterfall && waterfallLayout != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GalleryJustifiedWaterfallList(
                        layoutResult = waterfallLayout,
                        listState = waterfallListState,
                        singleColumnImmersive = activeZoomTier == GalleryZoomTier.WATERFALL_1,
                        showMonthHeaders = showMonthStampOverlay,
                        contentPadding = contentPadding,
                        isSelectionMode = isSelectionMode,
                        selectedPhotoIds = selectedPhotoIds,
                        monthPhotosMap = monthPhotosMap,
                        haptic = haptic,
                        onToggleBurst = onToggleBurst,
                        onSelectBestInBurst = onSelectBestInBurst,
                        onKeepBestInBurst = onKeepBestInBurst,
                        onTrashSinglePhoto = onTrashSinglePhoto,
                        onPhotoClick = onPhotoClick,
                        onToggleSelectPhotos = onToggleSelectPhotos,
                        onLongPressSelectPhotos = onLongPressSelectPhotos,
                        onCleanMonth = onCleanMonth
                    )

                    if (showMonthStampOverlay) {
                        TimelineMonthStampOverlay(
                            listState = waterfallListState,
                            clusterLayout = waterfallLayout.clusterLayout,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else if (showMonthStampOverlay && clusterLayout != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(effectiveGridColumns),
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(if (effectiveGridColumns >= 4) 7.dp else 10.dp),
                        verticalArrangement = Arrangement.spacedBy(if (effectiveGridColumns >= 4) 8.dp else 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = clusterLayout.gridItems,
                            key = { it.stableKey },
                            contentType = { item ->
                                when (item) {
                                    is TimelineGridItem.MonthDivider -> "month_divider"
                                    is TimelineGridItem.MediaEntry -> when (val entry = item.entry) {
                                        is GalleryEntry.Single -> "single"
                                        is GalleryEntry.Burst -> if (entry.groupId in expandedBurstIds) "burst_expanded" else "burst_collapsed"
                                    }
                                }
                            },
                            span = { item ->
                                val fullSpan = when (item) {
                                    is TimelineGridItem.MonthDivider -> true
                                    is TimelineGridItem.MediaEntry ->
                                        item.entry is GalleryEntry.Burst && item.entry.groupId in expandedBurstIds
                                }
                                GridItemSpan(if (fullSpan) maxLineSpan else 1)
                            }
                        ) { item ->
                            when (item) {
                                is TimelineGridItem.MonthDivider -> {
                                    val monthPhotos = monthPhotosMap[item.cluster.yearMonthKey].orEmpty()
                                    val isMonthAllSelected = isSelectionMode && monthPhotos.isNotEmpty() &&
                                        monthPhotos.all { it.id in selectedPhotoIds }
                                    DecoMonthClusterHeader(
                                        cluster = item.cluster,
                                        isSelectionMode = isSelectionMode,
                                        isMonthAllSelected = isMonthAllSelected,
                                        onToggleSelectMonth = if (isSelectionMode && monthPhotos.isNotEmpty()) {
                                            {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onToggleSelectPhotos(monthPhotos)
                                            }
                                        } else null,
                                        onCleanMonth = onCleanMonth?.let { cb ->
                                            { cb(item.cluster.year, item.cluster.month, item.cluster.isCleaned) }
                                        }
                                    )
                                }

                                is TimelineGridItem.MediaEntry -> {
                                    GalleryEntryGridCell(
                                        entry = item.entry,
                                        thumbSizePx = gridThumbPx,
                                        expandedBurstIds = expandedBurstIds,
                                        isSelectionMode = isSelectionMode,
                                        selectedPhotoIds = selectedPhotoIds,
                                        haptic = haptic,
                                        onToggleBurst = onToggleBurst,
                                        onSelectBestInBurst = onSelectBestInBurst,
                                        onKeepBestInBurst = onKeepBestInBurst,
                                        onTrashSinglePhoto = onTrashSinglePhoto,
                                        onPhotoClick = onPhotoClick,
                                        onToggleSelectPhotos = onToggleSelectPhotos,
                                        onLongPressSelectPhotos = onLongPressSelectPhotos
                                    )
                                }
                            }
                        }
                    }

                    TimelineMonthStampOverlay(
                        gridState = gridState,
                        clusterLayout = clusterLayout,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(effectiveGridColumns),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(if (effectiveGridColumns >= 4) 7.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (effectiveGridColumns >= 4) 8.dp else 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = entries,
                        key = { it.stableKey },
                        contentType = { entry ->
                            when (entry) {
                                is GalleryEntry.Single -> "single"
                                is GalleryEntry.Burst -> if (entry.groupId in expandedBurstIds) "burst_expanded" else "burst_collapsed"
                            }
                        },
                        span = { entry ->
                            val isExpandedBurst = entry is GalleryEntry.Burst && entry.groupId in expandedBurstIds
                            GridItemSpan(if (isExpandedBurst) maxLineSpan else 1)
                        }
                    ) { entry ->
                        GalleryEntryGridCell(
                            entry = entry,
                            thumbSizePx = gridThumbPx,
                            expandedBurstIds = expandedBurstIds,
                            isSelectionMode = isSelectionMode,
                            selectedPhotoIds = selectedPhotoIds,
                            haptic = haptic,
                            onToggleBurst = onToggleBurst,
                            onSelectBestInBurst = onSelectBestInBurst,
                            onKeepBestInBurst = onKeepBestInBurst,
                            onTrashSinglePhoto = onTrashSinglePhoto,
                            onPhotoClick = onPhotoClick,
                            onToggleSelectPhotos = onToggleSelectPhotos,
                            onLongPressSelectPhotos = onLongPressSelectPhotos
                        )
                    }
                }
            }
        }

        // Ephemeral floating zoom-tier HUD pill when pinch-zooming（摩登画报：深墨签底 + 纸黄字 + 金点）
        AnimatedVisibility(
            visible = showZoomHud,
            enter = fadeIn(tween(150)) + slideInVertically(tween(170)) { -it / 2 },
            exit = fadeOut(tween(220)) + slideOutVertically(tween(180)) { -it / 3 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(PillShape)
                    .background(SequencePillBg)
                    .border(0.9.dp, DecoBadgeBorder, PillShape)
                    .padding(horizontal = 13.dp, vertical = 6.dp)
                    .testTag("gallery_zoom_tier_hud")
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DecoGold)
                )
                Text(
                    text = activeZoomTier.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = DecoPaperCream
                )

            }
        }
    }
}

@Composable
private fun GalleryEntryGridCell(
    entry: GalleryEntry,
    thumbSizePx: Int,
    expandedBurstIds: Set<String>,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<Long>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onToggleBurst: (String) -> Unit,
    onSelectBestInBurst: (groupId: String, photoId: Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onTrashSinglePhoto: (PhotoItemEntity) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    onToggleSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    onLongPressSelectPhotos: (List<PhotoItemEntity>) -> Unit
) {
    when (entry) {
        is GalleryEntry.Single -> {
            val isSelected = isSelectionMode && (entry.photo.id in selectedPhotoIds)
            SingleGalleryTile(
                photo = entry.photo,
                thumbSizePx = thumbSizePx,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onClick = {
                    if (isSelectionMode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleSelectPhotos(listOf(entry.photo))
                    } else {
                        onPhotoClick(entry.photo)
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressSelectPhotos(listOf(entry.photo))
                },
                modifier = Modifier.testTag("gallery_single_${entry.photo.id}")
            )
        }

        is GalleryEntry.Burst -> {
            val isExpanded = entry.groupId in expandedBurstIds
            AnimatedBurstGalleryItem(
                burst = entry,
                thumbSizePx = thumbSizePx,
                isExpanded = isExpanded,
                isSelectionMode = isSelectionMode,
                selectedPhotoIds = selectedPhotoIds,
                onToggleExpand = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleBurst(entry.groupId)
                },
                onSelectBest = { photo ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectBestInBurst(entry.groupId, photo.id)
                },
                onKeepBest = { chosen ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onKeepBestInBurst(entry, chosen)
                },
                onTrashPhoto = { photo ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTrashSinglePhoto(photo)
                },
                onPhotoClick = onPhotoClick,
                onToggleSelectPhotos = { photos ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleSelectPhotos(photos)
                },
                onLongPressSelectPhotos = { photos ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPressSelectPhotos(photos)
                },
                modifier = Modifier.testTag("gallery_burst_${entry.groupId}")
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SingleGalleryTile(
    photo: PhotoItemEntity,
    thumbSizePx: Int,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only allocate a RenderNode graphicsLayer when actually scaled down in selection mode
    val selectionScaleModifier = if (isSelected) {
        Modifier.graphicsLayer {
            scaleX = 0.92f
            scaleY = 0.92f
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .then(selectionScaleModifier)
            .clip(TileShape)
            .background(LocalFlipYearPalette.current.decoMat)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) LocalFlipYearPalette.current.decoVerm else LocalFlipYearPalette.current.decoGold.copy(alpha = 0.45f),
                shape = TileShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        PhotoArtwork(
            photo = photo,
            contentDescription = photo.caption,
            showVideoBadge = false,
            downsampleTier = ImageDownsampleTier.GRID_THUMB,
            targetSizePx = thumbSizePx,
            modifier = Modifier.fillMaxSize()
        )

        // 深墨视频小签（压照片暗部 + 纸黄字，deco-app.html 画廊屏视频签）
        if (photo.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SequencePillBg)
                    .border(0.75.dp, DecoBadgeBorder, RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = DecoPaperCream,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "视频",
                    fontSize = 12.sp,
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = DecoPaperCream
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalFlipYearPalette.current.decoVerm.copy(alpha = 0.16f))
            )
        }

        if (isSelectionMode) {
            SelectionCheckIndicator(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        } else if (photo.photoStatus == PhotoStatus.KEPT) {
            // 已保留 = 金印章（金底 + 内衬字）
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(LocalFlipYearPalette.current.decoGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "已保留",
                    tint = LocalFlipYearPalette.current.decoMat,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        if (photo.isFavorite) {
            // 收藏 = 朱红小签章（朱底 + 纸黄星，压照片暗部）
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(LocalFlipYearPalette.current.decoVerm),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "收藏",
                    tint = DecoPaperCream,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
internal fun SelectionCheckIndicator(
    isSelected: Boolean,
    partialCountText: String? = null,
    modifier: Modifier = Modifier
) {
    when {
        isSelected -> {
            Box(
                modifier = modifier
                    .size(23.dp)
                    .clip(CircleShape)
                    .background(LocalFlipYearPalette.current.decoVerm)
                    .border(1.8.dp, UnselectedCheckBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "已选中",
                    tint = DecoPaperCream,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        partialCountText != null -> {
            Box(
                modifier = modifier
                    .clip(PillShape)
                    .background(LocalFlipYearPalette.current.decoVerm)
                    .border(1.5.dp, UnselectedCheckBorder, PillShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = partialCountText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    color = DecoPaperCream
                )
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .size(23.dp)
                    .clip(CircleShape)
                    .background(UnselectedCheckBg)
                    .border(1.8.dp, UnselectedCheckBorder, CircleShape)
            )
        }
    }
}

@Composable
private fun AnimatedBurstGalleryItem(
    burst: GalleryEntry.Burst,
    thumbSizePx: Int,
    isExpanded: Boolean,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onToggleExpand: () -> Unit,
    onSelectBest: (PhotoItemEntity) -> Unit,
    onKeepBest: (PhotoItemEntity) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    onToggleSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    onLongPressSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isExpanded) {
        CollapsedBurstStackTile(
            burst = burst,
            thumbSizePx = thumbSizePx,
            isSelectionMode = isSelectionMode,
            selectedPhotoIds = selectedPhotoIds,
            onClick = {
                if (isSelectionMode) {
                    onToggleSelectPhotos(burst.photos)
                } else {
                    onToggleExpand()
                }
            },
            onLongClick = {
                onLongPressSelectPhotos(burst.photos)
            },
            modifier = modifier
        )
    } else {
        ExpandedBurstStackTray(
            burst = burst,
            isSelectionMode = isSelectionMode,
            selectedPhotoIds = selectedPhotoIds,
            onCollapse = onToggleExpand,
            onSelectBest = onSelectBest,
            onKeepBest = onKeepBest,
            onTrashPhoto = onTrashPhoto,
            onPhotoClick = onPhotoClick,
            onToggleSelectPhoto = { photo -> onToggleSelectPhotos(listOf(photo)) },
            onLongPressSelectPhoto = { photo -> onLongPressSelectPhotos(listOf(photo)) },
            modifier = modifier
        )
    }
}

/**
 * High-FPS 3D Stacked Deck representation of a collapsed burst group in the gallery grid.
 * Draws the fanned peeking gallery-print edges in a single GPU-cheap `drawBehind` pass
 * (zero extra image decodes, zero offscreen layers, zero per-tile hardware shadows) while
 * rendering the crisp best-shot cover print on top.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollapsedBurstStackTile(
    burst: GalleryEntry.Burst,
    thumbSizePx: Int,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCount = if (!isSelectionMode) {
        0
    } else {
        burst.photos.count { it.id in selectedPhotoIds }
    }
    val allSelected = isSelectionMode && selectedCount == burst.photos.size && burst.photos.isNotEmpty()
    val anySelected = selectedCount > 0
    val hasThreePlus = burst.photos.size >= 3

    val selectionScaleModifier = if (anySelected) {
        Modifier.graphicsLayer {
            scaleX = 0.93f
            scaleY = 0.93f
        }
    } else {
        Modifier
    }

    val isDark = LocalIsDarkTheme.current
    val deco = LocalFlipYearPalette.current
    // 画报纸叠：纸面层色取内衬/地面系，纸边描弱金线
    val backCardDeepFill = if (isDark) Color(0xFF0F241F) else Color(0xFFDCD8CC)
    val backCardMidFill = if (isDark) Color(0xFF1B382F) else Color(0xFFE8E4D8)
    val backCardShadowTint = if (isDark) Color(0x45000000) else Color(0x1612141B)
    val frontCardShadowTint = if (isDark) Color(0x58000000) else Color(0x1C12141B)
    val backCardRimBorder = deco.decoGold.copy(alpha = 0.45f)
    val frontIdleBorder = deco.decoGold.copy(alpha = 0.60f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .then(selectionScaleModifier)
            .drawBehind {
                val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                val rimStroke = Stroke(width = 1.6.dp.toPx())
                val padX3 = 6.dp.toPx()
                val padX2 = 3.5.dp.toPx()
                val topFront = 8.dp.toPx()

                // 3rd Layer: Deepest fanned gallery-print edge (drawn directly on canvas)
                if (hasThreePlus) {
                    val cardW3 = size.width - padX3 * 2f
                    val cardH3 = size.height - 10.dp.toPx()
                    val left3 = padX3 + 3.dp.toPx()
                    val top3 = 0.5.dp.toPx()
                    withTransform({
                        rotate(
                            degrees = 4.6f,
                            pivot = Offset(left3 + cardW3 * 0.5f, top3 + cardH3 * 0.55f)
                        )
                    }) {
                        drawRoundRect(
                            color = backCardShadowTint,
                            topLeft = Offset(left3, top3 + 1.5.dp.toPx()),
                            size = Size(cardW3, cardH3),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            color = backCardDeepFill,
                            topLeft = Offset(left3, top3),
                            size = Size(cardW3, cardH3),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            color = backCardRimBorder,
                            topLeft = Offset(left3, top3),
                            size = Size(cardW3, cardH3),
                            cornerRadius = corner,
                            style = rimStroke
                        )
                    }
                }

                // 2nd Layer: Mid fanned gallery-print edge
                val cardW2 = size.width - padX2 * 2f
                val cardH2 = size.height - 9.dp.toPx()
                val left2 = padX2 - 2.5.dp.toPx()
                val top2 = 3.dp.toPx()
                withTransform({
                    rotate(
                        degrees = -4.8f,
                        pivot = Offset(left2 + cardW2 * 0.5f, top2 + cardH2 * 0.55f)
                    )
                }) {
                    drawRoundRect(
                        color = backCardShadowTint,
                        topLeft = Offset(left2, top2 + 1.5.dp.toPx()),
                        size = Size(cardW2, cardH2),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = backCardMidFill,
                        topLeft = Offset(left2, top2),
                        size = Size(cardW2, cardH2),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = backCardRimBorder,
                        topLeft = Offset(left2, top2),
                        size = Size(cardW2, cardH2),
                        cornerRadius = corner,
                        style = rimStroke
                    )
                }

                // Soft grounded shadow halo behind front print (zero RenderNode elevation cost)
                drawRoundRect(
                    color = frontCardShadowTint,
                    topLeft = Offset(1.dp.toPx(), topFront + 2.5.dp.toPx()),
                    size = Size(size.width - 2.dp.toPx(), size.height - topFront),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("burst_collapsed_tile_${burst.groupId}")
    ) {
        // Front Top Card (Gallery Print Cover with Deco Gold Rim or Vermilion Selection Rim)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .clip(TileShape)
                .background(deco.decoMat)
                .border(
                    width = if (anySelected) 2.5.dp else 1.8.dp,
                    color = if (anySelected) deco.decoVerm else frontIdleBorder,
                    shape = TileShape
                )
        ) {
            PhotoArtwork(
                photo = burst.bestPhoto,
                contentDescription = "连拍组，共 ${burst.photos.size} 张，点击展开或长按多选",
                showVideoBadge = false,
                downsampleTier = ImageDownsampleTier.GRID_THUMB,
                targetSizePx = thumbSizePx,
                modifier = Modifier.fillMaxSize()
            )

            if (anySelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(deco.decoVerm.copy(alpha = 0.16f))
                )
            }

            // Bottom subtle gallery vignette for badge legibility (hoisted static Brush)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(BurstBottomScrimBrush)
            )

            // Top-Right: Selection Check Badge in multi-select mode, else Best Shot Gold Seal
            if (isSelectionMode) {
                SelectionCheckIndicator(
                    isSelected = allSelected,
                    partialCountText = if (anySelected && !allSelected) "$selectedCount/${burst.photos.size}" else null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(deco.decoGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "最佳连拍",
                        tint = deco.decoMat,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Bottom cue: 朱红小签（连拍 N，压照片暗部）+ 展开/选组提示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(deco.decoVerm)
                        .border(0.75.dp, DecoBadgeBorder, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BurstMode,
                        contentDescription = null,
                        tint = DecoPaperCream,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "连拍 ${burst.photos.size}",
                        fontSize = 12.sp,
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = DecoPaperCream
                    )
                }
                if (isSelectionMode) {
                    Text(
                        text = if (allSelected) "已选整组" else "点击选整组",
                        fontSize = 12.sp,
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = DecoPaperCream
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(R.string.burst_expand),
                        tint = DecoPaperCream,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-width inline Expanded Burst Tray optimized for zero-jank vertical & horizontal scrolling.
 */
@Composable
internal fun ExpandedBurstStackTray(
    burst: GalleryEntry.Burst,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onCollapse: () -> Unit,
    onSelectBest: (PhotoItemEntity) -> Unit,
    onKeepBest: (PhotoItemEntity) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    onToggleSelectPhoto: (PhotoItemEntity) -> Unit,
    onLongPressSelectPhoto: (PhotoItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstTimestamp = remember(burst.photos) { burst.photos.minOf { it.takenAtMillis } }
    val headerSubtitle = remember(firstTimestamp, burst.spanSeconds, burst.totalBytes) {
        "${formatPhotoDateTime(firstTimestamp)} · ${burst.spanSeconds}秒内连拍 · ${formatStorageSize(burst.totalBytes)}"
    }
    val keepBestLabel = remember(burst.photos.size, burst.nonBestBytes) {
        "仅保留最佳 · 清理其余 ${burst.photos.size - 1} 张 (${formatStorageSize(burst.nonBestBytes)})"
    }

    // Keep enableAmbientBlur = false and minimal shadowElevation inside scrolling LazyGrid items
    FrostedGlassCard(
        shape = TrayShape,
        borderAccent = LocalFlipYearPalette.current.decoGold,
        shadowElevation = 2.dp,
        enableAmbientBlur = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("burst_expanded_tray_${burst.groupId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Burst Group Indicator + Capture Span + Collapse Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(SmallButtonShape)
                            .background(LocalFlipYearPalette.current.decoGold.copy(alpha = 0.12f))
                            .padding(7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BurstMode,
                            contentDescription = null,
                            tint = LocalFlipYearPalette.current.decoGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "快速连拍组 · ${burst.photos.size} 张",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = LocalFlipYearPalette.current.decoInkOnGround
                        )
                        Text(
                            text = headerSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DecoDisplayFontFamily,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = LocalFlipYearPalette.current.decoInkOnGround.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Prominent Collapse Action Pill
                Surface(
                    onClick = onCollapse,
                    shape = PillShape,
                    color = LocalFlipYearPalette.current.decoMat,
                    border = BorderStroke(1.dp, LocalFlipYearPalette.current.decoGold.copy(alpha = 0.40f)),
                    modifier = Modifier.testTag("burst_collapse_button_${burst.groupId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.UnfoldLess,
                            contentDescription = stringResource(R.string.burst_collapse),
                            tint = LocalFlipYearPalette.current.decoInkOnGround,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "收起",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = DecoDisplayFontFamily,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp,
                            color = LocalFlipYearPalette.current.decoInkOnGround
                        )
                    }
                }
            }

            // Horizontal Strip of Individual Burst Shots (instantaneous recycling, zero stagger lag)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(
                    items = burst.photos,
                    key = { _, p -> p.id },
                    contentType = { _, _ -> "burst_frame" }
                ) { idx, photo ->
                    val isBest = photo.id == burst.bestPhoto.id
                    val isSelected = isSelectionMode && (photo.id in selectedPhotoIds)
                    val deltaSeconds = ((photo.takenAtMillis - firstTimestamp) / 1000L).coerceAtLeast(0L)

                    BurstFrameCard(
                        index = idx,
                        photo = photo,
                        isBest = isBest,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        deltaSeconds = deltaSeconds,
                        onSelectBest = { onSelectBest(photo) },
                        onTrash = { onTrashPhoto(photo) },
                        onClick = {
                            if (isSelectionMode) onToggleSelectPhoto(photo) else onPhotoClick(photo)
                        },
                        onLongClick = { onLongPressSelectPhoto(photo) }
                    )
                }
            }

            // Bottom One-Tap Smart Cleanup Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GalleryActionButton(
                    text = keepBestLabel,
                    leadingIcon = Icons.Outlined.AutoAwesome,
                    tone = GalleryButtonTone.IMMERSIVE,
                    height = 44.dp,
                    shape = TileShape,
                    onClick = { onKeepBest(burst.bestPhoto) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("burst_keep_best_inline_${burst.groupId}")
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BurstFrameCard(
    index: Int,
    photo: PhotoItemEntity,
    isBest: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    deltaSeconds: Long,
    onSelectBest: () -> Unit,
    onTrash: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val highlight = isSelected || isBest
    val deco = LocalFlipYearPalette.current
    val containerColor = when {
        isSelected -> deco.decoVerm.copy(alpha = 0.16f)
        isBest -> deco.decoGold.copy(alpha = 0.16f)
        else -> deco.decoGold.copy(alpha = 0.06f)
    }
    val highlightColor = if (isSelected) deco.decoVerm else deco.decoGold

    Column(
        modifier = Modifier
            .width(146.dp)
            .clip(FrameCardShape)
            .background(containerColor)
            .border(
                width = if (highlight) 2.2.dp else 1.dp,
                color = if (highlight) highlightColor else deco.decoGold.copy(alpha = 0.45f),
                shape = FrameCardShape
            )
            .padding(6.dp)
            .testTag("burst_frame_card_${photo.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.86f)
                .clip(FrameImageShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            PhotoArtwork(
                photo = photo,
                contentDescription = photo.caption,
                showVideoBadge = false,
                downsampleTier = ImageDownsampleTier.PREVIEW_CARD,
                targetSizePx = 320,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(deco.decoVerm.copy(alpha = 0.16f))
                )
            }

            // Sequence + Time Delta Pill (Top-Left, 深墨底 + 纸黄字压照片暗部)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(PillShape)
                    .background(SequencePillBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#${index + 1} +${deltaSeconds}s",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    color = DecoPaperCream
                )
            }

            // Selection Indicator (Top-Right) in selection mode, else Best vermilion tag
            if (isSelectionMode) {
                SelectionCheckIndicator(
                    isSelected = isSelected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                )
            } else if (isBest) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(deco.decoVerm)
                        .border(0.75.dp, DecoBadgeBorder, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = DecoPaperCream,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "最佳",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DecoDisplayFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = DecoPaperCream
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "画质 ${photo.qualityScore}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = deco.decoInkOnGround.copy(alpha = if (isBest) 0.95f else 0.78f)
            )
            Text(
                text = formatStorageSize(photo.fileSizeBytes),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                color = deco.decoInkOnGround.copy(alpha = 0.55f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val bestActiveBrush = activeChromeSatinBrush()
        val bestIdleBrush = remember(deco) {
            Brush.verticalGradient(
                colors = listOf(
                    deco.decoGold.copy(alpha = 0.12f),
                    deco.decoGold.copy(alpha = 0.06f)
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(SmallButtonShape)
                    .background(if (isBest) bestActiveBrush else bestIdleBrush)
                    .border(
                        1.dp,
                        if (isBest) BestActiveBorder else deco.decoGold.copy(alpha = 0.50f),
                        SmallButtonShape
                    )
                    .clickable(onClick = onSelectBest)
            ) {
                Text(
                    text = if (isBest) "已选最佳" else "设为最佳",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DecoDisplayFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = if (isBest) deco.decoMat else deco.decoInkOnGround
                )
            }

            if (!isBest && !isSelectionMode) {
                Surface(
                    onClick = onTrash,
                    shape = SmallButtonShape,
                    color = deco.decoVerm.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, deco.decoVerm.copy(alpha = 0.45f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "移入回收站",
                            tint = deco.decoVerm,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 摩登画报 · 月头画报栏目线（deco-app.html 画廊屏月头）：
 * 衬线月名 20sp + 金色干支纪年副标 12sp + 弱金通栏细线 + 计数与动作跳签。
 * 功能等价迁移自 MonthlyClusterHeaderStamp：选本月 / 整理本月 / 已整理标记与 testTag 原样保留。
 */
@Composable
private fun DecoMonthClusterHeader(
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
                text = decoChineseMonth(cluster.month),
                fontFamily = DecoDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.6.sp,
                color = deco.decoInkOnGround
            )
            Text(
                text = "${decoGanZhi(cluster.year)}年",
                fontSize = 12.sp,
                fontFamily = DecoDisplayFontFamily,
                letterSpacing = 1.2.sp,
                color = deco.decoGold
            )
        }

        // 栏目细线
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(deco.decoGold.copy(alpha = 0.45f))
        )

        // 计数 + 动作跳签（选本月 / 整理·重温 / 已整理）
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
                    shape = PillShape,
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
                    shape = PillShape,
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
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.timeline_month_cleaned_tag),
                    tint = deco.decoGold,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
