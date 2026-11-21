package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BurstMode
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.DateRangeFilter
import com.example.ui.DescriptiveTag
import com.example.ui.LocationCluster
import com.example.ui.SEARCH_UNLOCATED_TOKEN
import com.example.ui.TimelineSearchQuery
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GlassLightBottom
import com.example.ui.theme.GlassLightTop
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure

enum class GallerySmartFilter(val label: String) {
    ALL("全部"),
    BURSTS("连拍组"),
    LARGE_PHOTOS("大图省空间"),
    MISSING_LOCATION("缺定位")
}

/**
 * Tactile Frosted-Glass Global Search Bar for the photo timeline.
 * Converges search + a single unified Filter button (housing smart views, albums, date range,
 * locations, and descriptive tags) plus accessible 48dp removable active-filter pills.
 */
@Composable
fun TimelineGlobalSearchBar(
    rawKeyword: String,
    searchQuery: TimelineSearchQuery,
    resultCount: Int,
    onKeywordChange: (String) -> Unit,
    onOpenDateRangePicker: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onClearDateRange: () -> Unit,
    onRemoveLocation: (String) -> Unit,
    onRemoveTag: (DescriptiveTag) -> Unit,
    onRemoveKeywordTag: (String) -> Unit,
    onResetAll: () -> Unit,
    activeSmartFilter: GallerySmartFilter = GallerySmartFilter.ALL,
    selectedAlbum: String? = null,
    onClearSmartFilter: () -> Unit = {},
    onClearAlbum: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val filterCount = searchQuery.structuredFilterCount +
        (if (activeSmartFilter != GallerySmartFilter.ALL) 1 else 0) +
        (if (selectedAlbum != null) 1 else 0)
    val hasAnyActiveFilter = searchQuery.isActive || activeSmartFilter != GallerySmartFilter.ALL || selectedAlbum != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FrostedGlassCard(
            shape = RoundedCornerShape(24.dp),
            topColor = if (isDark) {
                if (isFocused) Color(0xF2252C3E) else GlassLightTop
            } else {
                if (isFocused) Color(0xFAFFFFFF) else Color(0xECFFFFFF)
            },
            bottomColor = if (isDark) {
                if (isFocused) Color(0xEB1B2130) else GlassLightBottom
            } else {
                if (isFocused) Color(0xF5F7F9FF) else Color(0xDEF5F3EC)
            },
            borderAccent = if (isFocused || searchQuery.isActive) CobaltPrimary else null,
            shadowElevation = if (isFocused) 10.dp else 5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("timeline_search_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (isFocused || searchQuery.isActive) CobaltPrimary else InkSecondary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f)
                ) {
                    if (rawKeyword.isEmpty()) {
                        Text(
                            text = stringResource(R.string.timeline_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = rawKeyword,
                        onValueChange = onKeywordChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = InkPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(CobaltPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag("timeline_search_input")
                    )
                }

                if (rawKeyword.isNotEmpty()) {
                    IconButton(
                        onClick = { onKeywordChange("") },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("timeline_search_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_timeline_search_clear),
                            tint = InkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Single Unified Filter Sheet trigger (Smart Views + Albums + Date + Locations + Tags)
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        onOpenFilterSheet()
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (filterCount > 0) CobaltPrimary else SurfaceMuted.copy(alpha = 0.85f),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("timeline_search_filter_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = if (filterCount > 0) Color.White else InkPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (filterCount > 0) "筛选 $filterCount" else "筛选",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterCount > 0) Color.White else InkPrimary
                        )
                    }
                }
            }
        }

        // Active Search & Unified Filter Summary Strip
        AnimatedVisibility(
            visible = hasAnyActiveFilter,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_active_filters_row")
            ) {
                item(key = "search_result_count") {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CobaltSoft,
                        border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = "匹配 $resultCount 项",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CobaltDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (activeSmartFilter != GallerySmartFilter.ALL) {
                    item(key = "active_smart_filter") {
                        RemovableFilterPill(
                            icon = when (activeSmartFilter) {
                                GallerySmartFilter.BURSTS -> Icons.Filled.BurstMode
                                GallerySmartFilter.LARGE_PHOTOS -> Icons.Filled.Compress
                                GallerySmartFilter.MISSING_LOCATION -> Icons.Filled.LocationOff
                                GallerySmartFilter.ALL -> Icons.Filled.Tune
                            },
                            iconTint = CobaltPrimary,
                            label = activeSmartFilter.label,
                            onClick = onOpenFilterSheet,
                            onRemove = onClearSmartFilter,
                            testTag = "active_filter_smart"
                        )
                    }
                }

                selectedAlbum?.let { album ->
                    item(key = "active_album_filter") {
                        RemovableFilterPill(
                            icon = Icons.Filled.Folder,
                            iconTint = SolarAmber,
                            label = album,
                            onClick = onOpenFilterSheet,
                            onRemove = onClearAlbum,
                            testTag = "active_filter_album"
                        )
                    }
                }

                searchQuery.dateRange?.takeIf { it.isActive }?.let { range ->
                    item(key = "active_date_range") {
                        RemovableFilterPill(
                            icon = Icons.Filled.CalendarMonth,
                            iconTint = CobaltPrimary,
                            label = range.formatDisplayLabel(),
                            onClick = onOpenDateRangePicker,
                            onRemove = onClearDateRange,
                            testTag = "active_filter_date"
                        )
                    }
                }

                items(searchQuery.selectedLocations.toList(), key = { "loc_$it" }) { loc ->
                    val isUnlocated = loc == SEARCH_UNLOCATED_TOKEN
                    RemovableFilterPill(
                        icon = if (isUnlocated) Icons.Filled.LocationOff else Icons.Filled.LocationOn,
                        iconTint = if (isUnlocated) CoralDanger else CobaltPrimary,
                        label = if (isUnlocated) "无定位信息" else loc,
                        onClick = onOpenFilterSheet,
                        onRemove = { onRemoveLocation(loc) },
                        testTag = "active_filter_loc_$loc"
                    )
                }

                items(searchQuery.selectedTags.toList(), key = { "tag_${it.name}" }) { tag ->
                    RemovableFilterPill(
                        icon = Icons.Filled.Label,
                        iconTint = SolarAmber,
                        label = tag.label,
                        onClick = onOpenFilterSheet,
                        onRemove = { onRemoveTag(tag) },
                        testTag = "active_filter_tag_${tag.name.lowercase()}"
                    )
                }

                items(searchQuery.selectedKeywordTags.toList(), key = { "kw_$it" }) { kw ->
                    RemovableFilterPill(
                        icon = Icons.Filled.Label,
                        iconTint = CobaltPrimary,
                        label = "#$kw",
                        onClick = onOpenFilterSheet,
                        onRemove = { onRemoveKeywordTag(kw) },
                        testTag = "active_filter_kw_$kw"
                    )
                }

                item(key = "reset_all") {
                    Surface(
                        onClick = onResetAll,
                        shape = RoundedCornerShape(50),
                        color = SurfaceMuted.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("timeline_search_reset_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.timeline_search_clear_all),
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemovableFilterPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SurfacePure,
        border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.28f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(start = 11.dp, end = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${testTag}_remove")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SurfaceMuted)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "移除筛选 $label",
                        tint = InkSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * Structured multi-dimensional filter sheet covering:
 * 1. Date Range (quick presets + Material 3 DateRangePickerDialog)
 * 2. Shooting Locations (from real GPS clusters + Unlocated)
 * 3. Descriptive Tags (curated photo attributes + extracted scene keywords)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimelineSearchFilterSheet(
    currentQuery: TimelineSearchQuery,
    availableYears: List<Int>,
    locationClusters: List<LocationCluster>,
    hasUnlocatedMedia: Boolean,
    extractedKeywordTags: List<String>,
    previewResultCount: (TimelineSearchQuery, GallerySmartFilter, String?) -> Int,
    onApplyQuery: (TimelineSearchQuery) -> Unit,
    onOpenCustomDatePicker: (TimelineSearchQuery, GallerySmartFilter, String?) -> Unit,
    onDismiss: () -> Unit,
    currentSmartFilter: GallerySmartFilter = GallerySmartFilter.ALL,
    currentAlbum: String? = null,
    existingAlbums: List<String> = emptyList(),
    burstCount: Int = 0,
    largePhotoCount: Int = 0,
    missingLocationCount: Int = 0,
    allBurstsExpanded: Boolean = false,
    onSelectSmartFilter: (GallerySmartFilter) -> Unit = {},
    onSelectAlbum: (String?) -> Unit = {},
    onToggleExpandAllBursts: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var draft by remember(currentQuery) { mutableStateOf(currentQuery) }
    var draftSmartFilter by remember(currentSmartFilter) { mutableStateOf(currentSmartFilter) }
    var draftAlbum by remember(currentAlbum) { mutableStateOf(currentAlbum) }
    val liveMatchCount = remember(draft, draftSmartFilter, draftAlbum) {
        previewResultCount(draft, draftSmartFilter, draftAlbum)
    }

    val datePresets = remember(availableYears) {
        buildList {
            add(DateRangeFilter.lastDaysPreset(30, "近 30 天"))
            add(DateRangeFilter.lastDaysPreset(90, "近 3 个月"))
            availableYears.take(4).forEach { y ->
                add(DateRangeFilter.yearPreset(y))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePure,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("timeline_filter_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Pinned Sheet Header so title and Reset remain accessible while scrolling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.timeline_search_filter_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        text = "按智能分类、相册、日期、拍摄地点与标签组合查找",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                if (draft.hasStructuredFilters || draftSmartFilter != GallerySmartFilter.ALL || draftAlbum != null) {
                    TextButton(
                        onClick = {
                            draft = draft.copy(
                                dateRange = null,
                                selectedLocations = emptySet(),
                                selectedTags = emptySet(),
                                selectedKeywordTags = emptySet()
                            )
                            draftSmartFilter = GallerySmartFilter.ALL
                            draftAlbum = null
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("filter_sheet_reset_button")
                    ) {
                        Text(
                            text = "重置筛选",
                            color = CoralDanger,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (scrollState.canScrollBackward) {
                HorizontalDivider(
                    color = HairlineBorder,
                    thickness = 0.8.dp
                )
            }

            // Scrollable filter sections with floating bottom Apply Filter action bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 22.dp)
                        .padding(top = 10.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {

            // Section 0: Smart Views & Burst Expansion (folded into unified Filter Sheet)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BurstMode,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "智能视图",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                    }
                    if (burstCount > 0 && onToggleExpandAllBursts != null) {
                        Surface(
                            onClick = onToggleExpandAllBursts,
                            shape = RoundedCornerShape(50),
                            color = CobaltSoft,
                            border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("gallery_toggle_all_bursts")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (allBurstsExpanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                                    contentDescription = null,
                                    tint = CobaltDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (allBurstsExpanded) R.string.burst_collapse_all else R.string.burst_expand_all
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CobaltDark
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val smartChoices = buildList {
                        add(GallerySmartFilter.ALL to "全部")
                        if (burstCount > 0) add(GallerySmartFilter.BURSTS to "连拍组 ($burstCount)")
                        if (largePhotoCount > 0) add(GallerySmartFilter.LARGE_PHOTOS to "大图省空间 ($largePhotoCount)")
                        if (missingLocationCount > 0) add(GallerySmartFilter.MISSING_LOCATION to "缺定位 ($missingLocationCount)")
                    }
                    smartChoices.forEach { (filterOption, textLabel) ->
                        val selected = draftSmartFilter == filterOption
                        GallerySelectableChip(
                            selected = selected,
                            onClick = { draftSmartFilter = filterOption },
                            modifier = Modifier.testTag("gallery_filter_${filterOption.name.lowercase()}")
                        ) {
                            Text(
                                text = textLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                color = if (selected) Color.White else InkPrimary
                            )
                        }
                    }
                }
            }

            // Section 0.5: Albums
            if (existingAlbums.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = SolarAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "相册分类",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        existingAlbums.take(16).forEach { albumName ->
                            val selected = draftAlbum == albumName
                            GallerySelectableChip(
                                selected = selected,
                                onClick = {
                                    draftAlbum = if (selected) null else albumName
                                },
                                modifier = Modifier.testTag("gallery_album_chip_$albumName")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = if (selected) SolarAmber else InkSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    color = if (selected) Color.White else InkPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Date Range
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.timeline_search_date_range),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                    }
                    Surface(
                        onClick = { onOpenCustomDatePicker(draft, draftSmartFilter, draftAlbum) },
                        shape = RoundedCornerShape(50),
                        color = CobaltSoft,
                        border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.28f)),
                        modifier = Modifier.testTag("filter_sheet_custom_date_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = CobaltDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = draft.dateRange?.formatDisplayLabel() ?: "自选日期区间",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CobaltDark
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    datePresets.forEach { preset ->
                        val selected = draft.dateRange?.startDateKey == preset.startDateKey &&
                            draft.dateRange?.endDateKey == preset.endDateKey
                        GallerySelectableChip(
                            selected = selected,
                            onClick = {
                                draft = draft.copy(dateRange = if (selected) null else preset)
                            },
                            modifier = Modifier.testTag("filter_date_preset_${preset.presetLabel}")
                        ) {
                            Text(
                                text = preset.formatDisplayLabel(),
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                color = if (selected) Color.White else InkPrimary
                            )
                        }
                    }
                }
            }

            // Section 2: Shooting Locations
            if (locationClusters.isNotEmpty() || hasUnlocatedMedia) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.timeline_search_locations),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locationClusters.take(14).forEach { cluster ->
                            val selected = cluster.locationName in draft.selectedLocations
                            GallerySelectableChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) {
                                        draft.selectedLocations - cluster.locationName
                                    } else {
                                        draft.selectedLocations + cluster.locationName
                                    }
                                    draft = draft.copy(selectedLocations = next)
                                },
                                modifier = Modifier.testTag("filter_location_${cluster.locationName}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = if (selected) SolarAmber else CobaltPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${cluster.locationName} (${cluster.photos.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    color = if (selected) Color.White else InkPrimary
                                )
                            }
                        }

                        if (hasUnlocatedMedia) {
                            val selected = SEARCH_UNLOCATED_TOKEN in draft.selectedLocations
                            GallerySelectableChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) {
                                        draft.selectedLocations - SEARCH_UNLOCATED_TOKEN
                                    } else {
                                        draft.selectedLocations + SEARCH_UNLOCATED_TOKEN
                                    }
                                    draft = draft.copy(selectedLocations = next)
                                },
                                unselectedTint = SolarAmberSoft.copy(alpha = 0.65f),
                                unselectedBorderAccent = SolarAmber,
                                modifier = Modifier.testTag("filter_location_unlocated")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOff,
                                    contentDescription = null,
                                    tint = if (selected) SolarAmber else CoralDanger,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "无定位信息",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    color = if (selected) Color.White else InkPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Descriptive Tags (Attributes + Extracted Scene Keywords)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Label,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.timeline_search_tags),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DescriptiveTag.entries.forEach { tag ->
                        val selected = tag in draft.selectedTags
                        GallerySelectableChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draft.selectedTags - tag else draft.selectedTags + tag
                                draft = draft.copy(selectedTags = next)
                            },
                            modifier = Modifier.testTag("filter_tag_${tag.name.lowercase()}")
                        ) {
                            Text(
                                text = tag.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                color = if (selected) Color.White else InkPrimary
                            )
                        }
                    }

                    extractedKeywordTags.forEach { kw ->
                        val selected = kw in draft.selectedKeywordTags
                        GallerySelectableChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) {
                                    draft.selectedKeywordTags - kw
                                } else {
                                    draft.selectedKeywordTags + kw
                                }
                                draft = draft.copy(selectedKeywordTags = next)
                            },
                            modifier = Modifier.testTag("filter_kw_tag_$kw")
                        ) {
                            Text(
                                text = "#$kw",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                color = if (selected) Color.White else InkPrimary
                            )
                        }
                    }
                }
                }
                }

                // Floating Bottom Apply Filter Dock with soft fade scrim and click-through shield
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to SurfacePure.copy(alpha = 0f),
                                    0.30f to SurfacePure.copy(alpha = 0.90f),
                                    1.0f to SurfacePure
                                )
                            )
                        )
                        .pointerInput(Unit) {}
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp)
                        .padding(top = 22.dp, bottom = 14.dp)
                        .testTag("filter_sheet_floating_footer")
                ) {
                    GalleryActionButton(
                        text = "应用筛选 ($liveMatchCount 项)",
                        leadingIcon = Icons.Filled.Search,
                        tone = if (liveMatchCount > 0) GalleryButtonTone.OBSIDIAN else GalleryButtonTone.GLASS,
                        height = 52.dp,
                        onClick = {
                            onSelectSmartFilter(draftSmartFilter)
                            onSelectAlbum(draftAlbum)
                            onApplyQuery(draft)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("filter_sheet_apply_button")
                    )
                }
            }
        }
    }
}

