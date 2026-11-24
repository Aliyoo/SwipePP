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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CollageBackdropStyle
import com.example.data.CollageExportResult
import com.example.data.CollageSlotTransform
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.YearbookCoverMode
import com.example.ui.YearbookCoverCurator
import com.example.ui.YearbookSummary
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.DecoGround
import com.example.ui.theme.YearbookCoverStyle
import com.example.ui.theme.isReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    defaultCoverStyle: YearbookCoverStyle = YearbookCoverStyle.COLLAGE,
    onDismiss: () -> Unit,
    onSaveCover: (
        mode: YearbookCoverMode,
        selectedPhotoIds: List<Long>,
        intervalSeconds: Int,
        collageTemplateId: String,
        collageSpacingDp: Float,
        collageBackdrop: CollageBackdropStyle,
        slotTransforms: List<CollageSlotTransform>
    ) -> Unit,
    onResetToSmart: () -> Unit,
    onExportCollage: suspend (
        photos: List<PhotoItemEntity>,
        template: CollageTemplate,
        spacingDp: Float,
        backdrop: CollageBackdropStyle,
        editionColorArgb: Int,
        slotTransforms: List<CollageSlotTransform>,
        includeYearWatermark: Boolean
    ) -> CollageExportResult
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

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

    val smartDefaultTemplate = remember(summary.year, smartCuratedSlides.size) {
        YearbookCollageTemplates.defaultTemplateFor(summary.year, smartCuratedSlides.size)
    }

    var selectedMode by remember(summary.year) { mutableStateOf(summary.coverMode) }
    var intervalSeconds by remember(summary.year) { mutableIntStateOf(summary.coverIntervalSeconds) }
    var selectedIds by remember(summary.year) {
        mutableStateOf(
            summary.customCoverPhotoIds.filter { it in photoById }.ifEmpty {
                when (summary.coverMode) {
                    YearbookCoverMode.SINGLE_CUSTOM -> listOfNotNull(summary.coverPhoto?.id)
                    YearbookCoverMode.CUSTOM_CAROUSEL,
                    YearbookCoverMode.CUSTOM_COLLAGE -> summary.coverSlides.map { it.id }.take(YearbookCoverCurator.MAX_COVER_SLIDES)
                    YearbookCoverMode.SMART_CAROUSEL -> emptyList()
                }
            }
        )
    }
    var selectedTemplateId by remember(summary.year) {
        mutableStateOf(summary.collageTemplateId.ifBlank { smartDefaultTemplate.id })
    }
    var selectedTemplateCategory by remember(summary.year) { mutableStateOf<CollageStyleCategory?>(null) }
    var collageSpacingDp by remember(summary.year) { mutableFloatStateOf(summary.collageSpacingDp) }
    var collageBackdrop by remember(summary.year) { mutableStateOf(summary.collageBackdrop) }
    var slotTransforms by remember(summary.year) { mutableStateOf(summary.collageSlotTransforms) }
    var activeSlotIndex by remember(summary.year) { mutableStateOf<Int?>(null) }
    var includeYearWatermark by remember(summary.year) { mutableStateOf(true) }
    var isExporting by remember(summary.year) { mutableStateOf(false) }
    var exportStatusNote by remember(summary.year) { mutableStateOf<String?>(null) }
    var pickerFilter by remember(summary.year) { mutableStateOf(CoverPickerFilter.RECOMMENDED) }

    val activeCollageTemplate = remember(selectedTemplateId, summary.year, visiblePhotos.size) {
        YearbookCollageTemplates.resolveTemplate(
            templateId = selectedTemplateId,
            year = summary.year,
            availablePhotoCount = visiblePhotos.size.coerceAtLeast(2)
        )
    }

    // Ensure collage slots always have valid photos up to activeCollageTemplate.slotCount
    fun ensureCollageSelectionCount(current: List<Long>, targetCount: Int): List<Long> {
        val valid = current.filter { it in photoById }.distinct().toMutableList()
        if (valid.size < targetCount) {
            for (candidate in smartRankedPhotos) {
                if (valid.size >= targetCount) break
                if (candidate.id !in valid) valid.add(candidate.id)
            }
        }
        return valid.take(YearbookCoverCurator.MAX_COVER_SLIDES)
    }

    val activeCollagePhotos = remember(selectedIds, activeCollageTemplate, smartRankedPhotos, photoById) {
        val filledIds = ensureCollageSelectionCount(selectedIds, activeCollageTemplate.slotCount)
        filledIds.mapNotNull { photoById[it] }.take(activeCollageTemplate.slotCount)
    }

    val previewSlides = remember(selectedMode, selectedIds, smartCuratedSlides, photoById) {
        when (selectedMode) {
            YearbookCoverMode.SMART_CAROUSEL -> smartCuratedSlides
            YearbookCoverMode.CUSTOM_COLLAGE -> {
                val chosen = selectedIds.mapNotNull { photoById[it] }
                if (chosen.isNotEmpty()) chosen else smartCuratedSlides
            }
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

    val smartUsesCollage = defaultCoverStyle == YearbookCoverStyle.COLLAGE && smartCuratedSlides.size >= 2

    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }

    // Live miniature cover preview slideshow (carousel modes only, paused when reduced motion is enabled)
    var previewIndex by remember(previewSlides) { mutableIntStateOf(0) }
    val shouldAnimateMiniCarousel = (selectedMode == YearbookCoverMode.CUSTOM_CAROUSEL ||
        (selectedMode == YearbookCoverMode.SMART_CAROUSEL && !smartUsesCollage)) &&
        previewSlides.size > 1 && !reducedMotion

    LaunchedEffect(previewSlides, intervalSeconds, shouldAnimateMiniCarousel) {
        previewIndex = 0
        if (!shouldAnimateMiniCarousel) return@LaunchedEffect
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

    // deco 无版本色体系：拼块衬底与预览书封统一用画框衬纸 mat
    val editionBaseColor = DecoMat

    val canSave = when (selectedMode) {
        YearbookCoverMode.SMART_CAROUSEL -> true
        YearbookCoverMode.CUSTOM_COLLAGE -> activeCollagePhotos.size >= 2
        YearbookCoverMode.SINGLE_CUSTOM -> selectedIds.isNotEmpty()
        YearbookCoverMode.CUSTOM_CAROUSEL -> selectedIds.isNotEmpty()
    }

    fun triggerHdExport(
        photos: List<PhotoItemEntity>,
        template: CollageTemplate,
        spacing: Float,
        backdrop: CollageBackdropStyle,
        transforms: List<CollageSlotTransform>
    ) {
        if (isExporting || photos.isEmpty()) return
        scope.launch {
            isExporting = true
            exportStatusNote = null
            val res = onExportCollage(
                photos,
                template,
                spacing,
                backdrop,
                editionBaseColor.toArgb(),
                transforms,
                includeYearWatermark
            )
            isExporting = false
            exportStatusNote = res.message
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DecoGround,
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
        dragHandle = { DecoSheetDragHandle() },
        modifier = Modifier.modalSheetTopClearance()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("yearbook_cover_studio_sheet")
        ) {
            // 1) Pinned Header + 4-Mode Selector Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 2.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.cover_studio_title)} · ${summary.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DecoInkOnGround
                    )
                    Surface(
                        shape = CircleShape,
                        color = DecoMat,
                        border = BorderStroke(0.7.dp, DecoGold.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = "全局：${defaultCoverStyle.shortLabel}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DecoGold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = DecoInkOnGround.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CoverModeSegment(
                            label = "默认",
                            iconKind = DecoIconKind.SPARKLE,
                            selected = selectedMode == YearbookCoverMode.SMART_CAROUSEL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = YearbookCoverMode.SMART_CAROUSEL
                                activeSlotIndex = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cover_mode_smart")
                        )
                        CoverModeSegment(
                            label = "拼图",
                            iconKind = DecoIconKind.DASHBOARD,
                            selected = selectedMode == YearbookCoverMode.CUSTOM_COLLAGE,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = YearbookCoverMode.CUSTOM_COLLAGE
                                val tmpl = YearbookCollageTemplates.resolveTemplate(
                                    templateId = selectedTemplateId,
                                    year = summary.year,
                                    availablePhotoCount = visiblePhotos.size.coerceAtLeast(2)
                                )
                                selectedTemplateId = tmpl.id
                                selectedIds = ensureCollageSelectionCount(selectedIds, tmpl.slotCount)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cover_mode_collage")
                        )
                        CoverModeSegment(
                            label = "单图",
                            iconKind = DecoIconKind.PHOTO,
                            selected = selectedMode == YearbookCoverMode.SINGLE_CUSTOM,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = YearbookCoverMode.SINGLE_CUSTOM
                                activeSlotIndex = null
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
                            label = "轮播",
                            iconKind = DecoIconKind.PHOTO_LIBRARY,
                            selected = selectedMode == YearbookCoverMode.CUSTOM_CAROUSEL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = YearbookCoverMode.CUSTOM_CAROUSEL
                                activeSlotIndex = null
                                if (selectedIds.size < 2) {
                                    selectedIds = (selectedIds + smartCuratedSlides.map { it.id })
                                        .distinct()
                                        .take(3)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cover_mode_custom_carousel")
                        )
                    }
                }
            }

            if (scrollState.canScrollBackward) {
                HorizontalDivider(color = DecoGold.copy(alpha = 0.45f), thickness = 0.8.dp)
            }

            // 2) Scrollable Middle Studio Workbench
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Inline export feedback banner if user just exported HD image
                AnimatedVisibility(visible = exportStatusNote != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DecoGold.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, DecoGold.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            DecoIcon(
    kind = DecoIconKind.CHECK,
    color = DecoGold,
    iconSize = 15.dp,
    modifier = Modifier
                            )
                            Text(
                                text = exportStatusNote.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = DecoInkOnGround,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (selectedMode == YearbookCoverMode.CUSTOM_COLLAGE) {
                    // Full Interactive Irregular Collage Workbench
                    YearbookCollageEditorPanel(
                        year = summary.year,
                        editionColor = editionBaseColor,
                        photos = activeCollagePhotos,
                        template = activeCollageTemplate,
                        selectedCategory = selectedTemplateCategory,
                        spacingDp = collageSpacingDp,
                        backdrop = collageBackdrop,
                        slotTransforms = slotTransforms,
                        activeSlotIndex = activeSlotIndex,
                        includeYearWatermark = includeYearWatermark,
                        isExporting = isExporting,
                        onSelectCategory = { cat ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTemplateCategory = cat
                        },
                        onSelectTemplate = { tmpl ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTemplateId = tmpl.id
                            collageSpacingDp = tmpl.defaultSpacingDp
                            selectedIds = ensureCollageSelectionCount(selectedIds, tmpl.slotCount)
                            if (activeSlotIndex != null && activeSlotIndex!! >= tmpl.slotCount) {
                                activeSlotIndex = null
                            }
                            slotTransforms = List(tmpl.slotCount) { idx ->
                                slotTransforms.getOrNull(idx) ?: CollageSlotTransform()
                            }
                        },
                        onSelectSpacing = { spacing ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            collageSpacingDp = spacing
                        },
                        onSelectBackdrop = { bg ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            collageBackdrop = bg
                        },
                        onSlotTap = { tappedSlot ->
                            val currentActive = activeSlotIndex
                            if (currentActive == null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeSlotIndex = tappedSlot
                            } else if (currentActive == tappedSlot) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeSlotIndex = null
                            } else {
                                // Swap photos & framing transforms between currentActive and tappedSlot
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val padded = ensureCollageSelectionCount(selectedIds, activeCollageTemplate.slotCount).toMutableList()
                                val paddedTransforms = MutableList(activeCollageTemplate.slotCount) { idx ->
                                    slotTransforms.getOrNull(idx) ?: CollageSlotTransform()
                                }
                                if (currentActive in padded.indices && tappedSlot in padded.indices) {
                                    val tmpId = padded[currentActive]
                                    padded[currentActive] = padded[tappedSlot]
                                    padded[tappedSlot] = tmpId

                                    val tmpTransform = paddedTransforms[currentActive]
                                    paddedTransforms[currentActive] = paddedTransforms[tappedSlot]
                                    paddedTransforms[tappedSlot] = tmpTransform

                                    selectedIds = padded
                                    slotTransforms = paddedTransforms
                                }
                                activeSlotIndex = null
                            }
                        },
                        onClearActiveSlot = { activeSlotIndex = null },
                        onTransformSlot = { slotIdx, updated ->
                            val mutable = MutableList(activeCollageTemplate.slotCount) { idx ->
                                slotTransforms.getOrNull(idx) ?: CollageSlotTransform()
                            }
                            if (slotIdx in mutable.indices) {
                                mutable[slotIdx] = updated
                                slotTransforms = mutable
                            }
                        },
                        onShuffleLayout = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val candidates = YearbookCollageTemplates.all.filter {
                                (selectedTemplateCategory == null || it.category == selectedTemplateCategory) &&
                                    it.slotCount <= visiblePhotos.size.coerceAtLeast(2)
                            }.ifEmpty { YearbookCollageTemplates.all }
                            val currentIdx = candidates.indexOfFirst { it.id == activeCollageTemplate.id }
                            val nextTemplate = candidates[(currentIdx + 1) % candidates.size]
                            selectedTemplateId = nextTemplate.id
                            collageSpacingDp = nextTemplate.defaultSpacingDp
                            val padded = ensureCollageSelectionCount(selectedIds, nextTemplate.slotCount)
                            selectedIds = if (padded.size > 1) padded.drop(1) + padded.first() else padded
                            slotTransforms = emptyList()
                            activeSlotIndex = null
                        },
                        onResetTransforms = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            slotTransforms = emptyList()
                            activeSlotIndex = null
                        },
                        onToggleYearWatermark = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            includeYearWatermark = !includeYearWatermark
                        },
                        onExportHdCollage = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            triggerHdExport(
                                photos = activeCollagePhotos,
                                template = activeCollageTemplate,
                                spacing = collageSpacingDp,
                                backdrop = collageBackdrop,
                                transforms = slotTransforms
                            )
                        }
                    )
                } else {
                    // Live Preview & Mode Context for Smart Default / Single / Carousel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedMode == YearbookCoverMode.SMART_CAROUSEL && smartUsesCollage) {
                            MiniCollageCoverPreview(
                                year = summary.year,
                                editionColor = editionBaseColor,
                                photos = smartCuratedSlides,
                                template = smartDefaultTemplate,
                                modifier = Modifier
                                    .width(92.dp)
                                    .aspectRatio(0.70f)
                            )
                        } else {
                            MiniBookCoverPreview(
                                year = summary.year,
                                editionColor = editionBaseColor,
                                slides = previewSlides,
                                currentIndex = previewIndex,
                                modifier = Modifier
                                    .width(86.dp)
                                    .aspectRatio(0.70f)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = when (selectedMode) {
                                    YearbookCoverMode.SMART_CAROUSEL -> stringResource(R.string.cover_mode_smart_desc)
                                    YearbookCoverMode.SINGLE_CUSTOM -> stringResource(R.string.cover_mode_single_desc)
                                    YearbookCoverMode.CUSTOM_CAROUSEL -> stringResource(R.string.cover_mode_custom_desc)
                                    YearbookCoverMode.CUSTOM_COLLAGE -> stringResource(R.string.cover_mode_collage_desc)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = DecoInkOnGround.copy(alpha = 0.72f),
                                lineHeight = 18.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = DecoMat,
                                border = BorderStroke(0.8.dp, DecoGold.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    DecoIcon(
                                        kind = when {
                                            selectedMode == YearbookCoverMode.SMART_CAROUSEL && smartUsesCollage ->
                                                DecoIconKind.DASHBOARD
                                            previewSlides.size > 1 -> DecoIconKind.PLAY
                                            else -> DecoIconKind.PHOTO
                                        },
                                        color = DecoGold,
                                        iconSize = 13.dp
                                    )
                                    Text(
                                        text = when (selectedMode) {
                                            YearbookCoverMode.SMART_CAROUSEL -> if (smartUsesCollage) {
                                                "智能拼图 · ${smartDefaultTemplate.title} (${smartDefaultTemplate.slotCount}图)"
                                            } else {
                                                "智能优选 ${previewSlides.size} 张 · 每${intervalSeconds}秒轮播"
                                            }
                                            YearbookCoverMode.SINGLE_CUSTOM ->
                                                if (selectedIds.isNotEmpty()) "已锁定 1 张静态封面" else "请从下方点选 1 张封面"
                                            YearbookCoverMode.CUSTOM_CAROUSEL ->
                                                "已选 ${selectedIds.size}/${YearbookCoverCurator.MAX_COVER_SLIDES} 张 · 每${intervalSeconds}秒轮播"
                                            YearbookCoverMode.CUSTOM_COLLAGE -> ""
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DecoGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Quick Collage Actions when SMART_CAROUSEL previews Smart Collage
                            if (selectedMode == YearbookCoverMode.SMART_CAROUSEL && smartUsesCollage) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedTemplateId = smartDefaultTemplate.id
                                            collageSpacingDp = smartDefaultTemplate.defaultSpacingDp
                                            collageBackdrop = CollageBackdropStyle.EDITION
                                            selectedIds = smartCuratedSlides.map { it.id }.take(smartDefaultTemplate.slotCount)
                                            slotTransforms = emptyList()
                                            selectedMode = YearbookCoverMode.CUSTOM_COLLAGE
                                        },
                                        shape = RoundedCornerShape(3.dp),
                                        color = DecoMat,
                                        border = BorderStroke(0.85.dp, DecoGold.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .testTag("smart_collage_customize_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            DecoIcon(
    kind = DecoIconKind.EDIT,
    color = DecoInkOnGround,
    iconSize = 13.dp,
    modifier = Modifier
                                            )
                                            Text(
                                                text = "微调拼图",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DecoInkOnGround
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            if (!isExporting) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                triggerHdExport(
                                                    photos = smartCuratedSlides.take(smartDefaultTemplate.slotCount),
                                                    template = smartDefaultTemplate,
                                                    spacing = smartDefaultTemplate.defaultSpacingDp,
                                                    backdrop = CollageBackdropStyle.EDITION,
                                                    transforms = emptyList()
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(3.dp),
                                        color = DecoGold,
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .testTag("smart_collage_export_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isExporting) {
                                                CircularProgressIndicator(
                                                    color = DecoInkOnGround,
                                                    strokeWidth = 1.8.dp,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            } else {
                                                DecoIcon(
    kind = DecoIconKind.ARROW_DOWN,
    color = DecoInkOnGround,
    iconSize = 13.dp,
    modifier = Modifier
                                                )
                                            }
                                            Text(
                                                text = if (isExporting) "导出中…" else "导出高清图",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DecoInkOnGround
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Carousel Cadence ONLY when effective preview is a carousel
                    val showCadence = selectedMode == YearbookCoverMode.CUSTOM_CAROUSEL ||
                        (selectedMode == YearbookCoverMode.SMART_CAROUSEL && !smartUsesCollage)
                    AnimatedVisibility(visible = showCadence) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.cover_speed_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = DecoInkOnGround.copy(alpha = 0.72f)
                            )
                            listOf(
                                3 to stringResource(R.string.cover_speed_fast),
                                5 to stringResource(R.string.cover_speed_balanced),
                                8 to stringResource(R.string.cover_speed_calm)
                            ).forEach { (sec, label) ->
                                CoverStudioChip(
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
                                        color = if (intervalSeconds == sec) DecoInkOnGround else DecoInkOnGround,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 3) Smart Curated Strip (in SMART_CAROUSEL) OR Interactive Photo Grid Picker (in Custom modes)
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
                                color = DecoInkOnGround
                            )
                            Text(
                                text = "点任一照片可定格",
                                style = MaterialTheme.typography.labelSmall,
                                color = DecoInkOnGround.copy(alpha = 0.55f)
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
                                val isActiveSlide = !smartUsesCollage &&
                                    index == (previewIndex % smartCuratedSlides.size.coerceAtLeast(1))
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
                                CoverStudioChip(
                                    selected = pickerFilter == filter,
                                    onClick = { pickerFilter = filter },
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .testTag("cover_filter_${filter.name.lowercase()}")
                                ) {
                                    Text(
                                        text = "${filter.label}($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 11.sp,
                                        color = if (pickerFilter == filter) DecoInkOnGround else DecoInkOnGround,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (displayedGridPhotos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(DecoMat),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "当前分类下暂无可选照片",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DecoInkOnGround.copy(alpha = 0.72f)
                                )
                            }
                        } else {
                            val activeSlotIds = if (selectedMode == YearbookCoverMode.CUSTOM_COLLAGE) {
                                activeCollagePhotos.map { it.id }
                            } else {
                                selectedIds
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .testTag("cover_picker_grid")
                            ) {
                                items(
                                    items = displayedGridPhotos,
                                    key = { it.id }
                                ) { photo ->
                                    val orderIdx = activeSlotIds.indexOf(photo.id)
                                    val isSelected = orderIdx >= 0
                                    CoverPickerGridTile(
                                        photo = photo,
                                        isSelected = isSelected,
                                        selectionBadge = if (selectedMode != YearbookCoverMode.SINGLE_CUSTOM && isSelected) {
                                            (orderIdx + 1).toString()
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            when (selectedMode) {
                                                YearbookCoverMode.SINGLE_CUSTOM -> {
                                                    selectedIds = listOf(photo.id)
                                                }
                                                YearbookCoverMode.CUSTOM_CAROUSEL -> {
                                                    selectedIds = if (isSelected) {
                                                        if (selectedIds.size > 1) selectedIds - photo.id else selectedIds
                                                    } else if (selectedIds.size < YearbookCoverCurator.MAX_COVER_SLIDES) {
                                                        selectedIds + photo.id
                                                    } else {
                                                        selectedIds.drop(1) + photo.id
                                                    }
                                                }
                                                YearbookCoverMode.CUSTOM_COLLAGE -> {
                                                    val slotCount = activeCollageTemplate.slotCount
                                                    val padded = ensureCollageSelectionCount(selectedIds, slotCount).toMutableList()
                                                    val paddedTransforms = MutableList(slotCount) { idx ->
                                                        slotTransforms.getOrNull(idx) ?: CollageSlotTransform()
                                                    }
                                                    val targetSlot = activeSlotIndex
                                                    if (targetSlot != null && targetSlot in padded.indices) {
                                                        val existingIdx = padded.indexOf(photo.id)
                                                        if (existingIdx >= 0 && existingIdx != targetSlot) {
                                                            // Swap with existing slot
                                                            val tmpId = padded[targetSlot]
                                                            padded[targetSlot] = padded[existingIdx]
                                                            padded[existingIdx] = tmpId
                                                            val tmpTf = paddedTransforms[targetSlot]
                                                            paddedTransforms[targetSlot] = paddedTransforms[existingIdx]
                                                            paddedTransforms[existingIdx] = tmpTf
                                                        } else {
                                                            padded[targetSlot] = photo.id
                                                            paddedTransforms[targetSlot] = CollageSlotTransform()
                                                        }
                                                        selectedIds = padded
                                                        slotTransforms = paddedTransforms
                                                        activeSlotIndex = null
                                                    } else {
                                                        // No slot highlighted: replace last slot smoothly or remove if > 2
                                                        val existingIdx = padded.indexOf(photo.id)
                                                        if (existingIdx < 0) {
                                                            val replaceIdx = (slotCount - 1).coerceIn(0, padded.lastIndex)
                                                            padded[replaceIdx] = photo.id
                                                            paddedTransforms[replaceIdx] = CollageSlotTransform()
                                                            selectedIds = padded
                                                            slotTransforms = paddedTransforms
                                                        } else {
                                                            // Clicking an already-placed photo highlights its slot for quick fine-tuning
                                                            activeSlotIndex = existingIdx
                                                        }
                                                    }
                                                }
                                                YearbookCoverMode.SMART_CAROUSEL -> Unit
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4) Pinned Bottom Action Row
            HorizontalDivider(color = DecoGold.copy(alpha = 0.7f), thickness = 0.75.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (summary.coverMode != YearbookCoverMode.SMART_CAROUSEL || selectedMode != YearbookCoverMode.SMART_CAROUSEL) {
                    GalleryActionButton(
                        text = stringResource(R.string.cover_studio_reset_smart),
                        leadingIconKind = DecoIconKind.RESTART,
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
                    leadingIconKind = DecoIconKind.CHECK,
                    tone = GalleryButtonTone.IMMERSIVE,
                    enabled = canSave,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val effectiveMode = when {
                            selectedMode == YearbookCoverMode.CUSTOM_CAROUSEL && selectedIds.size <= 1 ->
                                YearbookCoverMode.SINGLE_CUSTOM
                            else -> selectedMode
                        }
                        when (effectiveMode) {
                            YearbookCoverMode.SMART_CAROUSEL -> {
                                onSaveCover(
                                    YearbookCoverMode.SMART_CAROUSEL,
                                    emptyList(),
                                    intervalSeconds,
                                    "",
                                    4f,
                                    CollageBackdropStyle.EDITION,
                                    emptyList()
                                )
                            }
                            YearbookCoverMode.CUSTOM_COLLAGE -> {
                                val finalIds = activeCollagePhotos.map { it.id }
                                val finalTransforms = List(activeCollageTemplate.slotCount) { idx ->
                                    slotTransforms.getOrNull(idx) ?: CollageSlotTransform()
                                }
                                onSaveCover(
                                    YearbookCoverMode.CUSTOM_COLLAGE,
                                    finalIds,
                                    intervalSeconds,
                                    activeCollageTemplate.id,
                                    collageSpacingDp,
                                    collageBackdrop,
                                    finalTransforms
                                )
                            }
                            YearbookCoverMode.SINGLE_CUSTOM,
                            YearbookCoverMode.CUSTOM_CAROUSEL -> {
                                onSaveCover(
                                    effectiveMode,
                                    selectedIds.take(YearbookCoverCurator.MAX_COVER_SLIDES),
                                    intervalSeconds,
                                    "",
                                    4f,
                                    CollageBackdropStyle.EDITION,
                                    emptyList()
                                )
                            }
                        }
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
private fun CoverStudioChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = if (selected) DecoGold else Color.Transparent,
        border = BorderStroke(0.85.dp, if (selected) DecoGold else DecoGold.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
private fun CoverModeSegment(
    label: String,
    iconKind: DecoIconKind,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = if (selected) DecoGold else Color.Transparent,
        border = if (selected) BorderStroke(0.9.dp, DecoGold) else null,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            DecoIcon(
                kind = iconKind,
                color = if (selected) DecoInkOnGround else DecoInkOnGround.copy(alpha = 0.72f),
                iconSize = 14.dp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) DecoInkOnGround else DecoInkOnGround.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DecoMiniCoverFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val frameShape = RoundedCornerShape(4.dp)
    val photoShape = RoundedCornerShape(2.dp)
    val miniShadowColor = if (isDark) Color.Black.copy(alpha = 0.56f) else DecoInkOnGround.copy(alpha = 0.28f)

    // 与书架 YearbookMonographCard 同构：金双框衬纸画框
    Box(
        modifier = modifier
            .shadow(10.dp, frameShape, spotColor = miniShadowColor)
            .clip(frameShape)
            .background(DecoMat)
            .border(1.5.dp, DecoGold, frameShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.5.dp)
                .clip(photoShape)
                .border(1.dp, DecoGold.copy(alpha = 0.40f), photoShape)
        ) {
            content()
        }
    }
}

@Composable
private fun MiniCollageCoverPreview(
    year: Int,
    editionColor: Color,
    photos: List<PhotoItemEntity>,
    template: CollageTemplate,
    modifier: Modifier = Modifier
) {
    DecoMiniCoverFrame(modifier = modifier) {
        YearbookCollageCanvas(
            photos = photos,
            template = template,
            spacingDp = template.defaultSpacingDp * 0.65f,
            backdrop = CollageBackdropStyle.EDITION,
            editionColor = editionColor,
            slotTransforms = emptyList(),
            interactive = false,
            downsampleTier = ImageDownsampleTier.PREVIEW_CARD,
            targetSizePx = 320,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1.0f to DecoScrimInk.copy(alpha = 0.78f)
                    )
                )
        )
        Text(
            text = year.toString(),
            fontFamily = DecoDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = DecoPaperCream,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 9.dp, bottom = 6.dp)
        )
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
    val activePhoto = slides.getOrNull(currentIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0)))

    DecoMiniCoverFrame(modifier = modifier) {
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
            } else {
                Box(modifier = Modifier.fillMaxSize().background(DecoGround))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to DecoScrimInk.copy(alpha = 0.30f),
                        0.42f to Color.Transparent,
                        1.0f to DecoScrimInk.copy(alpha = 0.78f)
                    )
                )
        )

        if (slides.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                slides.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.6.dp)
                            .background(
                                if (idx == currentIndex) DecoGold else DecoPaperCream.copy(alpha = 0.34f)
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 9.dp, end = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            if (activePhoto != null) {
                Text(
                    text = "${activePhoto.month}月",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DecoGold
                )
            }
            Text(
                text = year.toString(),
                fontFamily = DecoDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                lineHeight = 22.sp,
                color = DecoPaperCream
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
            .background(DecoMat)
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = if (isHighlighted) DecoGold else DecoGold.copy(alpha = 0.45f),
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
                color = if (isHighlighted) DecoGold else Color(0xCC12141C),
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
                DecoIcon(
    kind = DecoIconKind.FAVORITE,
    color = DecoGold,
    iconSize = 14.dp,
    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                )
            }
        }
        Text(
            text = "${photo.month}月 · $reasonTag",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighlighted) DecoGold else DecoInkOnGround,
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
                color = if (isSelected) DecoGold else DecoGold.copy(alpha = 0.45f),
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
                    .background(DecoGold.copy(alpha = 0.16f))
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) DecoGold else Color(0x7A10131A))
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
                    DecoIcon(
    kind = DecoIconKind.CHECK,
    color = DecoInkOnGround,
    iconSize = 14.dp,
    modifier = Modifier
                    )
                }
            }
        }

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
                DecoIcon(
    kind = DecoIconKind.FAVORITE,
    color = DecoGold,
    iconSize = 11.dp,
    modifier = Modifier
                )
            } else if (photo.photoStatus == PhotoStatus.KEPT) {
                DecoIcon(
    kind = DecoIconKind.CHECK,
    color = DecoGold,
    iconSize = 11.dp,
    modifier = Modifier
                )
            }
        }
    }
}
