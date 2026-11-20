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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CompressionPreset
import com.example.data.PhotoItemEntity
import com.example.data.ResolvedGeoLocation
import com.example.data.SameDayLocationMatch
import com.example.data.inferSameDayLocations
import com.example.sensor.rememberHapticEngine
import com.example.ui.DateRangeFilter
import com.example.ui.FlipYearUiState
import com.example.ui.GalleryEntry
import com.example.ui.LocalFloatingDockInset
import com.example.ui.TimelineSearchQuery
import com.example.ui.extractDescriptiveKeywordTags
import com.example.ui.filterGalleryEntriesBySearch
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.BatchCompressBottomSheet
import com.example.ui.components.BatchLocationEditorSheet
import com.example.ui.components.BurstGalleryGrid
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GallerySelectableChip
import com.example.ui.components.ObsidianGlassCard
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.TimelineDateRangePickerDialog
import com.example.ui.components.TimelineGlobalSearchBar
import com.example.ui.components.TimelineSearchFilterSheet
import com.example.ui.components.formatStorageSize
import com.example.ui.components.monthChineseName
import kotlinx.coroutines.delay
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure

private val CURATED_ALBUM_PRESETS = listOf("精选珍藏", "旅行日记", "家庭相册", "灵感素材")
private const val LARGE_PHOTO_THRESHOLD_BYTES = 3_000_000L

private enum class GallerySmartFilter {
    ALL,
    BURSTS,
    LARGE_PHOTOS,
    MISSING_LOCATION
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
    onSelectionModeChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hapticEngine = rememberHapticEngine()
    var expandedBurstIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var smartFilter by remember { mutableStateOf(GallerySmartFilter.ALL) }
    var selectedAlbumFilter by remember { mutableStateOf<String?>(null) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }

