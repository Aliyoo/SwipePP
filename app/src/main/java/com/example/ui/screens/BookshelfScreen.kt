package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.AppSettingsSheet
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.GalleryHeaderOrbButton
import com.example.ui.components.ThemeAppearanceSheet
import com.example.ui.components.YearbookCoverStudioSheet
import com.example.ui.components.activeChromeSatinBrush
import com.example.ui.components.formatStorageSize
import com.example.ui.screens.bookshelf.BookshelfStorageLedgerSheet
import com.example.ui.screens.bookshelf.EmptyLibraryPermissionCard
import com.example.ui.screens.bookshelf.ModernYearbook3DCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.DefaultStartTab
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.FlipbookDefaultView
import com.example.ui.theme.GalleryGridDensity
import com.example.ui.theme.GlassSpecularRimStart
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.UserPreferences
import com.example.ui.theme.isReducedMotionEnabled
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
fun BookshelfScreen(
    uiState: FlipYearUiState,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    userPreferences: UserPreferences = UserPreferences(themeMode = themeMode),
    onSelectThemeMode: (AppThemeMode) -> Unit = {},
    onSelectTimelineSortOrder: (TimelineSortOrder) -> Unit = {},
    onToggleShowEmptyMonths: (Boolean) -> Unit = {},
    onSelectFlipbookDefaultView: (FlipbookDefaultView) -> Unit = {},
    onSelectDefaultStartTab: (DefaultStartTab) -> Unit = {},
    onSelectGalleryGridDensity: (GalleryGridDensity) -> Unit = {},
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
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showStorageLedger by remember { mutableStateOf(false) }
    var customizingYear by remember { mutableStateOf<Int?>(null) }

    val floatingDockInset = LocalFloatingDockInset.current
    val totalMediaCount = uiState.totalImageCount + uiState.totalVideoCount
    val totalFreedBytes = uiState.stats.totalPurgedBytes + uiState.stats.totalCompressedSavedBytes

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
            // Crafted Editorial Top Header (Brand Wordmark + Tinted Storage Ledger Pill | Frosted Glass Orbs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val summaryText = when {
                    uiState.pendingTrashBytes > 0L ->
                        "待释放 ${formatStorageSize(uiState.pendingTrashBytes)}"
                    totalFreedBytes > 0L ->
                        "已释放 ${formatStorageSize(totalFreedBytes)}"
                    totalMediaCount > 0 ->
                        "$totalMediaCount 项回忆 · 账本"
                    else ->
                        "空间账本"
                }
                val dotAccent = when {
                    uiState.pendingTrashBytes > 0L -> CoralDanger
                    totalFreedBytes > 0L -> EmeraldKeep
                    else -> CobaltPrimary
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp,
                        color = InkPrimary
                    )

                    // Tactile hairline micro-pill for the on-demand storage ledger
                    Box(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .clip(CircleShape)
                            .clickable { showStorageLedger = true }
                            .testTag("bookshelf_storage_summary"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) SurfacePure.copy(alpha = 0.78f) else SurfacePure.copy(alpha = 0.92f),
                            border = BorderStroke(
                                0.85.dp,
                                if (isDark) HairlineBorder else GlassSpecularRimStart
                            ),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(dotAccent)
                                )
                                Text(
                                    text = summaryText,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = InkTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (uiState.isScanningDevice) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = CobaltPrimary,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(18.dp)
                        )
                    }

                    GalleryHeaderOrbButton(
                        onClick = { showThemeSheet = true },
                        modifier = Modifier.testTag("bookshelf_theme_button")
                    ) {
                        Icon(
                            imageVector = when (themeMode) {
                                AppThemeMode.SYSTEM -> Icons.Filled.Contrast
                                AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                AppThemeMode.DARK -> Icons.Filled.DarkMode
                            },
                            contentDescription = stringResource(R.string.cd_theme_switcher),
                            tint = if (isDark) SolarAmber else InkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    GalleryHeaderOrbButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("bookshelf_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            tint = InkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
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
                        .padding(top = 4.dp, bottom = 2.dp)
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
                        .padding(top = 4.dp)
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

            // Frameless Minimalist Text Tab Strip anchored on an architectural hairline baseline
            if (totalMediaCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        MediaFilter.entries.forEach { filter ->
                            val isSelected = uiState.mediaFilter == filter
                            val countSuffix = when (filter) {
                                MediaFilter.ALL -> "$totalMediaCount"
                                MediaFilter.PHOTOS -> "${uiState.totalImageCount}"
                                MediaFilter.VIDEOS -> "${uiState.totalVideoCount}"
                            }
                            val labelColor by animateColorAsState(
                                targetValue = if (isSelected) InkPrimary else InkTertiary,
                                animationSpec = tween(180),
                                label = "media_tab_color"
                            )
                            val indicatorAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0f,
                                animationSpec = tween(180),
                                label = "media_tab_indicator_alpha"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Tab
                                        selected = isSelected
                                    }
                                    .clickable(role = Role.Tab) { onSelectFilter(filter) }
                                    .testTag("filter_${filter.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(bottom = 9.dp)
                                ) {
                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = if (isSelected) 14.5.sp else 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = labelColor
                                    )
                                    Text(
                                        text = countSuffix,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            if (isDark) SolarAmber else CobaltPrimary
                                        } else {
                                            InkTertiary.copy(alpha = 0.72f)
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(2.5.dp)
                                        .graphicsLayer { alpha = indicatorAlpha }
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) {
                                                Brush.horizontalGradient(listOf(SolarAmber, Color(0xFFFFD580)))
                                            } else {
                                                Brush.horizontalGradient(listOf(CobaltPrimary, Color(0xFF60A5FA)))
                                            }
                                        )
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.75.dp,
                        color = HairlineBorder.copy(alpha = if (isDark) 0.55f else 0.72f)
                    )
                }
            }

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
                // Scope PagerState cleanly per MediaFilter and global TimelineSortOrder so switching
                // filters or reversing chronology cleanly anchors to index 0 without stale offsets.
                key(uiState.mediaFilter, uiState.timelineSortOrder) {
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
                                    haptics.pageFlipLift()
                                    onOpenYearbook(book.year)
                                } else if (page in books.indices) {
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                }
                            },
                            modifier = cardModifier.testTag("yearbook_card_${book.year}")
                        )
                    }

                    // Precision Analog Year Dial + Single Sculpted Annual Cleanup CTA (Cover Tap = Flipbook)
                    val safeCurrentIndex = pagerState.currentPage.coerceIn(0, books.lastIndex)
                    val activeBook = books[safeCurrentIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 2.dp, bottom = 10.dp),
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

                        AnnualCleanupHeroButton(
                            year = activeBook.year,
                            unreviewedCount = activeBook.unreviewedCount,
                            totalPhotos = activeBook.totalPhotos,
                            onClick = {
                                haptics.pageFlipLift()
                                onStartYearCleanup(activeBook.year, activeBook.unreviewedCount == 0)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_yearbook_button")
                        )
                    }
                }
            }
        }
    }

    if (showStorageLedger) {
        BookshelfStorageLedgerSheet(
            purgedBytes = uiState.stats.totalPurgedBytes,
            purgedCount = uiState.stats.totalPurgedCount,
            compressedSavedBytes = uiState.stats.totalCompressedSavedBytes,
            compressedCount = uiState.stats.totalCompressedCount,
            pendingTrashBytes = uiState.pendingTrashBytes,
            pendingTrashCount = uiState.trashedPhotos.size,
            onOpenVault = onOpenVault,
            onDismiss = { showStorageLedger = false }
        )
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

    if (showSettingsSheet) {
        AppSettingsSheet(
            preferences = userPreferences,
            mediaAccessLevel = uiState.mediaAccessLevel,
            totalImageCount = uiState.totalImageCount,
            totalVideoCount = uiState.totalVideoCount,
            isScanningDevice = uiState.isScanningDevice,
            hasSamplePhotos = uiState.hasSamplePhotos,
            onSelectTimelineSortOrder = onSelectTimelineSortOrder,
            onToggleShowEmptyMonths = onToggleShowEmptyMonths,
            onSelectFlipbookDefaultView = onSelectFlipbookDefaultView,
            onSelectDefaultStartTab = onSelectDefaultStartTab,
            onSelectGalleryGridDensity = onSelectGalleryGridDensity,
            onSelectThemeMode = onSelectThemeMode,
            onRescanDeviceMedia = onRescanDeviceMedia,
            onRequestMediaPermission = onRequestMediaPermission,
            onRemoveDemoArchive = onRemoveDemoArchive,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

/**
 * Flagship sculpted full-width CTA dedicated solely to launching the selected year's swipe cleanup
 * (or annual review when already cleaned), pairing satin obsidian craftsmanship with an embedded
 * monospace count badge.
 */
@Composable
private fun AnnualCleanupHeroButton(
    year: Int,
    unreviewedCount: Int,
    totalPhotos: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val palette = LocalFlipYearPalette.current
    val activeBrush = activeChromeSatinBrush()
    val isCleanupMode = unreviewedCount > 0
    val shape = RoundedCornerShape(18.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_cleanup_scale"
    )

    val borderBrush = remember(palette) {
        Brush.linearGradient(
            colors = listOf(
                palette.activeChromeBorderStart,
                Color.White.copy(alpha = 0.14f),
                palette.activeChromeBorderEnd
            )
        )
    }
    val shadowColor = (if (isDark) CobaltPrimary else InkPrimary).copy(alpha = if (isDark) 0.42f else 0.32f)

    Box(
        modifier = modifier
            .heightIn(min = 54.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (pressed) 5.dp else 12.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .drawBehind {
                drawRect(brush = activeBrush)
                // Sculpted champagne-tinted specular top highlight
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(0f, 0.8.dp.toPx()),
                    end = Offset(size.width, 0.8.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SolarAmber.copy(alpha = 0.18f))
                        .border(0.75.dp, SolarAmber.copy(alpha = 0.36f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isCleanupMode) Icons.Filled.CleaningServices else Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = if (isCleanupMode) {
                        "直接开始 $year 年度清理"
                    } else {
                        "重温 $year 年度回忆"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Embedded precision monospace count pill + forward arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.13f),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = if (isCleanupMode) "待清 $unreviewedCount" else "$totalPhotos 项",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCleanupMode) Color(0xFFFFDF9E) else Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Frameless precision watchmaker/camera-aperture year dial with graduated calibration ticks
 * and a luminous active index needle.
 */
@Composable
private fun ModernYearWheel(
    years: List<Int>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, years.size) {
        if (selectedIndex in years.indices) {
            listState.animateScrollToItem(index = (selectedIndex - 1).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = if (years.size <= 5) Arrangement.SpaceEvenly else Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(years) { idx, year ->
            val isSelected = idx == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (isSelected) InkPrimary else InkTertiary,
                animationSpec = tween(180),
                label = "year_wheel_color"
            )
            val tickColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) SolarAmber else CobaltPrimary
                } else {
                    HairlineBorder.copy(alpha = if (isDark) 0.55f else 0.85f)
                },
                animationSpec = tween(180),
                label = "year_wheel_tick"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectIndex(idx) }
                    .padding(horizontal = 8.dp)
                    .testTag("year_wheel_$year")
            ) {
                Text(
                    text = year.toString(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = if (isSelected) 16.sp else 12.5.sp,
                    letterSpacing = if (isSelected) 0.4.sp else 0.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(HairlineBorder.copy(alpha = 0.55f))
                    )
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 2.5.dp else 1.dp)
                            .height(if (isSelected) 9.dp else 5.dp)
                            .clip(CircleShape)
                            .background(tickColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(HairlineBorder.copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}
