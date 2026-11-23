package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.CollageBackdropStyle
import com.example.data.CollageSlotTransform
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.theme.DecoGold
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class CollageStyleCategory(val label: String) {
    ALL("全部"),
    EDITORIAL_DIAGONAL("杂志斜切"),
    POLAROID_STACK("拍立得"),
    GALLERY_INSET("艺术嵌窗")
}

enum class CollageSlotFrameStyle {
    EDGE_TO_EDGE,
    POLAROID,
    FLOATING_INSET
}

@Immutable
data class NormalizedPoint(val x: Float, val y: Float)

@Immutable
data class CollageSlotSpec(
    val polygon: List<NormalizedPoint>,
    val rotationDeg: Float = 0f,
    val frameStyle: CollageSlotFrameStyle = CollageSlotFrameStyle.EDGE_TO_EDGE,
    val cornerRadiusDp: Float = 4f,
    val zIndex: Float = 0f
)

@Immutable
data class CollageTemplate(
    val id: String,
    val title: String,
    val category: CollageStyleCategory,
    val slots: List<CollageSlotSpec>,
    val defaultSpacingDp: Float = 4f
) {
    val slotCount: Int get() = slots.size
}

private fun rectPoly(l: Float, t: Float, r: Float, b: Float): List<NormalizedPoint> = listOf(
    NormalizedPoint(l, t),
    NormalizedPoint(r, t),
    NormalizedPoint(r, b),
    NormalizedPoint(l, b)
)

object YearbookCollageTemplates {

