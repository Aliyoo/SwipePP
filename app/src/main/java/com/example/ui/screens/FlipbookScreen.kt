package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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
import com.example.ui.components.BatchCompressBottomSheet
import com.example.ui.components.BatchLocationEditorSheet
import com.example.ui.components.BurstGalleryGrid
import com.example.ui.components.DecoPaperGround
import com.example.ui.components.DecoSealButton
import com.example.ui.components.DecoVermStrip
import com.example.ui.components.GalleryZoomTier
import com.example.ui.components.GalleryZoomTierIconButton
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.defaultGalleryZoomTier
import com.example.ui.components.formatStorageSize
import com.example.ui.components.monthChineseName
import com.example.ui.components.monthEnglishName
import com.example.ui.screens.gallery.BulkMoveDestinationSheet
import com.example.ui.screens.gallery.FloatingBulkActionBar
import com.example.ui.screens.gallery.GalleryMoreBatchActionsSheet
import com.example.ui.screens.gallery.GallerySelectionTopBar
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoGroundDeep
import com.example.ui.theme.DecoInkDeep
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TimelineSortOrder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.absoluteValue

/* ============================================================
 * 摩登画报 · 翻书月历（design-demos/deco-app.html FlipScreen）
 * 屏内一次性画报材料（地基令牌之外的本屏私有值，不进 theme 层）
 * ============================================================ */

// 压照片深墨 scrim 改走 theme/Color.kt 共享常量 DecoScrimInk（原型 rgba(13,24,20,*)，浅暗主题同值）；
// 朱红通栏投影改走共享常量 DecoVermShadowInk（原型 rgba(122,26,15,0.45)）

