package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.CompressionPreset
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.data.PhotoRepository
import com.example.data.PhotoStatus
import com.example.data.ResolvedGeoLocation
import com.example.data.SameDayLocationMatch
import com.example.data.inferSameDayLocations
import com.example.sensor.rememberHapticEngine
import com.example.ui.GalleryEntry
import com.example.ui.LocationCluster
import com.example.ui.MonthPageModel
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.BatchCompressBottomSheet
import com.example.ui.components.BatchLocationEditorSheet
import com.example.ui.components.BurstGalleryGrid
import com.example.ui.components.CleanedCompletionBadge
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GalleryHeaderOrbButton
import com.example.ui.components.GallerySculptedHeroButton
import com.example.ui.components.GallerySelectableChip
import com.example.ui.components.GalleryZoomTier
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.activeChromeSatinBrush
import com.example.ui.components.defaultGalleryZoomTier
import com.example.ui.components.formatStorageSize
import com.example.ui.components.monthChineseName
import com.example.ui.screens.gallery.BulkMoveDestinationSheet
import com.example.ui.screens.gallery.FloatingBulkActionBar
import com.example.ui.screens.gallery.GalleryMoreBatchActionsSheet
import com.example.ui.screens.gallery.GallerySelectionTopBar
import com.example.ui.theme.GlassSpecularRimStart
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EditorialSerifFontFamily
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
import com.example.ui.theme.SolarAmberSoft
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.StudioCanvasBottom
import com.example.ui.theme.StudioCanvasTop
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.VioletVideo
import com.example.ui.theme.VioletVideoSoft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.key
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.absoluteValue

