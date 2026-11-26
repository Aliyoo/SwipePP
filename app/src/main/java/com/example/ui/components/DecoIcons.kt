package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 画报手绘图标全集：20 单位设计稿 + 1.4 细线圆头（圆角转折），
 * 与 DecoTabIcon 同一语汇（摩登画报：细线方章 + 印点实心点缀）。
 * 「选中/激活」语汇用实心点缀（日轮/心/星/方章印点填实），不整体加粗。
 * 复杂 Material 图标做语义等价简化（齿轮=六齿圆心、CleaningServices=画帚、
 * AutoAwesome=四芒星+印点、Verified=徽章圆环+芒点+勾）。
 */
enum class DecoIconKind {
    // 底部导航四签（与 DecoTabIconKind 同枚——typealias 一体）
    ALBUM, GALLERY, FOOTPRINT, VAULT,

    // 基础符号
    CLOSE, CHECK, CHECK_CIRCLE, RADIO_UNCHECKED, SELECT_ALL, MORE_HORIZ, ADD, MINUS,

    // 箭头/展开
    ARROW_BACK, ARROW_UP, ARROW_DOWN, CHEVRON_LEFT, CHEVRON_RIGHT, CHEVRON_UP, CHEVRON_DOWN,
    UNFOLD_MORE, UNFOLD_LESS, EXPAND, COMPRESS, SWIPE_UP, SWIPE_VERTICAL,

    // 检索/调节
    SEARCH, FILTER, SETTINGS, EDIT, EDIT_LOCATION, LOCATION, LOCATION_OFF, GPS, COMPASS,

    // 动作
    DELETE, DELETE_FOREVER, RESTORE, UNDO, REFRESH, RESTART, SHARE, SEND, CLEAN,
    LOCK, UNLOCK, BOLT, SPARKLE,

    // 收藏/徽记
    FAVORITE, STAR, SHIELD, VERIFIED,

    // 媒体
    PHOTO, PHOTO_LIBRARY, VIDEOCAM, BURST, LIVE, PLAY, PAUSE, VOLUME, VOLUME_OFF, FULLSCREEN, CENTER_FOCUS,

    // 结构/容器
    CALENDAR, LABEL, FOLDER, FOLDER_OPEN, FOLDER_MOVE, LAYERS, INVENTORY,
    GRID, DASHBOARD, VIEW_AGENDA, APPS,

    // 主题/设备
    SUN, MOON, CONTRAST, PHONE, VIBRATE, TOUCH, ZOOM_IN, ZOOM_OUT, FIT,

    // 说明
    INFO, HELP
}

/** typealias：底部导航四签沿用旧名（同一枚举，无重复语义）。 */
typealias DecoTabIconKind = DecoIconKind

/**
 * 通用画报图标：20 单位设计稿等比映射，1.4 细线圆头。
 * [contentDescription] 非空时写入 semantics（替代 Material Icon 的无障碍通道）。
 */
@Composable
fun DecoIcon(
    kind: DecoIconKind,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    iconSize: Dp = 20.dp,
    contentDescription: String? = null
) {
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    Canvas(modifier = semanticsModifier.size(iconSize)) {
        val unit = this.size.width / 20f
        withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
            val strokeStyle = Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawDecoIcon(kind, color, selected, strokeStyle)
        }
    }
}