    val all: List<CollageTemplate> = listOf(
        // 1. 杂志斜切 · 斜切双幕 (2张)
        CollageTemplate(
            id = "diag_split_2",
            title = "斜切双幕",
            category = CollageStyleCategory.EDITORIAL_DIAGONAL,
            defaultSpacingDp = 4f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0f, 0f),
                        NormalizedPoint(1f, 0f),
                        NormalizedPoint(1f, 0.36f),
                        NormalizedPoint(0f, 0.64f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0f, 0.64f),
                        NormalizedPoint(1f, 0.36f),
                        NormalizedPoint(1f, 1f),
                        NormalizedPoint(0f, 1f)
                    ),
                    cornerRadiusDp = 3f
                )
            )
        ),
        // 2. 杂志斜切 · 阶梯主副 (3张)
        CollageTemplate(
            id = "diag_step_3",
            title = "阶梯主副",
            category = CollageStyleCategory.EDITORIAL_DIAGONAL,
            defaultSpacingDp = 4f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0f, 0f),
                        NormalizedPoint(0.62f, 0f),
                        NormalizedPoint(0.46f, 1f),
                        NormalizedPoint(0f, 1f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0.62f, 0f),
                        NormalizedPoint(1f, 0f),
                        NormalizedPoint(1f, 0.45f),
                        NormalizedPoint(0.548f, 0.45f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0.548f, 0.45f),
                        NormalizedPoint(1f, 0.45f),
                        NormalizedPoint(1f, 1f),
                        NormalizedPoint(0.46f, 1f)
                    ),
                    cornerRadiusDp = 3f
                )
            )
        ),
        // 3. 杂志斜切 · 斜轴四幕 (4张)
        CollageTemplate(
            id = "diag_quad_4",
            title = "斜轴四幕",
            category = CollageStyleCategory.EDITORIAL_DIAGONAL,
            defaultSpacingDp = 4f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0f, 0f),
                        NormalizedPoint(0.58f, 0f),
                        NormalizedPoint(0.47f, 0.46f),
                        NormalizedPoint(0f, 0.56f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0.58f, 0f),
                        NormalizedPoint(1f, 0f),
                        NormalizedPoint(1f, 0.36f),
                        NormalizedPoint(0.47f, 0.46f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0f, 0.56f),
                        NormalizedPoint(0.47f, 0.46f),
                        NormalizedPoint(0.38f, 1f),
                        NormalizedPoint(0f, 1f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0.47f, 0.46f),
                        NormalizedPoint(1f, 0.36f),
                        NormalizedPoint(1f, 1f),
                        NormalizedPoint(0.38f, 1f)
                    ),
                    cornerRadiusDp = 3f
                )
            )
        ),
        // 4. 杂志斜切 · 五格剧场 (5张)
        CollageTemplate(
            id = "diag_theater_5",
            title = "五格剧场",
            category = CollageStyleCategory.EDITORIAL_DIAGONAL,
            defaultSpacingDp = 4f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0f, 0f),
                        NormalizedPoint(1f, 0f),
                        NormalizedPoint(1f, 0.34f),
                        NormalizedPoint(0f, 0.42f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0f, 0.42f),
                        NormalizedPoint(0.56f, 0.375f),
                        NormalizedPoint(0.44f, 0.72f),
                        NormalizedPoint(0f, 0.72f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = listOf(
                        NormalizedPoint(0.56f, 0.375f),
                        NormalizedPoint(1f, 0.34f),
                        NormalizedPoint(1f, 0.72f),
                        NormalizedPoint(0.44f, 0.72f)
                    ),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0f, 0.72f, 0.48f, 1f),
                    cornerRadiusDp = 3f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.48f, 0.72f, 1f, 1f),
                    cornerRadiusDp = 3f
                )
            )
        ),
        // 5. 拍立得手账 · 双联相纸 (2张)
        CollageTemplate(
            id = "polaroid_duo_2",
            title = "双联相纸",
            category = CollageStyleCategory.POLAROID_STACK,
            defaultSpacingDp = 6f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = rectPoly(0.08f, 0.09f, 0.66f, 0.56f),
                    rotationDeg = -5.5f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 1f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.32f, 0.42f, 0.91f, 0.87f),
                    rotationDeg = 4.5f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 2f
                )
            )
        ),
        // 6. 拍立得手账 · 三联手账 (3张)
        CollageTemplate(
            id = "polaroid_trio_3",
            title = "三联手账",
            category = CollageStyleCategory.POLAROID_STACK,
            defaultSpacingDp = 6f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = rectPoly(0.06f, 0.08f, 0.55f, 0.48f),
                    rotationDeg = -6f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 1f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.46f, 0.14f, 0.94f, 0.53f),
                    rotationDeg = 5f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 2f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.20f, 0.47f, 0.78f, 0.87f),
                    rotationDeg = -2.5f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 3f
                )
            )
        ),
        // 7. 拍立得手账 · 四联漫游 (4张)
        CollageTemplate(
            id = "polaroid_quad_4",
            title = "四联漫游",
            category = CollageStyleCategory.POLAROID_STACK,
            defaultSpacingDp = 6f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = rectPoly(0.06f, 0.08f, 0.52f, 0.44f),
                    rotationDeg = -5f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 1f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.49f, 0.11f, 0.94f, 0.47f),
                    rotationDeg = 4.5f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 2f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.08f, 0.46f, 0.53f, 0.82f),
                    rotationDeg = 3.5f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 3f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.47f, 0.49f, 0.93f, 0.85f),
                    rotationDeg = -4f,
                    frameStyle = CollageSlotFrameStyle.POLAROID,
                    cornerRadiusDp = 4f,
                    zIndex = 4f
                )
            )
        ),
        // 8. 艺术嵌窗 · 画中画浮窗 (2张)
        CollageTemplate(
            id = "inset_pip_2",
            title = "画中画浮窗",
            category = CollageStyleCategory.GALLERY_INSET,
            defaultSpacingDp = 0f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = rectPoly(0f, 0f, 1f, 1f),
                    frameStyle = CollageSlotFrameStyle.EDGE_TO_EDGE,
                    cornerRadiusDp = 0f,
                    zIndex = 0f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.46f, 0.14f, 0.91f, 0.54f),
                    rotationDeg = 1.8f,
                    frameStyle = CollageSlotFrameStyle.FLOATING_INSET,
                    cornerRadiusDp = 14f,
                    zIndex = 2f
                )
            )
        ),
        // 9. 艺术嵌窗 · 错落六宫 (6张)
        CollageTemplate(
            id = "bento_mosaic_6",
            title = "错落六宫",
            category = CollageStyleCategory.GALLERY_INSET,
            defaultSpacingDp = 5f,
            slots = listOf(
                CollageSlotSpec(
                    polygon = rectPoly(0f, 0f, 0.64f, 0.54f),
                    cornerRadiusDp = 8f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.64f, 0f, 1f, 0.27f),
                    cornerRadiusDp = 8f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.64f, 0.27f, 1f, 0.54f),
                    cornerRadiusDp = 8f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0f, 0.54f, 0.34f, 1f),
                    cornerRadiusDp = 8f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.34f, 0.54f, 0.68f, 1f),
                    cornerRadiusDp = 8f
                ),
                CollageSlotSpec(
                    polygon = rectPoly(0.68f, 0.54f, 1f, 1f),
                    cornerRadiusDp = 8f
                )
            )
        )
    )

    private val byId = all.associateBy { it.id }

    fun findById(id: String?): CollageTemplate? = id?.let { byId[it] }

    /**
     * Picks an aesthetically varied irregular template deterministically for a given year & photo count,
     * so adjacent years on the homepage shelf display distinct editorial compositions.
     */
    fun defaultTemplateFor(year: Int, availablePhotoCount: Int): CollageTemplate {
        val count = availablePhotoCount.coerceAtLeast(1)
        val candidates = when {
            count >= 5 -> listOf("diag_quad_4", "polaroid_trio_3", "diag_theater_5", "diag_step_3", "polaroid_quad_4", "bento_mosaic_6")
            count == 4 -> listOf("diag_quad_4", "polaroid_quad_4", "polaroid_trio_3", "diag_step_3")
            count == 3 -> listOf("diag_step_3", "polaroid_trio_3")
            else -> listOf("diag_split_2", "polaroid_duo_2", "inset_pip_2")
        }.mapNotNull { byId[it] }
        val pool = candidates.ifEmpty { all }
        val idx = (year.mod(pool.size) + pool.size) % pool.size
        return pool[idx]
    }

    /**
     * Resolves a saved or default template ID while guaranteeing that it fits available photos cleanly.
     */
    fun resolveTemplate(templateId: String?, year: Int, availablePhotoCount: Int): CollageTemplate {
        val explicit = findById(templateId)
        if (explicit != null && availablePhotoCount >= 2) {
            return explicit
        }
        return defaultTemplateFor(year, availablePhotoCount)
    }
}

