package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.MediaAccessLevel
import com.example.data.PhotoItemEntity
import com.example.data.YearbookCoverMode
import com.example.sensor.rememberHapticEngine
import com.example.ui.FlipYearUiState
import com.example.ui.LocalFloatingDockInset
import com.example.ui.MediaFilter
import com.example.ui.YearbookSummary
import com.example.ui.components.Adaptive3DBookshelfCarousel
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.ObsidianGlassCard
import com.example.ui.components.ObsidianGlassPill
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.ThemeAppearanceSheet
import com.example.ui.components.YearbookCoverStudioSheet
import com.example.ui.components.activeChromeSatinBrush
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EditionCobalt
import com.example.ui.theme.EditionObsidian
import com.example.ui.theme.EditionSlatePlum
import com.example.ui.theme.EditionTeal
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.GlassLightSubtle
import com.example.ui.theme.GlassSpecularRimStart
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SkylightChampagneGlow
import com.example.ui.theme.SkylightSapphireGlow
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.StudioCanvasBottom
import com.example.ui.theme.StudioCanvasTop
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.VioletVideo
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun BookshelfScreen(
    uiState: FlipYearUiState,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onSelectThemeMode: (AppThemeMode) -> Unit = {},
    onOpenYearbook: (Int) -> Unit,
    onStartYearCleanup: (year: Int, includeReviewed: Boolean) -> Unit = { _, _ -> },
    onOpenVault: () -> Unit = {},
    onRequestMediaPermission: () -> Unit,
    onRescanDeviceMedia: () -> Unit,
    onSelectFilter: (MediaFilter) -> Unit,
    onLoadDemoArchive: () -> Unit,
    onSaveYearbookCover: (year: Int, mode: YearbookCoverMode, selectedPhotoIds: List<Long>, intervalSeconds: Int) -> Unit = { _, _, _, _ -> },
    onResetYearbookCover: (year: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val books = uiState.yearbooks
    val haptics = rememberHapticEngine()
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var customizingYear by remember { mutableStateOf<Int?>(null) }

    val floatingDockInset = LocalFloatingDockInset.current

    ArtisticGalleryBackdrop(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = floatingDockInset)
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern Studio Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = InkPrimary
                    )
                    Text(
                        text = if (uiState.totalImageCount + uiState.totalVideoCount > 0) {
                            "${uiState.totalImageCount} 张照片 · ${uiState.totalVideoCount} 个视频"
                        } else {
                            "3D 照片与视频智能清理"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FrostedGlassCard(
                        onClick = {
                            if (uiState.mediaAccessLevel == MediaAccessLevel.NONE) {
                                onRequestMediaPermission()
                            } else {
                                onRescanDeviceMedia()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        shadowElevation = 4.dp,
                        modifier = Modifier.testTag("scan_media_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isScanningDevice) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = CobaltPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.cd_scan_media),
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_scan_media),
                                style = MaterialTheme.typography.labelLarge,
                                color = InkPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { showThemeSheet = true },
                        modifier = Modifier.testTag("bookshelf_theme_button")
                    ) {
                        Icon(
                            imageVector = when (themeMode) {
                                AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                AppThemeMode.DARK -> Icons.Filled.DarkMode
                            },
                            contentDescription = stringResource(R.string.cd_theme_switcher),
                            tint = if (isDark) SolarAmber else CobaltPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("bookshelf_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "更多选项",
                                tint = InkSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = SurfacePure
                        ) {
                            DropdownMenuItem(
                                text = { Text("外观主题 · ${themeMode.label}", color = InkPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (themeMode) {
                                            AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                            AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                            AppThemeMode.DARK -> Icons.Filled.DarkMode
                                        },
                                        contentDescription = null,
                                        tint = if (isDark) SolarAmber else CobaltPrimary
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showThemeSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("授权全部照片与视频权限", color = InkPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = CobaltPrimary)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRequestMediaPermission()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_load_demo), color = InkPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = InkSecondary)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onLoadDemoArchive()
                                }
                            )
                        }
                    }
                }
            }

            // Unified Frosted-Glass Studio Metrics Strip (translucent alabaster glass with specular rim)
            FrostedGlassCard(
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 5.dp,
                enableAmbientBlur = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernMetricSlot(
                        label = "已累计释放",
                        value = formatStorageSize(uiState.stats.totalPurgedBytes),
                        accent = EmeraldKeep,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(26.dp),
                        thickness = 1.dp,
                        color = HairlineBorder.copy(alpha = 0.7f)
                    )
                    ModernMetricSlot(
                        label = "回收站待清理",
                        value = formatStorageSize(uiState.pendingTrashBytes),
                        accent = CoralDanger,
                        onClick = if (uiState.pendingTrashBytes > 0L) onOpenVault else null,
                        showChevron = uiState.pendingTrashBytes > 0L,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bookshelf_vault_metric")
                    )
                    VerticalDivider(
                        modifier = Modifier.height(26.dp),
                        thickness = 1.dp,
                        color = HairlineBorder.copy(alpha = 0.7f)
                    )
                    ModernMetricSlot(
                        label = "已完成月份",
                        value = "${uiState.monthStamps.size} 个月",
                        accent = CobaltPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Partial access hint on Android 14+
            if (uiState.mediaAccessLevel == MediaAccessLevel.PARTIAL) {
                Surface(
                    onClick = onRequestMediaPermission,
                    shape = RoundedCornerShape(14.dp),
                    color = CobaltSoft,
                    border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "当前仅授权部分媒体，点击开放全部本地相册",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = CobaltDark,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "去授权",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = CobaltPrimary
                        )
                    }
                }
            }

            // Clean Frosted-Glass Segmented Media Filter (All / Photos / Videos)
            if (uiState.totalImageCount + uiState.totalVideoCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                FrostedGlassCard(
                    shape = RoundedCornerShape(50),
                    topColor = GlassLightSubtle,
                    bottomColor = SurfaceMuted.copy(alpha = 0.62f),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MediaFilter.entries.forEach { filter ->
                            val selected = uiState.mediaFilter == filter
                            val countSuffix = when (filter) {
                                MediaFilter.ALL -> "${uiState.totalImageCount + uiState.totalVideoCount}"
                                MediaFilter.PHOTOS -> "${uiState.totalImageCount}"
                                MediaFilter.VIDEOS -> "${uiState.totalVideoCount}"
                            }
                            Surface(
                                onClick = { onSelectFilter(filter) },
                                shape = RoundedCornerShape(50),
                                color = if (selected) {
                                    if (isDark) SurfaceSubtle else SurfacePure.copy(alpha = 0.94f)
                                } else {
                                    Color.Transparent
                                },
                                border = if (selected) BorderStroke(0.8.dp, GlassSpecularRimStart) else null,
                                shadowElevation = if (selected) 3.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_${filter.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "${filter.label} ($countSuffix)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 12.sp,
                                        color = if (selected) InkPrimary else InkSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (books.isEmpty()) {
                EmptyLibraryPermissionCard(
                    accessLevel = uiState.mediaAccessLevel,
                    isScanning = uiState.isScanningDevice,
                    activeFilter = uiState.mediaFilter,
                    onRequestPermission = onRequestMediaPermission,
                    onRescan = onRescanDeviceMedia,
                    onResetFilter = { onSelectFilter(MediaFilter.ALL) },
                    onLoadDemo = onLoadDemoArchive,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { books.size })

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .drop(1)
                        .distinctUntilChanged()
                        .collect {
                            haptics.bookshelfDetent()
                        }
                }

                // Adaptive 3D Bookshelf Carousel: dynamically adapts perspective & depth-of-field to scroll speed
                val settledBook = books[pagerState.settledPage.coerceIn(0, books.lastIndex)]
                var activeAmbientCoverPhoto by remember(settledBook.year, settledBook.coverPhoto) {
                    mutableStateOf(settledBook.coverPhoto)
                }

                Adaptive3DBookshelfCarousel(
                    items = books,
                    pagerState = pagerState,
                    settledCoverPhoto = activeAmbientCoverPhoto ?: settledBook.coverPhoto,
                    itemKey = { it.year },
                    pagerTestTag = "bookshelf_3d_pager",
                    modifier = Modifier.weight(1f)
                ) { book, page, kinematics, cardModifier ->
                    val isBookSettledCenter = kinematics.isCentered && pagerState.settledPage == page
                    ModernYearbook3DCard(
                        summary = book,
                        isFocused = isBookSettledCenter,
                        isCarouselPaused = pagerState.isScrollInProgress || customizingYear != null,
                        onCustomizeCover = {
                            haptics.pageFlipLift()
                            customizingYear = book.year
                        },
                        onActiveSlideChanged = { slidePhoto ->
                            if (pagerState.settledPage == page && slidePhoto != null) {
                                activeAmbientCoverPhoto = slidePhoto
                            }
                        },
                        onClick = {
                            if (pagerState.currentPage == page) {
                                onOpenYearbook(book.year)
                            } else {
                                scope.launch { pagerState.animateScrollToPage(page) }
                            }
                        },
                        modifier = cardModifier.testTag("yearbook_card_${book.year}")
                    )
                }

                // Active Year Wheel Scrubber + Primary Action
                val activeBook = books[pagerState.currentPage.coerceIn(0, books.lastIndex)]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModernYearWheel(
                        years = books.map { it.year },
                        selectedIndex = pagerState.currentPage,
                        onSelectIndex = { idx ->
                            scope.launch { pagerState.animateScrollToPage(idx) }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GalleryActionButton(
                        text = if (activeBook.unreviewedCount > 0) {
                            "立即整理 ${activeBook.year} 年 · ${activeBook.unreviewedCount} 项待理"
                        } else {
                            "重温 ${activeBook.year} 年精选回忆"
                        },
                        leadingIcon = if (activeBook.unreviewedCount > 0) Icons.Filled.CleaningServices else Icons.Filled.AutoAwesome,
                        tone = GalleryButtonTone.OBSIDIAN,
                        onClick = {
                            onStartYearCleanup(activeBook.year, activeBook.unreviewedCount == 0)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_yearbook_button")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    val targetBook = customizingYear?.let { year -> books.find { it.year == year } }
    if (targetBook != null) {
        val yearPhotos = remember(uiState.allPhotos, targetBook.year) {
            uiState.allPhotos.filter { it.year == targetBook.year }
        }
        YearbookCoverStudioSheet(
            summary = targetBook,
            yearPhotos = yearPhotos,
            onDismiss = { customizingYear = null },
            onSaveCover = { mode, ids, interval ->
                onSaveYearbookCover(targetBook.year, mode, ids, interval)
            },
            onResetToSmart = {
                onResetYearbookCover(targetBook.year)
            }
        )
    }

    if (showThemeSheet) {
        ThemeAppearanceSheet(
            currentMode = themeMode,
            onSelectMode = onSelectThemeMode,
            onDismiss = { showThemeSheet = false }
        )
    }
}

@Composable
private fun ModernYearbook3DCard(
    summary: YearbookSummary,
    isFocused: Boolean,
    isCarouselPaused: Boolean,
    onCustomizeCover: () -> Unit,
    onActiveSlideChanged: (PhotoItemEntity?) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverColor = when (summary.editionIndex) {
        0 -> EditionObsidian
        1 -> EditionCobalt
        2 -> EditionTeal
        else -> EditionSlatePlum
    }
    val bookShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp)
    val rimBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.24f)
            )
        )
    }

    val slides = remember(summary.coverSlides, summary.coverPhoto) {
        summary.coverSlides.ifEmpty { listOfNotNull(summary.coverPhoto) }
    }
    val slideKeys = remember(slides) { slides.map { it.id } }
    var currentSlideIndex by remember(summary.year, slideKeys) { mutableIntStateOf(0) }
    val safeSlideIndex = if (slides.isEmpty()) 0 else currentSlideIndex.coerceIn(0, slides.lastIndex)
    val displayedPhoto = slides.getOrNull(safeSlideIndex) ?: summary.coverPhoto
    val slideProgress = remember(summary.year, slideKeys) { Animatable(0f) }

    val isCarouselActive = isFocused && !isCarouselPaused && slides.size > 1

    LaunchedEffect(displayedPhoto, isFocused) {
        if (isFocused) {
            onActiveSlideChanged(displayedPhoto)
        }
    }

    // Smooth museum-grade slideshow clock + Ken Burns progress — strictly active ONLY on the focused center book
    LaunchedEffect(summary.year, slideKeys, summary.coverIntervalSeconds, isCarouselActive) {
        if (!isCarouselActive) {
            slideProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val durationMs = (summary.coverIntervalSeconds.coerceIn(3, 12) * 1000)
        while (true) {
            slideProgress.snapTo(0f)
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
            currentSlideIndex = (currentSlideIndex + 1) % slides.size
        }
    }

    val bookSpotShadow = if (LocalIsDarkTheme.current) Color.Black.copy(alpha = 0.60f) else InkPrimary.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .shadow(elevation = 22.dp, shape = bookShape, spotColor = bookSpotShadow)
            .clip(bookShape)
            .background(coverColor)
            .clickable(onClick = onClick)
    ) {
        // 100% Full-Bleed Edge-to-Edge Living Cover Showcase (Crossfade + whisper-subtle Ken Burns drift)
        if (displayedPhoto != null) {
            val kenBurnsScale = if (isCarouselActive) lerp(1.00f, 1.07f, slideProgress.value) else 1f
            val kenBurnsPanX = if (isCarouselActive) {
                val direction = if (safeSlideIndex % 2 == 0) 1f else -1f
                lerp(-6f, 6f, slideProgress.value) * direction
            } else {
                0f
            }

            Crossfade(
                targetState = displayedPhoto,
                animationSpec = tween(durationMillis = 820),
                label = "yearbook_cover_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { slidePhoto ->
                PhotoArtwork(
                    photo = slidePhoto,
                    contentDescription = stringResource(R.string.cd_yearbook_cover),
                    showVideoBadge = false,
                    downsampleTier = ImageDownsampleTier.HERO_CARD,
                    targetSizePx = 768,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                            translationX = kenBurnsPanX
                        }
                )
            }
        }

        // Cinematic Editorial Vignette for crisp legibility at top & bottom while preserving center photo clarity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to coverColor.copy(alpha = 0.72f),
                        0.34f to coverColor.copy(alpha = 0.12f),
                        0.66f to coverColor.copy(alpha = 0.18f),
                        1.0f to coverColor.copy(alpha = 0.86f)
                    )
                )
        )

        // Tactile Woven Book Spine Crease on Left Edge
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.44f),
                            Color.White.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Subtle dark right-edge contour falloff (gives organic hardcover curvature with ZERO white stripe)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(10.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f)
                        )
                    )
                )
        )

        // Outer Luminous Glass Contour Rim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, brush = rimBrush, shape = bookShape)
        )

        // Editorial Monograph Typography & Telemetry Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 16.dp, top = if (slides.size > 1) 12.dp else 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Hairline Editorial Slideshow Progress Bars (only visible in multi-slide carousel modes)
                if (slides.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cover_carousel_bars_${summary.year}"),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        slides.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.26f))
                            ) {
                                val segmentFill = when {
                                    idx < safeSlideIndex -> 1f
                                    idx == safeSlideIndex -> if (isCarouselActive) slideProgress.value.coerceIn(0.08f, 1f) else 1f
                                    else -> 0f
                                }
                                if (segmentFill > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(segmentFill)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = if (idx == safeSlideIndex) 0.96f else 0.72f))
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "EDITION // ${summary.year % 100}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Text(
                            text = summary.year.toString(),
                            fontFamily = EditorialSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp,
                            lineHeight = 46.sp,
                            letterSpacing = (-1.0).sp,
                            color = Color.White
                        )
                    }

                    if (summary.unreviewedCount == 0 && summary.totalPhotos > 0) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldKeep,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "年度已整理",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(16.dp)
                            )
                        }
                    } else {
                        ObsidianGlassPill {
                            Text(
                                text = "${summary.unreviewedCount} 待理",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Frosted Obsidian Glass Archive Plate + Live Slide Context & Cover Customizer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Live Cover Slide Story Chip (or Video badge)
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        if (slides.size > 1 && displayedPhoto != null) {
                            ObsidianGlassPill {
                                AnimatedContent(
                                    targetState = displayedPhoto to safeSlideIndex,
                                    transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
                                    label = "cover_slide_meta"
                                ) { (slidePhoto, idx) ->
                                    val locationSuffix = slidePhoto.locationName?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                                    val slideLabel = if (locationSuffix.isNotEmpty()) {
                                        "${slidePhoto.month}月$locationSuffix"
                                    } else {
                                        "${slidePhoto.month}月精选 · ${idx + 1}/${slides.size}"
                                    }
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = SolarAmber,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = slideLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.5.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else if (summary.videoCount > 0) {
                            ObsidianGlassPill {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = SolarAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${summary.videoCount} 视频",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Right: Subtle 48dp-accessible Cover Studio Trigger (shown on focused book)
                    if (isFocused) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    onClickLabel = stringResource(R.string.cd_customize_cover),
                                    onClick = onCustomizeCover
                                )
                                .testTag("customize_cover_button_${summary.year}")
                        ) {
                            ObsidianGlassPill {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = stringResource(R.string.cd_customize_cover),
                                        tint = SolarAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_customize_cover),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                ObsidianGlassCard(
                    shape = RoundedCornerShape(14.dp),
                    topColor = Color(0x88141720),
                    bottomColor = Color(0xA60E1016),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${summary.cleanedMonthsCount}/${summary.totalMonthsCount} 月归档",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Text(
                                text = "${summary.totalPhotos} ITEMS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = SolarAmber
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f))
                        ) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = summary.progressFraction,
                                animationSpec = spring(),
                                label = "book_progress"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.White, SolarAmber)
                                        )
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.cover_tap_flip_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernYearWheel(
    years: List<Int>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, years.size) {
        if (selectedIndex in years.indices) {
            listState.animateScrollToItem(index = (selectedIndex - 1).coerceAtLeast(0))
        }
    }

    val activeYearBrush = activeChromeSatinBrush()
    val transparentBrush = remember {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    FrostedGlassCard(
        shape = RoundedCornerShape(50),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = if (years.size <= 4) Arrangement.SpaceEvenly else Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(years) { idx, year ->
                val selected = idx == selectedIndex
                val pillWidth by animateDpAsState(
                    targetValue = if (selected) 80.dp else 64.dp,
                    animationSpec = spring(),
                    label = "year_pill"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(pillWidth)
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) activeYearBrush else transparentBrush)
                        .clickable { onSelectIndex(idx) }
                        .testTag("year_wheel_$year")
                ) {
                    Text(
                        text = year.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (selected) 13.sp else 12.sp,
                        color = if (selected) Color.White else InkSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernMetricSlot(
    label: String,
    value: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = stringResource(R.string.cd_open_vault_metric),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyLibraryPermissionCard(
    accessLevel: MediaAccessLevel,
    isScanning: Boolean,
    activeFilter: MediaFilter,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit,
    onResetFilter: () -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrostedGlassCard(
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 8.dp,
            enableAmbientBlur = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CobaltSoft,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                strokeWidth = 2.6.dp,
                                color = CobaltPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (activeFilter == MediaFilter.VIDEOS) Icons.Filled.Videocam else Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = if (activeFilter == MediaFilter.VIDEOS) VioletVideo else CobaltPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                val title = when {
                    isScanning -> "正在扫描本地照片和视频..."
                    activeFilter != MediaFilter.ALL -> "当前筛选分类下暂无媒体"
                    accessLevel == MediaAccessLevel.NONE -> "读取全部本地照片与视频"
                    else -> "设备相册暂未检测到照片或视频"
                }

                val desc = when {
                    isScanning -> "正在按年份和月份自动整理您的相册与连拍组。"
                    activeFilter != MediaFilter.ALL -> "切换回“全部”即可查看您的完整年度影集。"
                    accessLevel == MediaAccessLevel.NONE -> "授权后，FlipYear 将自动在设备本地扫描全部照片和视频，生成 3D 年度影集并智能识别相似连拍，数据全程不上传云端。"
                    else -> "您可以先拍照/录屏后点击重新扫描，或加载精选示例影集（含 4K 视频与连拍）立即体验全部功能。"
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    activeFilter != MediaFilter.ALL -> {
                        GalleryActionButton(
                            text = "查看全部照片和视频",
                            tone = GalleryButtonTone.OBSIDIAN,
                            onClick = onResetFilter,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    accessLevel == MediaAccessLevel.NONE -> {
                        GalleryActionButton(
                            text = stringResource(R.string.action_grant_media),
                            leadingIcon = Icons.Filled.FolderOpen,
                            tone = GalleryButtonTone.OBSIDIAN,
                            onClick = onRequestPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_media_permission_button")
                        )
                        GalleryActionButton(
                            text = "先加载示例影集体验",
                            tone = GalleryButtonTone.GLASS,
                            onClick = onLoadDemo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_demo_archive_button")
                        )
                    }
                    else -> {
                        GalleryActionButton(
                            text = "重新扫描本地相册",
                            leadingIcon = Icons.Filled.Refresh,
                            tone = GalleryButtonTone.OBSIDIAN,
                            onClick = onRescan,
                            modifier = Modifier.fillMaxWidth()
                        )
                        GalleryActionButton(
                            text = stringResource(R.string.action_load_demo),
                            tone = GalleryButtonTone.GLASS,
                            onClick = onLoadDemo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_demo_archive_button")
                        )
                    }
                }
            }
        }
    }
}