private fun DrawScope.drawDecoIcon(kind: DecoIconKind, color: Color, selected: Boolean, strokeStyle: Stroke) {
    when (kind) {
        DecoIconKind.ALBUM -> drawAlbum(color, selected)
        DecoIconKind.GALLERY -> drawGallery(color, selected)
        DecoIconKind.FOOTPRINT -> drawFootprint(color, selected)
        DecoIconKind.VAULT -> drawVault(color, selected)
        DecoIconKind.CLOSE -> drawClose(color, strokeStyle)
        DecoIconKind.CHECK -> drawCheck(color, strokeStyle)
        DecoIconKind.CHECK_CIRCLE -> drawCheckCircle(color, strokeStyle)
        DecoIconKind.RADIO_UNCHECKED -> drawCircle(color, radius = 6f, center = Offset(10f, 10f), style = strokeStyle)
        DecoIconKind.SELECT_ALL -> drawSelectAll(color, strokeStyle)
        DecoIconKind.MORE_HORIZ -> drawMoreHoriz(color)
        DecoIconKind.ADD -> drawAdd(color, strokeStyle)
        DecoIconKind.MINUS -> drawLine(color, Offset(4.5f, 10f), Offset(15.5f, 10f), strokeWidth = 1.4f, cap = StrokeCap.Round)
        DecoIconKind.ARROW_BACK -> drawArrowBack(color, strokeStyle)
        DecoIconKind.ARROW_UP -> drawArrowVertical(color, up = true, strokeStyle)
        DecoIconKind.ARROW_DOWN -> drawArrowVertical(color, up = false, strokeStyle)
        DecoIconKind.CHEVRON_LEFT -> drawChevron(color, DecoIconKind.CHEVRON_LEFT, strokeStyle)
        DecoIconKind.CHEVRON_RIGHT -> drawChevron(color, DecoIconKind.CHEVRON_RIGHT, strokeStyle)
        DecoIconKind.CHEVRON_UP -> drawChevron(color, DecoIconKind.CHEVRON_UP, strokeStyle)
        DecoIconKind.CHEVRON_DOWN -> drawChevron(color, DecoIconKind.CHEVRON_DOWN, strokeStyle)
        DecoIconKind.UNFOLD_MORE -> drawUnfold(color, converging = false, strokeStyle)
        DecoIconKind.UNFOLD_LESS -> drawUnfold(color, converging = true, strokeStyle)
        DecoIconKind.EXPAND -> drawExpand(color, strokeStyle)
        DecoIconKind.COMPRESS -> drawCompress(color, strokeStyle)
        DecoIconKind.SWIPE_UP -> drawSwipeUp(color, strokeStyle)
        DecoIconKind.SWIPE_VERTICAL -> drawSwipeVertical(color, strokeStyle)
        DecoIconKind.SEARCH -> drawSearch(color, strokeStyle)
        DecoIconKind.FILTER -> drawFilter(color, strokeStyle)
        DecoIconKind.SETTINGS -> drawSettings(color, strokeStyle)
        DecoIconKind.EDIT -> drawEdit(color, strokeStyle)
        DecoIconKind.EDIT_LOCATION -> drawEditLocation(color, strokeStyle)
        DecoIconKind.LOCATION -> drawLocation(color, strokeStyle)
        DecoIconKind.LOCATION_OFF -> drawLocationOff(color, strokeStyle)
        DecoIconKind.GPS -> drawGps(color, strokeStyle)
        DecoIconKind.COMPASS -> drawCompass(color, strokeStyle)
        DecoIconKind.DELETE -> drawTrash(color, forever = false, strokeStyle)
        DecoIconKind.DELETE_FOREVER -> drawTrash(color, forever = true, strokeStyle)
        DecoIconKind.RESTORE -> arcArrow(color, 10f, 10f, 5.6f, startAngle = -90f, sweepAngle = -290f, strokeStyle)
        DecoIconKind.UNDO -> drawUndo(color, strokeStyle)
        DecoIconKind.REFRESH -> arcArrow(color, 10f, 10f, 5.6f, startAngle = -90f, sweepAngle = 290f, strokeStyle)
        DecoIconKind.RESTART -> {
            arcArrow(color, 10f, 10f, 5.6f, startAngle = -90f, sweepAngle = 300f, strokeStyle)
            drawCircle(color = color, radius = 1.3f, center = Offset(10f, 10f))
        }
        DecoIconKind.SHARE -> drawShare(color, strokeStyle)
        DecoIconKind.SEND -> drawSend(color, strokeStyle)
        DecoIconKind.CLEAN -> drawClean(color, strokeStyle)
        DecoIconKind.LOCK -> drawLock(color, open = false, strokeStyle)
        DecoIconKind.UNLOCK -> drawLock(color, open = true, strokeStyle)
        DecoIconKind.BOLT -> drawBolt(color, strokeStyle)
        DecoIconKind.SPARKLE -> drawSparkle(color, strokeStyle)
        DecoIconKind.FAVORITE -> drawHeart(color, selected, strokeStyle)
        DecoIconKind.STAR -> drawStar(color, selected, strokeStyle)
        DecoIconKind.SHIELD -> drawShield(color, strokeStyle)
        DecoIconKind.VERIFIED -> drawVerified(color, strokeStyle)
        DecoIconKind.PHOTO -> drawPhoto(color, selected, strokeStyle)
        DecoIconKind.PHOTO_LIBRARY -> drawPhotoLibrary(color, selected, strokeStyle)
        DecoIconKind.VIDEOCAM -> drawVideocam(color, strokeStyle)
        DecoIconKind.BURST -> drawBurst(color, strokeStyle)
        DecoIconKind.LIVE -> drawLivePhoto(color, strokeStyle)
        DecoIconKind.PLAY -> drawPlay(color, strokeStyle)
        DecoIconKind.PAUSE -> drawPause(color)
        DecoIconKind.VOLUME -> drawVolume(color, muted = false, strokeStyle)
        DecoIconKind.VOLUME_OFF -> drawVolume(color, muted = true, strokeStyle)
        DecoIconKind.FULLSCREEN -> drawFullscreen(color, strokeStyle)
        DecoIconKind.CENTER_FOCUS -> drawCenterFocus(color, strokeStyle)
        DecoIconKind.CALENDAR -> drawCalendar(color, strokeStyle)
        DecoIconKind.LABEL -> drawLabel(color, strokeStyle)
        DecoIconKind.FOLDER -> drawFolder(color, strokeStyle)
        DecoIconKind.FOLDER_OPEN -> drawFolderOpen(color, strokeStyle)
        DecoIconKind.FOLDER_MOVE -> drawFolderMove(color, strokeStyle)
        DecoIconKind.LAYERS -> drawLayers(color, strokeStyle)
        DecoIconKind.INVENTORY -> drawInventory(color, strokeStyle)
        DecoIconKind.GRID -> drawGrid(color, strokeStyle)
        DecoIconKind.DASHBOARD -> drawDashboard(color, strokeStyle)
        DecoIconKind.VIEW_AGENDA -> drawViewAgenda(color, strokeStyle)
        DecoIconKind.APPS -> drawApps(color)
        DecoIconKind.SUN -> drawSun(color, strokeStyle)
        DecoIconKind.MOON -> drawMoon(color, strokeStyle)
        DecoIconKind.CONTRAST -> drawContrast(color, strokeStyle)
        DecoIconKind.PHONE -> drawPhone(color, strokeStyle)
        DecoIconKind.VIBRATE -> drawVibrate(color, strokeStyle)
        DecoIconKind.TOUCH -> drawTouch(color, strokeStyle)
        DecoIconKind.ZOOM_IN -> drawZoom(color, zoomIn = true, strokeStyle)
        DecoIconKind.ZOOM_OUT -> drawZoom(color, zoomIn = false, strokeStyle)
        DecoIconKind.FIT -> drawFit(color, strokeStyle)
        DecoIconKind.INFO -> drawInfo(color, strokeStyle)
        DecoIconKind.HELP -> drawHelp(color, strokeStyle)
    }.let { /* exhaustive */ }
}

// ── 通用小件 ──────────────────────────────────────────────────────────

/** 折线/多边形路径（20 单位坐标点列） */
private fun decoPath(vararg pts: Pair<Float, Float>, close: Boolean = false): Path = Path().apply {
    moveTo(pts[0].first, pts[0].second)
    pts.drop(1).forEach { lineTo(it.first, it.second) }
    if (close) close()
}

/**
 * 圆弧箭头：弧线 + 末端切向箭头（RESTORE↺ / REFRESH↻ / RESTART 共用）。
 * 角度系同 Compose drawArc：0° 在 3 点钟方向，正 sweep 顺时针（屏幕坐标 y 向下）。
 */
private fun DrawScope.arcArrow(
    color: Color,
    cx: Float,
    cy: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    strokeStyle: Stroke
) {
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = strokeStyle
    )
    val endRad = Math.toRadians((startAngle + sweepAngle).toDouble())
    val px = cx + radius * cos(endRad).toFloat()
    val py = cy + radius * sin(endRad).toFloat()
    // 行进方向（切线）：正 sweep 顺时针 → (-sinθ, cosθ)；负 sweep 反向
    val sgn = if (sweepAngle >= 0f) 1f else -1f
    val tx = -sin(endRad).toFloat() * sgn
    val ty = cos(endRad).toFloat() * sgn
    val nx = cos(endRad).toFloat()
    val ny = sin(endRad).toFloat()
    val back = 2.4f
    val side = 1.35f
    drawLine(color, Offset(px, py), Offset(px - back * tx + side * nx, py - back * ty + side * ny), strokeWidth = strokeStyle.width, cap = StrokeCap.Round)
    drawLine(color, Offset(px, py), Offset(px - back * tx - side * nx, py - back * ty - side * ny), strokeWidth = strokeStyle.width, cap = StrokeCap.Round)
}

// ── 底部导航四签（原 DecoTabIcons 语汇原样保留） ─────────────────────