/**
 * Exact 2D convex polygon edge-normal inward offset.
 * Produces crisp, uniform parallel gutters along both straight and diagonal seams.
 */
fun insetConvexPolygon(points: List<Offset>, insetPx: Float): List<Offset> {
    if (points.size < 3 || insetPx <= 0.1f) return points

    // Compute signed area to detect winding order (Compose Y-down: positive signed area = clockwise)
    var signedArea2 = 0f
    for (i in points.indices) {
        val a = points[i]
        val b = points[(i + 1) % points.size]
        signedArea2 += (a.x * b.y - b.x * a.y)
    }
    val isClockwise = signedArea2 >= 0f

    val n = points.size
    val shiftedEdges = ArrayList<Pair<Offset, Offset>>(n)
    for (i in 0 until n) {
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val len = hypot(dx, dy).coerceAtLeast(1e-4f)
        // Inward normal for Y-down screen coordinates
        val nx = if (isClockwise) -dy / len else dy / len
        val ny = if (isClockwise) dx / len else -dx / len
        shiftedEdges.add(
            Offset(p1.x + nx * insetPx, p1.y + ny * insetPx) to
                Offset(p2.x + nx * insetPx, p2.y + ny * insetPx)
        )
    }

    val result = ArrayList<Offset>(n)
    for (i in 0 until n) {
        val (a1, a2) = shiftedEdges[(i - 1 + n) % n]
        val (b1, b2) = shiftedEdges[i]
        val intersection = intersectLines(a1, a2, b1, b2) ?: b1
        result.add(intersection)
    }

    // Safety fallback if inset collapsed the polygon
    var newArea2 = 0f
    for (i in result.indices) {
        val a = result[i]
        val b = result[(i + 1) % result.size]
        newArea2 += (a.x * b.y - b.x * a.y)
    }
    if ((isClockwise && newArea2 <= 1f) || (!isClockwise && newArea2 >= -1f)) {
        return points
    }
    return result
}

