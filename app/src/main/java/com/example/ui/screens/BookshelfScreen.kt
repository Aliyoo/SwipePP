package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MediaAccessLevel
import com.example.data.YearbookCoverMode
import com.example.sensor.rememberHapticEngine
import com.example.ui.FlipYearUiState
import com.example.ui.LocalFloatingDockInset
import com.example.ui.MediaFilter
import com.example.ui.components.Adaptive3DBookshelfCarousel
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.ThemeAppearanceSheet
import com.example.ui.components.YearbookCoverStudioSheet
import com.example.ui.components.activeChromeSatinBrush
import com.example.ui.screens.bookshelf.CompactStudioMetricsBar
import com.example.ui.screens.bookshelf.EmptyLibraryPermissionCard
import com.example.ui.screens.bookshelf.ModernYearbook3DCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GlassLightSubtle
import com.example.ui.theme.GlassSpecularRimStart
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.isReducedMotionEnabled
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

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
    onRemoveDemoArchive: () -> Unit = {},
    onSaveYearbookCover: (year: Int, mode: YearbookCoverMode, selectedPhotoIds: List<Long>, intervalSeconds: Int) -> Unit = { _, _, _, _ -> },
    onResetYearbookCover: (year: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val books = uiState.yearbooks
    val haptics = rememberHapticEngine()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
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
                    .padding(horizontal = 20.dp, vertical = 10.dp),
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
                                text = { Text("授权全部照片与视频权限", color = InkPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = CobaltPrimary)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRequestMediaPermission()
                                }
                            )
                            if (uiState.hasSamplePhotos) {
                                DropdownMenuItem(
                                    text = { Text("移除示例影集", color = CoralDanger) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = CoralDanger)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onRemoveDemoArchive()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.hasSamplePhotos) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CobaltSoft,
                    border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "当前包含内置示例影集",
                            style = MaterialTheme.typography.labelMedium,
                            color = CobaltDark,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onRemoveDemoArchive,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("bookshelf_remove_demo_button")
                        ) {
                            Text(
                                text = "移除示例",
                                color = CoralDanger,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Compact Frosted-Glass Studio Metrics Strip (frees vertical height for 3D Bookshelf stage)
            CompactStudioMetricsBar(
                purgedBytes = uiState.stats.totalPurgedBytes,
                compressedSavedBytes = uiState.stats.totalCompressedSavedBytes,
                pendingTrashBytes = uiState.pendingTrashBytes,
                onOpenVault = onOpenVault
            )

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
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                Spacer(modifier = Modifier.height(8.dp))
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
                            .padding(3.dp),
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
                                    modifier = Modifier.padding(vertical = 6.dp)
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

            Spacer(modifier = Modifier.height(6.dp))

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
                // Scope PagerState cleanly per MediaFilter so switching All -> Videos -> Photos
                // never carries over stale page indices, scroll offsets, or key maps across filters.
                key(uiState.mediaFilter) {
                    val pagerState = rememberPagerState(initialPage = 0, pageCount = { books.size })

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }
                            .drop(1)
                            .distinctUntilChanged()
                            .collect {
                                haptics.bookshelfDetent()
                            }
                    }

                    val safeSettledIndex = pagerState.settledPage.coerceIn(0, books.lastIndex)
                    val settledBook = books[safeSettledIndex]
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
                        val isBookSettledCenter = kinematics.isCentered && safeSettledIndex == page
                        ModernYearbook3DCard(
                            summary = book,
                            isFocused = isBookSettledCenter,
                            isCarouselPaused = pagerState.isScrollInProgress || customizingYear != null || reducedMotion,
                            onCustomizeCover = {
                                haptics.pageFlipLift()
                                customizingYear = book.year
                            },
                            onActiveSlideChanged = { slidePhoto ->
                                if (safeSettledIndex == page && slidePhoto != null) {
                                    activeAmbientCoverPhoto = slidePhoto
                                }
                            },
                            onClick = {
                                if (pagerState.currentPage == page) {
                                    onOpenYearbook(book.year)
                                } else if (page in books.indices) {
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                }
                            },
                            modifier = cardModifier.testTag("yearbook_card_${book.year}")
                        )
                    }

                    // Active Year Wheel Scrubber + Clear Dual CTA (Flip Calendar + Quick Cleanup)
                    val safeCurrentIndex = pagerState.currentPage.coerceIn(0, books.lastIndex)
                    val activeBook = books[safeCurrentIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ModernYearWheel(
                            years = books.map { it.year },
                            selectedIndex = safeCurrentIndex,
                            onSelectIndex = { idx ->
                                if (idx in books.indices) {
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GalleryActionButton(
                                text = "翻阅月历",
                                leadingIcon = Icons.Filled.AutoStories,
                                tone = GalleryButtonTone.GLASS,
                                height = 48.dp,
                                onClick = {
                                    haptics.pageFlipLift()
                                    onOpenYearbook(activeBook.year)
                                },
                                modifier = Modifier
                                    .weight(0.42f)
                                    .testTag("open_flipbook_button")
                            )
                            GalleryActionButton(
                                text = if (activeBook.unreviewedCount > 0) {
                                    "清理 ${activeBook.year} · ${activeBook.unreviewedCount} 项"
                                } else {
                                    "重温 ${activeBook.year} · ${activeBook.totalPhotos} 项"
                                },
                                leadingIcon = if (activeBook.unreviewedCount > 0) Icons.Filled.CleaningServices else Icons.Filled.AutoAwesome,
                                tone = GalleryButtonTone.OBSIDIAN,
                                height = 48.dp,
                                onClick = {
                                    onStartYearCleanup(activeBook.year, activeBook.unreviewedCount == 0)
                                },
                                modifier = Modifier
                                    .weight(0.58f)
                                    .testTag("open_yearbook_button")
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = if (years.size <= 4) Arrangement.SpaceEvenly else Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(years) { idx, year ->
                val selected = idx == selectedIndex
                val pillWidth by animateDpAsState(
                    targetValue = if (selected) 78.dp else 62.dp,
                    animationSpec = spring(),
                    label = "year_pill"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(pillWidth)
                        .heightIn(min = 42.dp)
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