/** 影集（宋刻）：底层细框 + 前框横细竖粗填充笔画 + 中心章点 */
private fun DrawScope.drawAlbum(color: Color, selected: Boolean) {
    drawRect(
        color = color.copy(alpha = 0.55f),
        topLeft = Offset(4.2f, 3f),
        size = Size(12.6f, 12.6f),
        style = Stroke(width = 0.55f)
    )
    drawRect(color = color, topLeft = Offset(3f, 6f), size = Size(2.1f, 10.6f))
    drawRect(color = color, topLeft = Offset(14.9f, 6f), size = Size(2.1f, 10.6f))
    drawRect(color = color, topLeft = Offset(3f, 6f), size = Size(14f, 1.05f))
    drawRect(color = color, topLeft = Offset(3f, 15.55f), size = Size(14f, 1.05f))
    drawRect(color = color, topLeft = Offset(8f, 9.4f), size = Size(4f, 3.4f))
}

/** 画廊（宋刻）：竖粗横细外框 + 细山折线 + 实心日轮 */
private fun DrawScope.drawGallery(color: Color, selected: Boolean) {
    drawRect(color = color, topLeft = Offset(2.6f, 3f), size = Size(1.9f, 14f))
    drawRect(color = color, topLeft = Offset(15.5f, 3f), size = Size(1.9f, 14f))
    drawRect(color = color, topLeft = Offset(2.6f, 3f), size = Size(14.8f, 0.95f))
    drawRect(color = color, topLeft = Offset(2.6f, 16.05f), size = Size(14.8f, 0.95f))
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(5.2f, 13.4f)
            lineTo(8.6f, 9.2f)
            lineTo(11f, 11.8f)
            lineTo(13.2f, 9.6f)
            lineTo(15.4f, 13.2f)
        },
        color = color,
        style = Stroke(width = 0.6f)
    )
    drawCircle(color = color, radius = 1.5f, center = Offset(13.4f, 6.6f))
}

/** 足迹（宋刻）：粗山河折线 + 方峰印 + 细地平线 */
private fun DrawScope.drawFootprint(color: Color, selected: Boolean) {
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(2f, 15.2f)
            lineTo(6.6f, 8.4f)
            lineTo(10f, 12f)
            lineTo(13.4f, 5.4f)
            lineTo(18f, 13.2f)
        },
        color = color,
        style = Stroke(width = 1.9f)
    )
    drawRect(color = color, topLeft = Offset(12.5f, 4.5f), size = Size(1.9f, 1.9f))
    drawRect(color = color, topLeft = Offset(1.6f, 17.2f), size = Size(16.8f, 0.9f))
}

/** 回收站（宋刻）：细篓盖提手 + 竖粗篓边 + 横细口底 + 细篾纹 */
private fun DrawScope.drawVault(color: Color, selected: Boolean) {
    drawRect(color = color, topLeft = Offset(3.2f, 5.2f), size = Size(13.6f, 1f))
    drawRect(color = color, topLeft = Offset(7.6f, 3f), size = Size(4.8f, 0.9f))
    drawRect(color = color, topLeft = Offset(4.4f, 7.2f), size = Size(2f, 10.4f))
    drawRect(color = color, topLeft = Offset(13.6f, 7.2f), size = Size(2f, 10.4f))
    drawRect(color = color, topLeft = Offset(6.4f, 7.2f), size = Size(7.2f, 0.9f))
    drawRect(color = color, topLeft = Offset(6.4f, 16.7f), size = Size(7.2f, 0.9f))
    drawLine(
        color = color.copy(alpha = 0.8f),
        start = Offset(8.6f, 9.4f),
        end = Offset(8.6f, 15.8f),
        strokeWidth = 0.6f
    )
    drawLine(
        color = color.copy(alpha = 0.8f),
        start = Offset(11.4f, 9.4f),
        end = Offset(11.4f, 15.8f),
        strokeWidth = 0.6f
    )
}

// ── 基础符号 ──────────────────────────────────────────────────────────

