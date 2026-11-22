package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.components.ThemeAppearanceSheet
import com.example.ui.components.YearbookCoverStudioSheet
import com.example.ui.components.formatStorageSize
import com.example.ui.screens.bookshelf.BookshelfStorageLedgerSheet
import com.example.ui.screens.bookshelf.EmptyLibraryPermissionCard
import com.example.ui.screens.bookshelf.ModernYearbook3DCard
import com.example.ui.screens.bookshelf.decoGanzhiOfYear
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DefaultStartTab
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.FlipbookDefaultView
import com.example.ui.theme.GalleryGridDensity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.UserPreferences
import com.example.ui.theme.isReducedMotionEnabled
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * 摩登画报 · 影集首屏：画报纸纹地面 + 编辑式顶部（品牌字标/可点存储账本 pill/主题·设置圆钮）
 * + 全部·照片·视频文本 tab 条 + 沉浸画册（3D 年度影集轮播 × 金双框画片 × 美术字大年份）
 * + 底部年历格年份选择（5 格、选中朱点）+ 朱红通栏双动作条（翻阅月历 / 进入整理）。
 *
 * 功能基线与主分支 87a2cd3 对齐：扫描、授权、部分授权提示、示例加载/移除、换封面工作室、
 * 存储账本、主题与设置 sheets、翻页触感、中央卡点击进 Flipbook 全部保留。
 */
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
    val palette = LocalFlipYearPalette.current
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showStorageLedger by remember { mutableStateOf(false) }
    var customizingYear by remember { mutableStateOf<Int?>(null) }

    val floatingDockInset = LocalFloatingDockInset.current
    val totalMediaCount = uiState.totalImageCount + uiState.totalVideoCount
    val totalFreedBytes = uiState.stats.totalPurgedBytes + uiState.stats.totalCompressedSavedBytes

    val groundInk = DecoInkOnGround
    val gold = DecoGold
    val verm = DecoVerm
    val mat = DecoMat

    // 摩登画报纸底：地面渐层（decoGround → decoGroundDeep）+ 极淡纸纹横丝
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.decoGround, palette.decoGroundDeep)
                )
            )
            .drawBehind {
                val paperLineStep = 4.dp.toPx()
                val paperLineColor = groundInk.copy(alpha = 0.016f)
                var y = 0f
                while (y < size.height) {
                    drawLine(paperLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += paperLineStep
                }
            }
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
            // 编辑式顶部：品牌字标 + 可点存储账本 pill + 主题/设置玻璃圆钮（基线保留，画报重绘）
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
                    uiState.pendingTrashBytes > 0L -> verm
                    totalFreedBytes > 0L -> gold
                    else -> gold.copy(alpha = 0.62f)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = EditorialSerifFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        letterSpacing = 0.6.sp,
                        color = groundInk
                    )

                    // 画报金线账本 pill：内衬纸底 + 金描边 + 主题墨字（入口与 testTag 基线不变）
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
                            color = mat.copy(alpha = 0.62f),
                            border = BorderStroke(1.dp, gold.copy(alpha = 0.55f)),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = groundInk.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = gold,
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
                            color = gold,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(18.dp)
                        )
                    }

                    // 画报金线圆钮（38dp 视觉 + 48dp 命中区，地面透底 + 1.5dp 金描边）：
                    // 替换旧磨砂玻璃圆钮，色取 DecoGold / DecoInkOnGround
                    DecoHeaderRoundSeal(
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
                            tint = gold,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    DecoHeaderRoundSeal(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("bookshelf_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            tint = groundInk,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // 内置示例影集提示条：朱红描边画报横条，移除示例为朱红方章动作（入口基线不变）
            if (uiState.hasSamplePhotos) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = mat.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, verm.copy(alpha = 0.55f)),
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp,
                            color = groundInk,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = DecoPaperCream)
                                ) { onRemoveDemoArchive() }
                                .testTag("bookshelf_remove_demo_button")
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "移除示例",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.14.sp,
                                color = DecoPaperCream
                            )
                        }
                    }
                }
            }

            // Partial access hint on Android 14+（入口基线不变，画报金线重绘）
            if (uiState.mediaAccessLevel == MediaAccessLevel.PARTIAL) {
                Surface(
                    onClick = onRequestMediaPermission,
                    shape = RoundedCornerShape(3.dp),
                    color = gold.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, gold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "当前仅授权部分媒体，点击开放全部本地相册",
                            fontSize = 12.sp,
                            letterSpacing = 0.2.sp,
                            color = groundInk.copy(alpha = 0.88f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "去授权",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.14.sp,
                            color = gold
                        )
                    }
                }
            }

            // 全部 / 照片 / 视频 文本 tab 条带：计数 + 金色动画下划线（基线保留，画报重绘）
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
                                targetValue = if (isSelected) groundInk else groundInk.copy(alpha = 0.52f),
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
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
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
                                        fontSize = if (isSelected) 14.5.sp else 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        letterSpacing = 0.14.sp,
                                        color = labelColor
                                    )
                                    Text(
                                        text = countSuffix,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) gold else groundInk.copy(alpha = 0.45f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(2.dp)
                                        .graphicsLayer { alpha = indicatorAlpha }
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(gold, gold.copy(alpha = 0.35f))
                                            )
                                        )
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.75.dp,
                        color = gold.copy(alpha = 0.30f)
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
                    val bookYears = remember(books) { books.map { it.year } }

                    Adaptive3DBookshelfCarousel(
                        items = books,
                        pagerState = pagerState,
                        settledCoverPhoto = settledBook.coverPhoto,
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
                            onActiveSlideChanged = {},
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

                    // 底部年历格年份选择 + 朱红通栏双动作条
                    val safeCurrentIndex = pagerState.currentPage.coerceIn(0, books.lastIndex)
                    val activeBook = books[safeCurrentIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 2.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DecoYearGrid(
                            years = bookYears,
                            selectedIndex = safeCurrentIndex,
                            onSelectIndex = { idx ->
                                if (idx in books.indices) {
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                            }
                        )

                        DecoVermilionActionStrip(
                            unreviewedCount = activeBook.unreviewedCount,
                            onBrowseMonths = {
                                haptics.pageFlipLift()
                                onOpenYearbook(activeBook.year)
                            },
                            onStartCleanup = {
                                haptics.pageFlipLift()
                                onStartYearCleanup(activeBook.year, activeBook.unreviewedCount == 0)
                            },
                            modifier = Modifier.fillMaxWidth()
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
 * 摩登画报年历格年份选择：5 格横排（年份 + 干支副标），选中格金框金晕 + 右上朱点。
 * 超过 5 年时自动转为可横滑格带并锚定选中项。testTag 沿用基线 year_wheel_$year。
 */
@Composable
private fun DecoYearGrid(
    years: List<Int>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val cellShape = RoundedCornerShape(3.dp)

    if (years.size <= 5) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            years.forEachIndexed { idx, year ->
                DecoYearCell(
                    year = year,
                    isSelected = idx == selectedIndex,
                    onSelect = { onSelectIndex(idx) },
                    shape = cellShape,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        val listState = rememberLazyListState()
        LaunchedEffect(selectedIndex, years.size) {
            if (selectedIndex in years.indices) {
                listState.animateScrollToItem(index = (selectedIndex - 1).coerceAtLeast(0))
            }
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(years) { idx, year ->
                DecoYearCell(
                    year = year,
                    isSelected = idx == selectedIndex,
                    onSelect = { onSelectIndex(idx) },
                    shape = cellShape,
                    modifier = Modifier.width(72.dp)
                )
            }
        }
    }
}

@Composable
private fun DecoYearCell(
    year: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val gold = DecoGold
    val verm = DecoVerm
    val mat = DecoMat
    val groundInk = DecoInkOnGround

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(shape)
                .background(if (isSelected) gold.copy(alpha = 0.16f) else mat.copy(alpha = 0.50f))
                .border(1.dp, if (isSelected) gold else gold.copy(alpha = 0.45f), shape)
                .clickable { onSelect() }
                .testTag("year_wheel_$year")
        ) {
            Text(
                text = year.toString(),
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.4.sp,
                color = if (isSelected) groundInk else groundInk.copy(alpha = 0.62f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = decoGanzhiOfYear(year),
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
                color = if (isSelected) groundInk.copy(alpha = 0.88f) else groundInk.copy(alpha = 0.55f)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(verm)
            )
        }
    }
}

/** 画报金线圆钮：38dp 正圆、地面透底、1.5dp 金描边 + 48dp 命中区（替换旧磨砂玻璃圆钮） */
@Composable
private fun DecoHeaderRoundSeal(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val gold = DecoGold
    val ground = DecoGround
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ground.copy(alpha = 0.55f))
            .border(1.5.dp, gold, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = gold),
                onClick = onClick
            )
    ) {
        content()
    }
}

/**
 * 摩登画报朱红通栏双动作条：主动作「翻阅月历」（20sp 纸黄，进 Flipbook）+
 * 中缝 1dp 金渐变 + 次动作（17sp，未清「进入整理」/已清「重温回忆」，启动年度清理）。
 * testTag open_yearbook_button 落在次动作上，与基线 CTA 的清理语义一致。
 */
@Composable
private fun DecoVermilionActionStrip(
    unreviewedCount: Int,
    onBrowseMonths: () -> Unit,
    onStartCleanup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = DecoGold
    val verm = DecoVerm
    val isCleanupMode = unreviewedCount > 0
    val stripShape = RoundedCornerShape(3.dp)

    Row(
        modifier = modifier
            .height(58.dp)
            .shadow(
                elevation = 12.dp,
                shape = stripShape,
                ambientColor = verm.copy(alpha = 0.45f),
                spotColor = verm.copy(alpha = 0.45f)
            )
            .clip(stripShape)
            .background(verm)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1.35f)
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = DecoPaperCream)
                ) { onBrowseMonths() }
        ) {
            Text(
                text = "翻阅月历",
                fontFamily = EditorialSerifFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                letterSpacing = 2.6.sp,
                color = DecoPaperCream
            )
        }

        // 中缝：1dp 金渐变（上透明 → 金 → 下透明）
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            gold.copy(alpha = 0.80f),
                            gold.copy(alpha = 0.80f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = DecoPaperCream)
                ) { onStartCleanup() }
                .testTag("open_yearbook_button")
        ) {
            Text(
                text = if (isCleanupMode) "进入整理" else "重温回忆",
                fontFamily = EditorialSerifFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                letterSpacing = 2.2.sp,
                color = DecoPaperCream.copy(alpha = 0.88f),
                maxLines = 1
            )
        }
    }
}
