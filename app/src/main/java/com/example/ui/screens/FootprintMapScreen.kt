package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.example.sensor.rememberHapticEngine
import com.example.ui.FlipYearUiState
import com.example.ui.LocalFloatingDockInset
import com.example.ui.MediaFilter
import com.example.ui.UNLOCATED_LOCATION_KEY
import com.example.ui.components.BatchLocationEditorSheet
import com.example.ui.components.FootprintLocationScrubRail
import com.example.ui.components.FootprintMapStyle
import com.example.ui.components.FootprintScrubStop
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.InteractiveSlippyMap
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.computeChinaFocusTarget
import com.example.ui.components.formatStorageSize
import com.example.ui.components.isInsideChinaMainland
import com.example.ui.components.rememberSlippyMapCameraState
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.CobaltDark
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
import kotlinx.coroutines.launch

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
    onToggleFavorite: (PhotoItemEntity, Boolean) -> Unit = { p, f -> onKeepPhoto(p, f) },
    onResetGlobalMediaFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticEngine()
    val floatingDockInset = LocalFloatingDockInset.current
    val clusters = uiState.locationClusters
    val unlocatedPhotos = uiState.unlocatedPhotos
    val locatedCount = remember(clusters) { clusters.sumOf { it.photos.size } }
    val totalVisibleCount = locatedCount + unlocatedPhotos.size

    var selectedLocationName by rememberSaveable { mutableStateOf<String?>(null) }
    var isCardExpanded by rememberSaveable { mutableStateOf(false) }
    var showMapZoomControls by rememberSaveable { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    var lightboxPhotos by remember { mutableStateOf<List<PhotoItemEntity>>(emptyList()) }
    var showBatchLocationSheet by remember { mutableStateOf(false) }

    var mapStyleName by rememberSaveable { mutableStateOf(FootprintMapStyle.AMAP_ZH.name) }
    val mapStyle = remember(mapStyleName) {
        runCatching { FootprintMapStyle.valueOf(mapStyleName) }.getOrDefault(FootprintMapStyle.AMAP_ZH)
    }
    var tileRetryTrigger by remember { mutableIntStateOf(0) }
    var tileLoadedCount by remember(mapStyle, tileRetryTrigger) { mutableIntStateOf(0) }
    var tileFailedCount by remember(mapStyle, tileRetryTrigger) { mutableIntStateOf(0) }

    val showingUnlocated = selectedLocationName == UNLOCATED_LOCATION_KEY && unlocatedPhotos.isNotEmpty()
    val selectedCluster = remember(clusters, selectedLocationName, showingUnlocated, mapStyle) {
        if (showingUnlocated) {
            null
        } else {
            clusters.firstOrNull { it.locationName == selectedLocationName }
                ?: if (mapStyle == FootprintMapStyle.AMAP_ZH) {
                    clusters.firstOrNull { isInsideChinaMainland(it.latitude, it.longitude) } ?: clusters.firstOrNull()
                } else {
                    clusters.firstOrNull()
                }
        }
    }

    val scrubStops = remember(clusters, unlocatedPhotos) {
        buildList(clusters.size + 1) {
            clusters.forEach { cluster ->
                add(
                    FootprintScrubStop(
                        id = cluster.locationName,
                        displayName = cluster.locationName,
                        mediaCount = cluster.photos.size,
                        unreviewedCount = cluster.unreviewedCount,
                        totalBytes = cluster.totalBytes,
                        latitude = cluster.latitude,
                        longitude = cluster.longitude,
                        isUnlocated = false
                    )
                )
            }
            if (unlocatedPhotos.isNotEmpty()) {
                add(
                    FootprintScrubStop(
                        id = UNLOCATED_LOCATION_KEY,
                        displayName = "无定位信息",
                        mediaCount = unlocatedPhotos.size,
                        unreviewedCount = unlocatedPhotos.count { it.photoStatus == PhotoStatus.UNREVIEWED },
                        totalBytes = unlocatedPhotos.sumOf { it.fileSizeBytes },
                        latitude = 0.0,
                        longitude = 0.0,
                        isUnlocated = true
                    )
                )
            }
        }
    }

    val activeStopIndex = remember(scrubStops, showingUnlocated, selectedCluster) {
        val idx = when {
            showingUnlocated -> scrubStops.indexOfFirst { it.isUnlocated }
            selectedCluster != null -> scrubStops.indexOfFirst { !it.isUnlocated && it.id == selectedCluster.locationName }
            else -> 0
        }
        idx.coerceAtLeast(0)
    }

    // Only indexes rows with gpsScanned = false (instant no-op once initial scan completes)
    LaunchedEffect(uiState.hasDeviceMedia, uiState.hasMediaLocationPermission) {
        if (uiState.hasDeviceMedia && !uiState.isIndexingGps) {
            onTriggerGpsBackfill(false)
        }
    }

    val initialTarget = remember(mapStyle) {
        if (mapStyle == FootprintMapStyle.AMAP_ZH) {
            computeChinaFocusTarget(clusters)
        } else {
            val first = clusters.firstOrNull()
            com.example.ui.components.MapCameraTarget(
                lat = first?.latitude ?: 35.4,
                lon = first?.longitude ?: 104.8,
                zoom = if (clusters.size > 1) 3.2f else 5.4f
            )
        }
    }
    val cameraState = rememberSlippyMapCameraState(
        initialLat = initialTarget.lat,
        initialLon = initialTarget.lon,
        initialZoom = initialTarget.zoom
    )

    var hasAutoFitted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(clusters.size, mapStyle) {
        if (clusters.isNotEmpty() && !hasAutoFitted) {
            hasAutoFitted = true
            if (mapStyle == FootprintMapStyle.AMAP_ZH) {
                cameraState.focusChinaRegion(scope, clusters, durationMs = 450)
            } else {
                cameraState.fitClusters(scope, clusters, durationMs = 450)
            }
        }
    }

    // Tracks horizontal transition direction (+1 = forward/swipe-left, -1 = backward/swipe-right)
    var cardSlideDirection by remember { mutableIntStateOf(1) }

    val commitFootprintStop: (FootprintScrubStop, Boolean, Int?) -> Unit = { stop, shouldExpand, directionOverride ->
        val targetIdx = scrubStops.indexOfFirst { it.id == stop.id }
        cardSlideDirection = directionOverride ?: when {
            targetIdx >= 0 && targetIdx != activeStopIndex -> if (targetIdx > activeStopIndex) 1 else -1
            else -> cardSlideDirection
        }
        selectedLocationName = stop.id
        if (stop.isUnlocated) {
            isCardExpanded = true
        } else {
            if (shouldExpand) {
                isCardExpanded = true
            }
            cameraState.animateToLatLng(
                scope = scope,
                lat = stop.latitude,
                lon = stop.longitude,
                targetZoom = 6.8f
            )
        }
    }

    val stepFootprintStop: (Int) -> Unit = { delta ->
        if (scrubStops.size >= 2) {
            val nextIdx = (activeStopIndex + delta).mod(scrubStops.size)
            haptics.bookshelfDetent()
            commitFootprintStop(scrubStops[nextIdx], false, if (delta >= 0) 1 else -1)
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
                val targetIdx = scrubStops.indexOfFirst { !it.isUnlocated && it.id == cluster.locationName }
                if (targetIdx >= 0 && targetIdx != activeStopIndex) {
                    cardSlideDirection = if (targetIdx > activeStopIndex) 1 else -1
                }
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

        // Top Compact Telemetry Pill + Map Layer Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Single-line compact footprint telemetry pill (tap to recenter/fit all footprints)
                FrostedGlassCard(
                    onClick = {
                        haptics.pageFlipLift()
                        if (mapStyle == FootprintMapStyle.AMAP_ZH) {
                            val hasOverseas = clusters.any { !isInsideChinaMainland(it.latitude, it.longitude) }
                            if (hasOverseas && cameraState.isFramingChinaOverview()) {
                                cameraState.fitClusters(scope, clusters)
                            } else {
                                cameraState.focusChinaRegion(scope, clusters)
                            }
                        } else if (clusters.isNotEmpty()) {
                            cameraState.fitClusters(scope, clusters)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp,
                    enableAmbientBlur = true,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("map_stats_pill")
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 11.dp,
                            end = if (uiState.hasDeviceMedia) 4.dp else 11.dp,
                            top = 4.dp,
                            bottom = 4.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = stringResource(R.string.cd_map_fit_all),
                                tint = CobaltPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            val statsLabel = when {
                                uiState.isIndexingGps && uiState.gpsTotalCount > 0 ->
                                    "解析定位 ${uiState.gpsIndexedCount}/${uiState.gpsTotalCount}"
                                totalVisibleCount > 0 ->
                                    "${clusters.size} 足迹 · 已定位 $locatedCount/$totalVisibleCount"
                                else -> "暂无定位照片"
                            }
                            Text(
                                text = statsLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(vertical = 5.dp)
                            )
                            if (uiState.hasDeviceMedia) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable { onTriggerGpsBackfill(true) }
                                        .testTag("map_rescan_gps_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "重新解析全量照片定位",
                                        tint = InkSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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
                                    .padding(end = if (uiState.hasDeviceMedia) 6.dp else 0.dp)
                                    .height(2.5.dp)
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
                        if (next == FootprintMapStyle.AMAP_ZH && !cameraState.isFramingChinaOverview()) {
                            cameraState.focusChinaRegion(scope, clusters, durationMs = 480)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("map_layer_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = stringResource(R.string.cd_map_layer),
                            tint = CobaltPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = mapStyle.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
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

            // Top city chip row intentionally removed to liberate the map canvas;
            // one-handed location switching now lives on the right-edge FootprintLocationScrubRail
            // and bottom detail card quick-stepper.

            // Global MediaFilter active notice when user filtered to Photos/Videos on Bookshelf
            if (uiState.mediaFilter != MediaFilter.ALL) {
                FrostedGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    borderAccent = CobaltPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("map_global_filter_banner")
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
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "当前仅显示${uiState.mediaFilter.label}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CobaltDark
                            )
                        }
                        TextButton(onClick = onResetGlobalMediaFilter) {
                            Text("显示全部", color = CobaltPrimary, fontWeight = FontWeight.Bold)
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

        // Right-edge One-Handed Footprint Location Scrub Rail + Bottom-Left Map Zoom/Fit Controls
        val hasBottomCard = selectedCluster != null || showingUnlocated
        val density = LocalDensity.current
        val targetControlsShiftPx = with(density) {
            when {
                hasBottomCard && isCardExpanded -> -244.dp.toPx()
                hasBottomCard -> -102.dp.toPx()
                else -> 0f
            }
        }
        val controlsShiftYState = animateFloatAsState(
            targetValue = targetControlsShiftPx,
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "map_controls_clearance"
        )

        val railBottomClearance = when {
            hasBottomCard && isCardExpanded -> 268.dp + floatingDockInset
            hasBottomCard -> 124.dp + floatingDockInset
            else -> 36.dp + floatingDockInset
        }

        // Signature Right-Edge 「足迹小旗快滑轨」: drag to live-preview locations, release to fly
        if (scrubStops.size >= 2) {
            FootprintLocationScrubRail(
                stops = scrubStops,
                activeIndex = activeStopIndex,
                onCommitStop = { stop -> commitFootprintStop(stop, false, null) },
                topPadding = if (uiState.hasDeviceMedia && !uiState.hasMediaLocationPermission) 156.dp else 80.dp,
                bottomPadding = railBottomClearance,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )
        }

        // Bottom-Left Horizontal Map Zoom Capsule — hidden by default to preserve an uncluttered map canvas
        AnimatedVisibility(
            visible = showMapZoomControls,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 14.dp + floatingDockInset)
                .graphicsLayer {
                    translationY = controlsShiftYState.value
                }
                .pointerInput(Unit) {}
        ) {
            FrostedGlassCard(
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 5.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (clusters.isNotEmpty() || mapStyle == FootprintMapStyle.AMAP_ZH) {
                        IconButton(
                            onClick = {
                                if (mapStyle == FootprintMapStyle.AMAP_ZH) {
                                    val hasOverseas = clusters.any { !isInsideChinaMainland(it.latitude, it.longitude) }
                                    if (hasOverseas && cameraState.isFramingChinaOverview()) {
                                        cameraState.fitClusters(scope, clusters)
                                    } else {
                                        cameraState.focusChinaRegion(scope, clusters)
                                    }
                                } else {
                                    cameraState.fitClusters(scope, clusters)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("map_fit_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CenterFocusStrong,
                                contentDescription = stringResource(R.string.cd_map_fit_all),
                                tint = CobaltPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(0.8.dp)
                                .height(22.dp)
                                .background(HairlineBorder.copy(alpha = 0.7f))
                        )
                    }
                    IconButton(
                        onClick = { cameraState.animateZoomBy(scope, -1.2f) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.cd_map_zoom_out),
                            tint = InkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .height(22.dp)
                            .background(HairlineBorder.copy(alpha = 0.7f))
                    )
                    IconButton(
                        onClick = { cameraState.animateZoomBy(scope, +1.2f) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_map_zoom_in),
                            tint = InkPrimary,
                            modifier = Modifier.size(19.dp)
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
                            "截图、社交软件保存图或拍摄时未开启定位开关的照片/视频不会写入 GPS 坐标。你可以点击下方直接查看或批量补全定位。"
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

        // Bottom Collapsible Frosted-Glass Detail Card
        // Tap header or swipe vertically to expand/collapse; swipe horizontally to glide between locations
        val activeStopKey = when {
            showingUnlocated -> UNLOCATED_LOCATION_KEY
            selectedCluster != null -> selectedCluster.locationName
            else -> null
        }
        val cardSwipeNudgeX = remember { Animatable(0f) }
        val maxSwipeNudgePx = with(density) { 20.dp.toPx() }
        val swipeTriggerThresholdPx = with(density) { 42.dp.toPx() }

        AnimatedVisibility(
            visible = hasBottomCard && activeStopKey != null,
            enter = fadeIn(tween(185, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 3 },
            exit = fadeOut(tween(135)) +
                slideOutVertically(tween(160)) { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp + floatingDockInset)
        ) {
            val currentKey = activeStopKey ?: return@AnimatedVisibility
            val horizontalSwipeModifier = Modifier.pointerInput(scrubStops.size, activeStopIndex) {
                if (scrubStops.size < 2) return@pointerInput
                var totalDx = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        scope.launch { cardSwipeNudgeX.stop() }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDx += dragAmount
                        val damped = (totalDx * 0.24f).coerceIn(-maxSwipeNudgePx, maxSwipeNudgePx)
                        scope.launch { cardSwipeNudgeX.snapTo(damped) }
                    },
                    onDragCancel = {
                        scope.launch {
                            cardSwipeNudgeX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f)
                            )
                        }
                    },
                    onDragEnd = {
                        val delta = when {
                            totalDx < -swipeTriggerThresholdPx -> +1
                            totalDx > swipeTriggerThresholdPx -> -1
                            else -> 0
                        }
                        if (delta != 0) {
                            stepFootprintStop(delta)
                        }
                        scope.launch {
                            cardSwipeNudgeX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f)
                            )
                        }
                    }
                )
            }

            FrostedGlassCard(
                shape = RoundedCornerShape(26.dp),
                borderAccent = if (showingUnlocated) SolarAmber else null,
                shadowElevation = 14.dp,
                enableAmbientBlur = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = cardSwipeNudgeX.value
                    }
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                if (totalDrag < -36f) isCardExpanded = true
                                else if (totalDrag > 36f) isCardExpanded = false
                            }
                        )
                    }
                    .testTag(if (showingUnlocated) "unlocated_detail_card" else "location_detail_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Anchored grab handle strip (also responds to horizontal swipe & tap)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(horizontalSwipeModifier)
                            .clickable { isCardExpanded = !isCardExpanded }
                            .padding(top = 10.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(InkTertiary.copy(alpha = 0.35f))
                        )
                    }

                    // Unified directional left/right horizontal slide transition for all stops (including unlocated)
                    AnimatedContent(
                        targetState = currentKey,
                        transitionSpec = {
                            val dir = if (cardSlideDirection >= 0) 1 else -1
                            (slideInHorizontally(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> dir * (fullWidth * 0.36f).toInt() }
                            ) + fadeIn(tween(180, easing = FastOutSlowInEasing))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(170, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> -dir * (fullWidth * 0.32f).toInt() }
                                ) + fadeOut(tween(130))) using SizeTransform(clip = true)
                        },
                        label = "map_stop_card_horizontal_switch"
                    ) { stopKey ->
                        val stopIdx = remember(scrubStops, stopKey) {
                            scrubStops.indexOfFirst { it.id == stopKey }
                        }
                        if (stopKey == UNLOCATED_LOCATION_KEY) {
                            val unreviewedUnlocated = remember(unlocatedPhotos) {
                                unlocatedPhotos.count { it.photoStatus == PhotoStatus.UNREVIEWED }
                            }
                            val totalBytes = remember(unlocatedPhotos) { unlocatedPhotos.sumOf { it.fileSizeBytes } }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(horizontalSwipeModifier)
                                        .clickable { isCardExpanded = !isCardExpanded },
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "无定位信息的照片与视频",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
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
                                    if (scrubStops.size >= 2 && stopIdx >= 0) {
                                        Text(
                                            text = "${stopIdx + 1}/${scrubStops.size}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = InkTertiary
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isCardExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        val orderedUnlocated = remember(unlocatedPhotos, uiState.timelineSortOrder) {
                                            when (uiState.timelineSortOrder) {
                                                TimelineSortOrder.NEWEST_FIRST -> unlocatedPhotos.sortedByDescending { it.takenAtMillis }
                                                TimelineSortOrder.OLDEST_FIRST -> unlocatedPhotos.sortedBy { it.takenAtMillis }
                                            }
                                        }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(orderedUnlocated.take(24), key = { it.id }) { photo ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 72.dp, height = 84.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            lightboxPhotos = orderedUnlocated
                                                            lightboxPhoto = photo
                                                        }
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
                                                            .size(width = 76.dp, height = 84.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(StudioCanvas)
                                                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                onCleanLocation(UNLOCATED_LOCATION_KEY, unreviewedUnlocated == 0)
                                                            }
                                                    ) {
                                                        Text(
                                                            text = "查看全部",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CobaltPrimary,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(horizontalSwipeModifier)
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
                        } else {
                            val cluster = remember(clusters, stopKey) {
                                clusters.firstOrNull { it.locationName == stopKey }
                            } ?: selectedCluster
                            if (cluster != null) {
                                val latLabel = if (cluster.latitude >= 0) "N" else "S"
                                val lonLabel = if (cluster.longitude >= 0) "E" else "W"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(horizontalSwipeModifier)
                                            .clickable { isCardExpanded = !isCardExpanded },
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = cluster.locationName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = InkPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (cluster.unreviewedCount > 0) CoralSoft else EmeraldSoft
                                                ) {
                                                    Text(
                                                        text = if (cluster.unreviewedCount > 0) "${cluster.unreviewedCount} 待理" else "已整理",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (cluster.unreviewedCount > 0) CoralDanger else EmeraldKeep,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
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
                                                color = InkSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (scrubStops.size >= 2 && stopIdx >= 0) {
                                            Text(
                                                text = "${stopIdx + 1}/${scrubStops.size}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = InkTertiary
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = isCardExpanded) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            val orderedClusterPhotos = remember(cluster.photos, uiState.timelineSortOrder) {
                                                when (uiState.timelineSortOrder) {
                                                    TimelineSortOrder.NEWEST_FIRST -> cluster.photos.sortedByDescending { it.takenAtMillis }
                                                    TimelineSortOrder.OLDEST_FIRST -> cluster.photos.sortedBy { it.takenAtMillis }
                                                }
                                            }
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(orderedClusterPhotos.take(24), key = { it.id }) { photo ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = 72.dp, height = 84.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                lightboxPhotos = orderedClusterPhotos
                                                                lightboxPhoto = photo
                                                            }
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
                                                                .size(width = 76.dp, height = 84.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(StudioCanvas)
                                                                .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                                                                .clickable {
                                                                    onCleanLocation(cluster.locationName, cluster.unreviewedCount == 0)
                                                                }
                                                        ) {
                                                            Text(
                                                                text = "查看全部",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = CobaltPrimary,
                                                                textAlign = TextAlign.Center
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
                                                    .then(horizontalSwipeModifier)
                                                    .testTag("clean_location_button")
                                            )
                                        }
                                    }
                                }
                            }
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
        val list = lightboxPhotos.ifEmpty { listOf(photo) }
        val startIdx = list.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        PhotoDetailLightbox(
            photo = photo,
            photos = list,
            initialIndex = startIdx,
            onDismiss = { lightboxPhoto = null },
            onKeep = onKeepPhoto,
            onTrash = onTrashPhoto,
            onToggleFavorite = onToggleFavorite
        )
    }
}

