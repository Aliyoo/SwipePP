package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.components.DecoPosterPanel
import com.example.ui.components.DecoSunburst
import com.example.ui.components.DecoVermStrip
import com.example.ui.components.FootprintLocationScrubRail
import com.example.ui.components.FootprintMapStyle
import com.example.ui.components.FootprintScrubStop
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
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import java.util.Locale
import kotlin.math.abs

/**
 * 足迹地图 · 摩登画报（山河图）
 * 金双线画框包住全屏自绘山河图：金线经纬网格底 + 画片式地点照片印记（朱红计数徽）；
 * 底部地点卡 + 朱红通栏双动作条（查看此地/清理此地）。磨砂 → 画报纸面 mat。
 * 功能基线（87a2cd3）：Web Mercator 瓦片 + 离线矢量、图源切换、瓦片重试、GPS 解析进度、
 * ACCESS_MEDIA_LOCATION 授权、地点快速跳转 chips、右侧控制栏、可拖拽展开详情卡、
 * 批量补全定位（同日推断）——入口、testTag、回调全部保留。
 */
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
    val palette = LocalFlipYearPalette.current
    val ink = palette.decoInkOnGround
    val gold = palette.decoGold
    val verm = palette.decoVerm
    val mat = palette.decoMat
    // 纸黄 #EFE3C8 只压照片暗部与朱红底（可读性红线）
    val cream = DecoPaperCream
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

    val commitFootprintStop: (FootprintScrubStop, Boolean) -> Unit = { stop, shouldExpand ->
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
            commitFootprintStop(scrubStops[nextIdx], false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(palette.decoGround, palette.decoGroundDeep)
                )
            )
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

        // 金双线画框：外 1.5dp 金 + mat 内衬带 + 1dp 弱金（deco-app.html GoldFrame 语法）
        // 纯描边 Canvas，不注册 pointerInput，手势完全穿透到地图。
        // 画框顶线在状态栏下方（遥测卡下移入框内）；底线停在停靠栏上方——
        // floatingDockInset 只含 dock 高度，须再叠加系统导航条 inset，否则底线被 dock 吞掉
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = floatingDockInset + 10.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 8.dp)
        ) {
            val frameInset = 7.dp.toPx()
            val frameRadius = 2.dp.toPx()
            // mat 内衬带（6dp 描边带，覆盖瓦片边缘形成画框内衬）
            val bandHalf = 3.dp.toPx()
            drawRoundRect(
                color = mat,
                topLeft = Offset(frameInset + bandHalf, frameInset + bandHalf),
                size = Size(size.width - (frameInset + bandHalf) * 2, size.height - (frameInset + bandHalf) * 2),
                cornerRadius = CornerRadius(frameRadius),
                style = Stroke(width = bandHalf * 2)
            )
            drawRoundRect(
                color = gold,
                topLeft = Offset(frameInset, frameInset),
                size = Size(size.width - frameInset * 2, size.height - frameInset * 2),
                cornerRadius = CornerRadius(frameRadius),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val innerInset = frameInset + 5.5.dp.toPx()
            drawRoundRect(
                color = gold.copy(alpha = 0.40f),
                topLeft = Offset(innerInset, innerInset),
                size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                cornerRadius = CornerRadius(frameRadius),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 足迹山河图 水印（deco-app.html MapScreen 左下题签）
        Text(
            text = "足迹山河图",
            fontFamily = DecoDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 2.1.sp,
            color = ink.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(1f, 1f), blurRadius = 4f)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                // 题签上抬：避开导航条 + 停靠栏 + 底部地点卡（与画框底线同源 clearance）
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    bottom = 14.dp + floatingDockInset + if (selectedCluster != null || showingUnlocated) 132.dp else 0.dp
                )
        )

        // Top Compact Telemetry Pill + Map Layer Switcher
        // 下移入画框内：框线在状态栏下 ~15dp，卡片区再留 12dp 呼吸
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 顶部单行足迹遥测纸面药丸（点击回正/全览足迹，theirs 新交互）
                DecoPosterPanel(
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
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("map_stats_pill")
                ) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = if (uiState.hasDeviceMedia) 4.dp else 12.dp, top = 7.dp, bottom = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = stringResource(R.string.cd_map_fit_all),
                                tint = gold,
                                modifier = Modifier.size(16.dp)
                            )
                            val statsLabel = when {
                                uiState.isIndexingGps && uiState.gpsTotalCount > 0 ->
                                    "解析定位 ${uiState.gpsIndexedCount}/${uiState.gpsTotalCount}"
                                totalVisibleCount > 0 ->
                                    "${clusters.size} 个足迹 · 已定位 $locatedCount/$totalVisibleCount"
                                else -> "暂无带 GPS 信息的照片"
                            }
                            Text(
                                text = statsLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ink,
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
                                        tint = ink.copy(alpha = 0.72f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.isIndexingGps && uiState.gpsTotalCount > 0) {
                            val progress = (uiState.gpsIndexedCount.toFloat() / uiState.gpsTotalCount.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                color = gold,
                                trackColor = gold.copy(alpha = 0.22f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = if (uiState.hasDeviceMedia) 8.dp else 0.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }

                // Map Layer Switcher Pill → 画报方章式图源切换
                DecoPosterPanel(
                    onClick = {
                        val entries = FootprintMapStyle.entries
                        val next = entries[(entries.indexOf(mapStyle) + 1) % entries.size]
                        mapStyleName = next.name
                        if (next == FootprintMapStyle.AMAP_ZH && !cameraState.isFramingChinaOverview()) {
                            cameraState.focusChinaRegion(scope, clusters, durationMs = 480)
                        }
                    },
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("map_layer_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = stringResource(R.string.cd_map_layer),
                            tint = gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = mapStyle.label,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ink
                        )
                    }
                }
            }

            // Explicit ACCESS_MEDIA_LOCATION authorization prompt when system redacts EXIF GPS
            if (uiState.hasDeviceMedia && !uiState.hasMediaLocationPermission) {
                DecoPosterPanel(
                    borderAlpha = 0.70f,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                            tint = verm,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "未开启媒体位置权限，系统已隐藏照片 GPS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ink
                            )
                            Text(
                                text = "授权后将自动回溯全部照片与视频的真实拍摄地点",
                                fontSize = 12.sp,
                                color = ink.copy(alpha = 0.72f)
                            )
                        }
                        GalleryActionButton(
                            text = "授权",
                            onClick = onRequestMediaLocationPermission,
                            tone = GalleryButtonTone.IMMERSIVE,
                            height = 38.dp,
                            shape = RoundedCornerShape(3.dp),
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
                DecoPosterPanel(
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                                tint = gold,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "当前仅显示${uiState.mediaFilter.label}",
                                fontSize = 13.sp,
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ink
                            )
                        }
                        TextButton(onClick = onResetGlobalMediaFilter) {
                            Text(
                                "显示全部",
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = gold
                            )
                        }
                    }
                }
            }

            // Subtle retry pill only shown when online tiles fail to load (e.g. temporary offline state)
            if (mapStyle != FootprintMapStyle.VECTOR && tileFailedCount > 0 && tileLoadedCount == 0) {
                DecoPosterPanel(
                    onClick = { tileRetryTrigger++ },
                    borderAlpha = 0.70f,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .testTag("map_tile_retry_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_retry_map_tiles),
                            tint = verm,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_retry_map_tiles),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ink
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
                onCommitStop = { stop -> commitFootprintStop(stop, false) },
                topPadding = if (uiState.hasDeviceMedia && !uiState.hasMediaLocationPermission) 144.dp else 68.dp,
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
            // 缩放胶囊画报纸面化：DecoPosterPanel 替换磨砂玻璃（theirs 交互骨架原样保留）
            DecoPosterPanel(shadowElevation = 5.dp) {
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
                                tint = gold,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(0.8.dp)
                                .height(22.dp)
                                .background(gold.copy(alpha = 0.45f))
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
                            tint = ink,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .height(22.dp)
                            .background(gold.copy(alpha = 0.45f))
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
                            tint = ink,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        if (clusters.isEmpty() && !showingUnlocated) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 36.dp)
            ) {
                // 放射太阳纹衬底（24 扇形 · 金 0.14）
                DecoSunburst(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 0.dp)
                        .size(230.dp),
                    opacity = 0.14f
                )
                DecoPosterPanel(
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 26.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = if (unlocatedPhotos.isNotEmpty()) {
                                "当前 ${unlocatedPhotos.size} 项媒体均不含 GPS 信息"
                            } else {
                                "尚未发现带定位信息的照片"
                            },
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ink,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (unlocatedPhotos.isNotEmpty()) {
                                "截图、社交软件保存图或拍摄时未开启定位开关的照片/视频不会写入 GPS 坐标。你可以点击下方直接查看或批量补全定位。"
                            } else {
                                "当本地相册包含 GPS 拍摄位置时，这里会自动在地图上聚合足迹并支持按地点批量清理。"
                            },
                            fontSize = 14.sp,
                            color = ink.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                        if (unlocatedPhotos.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GalleryActionButton(
                                    text = "批量补全定位",
                                    leadingIcon = Icons.Filled.EditLocationAlt,
                                    onClick = { showBatchLocationSheet = true },
                                    tone = GalleryButtonTone.IMMERSIVE,
                                    height = 46.dp,
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("empty_map_batch_location_button")
                                )
                                DecoGhostButton(
                                    text = "查看 (${unlocatedPhotos.size})",
                                    onClick = {
                                        selectedLocationName = UNLOCATED_LOCATION_KEY
                                        isCardExpanded = true
                                    },
                                    modifier = Modifier.weight(0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部可折叠画报详情卡（点击卡头或纵向拖拽展开/收起；选中有定位地点 或 无定位媒体清单）
        AnimatedVisibility(
            visible = hasBottomCard,
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
        if (showingUnlocated) {
            val unreviewedUnlocated = remember(unlocatedPhotos) {
                unlocatedPhotos.count { it.photoStatus == PhotoStatus.UNREVIEWED }
            }
            val totalBytes = remember(unlocatedPhotos) { unlocatedPhotos.sumOf { it.fileSizeBytes } }

            DecoPosterPanel(
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
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
                    .testTag("unlocated_detail_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 拖拽提手：金短杠
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(gold.copy(alpha = 0.60f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(scrubStops.size, activeStopIndex) {
                                if (scrubStops.size < 2) return@pointerInput
                                var totalDx = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDx = 0f },
                                    onHorizontalDrag = { _, dragAmount -> totalDx += dragAmount },
                                    onDragEnd = {
                                        if (totalDx < -48f) stepFootprintStop(+1)
                                        else if (totalDx > 48f) stepFootprintStop(-1)
                                    }
                                )
                            }
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
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${unlocatedPhotos.size} 项媒体 · ${formatStorageSize(totalBytes)} · 截图/网图/未开定位拍摄",
                                fontFamily = DecoDisplayFontFamily,
                                fontSize = 12.sp,
                                color = ink.copy(alpha = 0.68f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (scrubStops.size >= 2) {
                            CompactLocationStepper(
                                onPrevious = { stepFootprintStop(-1) },
                                onNext = { stepFootprintStop(+1) }
                            )
                        }

                        // 画报展开/收起方钮（HEAD 保留的显式开合暗示）
                        IconButton(
                            onClick = { isCardExpanded = !isCardExpanded },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isCardExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                contentDescription = if (isCardExpanded) "收起卡片" else "展开卡片",
                                tint = ink.copy(alpha = 0.75f)
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
                            DecoThumbRail(
                                photos = orderedUnlocated,
                                onPhotoClick = { photo ->
                                    lightboxPhotos = orderedUnlocated
                                    lightboxPhoto = photo
                                },
                                showAllLabel = if (unlocatedPhotos.size > 24) "查看全部" else null,
                                onShowAllClick = { onCleanLocation(UNLOCATED_LOCATION_KEY, unreviewedUnlocated == 0) },
                                keyPrefix = "unlocated"
                            )

                            // 朱红通栏双动作条
                            DecoVermStrip(
                                leftLabel = "批量补全定位",
                                rightLabel = if (unreviewedUnlocated > 0) "整理 ($unreviewedUnlocated)" else "浏览全部",
                                onLeft = { showBatchLocationSheet = true },
                                onRight = {
                                    onCleanLocation(UNLOCATED_LOCATION_KEY, unreviewedUnlocated == 0)
                                },
                                leftTestTag = "batch_tag_unlocated_button",
                                rightTestTag = "clean_unlocated_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else selectedCluster?.let { activeCluster ->
            DecoPosterPanel(
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
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
                    .testTag("location_detail_card")
            ) {
                AnimatedContent(
                    targetState = activeCluster,
                    contentKey = { it.locationName },
                    transitionSpec = {
                        (fadeIn(tween(165, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(175, easing = FastOutSlowInEasing)) { it / 12 }) togetherWith
                            (fadeOut(tween(105)) + slideOutVertically(tween(110)) { -it / 14 })
                    },
                    label = "map_cluster_card_switch"
                ) { cluster ->
                val latLabel = if (cluster.latitude >= 0) "N" else "S"
                val lonLabel = if (cluster.longitude >= 0) "E" else "W"
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(gold.copy(alpha = 0.60f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(scrubStops.size, activeStopIndex) {
                                if (scrubStops.size < 2) return@pointerInput
                                var totalDx = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDx = 0f },
                                    onHorizontalDrag = { _, dragAmount -> totalDx += dragAmount },
                                    onDragEnd = {
                                        if (totalDx < -48f) stepFootprintStop(+1)
                                        else if (totalDx > 48f) stepFootprintStop(-1)
                                    }
                                )
                            }
                            .clickable { isCardExpanded = !isCardExpanded },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 地点卡：52dp 金框封面印记 + 美术字地名 + 坐标元数据
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(mat)
                                    .border(1.5.dp, gold, RoundedCornerShape(3.dp))
                                    .padding(2.dp)
                            ) {
                                PhotoArtwork(
                                    photo = cluster.coverPhoto,
                                    contentDescription = cluster.locationName,
                                    showVideoBadge = false,
                                    downsampleTier = ImageDownsampleTier.MICRO_THUMB,
                                    targetSizePx = 160,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cluster.locationName,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = ink,
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
                                    fontSize = 12.sp,
                                    color = ink.copy(alpha = 0.68f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DecoSealTag(
                                pendingCount = cluster.unreviewedCount,
                                ink = ink,
                                verm = verm,
                                gold = gold
                            )

                            IconButton(
                                onClick = { isCardExpanded = !isCardExpanded },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("toggle_location_card_button")
                            ) {
                                Icon(
                                    imageVector = if (isCardExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                    contentDescription = if (isCardExpanded) "收起卡片" else "展开卡片",
                                    tint = ink.copy(alpha = 0.75f)
                                )
                            }

                            // theirs 新增：单手地点快切步进器（逻辑原样保留）
                            if (scrubStops.size >= 2) {
                                CompactLocationStepper(
                                    onPrevious = { stepFootprintStop(-1) },
                                    onNext = { stepFootprintStop(+1) }
                                )
                            }
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
                            DecoThumbRail(
                                photos = orderedClusterPhotos,
                                onPhotoClick = { photo ->
                                    lightboxPhotos = orderedClusterPhotos
                                    lightboxPhoto = photo
                                },
                                showAllLabel = if (cluster.photos.size > 24) "查看全部" else null,
                                onShowAllClick = { onCleanLocation(cluster.locationName, cluster.unreviewedCount == 0) },
                                keyPrefix = "cluster_${cluster.locationName}"
                            )

                            // 朱红通栏：查看此地媒体 / 清理此地
                            DecoVermStrip(
                                leftLabel = "查看此地",
                                rightLabel = "清理此地",
                                onLeft = {
                                    if (orderedClusterPhotos.isNotEmpty()) {
                                        lightboxPhotos = orderedClusterPhotos
                                        lightboxPhoto = orderedClusterPhotos.first()
                                    }
                                },
                                onRight = {
                                    onCleanLocation(cluster.locationName, cluster.unreviewedCount == 0)
                                },
                                leftFlex = 1.2f,
                                leftTestTag = "view_location_media_button",
                                rightTestTag = "clean_location_button",
                                modifier = Modifier.fillMaxWidth()
                            )
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

// =====================================================================================
// 屏内画报一次性组件（色彩全部从主题 Deco 令牌派生，不引入硬编码色值）
// =====================================================================================

/** 待理/已整理 方章小签：待理朱红底纸黄字（纸黄压朱红底红线），已整理弱金底墨字 */
@Composable
private fun DecoSealTag(
    pendingCount: Int,
    ink: Color,
    verm: Color,
    gold: Color
) {
    val hasPending = pendingCount > 0
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(3.dp),
        color = if (hasPending) verm else gold.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (hasPending) DecoPaperCream.copy(alpha = 0.45f) else gold.copy(alpha = 0.55f)
        )
    ) {
        Text(
            text = if (hasPending) "$pendingCount 待理" else "已整理",
            fontFamily = DecoDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (hasPending) DecoPaperCream else ink,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** 24 张缩略画片横排 + 可选「查看全部」画片（金发丝框 · 2~4dp 小圆角） */
@Composable
private fun DecoThumbRail(
    photos: List<PhotoItemEntity>,
    onPhotoClick: (PhotoItemEntity) -> Unit,
    showAllLabel: String?,
    onShowAllClick: () -> Unit,
    keyPrefix: String
) {
    val palette = LocalFlipYearPalette.current
    val gold = palette.decoGold
    val ink = palette.decoInkOnGround
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(photos.take(24), key = { it.id }) { photo ->
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 84.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(1.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                    .clickable { onPhotoClick(photo) }
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
        if (showAllLabel != null) {
            item(key = "${keyPrefix}_more") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 76.dp, height = 84.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(ink.copy(alpha = 0.06f))
                        .border(1.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                        .clickable(onClick = onShowAllClick)
                ) {
                    Text(
                        text = showAllLabel,
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = gold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** 金描边幽灵按钮（画报透明纸面 + 1.5dp 金框 + 金字） */
@Composable
private fun DecoGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFlipYearPalette.current
    val gold = palette.decoGold
    val ink = palette.decoInkOnGround
    val shape = RoundedCornerShape(3.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(ink.copy(alpha = 0.05f))
            .border(1.5.dp, gold, shape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontFamily = DecoDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.4.sp,
            color = gold,
            maxLines = 1
        )
    }
}

/** theirs 新增：底部卡头单手地点步进器（逻辑与 testTag 原样保留，样式画报化：纸面 + 金发丝描边） */
@Composable
private fun CompactLocationStepper(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFlipYearPalette.current
    val gold = palette.decoGold
    val ink = palette.decoInkOnGround
    val pillShape = RoundedCornerShape(3.dp)
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(pillShape)
            .background(ink.copy(alpha = 0.05f))
            .border(0.8.dp, gold.copy(alpha = 0.55f), pillShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 34.dp, height = 34.dp)
                .clickable(onClick = onPrevious)
                .testTag("map_prev_location_button")
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.cd_footprint_prev_location),
                tint = ink,
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .width(0.8.dp)
                .height(16.dp)
                .background(gold.copy(alpha = 0.45f))
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 34.dp, height = 34.dp)
                .clickable(onClick = onNext)
                .testTag("map_next_location_button")
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.cd_footprint_next_location),
                tint = ink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