private fun intersectLines(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Offset? {
    val d1x = p2.x - p1.x
    val d1y = p2.y - p1.y
    val d2x = p4.x - p3.x
    val d2y = p4.y - p3.y
    val denom = d1x * d2y - d1y * d2x
    if (abs(denom) < 1e-4f) return null
    val t = ((p3.x - p1.x) * d2y - (p3.y - p1.y) * d2x) / denom
    return Offset(p1.x + t * d1x, p1.y + t * d1y)
}

fun buildRoundedPolygonPath(points: List<Offset>, cornerRadiusPx: Float): Path {
    val path = Path()
    if (points.size < 3) return path
    if (cornerRadiusPx <= 0.5f) {
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        path.close()
        return path
    }

    val n = points.size
    for (i in 0 until n) {
        val prev = points[(i - 1 + n) % n]
        val curr = points[i]
        val next = points[(i + 1) % n]

        val v1x = prev.x - curr.x
        val v1y = prev.y - curr.y
        val v2x = next.x - curr.x
        val v2y = next.y - curr.y
        val len1 = hypot(v1x, v1y).coerceAtLeast(1e-4f)
        val len2 = hypot(v2x, v2y).coerceAtLeast(1e-4f)
        val r = minOf(cornerRadiusPx, len1 * 0.35f, len2 * 0.35f)

        val start = Offset(curr.x + v1x / len1 * r, curr.y + v1y / len1 * r)
        val end = Offset(curr.x + v2x / len2 * r, curr.y + v2y / len2 * r)

        if (i == 0) {
            path.moveTo(start.x, start.y)
        } else {
            path.lineTo(start.x, start.y)
        }
        path.quadraticTo(curr.x, curr.y, end.x, end.y)
    }
    path.close()
    return path
}

/**
 * Ray-casting point-in-polygon check supporting optional rotation around polygon bounding center.
 */
fun isPointInSlot(
    point: Offset,
    slot: CollageSlotSpec,
    canvasWidth: Float,
    canvasHeight: Float
): Boolean {
    val pts = slot.polygon.map { Offset(it.x * canvasWidth, it.y * canvasHeight) }
    if (pts.size < 3) return false

    val minX = pts.minOf { it.x }
    val maxX = pts.maxOf { it.x }
    val minY = pts.minOf { it.y }
    val maxY = pts.maxOf { it.y }
    val cx = (minX + maxX) / 2f
    val cy = (minY + maxY) / 2f

    // Inverse-rotate test point into the slot's local unrotated space
    val testPt = if (abs(slot.rotationDeg) > 0.01f) {
        val rad = (-slot.rotationDeg * PI / 180.0).toFloat()
        val dx = point.x - cx
        val dy = point.y - cy
        Offset(
            x = cx + dx * cos(rad) - dy * sin(rad),
            y = cy + dx * sin(rad) + dy * cos(rad)
        )
    } else {
        point
    }

    var inside = false
    var j = pts.lastIndex
    for (i in pts.indices) {
        val pi = pts[i]
        val pj = pts[j]
        val intersects = ((pi.y > testPt.y) != (pj.y > testPt.y)) &&
            (testPt.x < (pj.x - pi.x) * (testPt.y - pi.y) / ((pj.y - pi.y).takeIf { abs(it) > 1e-5f } ?: 1e-5f) + pi.x)
        if (intersects) inside = !inside
        j = i
    }
    return inside
}

fun resolveBackdropColor(backdrop: CollageBackdropStyle, editionColor: Color): Color = when (backdrop) {
    CollageBackdropStyle.EDITION -> editionColor
    CollageBackdropStyle.WARM_PAPER -> Color(0xFFF4F1EA)
    CollageBackdropStyle.OBSIDIAN -> Color(0xFF10141C)
}

private class LocalPolygonShape(
    private val localPoints: List<Offset>,
    private val cornerRadiusPx: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(buildRoundedPolygonPath(localPoints, cornerRadiusPx))
    }
}

