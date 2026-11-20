package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CleaningServices
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.data.PhotoRepository
import com.example.data.PhotoStatus
import com.example.sensor.rememberHapticEngine
import com.example.ui.GalleryEntry
import com.example.ui.MonthPageModel
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.BurstGalleryGrid
import com.example.ui.components.CleanedCompletionBadge
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GallerySelectableChip
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.formatStorageSize
import com.example.ui.components.monthChineseName
import com.example.ui.components.monthEnglishName
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
import com.example.ui.theme.VioletVideo
import com.example.ui.theme.VioletVideoSoft
import androidx.compose.material3.HorizontalDivider
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
    onBack: () -> Unit,
    onStartMonthCleanup: (year: Int, month: Int, includeReviewed: Boolean, focusDay: Int?) -> Unit,
    onSelectBestInBurst: (groupId: String, photoId: Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onKeepPhoto: (PhotoItemEntity, Boolean) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticEngine()
    var showGalleryMode by remember { mutableStateOf(false) }
    var expandedBurstIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }

    ArtisticGalleryBackdrop(
        warmAccent = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("flipbook_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = InkPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "$year 年度影集",
                            style = MaterialTheme.typography.headlineMedium,
                            color = InkPrimary
                        )
                        Text(
                            text = if (showGalleryMode) {
                                "左右滑动翻页 · 展开管理连拍组"
                            } else {
                                "左右滑动翻页 · ${stringResource(R.string.calendar_day_jump_hint)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }
                }

                // Calendar vs Burst Gallery View Mode Switcher
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (showGalleryMode) haptics.pageFlipLift()
                                showGalleryMode = false
                            },
                            shape = RoundedCornerShape(50),
                            color = if (!showGalleryMode) SurfacePure else Color.Transparent,
                            shadowElevation = if (!showGalleryMode) 1.dp else 0.dp,
                            modifier = Modifier.testTag("flipbook_mode_calendar")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = "月历视图",
                                    tint = if (!showGalleryMode) CobaltPrimary else InkSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "月历",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!showGalleryMode) InkPrimary else InkSecondary
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                if (!showGalleryMode) haptics.pageFlipLift()
                                showGalleryMode = true
                            },
                            shape = RoundedCornerShape(50),
                            color = if (showGalleryMode) SurfacePure else Color.Transparent,
                            shadowElevation = if (showGalleryMode) 1.dp else 0.dp,
                            modifier = Modifier.testTag("flipbook_mode_gallery")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GridView,
                                    contentDescription = "连拍画廊视图",
                                    tint = if (showGalleryMode) CobaltPrimary else InkSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "画廊",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showGalleryMode) InkPrimary else InkSecondary
                                )
                            }
                        }
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
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { monthPages.size })
                val monthPillListState = rememberLazyListState()

                LaunchedEffect(pagerState.currentPage, monthPages.size) {
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

                // Modern Crisp White 3D Hinged Flipbook
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Underlying crisp page stack thickness
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp))
                            .background(SurfaceSubtle)
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("flipbook_3d_pager")
                    ) { index ->
                        val pageModel = monthPages[index]
                        val pageOffset = ((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction)
                        val clamped = pageOffset.coerceIn(-1f, 1f)
                        val isRecentStamp = recentlyStampedMonthKey == PhotoRepository.yearMonthKey(pageModel.year, pageModel.month)

                        ModernCalendarBookPage(
                            page = pageModel,
                            pageOffset = clamped,
                            animateBadge = isRecentStamp,
                            showGalleryMode = showGalleryMode,
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
                            onPhotoClick = { photo -> lightboxPhoto = photo },
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

                // Bottom Month Selector Pills (auto-scrolls with active month page)
                LazyRow(
                    state = monthPillListState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(monthPages) { index, monthModel ->
                        val selected = pagerState.currentPage == index
                        GallerySelectableChip(
                            selected = selected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            shape = RoundedCornerShape(14.dp),
                            unselectedTint = if (monthModel.isCleaned) EmeraldSoft else null,
                            unselectedBorderAccent = if (monthModel.isCleaned) EmeraldKeep else null,
                            modifier = Modifier.testTag("month_pill_${monthModel.month}")
                        ) {
                            Text(
                                text = monthChineseName(monthModel.month),
                                style = MaterialTheme.typography.labelLarge,
                                color = when {
                                    selected -> Color.White
                                    monthModel.isCleaned -> EmeraldKeep
                                    else -> InkPrimary
                                }
                            )
                            if (monthModel.isCleaned) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "已整理",
                                    tint = SolarAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    lightboxPhoto?.let { photo ->
        PhotoDetailLightbox(
            photo = photo,
            onDismiss = { lightboxPhoto = null },
            onKeep = onKeepPhoto,
            onTrash = onTrashPhoto
        )
    }
}

@Composable
private fun ModernCalendarBookPage(
    page: MonthPageModel,
    pageOffset: Float,
    animateBadge: Boolean,
    showGalleryMode: Boolean,
    expandedBurstIds: Set<String>,
    onToggleBurst: (String) -> Unit,
    onSelectBestInBurst: (String, Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    onCleanClick: () -> Unit,
    onDayClick: (Int) -> Unit
) {
    val absOffset = pageOffset.absoluteValue
    val hingeOrigin = if (pageOffset >= 0f) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
    val rotationDeg = (-pageOffset * 56f).coerceIn(-66f, 66f)
    val scale = 1f - (absOffset * 0.05f)
    val pageShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp)

    val isDark = LocalIsDarkTheme.current
    val pageShadowColor = if (isDark) Color.Black.copy(alpha = 0.55f) else InkPrimary.copy(alpha = 0.14f)
    val spineShadowColor = if (isDark) Color.Black.copy(alpha = 0.34f) else InkPrimary.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                transformOrigin = hingeOrigin
                rotationY = rotationDeg
                scaleX = scale
                scaleY = scale
                cameraDistance = 16f * density
            }
            .shadow(elevation = 12.dp, shape = pageShape, spotColor = pageShadowColor)
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
                .padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Editorial Magazine Spread Month Header (Giant Serif Numeral + Monospace Folio)
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = page.month.toString().padStart(2, '0'),
                            fontFamily = EditorialSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            lineHeight = 42.sp,
                            color = InkPrimary
                        )
                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                text = "${page.year} · ${monthEnglishName(page.month)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                color = InkTertiary
                            )
                            Text(
                                text = monthChineseName(page.month),
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = InkPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(thickness = 0.8.dp, color = HairlineBorder)
                    Spacer(modifier = Modifier.height(7.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SurfaceMuted
                        ) {
                            Text(
                                text = "共 ${page.totalPhotos} 项",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (page.videoCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = VioletVideoSoft
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = VioletVideo,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${page.videoCount} 视频",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VioletVideo,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (page.burstGroupCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SolarAmberSoft
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FilterNone,
                                        contentDescription = null,
                                        tint = SolarAmber,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "${page.burstGroupCount} 组连拍",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                if (page.isCleaned) {
                    CleanedCompletionBadge(
                        month = page.month,
                        freedBytes = page.stamp?.freedBytes ?: 0L,
                        animateEntry = animateBadge,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("month_cleaned_stamp_${page.month}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showGalleryMode) {
                val monthGalleryEntries = remember(page.photos) {
                    val visible = page.photos.filter { it.photoStatus != PhotoStatus.TRASHED }
                        .sortedByDescending { it.takenAtMillis }
                    val burstMap = HashMap<String, ArrayList<PhotoItemEntity>>()
                    for (photo in visible) {
                        val gid = photo.burstGroupId
                        if (gid != null && !photo.isVideo) {
                            burstMap.getOrPut(gid) { ArrayList(4) }.add(photo)
                        }
                    }
                    val entries = ArrayList<GalleryEntry>(visible.size)
                    val consumed = HashSet<String>(burstMap.size * 2)
                    for (photo in visible) {
                        val gid = photo.burstGroupId
                        if (gid != null && !photo.isVideo) {
                            if (consumed.add(gid)) {
                                val members = burstMap[gid].orEmpty().sortedBy { it.takenAtMillis }
                                if (members.size >= 2) {
                                    val best = members.firstOrNull { it.isBestInStack }
                                        ?: members.maxByOrNull { it.qualityScore }
                                        ?: members.first()
                                    entries += GalleryEntry.Burst(gid, members, best)
                                } else {
                                    entries += GalleryEntry.Single(photo)
                                }
                            }
                        } else {
                            entries += GalleryEntry.Single(photo)
                        }
                    }
                    entries
                }

                BurstGalleryGrid(
                    entries = monthGalleryEntries,
                    expandedBurstIds = expandedBurstIds,
                    onToggleBurst = onToggleBurst,
                    onSelectBestInBurst = onSelectBestInBurst,
                    onKeepBestInBurst = onKeepBestInBurst,
                    onTrashSinglePhoto = onTrashPhoto,
                    onPhotoClick = onPhotoClick,
                    columns = 3,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("flipbook_month_gallery_${page.month}")
                )
            } else {
                // Modern Monthly Calendar Grid
                MonthCalendarGrid(
                    page = page,
                    onDayClick = onDayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer CTA
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (page.unreviewedCount > 0) {
                            "剩余 ${page.unreviewedCount} 项待整理"
                        } else {
                            "已精选归档 ${page.keptCount} 项回忆"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Text(
                        text = if (page.unreviewedCount > 0) {
                            "共 ${formatStorageSize(page.reclaimableBytes)}"
                        } else {
                            "已释放 ${formatStorageSize(page.stamp?.freedBytes ?: 0L)}"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (page.unreviewedCount > 0) CoralDanger else EmeraldKeep
                    )
                }

                GalleryActionButton(
                    text = if (page.unreviewedCount > 0) {
                        "整理 ${monthChineseName(page.month)} · 进入弧形时光轴"
                    } else {
                        "重温 ${monthChineseName(page.month)} 回忆"
                    },
                    leadingIcon = if (page.unreviewedCount > 0) Icons.Filled.CleaningServices else Icons.Filled.AutoAwesome,
                    tone = GalleryButtonTone.OBSIDIAN,
                    onClick = onCleanClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clean_month_button_${page.month}")
                )
            }
        }

        if (absOffset > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InkPrimary.copy(alpha = (absOffset * 0.16f).coerceIn(0f, 0.24f)))
            )
        }
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
                            day = dayNum,
                            itemsCount = dayPhotos.size,
                            hasVideo = dayPhotos.any { it.isVideo },
                            coverPhoto = dayPhotos.firstOrNull(),
                            isCleanedStar = isDayCleaned,
                            onClick = { if (dayPhotos.isNotEmpty()) onDayClick(dayNum) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.92f)
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
    day: Int,
    itemsCount: Int,
    hasVideo: Boolean,
    coverPhoto: PhotoItemEntity?,
    isCleanedStar: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhoto = coverPhoto != null
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (hasPhoto) InkPrimary else SurfaceMuted.copy(alpha = 0.72f))
            .then(
                if (isCleanedStar) {
                    Modifier.border(1.6.dp, SolarAmber, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
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
                    .background(Color.Black.copy(alpha = 0.24f))
            )
        }

        Text(
            text = day.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = if (hasPhoto) FontWeight.Bold else FontWeight.Normal,
            color = if (hasPhoto) Color.White else InkTertiary,
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
                    .background(CobaltPrimary)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$itemsCount",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
