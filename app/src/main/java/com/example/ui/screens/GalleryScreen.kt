package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CompressionPreset
import com.example.data.ImageFormat
import com.example.data.MediaSubtype
import com.example.data.PhotoItemEntity
import com.example.data.ResolvedGeoLocation
import com.example.data.SameDayLocationMatch
import com.example.data.inferSameDayLocations
import com.example.sensor.rememberHapticEngine
import com.example.ui.DateRangeFilter
import com.example.ui.FlipYearUiState
import com.example.ui.GalleryEntry
import com.example.ui.LocalFloatingDockInset
import com.example.ui.MediaFilter
import com.example.ui.TimelineSearchQuery
import com.example.ui.components.BatchCompressBottomSheet
import com.example.ui.components.BatchLocationEditorSheet
import com.example.ui.components.BurstGalleryGrid
import com.example.ui.components.DecoIcon
import com.example.ui.components.DecoIconKind
import com.example.ui.components.DecoPaperGround
import com.example.ui.components.DecoSealButton
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GallerySmartFilter
import com.example.ui.components.GalleryZoomTier
import com.example.ui.components.GalleryZoomTierIconButton
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.TimelineDateRangePickerDialog
import com.example.ui.components.TimelineGlobalSearchBar
import com.example.ui.components.TimelineSearchFilterSheet
import com.example.ui.components.defaultGalleryZoomTier
import com.example.ui.countDescriptiveTagMatches
import com.example.ui.extractDescriptiveKeywordTags
import com.example.ui.filterGalleryEntriesBySearch
import com.example.ui.filterTreasuredGalleryEntries
import com.example.ui.parseTimelineKeyword
import com.example.ui.screens.gallery.BulkMoveDestinationSheet
import com.example.ui.screens.gallery.FloatingBulkActionBar
import com.example.ui.screens.gallery.GalleryMoreBatchActionsSheet
import com.example.ui.screens.gallery.GallerySelectionTopBar
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoGroundDeep
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoDisplayFontFamily
import kotlinx.coroutines.delay
import java.util.Calendar

private const val LARGE_PHOTO_THRESHOLD_BYTES = 3_000_000L

/* ---- 摩登画报 · 画廊屏一次性装饰（design-demos/deco-app.html 画廊屏语言） ---- */
/** 栏目线金渐变（跟随主题金令牌，浅暗两套都成立） */
@Composable
private fun galleryMonthRuleBrush(): Brush = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.0f to DecoGold.copy(alpha = 0f),
        0.15f to DecoGold.copy(alpha = 0.66f),
        0.85f to DecoGold.copy(alpha = 0.66f),
        1.0f to DecoGold.copy(alpha = 0f)
    )
)
private val GalleryTileOutline = RoundedCornerShape(3.dp)
private val GallerySealShape = RoundedCornerShape(3.dp)
private const val DECO_FULL_ART_SHADOW_MIN_SP = 36f

/**
 * 美术字标题：DecoDisplay Bold + artShadow 三层错位影（随字号等比：
 * 黑 0.024em → 朱红 0.049em → 软模糊 0.073em，同 VaultArtText，deco-app.html:140 artShadow）。
 */
