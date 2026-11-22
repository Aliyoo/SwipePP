package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 底部导航四签旧名入口：枚举与绘制已并入 [DecoIconKind] / [DecoIcon]
 * （同目录 DecoIcons 画报图标全集），本文件仅为既有调用点保留 typealias 与委托。
 */
/** 与 [DecoIcon] 同参的旧名入口（底部导航四签沿用）。 */
@Composable
fun DecoTabIcon(
    kind: DecoTabIconKind,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    iconSize: Dp = 20.dp
) {
    DecoIcon(kind = kind, color = color, modifier = modifier, selected = selected, iconSize = iconSize)
}