    var explicitSelectionMode by remember { mutableStateOf(false) }
    var selectedPhotoIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showCompressSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }

    // Global Timeline Search & Structured Filter State (date range, location, descriptive tags)
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

    val resetSearchAndFilters = remember {
        {
            rawSearchKeyword = ""
            searchQuery = TimelineSearchQuery()
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

    BackHandler(enabled = !isSelectionMode && searchQuery.isActive) {
        resetSearchAndFilters()
    }

    LaunchedEffect(isSelectionMode) {
        onSelectionModeChanged(isSelectionMode)
    }
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChanged(false) }
    }

    val allEntries = uiState.galleryEntries
    // Compute album list in a single pass without allocating an intermediate flat 10k+ photo list
    val existingAlbums = remember(allEntries) {
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

    // Clear stale album filter if all items were moved/deleted out of it
    LaunchedEffect(existingAlbums, selectedAlbumFilter) {
        if (selectedAlbumFilter != null && selectedAlbumFilter !in existingAlbums) {
            selectedAlbumFilter = null
        }
    }

    val burstEntries = remember(allEntries) { allEntries.filterIsInstance<GalleryEntry.Burst>() }
    val burstPhotosTotal = remember(burstEntries) { burstEntries.sumOf { it.photos.size } }
    val largePhotoEntries = remember(allEntries) {
        allEntries.filter { entry ->
            when (entry) {
                is GalleryEntry.Single -> !entry.photo.isVideo && entry.photo.fileSizeBytes >= LARGE_PHOTO_THRESHOLD_BYTES
                is GalleryEntry.Burst -> entry.photos.any { !it.isVideo && it.fileSizeBytes >= LARGE_PHOTO_THRESHOLD_BYTES }
            }
        }
    }
    val missingLocationEntries = remember(allEntries) {
        allEntries.filter { entry ->
            when (entry) {
                is GalleryEntry.Single ->
                    entry.photo.locationName.isNullOrBlank() || entry.photo.latitude == null || entry.photo.longitude == null
                is GalleryEntry.Burst ->
                    entry.photos.any { it.locationName.isNullOrBlank() || it.latitude == null || it.longitude == null }
            }
        }
    }

    val extractedKeywordTags = remember(allEntries) {
        extractDescriptiveKeywordTags(allEntries)
    }

    val chipFilteredEntries = remember(
        allEntries,
        burstEntries,
        largePhotoEntries,
        missingLocationEntries,
        smartFilter,
        selectedAlbumFilter
    ) {
        val base = when (smartFilter) {
            GallerySmartFilter.ALL -> allEntries
            GallerySmartFilter.BURSTS -> burstEntries
            GallerySmartFilter.LARGE_PHOTOS -> largePhotoEntries
            GallerySmartFilter.MISSING_LOCATION -> missingLocationEntries
        }
        val album = selectedAlbumFilter
        if (album == null) {
            base
        } else {
            base.filter { entry ->
                when (entry) {
                    is GalleryEntry.Single -> entry.photo.bucketName == album
                    is GalleryEntry.Burst -> entry.photos.any { it.bucketName == album }
                }
            }
        }
    }

    val displayedEntries = remember(chipFilteredEntries, searchQuery) {
        filterGalleryEntriesBySearch(chipFilteredEntries, searchQuery)
    }

    // Defer flattening 10k+ photos & building lookup maps until multi-select mode is actually active
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

    // Prune any selected IDs that are no longer visible (e.g. after deletion or filter switch)
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
    val effectiveBottomInset = if (isSelectionMode) 156.dp else floatingDockInset

    ArtisticGalleryBackdrop(
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Top Header swaps cleanly into sculpted Frosted Glass Selection Bar during multi-select
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
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "智能连拍画廊",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = if (burstEntries.isNotEmpty()) {
                                            "已聚合 ${burstEntries.size} 组连拍（共 $burstPhotosTotal 张）· 支持批量压缩与补定位"
                                        } else {
                                            "点击多选或长按照片，体验近无损压缩与补全定位"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (displayedEntries.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    FrostedGlassCard(
                                        onClick = {
                                            hapticEngine.bookshelfDetent()
                                            explicitSelectionMode = true
                                        },
                                        shape = RoundedCornerShape(50),
                                        borderAccent = CobaltPrimary,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier
                                            .heightIn(min = 42.dp)
                                            .testTag("gallery_enter_multiselect_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckBox,
                                                contentDescription = null,
                                                tint = CobaltPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.action_enter_multiselect),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CobaltDark
                                            )
                                        }
                                    }
                                }
                            }

                            // Global Timeline Search Bar (filter by date range, location, or descriptive tags)
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
                                    onResetAll = resetSearchAndFilters
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Smart Discovery Filter Ribbon: All / Bursts / Large Photos / Missing Location / Expand Bursts
                            if (allEntries.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item(key = "filter_all") {
                                        val selected = smartFilter == GallerySmartFilter.ALL && selectedAlbumFilter == null
                                        GallerySelectableChip(
                                            selected = selected,
                                            onClick = {
                                                smartFilter = GallerySmartFilter.ALL
                                                selectedAlbumFilter = null
                                            },
                                            modifier = Modifier.testTag("gallery_filter_all")
                                        ) {
                                            Text(
                                                text = "全部 (${allEntries.size})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontSize = 12.sp,
                                                color = if (selected) Color.White else InkPrimary
                                            )
                                        }
                                    }

                                    item(key = "filter_bursts") {
                                        val selected = smartFilter == GallerySmartFilter.BURSTS
                                        GallerySelectableChip(
                                            selected = selected,
                                            onClick = {
                                                smartFilter = GallerySmartFilter.BURSTS
                                                selectedAlbumFilter = null
                                            },
                                            modifier = Modifier.testTag("gallery_filter_bursts")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.BurstMode,
                                                contentDescription = null,
                                                tint = if (selected) SolarAmber else CobaltPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "连拍组 (${burstEntries.size})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontSize = 12.sp,
                                                color = if (selected) Color.White else InkPrimary
                                            )
                                        }
                                    }

                                    if (largePhotoEntries.isNotEmpty()) {
                                        item(key = "filter_large") {
                                            val selected = smartFilter == GallerySmartFilter.LARGE_PHOTOS
                                            GallerySelectableChip(
                                                selected = selected,
                                                onClick = {
                                                    smartFilter = GallerySmartFilter.LARGE_PHOTOS
                                                    selectedAlbumFilter = null
                                                },
                                                modifier = Modifier.testTag("gallery_filter_large")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Compress,
                                                    contentDescription = null,
                                                    tint = if (selected) SolarAmber else EmeraldKeep,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "大图省空间 (${largePhotoEntries.size})",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontSize = 12.sp,
                                                    color = if (selected) Color.White else InkPrimary
                                                )
                                            }
                                        }
                                    }

                                    if (missingLocationEntries.isNotEmpty()) {
                                        item(key = "filter_missing_loc") {
                                            val selected = smartFilter == GallerySmartFilter.MISSING_LOCATION
                                            GallerySelectableChip(
                                                selected = selected,
                                                onClick = {
                                                    smartFilter = GallerySmartFilter.MISSING_LOCATION
                                                    selectedAlbumFilter = null
                                                },
                                                modifier = Modifier.testTag("gallery_filter_missing_location")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.LocationOff,
                                                    contentDescription = null,
                                                    tint = if (selected) SolarAmber else CoralDanger,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "缺定位 (${missingLocationEntries.size})",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontSize = 12.sp,
                                                    color = if (selected) Color.White else InkPrimary
                                                )
                                            }
                                        }
                                    }

                                    if (burstEntries.isNotEmpty()) {
                                        item(key = "toggle_all_bursts") {
                                            FrostedGlassCard(
                                                onClick = {
                                                    expandedBurstIds = if (allBurstsExpanded) {
                                                        emptySet()
                                                    } else {
                                                        burstEntries.map { it.groupId }.toSet()
                                                    }
                                                },
                                                shape = RoundedCornerShape(50),
                                                borderAccent = CobaltPrimary,
                                                shadowElevation = 3.dp,
                                                modifier = Modifier.testTag("gallery_toggle_all_bursts")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (allBurstsExpanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                                                        contentDescription = null,
                                                        tint = CobaltDark,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = stringResource(
                                                            if (allBurstsExpanded) R.string.burst_collapse_all else R.string.burst_expand_all
                                                        ),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontSize = 12.sp,
                                                        color = CobaltDark
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Sleek Album Collection Filter Ribbon (auto-surfaces moved/device albums)
                                if (existingAlbums.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(existingAlbums, key = { it }) { albumName ->
                                            val isSelectedAlbum = selectedAlbumFilter == albumName
                                            GallerySelectableChip(
                                                selected = isSelectedAlbum,
                                                onClick = {
                                                    smartFilter = GallerySmartFilter.ALL
                                                    selectedAlbumFilter = if (isSelectedAlbum) null else albumName
                                                },
                                                modifier = Modifier.testTag("gallery_album_chip_$albumName")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Folder,
                                                    contentDescription = null,
                                                    tint = if (isSelectedAlbum) SolarAmber else InkSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = albumName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontSize = 11.5.sp,
                                                    color = if (isSelectedAlbum) Color.White else InkPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
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
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceMuted,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        searchQuery.isActive -> Icons.Filled.Search
                                        smartFilter == GallerySmartFilter.BURSTS -> Icons.Filled.BurstMode
                                        smartFilter == GallerySmartFilter.LARGE_PHOTOS -> Icons.Filled.Compress
                                        smartFilter == GallerySmartFilter.MISSING_LOCATION -> Icons.Filled.LocationOff
                                        else -> Icons.Filled.PhotoLibrary
                                    },
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = when {
                                searchQuery.isActive -> "未找到匹配的照片与视频"
                                selectedAlbumFilter != null -> "「$selectedAlbumFilter」相册暂无媒体"
                                smartFilter == GallerySmartFilter.BURSTS -> "当前没有待展开的连拍组"
                                smartFilter == GallerySmartFilter.LARGE_PHOTOS -> "暂未发现大于 3 MB 的高体积照片"
                                smartFilter == GallerySmartFilter.MISSING_LOCATION -> "太棒了！全部媒体均已拥有定位"
                                else -> "画廊暂无照片与视频"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = InkPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                searchQuery.isActive -> "尝试调整搜索关键词、放宽日期范围，或清除部分地点与描述标签筛选。"
                                selectedAlbumFilter != null -> "可以切换回全部媒体，或长按照片批量移动到此相册。"
                                smartFilter == GallerySmartFilter.BURSTS -> "所有连续拍摄组已整理完成，或切换至全部媒体查看。"
                                smartFilter == GallerySmartFilter.LARGE_PHOTOS -> "当前相册照片体积都很精巧，可切回全部媒体继续整理。"
                                smartFilter == GallerySmartFilter.MISSING_LOCATION -> "每张回忆都已标注好足迹地点，可前往足迹地图浏览。"
                                else -> "授权扫描本地相册或加载示例影集后，即可体验连拍叠放与批量多选整理。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        if (searchQuery.isActive || smartFilter != GallerySmartFilter.ALL || selectedAlbumFilter != null) {
                            GalleryActionButton(
                                text = if (searchQuery.isActive) "清除搜索与筛选" else "查看全部媒体",
                                tone = GalleryButtonTone.OBSIDIAN,
                                onClick = {
                                    resetSearchAndFilters()
                                    smartFilter = GallerySmartFilter.ALL
                                    selectedAlbumFilter = null
                                },
                                modifier = Modifier.testTag("timeline_search_empty_reset_button")
                            )
                        } else {
                            GalleryActionButton(
                                text = stringResource(R.string.action_load_demo),
                                tone = GalleryButtonTone.OBSIDIAN,
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
                        columns = 3,
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
                        monthStamps = uiState.monthStamps,
                        showMonthStampOverlay = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("main_gallery_grid")
                    )
                }
            }

            // Floating Obsidian Glass Bulk Action Capsule + Smart Batch Edit Strip
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 420.dp)
                ) {
                    if (selectedPhotos.isNotEmpty()) {
                        FloatingBatchEditStrip(
                            compressibleImageCount = selectedCompressibleCount,
                            missingLocationCount = selectedMissingLocationCount,
                            onCompress = {
                                hapticEngine.pageFlipLift()
                                showCompressSheet = true
                            },
                            onEditLocation = {
                                hapticEngine.pageFlipLift()
                                showLocationSheet = true
                            }
                        )
                    }

                    FloatingBulkActionBar(
                        selectedCount = selectedPhotos.size,
                        onMove = {
                            if (selectedPhotos.isNotEmpty()) {
                                hapticEngine.pageFlipLift()
                                showMoveSheet = true
                            }
                        },
                        onFavorite = {
                            if (selectedPhotos.isNotEmpty()) {
                                hapticEngine.swipeKeepConfirm()
                                onBulkKeepPhotos(selectedPhotos, true)
                                exitSelectionMode()
                            }
                        },
                        onKeep = {
                            if (selectedPhotos.isNotEmpty()) {
                                hapticEngine.swipeKeepConfirm()
                                onBulkKeepPhotos(selectedPhotos, false)
                                exitSelectionMode()
                            }
                        },
                        onDelete = {
                            if (selectedPhotos.isNotEmpty()) {
                                hapticEngine.swipeDeleteConfirm()
                                onBulkTrashPhotos(selectedPhotos)
                                exitSelectionMode()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showMoveSheet && selectedPhotos.isNotEmpty()) {
        val availableYears = remember(uiState.yearbooks) {
            (uiState.yearbooks.map { it.year } + listOf(2026, 2025, 2024, 2023))
                .distinct()
                .sortedDescending()
        }
        BulkMoveDestinationSheet(
            selectedCount = selectedPhotos.size,
            selectedBytes = selectedBytes,
            existingAlbums = existingAlbums,
            availableYears = availableYears,
            defaultYear = selectedPhotos.firstOrNull()?.year ?: availableYears.firstOrNull() ?: 2026,
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
            previewResultCount = { draft ->
                filterGalleryEntriesBySearch(chipFilteredEntries, draft).size
            },
            onApplyQuery = { nextQuery ->
                searchQuery = nextQuery
                rawSearchKeyword = nextQuery.keyword
            },
            onOpenCustomDatePicker = { currentRange ->
                pendingPickerInitialRange = currentRange
                showSearchFilterSheet = false
                showDateRangePicker = true
            },
            onDismiss = { showSearchFilterSheet = false }
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
        PhotoDetailLightbox(
            photo = photo,
            onDismiss = { lightboxPhoto = null },
            onKeep = onKeepPhoto,
            onTrash = onTrashPhoto
        )
    }
}

@Composable
private fun FloatingBatchEditStrip(
    compressibleImageCount: Int,
    missingLocationCount: Int,
    onCompress: () -> Unit,
    onEditLocation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onCompress,
            enabled = compressibleImageCount > 0,
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF161B26),
            border = BorderStroke(1.dp, EmeraldKeep.copy(alpha = if (compressibleImageCount > 0) 0.48f else 0.16f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("bulk_compress_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Compress,
                    contentDescription = null,
                    tint = if (compressibleImageCount > 0) EmeraldKeep else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (compressibleImageCount > 0) {
                        "${stringResource(R.string.action_batch_compress)}($compressibleImageCount)"
                    } else {
                        "仅支持照片压缩"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (compressibleImageCount > 0) Color.White else Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            onClick = onEditLocation,
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF161B26),
            border = BorderStroke(1.dp, SolarAmber.copy(alpha = 0.48f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("bulk_location_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EditLocationAlt,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (missingLocationCount > 0) {
                        "${stringResource(R.string.action_batch_location)} · ${missingLocationCount}缺"
                    } else {
                        stringResource(R.string.action_batch_location)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GallerySelectionTopBar(
    selectedCount: Int,
    selectedBytes: Long,
    allSelected: Boolean,
    onCancel: () -> Unit,
    onToggleSelectAll: () -> Unit
) {
    FrostedGlassCard(
        shape = RoundedCornerShape(22.dp),
        borderAccent = CobaltPrimary,
        shadowElevation = 10.dp,
        enableAmbientBlur = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("gallery_selection_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("gallery_selection_cancel")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_exit_selection),
                    tint = InkPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已选 $selectedCount 项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "合计 ${formatStorageSize(selectedBytes)} · 点击可增减项目",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            GallerySelectableChip(
                selected = allSelected,
                onClick = onToggleSelectAll,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("gallery_selection_toggle_all")
            ) {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = null,
                    tint = if (allSelected) SolarAmber else CobaltPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = stringResource(
                        if (allSelected) R.string.action_deselect_all else R.string.action_select_all
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 12.sp,
                    color = if (allSelected) Color.White else InkPrimary
                )
            }
        }
    }
}

@Composable
private fun FloatingBulkActionBar(
    selectedCount: Int,
    onMove: () -> Unit,
    onFavorite: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    ObsidianGlassCard(
        shape = RoundedCornerShape(30.dp),
        topColor = Color(0xFF181D28),
        bottomColor = Color(0xFF10141C),
        shadowElevation = 18.dp,
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .alpha(if (selectedCount > 0) 1f else 0.58f)
            .testTag("gallery_bulk_action_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BulkActionPillItem(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                label = stringResource(R.string.action_bulk_move),
                iconTint = Color(0xFF8FB0FF),
                onClick = onMove,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bulk_move_button")
            )

            BulkActionPillItem(
                icon = Icons.Filled.Favorite,
                label = stringResource(R.string.action_bulk_favorite),
                iconTint = SolarAmber,
                onClick = onFavorite,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bulk_favorite_button")
            )

            BulkActionPillItem(
                icon = Icons.Filled.CheckCircle,
                label = stringResource(R.string.action_bulk_keep),
                iconTint = EmeraldKeep,
                onClick = onKeep,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bulk_keep_button")
            )

            BulkActionPillItem(
                icon = Icons.Filled.DeleteOutline,
                label = "${stringResource(R.string.action_bulk_delete)}($selectedCount)",
                iconTint = Color(0xFFFF7A8A),
                containerHighlight = CoralDanger.copy(alpha = 0.24f),
                onClick = onDelete,
                modifier = Modifier
                    .weight(1.12f)
                    .testTag("bulk_delete_button")
            )
        }
    }
}

@Composable
private fun BulkActionPillItem(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    containerHighlight: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(containerHighlight)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkMoveDestinationSheet(
    selectedCount: Int,
    selectedBytes: Long,
    existingAlbums: List<String>,
    availableYears: List<Int>,
    defaultYear: Int,
    defaultMonth: Int,
    onDismiss: () -> Unit,
    onConfirmMove: (albumName: String?, targetYear: Int?, targetMonth: Int?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val albumSuggestions = remember(existingAlbums) {
        (CURATED_ALBUM_PRESETS + existingAlbums).distinct()
    }
    var albumInput by remember { mutableStateOf(albumSuggestions.firstOrNull() ?: "精选珍藏") }
    var changeYearMonth by remember { mutableStateOf(false) }
    var chosenYear by remember { mutableIntStateOf(defaultYear) }
    var chosenMonth by remember { mutableIntStateOf(defaultMonth.coerceIn(1, 12)) }

    val canConfirm = albumInput.isNotBlank() || changeYearMonth

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePure,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("bulk_move_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bulk_move_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        text = "已选 $selectedCount 项媒体 · 共 ${formatStorageSize(selectedBytes)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.5.sp,
                        color = InkSecondary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = CobaltSoft,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = "快速选择目标相册",
                style = MaterialTheme.typography.labelLarge,
                color = InkSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(albumSuggestions, key = { it }) { album ->
                    val selected = albumInput.trim() == album
                    GallerySelectableChip(
                        selected = selected,
                        onClick = { albumInput = album },
                        modifier = Modifier.testTag("bulk_move_preset_$album")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = if (selected) SolarAmber else CobaltPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = album,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = if (selected) Color.White else InkPrimary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = albumInput,
                onValueChange = { albumInput = it },
                singleLine = true,
                label = { Text(stringResource(R.string.bulk_move_album_label)) },
                placeholder = { Text(stringResource(R.string.bulk_move_album_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = CobaltPrimary
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CobaltPrimary,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = SurfaceMuted.copy(alpha = 0.45f),
                    unfocusedContainerColor = SurfaceMuted.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bulk_move_album_input")
            )

            // Optional Year / Month Yearbook Re-filing Toggle
            FrostedGlassCard(
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.bulk_move_date_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = InkPrimary
                            )
                        }
                        Switch(
                            checked = changeYearMonth,
                            onCheckedChange = { changeYearMonth = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltPrimary
                            ),
                            modifier = Modifier.testTag("bulk_move_date_switch")
                        )
                    }

                    AnimatedVisibility(visible = changeYearMonth) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(availableYears, key = { it }) { yr ->
                                    val isSel = yr == chosenYear
                                    GallerySelectableChip(
                                        selected = isSel,
                                        onClick = { chosenYear = yr }
                                    ) {
                                        Text(
                                            text = "${yr}年",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else InkPrimary
                                        )
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items((1..12).toList(), key = { it }) { month ->
                                    val isSel = month == chosenMonth
                                    GallerySelectableChip(
                                        selected = isSel,
                                        onClick = { chosenMonth = month }
                                    ) {
                                        Text(
                                            text = monthChineseName(month),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontSize = 11.5.sp,
                                            color = if (isSel) Color.White else InkPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GalleryActionButton(
                    text = "取消",
                    tone = GalleryButtonTone.GLASS,
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("bulk_move_cancel_button")
                )
                GalleryActionButton(
                    text = "确认移动 ($selectedCount)",
                    leadingIcon = Icons.AutoMirrored.Filled.DriveFileMove,
                    tone = GalleryButtonTone.OBSIDIAN,
                    enabled = canConfirm,
                    onClick = {
                        onConfirmMove(
                            albumInput.trim().takeIf { it.isNotBlank() },
                            if (changeYearMonth) chosenYear else null,
                            if (changeYearMonth) chosenMonth else null
                        )
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("bulk_move_confirm_button")
                )
            }
        }
    }
}
