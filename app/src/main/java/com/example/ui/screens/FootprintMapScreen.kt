package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.ResolvedGeoLocation
import com.example.data.SameDayLocationMatch
import com.example.data.inferSameDayLocations
import com.example.ui.FlipYearUiState
import com.example.ui.LocalFloatingDockInset
import com.example.ui.UNLOCATED_LOCATION_KEY
import com.example.ui.components.BatchLocationEditorSheet
import com.example.ui.components.FootprintMapStyle
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GallerySelectableChip
import com.example.ui.components.InteractiveSlippyMap
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.formatStorageSize
import com.example.ui.components.rememberSlippyMapCameraState
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberSoft
import com.example.ui.theme.StudioCanvas
import java.util.Locale
import kotlin.math.abs

@Composable
fun FootprintMapScreen(
    uiState: FlipYearUiState,
    onCleanLocation: (locationName: String, includeReviewed: Boolean) -> Unit,
    onKeepPhoto: (PhotoItemEntity, Boolean) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onRequestMediaLocationPermission: () -> Unit,
    onTriggerGpsBackfill: (forceRescan: Boolean) -> Unit,
    onBatchManualLocation: (List<PhotoItemEntity>, String, Double, Double, Boolean) -> Unit = { _, _, _, _, _ -> },
    onBatchSameDayLocation: (List<SameDayLocationMatch>, Boolean) -> Unit = { _, _ -> },
    onGeocodeLocation: suspend (String) -> ResolvedGeoLocation? = { null },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val floatingDockInset = LocalFloatingDockInset.current
    val clusters = uiState.locationClusters
    val unlocatedPhotos = uiState.unlocatedPhotos
    val locatedCount = remember(clusters) { clusters.sumOf { it.photos.size } }
    val totalVisibleCount = locatedCount + unlocatedPhotos.size

    var selectedLocationName by rememberSaveable { mutableStateOf<String?>(null) }
    var isCardExpanded by rememberSaveable { mutableStateOf(true) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    var showBatchLocationSheet by remember { mutableStateOf(false) }
    val locationChipsState = rememberLazyListState()

    val showingUnlocated = selectedLocationName == UNLOCATED_LOCATION_KEY && unlocatedPhotos.isNotEmpty()
    val selectedCluster = remember(clusters, selectedLocationName, showingUnlocated) {
        if (showingUnlocated) {
            null
        } else {
            clusters.firstOrNull { it.locationName == selectedLocationName } ?: clusters.firstOrNull()
        }
    }

    LaunchedEffect(selectedLocationName, clusters.size, showingUnlocated) {
        val targetIndex = when {
            showingUnlocated -> clusters.size
            selectedCluster != null -> clusters.indexOfFirst { it.locationName == selectedCluster.locationName }
            else -> -1
        }
        if (targetIndex >= 0) {
            locationChipsState.animateScrollToItem(index = (targetIndex - 1).coerceAtLeast(0))
        }
    }

    // Only indexes rows with gpsScanned = false (instant no-op once initial scan completes)
    LaunchedEffect(uiState.hasDeviceMedia, uiState.hasMediaLocationPermission) {
        if (uiState.hasDeviceMedia && !uiState.isIndexingGps) {
            onTriggerGpsBackfill(false)
        }
    }

    var mapStyleName by rememberSaveable { mutableStateOf(FootprintMapStyle.AMAP_ZH.name) }
    val mapStyle = remember(mapStyleName) {
        runCatching { FootprintMapStyle.valueOf(mapStyleName) }.getOrDefault(FootprintMapStyle.AMAP_ZH)
    }
    var tileRetryTrigger by remember { mutableIntStateOf(0) }
    var tileLoadedCount by remember(mapStyle, tileRetryTrigger) { mutableIntStateOf(0) }
    var tileFailedCount by remember(mapStyle, tileRetryTrigger) { mutableIntStateOf(0) }

    val initialLat = clusters.firstOrNull()?.latitude ?: 35.0
    val initialLon = clusters.firstOrNull()?.longitude ?: 105.0
    val cameraState = rememberSlippyMapCameraState(
        initialLat = initialLat,
        initialLon = initialLon,
        initialZoom = if (clusters.size > 1) 3.2f else 5.4f
    )

    var hasAutoFitted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(clusters.size) {
        if (clusters.isNotEmpty() && !hasAutoFitted) {
            hasAutoFitted = true
            cameraState.fitClusters(scope, clusters, durationMs = 450)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioCanvas)
    ) {
        // Real interactive Web Mercator tile + offline vector world map (zero Play Services crash risk)
        InteractiveSlippyMap(
            clusters = clusters,
            selectedCluster = selectedCluster,
            cameraState = cameraState,
            mapStyle = mapStyle,
            onSelectCluster = { cluster ->
                selectedLocationName = cluster.locationName
                isCardExpanded = true
            },
            tileRetryTrigger = tileRetryTrigger,
            onTileStatusChanged = { loaded, failed ->
                tileLoadedCount = loaded
                tileFailedCount = failed
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Header, Permission Prompt & Location Quick-Jump Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FrostedGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 8.dp,
                    enableAmbientBlur = true,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "足迹地图",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InkPrimary
                                )
                                val subtitle = when {
                                    uiState.isIndexingGps && uiState.gpsTotalCount > 0 ->
                                        "正在全量解析照片/视频定位 (${uiState.gpsIndexedCount}/${uiState.gpsTotalCount})"
                                    totalVisibleCount > 0 ->
                                        "${clusters.size} 个足迹 · 已定位 $locatedCount / 共 $totalVisibleCount 项"
                                    else -> "暂无带 GPS 信息的照片"
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (uiState.isIndexingGps && uiState.gpsTotalCount > 0) {
                            val progress = (uiState.gpsIndexedCount.toFloat() / uiState.gpsTotalCount.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                color = CobaltPrimary,
                                trackColor = HairlineBorder.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                // Map Layer Switcher Glass Pill
                FrostedGlassCard(
                    onClick = {
                        val entries = FootprintMapStyle.entries
                        val next = entries[(entries.indexOf(mapStyle) + 1) % entries.size]
                        mapStyleName = next.name
                    },
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("map_layer_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = stringResource(R.string.cd_map_layer),
                            tint = CobaltPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = mapStyle.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary
                        )
                    }
                }
            }

            // Explicit ACCESS_MEDIA_LOCATION authorization prompt when system redacts EXIF GPS
            if (uiState.hasDeviceMedia && !uiState.hasMediaLocationPermission) {
                FrostedGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    borderAccent = SolarAmber,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("footprint_location_permission_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = SolarAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "未开启媒体位置权限，系统已隐藏照片 GPS",
                                style = MaterialTheme.typography.labelLarge,
                                color = InkPrimary
                            )
                            Text(
                                text = "授权后将自动回溯全部照片与视频的真实拍摄地点",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                        }
                        GalleryActionButton(
                            text = "授权",
                            onClick = onRequestMediaLocationPermission,
                            tone = GalleryButtonTone.OBSIDIAN,
                            height = 38.dp,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("footprint_grant_location_button")
                        )
                    }
                }
            }

            if (clusters.isNotEmpty() || unlocatedPhotos.isNotEmpty()) {
                LazyRow(
                    state = locationChipsState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clusters, key = { it.locationName }) { cluster ->
                        val selected = !showingUnlocated && cluster.locationName == selectedCluster?.locationName
                        GallerySelectableChip(
                            selected = selected,
                            onClick = {
                                selectedLocationName = cluster.locationName
                                isCardExpanded = true
                                cameraState.animateToLatLng(
                                    scope = scope,
                                    lat = cluster.latitude,
                                    lon = cluster.longitude,
                                    targetZoom = 6.8f
                                )
                            },
                            modifier = Modifier.testTag("location_chip_${cluster.locationName}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = if (selected) Color.White else CobaltPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = cluster.locationName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else InkPrimary
                            )
                            if (cluster.unreviewedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (selected) Color.White.copy(alpha = 0.22f) else CoralSoft)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${cluster.unreviewedCount}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.White else CoralDanger
                                    )
                                }
                            }
                        }
                    }

                    // Transparent chip for media files without embedded GPS coordinates (screenshots, saved images, etc.)
                    if (unlocatedPhotos.isNotEmpty()) {
                        item(key = UNLOCATED_LOCATION_KEY) {
                            GallerySelectableChip(
                                selected = showingUnlocated,
                                onClick = {
                                    selectedLocationName = UNLOCATED_LOCATION_KEY
                                    isCardExpanded = true
                                },
                                unselectedTint = SolarAmberSoft.copy(alpha = 0.72f),
                                unselectedBorderAccent = SolarAmber,
                                modifier = Modifier.testTag("location_chip_unlocated")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOff,
                                    contentDescription = null,
                                    tint = if (showingUnlocated) SolarAmber else InkSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "无定位信息",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (showingUnlocated) Color.White else InkPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (showingUnlocated) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.75f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${unlocatedPhotos.size}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = if (showingUnlocated) Color.White else InkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subtle retry pill only shown when online tiles fail to load (e.g. temporary offline state)
            if (mapStyle != FootprintMapStyle.VECTOR && tileFailedCount > 0 && tileLoadedCount == 0) {
                FrostedGlassCard(
                    onClick = { tileRetryTrigger++ },
                    shape = RoundedCornerShape(50),
                    borderAccent = SolarAmber,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("map_tile_retry_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_retry_map_tiles),
                            tint = CobaltPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_retry_map_tiles),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkPrimary
                        )
                    }
                }
            }
        }

        // Right-side Floating Zoom, Fit-All & GPS Re-Index Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    end = 14.dp,
                    bottom = if (selectedCluster != null || showingUnlocated) 140.dp + floatingDockInset * 0.4f else 0.dp
                )
                .pointerInput(Unit) {},
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FrostedGlassCard(
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { cameraState.animateZoomBy(scope, +1.2f) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_map_zoom_in),
                            tint = InkPrimary
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.width(28.dp),
                        thickness = 1.dp,
                        color = HairlineBorder.copy(alpha = 0.7f)
                    )
                    IconButton(
                        onClick = { cameraState.animateZoomBy(scope, -1.2f) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.cd_map_zoom_out),
                            tint = InkPrimary
                        )
                    }
                }
            }

            if (clusters.isNotEmpty()) {
                FrostedGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    IconButton(
                        onClick = { cameraState.fitClusters(scope, clusters) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_fit_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CenterFocusStrong,
                            contentDescription = stringResource(R.string.cd_map_fit_all),
                            tint = CobaltPrimary
                        )
                    }
                }
            }

            if (uiState.hasDeviceMedia) {
                FrostedGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    IconButton(
                        onClick = { onTriggerGpsBackfill(true) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_rescan_gps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "重新解析全量照片定位",
                            tint = InkPrimary
                        )
                    }
                }
            }
        }

        if (clusters.isEmpty() && !showingUnlocated) {
            FrostedGlassCard(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 10.dp,
                enableAmbientBlur = true,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = if (unlocatedPhotos.isNotEmpty()) {
                            "当前 ${unlocatedPhotos.size} 项媒体均不含 GPS 信息"
                        } else {
                            "尚未发现带定位信息的照片"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (unlocatedPhotos.isNotEmpty()) {
                            "截图、社交软件保存图或拍摄时未开启定位开关的照片/视频不会写入 GPS 坐标。你可以点击上方「无定位信息」直接浏览并整理。"
                        } else {
                            "当本地相册包含 GPS 拍摄位置时，这里会自动在地图上聚合足迹并支持按地点批量清理。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (unlocatedPhotos.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GalleryActionButton(
                                text = "批量补全定位",
                                leadingIcon = Icons.Filled.EditLocationAlt,
                                onClick = { showBatchLocationSheet = true },
                                tone = GalleryButtonTone.OBSIDIAN,
                                height = 46.dp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("empty_map_batch_location_button")
                            )
                            GalleryActionButton(
                                text = "查看 (${unlocatedPhotos.size})",
                                onClick = {
                                    selectedLocationName = UNLOCATED_LOCATION_KEY
                                    isCardExpanded = true
                                },
                                tone = GalleryButtonTone.GLASS,
                                height = 46.dp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Collapsible Frosted-Glass Detail Card (Either Selected Geotagged Cluster OR Unlocated Media Sheet)
        if (showingUnlocated) {
            val unreviewedUnlocated = remember(unlocatedPhotos) {
                unlocatedPhotos.count { it.photoStatus == PhotoStatus.UNREVIEWED }
            }
            val totalBytes = remember(unlocatedPhotos) { unlocatedPhotos.sumOf { it.fileSizeBytes } }

            FrostedGlassCard(
                shape = RoundedCornerShape(26.dp),
                borderAccent = SolarAmber,
                shadowElevation = 14.dp,
                enableAmbientBlur = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp + floatingDockInset)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .testTag("unlocated_detail_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCardExpanded = !isCardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "无定位信息的照片与视频",
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${unlocatedPhotos.size} 项媒体 · ${formatStorageSize(totalBytes)} · 截图/网图/未开定位拍摄",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { isCardExpanded = !isCardExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isCardExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                contentDescription = if (isCardExpanded) "收起卡片" else "展开卡片",
                                tint = InkSecondary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isCardExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(unlocatedPhotos.take(24), key = { it.id }) { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 72.dp, height = 84.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                            .clickable { lightboxPhoto = photo }
                                    ) {
                                        PhotoArtwork(
                                            photo = photo,
                                            contentDescription = photo.caption,
                                            downsampleTier = ImageDownsampleTier.MICRO_THUMB,
                                            targetSizePx = 192,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                if (unlocatedPhotos.size > 24) {
                                    item(key = "unlocated_more") {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 84.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(StudioCanvas)
                                                .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    onCleanLocation(UNLOCATED_LOCATION_KEY, unreviewedUnlocated == 0)
                                                }
                                        ) {
                                            Text(
                                                text = "+${unlocatedPhotos.size - 24}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = InkTertiary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GalleryActionButton(
                                    text = "批量补全定位",
                                    onClick = { showBatchLocationSheet = true },
                                    tone = GalleryButtonTone.OBSIDIAN,
                                    leadingIcon = Icons.Filled.EditLocationAlt,
                                    height = 48.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .testTag("batch_tag_unlocated_button")
                                )
                                GalleryActionButton(
                                    text = if (unreviewedUnlocated > 0) {
                                        "整理 ($unreviewedUnlocated)"
                                    } else {
                                        "浏览全部"
                                    },
                                    onClick = {
                                        onCleanLocation(UNLOCATED_LOCATION_KEY, unreviewedUnlocated == 0)
                                    },
                                    tone = GalleryButtonTone.GLASS,
                                    leadingIcon = if (unreviewedUnlocated > 0) Icons.Filled.CleaningServices else Icons.Filled.PhotoCamera,
                                    height = 48.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("clean_unlocated_button")
                                )
                            }
                        }
                    }
                }
            }
        } else selectedCluster?.let { cluster ->
            val latLabel = if (cluster.latitude >= 0) "N" else "S"
            val lonLabel = if (cluster.longitude >= 0) "E" else "W"

            FrostedGlassCard(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 14.dp,
                enableAmbientBlur = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp + floatingDockInset)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .testTag("location_detail_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCardExpanded = !isCardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cluster.locationName,
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.2f°%s, %.2f°%s · %d 项媒体 · %s",
                                    abs(cluster.latitude),
                                    latLabel,
                                    abs(cluster.longitude),
                                    lonLabel,
                                    cluster.photos.size,
                                    formatStorageSize(cluster.totalBytes)
                                ),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = InkSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (cluster.unreviewedCount > 0) CoralSoft else EmeraldSoft
                            ) {
                                Text(
                                    text = if (cluster.unreviewedCount > 0) "${cluster.unreviewedCount} 待理" else "已整理",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cluster.unreviewedCount > 0) CoralDanger else EmeraldKeep,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            IconButton(
                                onClick = { isCardExpanded = !isCardExpanded },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toggle_location_card_button")
                            ) {
                                Icon(
                                    imageVector = if (isCardExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                    contentDescription = if (isCardExpanded) "收起卡片" else "展开卡片",
                                    tint = InkSecondary
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isCardExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(cluster.photos.take(24), key = { it.id }) { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 72.dp, height = 84.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                            .clickable { lightboxPhoto = photo }
                                    ) {
                                        PhotoArtwork(
                                            photo = photo,
                                            contentDescription = photo.caption,
                                            downsampleTier = ImageDownsampleTier.MICRO_THUMB,
                                            targetSizePx = 192,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                if (cluster.photos.size > 24) {
                                    item(key = "cluster_more_${cluster.locationName}") {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 84.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(StudioCanvas)
                                                .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    onCleanLocation(cluster.locationName, cluster.unreviewedCount == 0)
                                                }
                                        ) {
                                            Text(
                                                text = "+${cluster.photos.size - 24}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = InkTertiary
                                            )
                                        }
                                    }
                                }
                            }

                            GalleryActionButton(
                                text = if (cluster.unreviewedCount > 0) {
                                    "${stringResource(R.string.action_clean_location)} (${cluster.unreviewedCount} 项)"
                                } else {
                                    "在弧形时光轴中重温此地回忆"
                                },
                                onClick = {
                                    onCleanLocation(cluster.locationName, cluster.unreviewedCount == 0)
                                },
                                tone = GalleryButtonTone.OBSIDIAN,
                                leadingIcon = if (cluster.unreviewedCount > 0) Icons.Filled.CleaningServices else Icons.Filled.PhotoCamera,
                                height = 48.dp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("clean_location_button")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBatchLocationSheet && unlocatedPhotos.isNotEmpty()) {
        val sameDayMatches = remember(unlocatedPhotos, uiState.allPhotos) {
            inferSameDayLocations(unlocatedPhotos, uiState.allPhotos)
        }
        BatchLocationEditorSheet(
            targetPhotos = unlocatedPhotos,
            existingClusters = clusters,
            sameDayMatches = sameDayMatches,
            onResolveQuery = onGeocodeLocation,
            onApplySameDayInference = { matches, writeExif ->
                onBatchSameDayLocation(matches, writeExif)
                showBatchLocationSheet = false
            },
            onConfirmManualLocation = { locName, lat, lon, writeExif ->
                onBatchManualLocation(unlocatedPhotos, locName, lat, lon, writeExif)
                showBatchLocationSheet = false
            },
            onDismiss = { showBatchLocationSheet = false }
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