private fun DrawScope.drawClose(color: Color, strokeStyle: Stroke) {
    drawLine(color, Offset(5.5f, 5.5f), Offset(14.5f, 14.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(14.5f, 5.5f), Offset(5.5f, 14.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
}

private fun DrawScope.drawCheck(color: Color, strokeStyle: Stroke) {
    drawPath(path = decoPath(4f to 11f, 8.5f to 15.5f, 16f to 6f), color = color, style = strokeStyle)
}

private fun DrawScope.drawCheckCircle(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 7f, center = Offset(10f, 10f), style = strokeStyle)
    drawPath(path = decoPath(6.4f to 10.2f, 9f to 12.8f, 13.8f to 7.4f), color = color, style = strokeStyle)
}

private fun DrawScope.drawSelectAll(color: Color, strokeStyle: Stroke) {
    drawRoundRect(
        color = color,
        topLeft = Offset(3.5f, 3.5f),
        size = Size(13f, 13f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    drawPath(path = decoPath(6.8f to 10.2f, 9.2f to 12.6f, 13.4f to 7.6f), color = color, style = strokeStyle)
}

private fun DrawScope.drawMoreHoriz(color: Color) {
    listOf(4.5f, 10f, 15.5f).forEach { x ->
        drawCircle(color = color, radius = 1.2f, center = Offset(x, 10f))
    }
}

private fun DrawScope.drawAdd(color: Color, strokeStyle: Stroke) {
    drawLine(color, Offset(10f, 4.5f), Offset(10f, 15.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(4.5f, 10f), Offset(15.5f, 10f), strokeWidth = 1.4f, cap = StrokeCap.Round)
}

// ── 箭头/展开 ─────────────────────────────────────────────────────────

private fun DrawScope.drawArrowBack(color: Color, strokeStyle: Stroke) {
    drawLine(color, Offset(16.5f, 10f), Offset(4f, 10f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(8.6f to 4.8f, 4f to 10f, 8.6f to 15.2f), color = color, style = strokeStyle)
}

private fun DrawScope.drawArrowVertical(color: Color, up: Boolean, strokeStyle: Stroke) {
    if (up) {
        drawLine(color, Offset(10f, 16.5f), Offset(10f, 4f), strokeWidth = 1.4f, cap = StrokeCap.Round)
        drawPath(path = decoPath(5.6f to 8.2f, 10f to 4f, 14.4f to 8.2f), color = color, style = strokeStyle)
    } else {
        drawLine(color, Offset(10f, 3.5f), Offset(10f, 16f), strokeWidth = 1.4f, cap = StrokeCap.Round)
        drawPath(path = decoPath(5.6f to 11.8f, 10f to 16f, 14.4f to 11.8f), color = color, style = strokeStyle)
    }
}

private fun DrawScope.drawChevron(color: Color, kind: DecoIconKind, strokeStyle: Stroke) {
    val path = when (kind) {
        DecoIconKind.CHEVRON_LEFT -> decoPath(12.5f to 4.5f, 6.5f to 10f, 12.5f to 15.5f)
        DecoIconKind.CHEVRON_RIGHT -> decoPath(7.5f to 4.5f, 13.5f to 10f, 7.5f to 15.5f)
        DecoIconKind.CHEVRON_UP -> decoPath(4.5f to 12.5f, 10f to 6.5f, 15.5f to 12.5f)
        else -> decoPath(4.5f to 7.5f, 10f to 13.5f, 15.5f to 7.5f)
    }
    drawPath(path = path, color = color, style = strokeStyle)
}

private fun DrawScope.drawUnfold(color: Color, converging: Boolean, strokeStyle: Stroke) {
    if (converging) {
        // 上尖朝下、下尖朝上（收拢）
        drawPath(path = decoPath(6f to 4f, 10f to 7.6f, 14f to 4f), color = color, style = strokeStyle)
        drawPath(path = decoPath(6f to 16f, 10f to 12.4f, 14f to 16f), color = color, style = strokeStyle)
    } else {
        // 上尖朝上、下尖朝下（展开）
        drawPath(path = decoPath(6f to 7.6f, 10f to 4f, 14f to 7.6f), color = color, style = strokeStyle)
        drawPath(path = decoPath(6f to 12.4f, 10f to 16f, 14f to 12.4f), color = color, style = strokeStyle)
    }
}

private fun DrawScope.drawExpand(color: Color, strokeStyle: Stroke) {
    // 对角双箭头（OpenInFull 语汇：斜杆 + 两端 L 形箭头）
    drawLine(color, Offset(6.2f, 6.2f), Offset(13.8f, 13.8f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(6.2f to 10.6f, 6.2f to 6.2f, 10.6f to 6.2f), color = color, style = strokeStyle)
    drawPath(path = decoPath(13.8f to 9.4f, 13.8f to 13.8f, 9.4f to 13.8f), color = color, style = strokeStyle)
}

private fun DrawScope.drawCompress(color: Color, strokeStyle: Stroke) {
    // 上下两支向心箭头
    drawLine(color, Offset(10f, 3.5f), Offset(10f, 9f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(6.6f to 6.2f, 10f to 9f, 13.4f to 6.2f), color = color, style = strokeStyle)
    drawLine(color, Offset(10f, 16.5f), Offset(10f, 11f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(6.6f to 13.8f, 10f to 11f, 13.4f to 13.8f), color = color, style = strokeStyle)
}

private fun DrawScope.drawSwipeUp(color: Color, strokeStyle: Stroke) {
    drawLine(color, Offset(10f, 15.5f), Offset(10f, 4.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(6.6f to 8f, 10f to 4.5f, 13.4f to 8f), color = color, style = strokeStyle)
    drawLine(color = color, start = Offset(5.5f, 17.5f), end = Offset(14.5f, 17.5f), strokeWidth = 1.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawSwipeVertical(color: Color, strokeStyle: Stroke) {
    drawLine(color, Offset(10f, 5.8f), Offset(10f, 14.2f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(6.8f to 8.4f, 10f to 5.5f, 13.2f to 8.4f), color = color, style = strokeStyle)
    drawPath(path = decoPath(6.8f to 11.6f, 10f to 14.5f, 13.2f to 11.6f), color = color, style = strokeStyle)
    drawCircle(color = color, radius = 1f, center = Offset(4.2f, 10f))
    drawCircle(color = color, radius = 1f, center = Offset(15.8f, 10f))
}

// ── 检索/调节 ─────────────────────────────────────────────────────────

private fun DrawScope.drawSearch(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 5.6f, center = Offset(9f, 9f), style = strokeStyle)
    drawLine(color, Offset(13.2f, 13.2f), Offset(16.5f, 16.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
}

private fun DrawScope.drawFilter(color: Color, strokeStyle: Stroke) {
    // 三横杆滑杆 + 印点滑钮（Tune 同语汇）
    drawLine(color, Offset(4f, 5.5f), Offset(16f, 5.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(4f, 10f), Offset(16f, 10f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(4f, 14.5f), Offset(16f, 14.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawCircle(color = color, radius = 1.5f, center = Offset(13f, 5.5f))
    drawCircle(color = color, radius = 1.5f, center = Offset(7f, 10f))
    drawCircle(color = color, radius = 1.5f, center = Offset(11f, 14.5f))
}

private fun DrawScope.drawSettings(color: Color, strokeStyle: Stroke) {
    // 齿轮语义等价简化：圆环 + 六齿 + 圆心印点
    drawCircle(color = color, radius = 5.2f, center = Offset(10f, 10f), style = strokeStyle)
    for (i in 0 until 6) {
        val rad = Math.toRadians((i * 60).toDouble())
        val cosV = cos(rad).toFloat()
        val sinV = sin(rad).toFloat()
        drawLine(
            color = color,
            start = Offset(10f + 6.2f * cosV, 10f + 6.2f * sinV),
            end = Offset(10f + 8f * cosV, 10f + 8f * sinV),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
        )
    }
    drawCircle(color = color, radius = 1.3f, center = Offset(10f, 10f))
}

private fun DrawScope.drawEdit(color: Color, strokeStyle: Stroke) {
    // 铅笔：斜杆笔身 + 笔尖三角 + 笔箍线
    drawPath(
        path = decoPath(13.2f to 4.4f, 15.6f to 6.8f, 7.2f to 15.2f, 4.2f to 15.8f, 4.8f to 12.8f, close = true),
        color = color,
        style = strokeStyle
    )
    drawLine(color, Offset(12.2f, 5.4f), Offset(14.6f, 7.8f), strokeWidth = 1.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawEditLocation(color: Color, strokeStyle: Stroke) {
    // 水滴 pins（左移缩小）+ 侧旁小笔
    drawPath(
        path = Path().apply {
            moveTo(8.5f, 16.6f)
            cubicTo(8.5f, 16.6f, 4f, 12.2f, 4f, 9.3f)
            cubicTo(4f, 6.5f, 6f, 4.5f, 8.5f, 4.5f)
            cubicTo(11f, 4.5f, 13f, 6.5f, 13f, 9.3f)
            cubicTo(13f, 12.2f, 8.5f, 16.6f, 8.5f, 16.6f)
            close()
        },
        color = color,
        style = strokeStyle
    )
    drawCircle(color = color, radius = 1.4f, center = Offset(8.5f, 9.3f))
    drawLine(color, Offset(14.4f, 14.6f), Offset(17.4f, 11.6f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(13.6f to 16.4f, 14.4f to 14.6f, 16.2f to 16f, close = true), color = color, style = strokeStyle)
}

private fun DrawScope.drawLocation(color: Color, strokeStyle: Stroke) {
    // 水滴 pins + 圆心印点
    drawPath(
        path = Path().apply {
            moveTo(10f, 17f)
            cubicTo(10f, 17f, 4.5f, 11.7f, 4.5f, 8.4f)
            cubicTo(4.5f, 5.4f, 7f, 3.5f, 10f, 3.5f)
            cubicTo(13f, 3.5f, 15.5f, 5.4f, 15.5f, 8.4f)
            cubicTo(15.5f, 11.7f, 10f, 17f, 10f, 17f)
            close()
        },
        color = color,
        style = strokeStyle
    )
    drawCircle(color = color, radius = 1.6f, center = Offset(10f, 8.4f))
}

private fun DrawScope.drawLocationOff(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = Path().apply {
            moveTo(10f, 17f)
            cubicTo(10f, 17f, 4.5f, 11.7f, 4.5f, 8.4f)
            cubicTo(4.5f, 5.4f, 7f, 3.5f, 10f, 3.5f)
            cubicTo(13f, 3.5f, 15.5f, 5.4f, 15.5f, 8.4f)
            cubicTo(15.5f, 11.7f, 10f, 17f, 10f, 17f)
            close()
        },
        color = color.copy(alpha = 0.55f),
        style = strokeStyle
    )
    drawLine(color, Offset(4f, 4f), Offset(16f, 16f), strokeWidth = 1.4f, cap = StrokeCap.Round)
}

private fun DrawScope.drawGps(color: Color, strokeStyle: Stroke) {
    // 十字准星 + 圆心印点（GpsFixed / MyLocation 同语汇合并）
    drawCircle(color = color, radius = 4.6f, center = Offset(10f, 10f), style = strokeStyle)
    drawCircle(color = color, radius = 1.5f, center = Offset(10f, 10f))
    drawLine(color, Offset(10f, 3f), Offset(10f, 5.4f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(10f, 14.6f), Offset(10f, 17f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(3f, 10f), Offset(5.4f, 10f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(14.6f, 10f), Offset(17f, 10f), strokeWidth = 1.3f, cap = StrokeCap.Round)
}

private fun DrawScope.drawCompass(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 7f, center = Offset(10f, 10f), style = strokeStyle)
    drawPath(
        path = decoPath(10f to 5.4f, 12.2f to 10f, 10f to 14.6f, 7.8f to 10f, close = true),
        color = color,
        style = strokeStyle
    )
    drawPath(path = decoPath(10f to 5.4f, 12.2f to 10f, 10f to 14.6f, close = true), color = color)
}

// ── 动作 ──────────────────────────────────────────────────────────────

private fun DrawScope.drawTrash(color: Color, forever: Boolean, strokeStyle: Stroke) {
    val r = CornerRadius(1.5f)
    drawLine(color, Offset(4f, 5.5f), Offset(16f, 5.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(path = decoPath(8f to 5.5f, 8f to 3.6f, 12f to 3.6f, 12f to 5.5f), color = color, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(5f, 7.5f), size = Size(10f, 10f), cornerRadius = r, style = strokeStyle)
    if (forever) {
        drawLine(color, Offset(8.2f, 10.2f), Offset(11.8f, 14.8f), strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(color, Offset(11.8f, 10.2f), Offset(8.2f, 14.8f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    } else {
        drawLine(color, Offset(8.5f, 10f), Offset(8.5f, 14.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(color, Offset(11.5f, 10f), Offset(11.5f, 14.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawUndo(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = Path().apply {
            moveTo(15.6f, 14.4f)
            cubicTo(15.2f, 9.6f, 12.2f, 7.6f, 8f, 7.6f)
            lineTo(4.8f, 7.6f)
        },
        color = color,
        style = strokeStyle
    )
    drawLine(color, Offset(4.8f, 7.6f), Offset(7.4f, 5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(4.8f, 7.6f), Offset(7.4f, 10.2f), strokeWidth = 1.4f, cap = StrokeCap.Round)
}

private fun DrawScope.drawShare(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 2.1f, center = Offset(14.6f, 5.6f), style = strokeStyle)
    drawCircle(color = color, radius = 2.1f, center = Offset(5.4f, 10f), style = strokeStyle)
    drawCircle(color = color, radius = 2.1f, center = Offset(14.6f, 14.4f), style = strokeStyle)
    drawLine(color, Offset(7.4f, 8.9f), Offset(12.7f, 6.7f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(7.4f, 11.1f), Offset(12.7f, 13.3f), strokeWidth = 1.3f, cap = StrokeCap.Round)
}

private fun DrawScope.drawSend(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = decoPath(17f to 3f, 3f to 9.8f, 9.2f to 12.2f, 10.8f to 17.4f, close = true),
        color = color,
        style = strokeStyle
    )
    drawLine(color, Offset(17f, 3f), Offset(9.2f, 12.2f), strokeWidth = 1.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawClean(color: Color, strokeStyle: Stroke) {
    // CleaningServices 语义等价简化：画帚（斜柄 + 撇刷 + 篾纹）
    drawLine(color, Offset(14.2f, 3.4f), Offset(8.8f, 10.4f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawPath(
        path = decoPath(8.8f to 10.4f, 4.6f to 16.6f, 12.4f to 16.6f, close = true),
        color = color,
        style = strokeStyle
    )
    drawLine(color, Offset(6.6f, 16.6f), Offset(7.6f, 13.6f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    drawLine(color, Offset(10.2f, 16.6f), Offset(9.6f, 13.6f), strokeWidth = 1.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawLock(color: Color, open: Boolean, strokeStyle: Stroke) {
    drawRoundRect(
        color = color,
        topLeft = Offset(5.5f, 9f),
        size = Size(9f, 8f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    if (open) {
        // 锁扣脱开：右腿悬空
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(6.6f, 4.6f),
            size = Size(6.8f, 6.8f),
            style = strokeStyle
        )
        drawLine(color, Offset(6.6f, 8f), Offset(6.6f, 9f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    } else {
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(6.6f, 4.2f),
            size = Size(6.8f, 6.8f),
            style = strokeStyle
        )
    }
    drawCircle(color = color, radius = 1.2f, center = Offset(10f, 12.6f))
    drawLine(color, Offset(10f, 12.6f), Offset(10f, 14.4f), strokeWidth = 1.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawBolt(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = decoPath(11.6f to 3f, 5f to 11.4f, 9.4f to 11.4f, 8.4f to 17f, 15f to 8.6f, 10.6f to 8.6f, close = true),
        color = color,
        style = strokeStyle
    )
}

private fun DrawScope.drawSparkle(color: Color, strokeStyle: Stroke) {
    // AutoAwesome 语义等价简化：四芒星 + 两粒印点
    drawPath(
        path = Path().apply {
            moveTo(10f, 3.6f)
            quadraticTo(10.8f, 8.4f, 14.6f, 10f)
            quadraticTo(10.8f, 11.6f, 10f, 16.4f)
            quadraticTo(9.2f, 11.6f, 5.4f, 10f)
            quadraticTo(9.2f, 8.4f, 10f, 3.6f)
            close()
        },
        color = color,
        style = strokeStyle
    )
    drawCircle(color = color, radius = 0.9f, center = Offset(15.6f, 4.6f))
    drawCircle(color = color, radius = 0.9f, center = Offset(4.8f, 15.2f))
}

// ── 收藏/徽记 ─────────────────────────────────────────────────────────

private fun DrawScope.drawHeart(color: Color, selected: Boolean, strokeStyle: Stroke) {
    val path = Path().apply {
        moveTo(10f, 16.8f)
        cubicTo(4.2f, 12.4f, 3.2f, 8f, 6.2f, 5.8f)
        cubicTo(8.1f, 4.4f, 10f, 6.1f, 10f, 7.5f)
        cubicTo(10f, 6.1f, 11.9f, 4.4f, 13.8f, 5.8f)
        cubicTo(16.8f, 8f, 15.8f, 12.4f, 10f, 16.8f)
        close()
    }
    if (selected) {
        drawPath(path = path, color = color)
    } else {
        drawPath(path = path, color = color, style = strokeStyle)
    }
}

private fun DrawScope.drawStar(color: Color, selected: Boolean, strokeStyle: Stroke) {
    val path = decoPath(
        10f to 4.5f, 11.6f to 8.8f, 16.2f to 9f, 12.6f to 11.8f, 13.8f to 16.3f,
        10f to 13.7f, 6.2f to 16.3f, 7.4f to 11.8f, 3.8f to 9f, 8.4f to 8.8f,
        close = true
    )
    if (selected) {
        drawPath(path = path, color = color)
    } else {
        drawPath(path = path, color = color, style = strokeStyle)
    }
}

private fun DrawScope.drawShield(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = Path().apply {
            moveTo(10f, 3.4f)
            lineTo(15.6f, 5.7f)
            lineTo(15.6f, 10.2f)
            cubicTo(15.6f, 13.9f, 13.2f, 15.9f, 10f, 16.9f)
            cubicTo(6.8f, 15.9f, 4.4f, 13.9f, 4.4f, 10.2f)
            lineTo(4.4f, 5.7f)
            close()
        },
        color = color,
        style = strokeStyle
    )
    drawPath(path = decoPath(7.4f to 10f, 9.3f to 11.9f, 12.9f to 8.2f), color = color, style = strokeStyle)
}

private fun DrawScope.drawVerified(color: Color, strokeStyle: Stroke) {
    // Verified 语义等价简化：徽章圆环 + 八向芒点 + 勾
    drawCircle(color = color, radius = 5.6f, center = Offset(10f, 10f), style = strokeStyle)
    for (i in 0 until 8) {
        val rad = Math.toRadians((i * 45 + 22.5).toDouble())
        val cosV = cos(rad).toFloat()
        val sinV = sin(rad).toFloat()
        drawLine(
            color = color,
            start = Offset(10f + 6.5f * cosV, 10f + 6.5f * sinV),
            end = Offset(10f + 7.9f * cosV, 10f + 7.9f * sinV),
            strokeWidth = 1.3f,
            cap = StrokeCap.Round
        )
    }
    drawPath(path = decoPath(7.2f to 10.1f, 9.2f to 12.1f, 13f to 8.1f), color = color, style = strokeStyle)
}

// ── 媒体 ──────────────────────────────────────────────────────────────

private fun DrawScope.drawPhoto(color: Color, selected: Boolean, strokeStyle: Stroke) {
    drawRoundRect(
        color = color,
        topLeft = Offset(3f, 4f),
        size = Size(14f, 12f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    drawPath(
        path = decoPath(5.4f to 13.4f, 8.4f to 9.4f, 10.4f to 11.4f, 12f to 9.8f, 14.8f to 13.4f),
        color = color,
        style = strokeStyle
    )
    if (selected) {
        drawCircle(color = color, radius = 1.6f, center = Offset(13.2f, 7.2f))
    } else {
        drawCircle(color = color, radius = 1.6f, center = Offset(13.2f, 7.2f), style = strokeStyle)
    }
}

private fun DrawScope.drawPhotoLibrary(color: Color, selected: Boolean, strokeStyle: Stroke) {
    val r = CornerRadius(1.5f)
    drawRoundRect(
        color = color.copy(alpha = 0.45f),
        topLeft = Offset(6f, 3f),
        size = Size(11f, 9.5f),
        cornerRadius = r,
        style = strokeStyle
    )
    drawRoundRect(color = color, topLeft = Offset(3f, 6f), size = Size(14f, 11f), cornerRadius = r, style = strokeStyle)
    drawPath(
        path = decoPath(5.4f to 14.2f, 8f to 10.6f, 10f to 12.6f, 11.8f to 10.8f, 14.6f to 14.2f),
        color = color,
        style = strokeStyle
    )
    if (selected) {
        drawCircle(color = color, radius = 1.3f, center = Offset(12.4f, 8.8f))
    }
}

private fun DrawScope.drawVideocam(color: Color, strokeStyle: Stroke) {
    drawRoundRect(
        color = color,
        topLeft = Offset(3f, 6.4f),
        size = Size(10.6f, 7.6f),
        cornerRadius = CornerRadius(1.8f),
        style = strokeStyle
    )
    drawPath(
        path = decoPath(13.6f to 9.4f, 17.4f to 7.4f, 17.4f to 13f, 13.6f to 11f, close = true),
        color = color,
        style = strokeStyle
    )
    drawCircle(color = color, radius = 1.1f, center = Offset(8.2f, 10.2f))
}

private fun DrawScope.drawBurst(color: Color, strokeStyle: Stroke) {
    // 连拍：错位叠片（后两片弱化）+ 前片
    drawRoundRect(
        color = color.copy(alpha = 0.28f),
        topLeft = Offset(8.6f, 2.8f),
        size = Size(8f, 10f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    drawRoundRect(
        color = color.copy(alpha = 0.5f),
        topLeft = Offset(6.2f, 4.8f),
        size = Size(8f, 10f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(3.4f, 6.8f),
        size = Size(8f, 10f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
}

private fun DrawScope.drawLivePhoto(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 7f, center = Offset(10f, 10f), style = strokeStyle)
    drawCircle(color = color.copy(alpha = 0.58f), radius = 4.3f, center = Offset(10f, 10f), style = strokeStyle)
    drawCircle(color = color, radius = 1.5f, center = Offset(10f, 10f))
}

private fun DrawScope.drawPlay(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = decoPath(6.6f to 4.6f, 15.6f to 10f, 6.6f to 15.4f, close = true),
        color = color,
        style = strokeStyle
    )
}

private fun DrawScope.drawPause(color: Color) {
    drawLine(color, Offset(7.4f, 5.4f), Offset(7.4f, 14.6f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(12.6f, 5.4f), Offset(12.6f, 14.6f), strokeWidth = 2.4f, cap = StrokeCap.Round)
}

private fun DrawScope.drawVolume(color: Color, muted: Boolean, strokeStyle: Stroke) {
    drawPath(
        path = decoPath(4.4f to 8f, 7.4f to 8f, 11f to 4.8f, 11f to 15.2f, 7.4f to 12f, 4.4f to 12f, close = true),
        color = color,
        style = strokeStyle
    )
    if (muted) {
        drawLine(color, Offset(13.4f, 6.4f), Offset(17f, 13.6f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    } else {
        drawArc(
            color = color,
            startAngle = -55f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(12.2f, 6.4f),
            size = Size(6.8f, 7.2f),
            style = strokeStyle
        )
    }
}

private fun DrawScope.drawFullscreen(color: Color, strokeStyle: Stroke) {
    drawPath(path = decoPath(4f to 8f, 4f to 4f, 8f to 4f), color = color, style = strokeStyle)
    drawPath(path = decoPath(12f to 4f, 16f to 4f, 16f to 8f), color = color, style = strokeStyle)
    drawPath(path = decoPath(16f to 12f, 16f to 16f, 12f to 16f), color = color, style = strokeStyle)
    drawPath(path = decoPath(8f to 16f, 4f to 16f, 4f to 12f), color = color, style = strokeStyle)
}

private fun DrawScope.drawCenterFocus(color: Color, strokeStyle: Stroke) {
    drawPath(path = decoPath(3.5f to 7f, 3.5f to 3.5f, 7f to 3.5f), color = color, style = strokeStyle)
    drawPath(path = decoPath(13f to 3.5f, 16.5f to 3.5f, 16.5f to 7f), color = color, style = strokeStyle)
    drawPath(path = decoPath(16.5f to 13f, 16.5f to 16.5f, 13f to 16.5f), color = color, style = strokeStyle)
    drawPath(path = decoPath(7f to 16.5f, 3.5f to 16.5f, 3.5f to 13f), color = color, style = strokeStyle)
    drawCircle(color = color, radius = 2.2f, center = Offset(10f, 10f))
}

// ── 结构/容器 ─────────────────────────────────────────────────────────

private fun DrawScope.drawCalendar(color: Color, strokeStyle: Stroke) {
    drawRoundRect(
        color = color,
        topLeft = Offset(3.5f, 4.5f),
        size = Size(13f, 12f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    drawLine(color, Offset(3.5f, 8.2f), Offset(16.5f, 8.2f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(6.6f, 3f), Offset(6.6f, 6f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(13.4f, 3f), Offset(13.4f, 6f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawCircle(color = color, radius = 1.2f, center = Offset(10f, 12.4f))
}

private fun DrawScope.drawLabel(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = decoPath(4.5f to 4.5f, 9.6f to 4.5f, 16f to 10.6f, 10.6f to 16f, 4.5f to 16f, close = true),
        color = color,
        style = strokeStyle
    )
    drawCircle(color = color, radius = 1.1f, center = Offset(7.6f, 7.6f), style = strokeStyle)
}

private fun DrawScope.drawFolder(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = Path().apply {
            moveTo(3.5f, 15.4f)
            lineTo(3.5f, 5.6f)
            quadraticTo(3.5f, 4.2f, 4.9f, 4.2f)
            lineTo(8.2f, 4.2f)
            lineTo(10.2f, 6.2f)
            lineTo(15.1f, 6.2f)
            quadraticTo(16.5f, 6.2f, 16.5f, 7.6f)
            lineTo(16.5f, 15.4f)
            quadraticTo(16.5f, 16.8f, 15.1f, 16.8f)
            lineTo(4.9f, 16.8f)
            quadraticTo(3.5f, 16.8f, 3.5f, 15.4f)
            close()
        },
        color = color,
        style = strokeStyle
    )
}

private fun DrawScope.drawFolderOpen(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = decoPath(4f to 15.2f, 4f to 5.6f, 8.6f to 5.6f, 10.2f to 7.2f, 14f to 7.2f),
        color = color,
        style = strokeStyle
    )
    drawPath(
        path = decoPath(4f to 15.2f, 6.2f to 9.8f, 17.6f to 9.8f, 15.4f to 15.2f, close = true),
        color = color,
        style = strokeStyle
    )
}

private fun DrawScope.drawFolderMove(color: Color, strokeStyle: Stroke) {
    drawFolder(color, strokeStyle)
    drawLine(color, Offset(8f, 11.5f), Offset(12.6f, 11.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawPath(path = decoPath(11f to 9.7f, 12.6f to 11.5f, 11f to 13.3f), color = color, style = strokeStyle)
}

private fun DrawScope.drawLayers(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = decoPath(10f to 3.8f, 16.6f to 7.4f, 10f to 11f, 3.4f to 7.4f, close = true),
        color = color,
        style = strokeStyle
    )
    drawPath(path = decoPath(3.4f to 11.2f, 10f to 14.8f, 16.6f to 11.2f), color = color, style = strokeStyle)
    drawPath(path = decoPath(3.4f to 14.6f, 10f to 18.2f, 16.6f to 14.6f), color = color.copy(alpha = 0.5f), style = strokeStyle)
}

private fun DrawScope.drawInventory(color: Color, strokeStyle: Stroke) {
    // 叠组箱：箱体 + 分隔线 + 提梁（bundle/inventory 语汇）
    drawRoundRect(
        color = color,
        topLeft = Offset(3.5f, 7f),
        size = Size(13f, 9.5f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    drawLine(color, Offset(3.5f, 10.2f), Offset(16.5f, 10.2f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(7.5f, 4.2f),
        size = Size(5f, 5.6f),
        style = strokeStyle
    )
}

private fun DrawScope.drawGrid(color: Color, strokeStyle: Stroke) {
    val r = CornerRadius(1.2f)
    drawRoundRect(color = color, topLeft = Offset(4f, 4f), size = Size(5.2f, 5.2f), cornerRadius = r, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(10.8f, 4f), size = Size(5.2f, 5.2f), cornerRadius = r, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(4f, 10.8f), size = Size(5.2f, 5.2f), cornerRadius = r, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(10.8f, 10.8f), size = Size(5.2f, 5.2f), cornerRadius = r, style = strokeStyle)
}

private fun DrawScope.drawDashboard(color: Color, strokeStyle: Stroke) {
    val r = CornerRadius(1.2f)
    drawRoundRect(color = color, topLeft = Offset(4f, 4f), size = Size(6f, 7.4f), cornerRadius = r, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(11.6f, 4f), size = Size(4.4f, 4.4f), cornerRadius = r, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(11.6f, 10f), size = Size(4.4f, 7f), cornerRadius = r, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(4f, 13f), size = Size(6f, 4f), cornerRadius = r, style = strokeStyle)
}

private fun DrawScope.drawViewAgenda(color: Color, strokeStyle: Stroke) {
    val r = CornerRadius(1.4f)
    drawRoundRect(color = color, topLeft = Offset(4f, 4f), size = Size(12f, 4.6f), cornerRadius = r, style = strokeStyle)
    drawRoundRect(color = color, topLeft = Offset(4f, 11.4f), size = Size(12f, 4.6f), cornerRadius = r, style = strokeStyle)
}

private fun DrawScope.drawApps(color: Color) {
    listOf(4.6f, 10f, 15.4f).forEach { x ->
        listOf(4.6f, 10f, 15.4f).forEach { y ->
            drawCircle(color = color, radius = 1.15f, center = Offset(x, y))
        }
    }
}

// ── 主题/设备 ─────────────────────────────────────────────────────────

private fun DrawScope.drawSun(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 3.6f, center = Offset(10f, 10f), style = strokeStyle)
    for (i in 0 until 8) {
        val rad = Math.toRadians((i * 45).toDouble())
        val cosV = cos(rad).toFloat()
        val sinV = sin(rad).toFloat()
        drawLine(
            color = color,
            start = Offset(10f + 5.4f * cosV, 10f + 5.4f * sinV),
            end = Offset(10f + 7.2f * cosV, 10f + 7.2f * sinV),
            strokeWidth = 1.3f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawMoon(color: Color, strokeStyle: Stroke) {
    drawPath(
        path = Path().apply {
            moveTo(12.2f, 3.8f)
            cubicTo(6.9f, 5.1f, 4.7f, 9.3f, 6.9f, 13.1f)
            cubicTo(8f, 15.1f, 10.2f, 16.3f, 12.7f, 16.2f)
            cubicTo(9.4f, 14.6f, 8.4f, 11.4f, 9.7f, 8.5f)
            cubicTo(10.4f, 6.7f, 11.5f, 5.1f, 12.2f, 3.8f)
            close()
        },
        color = color,
        style = strokeStyle
    )
}

private fun DrawScope.drawContrast(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 6.5f, center = Offset(10f, 10f), style = strokeStyle)
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(3.5f, 3.5f),
        size = Size(13f, 13f)
    )
}

private fun DrawScope.drawPhone(color: Color, strokeStyle: Stroke) {
    drawRoundRect(
        color = color,
        topLeft = Offset(5.5f, 3f),
        size = Size(9f, 14f),
        cornerRadius = CornerRadius(2f),
        style = strokeStyle
    )
    drawLine(color, Offset(8.4f, 5.2f), Offset(11.6f, 5.2f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    drawCircle(color = color, radius = 0.9f, center = Offset(10f, 14.5f))
}

private fun DrawScope.drawVibrate(color: Color, strokeStyle: Stroke) {
    drawRoundRect(
        color = color,
        topLeft = Offset(6.5f, 5.5f),
        size = Size(7f, 9f),
        cornerRadius = CornerRadius(1.5f),
        style = strokeStyle
    )
    drawLine(color, Offset(4.4f, 7.2f), Offset(4.4f, 12.8f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(15.6f, 7.2f), Offset(15.6f, 12.8f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(2.6f, 8.6f), Offset(2.6f, 11.4f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawLine(color, Offset(17.4f, 8.6f), Offset(17.4f, 11.4f), strokeWidth = 1.3f, cap = StrokeCap.Round)
}

private fun DrawScope.drawTouch(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 1.8f, center = Offset(10f, 7.6f))
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(6f, 3.6f),
        size = Size(8f, 8f),
        style = strokeStyle
    )
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(3.5f, 1.1f),
        size = Size(13f, 13f),
        style = strokeStyle
    )
}

private fun DrawScope.drawZoom(color: Color, zoomIn: Boolean, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 5.6f, center = Offset(8.8f, 8.8f), style = strokeStyle)
    drawLine(color, Offset(13f, 13f), Offset(16.5f, 16.5f), strokeWidth = 1.4f, cap = StrokeCap.Round)
    drawLine(color, Offset(8.8f, 6.3f), Offset(8.8f, 11.3f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    if (zoomIn) {
        drawLine(color, Offset(6.3f, 8.8f), Offset(11.3f, 8.8f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawFit(color: Color, strokeStyle: Stroke) {
    drawRoundRect(
        color = color.copy(alpha = 0.55f),
        topLeft = Offset(3.5f, 3.5f),
        size = Size(13f, 13f),
        cornerRadius = CornerRadius(1.8f),
        style = strokeStyle
    )
    drawLine(color, Offset(8.2f, 11.8f), Offset(11.8f, 8.2f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    drawPath(path = decoPath(8.2f to 9.6f, 8.2f to 11.8f, 10.4f to 11.8f), color = color, style = strokeStyle)
    drawPath(path = decoPath(11.8f to 10.4f, 11.8f to 8.2f, 9.6f to 8.2f), color = color, style = strokeStyle)
}

// ── 说明 ──────────────────────────────────────────────────────────────

private fun DrawScope.drawInfo(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 7f, center = Offset(10f, 10f), style = strokeStyle)
    drawCircle(color = color, radius = 1.1f, center = Offset(10f, 6.4f))
    drawLine(color, Offset(10f, 9.4f), Offset(10f, 14f), strokeWidth = 1.4f, cap = StrokeCap.Round)
}

private fun DrawScope.drawHelp(color: Color, strokeStyle: Stroke) {
    drawCircle(color = color, radius = 7f, center = Offset(10f, 10f), style = strokeStyle)
    drawArc(
        color = color,
        startAngle = -40f,
        sweepAngle = -260f,
        useCenter = false,
        topLeft = Offset(7.4f, 5.8f),
        size = Size(5.2f, 5.2f),
        style = strokeStyle
    )
    drawCircle(color = color, radius = 1.1f, center = Offset(10f, 13.8f))
}
