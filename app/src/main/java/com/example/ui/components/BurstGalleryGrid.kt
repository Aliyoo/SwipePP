package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.GlassLightBottom
import com.example.ui.theme.GlassLightTop
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.ObsidianOverlayBorder
import com.example.ui.theme.ObsidianOverlayGlass
import com.example.ui.theme.ObsidianOverlayOrb
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure

// Hoisted static shapes & invariant overlay colors to guarantee zero per-tile allocations during high-FPS scrolling
private val TileShape = RoundedCornerShape(14.dp)
private val FrameCardShape = RoundedCornerShape(16.dp)
private val FrameImageShape = RoundedCornerShape(11.dp)
private val TrayShape = RoundedCornerShape(22.dp)
private val SmallButtonShape = RoundedCornerShape(10.dp)
private val PillShape = RoundedCornerShape(50)

private val BurstBottomScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xAD000000))
)
private val ObsidianPillBg = ObsidianOverlayGlass
private val ObsidianPillBorder = ObsidianOverlayBorder
private val SequencePillBg = ObsidianOverlayOrb
private val UnselectedCheckBg = Color(0x47000000)
private val UnselectedCheckBorder = Color(0xEBFFFFFF)
private val BestActiveBorder = Color(0x3DFFFFFF)

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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    // Density-calibrated thumbnail decode size capped tightly to keep memory & texture upload fast
    val gridThumbPx = remember(density, columns) {
        val approxTileDp = (360f / columns.coerceAtLeast(1))
        with(density) { (approxTileDp * 0.88f).dp.toPx().toInt() }
            .coerceIn(180, ImageDownsampleTier.GRID_THUMB.maxSizePx)
    }

    if (showMonthStampOverlay) {
        val clusterLayout = remember(entries, monthStamps) {
            buildTimelineClusterLayout(entries, monthStamps)
        }
        val monthPhotosMap = remember(entries, isSelectionMode) {
            if (!isSelectionMode) {
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

        Box(modifier = modifier) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            MonthlyClusterHeaderStamp(
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
        return
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
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
            .aspectRatio(0.92f)
            .then(selectionScaleModifier)
            .clip(TileShape)
            .background(SurfaceMuted)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) CobaltPrimary else HairlineBorder,
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
            downsampleTier = ImageDownsampleTier.GRID_THUMB,
            targetSizePx = thumbSizePx,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CobaltPrimary.copy(alpha = 0.18f))
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
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(EmeraldKeep),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已保留",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        if (photo.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(SolarAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "收藏",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionCheckIndicator(
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
                    .background(CobaltPrimary)
                    .border(1.8.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "已选中",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        partialCountText != null -> {
            Box(
                modifier = modifier
                    .clip(PillShape)
                    .background(CobaltPrimary)
                    .border(1.5.dp, Color.White, PillShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = partialCountText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    color = Color.White
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
    val backCardDeepFill = if (isDark) Color(0xFF1C2230) else Color(0xFFD6DAE3)
    val backCardMidFill = if (isDark) Color(0xFF242B3D) else Color(0xFFE4E7EE)
    val backCardShadowTint = if (isDark) Color(0x45000000) else Color(0x1612141B)
    val frontCardShadowTint = if (isDark) Color(0x58000000) else Color(0x1C12141B)
    val backCardRimBorder = if (isDark) Color(0xFF36405A) else Color(0xFFFDFCF9)
    val frontIdleBorder = if (isDark) Color(0xFF3F4A66) else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .then(selectionScaleModifier)
            .drawBehind {
                val corner = CornerRadius(13.dp.toPx(), 13.dp.toPx())
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
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("burst_collapsed_tile_${burst.groupId}")
    ) {
        // Front Top Card (Gallery Print Cover with Crisp Rim or Cobalt Selection Rim)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .clip(TileShape)
                .background(SurfacePure)
                .border(
                    width = if (anySelected) 2.5.dp else 1.8.dp,
                    color = if (anySelected) CobaltPrimary else frontIdleBorder,
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
                        .background(CobaltPrimary.copy(alpha = 0.18f))
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

            // Lightweight Obsidian Burst Indicator Badge (Top-Left, no Material Surface overhead)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(PillShape)
                    .background(ObsidianPillBg)
                    .border(0.85.dp, ObsidianPillBorder, PillShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
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
                    text = "${burst.photos.size}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }

            // Top-Right: Selection Check Badge in multi-select mode, else Best Shot Star Micro-Badge
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
                        .background(SolarAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "最佳连拍",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Bottom "Expand Burst" or "Select Burst" visual cue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSelectionMode) {
                        if (allSelected) "已选整组" else "点击选整组"
                    } else {
                        "连拍 · 展开"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!isSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.burst_expand),
                        tint = Color.White,
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
private fun ExpandedBurstStackTray(
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
        borderAccent = CobaltPrimary,
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
                            .background(CobaltSoft)
                            .padding(7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BurstMode,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "快速连拍组 · ${burst.photos.size} 张",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        Text(
                            text = headerSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Prominent Collapse Action Pill
                Surface(
                    onClick = onCollapse,
                    shape = PillShape,
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.testTag("burst_collapse_button_${burst.groupId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.UnfoldLess,
                            contentDescription = stringResource(R.string.burst_collapse),
                            tint = InkPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "收起",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = InkPrimary
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
                    leadingIcon = Icons.Filled.AutoAwesome,
                    tone = GalleryButtonTone.OBSIDIAN,
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
    val containerColor = when {
        isSelected -> CobaltSoft.copy(alpha = 0.78f)
        isBest -> CobaltSoft.copy(alpha = 0.55f)
        else -> SurfaceMuted.copy(alpha = 0.65f)
    }

    Column(
        modifier = Modifier
            .width(146.dp)
            .clip(FrameCardShape)
            .background(containerColor)
            .border(
                width = if (highlight) 2.2.dp else 1.dp,
                color = if (highlight) CobaltPrimary else HairlineBorder,
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
                        .background(CobaltPrimary.copy(alpha = 0.18f))
                )
            }

            // Sequence + Time Delta Pill (Top-Left)
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
                    fontSize = 9.sp,
                    color = Color.White
                )
            }

            // Selection Indicator (Top-Right) in selection mode, else Best Star Badge
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
                        .clip(PillShape)
                        .background(SolarAmber)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "最佳",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBest) CobaltDark else InkSecondary
            )
            Text(
                text = formatStorageSize(photo.fileSizeBytes),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = InkTertiary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val bestActiveBrush = activeChromeSatinBrush()
        val glassTop = GlassLightTop
        val glassBottom = GlassLightBottom
        val bestIdleBrush = remember(glassTop, glassBottom) {
            Brush.verticalGradient(colors = listOf(glassTop, glassBottom))
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
                        if (isBest) BestActiveBorder else HairlineBorder,
                        SmallButtonShape
                    )
                    .clickable(onClick = onSelectBest)
            ) {
                Text(
                    text = if (isBest) "已选最佳" else "设为最佳",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isBest) Color.White else InkPrimary
                )
            }

            if (!isBest && !isSelectionMode) {
                Surface(
                    onClick = onTrash,
                    shape = SmallButtonShape,
                    color = CoralSoft,
                    border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.28f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "移入回收站",
                            tint = CoralDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
