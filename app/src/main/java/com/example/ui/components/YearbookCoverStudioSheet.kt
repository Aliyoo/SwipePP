package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.YearbookCoverMode
import com.example.ui.YearbookCoverCurator
import com.example.ui.YearbookSummary
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.EditionCobalt
import com.example.ui.theme.EditionObsidian
import com.example.ui.theme.EditionSlatePlum
import com.example.ui.theme.EditionTeal
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import kotlinx.coroutines.delay

private enum class CoverPickerFilter(val label: String) {
    RECOMMENDED("智能推荐"),
    FAVORITES("我的收藏"),
    ALL_CHRONOLOGICAL("全部时间线")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearbookCoverStudioSheet(
    summary: YearbookSummary,
    yearPhotos: List<PhotoItemEntity>,
    onDismiss: () -> Unit,
    onSaveCover: (mode: YearbookCoverMode, selectedPhotoIds: List<Long>, intervalSeconds: Int) -> Unit,
    onResetToSmart: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val visiblePhotos = remember(yearPhotos) {
        yearPhotos.filter { it.photoStatus != PhotoStatus.TRASHED && it.photoStatus != PhotoStatus.PURGED }
    }
    val smartRankedPhotos = remember(visiblePhotos) {
        YearbookCoverCurator.rankPhotosForCoverPicker(visiblePhotos)
    }
    val smartCuratedSlides = remember(visiblePhotos) {
        YearbookCoverCurator.curateSmartCoverSlides(visiblePhotos)
    }
    val favoritePhotos = remember(smartRankedPhotos) {
        smartRankedPhotos.filter { it.isFavorite }
    }
    val photoById = remember(visiblePhotos) {
        visiblePhotos.associateBy { it.id }
    }

    var selectedMode by remember(summary.year) { mutableStateOf(summary.coverMode) }
    var intervalSeconds by remember(summary.year) { mutableIntStateOf(summary.coverIntervalSeconds) }
    var selectedIds by remember(summary.year) {
        mutableStateOf(
            summary.customCoverPhotoIds.filter { it in photoById }.ifEmpty {
                when (summary.coverMode) {
                    YearbookCoverMode.SINGLE_CUSTOM -> listOfNotNull(summary.coverPhoto?.id)
                    YearbookCoverMode.CUSTOM_CAROUSEL -> summary.coverSlides.map { it.id }.take(YearbookCoverCurator.MAX_COVER_SLIDES)
                    YearbookCoverMode.SMART_CAROUSEL -> emptyList()
                }
            }
        )
    }
    var pickerFilter by remember(summary.year) { mutableStateOf(CoverPickerFilter.RECOMMENDED) }

    val previewSlides = remember(selectedMode, selectedIds, smartCuratedSlides, photoById) {
        when (selectedMode) {
            YearbookCoverMode.SMART_CAROUSEL -> smartCuratedSlides
            YearbookCoverMode.SINGLE_CUSTOM -> {
                val chosen = selectedIds.firstOrNull()?.let { photoById[it] } ?: smartCuratedSlides.firstOrNull()
                listOfNotNull(chosen)
            }
            YearbookCoverMode.CUSTOM_CAROUSEL -> {
                val chosen = selectedIds.mapNotNull { photoById[it] }
                if (chosen.isNotEmpty()) chosen else smartCuratedSlides.take(3)
            }
        }
    }

    // Live miniature cover preview slideshow
    var previewIndex by remember(previewSlides) { mutableIntStateOf(0) }
    LaunchedEffect(previewSlides, intervalSeconds, selectedMode) {
        previewIndex = 0
        if (previewSlides.size <= 1) return@LaunchedEffect
        val previewStepMs = (intervalSeconds * 1000L).coerceAtMost(3500L)
        while (true) {
            delay(previewStepMs)
            previewIndex = (previewIndex + 1) % previewSlides.size
        }
    }

    val displayedGridPhotos = remember(pickerFilter, smartRankedPhotos, favoritePhotos, visiblePhotos) {
        when (pickerFilter) {
            CoverPickerFilter.RECOMMENDED -> smartRankedPhotos
            CoverPickerFilter.FAVORITES -> favoritePhotos
            CoverPickerFilter.ALL_CHRONOLOGICAL -> visiblePhotos
        }
    }

    val editionBaseColor = when (summary.editionIndex) {
        0 -> EditionObsidian
        1 -> EditionCobalt
        2 -> EditionTeal
        else -> EditionSlatePlum
    }

    val canSave = when (selectedMode) {
        YearbookCoverMode.SMART_CAROUSEL -> true
        YearbookCoverMode.SINGLE_CUSTOM -> selectedIds.isNotEmpty()
        YearbookCoverMode.CUSTOM_CAROUSEL -> selectedIds.isNotEmpty()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioCanvas,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(InkTertiary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
                .testTag("yearbook_cover_studio_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) Top Hero Row: Live Miniature 3D Book Preview + Mode Context
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniBookCoverPreview(
                    year = summary.year,
                    editionColor = editionBaseColor,
                    slides = previewSlides,
                    currentIndex = previewIndex,
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(0.70f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${stringResource(R.string.cover_studio_title)} · ${summary.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        text = when (selectedMode) {
                            YearbookCoverMode.SMART_CAROUSEL -> stringResource(R.string.cover_mode_smart_desc)
                            YearbookCoverMode.SINGLE_CUSTOM -> stringResource(R.string.cover_mode_single_desc)
                            YearbookCoverMode.CUSTOM_CAROUSEL -> stringResource(R.string.cover_mode_custom_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        lineHeight = 18.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CobaltSoft,
                        border = BorderStroke(0.8.dp, CobaltPrimary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (previewSlides.size > 1) Icons.Filled.PlayArrow else Icons.Filled.Image,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = when (selectedMode) {
                                    YearbookCoverMode.SMART_CAROUSEL ->
                                        "智能优选 ${previewSlides.size} 张 · 每${intervalSeconds}秒轮播"
                                    YearbookCoverMode.SINGLE_CUSTOM ->
                                        if (selectedIds.isNotEmpty()) "已锁定 1 张专属静态封面" else "请从下方点选 1 张封面"
                                    YearbookCoverMode.CUSTOM_CAROUSEL ->
                                        "已选 ${selectedIds.size}/${YearbookCoverCurator.MAX_COVER_SLIDES} 张 · 每${intervalSeconds}秒轮播"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CobaltPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2) Mode Selector Segmented Pill
            FrostedGlassCard(
                shape = RoundedCornerShape(50),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CoverModeSegment(
                        mode = YearbookCoverMode.SMART_CAROUSEL,
                        icon = Icons.Filled.AutoAwesome,
                        selected = selectedMode == YearbookCoverMode.SMART_CAROUSEL,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedMode = YearbookCoverMode.SMART_CAROUSEL
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cover_mode_smart")
                    )
                    CoverModeSegment(
                        mode = YearbookCoverMode.SINGLE_CUSTOM,
                        icon = Icons.Filled.Image,
                        selected = selectedMode == YearbookCoverMode.SINGLE_CUSTOM,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedMode = YearbookCoverMode.SINGLE_CUSTOM
                            if (selectedIds.isEmpty()) {
                                smartCuratedSlides.firstOrNull()?.let { selectedIds = listOf(it.id) }
                            } else if (selectedIds.size > 1) {
                                selectedIds = listOf(selectedIds.first())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cover_mode_single")
                    )
                    CoverModeSegment(
                        mode = YearbookCoverMode.CUSTOM_CAROUSEL,
                        icon = Icons.Filled.Collections,
                        selected = selectedMode == YearbookCoverMode.CUSTOM_CAROUSEL,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedMode = YearbookCoverMode.CUSTOM_CAROUSEL
                            if (selectedIds.size < 2) {
                                val seed = (selectedIds + smartCuratedSlides.map { it.id })
                                    .distinct()
                                    .take(3)
                                selectedIds = seed
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cover_mode_custom_carousel")
                    )
                }
            }

            // 3) Carousel Cadence Selector (visible in Smart & Custom Carousel modes)
            AnimatedVisibility(visible = selectedMode != YearbookCoverMode.SINGLE_CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cover_speed_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = InkSecondary
                    )
                    listOf(
                        3 to stringResource(R.string.cover_speed_fast),
                        5 to stringResource(R.string.cover_speed_balanced),
                        8 to stringResource(R.string.cover_speed_calm)
                    ).forEach { (sec, label) ->
                        GallerySelectableChip(
                            selected = intervalSeconds == sec,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                intervalSeconds = sec
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("cover_speed_${sec}s")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.sp,
                                color = if (intervalSeconds == sec) Color.White else InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4) Smart Curated Strip (in SMART_CAROUSEL) OR Interactive Photo Grid Picker (in Custom modes)
            if (selectedMode == YearbookCoverMode.SMART_CAROUSEL) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "本年度智能精选序列 (${smartCuratedSlides.size} 张)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        Text(
                            text = "点任一照片可定格",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkTertiary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = smartCuratedSlides,
                            key = { _, item -> item.id }
                        ) { index, photo ->
                            val isActiveSlide = index == (previewIndex % smartCuratedSlides.size.coerceAtLeast(1))
                            SmartSlideCard(
                                photo = photo,
                                orderNumber = index + 1,
                                isHighlighted = isActiveSlide,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedMode = YearbookCoverMode.SINGLE_CUSTOM
                                    selectedIds = listOf(photo.id)
                                }
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverPickerFilter.entries.forEach { filter ->
                            val count = when (filter) {
                                CoverPickerFilter.RECOMMENDED -> smartRankedPhotos.size
                                CoverPickerFilter.FAVORITES -> favoritePhotos.size
                                CoverPickerFilter.ALL_CHRONOLOGICAL -> visiblePhotos.size
                            }
                            GallerySelectableChip(
                                selected = pickerFilter == filter,
                                onClick = { pickerFilter = filter },
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("cover_filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = "${filter.label}($count)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 11.sp,
                                    color = if (pickerFilter == filter) Color.White else InkPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (displayedGridPhotos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "当前分类下暂无可选照片",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 210.dp, max = 320.dp)
                                .testTag("cover_picker_grid")
                        ) {
                            items(
                                items = displayedGridPhotos,
                                key = { it.id }
                            ) { photo ->
                                val orderIdx = selectedIds.indexOf(photo.id)
                                val isSelected = orderIdx >= 0
                                CoverPickerGridTile(
                                    photo = photo,
                                    isSelected = isSelected,
                                    selectionBadge = if (selectedMode == YearbookCoverMode.CUSTOM_CAROUSEL && isSelected) {
                                        (orderIdx + 1).toString()
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (selectedMode == YearbookCoverMode.SINGLE_CUSTOM) {
                                            selectedIds = listOf(photo.id)
                                        } else {
                                            selectedIds = if (isSelected) {
                                                if (selectedIds.size > 1) selectedIds - photo.id else selectedIds
                                            } else if (selectedIds.size < YearbookCoverCurator.MAX_COVER_SLIDES) {
                                                selectedIds + photo.id
                                            } else {
                                                // Replace oldest selection smoothly so tapping never feels dead
                                                selectedIds.drop(1) + photo.id
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5) Bottom Sticky Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (summary.coverMode != YearbookCoverMode.SMART_CAROUSEL || selectedMode != YearbookCoverMode.SMART_CAROUSEL) {
                    GalleryActionButton(
                        text = stringResource(R.string.cover_studio_reset_smart),
                        leadingIcon = Icons.Filled.RestartAlt,
                        tone = GalleryButtonTone.GLASS,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onResetToSmart()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(0.95f)
                            .testTag("cover_reset_smart_button")
                    )
                }

                GalleryActionButton(
                    text = stringResource(R.string.cover_studio_save),
                    leadingIcon = Icons.Filled.Check,
                    tone = GalleryButtonTone.OBSIDIAN,
                    enabled = canSave,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val effectiveMode = when {
                            selectedMode == YearbookCoverMode.CUSTOM_CAROUSEL && selectedIds.size <= 1 ->
                                YearbookCoverMode.SINGLE_CUSTOM
                            else -> selectedMode
                        }
                        val idsToSave = if (effectiveMode == YearbookCoverMode.SMART_CAROUSEL) {
                            emptyList()
                        } else {
                            selectedIds.take(YearbookCoverCurator.MAX_COVER_SLIDES)
                        }
                        onSaveCover(effectiveMode, idsToSave, intervalSeconds)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("cover_save_button")
                )
            }
        }
    }
}

@Composable
private fun CoverModeSegment(
    mode: YearbookCoverMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFlipYearPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) palette.activeChromeMid else Color.Transparent,
        border = if (selected) BorderStroke(0.9.dp, palette.activeChromeBorderStart) else null,
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) SolarAmber else InkSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniBookCoverPreview(
    year: Int,
    editionColor: Color,
    slides: List<PhotoItemEntity>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val bookShape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 14.dp, bottomEnd = 14.dp)
    val activePhoto = slides.getOrNull(currentIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0)))

    val miniShadowColor = if (LocalIsDarkTheme.current) Color.Black.copy(alpha = 0.52f) else InkPrimary.copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .shadow(12.dp, bookShape, spotColor = miniShadowColor)
            .clip(bookShape)
            .background(editionColor)
    ) {
        Crossfade(
            targetState = activePhoto,
            animationSpec = tween(durationMillis = 650),
            label = "mini_cover_preview",
            modifier = Modifier.fillMaxSize()
        ) { photo ->
            if (photo != null) {
                PhotoArtwork(
                    photo = photo,
                    contentDescription = null,
                    showVideoBadge = false,
                    downsampleTier = ImageDownsampleTier.PREVIEW_CARD,
                    targetSizePx = 384,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to editionColor.copy(alpha = 0.62f),
                        0.45f to Color.Transparent,
                        1.0f to editionColor.copy(alpha = 0.78f)
                    )
                )
        )

        // Left spine shadow
        Box(
            modifier = Modifier
                .width(9.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Mini slide indicators at top
        if (slides.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                slides.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == currentIndex) Color.White else Color.White.copy(alpha = 0.34f)
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 13.dp, top = if (slides.size > 1) 14.dp else 10.dp)
        ) {
            Text(
                text = "ED // ${year % 100}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 6.5.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = year.toString(),
                fontFamily = EditorialSerifFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                color = Color.White
            )
        }

        if (activePhoto != null) {
            Text(
                text = "${activePhoto.month}月",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = SolarAmber,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 13.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun SmartSlideCard(
    photo: PhotoItemEntity,
    orderNumber: Int,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val reasonTag = remember(photo) {
        when {
            photo.isFavorite -> "收藏佳作"
            !photo.locationName.isNullOrBlank() -> photo.locationName
            photo.isBestInStack -> "连拍优选"
            photo.qualityScore >= 90 -> "高画质"
            else -> "${photo.month}月精选"
        }
    }

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfacePure)
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = if (isHighlighted) CobaltPrimary else HairlineBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(6.dp)
            .testTag("smart_slide_card_${photo.id}"),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(RoundedCornerShape(11.dp))
        ) {
            PhotoArtwork(
                photo = photo,
                contentDescription = photo.caption,
                downsampleTier = ImageDownsampleTier.GRID_THUMB,
                targetSizePx = 320,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                shape = CircleShape,
                color = if (isHighlighted) CobaltPrimary else Color(0xCC12141C),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = orderNumber.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (photo.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(14.dp)
                )
            }
        }
        Text(
            text = "${photo.month}月 · $reasonTag",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighlighted) CobaltPrimary else InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun CoverPickerGridTile(
    photo: PhotoItemEntity,
    isSelected: Boolean,
    selectionBadge: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.5.dp else 0.8.dp,
                color = if (isSelected) CobaltPrimary else HairlineBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("cover_picker_tile_${photo.id}")
    ) {
        PhotoArtwork(
            photo = photo,
            contentDescription = photo.caption,
            downsampleTier = ImageDownsampleTier.GRID_THUMB,
            targetSizePx = 320,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CobaltPrimary.copy(alpha = 0.16f))
            )
        }

        // Top-right selection badge (numbered for multi-carousel, checkmark for single)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) CobaltPrimary else Color(0x7A10131A))
                .border(1.2.dp, Color.White.copy(alpha = if (isSelected) 0.95f else 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                if (selectionBadge != null) {
                    Text(
                        text = selectionBadge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Bottom subtle date/favorite strip
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xB80A0D14))
                    )
                )
                .padding(horizontal = 7.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${photo.month}月${photo.day}日",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.5.sp,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1
            )
            if (photo.isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(11.dp)
                )
            } else if (photo.photoStatus == PhotoStatus.KEPT) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = EmeraldKeep,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
