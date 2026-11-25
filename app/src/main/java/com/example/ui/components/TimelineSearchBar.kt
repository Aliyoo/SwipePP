package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.JetBrainsMonoFontFamily

enum class GallerySmartFilter(val label: String) {
    ALL("全部"),
    BURSTS("连拍组"),
    LARGE_PHOTOS("大图省空间"),
    MISSING_LOCATION("缺定位"),
    LIVE_PHOTOS("Live 图"),
    ANIMATED_IMAGES("动图"),
    SCREENSHOTS("截图/录屏"),
    SPECIAL_FORMATS("HEIC/RAW")
}

/* ---- 摩登画报 · 搜索与筛选装饰（design-demos/deco-app.html 金边跳签 / 金双线画框） ---- */
private val SearchRowShape = RoundedCornerShape(3.dp)
private val SearchTagShape = RoundedCornerShape(3.dp)

/** 金边跳签：选中金底 + 内衬字，未选中金描边透明底 */
@Composable
private fun DecoSearchJumpTag(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unselectedAccent: Color = DecoGold,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = SearchTagShape,
        color = if (selected) unselectedAccent else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) unselectedAccent else unselectedAccent.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            content = content
        )
    }
}

@Composable
private fun decoTagContentColor(selected: Boolean, accent: Color = DecoGold): Color =
    if (selected) {
        if (accent == DecoVerm) DecoPaperCream else DecoMat
    } else {
        DecoInkOnGround.copy(alpha = 0.78f)
    }

