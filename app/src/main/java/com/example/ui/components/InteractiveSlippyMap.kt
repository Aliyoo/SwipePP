package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.data.FlipYearImageLoaders
import com.example.data.ImageDownsampleTier
import com.example.ui.LocationCluster
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class FootprintMapStyle(val label: String) {
    AMAP_ZH("中文"),
    GLOBAL("国际"),
    OSM("OSM"),
    VECTOR("矢量")
}

private const val MIN_ZOOM = 2.0f
private const val MAX_ZOOM = 15.5f
private const val BASE_TILE_DP = 256f
private const val MAX_TRAVEL_ARCS = 8
private const val MAX_RENDERED_TILES = 120

const val CHINA_DEFAULT_LAT = 35.4
const val CHINA_DEFAULT_LON = 104.8
const val CHINA_DEFAULT_ZOOM = 3.45f
private const val CHINA_MIN_FIT_ZOOM = 3.45f
private const val CHINA_MAX_FIT_ZOOM = 7.2f

// Hue-preserving luminance inversion matrix (Invert * HueRotate180 scaled to 0.85 over #141822 obsidian base)
// Turns daylight map tiles (AMap, ArcGIS, OSM) into sleek low-glare nocturnal darkroom cartography while keeping water blue and labels crisp.
private val NocturnalMapTileColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.488f, -1.215f, -0.122f, 0f, 237f,
            -0.362f, -0.366f, -0.122f, 0f, 241f,
            -0.362f, -1.215f, 0.728f, 0f, 251f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

// 亮色系「山河图」复古制图矩阵：Rec.709 去饱和 55%（s=0.45）后按行缩放 R×0.965 / G×0.94 / B×0.875，
// 纸白瓦片底 → #F6F0DF 暖羊皮纸。现代冷蓝高饱瓦片直接放在暖纸金框里会造成跨页风格割裂；
// 经此矩阵后水系呈钢青、绿地呈橄榄灰绿、道路呈琥珀，整体落入摩登画报色谱
private val ParchmentMapTileColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.547f, 0.380f, 0.038f, 0f, 0f,
            0.110f, 0.793f, 0.037f, 0f, 0f,
            0.102f, 0.344f, 0.429f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

fun lonToNormX(lon: Double): Double {
    val safeLon = if (lon.isFinite()) lon.coerceIn(-180.0, 180.0) else 0.0
    return (safeLon + 180.0) / 360.0
}

fun latToNormY(lat: Double): Double {
    val safeLat = if (lat.isFinite()) lat.coerceIn(-85.0511, 85.0511) else 0.0
    val rad = Math.toRadians(safeLat)
    val merc = ln(tan(rad) + 1.0 / cos(rad))
    val ny = (1.0 - merc / PI) / 2.0
    return if (ny.isFinite()) ny.coerceIn(0.0, 1.0) else 0.5
}

fun isInsideChinaMainland(lat: Double, lon: Double): Boolean {
    return lat in 0.8293..55.8271 && lon in 72.004..137.8347
}

private fun wgs84ToGcj02(lat: Double, lon: Double): Pair<Double, Double> {
    if (!isInsideChinaMainland(lat, lon)) return lat to lon
    val a = 6378245.0
    val ee = 0.00669342162296594323
    val x = lon - 105.0
    val y = lat - 35.0
    var dLat = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
    dLat += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    dLat += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
    dLat += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0

    var dLon = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
    dLon += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    dLon += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
    dLon += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0

    val radLat = lat / 180.0 * PI
    var magic = sin(radLat)
    magic = 1.0 - ee * magic * magic
    val sqrtMagic = sqrt(magic)
    dLat = (dLat * 180.0) / ((a * (1.0 - ee)) / (magic * sqrtMagic) * PI)
    dLon = (dLon * 180.0) / (a / sqrtMagic * cos(radLat) * PI)
    return (lat + dLat) to (lon + dLon)
}

private fun wrapNormX(x: Double): Double {
    if (!x.isFinite()) return 0.5
    val m = x % 1.0
    return if (m < 0.0) m + 1.0 else m
}

internal data class ProjectedClusterMeta(
    val cluster: LocationCluster,
    val normX: Double,
    val normY: Double
)

internal data class DisplayedMapPin(
    val stableKey: String,
    val primaryCluster: LocationCluster,
    val mergedClustersCount: Int,
    val totalPhotosCount: Int,
    val totalUnreviewedCount: Int,
    val screenX: Float,
    val screenY: Float,
    val isSelected: Boolean
)

internal data class MapCameraTarget(
    val lat: Double,
    val lon: Double,
    val zoom: Float,
    val verticalBiasFraction: Float = 0.04f
)

internal fun computeChinaFocusTarget(clusters: List<LocationCluster>): MapCameraTarget {
    val chinaClusters = clusters.filter { isInsideChinaMainland(it.latitude, it.longitude) }
    if (chinaClusters.isEmpty()) {
        return MapCameraTarget(
            lat = CHINA_DEFAULT_LAT,
            lon = CHINA_DEFAULT_LON,
            zoom = CHINA_DEFAULT_ZOOM,
            verticalBiasFraction = 0.04f
        )
    }
    if (chinaClusters.size == 1) {
        val only = chinaClusters.first()
        return MapCameraTarget(
            lat = only.latitude,
            lon = only.longitude,
            zoom = 5.8f,
            verticalBiasFraction = 0.06f
        )
    }

    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    var sumLat = 0.0
    var sumLon = 0.0
    for (c in chinaClusters) {
        val x = lonToNormX(c.longitude)
        val y = latToNormY(c.latitude)
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
        sumLat += c.latitude
        sumLon += c.longitude
    }

    val spanX = (maxX - minX).coerceAtLeast(0.01)
    val spanY = (maxY - minY).coerceAtLeast(0.01)
    val maxSpan = max(spanX * 1.48, spanY * 1.9)
    val computedZoom = (ln(1.0 / maxSpan) / ln(2.0)).toFloat().coerceIn(CHINA_MIN_FIT_ZOOM, CHINA_MAX_FIT_ZOOM)
    val centerLat = (chinaClusters.minOf { it.latitude } + chinaClusters.maxOf { it.latitude }) / 2.0
    val centerLon = (chinaClusters.minOf { it.longitude } + chinaClusters.maxOf { it.longitude }) / 2.0
    // Guard against degenerate coordinates
    val safeLat = if (centerLat.isFinite()) centerLat else sumLat / chinaClusters.size
    val safeLon = if (centerLon.isFinite()) centerLon else sumLon / chinaClusters.size
    return MapCameraTarget(
        lat = safeLat,
        lon = safeLon,
        zoom = computedZoom,
        verticalBiasFraction = 0.05f
    )
}

