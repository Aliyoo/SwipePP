package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** 底部导航四签的画报图标语汇（与 TopLevelTab 路由一一对应）。 */
enum class DecoTabIconKind { ALBUM, GALLERY, FOOTPRINT, VAULT }

/**
 * 画报导航图标：20dp 画布、1.5dp 细线描边（圆头圆角），
 * 语汇取自摩登画报——叠放画片（影集）、金框画廊（画廊）、山河折线（足迹）、
 * 朱红篓盖方篓（回收站）。选中态由 tint 换色表达，图标本身不变形。
 */
@Composable
fun DecoTabIcon(
    kind: DecoTabIconKind,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp
) {
    Canvas(modifier = modifier.size(iconSize)) {
        // 20 单位设计稿等比映射到实际像素（此前坐标被当作 px，图标只有 1/3 大）
        val unit = size.width / 20f
        withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
        val strokeStyle = Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            DecoTabIconKind.ALBUM -> drawAlbum(color, selected, strokeStyle)
            DecoTabIconKind.GALLERY -> drawGallery(color, selected, strokeStyle)
            DecoTabIconKind.FOOTPRINT -> drawFootprint(color, selected, strokeStyle)
            DecoTabIconKind.VAULT -> drawVault(color, selected, strokeStyle)
        }
        }
    }
}

/** 影集：错位叠放的双画片 + 内框印点（画报画片语法） */
private fun DrawScope.drawAlbum(color: Color, selected: Boolean, strokeStyle: Stroke) {
    val r = CornerRadius(1.5f)
    // 底层画片（错位）
    drawRoundRect(
        color = color.copy(alpha = if (selected) 0.55f else 0.4f),
        topLeft = Offset(4f, 3f),
        size = Size(13f, 13f),
        cornerRadius = r,
        style = strokeStyle
    )
    // 顶层画片
    drawRoundRect(
        color = color,
        topLeft = Offset(3f, 6f),
        size = Size(13f, 12f),
        cornerRadius = r,
        style = strokeStyle
    )
    // 内框印点：仅选中填实（方章语汇）
    if (selected) {
        drawRoundRect(color = color, topLeft = Offset(6f, 9f), size = Size(3f, 3f), cornerRadius = r)
    }
}

/** 画廊：金双框画片 + 山折线 + 画报日轮（选中日轮填实） */
private fun DrawScope.drawGallery(color: Color, selected: Boolean, strokeStyle: Stroke) {
    val r = CornerRadius(1.5f)
    drawRoundRect(color = color, topLeft = Offset(2f, 3f), size = Size(16f, 14f), cornerRadius = r, style = strokeStyle)
    // 山折线
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(5f, 13.5f)
            lineTo(8.5f, 9.5f)
            lineTo(11f, 12f)
            lineTo(13f, 10f)
            lineTo(15.5f, 13.5f)
        },
        color = color,
        style = strokeStyle
    )
    // 日轮：仅选中填实
    if (selected) {
        drawCircle(color = color, radius = 1.6f, center = Offset(13.5f, 6.8f))
    }
}

/** 足迹：山河折线两重峰 + 途经印点（足迹山河图语汇） */
private fun DrawScope.drawFootprint(color: Color, selected: Boolean, strokeStyle: Stroke) {
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(2f, 15f)
            lineTo(6.5f, 8.5f)
            lineTo(10f, 12f)
            lineTo(13.5f, 5.5f)
            lineTo(18f, 13f)
        },
        color = color,
        style = strokeStyle
    )
    // 途经印点：当前峰（选中填实）
    if (selected) {
        drawCircle(color = color, radius = 1.7f, center = Offset(13.5f, 5.5f))
    } else {
        drawCircle(color = color, radius = 1.7f, center = Offset(13.5f, 5.5f), style = strokeStyle)
    }
    // 地平线（细弱）
    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(2f, 17.5f),
        end = Offset(18f, 17.5f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
}

/** 回收站：方篓 + 篓盖双线 + 竖篾纹（方章化语汇） */
private fun DrawScope.drawVault(color: Color, selected: Boolean, strokeStyle: Stroke) {
    val r = CornerRadius(1.5f)
    // 篓盖横线
    drawLine(color = color, start = Offset(3.5f, 5.5f), end = Offset(16.5f, 5.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    // 提手
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(8f, 5.5f)
            lineTo(8f, 3.5f)
            lineTo(12f, 3.5f)
            lineTo(12f, 5.5f)
        },
        color = color,
        style = strokeStyle
    )
    // 方篓身
    drawRoundRect(
        color = color,
        topLeft = Offset(4.5f, 7.5f),
        size = Size(11f, 10f),
        cornerRadius = r,
        style = strokeStyle
    )
    // 竖篾纹（选中双纹填点，未选单纹）
    if (selected) {
        drawLine(color = color, start = Offset(8f, 10f), end = Offset(8f, 14.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(12f, 10f), end = Offset(12f, 14.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    } else {
        drawLine(color = color, start = Offset(10f, 10f), end = Offset(10f, 14.5f), strokeWidth = 1.3f, cap = StrokeCap.Round)
    }
}