/**
 * Official Material 3 DateRangePickerDialog configured for picking any custom start/end date
 * range on the photo timeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineDateRangePickerDialog(
    initialRange: DateRangeFilter?,
    onConfirm: (DateRangeFilter?) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange?.startDateKey?.let(DateRangeFilter::dateKeyToUtcMillis),
        initialSelectedEndDateMillis = initialRange?.endDateKey?.let(DateRangeFilter::dateKeyToUtcMillis)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val range = DateRangeFilter.fromUtcMillisRange(
                        startUtcMillis = pickerState.selectedStartDateMillis,
                        endUtcMillis = pickerState.selectedEndDateMillis
                    )
                    onConfirm(range)
                },
                modifier = Modifier.testTag("date_range_confirm_button")
            ) {
                Text("确定", fontWeight = FontWeight.Bold, color = CobaltPrimary)
            }
        },
        dismissButton = {
            Row {
                if (initialRange?.isActive == true || pickerState.selectedStartDateMillis != null) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        modifier = Modifier.testTag("date_range_clear_button")
                    ) {
                        Text("清除", color = CoralDanger)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消", color = InkSecondary)
                }
            }
        },
        colors = DatePickerDefaults.colors(containerColor = SurfacePure),
        modifier = Modifier.testTag("timeline_date_range_dialog")
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    text = "选择拍摄日期范围",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
                )
            },
            headline = {
                val preview = DateRangeFilter.fromUtcMillisRange(
                    pickerState.selectedStartDateMillis,
                    pickerState.selectedEndDateMillis
                )?.formatDisplayLabel() ?: "点击起止日期"
                Text(
                    text = preview,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = CobaltPrimary,
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp, end = 24.dp)
                )
            },
            showModeToggle = true,
            colors = DatePickerDefaults.colors(
                containerColor = SurfacePure,
                selectedDayContainerColor = CobaltPrimary,
                todayContentColor = CobaltPrimary,
                todayDateBorderColor = CobaltPrimary,
                dayInSelectionRangeContainerColor = CobaltSoft,
                dayInSelectionRangeContentColor = CobaltDark
            ),
            modifier = Modifier.heightIn(max = 500.dp)
        )
    }
}