private data class TileCandidate(
    val url: String,
    val userAgent: String,
    val referer: String? = null,
    val isGcj02: Boolean = false
)

@Stable
class SlippyMapCameraState(
    initialLat: Double = CHINA_DEFAULT_LAT,
    initialLon: Double = CHINA_DEFAULT_LON,
    initialZoom: Float = CHINA_DEFAULT_ZOOM
) {
    var centerX by mutableDoubleStateOf(lonToNormX(initialLon))
    var centerY by mutableDoubleStateOf(latToNormY(initialLat).coerceIn(0.08, 0.92))
    var zoom by mutableFloatStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))

    private var animationJob: Job? = null

    fun cancelAnimation() {
        animationJob?.cancel()
        animationJob = null
    }

    fun panByPixels(dxPx: Float, dyPx: Float, tileSizePx: Float) {
        if (tileSizePx <= 1f || !dxPx.isFinite() || !dyPx.isFinite()) return
        val worldPx = tileSizePx * 2.0.pow(zoom.toDouble())
        if (worldPx <= 1.0) return
        centerX = wrapNormX(centerX - dxPx / worldPx)
        centerY = (centerY - dyPx / worldPx).coerceIn(0.06, 0.94)
    }

    fun zoomAroundScreenPoint(
        zoomFactor: Float,
        centroidX: Float,
        centroidY: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        tileSizePx: Float
    ) {
        if (!zoomFactor.isFinite() || zoomFactor <= 0f || tileSizePx <= 1f) return
        val oldZoom = zoom
        val newZoom = (oldZoom + (ln(zoomFactor.toDouble()) / ln(2.0)).toFloat()).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (newZoom == oldZoom) return

        val oldWorldPx = tileSizePx * 2.0.pow(oldZoom.toDouble())
        val newWorldPx = tileSizePx * 2.0.pow(newZoom.toDouble())
        val dxFromCenter = centroidX - viewportWidthPx / 2f
        val dyFromCenter = centroidY - viewportHeightPx / 2f

        val focusWorldX = centerX + dxFromCenter / oldWorldPx
        val focusWorldY = centerY + dyFromCenter / oldWorldPx

        zoom = newZoom
        centerX = wrapNormX(focusWorldX - dxFromCenter / newWorldPx)
        centerY = (focusWorldY - dyFromCenter / newWorldPx).coerceIn(0.06, 0.94)
    }

    fun animateZoomBy(scope: CoroutineScope, deltaZoom: Float) {
        val targetZoom = (zoom + deltaZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
        animateToNorm(scope, centerX, centerY, targetZoom, durationMs = 280)
    }

    fun animateToLatLng(
        scope: CoroutineScope,
        lat: Double,
        lon: Double,
        targetZoom: Float = 5.6f,
        verticalBiasFraction: Float = 0.08f,
        durationMs: Int = 550
    ) {
        val tx = lonToNormX(lon)
        // Bias target slightly south so the pin sits cleanly above the bottom detail card
        val clampedZoom = targetZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val extraYOffset = (verticalBiasFraction / 2.0.pow(clampedZoom.toDouble())) * 0.45
        val ty = (latToNormY(lat) + extraYOffset).coerceIn(0.08, 0.92)
        animateToNorm(scope, tx, ty, clampedZoom, durationMs)
    }

    fun fitClusters(
        scope: CoroutineScope,
        clusters: List<LocationCluster>,
        durationMs: Int = 580
    ) {
        if (clusters.isEmpty()) {
            animateToLatLng(scope, 32.0, 105.0, 2.4f, verticalBiasFraction = 0f, durationMs = durationMs)
            return
        }
        if (clusters.size == 1) {
            val only = clusters.first()
            animateToLatLng(scope, only.latitude, only.longitude, 6.2f, durationMs = durationMs)
            return
        }

        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (c in clusters) {
            val x = lonToNormX(c.longitude)
            val y = latToNormY(c.latitude)
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        val spanX = (maxX - minX).coerceAtLeast(0.008)
        val spanY = (maxY - minY).coerceAtLeast(0.008)
        val maxSpan = max(spanX * 1.45, spanY * 1.85)
        val computedZoom = (ln(1.0 / maxSpan) / ln(2.0)).toFloat().coerceIn(2.0f, 8.5f)
        val targetX = (minX + maxX) / 2.0
        val targetY = ((minY + maxY) / 2.0 + 0.03 / 2.0.pow(computedZoom.toDouble())).coerceIn(0.08, 0.92)
        animateToNorm(scope, targetX, targetY, computedZoom, durationMs)
    }

    fun focusChinaRegion(
        scope: CoroutineScope,
        clusters: List<LocationCluster>,
        durationMs: Int = 520
    ) {
        val target = computeChinaFocusTarget(clusters)
        animateToLatLng(
            scope = scope,
            lat = target.lat,
            lon = target.lon,
            targetZoom = target.zoom,
            verticalBiasFraction = target.verticalBiasFraction,
            durationMs = durationMs
        )
    }

    fun isFramingChinaOverview(): Boolean {
        val chinaX = lonToNormX(CHINA_DEFAULT_LON)
        val chinaY = latToNormY(CHINA_DEFAULT_LAT)
        val inChinaBounds = abs(centerX - chinaX) < 0.095 && abs(centerY - chinaY) < 0.085
        return inChinaBounds && zoom in (CHINA_DEFAULT_ZOOM - 0.35f)..(CHINA_DEFAULT_ZOOM + 1.2f)
    }

    private fun animateToNorm(
        scope: CoroutineScope,
        targetX: Double,
        targetY: Double,
        targetZoom: Float,
        durationMs: Int
    ) {
        animationJob?.cancel()
        val startX = centerX
        val startY = centerY
        val startZoom = zoom

        // Pick shortest longitudinal path across the antimeridian
        var deltaX = targetX - startX
        if (deltaX > 0.5) deltaX -= 1.0
        if (deltaX < -0.5) deltaX += 1.0

        val deltaY = targetY - startY
        val deltaZ = targetZoom - startZoom

        animationJob = scope.launch {
            val anim = Animatable(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
            ) {
                val t = value.toDouble()
                centerX = wrapNormX(startX + deltaX * t)
                centerY = (startY + deltaY * t).coerceIn(0.06, 0.94)
                zoom = (startZoom + deltaZ * value).coerceIn(MIN_ZOOM, MAX_ZOOM)
            }
        }
    }
}

@Composable
fun rememberSlippyMapCameraState(
    initialLat: Double = CHINA_DEFAULT_LAT,
    initialLon: Double = CHINA_DEFAULT_LON,
    initialZoom: Float = CHINA_DEFAULT_ZOOM
): SlippyMapCameraState {
    return remember {
        SlippyMapCameraState(
            initialLat = initialLat,
            initialLon = initialLon,
            initialZoom = initialZoom
        )
    }
}

private const val MOBILE_BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val OSM_APP_UA =
    "SwipePPAndroid/1.0 (PhotoFootprintMap; +https://swipepp.app)"

private fun buildTileCandidates(style: FootprintMapStyle, z: Int, x: Int, y: Int): List<TileCandidate> {
    val sub = ((x + y) and 3)
    val amapHost = (sub % 4) + 1
    val cartoSub = when (sub) {
        0 -> "a"
        1 -> "b"
        2 -> "c"
        else -> "d"
    }
    return when (style) {
        FootprintMapStyle.AMAP_ZH -> listOf(
            // Primary: AMap standard Chinese road & city map (crisp Chinese labels, works globally & in China)
            TileCandidate(
                url = "https://wprd0$amapHost.is.autonavi.com/appmaptile?x=$x&y=$y&z=$z&lang=zh_cn&size=1&scl=1&style=7",
                userAgent = MOBILE_BROWSER_UA,
                referer = "https://www.amap.com/",
                isGcj02 = true
            ),
            // Secondary: AMap web road map (style=8, scale=1 standard 256px PNG)
            TileCandidate(
                url = "https://webrd0$amapHost.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=$x&y=$y&z=$z",
                userAgent = MOBILE_BROWSER_UA,
                referer = "https://www.amap.com/",
                isGcj02 = true
            ),
            // Fallback 1: ArcGIS World Street Map (if Autonavi is blocked on an overseas/restricted network)
            TileCandidate(
                url = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/$z/$y/$x",
                userAgent = MOBILE_BROWSER_UA,
                isGcj02 = false
            ),
            // Fallback 2: CartoDB Voyager CDN
            TileCandidate(
                url = "https://$cartoSub.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png",
                userAgent = MOBILE_BROWSER_UA,
                isGcj02 = false
            )
        )

        FootprintMapStyle.GLOBAL -> listOf(
            TileCandidate(
                url = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/$z/$y/$x",
                userAgent = MOBILE_BROWSER_UA,
                isGcj02 = false
            ),
            TileCandidate(
                url = "https://$cartoSub.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png",
                userAgent = MOBILE_BROWSER_UA,
                isGcj02 = false
            ),
            TileCandidate(
                url = "https://wprd0$amapHost.is.autonavi.com/appmaptile?x=$x&y=$y&z=$z&lang=en&size=1&scl=1&style=7",
                userAgent = MOBILE_BROWSER_UA,
                referer = "https://www.amap.com/",
                isGcj02 = true
            )
        )

        FootprintMapStyle.OSM -> listOf(
            TileCandidate(
                url = "https://tile.openstreetmap.org/$z/$x/$y.png",
                userAgent = OSM_APP_UA,
                isGcj02 = false
            ),
            TileCandidate(
                url = "https://$cartoSub.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png",
                userAgent = MOBILE_BROWSER_UA,
                isGcj02 = false
            ),
            TileCandidate(
                url = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/$z/$y/$x",
                userAgent = MOBILE_BROWSER_UA,
                isGcj02 = false
            )
        )

        FootprintMapStyle.VECTOR -> emptyList()
    }
}

@Composable
fun InteractiveSlippyMap(
    clusters: List<LocationCluster>,
    selectedCluster: LocationCluster?,
    cameraState: SlippyMapCameraState,
    mapStyle: FootprintMapStyle,
    onSelectCluster: (LocationCluster) -> Unit,
    tileRetryTrigger: Int = 0,
    onTileStatusChanged: (loadedCount: Int, failedCount: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val tileImageLoader = remember(context) { FlipYearImageLoaders.getMapTileImageLoader(context) }

    // Per-style adaptive CDN state: if primary host fails repeatedly, newly visible tiles skip straight to working fallback
    var preferredCandidateIndex by remember(mapStyle, tileRetryTrigger) { mutableIntStateOf(0) }
    var activeLayerIsGcj02 by remember(mapStyle, tileRetryTrigger) {
        mutableStateOf(mapStyle == FootprintMapStyle.AMAP_ZH)
    }
    var loadedTileCount by remember(mapStyle, tileRetryTrigger) { mutableIntStateOf(0) }
    var failedTileCount by remember(mapStyle, tileRetryTrigger) { mutableIntStateOf(0) }
    val primaryFailureCounts = remember(mapStyle, tileRetryTrigger) { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(mapStyle, tileRetryTrigger, loadedTileCount, failedTileCount) {
        onTileStatusChanged(loadedTileCount, failedTileCount)
    }

    val useGcj02Alignment = mapStyle == FootprintMapStyle.AMAP_ZH && activeLayerIsGcj02

    // Precompute normalized Web Mercator coordinates once per cluster list / projection change (zero per-frame trig)
    val projectedMeta = remember(clusters, useGcj02Alignment) {
        clusters.map { cluster ->
            val (projLat, projLon) = if (useGcj02Alignment) {
                wgs84ToGcj02(cluster.latitude, cluster.longitude)
            } else {
                cluster.latitude to cluster.longitude
            }
            ProjectedClusterMeta(
                cluster = cluster,
                normX = lonToNormX(projLon),
                normY = latToNormY(projLat)
            )
        }
    }
    val topArcMeta = remember(projectedMeta) {
        if (projectedMeta.size <= MAX_TRAVEL_ARCS) projectedMeta else projectedMeta.take(MAX_TRAVEL_ARCS)
    }

    val isDark = LocalIsDarkTheme.current
    val palette = LocalFlipYearPalette.current
    // 摩登画报 · 山河图配色：地面为画报 ground 渐变，经纬网/陆块/航线全部从主题金派生
    val goldAccent = palette.decoGold
    val reusableContinentPath = remember { Path() }
    val reusableArcPath = remember { Path() }
    val arcDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f) }
    val oceanGradient = remember(palette) {
        Brush.verticalGradient(colors = listOf(palette.decoGround, palette.decoGroundDeep))
    }
    // 陆块：暗房用弱金晕染（山河图墨绿底上的淡金大陆），画廊用 mat 纸面
    val landFillColor = if (isDark) palette.decoGold.copy(alpha = 0.10f) else palette.decoMat
    val landStrokeColor = palette.decoGold.copy(alpha = if (isDark) 0.42f else 0.58f)
    val tileColorFilter = if (isDark) NocturnalMapTileColorFilter else ParchmentMapTileColorFilter

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(oceanGradient)
            .testTag("footprint_interactive_map")
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val viewportHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val baseTilePx = with(density) { BASE_TILE_DP.dp.toPx() }.coerceAtLeast(128f)

        val zoom = cameraState.zoom
        val minPinSeparationPx = with(density) {
            (if (zoom < 4.5f) 48.dp else 42.dp).toPx()
        }.coerceAtLeast(40f)
        val selectedSeparationPx = with(density) { 54.dp.toPx() }.coerceAtLeast(50f)
        val maxVisiblePins = when {
            zoom < 4.5f -> 12
            zoom < 7.5f -> 16
            else -> 22
        }
        val centerX = cameraState.centerX
        val centerY = cameraState.centerY
        val worldPx = baseTilePx * 2.0.pow(zoom.toDouble())

        fun projectToScreen(normX: Double, normY: Double): Offset {
            var dxNorm = normX - centerX
            if (dxNorm > 0.5) dxNorm -= 1.0
            if (dxNorm < -0.5) dxNorm += 1.0
            val sx = (viewportWidthPx / 2.0 + dxNorm * worldPx).toFloat()
            val sy = (viewportHeightPx / 2.0 + (normY - centerY) * worldPx).toFloat()
            return Offset(
                x = if (sx.isFinite()) sx else -10000f,
                y = if (sy.isFinite()) sy else -10000f
            )
        }

        // Gesture Surface: Pinch-to-zoom, pan, and double-tap zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(viewportWidthPx, viewportHeightPx, baseTilePx) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        cameraState.cancelAnimation()
                        if (gestureZoom != 1f) {
                            cameraState.zoomAroundScreenPoint(
                                zoomFactor = gestureZoom,
                                centroidX = centroid.x,
                                centroidY = centroid.y,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                tileSizePx = baseTilePx
                            )
                        }
                        if (pan != Offset.Zero) {
                            cameraState.panByPixels(pan.x, pan.y, baseTilePx)
                        }
                    }
                }
                .pointerInput(viewportWidthPx, viewportHeightPx, baseTilePx) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            cameraState.cancelAnimation()
                            cameraState.zoomAroundScreenPoint(
                                zoomFactor = 2.0f,
                                centroidX = tapOffset.x,
                                centroidY = tapOffset.y,
                                viewportWidthPx = viewportWidthPx,
                                viewportHeightPx = viewportHeightPx,
                                tileSizePx = baseTilePx
                            )
                        }
                    )
                }
        ) {
            // Layer 1: Always-ready Vector World Atlas (zero per-frame trig, reused Path)
            // 摩登画报 · 金线网格底：经纬网全部为主题金细线（呼应 deco-app.html 山河图 0.28 网格）
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = goldAccent.copy(alpha = if (isDark) 0.16f else 0.24f)
                val stepDeg = when {
                    zoom < 3f -> 30
                    zoom < 5f -> 15
                    zoom < 8f -> 5
                    else -> 2
                }
                for (lon in -180..180 step stepDeg) {
                    val nx = (lon + 180.0) / 360.0
                    val pt = projectToScreen(nx, centerY)
                    if (pt.x in -20f..(size.width + 20f)) {
                        drawLine(
                            color = gridColor,
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                for (lat in -60..75 step stepDeg) {
                    val ny = latToNormY(lat.toDouble())
                    val pt = projectToScreen(centerX, ny)
                    if (pt.y in -20f..(size.height + 20f)) {
                        drawLine(
                            color = if (lat == 0) goldAccent.copy(alpha = if (isDark) 0.32f else 0.40f) else gridColor,
                            start = Offset(0f, pt.y),
                            end = Offset(size.width, pt.y),
                            strokeWidth = if (lat == 0) 1.3.dp.toPx() else 1.dp.toPx()
                        )
                    }
                }

                val landFill = landFillColor
                val landStroke = landStrokeColor
                val strokeStyle = Stroke(
                    width = 1.1.dp.toPx(),
                    join = StrokeJoin.Round,
                    cap = StrokeCap.Round
                )

                for (worldShift in -1..1) {
                    for (polygon in WORLD_CONTINENTS_NORM) {
                        reusableContinentPath.rewind()
                        var started = false
                        var minX = Float.POSITIVE_INFINITY
                        var maxX = Float.NEGATIVE_INFINITY
                        var minY = Float.POSITIVE_INFINITY
                        var maxY = Float.NEGATIVE_INFINITY

                        for (ptNorm in polygon) {
                            val rawNormX = ptNorm.first + worldShift
                            val sx = (viewportWidthPx / 2.0 + (rawNormX - centerX) * worldPx).toFloat()
                            val sy = (viewportHeightPx / 2.0 + (ptNorm.second - centerY) * worldPx).toFloat()
                            if (sx.isFinite() && sy.isFinite()) {
                                if (sx < minX) minX = sx
                                if (sx > maxX) maxX = sx
                                if (sy < minY) minY = sy
                                if (sy > maxY) maxY = sy
                                if (!started) {
                                    reusableContinentPath.moveTo(sx, sy)
                                    started = true
                                } else {
                                    reusableContinentPath.lineTo(sx, sy)
                                }
                            }
                        }
                        if (started && maxX >= -20f && minX <= size.width + 20f && maxY >= -20f && minY <= size.height + 20f) {
                            reusableContinentPath.close()
                            drawPath(path = reusableContinentPath, color = landFill)
                            drawPath(path = reusableContinentPath, color = landStroke, style = strokeStyle)
                        }
                    }
                }
            }

            // Layer 2: Live Web Mercator Slippy Map Tiles with Multi-CDN Automatic Fallback
            if (mapStyle != FootprintMapStyle.VECTOR) {
                // AMap tile servers reliably start at z=3; clamping ensures zoom 2.0..2.99 still renders crisp Chinese tiles
                val minTileZoom = if (mapStyle == FootprintMapStyle.AMAP_ZH) 3 else 2
                val tileZoom = floor(zoom).toInt().coerceIn(minTileZoom, 15)
                val tileCount = 1 shl tileZoom
                val scaleFactor = 2.0.pow((zoom - tileZoom).toDouble())
                val renderedTilePx = (baseTilePx * scaleFactor).toFloat().coerceAtLeast(64f)

                val centerTileX = centerX * tileCount
                val centerTileY = centerY * tileCount
                val halfCols = (viewportWidthPx / (2f * renderedTilePx)).toInt() + 1
                val halfRows = (viewportHeightPx / (2f * renderedTilePx)).toInt() + 1

                val minTx = floor(centerTileX).toInt() - halfCols
                val maxTx = floor(centerTileX).toInt() + halfCols
                val minTy = max(0, floor(centerTileY).toInt() - halfRows)
                val maxTy = min(tileCount - 1, floor(centerTileY).toInt() + halfRows)

                if ((maxTx - minTx + 1) * (maxTy - minTy + 1) <= MAX_RENDERED_TILES) {
                    for (ty in minTy..maxTy) {
                        for (tx in minTx..maxTx) {
                            val wrappedX = ((tx % tileCount) + tileCount) % tileCount
                            val tileLeftPx = viewportWidthPx / 2f + ((tx - centerTileX) * renderedTilePx).toFloat()
                            val tileTopPx = viewportHeightPx / 2f + ((ty - centerTileY) * renderedTilePx).toFloat()
                            val candidates = buildTileCandidates(mapStyle, tileZoom, wrappedX, ty)

                            if (candidates.isNotEmpty() && tileLeftPx.isFinite() && tileTopPx.isFinite()) {
                                key(tileZoom, tx, ty, mapStyle, tileRetryTrigger) {
                                    var candidateIdx by remember {
                                        mutableIntStateOf(
                                            preferredCandidateIndex.coerceIn(0, candidates.lastIndex)
                                        )
                                    }
                                    val candidate = candidates[candidateIdx.coerceIn(0, candidates.lastIndex)]
                                    val tileRequest = remember(candidate.url, tileRetryTrigger) {
                                        ImageRequest.Builder(context)
                                            .data(candidate.url)
                                            .size(Size.ORIGINAL)
                                            .allowHardware(false)
                                            .addHeader("User-Agent", candidate.userAgent)
                                            .addHeader("Accept", "image/webp,image/png,image/*;q=0.8")
                                            .apply {
                                                candidate.referer?.let { addHeader("Referer", it) }
                                            }
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .networkCachePolicy(CachePolicy.ENABLED)
                                            .build()
                                    }

                                    AsyncImage(
                                        model = tileRequest,
                                        imageLoader = tileImageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillBounds,
                                        colorFilter = tileColorFilter,
                                        onSuccess = {
                                            loadedTileCount++
                                            if (mapStyle == FootprintMapStyle.AMAP_ZH) {
                                                activeLayerIsGcj02 = candidate.isGcj02
                                            }
                                        },
                                        onError = {
                                            val nextIdx = candidateIdx + 1
                                            if (nextIdx < candidates.size) {
                                                val failures = (primaryFailureCounts[candidateIdx] ?: 0) + 1
                                                primaryFailureCounts[candidateIdx] = failures
                                                // If 3+ tiles fail on this CDN before anything succeeds, auto-promote fallback for new tiles
                                                if (failures >= 3 && loadedTileCount == 0 && preferredCandidateIndex <= candidateIdx) {
                                                    preferredCandidateIndex = nextIdx
                                                }
                                                candidateIdx = nextIdx
                                            } else {
                                                failedTileCount++
                                            }
                                        },
                                        modifier = Modifier.layout { measurable, _ ->
                                            val tileDim = ceil(renderedTilePx + 1.5f).toInt().coerceAtLeast(1)
                                            val placeable = measurable.measure(Constraints.fixed(tileDim, tileDim))
                                            layout(0, 0) {
                                                placeable.place(tileLeftPx.roundToInt(), tileTopPx.roundToInt())
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Layer 3: Subtle Curved Travel Footprint Arcs (restrained count & low alpha to keep roads/labels clear)
            if (topArcMeta.size >= 2) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeStyle = Stroke(
                        width = 1.35.dp.toPx(),
                        pathEffect = arcDashEffect,
                        cap = StrokeCap.Round
                    )
                    var prev = projectToScreen(topArcMeta[0].normX, topArcMeta[0].normY)
                    for (i in 1 until topArcMeta.size) {
                        val curr = projectToScreen(topArcMeta[i].normX, topArcMeta[i].normY)
                        if ((prev.x in -300f..(size.width + 300f) || curr.x in -300f..(size.width + 300f)) &&
                            (prev.y in -300f..(size.height + 300f) || curr.y in -300f..(size.height + 300f))
                        ) {
                            val dx = curr.x - prev.x
                            val dy = curr.y - prev.y
                            val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                            if (dist in 48f..1800f) {
                                val midX = (prev.x + curr.x) * 0.5f
                                val midY = (prev.y + curr.y) * 0.5f
                                val lift = (dist * 0.14f).coerceIn(12f, 64f)
                                reusableArcPath.rewind()
                                reusableArcPath.moveTo(prev.x, prev.y)
                                reusableArcPath.quadraticTo(midX, midY - lift, curr.x, curr.y)
                            drawPath(
                                path = reusableArcPath,
                                color = goldAccent.copy(alpha = if (isDark) 0.40f else 0.28f),
                                style = strokeStyle
                            )
                            }
                        }
                        prev = curr
                    }
                }
            }

            // Layer 4: Overlap-Free Radial Screen-Space Aggregated Photo Thumbnail Pins
            val hitWidthPx = with(density) { PIN_HIT_WIDTH_DP.dp.toPx() }
            val hitHeightPx = with(density) { PIN_HIT_HEIGHT_DP.dp.toPx() }
            val anchorOffsetYFromBottomPx = with(density) { PIN_ANCHOR_BOTTOM_OFFSET_DP.dp.toPx() }
            val selectedName = selectedCluster?.locationName

            val visiblePins = buildVisibleAggregatedPins(
                metas = projectedMeta,
                selectedLocationName = selectedName,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                minSeparationPx = minPinSeparationPx,
                selectedSeparationPx = selectedSeparationPx,
                maxVisiblePins = maxVisiblePins,
                project = ::projectToScreen
            )

            for (pin in visiblePins) {
                key(pin.stableKey) {
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .zIndex(if (pin.isSelected) 10f else 1f)
                            .size(width = PIN_HIT_WIDTH_DP.dp, height = PIN_HIT_HEIGHT_DP.dp)
                            .offset {
                                IntOffset(
                                    x = (pin.screenX - hitWidthPx / 2f).roundToInt(),
                                    y = (pin.screenY - (hitHeightPx - anchorOffsetYFromBottomPx)).roundToInt()
                                )
                            }
                            .clickable {
                                onSelectCluster(pin.primaryCluster)
                                val targetZoom = if (pin.mergedClustersCount > 1) {
                                    (cameraState.zoom + 2.0f).coerceIn(5.8f, 12.0f)
                                } else {
                                    max(cameraState.zoom, 6.4f)
                                }
                                cameraState.animateToLatLng(
                                    scope = scope,
                                    lat = pin.primaryCluster.latitude,
                                    lon = pin.primaryCluster.longitude,
                                    targetZoom = targetZoom
                                )
                            }
                            .testTag("map_pin_${pin.primaryCluster.locationName}")
                    ) {
                        FootprintPinMarker(
                            cluster = pin.primaryCluster,
                            totalPhotosCount = pin.totalPhotosCount,
                            totalUnreviewedCount = pin.totalUnreviewedCount,
                            mergedClustersCount = pin.mergedClustersCount,
                            isSelected = pin.isSelected
                        )
                    }
                }
            }
        }
    }
}

private const val PIN_HIT_WIDTH_DP = 52f
private const val PIN_HIT_HEIGHT_DP = 60f
private const val PIN_ANCHOR_BOTTOM_OFFSET_DP = 4f

private class RadialPinClusterAcc(
    val primary: LocationCluster,
    val anchorX: Float,
    val anchorY: Float,
    var mergedCount: Int,
    var totalPhotos: Int,
    var totalUnreviewed: Int,
    var containsSelected: Boolean
)

private data class VisibleProjectedCandidate(
    val cluster: LocationCluster,
    val screenX: Float,
    val screenY: Float,
    val isSelected: Boolean
)

/**
 * Overlap-free greedy Euclidean screen-space clustering:
 * Guarantees every rendered pin maintains at least [minSeparationPx] (or [selectedSeparationPx]
 * around the active pin) so adjacent locations across grid boundaries never collide or stack messily.
 * Anchors remain locked to the primary cluster's true coordinate rather than drifting into open water.
 */
internal fun buildVisibleAggregatedPins(
    metas: List<ProjectedClusterMeta>,
    selectedLocationName: String?,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    minSeparationPx: Float,
    selectedSeparationPx: Float = minSeparationPx * 1.2f,
    maxVisiblePins: Int = 16,
    project: (Double, Double) -> Offset
): List<DisplayedMapPin> {
    if (metas.isEmpty()) return emptyList()

    val margin = 72f
    val candidates = ArrayList<VisibleProjectedCandidate>(metas.size)
    for (meta in metas) {
        val pos = project(meta.normX, meta.normY)
        if (pos.x < -margin || pos.x > viewportWidthPx + margin || pos.y < -margin || pos.y > viewportHeightPx + margin) {
            continue
        }
        val cluster = meta.cluster
        candidates.add(
            VisibleProjectedCandidate(
                cluster = cluster,
                screenX = pos.x,
                screenY = pos.y,
                isSelected = cluster.locationName == selectedLocationName
            )
        )
    }
    if (candidates.isEmpty()) return emptyList()

    // Deterministic priority: active selection first, then largest photo footprint, then name
    candidates.sortWith(
        compareByDescending<VisibleProjectedCandidate> { it.isSelected }
            .thenByDescending { it.cluster.photos.size }
            .thenBy { it.cluster.locationName }
    )

    val minSepSq = minSeparationPx * minSeparationPx
    val selSepSq = selectedSeparationPx * selectedSeparationPx
    val accepted = ArrayList<RadialPinClusterAcc>(min(candidates.size, maxVisiblePins))

    for (cand in candidates) {
        var nearestAcc: RadialPinClusterAcc? = null
        var nearestDistSq = Float.POSITIVE_INFINITY

        for (acc in accepted) {
            val dx = cand.screenX - acc.anchorX
            val dy = cand.screenY - acc.anchorY
            val distSq = dx * dx + dy * dy
            val thresholdSq = if (acc.containsSelected || cand.isSelected) selSepSq else minSepSq
            if (distSq < thresholdSq && distSq < nearestDistSq) {
                nearestDistSq = distSq
                nearestAcc = acc
            }
        }

        if (nearestAcc != null) {
            nearestAcc.mergedCount++
            nearestAcc.totalPhotos += cand.cluster.photos.size
            nearestAcc.totalUnreviewed += cand.cluster.unreviewedCount
            if (cand.isSelected) nearestAcc.containsSelected = true
        } else if (accepted.size < maxVisiblePins) {
            accepted.add(
                RadialPinClusterAcc(
                    primary = cand.cluster,
                    anchorX = cand.screenX,
                    anchorY = cand.screenY,
                    mergedCount = 1,
                    totalPhotos = cand.cluster.photos.size,
                    totalUnreviewed = cand.cluster.unreviewedCount,
                    containsSelected = cand.isSelected
                )
            )
        } else {
            // Once visual cap is reached, fold remaining distant minor points into the closest visible stack
            // only if reasonably nearby (within 2.2x separation) so counts stay honest without distorting distant cities
            val softFoldSq = minSepSq * 4.84f
            var foldTarget: RadialPinClusterAcc? = null
            var foldDistSq = softFoldSq
            for (acc in accepted) {
                val dx = cand.screenX - acc.anchorX
                val dy = cand.screenY - acc.anchorY
                val distSq = dx * dx + dy * dy
                if (distSq < foldDistSq) {
                    foldDistSq = distSq
                    foldTarget = acc
                }
            }
            if (foldTarget != null) {
                foldTarget.mergedCount++
                foldTarget.totalPhotos += cand.cluster.photos.size
                foldTarget.totalUnreviewed += cand.cluster.unreviewedCount
            }
        }
    }

    return accepted.map { acc ->
        DisplayedMapPin(
            stableKey = acc.primary.locationName,
            primaryCluster = acc.primary,
            mergedClustersCount = acc.mergedCount,
            totalPhotosCount = acc.totalPhotos,
            totalUnreviewedCount = acc.totalUnreviewed,
            screenX = acc.anchorX,
            screenY = acc.anchorY,
            isSelected = acc.containsSelected
        )
    }
}

private fun formatCompactPinCount(count: Int): String {
    return when {
        count >= 10_000 -> String.format("%.1fw", count / 10_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}

@Composable
private fun FootprintPinMarker(
    cluster: LocationCluster,
    totalPhotosCount: Int,
    totalUnreviewedCount: Int,
    mergedClustersCount: Int,
    isSelected: Boolean
) {
    val palette = LocalFlipYearPalette.current
    val gold = palette.decoGold
    val mat = palette.decoMat
    val inkOnGround = palette.decoInkOnGround
    val verm = palette.decoVerm
    // 纸黄永远只压朱红底 / 照片暗部（可读性红线）
    val cream = DecoPaperCream

    val thumbSize by animateDpAsState(
        targetValue = if (isSelected) 42.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "map_pin_thumb_size"
    )
    // 画片圆角 2~4dp（不用 22dp 大圆角）
    val cornerRadius = 3.dp
    val innerCornerRadius = 2.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 1.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(thumbSize + 8.dp)
                .then(
                    if (isSelected) {
                        // 选中印记：金 0.30 晕圈（deco-app.html 0 0 0 4px goldDim(0.3)）
                        Modifier.border(
                            width = 3.5.dp,
                            color = gold.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(cornerRadius + 2.dp)
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            // Subtle rotated back-card cue when multiple nearby footprints are stacked here
            if (mergedClustersCount > 1) {
                Box(
                    modifier = Modifier
                        .size(thumbSize - 2.dp)
                        .offset(x = 3.dp, y = (-2).dp)
                        .rotate(7f)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(mat.copy(alpha = 0.88f))
                        .border(
                            width = 1.dp,
                            color = gold.copy(alpha = if (isSelected) 0.60f else 0.38f),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                )
            }

            // 画片式照片印记：金双线感外框（金 1.5dp 描边 + mat 内衬），选中换主题墨色描边
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .shadow(
                        elevation = if (isSelected) 9.dp else 3.5.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        ambientColor = Color.Black.copy(alpha = 0.30f),
                        spotColor = Color.Black.copy(alpha = 0.42f)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(mat)
                    .border(
                        width = if (isSelected) 2.dp else 1.5.dp,
                        color = if (isSelected) inkOnGround else gold,
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .padding(2.dp)
            ) {
                PhotoArtwork(
                    photo = cluster.coverPhoto,
                    contentDescription = cluster.locationName,
                    showVideoBadge = false,
                    downsampleTier = ImageDownsampleTier.MICRO_THUMB,
                    targetSizePx = 112,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(innerCornerRadius))
                )
            }

            // 朱红计数徽：压在印记右上角，纸黄描边 + 纸黄数字（原型 11px/17px 比例）
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(verm)
                    .border(1.dp, cream.copy(alpha = 0.60f), CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = formatCompactPinCount(totalPhotosCount),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = cream
                )
            }
        }

        // Delicate Pin Anchor Dot precisely aligned over lat/lon coordinate
        Box(
            modifier = Modifier
                .size(if (isSelected) 7.dp else 5.5.dp)
                .clip(CircleShape)
                .background(if (isSelected) gold else inkOnGround)
                .border(1.dp, cream.copy(alpha = 0.80f), CircleShape)
        )
    }
}

// Simplified accurate world landmass polygons (latitude, longitude) for instant offline vector map rendering
private val WORLD_CONTINENTS: List<List<Pair<Double, Double>>> = listOf(
    // North America
    listOf(
        70.0 to -162.0, 71.0 to -135.0, 68.0 to -95.0, 60.0 to -64.0, 47.0 to -53.0,
        44.0 to -66.0, 39.0 to -74.0, 25.0 to -80.0, 29.0 to -85.0, 29.0 to -95.0,
        21.0 to -97.0, 21.0 to -87.0, 15.0 to -84.0, 9.0 to -78.0, 8.0 to -82.0,
        15.0 to -93.0, 23.0 to -106.0, 32.0 to -117.0, 41.0 to -124.0, 49.0 to -125.0,
        59.0 to -138.0, 60.0 to -152.0, 55.0 to -162.0, 63.0 to -165.0
    ),
    // Greenland
    listOf(
        82.0 to -35.0, 76.0 to -18.0, 66.0 to -35.0, 60.0 to -44.0,
        66.0 to -54.0, 76.0 to -68.0, 81.0 to -54.0
    ),
    // South America
    listOf(
        11.0 to -75.0, 10.0 to -62.0, 2.0 to -50.0, -5.0 to -35.0, -15.0 to -39.0,
        -23.0 to -42.0, -34.0 to -53.0, -42.0 to -64.0, -54.0 to -68.0, -52.0 to -74.0,
        -38.0 to -73.0, -18.0 to -71.0, -5.0 to -81.0, 5.0 to -77.0
    ),
    // Europe + Asia (Eurasia)
    listOf(
        36.0 to -9.0, 43.0 to -9.0, 48.0 to -4.0, 51.0 to 2.0, 54.0 to 9.0,
        58.0 to 6.0, 64.0 to 10.0, 71.0 to 26.0, 69.0 to 42.0, 71.0 to 68.0,
        74.0 to 105.0, 71.0 to 140.0, 66.0 to 176.0, 60.0 to 164.0, 51.0 to 156.0,
        54.0 to 140.0, 43.0 to 135.0, 35.0 to 129.0, 38.0 to 125.0, 40.0 to 121.0,
        35.0 to 120.0, 31.0 to 122.0, 23.0 to 117.0, 21.0 to 108.0, 12.0 to 109.0,
        10.0 to 105.0, 13.0 to 100.0, 2.0 to 103.0, 7.0 to 98.0, 16.0 to 96.0,
        22.0 to 89.0, 15.0 to 80.0, 8.0 to 77.0, 20.0 to 72.0, 25.0 to 66.0,
        25.0 to 57.0, 30.0 to 48.0, 22.0 to 59.0, 13.0 to 44.0, 29.0 to 35.0,
        31.0 to 34.0, 36.0 to 36.0, 36.0 to 27.0, 41.0 to 29.0, 38.0 to 23.0,
        45.0 to 14.0, 40.0 to 18.0, 38.0 to 15.0, 44.0 to 9.0, 43.0 to 3.0,
        36.0 to -5.0
    ),
    // Great Britain
    listOf(
        58.6 to -5.0, 57.7 to -1.8, 52.8 to 1.7, 50.8 to 0.3, 50.1 to -5.5, 54.5 to -3.5
    ),
    // Japan
    listOf(
        45.2 to 141.8, 43.2 to 145.4, 41.4 to 141.4, 38.3 to 141.5, 35.5 to 140.8,
        33.4 to 135.8, 31.2 to 130.6, 33.9 to 130.9, 36.8 to 137.0, 40.8 to 140.2
    ),
    // Taiwan
    listOf(
        25.3 to 121.6, 23.8 to 121.6, 21.9 to 120.8, 23.4 to 120.1, 25.1 to 121.2
    ),
    // Africa
    listOf(
        35.8 to -5.5, 37.0 to 10.5, 32.0 to 15.0, 31.5 to 25.0, 31.2 to 32.5,
        22.0 to 37.0, 11.8 to 43.5, 11.8 to 51.2, 2.0 to 45.0, -5.0 to 39.5,
        -15.0 to 40.5, -26.0 to 33.0, -34.5 to 20.0, -30.0 to 17.0, -17.0 to 11.8,
        -5.0 to 12.0, 4.0 to 9.5, 5.0 to -3.0, 5.0 to -10.5, 15.0 to -17.5,
        28.0 to -13.0
    ),
    // Madagascar
    listOf(
        -12.3 to 49.3, -15.5 to 50.3, -25.2 to 46.8, -24.5 to 43.7, -16.0 to 44.5
    ),
    // Australia
    listOf(
        -12.5 to 131.0, -11.0 to 142.5, -20.0 to 148.5, -28.0 to 153.5, -37.5 to 150.0,
        -38.5 to 141.0, -32.0 to 132.0, -34.5 to 115.0, -22.0 to 114.0, -16.0 to 123.0
    ),
    // New Zealand
    listOf(
        -34.5 to 173.0, -37.8 to 178.5, -41.5 to 175.3, -46.5 to 167.5, -42.5 to 171.2, -37.0 to 174.5
    ),
    // Borneo & Indonesia
    listOf(
        6.8 to 117.2, 1.0 to 118.8, -3.5 to 116.0, -2.5 to 110.2, 2.0 to 109.6
    ),
    listOf(
        5.5 to 95.3, 1.0 to 103.0, -5.8 to 105.8, -4.0 to 102.0, 1.5 to 98.0
    )
)

// Precomputed normalized Web Mercator vertices (zero trig overhead during map pan/zoom)
private val WORLD_CONTINENTS_NORM: List<List<Pair<Double, Double>>> = WORLD_CONTINENTS.map { poly ->
    poly.map { (lat, lon) -> lonToNormX(lon) to latToNormY(lat) }
}