@Composable
fun FlipbookScreen(
    year: Int,
    monthPages: List<MonthPageModel>,
    recentlyStampedMonthKey: String?,
    timelineSortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST,
    showEmptyMonths: Boolean = false,
    defaultShowGalleryMode: Boolean = false,
    galleryColumns: Int = 3,
    existingAlbums: List<String> = emptyList(),
    availableYears: List<Int> = emptyList(),
    locationClusters: List<LocationCluster> = emptyList(),
    allPhotos: List<PhotoItemEntity> = emptyList(),
    onBack: () -> Unit,
    onStartMonthCleanup: (year: Int, month: Int, includeReviewed: Boolean, focusDay: Int?) -> Unit,
    onSelectBestInBurst: (groupId: String, photoId: Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onKeepPhoto: (PhotoItemEntity, Boolean) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onToggleFavorite: (PhotoItemEntity, Boolean) -> Unit = { p, f -> onKeepPhoto(p, f) },
    onBulkTrashPhotos: (List<PhotoItemEntity>) -> Unit = {},
    onBulkKeepPhotos: (List<PhotoItemEntity>, Boolean) -> Unit = { _, _ -> },
    onBulkMovePhotos: (List<PhotoItemEntity>, String?, Int?, Int?) -> Unit = { _, _, _, _ -> },
    onBatchCompress: (List<PhotoItemEntity>, CompressionPreset) -> Unit = { _, _ -> },
    onBatchManualLocation: (List<PhotoItemEntity>, String, Double, Double, Boolean) -> Unit = { _, _, _, _, _ -> },
    onBatchSameDayLocation: (List<SameDayLocationMatch>, Boolean) -> Unit = { _, _ -> },
    onGeocodeLocation: suspend (String) -> ResolvedGeoLocation? = { null },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticEngine()
    var showGalleryMode by remember(year, defaultShowGalleryMode) { mutableStateOf(defaultShowGalleryMode) }
    var expandedBurstIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    var lightboxContextPhotos by remember { mutableStateOf<List<PhotoItemEntity>>(emptyList()) }

    var galleryZoomTier by rememberSaveable(year) { mutableStateOf(defaultGalleryZoomTier(galleryColumns)) }
    LaunchedEffect(galleryColumns) {
        if (!galleryZoomTier.isWaterfall) {
            galleryZoomTier = defaultGalleryZoomTier(galleryColumns)
        }
    }

    // Unified Multi-Select & Bulk Action state for Flipbook Month Gallery View
    var explicitSelectionMode by remember { mutableStateOf(false) }
    var selectedPhotoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showMoreActionsSheet by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showCompressSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }

    val isSelectionMode = showGalleryMode && (explicitSelectionMode || selectedPhotoIds.isNotEmpty())
    val exitSelectionMode = remember {
        {
            explicitSelectionMode = false
            selectedPhotoIds = emptySet()
            showMoreActionsSheet = false
            showMoveSheet = false
            showCompressSheet = false
            showLocationSheet = false
        }
    }

    BackHandler(enabled = isSelectionMode) {
        exitSelectionMode()
    }

    LaunchedEffect(showGalleryMode) {
        if (!showGalleryMode) {
            exitSelectionMode()
        }
    }

    val defaultPage = remember(year, timelineSortOrder, showEmptyMonths, monthPages.size) {
        monthPages.indexOfFirst { it.totalPhotos > 0 }.takeIf { it >= 0 } ?: 0
    }
    var activePageIndex by remember(year, timelineSortOrder, showEmptyMonths) {
        mutableIntStateOf(defaultPage)
    }

    val activeMonthPage = monthPages.getOrNull(activePageIndex)
    val activeMonthPhotos = remember(activeMonthPage) {
        activeMonthPage?.photos?.filter { it.photoStatus != PhotoStatus.TRASHED }.orEmpty()
    }
    val allYearVisiblePhotoMap = remember(monthPages, isSelectionMode) {
        if (!isSelectionMode) {
            emptyMap()
        } else {
            val map = LinkedHashMap<Long, PhotoItemEntity>()
            for (page in monthPages) {
                for (photo in page.photos) {
                    if (photo.photoStatus != PhotoStatus.TRASHED) {
                        map[photo.id] = photo
                    }
                }
            }
            map
        }
    }

    LaunchedEffect(allYearVisiblePhotoMap, isSelectionMode) {
        if (isSelectionMode && selectedPhotoIds.isNotEmpty()) {
            val valid = selectedPhotoIds.filterTo(mutableSetOf()) { it in allYearVisiblePhotoMap }
            if (valid.size != selectedPhotoIds.size) {
                selectedPhotoIds = valid
            }
        }
    }

    val selectedPhotos = remember(selectedPhotoIds, allYearVisiblePhotoMap) {
        if (selectedPhotoIds.isEmpty()) emptyList() else selectedPhotoIds.mapNotNull { allYearVisiblePhotoMap[it] }
    }
    val selectedBytes = remember(selectedPhotos) {
        selectedPhotos.sumOf { it.fileSizeBytes }
    }
    val selectedCompressibleCount = remember(selectedPhotos) {
        selectedPhotos.count { !it.isVideo }
    }
    val selectedMissingLocationCount = remember(selectedPhotos) {
        selectedPhotos.count { it.locationName.isNullOrBlank() || it.latitude == null || it.longitude == null }
    }
    val allActiveMonthSelected = isSelectionMode &&
        activeMonthPhotos.isNotEmpty() &&
        activeMonthPhotos.all { it.id in selectedPhotoIds }

    val toggleSelectPhotos = remember {
        { photos: List<PhotoItemEntity> ->
            val ids = photos.map { it.id }
            val allAlreadySelected = ids.isNotEmpty() && ids.all { it in selectedPhotoIds }
            selectedPhotoIds = if (allAlreadySelected) {
                selectedPhotoIds - ids.toSet()
            } else {
                selectedPhotoIds + ids
            }
        }
    }

    val longPressSelectPhotos = remember(haptics) {
        { photos: List<PhotoItemEntity> ->
            haptics.bookshelfDetent()
            explicitSelectionMode = true
            selectedPhotoIds = selectedPhotoIds + photos.map { it.id }
        }
    }

    ArtisticGalleryBackdrop(
        warmAccent = true,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .widthIn(max = 640.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Top Navigation Bar — seamlessly morphs into GallerySelectionTopBar during multi-select
                val cleanedMonthCount = remember(monthPages) { monthPages.count { it.isCleaned } }
                val populatedMonthCount = remember(monthPages) { monthPages.count { it.totalPhotos > 0 } }

                AnimatedContent(
                    targetState = isSelectionMode,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically(tween(200)) { -it / 4 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically(tween(160)) { -it / 4 })
                    },
                    label = "flipbook_header_mode"
                ) { selecting ->
                    if (selecting) {
                        GallerySelectionTopBar(
                            selectedCount = selectedPhotos.size,
                            selectedBytes = selectedBytes,
                            allSelected = allActiveMonthSelected,
                            onCancel = {
                                haptics.pageFlipLift()
                                exitSelectionMode()
                            },
                            onToggleSelectAll = {
                                haptics.bookshelfDetent()
                                val monthIds = activeMonthPhotos.map { it.id }.toSet()
                                selectedPhotoIds = if (allActiveMonthSelected) {
                                    selectedPhotoIds - monthIds
                                } else {
                                    selectedPhotoIds + monthIds
                                }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                GalleryHeaderOrbButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("flipbook_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back),
                                        tint = InkPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$year 年度影集",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = InkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (showGalleryMode) {
                                            "${populatedMonthCount} 个活跃月 · 双指放大 · 长按多选"
                                        } else {
                                            "已归档 $cleanedMonthCount/$populatedMonthCount 月 · 轻触日期直达"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showGalleryMode && activeMonthPhotos.isNotEmpty(),
                                enter = fadeIn(tween(160)),
                                exit = fadeOut(tween(120))
                            ) {
                                GalleryHeaderOrbButton(
                                    onClick = {
                                        haptics.bookshelfDetent()
                                        explicitSelectionMode = true
                                    },
                                    modifier = Modifier.testTag("flipbook_enter_multiselect_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckBox,
                                        contentDescription = stringResource(R.string.action_enter_multiselect),
                                        tint = CobaltPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Optically matched 38dp frosted pill switcher with sliding satin thumb (48dp touch target)
                            FlipbookViewModeSwitch(
                                showGalleryMode = showGalleryMode,
                                onSelectMode = { targetGallery ->
                                    if (targetGallery != showGalleryMode) {
                                        haptics.pageFlipLift()
                                        showGalleryMode = targetGallery
                                    }
                                }
                            )
                        }
                    }
                }

                if (monthPages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("该年份暂无照片或视频", color = InkSecondary)
                    }
                } else {
                    key(year, timelineSortOrder, showEmptyMonths) {
                        val pagerState = rememberPagerState(initialPage = defaultPage, pageCount = { monthPages.size })
                        val monthPillListState = rememberLazyListState()

                        LaunchedEffect(pagerState.currentPage, monthPages.size) {
                            activePageIndex = pagerState.currentPage
                            if (pagerState.currentPage in monthPages.indices) {
                                monthPillListState.animateScrollToItem(
                                    index = (pagerState.currentPage - 1).coerceAtLeast(0)
                                )
                            }
                        }

                        // Tactile 3D Calendar Page-Flip Haptics:
                        // 1) Paper-hinge lift tick halfway through dragging a page across the spine
                        LaunchedEffect(pagerState) {
                            var midpointTriggered = false
                            snapshotFlow { pagerState.currentPageOffsetFraction.absoluteValue }
                                .collect { absOffset ->
                                    if (absOffset >= 0.42f && !midpointTriggered) {
                                        midpointTriggered = true
                                        haptics.pageFlipLift()
                                    } else if (absOffset < 0.12f) {
                                        midpointTriggered = false
                                    }
                                }
                        }

                        // 2) Soft double-beat paper settle whenever flipping lands on a new month page
                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.currentPage }
                                .drop(1)
                                .distinctUntilChanged()
                                .collect {
                                    haptics.pageFlipSettle()
                                }
                        }

                        // Full-bleed HorizontalPager stage: keeps pages cleanly inset at rest via contentPadding
                        // while letting turning pages glide all the way to the screen edges without harsh 16dp slicing.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Underlying crisp page stack thickness — alpha driven strictly in graphicsLayer (0 recompositions!)
                            if (monthPages.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 22.dp, end = 12.dp, top = 14.dp, bottom = 6.dp)
                                        .graphicsLayer {
                                            val scrollFraction = pagerState.currentPageOffsetFraction
                                                .let { if (it.isFinite()) it.absoluteValue else 0f }
                                            alpha = (1f - scrollFraction * 4.2f).coerceIn(0f, 1f)
                                        }
                                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp))
                                        .background(SurfaceSubtle)
                                        .border(
                                            0.8.dp,
                                            HairlineBorder.copy(alpha = 0.7f),
                                            RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp)
                                        )
                                )
                            }

                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                pageSpacing = 14.dp,
                                beyondViewportPageCount = (monthPages.size - 1).coerceIn(0, 1),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("flipbook_3d_pager")
                            ) { index ->
                                val pageModel = monthPages.getOrNull(index) ?: return@HorizontalPager
                                val isCurrentPage by remember(index, pagerState) {
                                    derivedStateOf { pagerState.currentPage == index }
                                }
                                val isRecentStamp = recentlyStampedMonthKey == PhotoRepository.yearMonthKey(pageModel.year, pageModel.month)

                                ModernCalendarBookPage(
                                    page = pageModel,
                                    isCurrentPage = isCurrentPage,
                                    pageOffsetProvider = {
                                        val rawOffset = pagerState.currentPageOffsetFraction
                                        val safeOffset = if (rawOffset.isFinite()) rawOffset else 0f
                                        ((pagerState.currentPage - index) + safeOffset).coerceIn(-1f, 1f)
                                    },
                                    animateBadge = isRecentStamp,
                                    showGalleryMode = showGalleryMode,
                                    timelineSortOrder = timelineSortOrder,
                                    galleryColumns = galleryColumns,
                                    galleryZoomTier = galleryZoomTier,
                                    onGalleryZoomTierChanged = { galleryZoomTier = it },
                                    isSelectionMode = isSelectionMode,
                                    selectedPhotoIds = selectedPhotoIds,
                                    onToggleSelectPhotos = toggleSelectPhotos,
                                    onLongPressSelectPhotos = longPressSelectPhotos,
                                    expandedBurstIds = expandedBurstIds,
                                    onToggleBurst = { gid ->
                                        expandedBurstIds = if (gid in expandedBurstIds) {
                                            expandedBurstIds - gid
                                        } else {
                                            expandedBurstIds + gid
                                        }
                                    },
                                    onSelectBestInBurst = onSelectBestInBurst,
                                    onKeepBestInBurst = { burst, chosen ->
                                        haptics.swipeDeleteConfirm()
                                        expandedBurstIds = expandedBurstIds - burst.groupId
                                        onKeepBestInBurst(burst, chosen)
                                    },
                                    onTrashPhoto = { photo ->
                                        haptics.swipeDeleteConfirm()
                                        onTrashPhoto(photo)
                                    },
                                    onPhotoClick = { photo ->
                                        val visible = pageModel.photos.filter { it.photoStatus != PhotoStatus.TRASHED }
                                        lightboxContextPhotos = if (timelineSortOrder == TimelineSortOrder.NEWEST_FIRST) {
                                            visible.sortedByDescending { it.takenAtMillis }
                                        } else {
                                            visible.sortedBy { it.takenAtMillis }
                                        }
                                        lightboxPhoto = photo
                                    },
                                    onCleanClick = {
                                        onStartMonthCleanup(
                                            pageModel.year,
                                            pageModel.month,
                                            pageModel.unreviewedCount == 0,
                                            null
                                        )
                                    },
                                    onDayClick = { dayNum ->
                                        val dayPhotos = pageModel.photosByDay[dayNum].orEmpty()
                                        val dayHasUnreviewed = dayPhotos.any { it.photoStatus == PhotoStatus.UNREVIEWED }
                                        onStartMonthCleanup(
                                            pageModel.year,
                                            pageModel.month,
                                            !dayHasUnreviewed,
                                            dayNum
                                        )
                                    }
                                )
                            }
                        }

                        // Frameless Watchmaker Precision Month Scale (mirrors Bookshelf's year dial; hides during multi-select for bulk bar clarity)
                        AnimatedVisibility(
                            visible = !isSelectionMode,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(120))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyRow(
                                    state = monthPillListState,
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(monthPages) { index, monthModel ->
                                        val selected = pagerState.currentPage == index
                                        val hasMedia = monthModel.totalPhotos > 0
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    scope.launch { pagerState.animateScrollToPage(index) }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("month_pill_${monthModel.month}")
                                        ) {
                                            // Subtle watchmaker tick above month numeral
                                            Box(
                                                modifier = Modifier
                                                    .width(if (selected) 2.dp else 1.dp)
                                                    .height(if (selected) 6.dp else 4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            selected -> CobaltPrimary
                                                            monthModel.isCleaned -> EmeraldKeep
                                                            hasMedia -> InkTertiary.copy(alpha = 0.45f)
                                                            else -> InkTertiary.copy(alpha = 0.20f)
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = monthChineseName(monthModel.month),
                                                    fontFamily = if (selected) OutfitFontFamily else JetBrainsMonoFontFamily,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = if (selected) 14.sp else 12.sp,
                                                    color = when {
                                                        selected -> InkPrimary
                                                        monthModel.isCleaned -> EmeraldKeep
                                                        hasMedia -> InkSecondary
                                                        else -> InkTertiary.copy(alpha = 0.50f)
                                                    }
                                                )
                                                if (monthModel.isCleaned) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (selected) SolarAmber else EmeraldKeep)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(if (selected) 20.dp else 0.dp)
                                                    .height(2.dp)
                                                    .clip(CircleShape)
                                                    .background(if (selected) InkPrimary else Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating 4-Action Obsidian Glass Bulk Action Capsule — unified with Main Gallery
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(tween(200)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut(tween(150)) + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(150)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .pointerInput(Unit) {}
                ) {
                    FloatingBulkActionBar(
                        selectedCount = selectedPhotos.size,
                        onKeep = {
                            if (selectedPhotos.isNotEmpty()) {
                                haptics.swipeKeepConfirm()
                                onBulkKeepPhotos(selectedPhotos, false)
                                exitSelectionMode()
                            }
                        },
                        onFavorite = {
                            if (selectedPhotos.isNotEmpty()) {
                                haptics.swipeKeepConfirm()
                                onBulkKeepPhotos(selectedPhotos, true)
                                exitSelectionMode()
                            }
                        },
                        onTrash = {
                            if (selectedPhotos.isNotEmpty()) {
                                haptics.swipeDeleteConfirm()
                                onBulkTrashPhotos(selectedPhotos)
                                exitSelectionMode()
                            }
                        },
                        onMore = {
                            if (selectedPhotos.isNotEmpty()) {
                                haptics.pageFlipLift()
                                showMoreActionsSheet = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showMoreActionsSheet && selectedPhotos.isNotEmpty()) {
        GalleryMoreBatchActionsSheet(
            selectedCount = selectedPhotos.size,
            compressibleCount = selectedCompressibleCount,
            missingLocationCount = selectedMissingLocationCount,
            onCompress = {
                showMoreActionsSheet = false
                showCompressSheet = true
            },
            onEditLocation = {
                showMoreActionsSheet = false
                showLocationSheet = true
            },
            onMove = {
                showMoreActionsSheet = false
                showMoveSheet = true
            },
            onDismiss = { showMoreActionsSheet = false }
        )
    }

    if (showMoveSheet && selectedPhotos.isNotEmpty()) {
        val yearsList = remember(availableYears, year) {
            availableYears.ifEmpty { listOf(year) }
        }
        BulkMoveDestinationSheet(
            selectedCount = selectedPhotos.size,
            selectedBytes = selectedBytes,
            existingAlbums = existingAlbums,
            availableYears = yearsList,
            defaultYear = selectedPhotos.firstOrNull()?.year ?: year,
            defaultMonth = selectedPhotos.firstOrNull()?.month ?: activeMonthPage?.month ?: 1,
            onDismiss = { showMoveSheet = false },
            onConfirmMove = { albumName, targetYear, targetMonth ->
                haptics.swipeKeepConfirm()
                onBulkMovePhotos(selectedPhotos, albumName, targetYear, targetMonth)
                showMoveSheet = false
                exitSelectionMode()
            }
        )
    }

    if (showCompressSheet && selectedPhotos.isNotEmpty()) {
        BatchCompressBottomSheet(
            selectedPhotos = selectedPhotos,
            onDismiss = { showCompressSheet = false },
            onConfirmCompress = { preset ->
                haptics.swipeKeepConfirm()
                onBatchCompress(selectedPhotos, preset)
                showCompressSheet = false
                exitSelectionMode()
            }
        )
    }

    if (showLocationSheet && selectedPhotos.isNotEmpty()) {
        val referencePhotos = remember(allPhotos, monthPages) {
            allPhotos.ifEmpty { monthPages.flatMap { it.photos } }
        }
        val sameDayMatches = remember(selectedPhotos, referencePhotos) {
            inferSameDayLocations(selectedPhotos, referencePhotos)
        }
        BatchLocationEditorSheet(
            targetPhotos = selectedPhotos,
            existingClusters = locationClusters,
            sameDayMatches = sameDayMatches,
            onResolveQuery = onGeocodeLocation,
            onApplySameDayInference = { matches, writeExif ->
                haptics.swipeKeepConfirm()
                onBatchSameDayLocation(matches, writeExif)
                showLocationSheet = false
                exitSelectionMode()
            },
            onConfirmManualLocation = { locName, lat, lon, writeExif ->
                haptics.swipeKeepConfirm()
                onBatchManualLocation(selectedPhotos, locName, lat, lon, writeExif)
                showLocationSheet = false
                exitSelectionMode()
            },
            onDismiss = { showLocationSheet = false }
        )
    }

    lightboxPhoto?.let { photo ->
        val list = lightboxContextPhotos.ifEmpty { listOf(photo) }
        val startIdx = list.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        PhotoDetailLightbox(
            photo = photo,
            photos = list,
            initialIndex = startIdx,
            onDismiss = { lightboxPhoto = null },
            onKeep = onKeepPhoto,
            onTrash = onTrashPhoto,
            onToggleFavorite = onToggleFavorite
        )
    }
}

/**
 * Precision 38dp-tall frosted segmented toggle optically aligned with [GalleryHeaderOrbButton],
 * featuring a spring-sliding adaptive satin thumb (obsidian in light, sapphire-slate in dark)
 * inside a 48dp accessible hit area with zero M3 minimum-size inflation bloat.
 */
@Composable
private fun FlipbookViewModeSwitch(
    showGalleryMode: Boolean,
    onSelectMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val palette = LocalFlipYearPalette.current
    val activeBrush = activeChromeSatinBrush()
    val density = LocalDensity.current
    val segmentWidth = 62.dp
    val segmentWidthPx = with(density) { segmentWidth.toPx() }
    val trackPadding = 3.dp
    val trackHeight = 38.dp
    val thumbHeight = trackHeight - trackPadding * 2
    val totalWidth = segmentWidth * 2 + trackPadding * 2

    val thumbOffsetPxState = animateFloatAsState(
        targetValue = if (showGalleryMode) segmentWidthPx else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "flipbook_mode_thumb_x"
    )
    val thumbBorderBrush = remember(palette) {
        Brush.linearGradient(
            listOf(
                palette.activeChromeBorderStart,
                Color.White.copy(alpha = 0.14f),
                palette.activeChromeBorderEnd
            )
        )
    }

    val calendarTextColor by animateColorAsState(
        targetValue = if (!showGalleryMode) Color.White else InkSecondary,
        animationSpec = tween(180),
        label = "cal_txt_color"
    )
    val calendarIconColor by animateColorAsState(
        targetValue = if (!showGalleryMode) SolarAmber else InkSecondary,
        animationSpec = tween(180),
        label = "cal_ico_color"
    )
    val galleryTextColor by animateColorAsState(
        targetValue = if (showGalleryMode) Color.White else InkSecondary,
        animationSpec = tween(180),
        label = "gal_txt_color"
    )
    val galleryIconColor by animateColorAsState(
        targetValue = if (showGalleryMode) SolarAmber else InkSecondary,
        animationSpec = tween(180),
        label = "gal_ico_color"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(totalWidth)
            .height(48.dp)
    ) {
        // Visual 38dp frosted glass housing (zero HWUI shadow on translucent pill)
        Box(
            modifier = Modifier
                .width(totalWidth)
                .height(trackHeight)
                .clip(CircleShape)
                .background(if (isDark) SurfacePure.copy(alpha = 0.80f) else SurfacePure.copy(alpha = 0.94f))
                .border(
                    width = 0.95.dp,
                    color = if (isDark) HairlineBorder else GlassSpecularRimStart,
                    shape = CircleShape
                )
                .padding(trackPadding)
        ) {
            // Sliding sculpted satin active indicator (draw-phase translationX -> zero layout passes!)
            Box(
                modifier = Modifier
                    .graphicsLayer { translationX = thumbOffsetPxState.value }
                    .width(segmentWidth)
                    .height(thumbHeight)
                    .clip(CircleShape)
                    .drawBehind {
                        drawRect(brush = activeBrush)
                        drawLine(
                            color = Color.White.copy(alpha = if (isDark) 0.18f else 0.24f),
                            start = Offset(0f, 0.7.dp.toPx()),
                            end = Offset(size.width, 0.7.dp.toPx()),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }
                    .border(width = 0.75.dp, brush = thumbBorderBrush, shape = CircleShape)
            )
        }

        // Full 48dp-tall accessible clickable tab segments layered cleanly over the 38dp track
        Row(
            modifier = Modifier
                .width(totalWidth)
                .height(48.dp)
                .padding(horizontal = trackPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .semantics(mergeDescendants = true) {
                        selected = !showGalleryMode
                        contentDescription = "月历视图"
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelectMode(false) }
                    )
                    .testTag("flipbook_mode_calendar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = calendarIconColor,
                        modifier = Modifier.size(13.5.dp)
                    )
                    Text(
                        text = "月历",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = calendarTextColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .semantics(mergeDescendants = true) {
                        selected = showGalleryMode
                        contentDescription = "连拍画廊视图"
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelectMode(true) }
                    )
                    .testTag("flipbook_mode_gallery")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = null,
                        tint = galleryIconColor,
                        modifier = Modifier.size(13.5.dp)
                    )
                    Text(
                        text = "画廊",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = galleryTextColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernCalendarBookPage(
    page: MonthPageModel,
    isCurrentPage: Boolean,
    pageOffsetProvider: () -> Float,
    animateBadge: Boolean,
    showGalleryMode: Boolean,
    timelineSortOrder: TimelineSortOrder,
    galleryColumns: Int,
    galleryZoomTier: GalleryZoomTier,
    onGalleryZoomTierChanged: (GalleryZoomTier) -> Unit,
    isSelectionMode: Boolean,
    selectedPhotoIds: Set<Long>,
    onToggleSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    onLongPressSelectPhotos: (List<PhotoItemEntity>) -> Unit,
    expandedBurstIds: Set<String>,
    onToggleBurst: (String) -> Unit,
    onSelectBestInBurst: (String, Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    onCleanClick: () -> Unit,
    onDayClick: (Int) -> Unit
) {
    val pageShape = remember { RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp) }

    val isDark = LocalIsDarkTheme.current
    val pageShadowColor = if (isDark) Color.Black.copy(alpha = 0.55f) else InkPrimary.copy(alpha = 0.14f)
    val spineShadowColor = if (isDark) Color.Black.copy(alpha = 0.34f) else InkPrimary.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isCurrentPage) 2f else 1f)
            .graphicsLayer {
                val pageOffset = pageOffsetProvider().coerceIn(-1f, 1f)
                val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                // Realistic left-spine hardcover hinge evaluated strictly in draw phase (0 recompositions!)
                transformOrigin = TransformOrigin(0.06f, 0.5f)
                rotationY = (-pageOffset * 20f).coerceIn(-24f, 24f)
                val scale = lerp(1f, 0.92f, absOffset)
                scaleX = scale
                scaleY = scale
                alpha = lerp(1f, 0.76f, absOffset)
                cameraDistance = 26f * density
            }
            .shadow(elevation = 8.dp, shape = pageShape, spotColor = pageShadowColor)
            .clip(pageShape)
            .background(SurfacePure)
            .border(1.dp, HairlineBorder, pageShape)
    ) {
        // Subtle modern left spine shadow
        Box(
            modifier = Modifier
                .width(14.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            spineShadowColor,
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Clean Editorial Month Header (Single sculpted month lockup — no duplicate numeral)
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "${page.year}年${page.month}月"
                        }
                    ) {
                        Text(
                            text = page.month.toString(),
                            fontFamily = EditorialSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            lineHeight = 38.sp,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "月",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = InkPrimary,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "· ${page.year}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = InkTertiary,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    HorizontalDivider(thickness = 0.8.dp, color = HairlineBorder)
                    Spacer(modifier = Modifier.height(6.dp))
                    val editorialMetaLine = remember(page.totalPhotos, page.videoCount, page.burstGroupCount) {
                        buildString {
                            append("共 ${page.totalPhotos} 项")
                            if (page.videoCount > 0) append(" · ${page.videoCount} 视频")
                            if (page.burstGroupCount > 0) append(" · ${page.burstGroupCount} 组连拍")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = editorialMetaLine,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (showGalleryMode && page.totalPhotos > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            GallerySelectableChip(
                                selected = galleryZoomTier.isWaterfall,
                                onClick = {
                                    val next = if (galleryZoomTier.isWaterfall) {
                                        defaultGalleryZoomTier(galleryColumns)
                                    } else {
                                        GalleryZoomTier.WATERFALL_2
                                    }
                                    onGalleryZoomTierChanged(next)
                                },
                                modifier = Modifier.testTag("flipbook_waterfall_toggle_${page.month}")
                            ) {
                                Icon(
                                    imageVector = if (galleryZoomTier.isWaterfall) Icons.Filled.Dashboard else Icons.Filled.GridView,
                                    contentDescription = if (galleryZoomTier.isWaterfall) "切换为标准网格" else "展开大图瀑布流",
                                    tint = if (galleryZoomTier.isWaterfall) SolarAmber else CobaltPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (galleryZoomTier.isWaterfall) galleryZoomTier.shortChipLabel else "瀑布流",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (galleryZoomTier.isWaterfall) Color.White else InkPrimary
                                )
                            }
                        }
                    }
                }

                if (page.isCleaned) {
                    CleanedCompletionBadge(
                        month = page.month,
                        freedBytes = page.stamp?.freedBytes ?: 0L,
                        animateEntry = animateBadge,
                        showMonthPrefix = false,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("month_cleaned_stamp_${page.month}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (page.totalPhotos == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = InkTertiary.copy(alpha = 0.65f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${monthChineseName(page.month)}暂无照片或视频",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "这一页留白给未来的回忆，左右滑动可浏览其他月份",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                AnimatedContent(
                    targetState = showGalleryMode,
                    transitionSpec = {
                        val enter = fadeIn(tween(185, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(tween(205, easing = FastOutSlowInEasing)) { width ->
                                if (targetState) width / 14 else -width / 14
                            }
                        val exit = fadeOut(tween(120)) +
                            slideOutHorizontally(tween(145, easing = FastOutSlowInEasing)) { width ->
                                if (targetState) -width / 16 else width / 16
                            }
                        enter togetherWith exit
                    },
                    label = "flipbook_calendar_gallery_switch",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { galleryActive ->
                    if (galleryActive) {
                        val monthGalleryEntries = remember(page.photos, timelineSortOrder) {
                            com.example.ui.domain.LibraryDerivationEngine.buildGalleryEntries(
                                photos = page.photos.filter { it.photoStatus != PhotoStatus.TRASHED },
                                sortOrder = timelineSortOrder
                            )
                        }

                        BurstGalleryGrid(
                            entries = monthGalleryEntries,
                            expandedBurstIds = expandedBurstIds,
                            onToggleBurst = onToggleBurst,
                            onSelectBestInBurst = onSelectBestInBurst,
                            onKeepBestInBurst = onKeepBestInBurst,
                            onTrashSinglePhoto = onTrashPhoto,
                            onPhotoClick = onPhotoClick,
                            columns = galleryColumns.coerceIn(3, 4),
                            contentPadding = PaddingValues(
                                top = 4.dp,
                                bottom = if (isSelectionMode) 82.dp else 4.dp
                            ),
                            isSelectionMode = isSelectionMode,
                            selectedPhotoIds = selectedPhotoIds,
                            onToggleSelectPhotos = onToggleSelectPhotos,
                            onLongPressSelectPhotos = onLongPressSelectPhotos,
                            zoomTier = galleryZoomTier,
                            onZoomTierChanged = onGalleryZoomTierChanged,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("flipbook_month_gallery_${page.month}")
                        )
                    } else {
                        // Modern Monthly Calendar Grid
                        MonthCalendarGrid(
                            page = page,
                            onDayClick = onDayClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Footer CTA — sculpted satin hero button with embedded telemetry badge (smoothly hidden during multi-select)
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (page.totalPhotos > 0) {
                        val monthName = monthChineseName(page.month)
                        val isCleanup = page.unreviewedCount > 0
                        val badge = if (isCleanup) {
                            "待清 ${page.unreviewedCount} · ${formatStorageSize(page.reclaimableBytes)}"
                        } else {
                            "已归档 ${page.keptCount}"
                        }
                        GallerySculptedHeroButton(
                            title = if (isCleanup) "直接开始 ${monthName}清理" else "重温 ${monthName}回忆",
                            leadingIcon = if (isCleanup) Icons.Filled.CleaningServices else Icons.Filled.AutoAwesome,
                            badgeText = badge,
                            tone = GalleryButtonTone.OBSIDIAN,
                            height = 52.dp,
                            onClick = onCleanClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clean_month_button_${page.month}")
                        )
                    } else {
                        Text(
                            text = "本月无媒体记录",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        val turnShadeColor = if (isDark) Color.Black else InkPrimary
        val maxShadeAlpha = if (isDark) 0.24f else 0.12f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val absOffset = pageOffsetProvider().absoluteValue.coerceIn(0f, 1f)
                    alpha = if (absOffset > 0.02f) {
                        (absOffset * maxShadeAlpha).coerceIn(0f, 0.26f)
                    } else {
                        0f
                    }
                }
                .background(turnShadeColor)
        )
    }
}

@Composable
private fun MonthCalendarGrid(
    page: MonthPageModel,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekHeaders = listOf("一", "二", "三", "四", "五", "六", "日")
    val (leadingBlanks, daysInMonth) = remember(page.year, page.month) {
        val cal = Calendar.getInstance().apply { set(page.year, page.month - 1, 1) }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val mondayBasedOffset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        mondayBasedOffset to cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { dayName ->
                Text(
                    text = dayName,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - leadingBlanks + 1
                    if (dayNum in 1..daysInMonth) {
                        val dayPhotos = page.photosByDay[dayNum].orEmpty()
                        val isDayCleaned = dayNum in page.cleanedDays
                        CalendarDaySlot(
                            month = page.month,
                            day = dayNum,
                            itemsCount = dayPhotos.size,
                            hasVideo = dayPhotos.any { it.isVideo },
                            coverPhoto = dayPhotos.firstOrNull(),
                            isCleanedStar = isDayCleaned,
                            onClick = { if (dayPhotos.isNotEmpty()) onDayClick(dayNum) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("calendar_day_${page.month}_$dayNum")
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDaySlot(
    month: Int,
    day: Int,
    itemsCount: Int,
    hasVideo: Boolean,
    coverPhoto: PhotoItemEntity?,
    isCleanedStar: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhoto = coverPhoto != null
    val dayDesc = if (hasPhoto) "${month}月${day}日，${itemsCount}项媒体，点击进入整理" else "${month}月${day}日，无照片"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (hasPhoto) InkPrimary else SurfaceSubtle.copy(alpha = 0.50f))
            .then(
                when {
                    isCleanedStar -> Modifier.border(1.5.dp, SolarAmber, RoundedCornerShape(10.dp))
                    !hasPhoto -> Modifier.border(0.65.dp, HairlineBorder.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    else -> Modifier
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = dayDesc }
            .clickable(enabled = hasPhoto, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (coverPhoto != null) {
            PhotoArtwork(
                photo = coverPhoto,
                contentDescription = "${day}日媒体",
                showVideoBadge = false,
                downsampleTier = ImageDownsampleTier.MICRO_THUMB,
                targetSizePx = 144,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
            )
        }

        Text(
            text = day.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = if (hasPhoto) FontWeight.Bold else FontWeight.Normal,
            color = if (hasPhoto) Color.White else InkTertiary.copy(alpha = 0.72f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )

        if (hasVideo && !isCleanedStar) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(3.dp)
                    .size(12.dp)
            )
        }

        if (isCleanedStar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SolarAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "已整理星标",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        } else if (itemsCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xB80E121D))
                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$itemsCount",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