@Composable
private fun DecoArtTitle(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val verm = DecoVerm
    val density = LocalDensity.current
    fun emOffset(ratio: Float): androidx.compose.ui.unit.Dp = with(fontSize) { value * ratio }.dp
    val blurShadow = remember(fontSize, density) {
        Shadow(
            color = Color.Black.copy(alpha = 0.35f),
            offset = with(density) { Offset(emOffset(0.073f).toPx(), emOffset(0.073f).toPx()) },
            blurRadius = with(density) { emOffset(0.17f).toPx() }
        )
    }
    val vermOffset = remember(fontSize, density) { emOffset(0.049f) }
    val inkOffset = remember(fontSize, density) { emOffset(0.024f) }
    val baseStyle = TextStyle(
        fontFamily = DecoDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        letterSpacing = 0.8.sp
    )
    // 三层错位影只给 ≥36sp 展示级大字；常规页眉仅保留一层清爽朱红错位（无黑影、无模糊），
    // 避免浅色纸地上中小字号叠影发虚发脏
    val fullArtShadow = fontSize.value >= DECO_FULL_ART_SHADOW_MIN_SP
    Box(modifier) {
        // 错位阴影层对无障碍隐藏，仅顶层正稿参与语义（同 VaultArtText）
        if (fullArtShadow) {
            Text(
                text = text,
                color = Color.Transparent,
                style = baseStyle.copy(shadow = blurShadow),
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        Text(
            text = text,
            color = verm.copy(alpha = if (fullArtShadow) 0.60f else 0.42f),
            style = baseStyle,
            modifier = Modifier
                .offset(x = vermOffset, y = vermOffset)
                .clearAndSetSemantics { }
        )
        if (fullArtShadow) {
            Text(
                text = text,
                color = Color.Black.copy(alpha = 0.55f),
                style = baseStyle,
                modifier = Modifier
                    .offset(x = inkOffset, y = inkOffset)
                    .clearAndSetSemantics { }
            )
        }
        Text(
            text = text,
            color = DecoInkOnGround,
            style = baseStyle
        )
    }
}

/** 金边跳签（选中金底 + 内衬字，未选中金描边透明底） */
@Composable
private fun GalleryDecoJumpTag(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = if (selected) DecoGold else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) DecoGold else DecoGold.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

// 方章按钮收敛到共享组件 com.example.ui.components.DecoSealButton（deco-app.html SealBtn）


private inline fun GalleryEntry.anyPhoto(predicate: (PhotoItemEntity) -> Boolean): Boolean {
    return when (this) {
        is GalleryEntry.Single -> predicate(photo)
        is GalleryEntry.Burst -> photos.any(predicate)
    }
}

internal fun filterGalleryEntriesBySmartAndAlbum(
    allEntries: List<GalleryEntry>,
    smartFilter: GallerySmartFilter,
    selectedAlbum: String?,
    burstEntries: List<GalleryEntry.Burst>? = null,
    treasuredEntries: List<GalleryEntry>? = null,
    largePhotoEntries: List<GalleryEntry>? = null,
    missingLocationEntries: List<GalleryEntry>? = null
): List<GalleryEntry> {
    val base = when (smartFilter) {
        GallerySmartFilter.ALL -> allEntries
        GallerySmartFilter.TREASURED -> treasuredEntries ?: filterTreasuredGalleryEntries(allEntries)
        GallerySmartFilter.BURSTS -> burstEntries ?: allEntries.filterIsInstance<GalleryEntry.Burst>()
        GallerySmartFilter.LARGE_PHOTOS -> largePhotoEntries ?: allEntries.filter { entry ->
            when (entry) {
                is GalleryEntry.Single -> !entry.photo.isVideo && entry.photo.fileSizeBytes >= LARGE_PHOTO_THRESHOLD_BYTES
                is GalleryEntry.Burst -> entry.photos.any { !it.isVideo && it.fileSizeBytes >= LARGE_PHOTO_THRESHOLD_BYTES }
            }
        }
        GallerySmartFilter.MISSING_LOCATION -> missingLocationEntries ?: allEntries.filter { entry ->
            when (entry) {
                is GalleryEntry.Single ->
                    entry.photo.locationName.isNullOrBlank() || entry.photo.latitude == null || entry.photo.longitude == null
                is GalleryEntry.Burst ->
                    entry.photos.any { it.locationName.isNullOrBlank() || it.latitude == null || it.longitude == null }
            }
        }
        GallerySmartFilter.LIVE_PHOTOS -> allEntries.filter { entry -> entry.anyPhoto { it.isLivePhoto } }
        GallerySmartFilter.ANIMATED_IMAGES -> allEntries.filter { entry -> entry.anyPhoto { it.isAnimated } }
        GallerySmartFilter.SCREENSHOTS -> allEntries.filter { entry ->
            entry.anyPhoto {
                it.mediaSubtypeKind in setOf(MediaSubtype.SCREENSHOT, MediaSubtype.LONG_SCREENSHOT, MediaSubtype.SCREEN_RECORD)
            }
        }
        GallerySmartFilter.SPECIAL_FORMATS -> allEntries.filter { entry ->
            entry.anyPhoto { it.imageFormatKind in setOf(ImageFormat.HEIC, ImageFormat.AVIF, ImageFormat.RAW) }
        }
    }
    return if (selectedAlbum == null) {
        base
    } else {
        base.filter { entry ->
            when (entry) {
                is GalleryEntry.Single -> entry.photo.bucketName == selectedAlbum
                is GalleryEntry.Burst -> entry.photos.any { it.bucketName == selectedAlbum }
            }
        }
    }
}

@Composable
fun GalleryScreen(
    uiState: FlipYearUiState,
    onSelectBestInBurst: (groupId: String, photoId: Long) -> Unit,
    onKeepBestInBurst: (GalleryEntry.Burst, PhotoItemEntity) -> Unit,
    onKeepPhoto: (PhotoItemEntity, Boolean) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onLoadDemoArchive: () -> Unit,
    onBulkTrashPhotos: (List<PhotoItemEntity>) -> Unit = {},
    onBulkKeepPhotos: (List<PhotoItemEntity>, Boolean) -> Unit = { _, _ -> },
    onBulkMovePhotos: (List<PhotoItemEntity>, String?, Int?, Int?) -> Unit = { _, _, _, _ -> },
    onBatchCompress: (List<PhotoItemEntity>, CompressionPreset) -> Unit = { _, _ -> },
    onBatchManualLocation: (List<PhotoItemEntity>, String, Double, Double, Boolean) -> Unit = { _, _, _, _, _ -> },
    onBatchSameDayLocation: (List<SameDayLocationMatch>, Boolean) -> Unit = { _, _ -> },
    onGeocodeLocation: suspend (String) -> ResolvedGeoLocation? = { null },
    onComputeSemanticMatches: suspend (String) -> Map<Long, Float> = { emptyMap() },
    onSelectionModeChanged: (Boolean) -> Unit = {},
    onToggleFavorite: (PhotoItemEntity, Boolean) -> Unit = { p, f -> onKeepPhoto(p, f) },
    onStartCustomCleanup: (title: String, photoIds: Set<Long>, includeReviewed: Boolean) -> Unit = { _, _, _ -> },
    onStartMonthCleanup: (year: Int, month: Int, includeReviewed: Boolean) -> Unit = { _, _, _ -> },
    onResetGlobalMediaFilter: () -> Unit = {},
    onConsumeTreasureGalleryRequest: () -> Unit = {},
    galleryColumns: Int = 3,
    modifier: Modifier = Modifier
) {
    val hapticEngine = rememberHapticEngine()
    var expandedBurstIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var smartFilter by remember { mutableStateOf(GallerySmartFilter.ALL) }
    var selectedAlbumFilter by remember { mutableStateOf<String?>(null) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    var galleryZoomTier by rememberSaveable { mutableStateOf(defaultGalleryZoomTier(galleryColumns)) }
    LaunchedEffect(galleryColumns) {
        if (!galleryZoomTier.isWaterfall) {
            galleryZoomTier = defaultGalleryZoomTier(galleryColumns)
        }
    }

    var explicitSelectionMode by remember { mutableStateOf(false) }
    var selectedPhotoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showMoreActionsSheet by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showCompressSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }

    var rawSearchKeyword by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf(TimelineSearchQuery()) }
    var showSearchFilterSheet by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var pendingPickerInitialRange by remember { mutableStateOf<DateRangeFilter?>(null) }

    LaunchedEffect(rawSearchKeyword) {
        if (rawSearchKeyword.isBlank()) {
            if (searchQuery.keyword.isNotEmpty()) {
                searchQuery = searchQuery.copy(keyword = "")
            }
        } else {
            delay(110L)
            searchQuery = searchQuery.copy(keyword = rawSearchKeyword)
        }
    }

    LaunchedEffect(uiState.pendingTreasureGalleryRequest) {
        val request = uiState.pendingTreasureGalleryRequest ?: return@LaunchedEffect
        explicitSelectionMode = false
        selectedPhotoIds = emptySet()
        smartFilter = GallerySmartFilter.TREASURED
        selectedAlbumFilter = null
        rawSearchKeyword = ""
        searchQuery = if (request.year != null && request.month != null) {
            TimelineSearchQuery(
                dateRange = DateRangeFilter(
                    startDateKey = DateRangeFilter.encodeDateKey(request.year, request.month, 1),
                    endDateKey = DateRangeFilter.encodeDateKey(request.year, request.month, 31),
                    presetLabel = "${request.year}年${request.month}月"
                )
            )
        } else {
            TimelineSearchQuery()
        }
        onResetGlobalMediaFilter()
        onConsumeTreasureGalleryRequest()
    }

    val resetSearchAndFilters = remember {
        {
            rawSearchKeyword = ""
            searchQuery = TimelineSearchQuery()
            smartFilter = GallerySmartFilter.ALL
            selectedAlbumFilter = null
        }
    }

    val isSelectionMode = explicitSelectionMode || selectedPhotoIds.isNotEmpty()

    val exitSelectionMode = remember {
        {
            explicitSelectionMode = false
            selectedPhotoIds = emptySet()
        }
    }

    BackHandler(enabled = isSelectionMode) {
        exitSelectionMode()
    }

    BackHandler(
        enabled = !isSelectionMode && (
            searchQuery.isActive ||
                smartFilter != GallerySmartFilter.ALL ||
                selectedAlbumFilter != null
            )
    ) {
        resetSearchAndFilters()
    }

    LaunchedEffect(isSelectionMode) {
        onSelectionModeChanged(isSelectionMode)
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChanged(false) }
    }

    val allEntries = uiState.galleryEntries
    val precomputed = uiState.galleryIndex.takeIf { it.matches(allEntries) }

    val existingAlbums = precomputed?.existingAlbums ?: remember(allEntries) {
        val counts = HashMap<String, Int>(16)
        for (entry in allEntries) {
            when (entry) {
                is GalleryEntry.Single -> {
                    val name = entry.photo.bucketName?.trim()
                    if (!name.isNullOrEmpty()) counts[name] = (counts[name] ?: 0) + 1
                }
                is GalleryEntry.Burst -> {
                    for (photo in entry.photos) {
                        val name = photo.bucketName?.trim()
                        if (!name.isNullOrEmpty()) counts[name] = (counts[name] ?: 0) + 1
                    }
                }
            }
        }
        counts.entries.sortedByDescending { it.value }.map { it.key }
    }

    val knownLocationNames = remember(uiState.locationClusters) {
        uiState.locationClusters.map { it.locationName }
    }

    val parsedSearchKeyword = remember(searchQuery.keyword, knownLocationNames, existingAlbums) {
        parseTimelineKeyword(
            rawKeyword = searchQuery.keyword,
            knownLocations = knownLocationNames,
            knownAlbums = existingAlbums
        )
    }

    val semanticScores by produceState<Map<Long, Float>>(
        emptyMap(),
        parsedSearchKeyword.semanticQuery,
        uiState.smartTagRevision
    ) {
        val visualQuery = parsedSearchKeyword.semanticQuery
        if (visualQuery.isBlank()) {
            value = emptyMap()
        } else {
            // Debounce past keystroke settling; only the clean visual residual (stripped of
            // dates, tag qualifiers, and known locations) is sent to the Chinese-CLIP text tower.
            delay(280L)
            value = onComputeSemanticMatches(visualQuery)
        }
    }

    LaunchedEffect(existingAlbums, selectedAlbumFilter) {
        if (selectedAlbumFilter != null && selectedAlbumFilter !in existingAlbums) {
            selectedAlbumFilter = null
        }
    }

    val burstEntries = precomputed?.burstEntries ?: remember(allEntries) {
        allEntries.filterIsInstance<GalleryEntry.Burst>()
    }
    val treasuredEntries = precomputed?.treasuredEntries ?: remember(allEntries) {
        filterTreasuredGalleryEntries(allEntries)
    }
    val treasuredMediaCount = remember(treasuredEntries) {
        treasuredEntries.sumOf { entry ->
            when (entry) {
                is GalleryEntry.Single -> 1
                is GalleryEntry.Burst -> entry.photos.size
            }
        }
    }
    val largePhotoEntries = precomputed?.largePhotoEntries ?: remember(allEntries) {
        allEntries.filter { entry ->
            when (entry) {
                is GalleryEntry.Single -> !entry.photo.isVideo && entry.photo.fileSizeBytes >= LARGE_PHOTO_THRESHOLD_BYTES
                is GalleryEntry.Burst -> entry.photos.any { !it.isVideo && it.fileSizeBytes >= LARGE_PHOTO_THRESHOLD_BYTES }
            }
        }
    }
    val missingLocationEntries = precomputed?.missingLocationEntries ?: remember(allEntries) {
        allEntries.filter { entry ->
            when (entry) {
                is GalleryEntry.Single ->
                    entry.photo.locationName.isNullOrBlank() || entry.photo.latitude == null || entry.photo.longitude == null
                is GalleryEntry.Burst ->
                    entry.photos.any { it.locationName.isNullOrBlank() || it.latitude == null || it.longitude == null }
            }
        }
    }

    val livePhotoEntries = remember(allEntries) {
        allEntries.filter { entry -> entry.anyPhoto { it.isLivePhoto } }
    }
    val animatedImageEntries = remember(allEntries) {
        allEntries.filter { entry -> entry.anyPhoto { it.isAnimated } }
    }
    val screenshotEntries = remember(allEntries) {
        allEntries.filter { entry ->
            entry.anyPhoto {
                it.mediaSubtypeKind in setOf(MediaSubtype.SCREENSHOT, MediaSubtype.LONG_SCREENSHOT, MediaSubtype.SCREEN_RECORD)
            }
        }
    }
    val specialFormatEntries = remember(allEntries) {
        allEntries.filter { entry ->
            entry.anyPhoto { it.imageFormatKind in setOf(ImageFormat.HEIC, ImageFormat.AVIF, ImageFormat.RAW) }
        }
    }

    val extractedKeywordTags = precomputed?.extractedKeywordTags ?: remember(allEntries) {
        extractDescriptiveKeywordTags(allEntries)
    }

    val descriptiveTagCounts = remember(allEntries) {
        countDescriptiveTagMatches(allEntries)
    }

    val chipFilteredEntries = remember(
        allEntries,
        burstEntries,
        largePhotoEntries,
        missingLocationEntries,
        smartFilter,
        selectedAlbumFilter
    ) {
        filterGalleryEntriesBySmartAndAlbum(
            allEntries = allEntries,
            smartFilter = smartFilter,
            selectedAlbum = selectedAlbumFilter,
            burstEntries = burstEntries,
            treasuredEntries = treasuredEntries,
            largePhotoEntries = largePhotoEntries,
            missingLocationEntries = missingLocationEntries
        )
    }

    val displayedEntries = remember(chipFilteredEntries, searchQuery, semanticScores) {
        filterGalleryEntriesBySearch(chipFilteredEntries, searchQuery, semanticScores)
    }

    val isRelevanceRanked = searchQuery.keyword.isNotBlank() &&
        (parsedSearchKeyword.visualTokens.isNotEmpty() || parsedSearchKeyword.locationOrAlbumTokens.isNotEmpty())

    val displayedPhotos = remember(displayedEntries, isSelectionMode) {
        if (!isSelectionMode) {
            emptyList()
        } else {
            val result = ArrayList<PhotoItemEntity>(displayedEntries.size)
            for (entry in displayedEntries) {
                when (entry) {
                    is GalleryEntry.Single -> result.add(entry.photo)
                    is GalleryEntry.Burst -> result.addAll(entry.photos)
                }
            }
            result
        }
    }
    val displayedPhotoMap = remember(displayedPhotos) {
        if (displayedPhotos.isEmpty()) emptyMap() else displayedPhotos.associateBy { it.id }
    }

    LaunchedEffect(displayedPhotoMap, isSelectionMode) {
        if (isSelectionMode && selectedPhotoIds.isNotEmpty()) {
            val valid = selectedPhotoIds.filterTo(mutableSetOf()) { it in displayedPhotoMap }
            if (valid.size != selectedPhotoIds.size) {
                selectedPhotoIds = valid
            }
        }
    }

    val selectedPhotos = remember(selectedPhotoIds, displayedPhotoMap) {
        if (selectedPhotoIds.isEmpty()) emptyList() else selectedPhotoIds.mapNotNull { displayedPhotoMap[it] }
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
    val allDisplayedSelected = isSelectionMode && displayedPhotos.isNotEmpty() && selectedPhotos.size == displayedPhotos.size

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

    val longPressSelectPhotos = remember(hapticEngine) {
        { photos: List<PhotoItemEntity> ->
            hapticEngine.bookshelfDetent()
            explicitSelectionMode = true
            selectedPhotoIds = selectedPhotoIds + photos.map { it.id }
        }
    }

    val onToggleBurstStable = remember {
        { gid: String ->
            expandedBurstIds = if (gid in expandedBurstIds) {
                expandedBurstIds - gid
            } else {
                expandedBurstIds + gid
            }
        }
    }

    val onKeepBestInBurstStable = remember(onKeepBestInBurst) {
        { burst: GalleryEntry.Burst, chosen: PhotoItemEntity ->
            expandedBurstIds = expandedBurstIds - burst.groupId
            onKeepBestInBurst(burst, chosen)
        }
    }

    val onPhotoClickStable = remember {
        { photo: PhotoItemEntity ->
            lightboxPhoto = photo
        }
    }

    val allBurstsExpanded = burstEntries.isNotEmpty() &&
        expandedBurstIds.size >= burstEntries.size &&
        burstEntries.all { it.groupId in expandedBurstIds }
    val floatingDockInset = LocalFloatingDockInset.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val effectiveBottomInset = if (isSelectionMode) 96.dp else floatingDockInset

    // 摩登画报 · 画报地面收敛到共享 DecoPaperGround（groundA→groundB 渐变 + 纸纹 alpha 0.016 统一）
    DecoPaperGround(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter)
            ) {
                AnimatedContent(
                    targetState = isSelectionMode,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically(tween(200)) { -it / 4 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically(tween(160)) { -it / 4 })
                    },
                    label = "gallery_header_mode"
                ) { selecting ->
                    if (selecting) {
                        GallerySelectionTopBar(
                            selectedCount = selectedPhotos.size,
                            selectedBytes = selectedBytes,
                            allSelected = allDisplayedSelected,
                            onCancel = {
                                hapticEngine.pageFlipLift()
                                exitSelectionMode()
                            },
                            onToggleSelectAll = {
                                hapticEngine.bookshelfDetent()
                                selectedPhotoIds = if (allDisplayedSelected) {
                                    emptySet()
                                } else {
                                    displayedPhotos.map { it.id }.toSet()
                                }
                            }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DecoArtTitle(
                                        text = stringResource(R.string.nav_gallery),
                                        fontSize = 30.sp
                                    )
                                    Text(
                                        text = if (burstEntries.isNotEmpty()) {
                                            "共 ${displayedEntries.size} 项 · 含 ${burstEntries.size} 组连拍 · 双指缩放"
                                        } else {
                                            "共 ${displayedEntries.size} 项 · 双指缩放换布局 · 长按多选"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        color = DecoInkOnGround.copy(alpha = 0.66f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (displayedEntries.isNotEmpty()) {
                                    val isFilteredView = selectedAlbumFilter != null ||
                                        smartFilter != GallerySmartFilter.ALL ||
                                        searchQuery.isActive
                                    val multiselectAccessibilityDesc = stringResource(R.string.action_enter_multiselect)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // One-tap Gallery -> ArcCleaner bridge with clear context-aware label
                                        Surface(
                                            onClick = {
                                                hapticEngine.pageFlipLift()
                                                val ids = LinkedHashSet<Long>(displayedEntries.size)
                                                for (entry in displayedEntries) {
                                                    when (entry) {
                                                        is GalleryEntry.Single -> ids.add(entry.photo.id)
                                                        is GalleryEntry.Burst -> entry.photos.forEach { ids.add(it.id) }
                                                    }
                                                }
                                                val sessionTitle = when {
                                                    selectedAlbumFilter != null -> "整理「$selectedAlbumFilter」"
                                                    smartFilter != GallerySmartFilter.ALL -> "整理 · ${smartFilter.label}"
                                                    searchQuery.keyword.isNotBlank() -> "整理 · ${searchQuery.keyword.trim()}"
                                                    searchQuery.isActive -> "整理筛选结果"
                                                    else -> "整理画廊媒体"
                                                }
                                                onStartCustomCleanup(sessionTitle, ids, true)
                                            },
                                            shape = GallerySealShape,
                                            color = Color.Transparent,
                                            border = BorderStroke(1.5.dp, DecoGold.copy(alpha = 0.85f)),
                                            modifier = Modifier
                                                .heightIn(min = 38.dp)
                                                .testTag("gallery_swipe_clean_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                DecoIcon(
                                                    kind = DecoIconKind.CLEAN,
                                                    color = DecoGold,
                                                    iconSize = 14.dp
                                                )
                                                Text(
                                                    text = if (isFilteredView) "整理当前结果" else "滑动整理",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontFamily = DecoDisplayFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DecoGold
                                                )
                                            }
                                        }

                                        DecoSealButton(
                                            labelTop = "多",
                                            labelBottom = "选",
                                            onClick = {
                                                hapticEngine.bookshelfDetent()
                                                explicitSelectionMode = true
                                            },
                                            modifier = Modifier
                                                .semantics {
                                                    contentDescription = multiselectAccessibilityDesc
                                                }
                                                .testTag("gallery_enter_multiselect_button"),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Explicit notice when Bookshelf global MediaFilter (Photos/Videos) is active
                            if (uiState.mediaFilter != MediaFilter.ALL) {
                                Surface(
                                    shape = GalleryTileOutline,
                                    color = DecoVerm.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, DecoVerm.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .padding(bottom = 8.dp)
                                        .testTag("gallery_global_filter_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            DecoIcon(
                                                kind = DecoIconKind.FILTER,
                                                color = DecoVerm,
                                                iconSize = 15.dp
                                            )
                                            Text(
                                                text = "全局筛选：仅显示${uiState.mediaFilter.label}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontFamily = DecoDisplayFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DecoInkOnGround
                                            )
                                        }
                                        TextButton(
                                            onClick = onResetGlobalMediaFilter,
                                            modifier = Modifier
                                                .heightIn(min = 40.dp)
                                                .testTag("gallery_reset_global_filter_button")
                                        ) {
                                            Text(
                                                text = "显示全部",
                                                fontFamily = DecoDisplayFontFamily,
                                                color = DecoVerm,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Unified Search Bar + Filter Drawer trigger
                            if (allEntries.isNotEmpty()) {
                                TimelineGlobalSearchBar(
                                    rawKeyword = rawSearchKeyword,
                                    searchQuery = searchQuery,
                                    resultCount = displayedEntries.size,
                                    onKeywordChange = { rawSearchKeyword = it },
                                    onOpenDateRangePicker = {
                                        pendingPickerInitialRange = searchQuery.dateRange
                                        showDateRangePicker = true
                                    },
                                    onOpenFilterSheet = { showSearchFilterSheet = true },
                                    onClearDateRange = {
                                        searchQuery = searchQuery.copy(dateRange = null)
                                    },
                                    onRemoveLocation = { loc ->
                                        searchQuery = searchQuery.copy(
                                            selectedLocations = searchQuery.selectedLocations - loc
                                        )
                                    },
                                    onRemoveTag = { tag ->
                                        searchQuery = searchQuery.copy(
                                            selectedTags = searchQuery.selectedTags - tag
                                        )
                                    },
                                    onRemoveKeywordTag = { kw ->
                                        searchQuery = searchQuery.copy(
                                            selectedKeywordTags = searchQuery.selectedKeywordTags - kw
                                        )
                                    },
                                    onResetAll = resetSearchAndFilters,
                                    activeSmartFilter = smartFilter,
                                    selectedAlbum = selectedAlbumFilter,
                                    onClearSmartFilter = { smartFilter = GallerySmartFilter.ALL },
                                    onClearAlbum = { selectedAlbumFilter = null },
                                    isIndexingSmartTags = uiState.isIndexingSmartTags,
                                    smartTagIndexedCount = uiState.smartTagIndexedCount,
                                    smartTagTotalCount = uiState.smartTagTotalCount
                                )

                                // One-tap Quick Smart Filter Strip right below Search Bar
                                val quickSmartChoices = remember(
                                    burstEntries.size,
                                    treasuredMediaCount,
                                    largePhotoEntries.size,
                                    missingLocationEntries.size,
                                    livePhotoEntries.size,
                                    animatedImageEntries.size,
                                    screenshotEntries.size,
                                    specialFormatEntries.size
                                ) {
                                    buildList {
                                        add(GallerySmartFilter.ALL to "全部")
                                        add(GallerySmartFilter.TREASURED to if (treasuredMediaCount > 0) "珍藏 ($treasuredMediaCount)" else "珍藏")
                                        if (burstEntries.isNotEmpty()) add(GallerySmartFilter.BURSTS to "连拍组 (${burstEntries.size})")
                                        if (largePhotoEntries.isNotEmpty()) add(GallerySmartFilter.LARGE_PHOTOS to "大图 (${largePhotoEntries.size})")
                                        if (missingLocationEntries.isNotEmpty()) add(GallerySmartFilter.MISSING_LOCATION to "缺定位 (${missingLocationEntries.size})")
                                        if (livePhotoEntries.isNotEmpty()) add(GallerySmartFilter.LIVE_PHOTOS to "Live (${livePhotoEntries.size})")
                                        if (animatedImageEntries.isNotEmpty()) add(GallerySmartFilter.ANIMATED_IMAGES to "动图 (${animatedImageEntries.size})")
                                        if (screenshotEntries.isNotEmpty()) add(GallerySmartFilter.SCREENSHOTS to "截图/录屏 (${screenshotEntries.size})")
                                        if (specialFormatEntries.isNotEmpty()) add(GallerySmartFilter.SPECIAL_FORMATS to "HEIC/RAW (${specialFormatEntries.size})")
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (quickSmartChoices.size > 1) {
                                        LazyRow(
                                            contentPadding = PaddingValues(start = 20.dp, end = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("gallery_quick_smart_strip")
                                        ) {
                                            items(quickSmartChoices, key = { it.first.name }) { (filterOption, label) ->
                                                val isSel = smartFilter == filterOption
                                                GalleryDecoJumpTag(
                                                    selected = isSel,
                                                    onClick = {
                                                        hapticEngine.pageFlipLift()
                                                        smartFilter = filterOption
                                                    },
                                                    modifier = Modifier.testTag("gallery_quick_${filterOption.name.lowercase()}")
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontFamily = DecoDisplayFontFamily,
                                                        fontSize = 12.sp,
                                                        letterSpacing = 0.5.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSel) DecoMat else DecoInkOnGround.copy(alpha = 0.78f)
                                                    )
                                                }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .width(1.dp)
                                                .height(18.dp)
                                                .background(DecoGold.copy(alpha = 0.45f))
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    // Pinned right-edge icon-only view density button reflecting current layout state
                                    GalleryZoomTierIconButton(
                                        currentTier = galleryZoomTier,
                                        onClick = {
                                            hapticEngine.pageFlipLift()
                                            galleryZoomTier = galleryZoomTier.nextByTap(galleryColumns)
                                        },
                                        modifier = Modifier.testTag("gallery_zoom_tier_toggle")
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // 画报栏目线：通栏金渐变细线（deco-app.html 画廊屏 chips 与月头之间）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .height(1.dp)
                                        .background(galleryMonthRuleBrush())
                                )
                            }
                        }
                    }
                }

                if (displayedEntries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(
                                start = 32.dp,
                                end = 32.dp,
                                top = 32.dp,
                                bottom = 32.dp + effectiveBottomInset + navBarBottom
                            ),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = GalleryTileOutline,
                            color = DecoGroundDeep,
                            border = BorderStroke(1.5.dp, DecoGold.copy(alpha = 0.75f)),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                DecoIcon(
                                    kind = when {
                                        searchQuery.isActive -> DecoIconKind.SEARCH
                                        smartFilter == GallerySmartFilter.TREASURED -> DecoIconKind.FAVORITE
                                        smartFilter == GallerySmartFilter.BURSTS -> DecoIconKind.BURST
                                        smartFilter == GallerySmartFilter.LARGE_PHOTOS -> DecoIconKind.COMPRESS
                                        smartFilter == GallerySmartFilter.MISSING_LOCATION -> DecoIconKind.LOCATION_OFF
                                        smartFilter == GallerySmartFilter.LIVE_PHOTOS -> DecoIconKind.LIVE
                                        smartFilter == GallerySmartFilter.ANIMATED_IMAGES -> DecoIconKind.BOLT
                                        smartFilter == GallerySmartFilter.SCREENSHOTS -> DecoIconKind.PHONE
                                        smartFilter == GallerySmartFilter.SPECIAL_FORMATS -> DecoIconKind.PHOTO
                                        else -> DecoIconKind.PHOTO_LIBRARY
                                    },
                                    color = DecoGold,
                                    iconSize = 30.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = when {
                                searchQuery.isActive -> "未找到匹配的照片与视频"
                                selectedAlbumFilter != null -> "「$selectedAlbumFilter」相册暂无媒体"
                                smartFilter == GallerySmartFilter.TREASURED -> "暂无珍藏媒体"
                                smartFilter == GallerySmartFilter.BURSTS -> "当前没有待展开的连拍组"
                                smartFilter == GallerySmartFilter.LARGE_PHOTOS -> "暂未发现大于 3 MB 的高体积照片"
                                smartFilter == GallerySmartFilter.MISSING_LOCATION -> "全部媒体均已拥有定位"
                                smartFilter == GallerySmartFilter.LIVE_PHOTOS -> "暂未识别到 Live 图"
                                smartFilter == GallerySmartFilter.ANIMATED_IMAGES -> "暂未识别到动图"
                                smartFilter == GallerySmartFilter.SCREENSHOTS -> "暂未识别到截图或录屏"
                                smartFilter == GallerySmartFilter.SPECIAL_FORMATS -> "暂未识别到 HEIC/AVIF/RAW 原片"
                                else -> "画廊暂无照片与视频"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = DecoDisplayFontFamily,
                            color = DecoInkOnGround,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                searchQuery.isActive -> "尝试调整搜索关键词、放宽日期范围，或清除部分地点与描述标签筛选。"
                                selectedAlbumFilter != null -> "可以切换回全部媒体，或长按照片批量移动到此相册。"
                                smartFilter == GallerySmartFilter.TREASURED -> "在清理或详情页收藏后，珍藏媒体会汇聚在这里。"
                                smartFilter == GallerySmartFilter.BURSTS -> "所有连续拍摄组已整理完成，或切换至全部媒体查看。"
                                smartFilter == GallerySmartFilter.LARGE_PHOTOS -> "当前相册照片体积都很精巧，可切回全部媒体继续整理。"
                                smartFilter == GallerySmartFilter.MISSING_LOCATION -> "每张回忆都已标注好足迹地点，可前往足迹地图浏览。"
                                else -> "授权扫描本地相册或加载示例影集后，即可体验连拍叠放与批量多选整理。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = DecoInkOnGround.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        if (searchQuery.isActive || smartFilter != GallerySmartFilter.ALL || selectedAlbumFilter != null) {
                            GalleryActionButton(
                                text = if (searchQuery.isActive) "清除搜索与筛选" else "查看全部媒体",
                                tone = GalleryButtonTone.IMMERSIVE,
                                onClick = {
                                    resetSearchAndFilters()
                                    smartFilter = GallerySmartFilter.ALL
                                    selectedAlbumFilter = null
                                },
                                modifier = Modifier.testTag("timeline_search_empty_reset_button")
                            )
                        } else if (uiState.mediaFilter != MediaFilter.ALL) {
                            GalleryActionButton(
                                text = "显示全部照片与视频",
                                tone = GalleryButtonTone.IMMERSIVE,
                                onClick = onResetGlobalMediaFilter
                            )
                        } else {
                            GalleryActionButton(
                                text = stringResource(R.string.action_load_demo),
                                tone = GalleryButtonTone.IMMERSIVE,
                                onClick = onLoadDemoArchive
                            )
                        }
                    }
                } else {
                    BurstGalleryGrid(
                        entries = displayedEntries,
                        expandedBurstIds = expandedBurstIds,
                        onToggleBurst = onToggleBurstStable,
                        onSelectBestInBurst = onSelectBestInBurst,
                        onKeepBestInBurst = onKeepBestInBurstStable,
                        onTrashSinglePhoto = onTrashPhoto,
                        onPhotoClick = onPhotoClickStable,
                        columns = galleryColumns.coerceIn(3, 4),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 10.dp,
                            bottom = 20.dp + effectiveBottomInset + navBarBottom
                        ),
                        isSelectionMode = isSelectionMode,
                        selectedPhotoIds = selectedPhotoIds,
                        onToggleSelectPhotos = toggleSelectPhotos,
                        onLongPressSelectPhotos = longPressSelectPhotos,
                        onCleanMonth = { y, m, isCleaned ->
                            onStartMonthCleanup(y, m, isCleaned)
                        },
                        monthStamps = uiState.monthStamps,
                        showMonthStampOverlay = !isRelevanceRanked,
                        precomputedClusterLayout = if (!isRelevanceRanked && precomputed != null && displayedEntries === allEntries) {
                            precomputed.defaultClusterLayout
                        } else {
                            null
                        },
                        zoomTier = galleryZoomTier,
                        onZoomTierChanged = { galleryZoomTier = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("main_gallery_grid")
                    )
                }
            }

            // Floating 4-Action Obsidian Glass Bulk Action Capsule
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
                                hapticEngine.swipeKeepConfirm()
                                onBulkKeepPhotos(selectedPhotos, false)
                                exitSelectionMode()
                            }
                        },
                        onFavorite = {
                            if (selectedPhotos.isNotEmpty()) {
                                hapticEngine.swipeKeepConfirm()
                                onBulkKeepPhotos(selectedPhotos, true)
                                exitSelectionMode()
                            }
                        },
                        onTrash = {
                            if (selectedPhotos.isNotEmpty()) {
                                hapticEngine.swipeDeleteConfirm()
                                onBulkTrashPhotos(selectedPhotos)
                                exitSelectionMode()
                            }
                        },
                        onMore = {
                            if (selectedPhotos.isNotEmpty()) {
                                hapticEngine.pageFlipLift()
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
        val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
        val availableYears = remember(uiState.yearbooks, currentYear) {
            uiState.yearbooks.map { it.year }
                .distinct()
                .sortedDescending()
                .ifEmpty { listOf(currentYear) }
        }
        BulkMoveDestinationSheet(
            selectedCount = selectedPhotos.size,
            selectedBytes = selectedBytes,
            existingAlbums = existingAlbums,
            availableYears = availableYears,
            defaultYear = selectedPhotos.firstOrNull()?.year ?: availableYears.firstOrNull() ?: currentYear,
            defaultMonth = selectedPhotos.firstOrNull()?.month ?: 1,
            onDismiss = { showMoveSheet = false },
            onConfirmMove = { albumName, targetYear, targetMonth ->
                hapticEngine.swipeKeepConfirm()
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
                hapticEngine.swipeKeepConfirm()
                onBatchCompress(selectedPhotos, preset)
                showCompressSheet = false
                exitSelectionMode()
            }
        )
    }

    if (showLocationSheet && selectedPhotos.isNotEmpty()) {
        val sameDayMatches = remember(selectedPhotos, uiState.allPhotos) {
            inferSameDayLocations(selectedPhotos, uiState.allPhotos)
        }
        BatchLocationEditorSheet(
            targetPhotos = selectedPhotos,
            existingClusters = uiState.locationClusters,
            sameDayMatches = sameDayMatches,
            onResolveQuery = onGeocodeLocation,
            onApplySameDayInference = { matches, writeExif ->
                hapticEngine.swipeKeepConfirm()
                onBatchSameDayLocation(matches, writeExif)
                showLocationSheet = false
                exitSelectionMode()
            },
            onConfirmManualLocation = { locName, lat, lon, writeExif ->
                hapticEngine.swipeKeepConfirm()
                onBatchManualLocation(selectedPhotos, locName, lat, lon, writeExif)
                showLocationSheet = false
                exitSelectionMode()
            },
            onDismiss = { showLocationSheet = false }
        )
    }

    if (showSearchFilterSheet) {
        val availableYears = remember(uiState.yearbooks) {
            uiState.yearbooks.map { it.year }.distinct().sortedDescending()
        }
        TimelineSearchFilterSheet(
            currentQuery = searchQuery,
            availableYears = availableYears,
            locationClusters = uiState.locationClusters,
            hasUnlocatedMedia = uiState.unlocatedPhotos.isNotEmpty(),
            extractedKeywordTags = extractedKeywordTags,
            previewResultCount = { draftQuery, draftSmart, draftAlbum ->
                val smartAndAlbumEntries = filterGalleryEntriesBySmartAndAlbum(
                    allEntries = allEntries,
                    smartFilter = draftSmart,
                    selectedAlbum = draftAlbum,
                    burstEntries = burstEntries,
                    treasuredEntries = treasuredEntries,
                    largePhotoEntries = largePhotoEntries,
                    missingLocationEntries = missingLocationEntries
                )
                val previewSemantic = if (draftQuery.keyword == searchQuery.keyword) {
                    semanticScores
                } else {
                    emptyMap()
                }
                filterGalleryEntriesBySearch(smartAndAlbumEntries, draftQuery, previewSemantic).size
            },
            onApplyQuery = { nextQuery ->
                searchQuery = nextQuery
                rawSearchKeyword = nextQuery.keyword
            },
            onOpenCustomDatePicker = { draftQuery, draftSmart, draftAlbum ->
                searchQuery = draftQuery
                rawSearchKeyword = draftQuery.keyword
                smartFilter = draftSmart
                selectedAlbumFilter = draftAlbum
                pendingPickerInitialRange = draftQuery.dateRange
                showSearchFilterSheet = false
                showDateRangePicker = true
            },
            onDismiss = { showSearchFilterSheet = false },
            currentSmartFilter = smartFilter,
            currentAlbum = selectedAlbumFilter,
            existingAlbums = existingAlbums,
            burstCount = burstEntries.size,
            treasuredCount = treasuredMediaCount,
            largePhotoCount = largePhotoEntries.size,
            missingLocationCount = missingLocationEntries.size,
            livePhotoCount = livePhotoEntries.size,
            animatedImageCount = animatedImageEntries.size,
            screenshotCount = screenshotEntries.size,
            specialFormatCount = specialFormatEntries.size,
            allBurstsExpanded = allBurstsExpanded,
            onSelectSmartFilter = { smartFilter = it },
            onSelectAlbum = { selectedAlbumFilter = it },
            onToggleExpandAllBursts = {
                expandedBurstIds = if (allBurstsExpanded) {
                    emptySet()
                } else {
                    burstEntries.map { it.groupId }.toSet()
                }
            },
            availableTagCounts = descriptiveTagCounts
        )
    }

    if (showDateRangePicker) {
        TimelineDateRangePickerDialog(
            initialRange = pendingPickerInitialRange ?: searchQuery.dateRange,
            onConfirm = { newRange ->
                searchQuery = searchQuery.copy(dateRange = newRange)
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
        )
    }

    lightboxPhoto?.let { photo ->
        val flatPhotos = remember(displayedEntries) {
            displayedEntries.flatMap { entry ->
                when (entry) {
                    is GalleryEntry.Single -> listOf(entry.photo)
                    is GalleryEntry.Burst -> entry.photos
                }
            }
        }
        val startIdx = flatPhotos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        PhotoDetailLightbox(
            photo = photo,
            photos = flatPhotos.ifEmpty { listOf(photo) },
            initialIndex = startIdx,
            onDismiss = { lightboxPhoto = null },
            onKeep = onKeepPhoto,
            onTrash = onTrashPhoto,
            onToggleFavorite = onToggleFavorite
        )
    }
}