// 干支纪年副标（2026 丙午 / 2025 乙巳 / 2024 甲辰 / 2023 癸卯 / 2022 壬寅，公式覆盖其余年份）
private const val DECO_GAN = "甲乙丙丁戊己庚辛壬癸"
private const val DECO_ZHI = "子丑寅卯辰巳午未申酉戌亥"
private fun ganzhiOfYear(year: Int): String {
    val g = ((year - 4) % 10 + 10) % 10
    val z = ((year - 4) % 12 + 12) % 12
    return "${DECO_GAN[g]}${DECO_ZHI[z]}"
}

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

    DecoPaperGround(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            // 画报顶栏：方章返回 + 干支纪年副标 + 『月历/画廊』跳签（多选态整栏切换为选择顶栏）
            val cleanedMonthCount = remember(monthPages) { monthPages.count { it.isCleaned } }
            val populatedMonthCount = remember(monthPages) { monthPages.count { it.totalPhotos > 0 } }
            val topSubtitle = if (showGalleryMode) {
                "$year 年度影集 · ${populatedMonthCount} 个活跃月 · 展开管理连拍"
            } else {
                "$year 年度影集 · 已归档 $cleanedMonthCount/$populatedMonthCount 月 · 轻触日期直达"
            }

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
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DecoSealButton(
                            labelTop = "返",
                            labelBottom = "回",
                            onClick = onBack,
                            testTag = "flipbook_back_button",
                            contentDescriptionText = stringResource(R.string.cd_back),
                            size = 40.dp,
                            fontSize = 14.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${ganzhiOfYear(year)}年 · 年鉴",
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                letterSpacing = 1.2.sp,
                                color = DecoInkOnGround,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = topSubtitle,
                                fontSize = 12.sp,
                                color = DecoInkOnGround.copy(alpha = 0.66f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 多选入口（仅画廊态、当月有媒体时出现）：画报方章造型 + 基线进入多选逻辑
                        AnimatedVisibility(
                            visible = showGalleryMode && activeMonthPhotos.isNotEmpty(),
                            enter = fadeIn(tween(160)),
                            exit = fadeOut(tween(120))
                        ) {
                            DecoSealButton(
                                labelTop = "多",
                                labelBottom = "选",
                                onClick = {
                                    haptics.bookshelfDetent()
                                    explicitSelectionMode = true
                                },
                                testTag = "flipbook_enter_multiselect_button",
                                contentDescriptionText = stringResource(R.string.action_enter_multiselect),
                                size = 40.dp,
                                fontSize = 14.sp
                            )
                        }

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
                    Text(
                        text = "该年份暂无照片或视频",
                        fontSize = 14.sp,
                        color = DecoInkOnGround.copy(alpha = 0.62f)
                    )
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

                    // 翻页触感（基线保留）：过中点 0.42 纸页提起
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

                    // 翻页触感（基线保留）：落页双拍
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }
                            .drop(1)
                            .distinctUntilChanged()
                            .collect {
                                haptics.pageFlipSettle()
                            }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // 页堆纸叠厚度（静止可见，滑动时退场；alpha 绘制期求值 0 重组）
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
                                    .clip(DecoPageShape)
                                    .background(DecoGroundDeep)
                                    .border(0.8.dp, DecoGold.copy(alpha = 0.35f), DecoPageShape)
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

                            DecoMonthPosterPage(
                                page = pageModel,
                                isCurrentPage = isCurrentPage,
                                pageOffsetProvider = {
                                    val rawOffset = pagerState.currentPageOffsetFraction
                                    val safeOffset = if (rawOffset.isFinite()) rawOffset else 0f
                                    ((pagerState.currentPage - index) + safeOffset).coerceIn(-1f, 1f)
                                },
                                animateBadge = isRecentStamp,
                                showGalleryMode = showGalleryMode,
                                onSwitchViewMode = { target ->
                                    if (target != showGalleryMode) {
                                        haptics.pageFlipLift()
                                        showGalleryMode = target
                                    }
                                },
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

                    // 月份跳签（表冠刻度画报化：金框跳签 + 已清朱红印点 + 选中金下划线；多选时隐藏给批量栏让位）
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                            .clip(RoundedCornerShape(2.dp))
                                            .clickable {
                                                scope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                            .padding(horizontal = 9.dp, vertical = 6.dp)
                                            .testTag("month_pill_${monthModel.month}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(if (selected) 2.dp else 1.dp)
                                                .height(if (selected) 6.dp else 4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        selected -> DecoGold
                                                        // 已清标记：画报色板内表达（朱红印泥，原型 deco-app.html:351 朱红星章）
                                                        monthModel.isCleaned -> DecoVerm
                                                        hasMedia -> DecoGold.copy(alpha = 0.40f)
                                                        else -> DecoGold.copy(alpha = 0.15f)
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
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = if (selected) 13.sp else 12.sp,
                                                letterSpacing = 0.6.sp,
                                                color = when {
                                                    selected -> DecoInkOnGround
                                                    monthModel.isCleaned -> DecoInkOnGround.copy(alpha = 0.78f)
                                                    hasMedia -> DecoInkOnGround.copy(alpha = 0.66f)
                                                    else -> DecoInkOnGround.copy(alpha = 0.42f)
                                                }
                                            )
                                            if (monthModel.isCleaned) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        // 已清小圆点：朱红印泥（画报色板，弃绿/琥珀旧语义色）
                                                        .background(DecoVerm)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(if (selected) 18.dp else 0.dp)
                                                .height(2.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) DecoGold else Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating 4-Action Bulk Action Capsule — unified with Main Gallery
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

/* ============================================================
 * 画报书页 —— 左侧书脊铰链 3D 透视（基线参数原样保留）
 * ============================================================ */

private val DecoPageShape = RoundedCornerShape(3.dp)

@Composable
private fun DecoMonthPosterPage(
    page: MonthPageModel,
    isCurrentPage: Boolean,
    pageOffsetProvider: () -> Float,
    animateBadge: Boolean,
    showGalleryMode: Boolean,
    onSwitchViewMode: (Boolean) -> Unit,
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
    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isCurrentPage) 2f else 1f)
            .graphicsLayer {
                val pageOffset = pageOffsetProvider().coerceIn(-1f, 1f)
                val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                // 左侧书脊硬装铰链：x=0.06f 销点、±20°、缩放/透明随偏移（基线原样，绘制期求值 0 重组）
                transformOrigin = TransformOrigin(0.06f, 0.5f)
                rotationY = (-pageOffset * 20f).coerceIn(-24f, 24f)
                val scale = lerp(1f, 0.92f, absOffset)
                scaleX = scale
                scaleY = scale
                alpha = lerp(1f, 0.76f, absOffset)
                cameraDistance = 26f * density
            }
            .shadow(
                elevation = 12.dp,
                shape = DecoPageShape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.55f) else DecoInkDeep.copy(alpha = 0.16f)
            )
            .clip(DecoPageShape)
            .background(DecoMat)
            .border(1.dp, DecoGold.copy(alpha = 0.55f), DecoPageShape)
    ) {
        // 书脊阴影
        Box(
            modifier = Modifier
                .width(14.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (isDark) 0.42f else 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 月份牌：金双框月照片画片（外 1.5dp 金 + 3dp 内衬 + 1dp 弱金 + 9dp mat 呼吸边，原型 GoldFrame inset:9）
            DecoGoldFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // mat 衬边：照片不抵 1dp 弱金线（deco-app.html:312 inset:9 留白）
                        .padding(9.dp)
                ) {
                    val cover = page.coverPhoto
                    if (cover != null) {
                        PhotoArtwork(
                            photo = cover,
                            contentDescription = "${page.month}月封面画片",
                            showVideoBadge = false,
                            downsampleTier = ImageDownsampleTier.PREVIEW_CARD,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(DecoGround))
                    }

                    // 压字暗 scrim（可读性红线）：左 0.62 → 中透明 → 底 0.94
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0f to DecoScrimInk.copy(alpha = 0.62f),
                                    0.52f to DecoScrimInk.copy(alpha = 0f),
                                    1f to DecoScrimInk.copy(alpha = 0.16f)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to DecoScrimInk.copy(alpha = 0f),
                                    0.45f to DecoScrimInk.copy(alpha = 0f),
                                    1f to DecoScrimInk.copy(alpha = 0.94f)
                                )
                            )
                    )
                    // 放射太阳纹（24 扇形 × 6.5°，alpha 0.13，压在左下角）
                    val rayGold = DecoGold
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val rayRadius = 200.dp.toPx() / 2f
                                val sunCenter = Offset(70.dp.toPx(), size.height - 78.dp.toPx())
                                onDrawBehind {
                                    repeat(24) { i ->
                                        drawArc(
                                            color = rayGold.copy(alpha = 0.13f),
                                            startAngle = i * 15f,
                                            sweepAngle = 6.5f,
                                            useCenter = true,
                                            topLeft = Offset(sunCenter.x - rayRadius, sunCenter.y - rayRadius),
                                            size = Size(rayRadius * 2f, rayRadius * 2f)
                                        )
                                    }
                                }
                            }
                    )

                    // 美术字月份（错位阴影 2/4/6dp）+ 干支纪年副标；照片暗部用纸黄
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 8.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${page.year}年${page.month}月"
                            }
                    ) {
                        DecoArtText(
                            text = monthEnglishName(page.month),
                            fontSize = 38.sp,
                            color = DecoPaperCream
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${ganzhiOfYear(page.year)}年",
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium,
                            color = DecoPaperCream.copy(alpha = 0.95f),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    offset = Offset(1.5f, 1.5f),
                                    blurRadius = 2f
                                )
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // 归档印记：朱红方章（基线 CleanedCompletionBadge 等价迁移）
                    if (page.isCleaned) {
                        DecoMonthArchiveSeal(
                            month = page.month,
                            freedBytes = page.stamp?.freedBytes ?: 0L,
                            animateEntry = animateBadge,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .testTag("month_cleaned_stamp_${page.month}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 元信息行 + 归档释放量（画廊态附基线缩放档位切换）
            val editorialMetaLine = remember(page.totalPhotos, page.videoCount, page.burstGroupCount) {
                buildString {
                    append("共 ${page.totalPhotos} 项")
                    if (page.videoCount > 0) append(" · ${page.videoCount} 视频")
                    if (page.burstGroupCount > 0) append(" · ${page.burstGroupCount} 组连拍")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = editorialMetaLine,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DecoInkOnGround.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (page.isCleaned) {
                    val freedBytes = page.stamp?.freedBytes ?: 0L
                    Text(
                        text = if (freedBytes > 0L) "释放 ${formatStorageSize(freedBytes)}" else "全部保留",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DecoGold,
                        maxLines = 1
                    )
                }
                if (showGalleryMode && page.totalPhotos > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    GalleryZoomTierIconButton(
                        currentTier = galleryZoomTier,
                        onClick = {
                            onGalleryZoomTierChanged(galleryZoomTier.nextByTap(galleryColumns))
                        },
                        visualSize = 34.dp,
                        modifier = Modifier.testTag("flipbook_waterfall_toggle_${page.month}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        tint = DecoGold.copy(alpha = 0.55f),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${monthChineseName(page.month)}暂无照片或视频",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DecoInkOnGround,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "这一页留白给未来的回忆，左右滑动可浏览其他月份",
                        fontSize = 14.sp,
                        color = DecoInkOnGround.copy(alpha = 0.64f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 月历 ↔ 画廊切换（基线滑动过渡逻辑原样保留）
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
                        // 月历格：周一开头，有媒体嵌封面缩略、已清盖朱红星章
                        MonthCalendarGrid(
                            page = page,
                            onDayClick = onDayClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 页脚：朱红通栏双动作条（多选时隐藏给批量动作坞让位）
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (page.totalPhotos > 0) {
                        val monthName = monthChineseName(page.month)
                        val isCleanup = page.unreviewedCount > 0
                        val badge = if (isCleanup) {
                            "待清 ${page.unreviewedCount} · ${formatStorageSize(page.reclaimableBytes)}"
                        } else {
                            "已归档 ${page.keptCount}"
                        }
                        Text(
                            text = badge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DecoInkOnGround.copy(alpha = 0.66f),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DecoVermStrip(
                            leftLabel = if (isCleanup) "整理${monthName}" else "重温${monthName}",
                            rightLabel = if (showGalleryMode) "返回月历" else "翻阅画廊",
                            onLeft = onCleanClick,
                            onRight = {
                                // 与顶栏『月历/画廊』跳签同一入口：朱红通栏副动作切换视图
                                onSwitchViewMode(!showGalleryMode)
                            },
                            leftFlex = 1.35f,
                            leftTestTag = "clean_month_button_${page.month}"
                        )
                    } else {
                        Text(
                            text = "本月无媒体记录",
                            fontSize = 12.sp,
                            color = DecoInkOnGround.copy(alpha = 0.62f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 翻页暗角（绘制期求值 0 重组）
        val turnShadeColor = if (isDark) Color.Black else DecoInkDeep
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val absOffset = pageOffsetProvider().absoluteValue.coerceIn(0f, 1f)
                    alpha = if (absOffset > 0.02f) {
                        (absOffset * if (isDark) 0.24f else 0.12f).coerceIn(0f, 0.26f)
                    } else {
                        0f
                    }
                }
                .background(turnShadeColor)
        )
    }
}

/* ============================================================
 * 月历格（周一开头 —— 基线布局逻辑原样，画报金框视觉）
 * ============================================================ */

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
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(DecoGold.copy(alpha = 0.07f))
            .border(1.dp, DecoGold.copy(alpha = 0.40f), RoundedCornerShape(2.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { dayName ->
                Text(
                    text = dayName,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DecoInkOnGround.copy(alpha = 0.72f),
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
            .clip(RoundedCornerShape(2.dp))
            .then(
                when {
                    isCleanedStar -> Modifier.border(1.2.dp, DecoGold.copy(alpha = 0.90f), RoundedCornerShape(2.dp))
                    hasPhoto -> Modifier.border(1.dp, DecoGold.copy(alpha = 0.70f), RoundedCornerShape(2.dp))
                    else -> Modifier.border(0.8.dp, DecoGold.copy(alpha = 0.16f), RoundedCornerShape(2.dp))
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
            // 压字暗 scrim（可读性红线）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DecoScrimInk.copy(alpha = 0.30f))
            )
        }

        Text(
            text = day.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = if (hasPhoto) FontWeight.Bold else FontWeight.Normal,
            color = if (hasPhoto) DecoPaperCream else DecoInkOnGround.copy(alpha = 0.62f),
            style = if (hasPhoto) {
                TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.80f),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            } else {
                TextStyle.Default
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )

        if (hasVideo && !isCleanedStar) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = DecoPaperCream,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(3.dp)
                    .size(12.dp)
            )
        }

        if (isCleanedStar) {
            // 朱红星章：方章朱红底 + 金描边 + 纸黄星（基线"已整理星标"语义保留）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(14.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(DecoVerm)
                    .border(0.8.dp, DecoGold.copy(alpha = 0.85f), RoundedCornerShape(1.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "已整理星标",
                    tint = DecoPaperCream,
                    modifier = Modifier.size(9.dp)
                )
            }
        } else if (itemsCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(DecoScrimInk.copy(alpha = 0.88f))
                    .border(0.5.dp, DecoPaperCream.copy(alpha = 0.28f), RoundedCornerShape(1.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$itemsCount",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DecoPaperCream,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/* ============================================================
 * 画报构件（屏内私有）：金双框 / 太阳纹 / 美术字
 * （纸面地 / 方章 / 朱红通栏收敛到 components/GlassSurface 共享版）
 * ============================================================ */

/** 金双线画框：外 1.5dp 金 + 3dp 内衬 + 1dp 弱金（原型 GoldFrame） */
@Composable
private fun DecoGoldFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val gold = DecoGold
    Box(modifier = modifier.border(1.5.dp, gold, RoundedCornerShape(3.dp))) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, gold.copy(alpha = 0.40f), RoundedCornerShape(2.dp))
        ) {
            content()
        }
    }
}

/** 美术字：EditorialSerif Bold(700) + 2/4/6dp 错位阴影（原型 artShadow；家族无 900 字面，900 请求回落 Bold） */
@Composable
private fun DecoArtText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val verm = DecoVerm
    val baseStyle = TextStyle(
        fontFamily = EditorialSerifFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        lineHeight = fontSize,
        letterSpacing = 0.5.sp
    )
    Box(modifier = modifier) {
        Text(
            text = text,
            style = baseStyle.copy(color = Color.Black.copy(alpha = 0.35f)),
            modifier = Modifier.offset(x = 6.dp, y = 6.dp)
        )
        Text(
            text = text,
            style = baseStyle.copy(color = verm.copy(alpha = 0.60f)),
            modifier = Modifier.offset(x = 4.dp, y = 4.dp)
        )
        Text(
            text = text,
            style = baseStyle.copy(color = Color.Black.copy(alpha = 0.55f)),
            modifier = Modifier.offset(x = 2.dp, y = 2.dp)
        )
        Text(text = text, style = baseStyle.copy(color = color))
    }
}

// 方章按钮收敛到共享组件 com.example.ui.components.DecoSealButton（衬线金墨字 + 48dp 命中区）

/** 朱红归档方章：朱红底 + 金描边 + 纸黄字（基线 CleanedCompletionBadge 语义等价迁移） */
@Composable
private fun DecoMonthArchiveSeal(
    month: Int,
    freedBytes: Long,
    animateEntry: Boolean,
    modifier: Modifier = Modifier
) {
    val verm = DecoVerm
    val gold = DecoGold
    val cream = DecoPaperCream
    val scale = remember(month, animateEntry) { Animatable(if (animateEntry) 1.6f else 1f) }
    val alpha = remember(month, animateEntry) { Animatable(if (animateEntry) 0f else 1f) }

    LaunchedEffect(month, animateEntry) {
        if (animateEntry) {
            alpha.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val statusDesc = if (freedBytes > 0L) {
        "${month}月已整理完成，待释放或已释放 ${formatStorageSize(freedBytes)}"
    } else {
        "${month}月已整理完成，全部保留"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = statusDesc }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                rotationZ = -4f
            }
            .clip(RoundedCornerShape(2.dp))
            .background(verm)
            .border(1.dp, gold.copy(alpha = 0.65f), RoundedCornerShape(2.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = cream,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = "已整理",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = cream
        )
    }
}


/**
 * 『月历/画廊』38dp 跳签（基线磨砂滑块功能等价迁移为画报跳签：
 * 保留 testTag / semantics(selected) / Role.Tab / 38dp 视觉高度，弃胶囊滑块）。
 */
@Composable
private fun FlipbookViewModeSwitch(
    showGalleryMode: Boolean,
    onSelectMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = DecoGold
    val ink = DecoInkOnGround
    val calendarTextColor by animateColorAsState(
        targetValue = if (!showGalleryMode) ink else ink.copy(alpha = 0.62f),
        animationSpec = tween(180),
        label = "cal_txt_color"
    )
    val calendarIconColor by animateColorAsState(
        targetValue = if (!showGalleryMode) gold else gold.copy(alpha = 0.45f),
        animationSpec = tween(180),
        label = "cal_ico_color"
    )
    val galleryTextColor by animateColorAsState(
        targetValue = if (showGalleryMode) ink else ink.copy(alpha = 0.62f),
        animationSpec = tween(180),
        label = "gal_txt_color"
    )
    val galleryIconColor by animateColorAsState(
        targetValue = if (showGalleryMode) gold else gold.copy(alpha = 0.45f),
        animationSpec = tween(180),
        label = "gal_ico_color"
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DecoModeTab(
            testTag = "flipbook_mode_calendar",
            desc = "月历视图",
            isSelected = !showGalleryMode,
            label = "月历",
            icon = Icons.Filled.CalendarMonth,
            iconTint = calendarIconColor,
            textColor = calendarTextColor,
            onClick = { onSelectMode(false) }
        )
        DecoModeTab(
            testTag = "flipbook_mode_gallery",
            desc = "连拍画廊视图",
            isSelected = showGalleryMode,
            label = "画廊",
            icon = Icons.Filled.GridView,
            iconTint = galleryIconColor,
            textColor = galleryTextColor,
            onClick = { onSelectMode(true) }
        )
    }
}

@Composable
private fun DecoModeTab(
    testTag: String,
    desc: String,
    isSelected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val gold = DecoGold
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(64.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(2.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(testTag)
            .semantics(mergeDescendants = true) {
                selected = isSelected
                contentDescription = desc
            }
    ) {
        // 38dp 视觉跳签（48dp 命中区垂直居中）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(if (isSelected) gold.copy(alpha = 0.16f) else Color.Transparent)
                .border(1.dp, if (isSelected) gold else gold.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.5.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