private val DecoSheetTopShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)


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
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val filterCount = searchQuery.structuredFilterCount +
        (if (activeSmartFilter != GallerySmartFilter.ALL) 1 else 0) +
        (if (selectedAlbum != null) 1 else 0)
    val hasAnyActiveFilter = searchQuery.isActive || activeSmartFilter != GallerySmartFilter.ALL || selectedAlbum != null
    val searchAccent = isFocused || searchQuery.isActive

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 金边搜索行（deco-app.html 画廊屏：42dp 金描边地槽 + 金色放大镜）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(SearchRowShape)
                .background(DecoMat.copy(alpha = 0.55f))
                .border(
                    width = 1.dp,
                    color = if (searchAccent) DecoGold else DecoGold.copy(alpha = 0.53f),
                    shape = SearchRowShape
                )
                .testTag("timeline_search_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DecoIcon(
                    kind = DecoIconKind.SEARCH,
                    color = DecoGold,
                    iconSize = 20.dp
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
                            color = DecoInkOnGround.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = rawKeyword,
                        onValueChange = onKeywordChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = DecoInkOnGround,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(DecoGold),
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
                        DecoIcon(
                            kind = DecoIconKind.CLOSE,
                            color = DecoInkOnGround.copy(alpha = 0.66f),
                            iconSize = 18.dp,
                            contentDescription = stringResource(R.string.cd_timeline_search_clear)
                        )
                    }
                }

                // Single Unified Filter Sheet trigger (Smart Views + Albums + Date + Locations + Tags)
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        onOpenFilterSheet()
                    },
                    shape = SearchRowShape,
                    color = if (filterCount > 0) DecoGold else Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (filterCount > 0) DecoGold else DecoGold.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("timeline_search_filter_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        DecoIcon(
                            kind = DecoIconKind.FILTER,
                            color = if (filterCount > 0) DecoMat else DecoGold,
                            iconSize = 17.dp
                        )
                        Text(
                            text = if (filterCount > 0) "筛选 $filterCount" else "筛选",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = DecoDisplayFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterCount > 0) DecoMat else DecoGold
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
                        shape = SearchTagShape,
                        color = DecoGold.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "匹配 $resultCount 项",
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = DecoInkOnGround,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (activeSmartFilter != GallerySmartFilter.ALL) {
                    item(key = "active_smart_filter") {
                        RemovableFilterPill(
                            iconKind = when (activeSmartFilter) {
                                GallerySmartFilter.BURSTS -> DecoIconKind.BURST
                                GallerySmartFilter.LARGE_PHOTOS -> DecoIconKind.COMPRESS
                                GallerySmartFilter.MISSING_LOCATION -> DecoIconKind.LOCATION_OFF
                                GallerySmartFilter.LIVE_PHOTOS -> DecoIconKind.PLAY
                                GallerySmartFilter.ANIMATED_IMAGES -> DecoIconKind.BOLT
                                GallerySmartFilter.SCREENSHOTS -> DecoIconKind.PHONE
                                GallerySmartFilter.SPECIAL_FORMATS -> DecoIconKind.PHOTO
                                GallerySmartFilter.ALL -> DecoIconKind.FILTER
                            },
                            iconTint = DecoGold,
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
                            iconKind = DecoIconKind.FOLDER,
                            iconTint = DecoGold,
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
                            iconKind = DecoIconKind.CALENDAR,
                            iconTint = DecoGold,
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
                        iconKind = if (isUnlocated) DecoIconKind.LOCATION_OFF else DecoIconKind.LOCATION,
                        iconTint = if (isUnlocated) DecoVerm else DecoGold,
                        label = if (isUnlocated) "无定位信息" else loc,
                        onClick = onOpenFilterSheet,
                        onRemove = { onRemoveLocation(loc) },
                        testTag = "active_filter_loc_$loc"
                    )
                }

                items(searchQuery.selectedTags.toList(), key = { "tag_${it.name}" }) { tag ->
                    RemovableFilterPill(
                        iconKind = DecoIconKind.LABEL,
                        iconTint = DecoGold,
                        label = tag.label,
                        onClick = onOpenFilterSheet,
                        onRemove = { onRemoveTag(tag) },
                        testTag = "active_filter_tag_${tag.name.lowercase()}"
                    )
                }

                items(searchQuery.selectedKeywordTags.toList(), key = { "kw_$it" }) { kw ->
                    RemovableFilterPill(
                        iconKind = DecoIconKind.LABEL,
                        iconTint = DecoGold,
                        label = "#$kw",
                        onClick = onOpenFilterSheet,
                        onRemove = { onRemoveKeywordTag(kw) },
                        testTag = "active_filter_kw_$kw"
                    )
                }

                item(key = "reset_all") {
                    Surface(
                        onClick = onResetAll,
                        shape = SearchTagShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.45f)),
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
                                fontFamily = DecoDisplayFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DecoInkOnGround.copy(alpha = 0.72f)
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
    iconKind: DecoIconKind,
    iconTint: Color,
    label: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    testTag: String
) {
    Surface(
        shape = SearchTagShape,
        color = DecoMat.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.45f)),
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
            DecoIcon(
                kind = iconKind,
                color = iconTint,
                iconSize = 14.dp
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DecoDisplayFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DecoInkOnGround,
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
                        .background(DecoGold.copy(alpha = 0.16f))
                ) {
                    DecoIcon(
                        kind = DecoIconKind.CLOSE,
                        color = DecoInkOnGround.copy(alpha = 0.66f),
                        iconSize = 13.dp,
                        contentDescription = "移除筛选 $label"
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
    livePhotoCount: Int = 0,
    animatedImageCount: Int = 0,
    screenshotCount: Int = 0,
    specialFormatCount: Int = 0,
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
        containerColor = DecoMat,
        shape = DecoSheetTopShape,
        dragHandle = { DecoSheetDragHandle() },
        modifier = Modifier
            .modalSheetTopClearance()
            .testTag("timeline_filter_sheet")
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
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DecoInkOnGround
                    )
                    Text(
                        text = "按智能分类、相册、日期、拍摄地点与标签组合查找",
                        style = MaterialTheme.typography.bodySmall,
                        color = DecoInkOnGround.copy(alpha = 0.7f)
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
                            fontFamily = DecoDisplayFontFamily,
                            color = DecoVerm,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (scrollState.canScrollBackward) {
                HorizontalDivider(
                    color = DecoGold.copy(alpha = 0.3f),
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
                        DecoIcon(
                            kind = DecoIconKind.BURST,
                            color = DecoGold,
                            iconSize = 18.dp
                        )
                        Text(
                            text = "智能视图",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DecoInkOnGround
                        )
                    }
                    if (burstCount > 0 && onToggleExpandAllBursts != null) {
                        Surface(
                            onClick = onToggleExpandAllBursts,
                            shape = SearchTagShape,
                            color = if (allBurstsExpanded) DecoGold else Color.Transparent,
                            border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("gallery_toggle_all_bursts")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DecoIcon(
                                    kind = if (allBurstsExpanded) DecoIconKind.UNFOLD_LESS else DecoIconKind.UNFOLD_MORE,
                                    color = if (allBurstsExpanded) DecoMat else DecoGold,
                                    iconSize = 14.dp
                                )
                                Text(
                                    text = stringResource(
                                        if (allBurstsExpanded) R.string.burst_collapse_all else R.string.burst_expand_all
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allBurstsExpanded) DecoMat else DecoGold
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
                        if (livePhotoCount > 0) add(GallerySmartFilter.LIVE_PHOTOS to "Live 图 ($livePhotoCount)")
                        if (animatedImageCount > 0) add(GallerySmartFilter.ANIMATED_IMAGES to "动图 ($animatedImageCount)")
                        if (screenshotCount > 0) add(GallerySmartFilter.SCREENSHOTS to "截图/录屏 ($screenshotCount)")
                        if (specialFormatCount > 0) add(GallerySmartFilter.SPECIAL_FORMATS to "HEIC/RAW ($specialFormatCount)")
                    }
                    smartChoices.forEach { (filterOption, textLabel) ->
                        val selected = draftSmartFilter == filterOption
                        DecoSearchJumpTag(
                            selected = selected,
                            onClick = { draftSmartFilter = filterOption },
                            modifier = Modifier.testTag("gallery_filter_${filterOption.name.lowercase()}")
                        ) {
                            Text(
                                text = textLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = DecoDisplayFontFamily,
                                fontSize = 12.sp,
                                color = decoTagContentColor(selected)
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
                        DecoIcon(
                            kind = DecoIconKind.FOLDER,
                            color = DecoGold,
                            iconSize = 18.dp
                        )
                        Text(
                            text = "相册分类",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DecoInkOnGround
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        existingAlbums.take(16).forEach { albumName ->
                            val selected = draftAlbum == albumName
                            DecoSearchJumpTag(
                                selected = selected,
                                onClick = {
                                    draftAlbum = if (selected) null else albumName
                                },
                                modifier = Modifier.testTag("gallery_album_chip_$albumName")
                            ) {
                                DecoIcon(
                                    kind = DecoIconKind.FOLDER,
                                    color = if (selected) DecoMat else DecoGold.copy(alpha = 0.85f),
                                    iconSize = 13.dp
                                )
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontSize = 12.sp,
                                    color = decoTagContentColor(selected)
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
                        DecoIcon(
                            kind = DecoIconKind.CALENDAR,
                            color = DecoGold,
                            iconSize = 18.dp
                        )
                        Text(
                            text = stringResource(R.string.timeline_search_date_range),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DecoInkOnGround
                        )
                    }
                    Surface(
                        onClick = { onOpenCustomDatePicker(draft, draftSmartFilter, draftAlbum) },
                        shape = SearchTagShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("filter_sheet_custom_date_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DecoIcon(
                                kind = DecoIconKind.CALENDAR,
                                color = DecoGold,
                                iconSize = 14.dp
                            )
                            Text(
                                text = draft.dateRange?.formatDisplayLabel() ?: "自选日期区间",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = DecoDisplayFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DecoGold
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
                        DecoSearchJumpTag(
                            selected = selected,
                            onClick = {
                                draft = draft.copy(dateRange = if (selected) null else preset)
                            },
                            modifier = Modifier.testTag("filter_date_preset_${preset.presetLabel}")
                        ) {
                            Text(
                                text = preset.formatDisplayLabel(),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = DecoDisplayFontFamily,
                                fontSize = 12.sp,
                                color = decoTagContentColor(selected)
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
                        DecoIcon(
                            kind = DecoIconKind.LOCATION,
                            color = DecoGold,
                            iconSize = 18.dp
                        )
                        Text(
                            text = stringResource(R.string.timeline_search_locations),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DecoInkOnGround
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locationClusters.take(14).forEach { cluster ->
                            val selected = cluster.locationName in draft.selectedLocations
                            DecoSearchJumpTag(
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
                                DecoIcon(
                                    kind = DecoIconKind.LOCATION,
                                    color = if (selected) DecoMat else DecoGold.copy(alpha = 0.85f),
                                    iconSize = 13.dp
                                )
                                Text(
                                    text = "${cluster.locationName} (${cluster.photos.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontSize = 12.sp,
                                    color = decoTagContentColor(selected)
                                )
                            }
                        }

                        if (hasUnlocatedMedia) {
                            val selected = SEARCH_UNLOCATED_TOKEN in draft.selectedLocations
                            DecoSearchJumpTag(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) {
                                        draft.selectedLocations - SEARCH_UNLOCATED_TOKEN
                                    } else {
                                        draft.selectedLocations + SEARCH_UNLOCATED_TOKEN
                                    }
                                    draft = draft.copy(selectedLocations = next)
                                },
                                unselectedAccent = DecoVerm,
                                modifier = Modifier.testTag("filter_location_unlocated")
                            ) {
                                DecoIcon(
                                    kind = DecoIconKind.LOCATION_OFF,
                                    color = if (selected) DecoPaperCream else DecoVerm,
                                    iconSize = 13.dp
                                )
                                Text(
                                    text = "无定位信息",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontSize = 12.sp,
                                    color = decoTagContentColor(selected, DecoVerm)
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
                    DecoIcon(
                        kind = DecoIconKind.LABEL,
                        color = DecoGold,
                        iconSize = 18.dp
                    )
                    Text(
                        text = stringResource(R.string.timeline_search_tags),
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DecoInkOnGround
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DescriptiveTag.entries.forEach { tag ->
                        val selected = tag in draft.selectedTags
                        DecoSearchJumpTag(
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
                                fontFamily = DecoDisplayFontFamily,
                                fontSize = 12.sp,
                                color = decoTagContentColor(selected)
                            )
                        }
                    }

                    extractedKeywordTags.forEach { kw ->
                        val selected = kw in draft.selectedKeywordTags
                        DecoSearchJumpTag(
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
                                fontFamily = DecoDisplayFontFamily,
                                fontSize = 12.sp,
                                color = decoTagContentColor(selected)
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
                                    0.0f to DecoMat.copy(alpha = 0f),
                                    0.30f to DecoMat.copy(alpha = 0.90f),
                                    1.0f to DecoMat
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
                        leadingIconKind = DecoIconKind.SEARCH,
                        tone = if (liveMatchCount > 0) GalleryButtonTone.IMMERSIVE else GalleryButtonTone.GLASS,
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
                Text(
                    text = "确定",
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = DecoGold
                )
            }
        },
        dismissButton = {
            Row {
                if (initialRange?.isActive == true || pickerState.selectedStartDateMillis != null) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        modifier = Modifier.testTag("date_range_clear_button")
                    ) {
                        Text(
                            text = "清除",
                            fontFamily = DecoDisplayFontFamily,
                            color = DecoVerm
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "取消",
                        fontFamily = DecoDisplayFontFamily,
                        color = DecoInkOnGround.copy(alpha = 0.66f)
                    )
                }
            }
        },
        colors = DatePickerDefaults.colors(containerColor = DecoMat),
        modifier = Modifier.testTag("timeline_date_range_dialog")
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    text = "选择拍摄日期范围",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = DecoInkOnGround,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
                )
            },
            headline = {
                val preview = DateRangeFilter.fromUtcMillisRange(
                    pickerState.selectedStartDateMillis,
                    pickerState.selectedEndDateMillis
                )?.formatDisplayLabel() ?: "点击起止日期"
                // 预览可能为纯数字区间或带汉字（「全部日期」「… 起」），按内容择族
                Text(
                    text = preview,
                    fontFamily = if (preview.any { it.code in 0x4E00..0x9FFF }) {
                        DecoDisplayFontFamily
                    } else {
                        JetBrainsMonoFontFamily
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = DecoGold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp, end = 24.dp)
                )
            },
            showModeToggle = true,
            colors = DatePickerDefaults.colors(
                containerColor = DecoMat,
                selectedDayContainerColor = DecoVerm,
                todayContentColor = DecoGold,
                todayDateBorderColor = DecoGold,
                dayInSelectionRangeContainerColor = DecoVerm.copy(alpha = 0.2f),
                dayInSelectionRangeContentColor = DecoInkOnGround
            ),
            modifier = Modifier.heightIn(max = 500.dp)
        )
    }
}
