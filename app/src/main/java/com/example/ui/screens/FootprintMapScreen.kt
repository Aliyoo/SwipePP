package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.MediaFilter
import com.example.ui.UNLOCATED_LOCATION_KEY
import com.example.ui.components.BatchLocationEditorSheet
import com.example.ui.components.DecoPosterPanel
import com.example.ui.components.DecoSunburst
import com.example.ui.components.DecoVermStrip
import com.example.ui.components.FootprintMapStyle
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
import com.example.ui.theme.EditorialSerifFontFamily
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

    val floatingDockInset = LocalFloatingDockInset.current
    val clusters = uiState.locationClusters
    val unlocatedPhotos = uiState.unlocatedPhotos
    val locatedCount = remember(clusters) { clusters.sumOf { it.photos.size } }
    val totalVisibleCount = locatedCount + unlocatedPhotos.size

    var selectedLocationName by rememberSaveable { mutableStateOf<String?>(null) }
    var isCardExpanded by rememberSaveable { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    var lightboxPhotos by remember { mutableStateOf<List<PhotoItemEntity>>(emptyList()) }
    var showBatchLocationSheet by remember { mutableStateOf(false) }
    val locationChipsState = rememberLazyListState()

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
        // 纯描边 Canvas，不注册 pointerInput，手势完全穿透到地图
        Canvas(modifier = Modifier.fillMaxSize()) {
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
            fontFamily = EditorialSerifFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 2.1.sp,
            color = ink.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(1f, 1f), blurRadius = 4f)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
        )

        // Top Floating Header, Permission Prompt & Location Quick-Jump Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 顶部磨砂信息卡 → 画报纸面信息卡
                DecoPosterPanel(
                    shadowElevation = 4.dp,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = if (uiState.hasDeviceMedia) 4.dp else 12.dp, top = 7.dp, bottom = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Explore,
                                contentDescription = null,
                                tint = gold,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "足迹地图",
                                    fontFamily = EditorialSerifFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = ink
                                )
                                val subtitle = when {
                                    uiState.isIndexingGps && uiState.gpsTotalCount > 0 ->
                                        "正在解析定位 (${uiState.gpsIndexedCount}/${uiState.gpsTotalCount})"
                                    totalVisibleCount > 0 ->
                                        "${clusters.size} 个足迹 · 已定位 $locatedCount / $totalVisibleCount 项"
                                    else -> "暂无带 GPS 信息的照片"
                                }
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = ink.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (uiState.hasDeviceMedia) {
                                IconButton(
                                    onClick = { onTriggerGpsBackfill(true) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("map_rescan_gps_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "重新解析全量照片定位",
                                        tint = ink.copy(alpha = 0.72f),
                                        modifier = Modifier.size(18.dp)
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
                            fontFamily = EditorialSerifFontFamily,
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

            if (clusters.isNotEmpty() || unlocatedPhotos.isNotEmpty()) {
                LazyRow(
                    state = locationChipsState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clusters, key = { it.locationName }) { cluster ->
                        val selected = !showingUnlocated && cluster.locationName == selectedCluster?.locationName
                        DecoFootprintChip(
                            label = cluster.locationName,
                            selected = selected,
                            count = cluster.unreviewedCount,
                            countTone = DecoChipBadgeTone.VERM,
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
                        )
                    }

                    // Transparent chip for media files without embedded GPS coordinates (screenshots, saved images, etc.)
                    if (unlocatedPhotos.isNotEmpty()) {
                        item(key = UNLOCATED_LOCATION_KEY) {
                            DecoFootprintChip(
                                label = "无定位信息",
                                selected = showingUnlocated,
                                dashed = true,
                                count = unlocatedPhotos.size,
                                countTone = DecoChipBadgeTone.NEUTRAL,
                                onClick = {
                                    selectedLocationName = UNLOCATED_LOCATION_KEY
                                    isCardExpanded = true
                                },
                                modifier = Modifier.testTag("location_chip_unlocated")
                            )
                        }
                    }
                }
            }

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
                                fontWeight = FontWeight.Bold,
                                color = ink
                            )
                        }
                        TextButton(onClick = onResetGlobalMediaFilter) {
                            Text(
                                "显示全部",
                                fontFamily = EditorialSerifFontFamily,
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

        // Right-side Floating Zoom, Fit-All & GPS Re-Index Controls (dynamically clears expanded bottom card)
        val hasBottomCard = selectedCluster != null || showingUnlocated
        val controlsBottomPadding by animateDpAsState(
            targetValue = when {
                hasBottomCard && isCardExpanded -> 268.dp + floatingDockInset * 0.45f
                hasBottomCard -> 132.dp + floatingDockInset * 0.4f
                else -> 0.dp
            },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "map_controls_clearance"
        )
        // Unified single right-side paper map control rail (Zoom + Fit-All)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    end = 16.dp,
                    bottom = controlsBottomPadding
                )
                .pointerInput(Unit) {}
        ) {
            DecoPosterPanel(shadowElevation = 5.dp) {
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
                            tint = ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.width(24.dp),
                        thickness = 1.dp,
                        color = gold.copy(alpha = 0.45f)
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
                            tint = ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (clusters.isNotEmpty() || mapStyle == FootprintMapStyle.AMAP_ZH) {
                        HorizontalDivider(
                            modifier = Modifier.width(24.dp),
                            thickness = 1.dp,
                            color = gold.copy(alpha = 0.45f)
                        )
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                            fontFamily = EditorialSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ink,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (unlocatedPhotos.isNotEmpty()) {
                                "截图、社交软件保存图或拍摄时未开启定位开关的照片/视频不会写入 GPS 坐标。你可以点击上方「无定位信息」直接浏览并整理。"
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

        // Bottom Collapsible Paper Detail Card (Either Selected Geotagged Cluster OR Unlocated Media Sheet)
        if (showingUnlocated) {
            val unreviewedUnlocated = remember(unlocatedPhotos) {
                unlocatedPhotos.count { it.photoStatus == PhotoStatus.UNREVIEWED }
            }
            val totalBytes = remember(unlocatedPhotos) { unlocatedPhotos.sumOf { it.fileSizeBytes } }

            DecoPosterPanel(
                shadowElevation = 14.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp + floatingDockInset)
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
                            .clickable { isCardExpanded = !isCardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "无定位信息的照片与视频",
                                fontFamily = EditorialSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${unlocatedPhotos.size} 项媒体 · ${formatStorageSize(totalBytes)} · 截图/网图/未开定位拍摄",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = ink.copy(alpha = 0.68f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

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
        } else selectedCluster?.let { cluster ->
            val latLabel = if (cluster.latitude >= 0) "N" else "S"
            val lonLabel = if (cluster.longitude >= 0) "E" else "W"

            DecoPosterPanel(
                shadowElevation = 14.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp + floatingDockInset)
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
                            .clickable { isCardExpanded = !isCardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                                    fontFamily = EditorialSerifFontFamily,
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

private enum class DecoChipBadgeTone { VERM, NEUTRAL }

/** 地点快速跳转 chips：金描边纸面方章；[dashed] 用于「无定位信息」（虚线金框），[count] 为朱红/中性计数徽 */
@Composable
private fun DecoFootprintChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    countTone: DecoChipBadgeTone = DecoChipBadgeTone.VERM,
    dashed: Boolean = false
) {
    val palette = LocalFlipYearPalette.current
    val ink = palette.decoInkOnGround
    val gold = palette.decoGold
    val verm = palette.decoVerm
    val shape = RoundedCornerShape(3.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (pressed) 0.96f else 1f
                scaleY = if (pressed) 0.96f else 1f
            }
            .clip(shape)
            .background(if (selected) gold.copy(alpha = 0.16f) else ink.copy(alpha = 0.05f))
            .then(
                if (dashed) {
                    Modifier.drawBehind {
                        // 虚线金框（deco-app.html 无定位印记：borderStyle dashed）
                        val half = 1.dp.toPx() / 2f
                        drawRoundRect(
                            color = gold.copy(alpha = 0.75f),
                            topLeft = Offset(half, half),
                            size = Size(size.width - half * 2, size.height - half * 2),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
                            )
                        )
                    }
                } else {
                    Modifier.border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) gold else gold.copy(alpha = 0.55f),
                        shape = shape
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = ink),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) ink else ink.copy(alpha = 0.85f),
                maxLines = 1
            )
            if (count != null && count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when (countTone) {
                                DecoChipBadgeTone.VERM -> verm
                                DecoChipBadgeTone.NEUTRAL -> gold.copy(alpha = 0.20f)
                            }
                        )
                        .then(
                            if (countTone == DecoChipBadgeTone.VERM) {
                                Modifier.border(1.dp, DecoPaperCream.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        fontFamily = JetBrainsMonoFontFamily,
                        // 标签字号红线 ≥12sp：计数徽从原型 11px 升 12sp（原型例外已记录）
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (countTone == DecoChipBadgeTone.VERM) DecoPaperCream else ink
                    )
                }
            }
        }
    }
}

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
            fontFamily = EditorialSerifFontFamily,
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
                        fontFamily = EditorialSerifFontFamily,
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
            fontFamily = EditorialSerifFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.4.sp,
            color = gold,
            maxLines = 1
        )
    }
}