/**
 * Unified GPU-accelerated Irregular Collage Renderer.
 * Drives both the homepage 3D YearbookMonographCard and the interactive YearbookCoverStudioSheet.
 */
@Composable
fun YearbookCollageCanvas(
    photos: List<PhotoItemEntity>,
    template: CollageTemplate,
    spacingDp: Float,
    backdrop: CollageBackdropStyle,
    editionColor: Color,
    slotTransforms: List<CollageSlotTransform>,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    activeSlotIndex: Int? = null,
    downsampleTier: ImageDownsampleTier = ImageDownsampleTier.HERO_CARD,
    targetSizePx: Int = 640,
    onSlotTap: ((Int) -> Unit)? = null,
    onTransformSlot: ((slotIndex: Int, updated: CollageSlotTransform) -> Unit)? = null
) {
    val bgColor = resolveBackdropColor(backdrop, editionColor)
    val density = LocalDensity.current

    val currentActiveSlot by rememberUpdatedState(activeSlotIndex)
    val currentTransforms by rememberUpdatedState(slotTransforms)
    val currentTemplate by rememberUpdatedState(template)
    val currentOnSlotTap by rememberUpdatedState(onSlotTap)
    val currentOnTransformSlot by rememberUpdatedState(onTransformSlot)

    BoxWithConstraints(
        modifier = modifier.background(bgColor)
    ) {
        val canvasW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val canvasH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val outerMarginPx = with(density) { (spacingDp * 0.55f).dp.toPx() }
        val innerHalfGutterPx = with(density) { (spacingDp * 0.5f).dp.toPx() }

        // Render slots in zIndex ascending order
        val orderedIndices = remember(template.id) {
            template.slots.indices.sortedBy { template.slots[it].zIndex }
        }

        orderedIndices.forEach { slotIndex ->
            val slot = template.slots[slotIndex]
            val photo = photos.getOrNull(slotIndex) ?: photos.firstOrNull()
            val transform = slotTransforms.getOrNull(slotIndex) ?: CollageSlotTransform()
            val isSelected = interactive && activeSlotIndex == slotIndex

            val rawPts = remember(slot, canvasW, canvasH, outerMarginPx, innerHalfGutterPx) {
                val basePts = slot.polygon.map { p ->
                    val x = if (slot.frameStyle == CollageSlotFrameStyle.EDGE_TO_EDGE) {
                        outerMarginPx + p.x * (canvasW - outerMarginPx * 2f).coerceAtLeast(1f)
                    } else {
                        p.x * canvasW
                    }
                    val y = if (slot.frameStyle == CollageSlotFrameStyle.EDGE_TO_EDGE) {
                        outerMarginPx + p.y * (canvasH - outerMarginPx * 2f).coerceAtLeast(1f)
                    } else {
                        p.y * canvasH
                    }
                    Offset(x, y)
                }
                if (slot.frameStyle == CollageSlotFrameStyle.EDGE_TO_EDGE && innerHalfGutterPx > 0.25f) {
                    insetConvexPolygon(basePts, innerHalfGutterPx)
                } else {
                    basePts
                }
            }

            val minX = remember(rawPts) { rawPts.minOf { it.x } }
            val minY = remember(rawPts) { rawPts.minOf { it.y } }
            val maxX = remember(rawPts) { rawPts.maxOf { it.x } }
            val maxY = remember(rawPts) { rawPts.maxOf { it.y } }
            val boxW = (maxX - minX).coerceAtLeast(1f)
            val boxH = (maxY - minY).coerceAtLeast(1f)

            val localPts = remember(rawPts, minX, minY) {
                rawPts.map { Offset(it.x - minX, it.y - minY) }
            }
            val cornerPx = with(density) { slot.cornerRadiusDp.dp.toPx() }
            val polygonShape = remember(localPts, cornerPx) {
                LocalPolygonShape(localPts, cornerPx)
            }

            val selectBounce by animateFloatAsState(
                targetValue = if (isSelected) 1.025f else 1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                label = "collage_slot_select_$slotIndex"
            )

            Box(
                modifier = Modifier
                    .zIndex(slot.zIndex + if (isSelected) 10f else 0f)
                    .offset { IntOffset(minX.roundToInt(), minY.roundToInt()) }
                    .size(
                        width = with(density) { boxW.toDp() },
                        height = with(density) { boxH.toDp() }
                    )
                    .graphicsLayer {
                        rotationZ = slot.rotationDeg
                        scaleX = selectBounce
                        scaleY = selectBounce
                    }
            ) {
                when (slot.frameStyle) {
                    CollageSlotFrameStyle.POLAROID -> {
                        val paperShape = RoundedCornerShape(slot.cornerRadiusDp.dp)
                        val sidePad = (boxW * 0.065f).coerceIn(with(density) { 4.dp.toPx() }, with(density) { 10.dp.toPx() })
                        val bottomPad = (boxH * 0.16f).coerceIn(with(density) { 12.dp.toPx() }, with(density) { 26.dp.toPx() })
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(
                                    elevation = 9.dp,
                                    shape = paperShape,
                                    ambientColor = Color.Black.copy(alpha = 0.38f),
                                    spotColor = Color.Black.copy(alpha = 0.45f)
                                )
                                .clip(paperShape)
                                .background(Color(0xFFFAF8F3))
                                .border(0.75.dp, Color.Black.copy(alpha = 0.08f), paperShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(sidePad.roundToInt(), sidePad.roundToInt()) }
                                    .size(
                                        width = with(density) { (boxW - sidePad * 2f).coerceAtLeast(1f).toDp() },
                                        height = with(density) { (boxH - sidePad - bottomPad).coerceAtLeast(1f).toDp() }
                                    )
                                    .clip(RoundedCornerShape(2.dp))
                            ) {
                                if (photo != null) {
                                    PhotoArtwork(
                                        photo = photo,
                                        contentDescription = photo.caption,
                                        showVideoBadge = false,
                                        downsampleTier = downsampleTier,
                                        targetSizePx = targetSizePx,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                val s = transform.scale.coerceIn(1f, 3f)
                                                scaleX = s
                                                scaleY = s
                                                translationX = transform.offsetX * size.width
                                                translationY = transform.offsetY * size.height
                                            }
                                    )
                                }
                            }
                        }
                    }

                    CollageSlotFrameStyle.FLOATING_INSET -> {
                        val insetShape = RoundedCornerShape(slot.cornerRadiusDp.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(
                                    elevation = 12.dp,
                                    shape = insetShape,
                                    ambientColor = Color.Black.copy(alpha = 0.44f),
                                    spotColor = Color.Black.copy(alpha = 0.52f)
                                )
                                .clip(insetShape)
                                .border(2.2.dp, Color.White.copy(alpha = 0.92f), insetShape)
                        ) {
                            if (photo != null) {
                                PhotoArtwork(
                                    photo = photo,
                                    contentDescription = photo.caption,
                                    showVideoBadge = false,
                                    downsampleTier = downsampleTier,
                                    targetSizePx = targetSizePx,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val s = transform.scale.coerceIn(1f, 3f)
                                            scaleX = s
                                            scaleY = s
                                            translationX = transform.offsetX * size.width
                                            translationY = transform.offsetY * size.height
                                        }
                                )
                            }
                        }
                    }

                    CollageSlotFrameStyle.EDGE_TO_EDGE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(polygonShape)
                        ) {
                            if (photo != null) {
                                PhotoArtwork(
                                    photo = photo,
                                    contentDescription = photo.caption,
                                    showVideoBadge = false,
                                    downsampleTier = downsampleTier,
                                    targetSizePx = targetSizePx,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val s = transform.scale.coerceIn(1f, 3f)
                                            scaleX = s
                                            scaleY = s
                                            translationX = transform.offsetX * size.width
                                            translationY = transform.offsetY * size.height
                                        }
                                )
                            }
                        }
                    }
                }

                // deco 金线选中描边，沿不规则多边形轮廓
                if (isSelected) {
                    val selectionAccent = DecoGold
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = buildRoundedPolygonPath(localPts, cornerPx)
                        drawPath(
                            path = path,
                            color = selectionAccent,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.92f),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
            }
        }

        // Exact polygon-aware interactive gesture overlay (reverse zIndex hit-testing -> zero diagonal overlap bugs)
        if (interactive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
                    .pointerInput(template.id, canvasW, canvasH) {
                        detectTapGestures { tapOffset ->
                            val tmpl = currentTemplate
                            val hitOrder = tmpl.slots.indices.sortedByDescending { tmpl.slots[it].zIndex }
                            val hitSlot = hitOrder.firstOrNull { idx ->
                                isPointInSlot(tapOffset, tmpl.slots[idx], canvasW, canvasH)
                            }
                            if (hitSlot != null) {
                                currentOnSlotTap?.invoke(hitSlot)
                            }
                        }
                    }
                    .then(
                        if (activeSlotIndex != null) {
                            Modifier.pointerInput(activeSlotIndex, template.id, canvasW, canvasH) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val idx = currentActiveSlot ?: return@detectTransformGestures
                                    val slot = currentTemplate.slots.getOrNull(idx) ?: return@detectTransformGestures
                                    val prev = currentTransforms.getOrNull(idx) ?: CollageSlotTransform()

                                    val slotW = ((slot.polygon.maxOf { it.x } - slot.polygon.minOf { it.x }) * canvasW).coerceAtLeast(48f)
                                    val slotH = ((slot.polygon.maxOf { it.y } - slot.polygon.minOf { it.y }) * canvasH).coerceAtLeast(48f)

                                    val nextScale = (prev.scale * zoom).coerceIn(1f, 3f)
                                    val maxOffset = ((nextScale - 1f) * 0.5f + 0.22f).coerceAtMost(0.85f)
                                    val nextOffsetX = (prev.offsetX + pan.x / slotW).coerceIn(-maxOffset, maxOffset)
                                    val nextOffsetY = (prev.offsetY + pan.y / slotH).coerceIn(-maxOffset, maxOffset)

                                    currentOnTransformSlot?.invoke(
                                        idx,
                                        CollageSlotTransform(
                                            scale = nextScale,
                                            offsetX = nextOffsetX,
                                            offsetY = nextOffsetY
                                        )
                                    )
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

/**
 * Miniature vector wireframe icon depicting each irregular collage template's exact geometry.
 */
@Composable
fun CollageTemplateWireframe(
    template: CollageTemplate,
    selected: Boolean,
    accentColor: Color,
    strokeColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = 2.dp.toPx()
        val ordered = template.slots.indices.sortedBy { template.slots[it].zIndex }

        ordered.forEach { idx ->
            val slot = template.slots[idx]
            val pts = slot.polygon.map { p ->
                Offset(
                    x = pad + p.x * (w - pad * 2f),
                    y = pad + p.y * (h - pad * 2f)
                )
            }
            val insetPts = if (slot.frameStyle == CollageSlotFrameStyle.EDGE_TO_EDGE) {
                insetConvexPolygon(pts, 1.3.dp.toPx())
            } else {
                pts
            }
            val path = buildRoundedPolygonPath(insetPts, 1.8.dp.toPx())
            val slotFill = if (slot.frameStyle == CollageSlotFrameStyle.POLAROID || slot.frameStyle == CollageSlotFrameStyle.FLOATING_INSET) {
                if (selected) accentColor.copy(alpha = 0.24f) else fillColor.copy(alpha = 0.92f)
            } else {
                if (selected) accentColor.copy(alpha = 0.20f) else fillColor
            }
            drawPath(path = path, color = slotFill)
            drawPath(
                path = path,
                color = if (selected) accentColor else strokeColor,
                style = Stroke(width = if (selected) 1.35.dp.toPx() else 1.dp.toPx())
            )
        }
    }
}
