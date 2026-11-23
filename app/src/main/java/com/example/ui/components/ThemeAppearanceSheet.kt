package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DarkFlipYearPalette
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkDeep
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LightFlipYearPalette
import com.example.ui.theme.LocalFlipYearPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeAppearanceSheet(
    currentMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LocalFlipYearPalette.current.decoGround,
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
        dragHandle = { DecoSheetDragHandle() },
        modifier = Modifier.modalSheetTopClearance()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp)
                .testTag("theme_appearance_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.theme_appearance_title),
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 1.2.sp,
                    color = InkPrimary
                )
                Text(
                    text = stringResource(R.string.theme_appearance_subtitle),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = InkSecondary
                )
            }

            ThemePreviewCardsRow(
                currentMode = currentMode,
                onSelectMode = { mode ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectMode(mode)
                }
            )
        }
    }
}

@Composable
internal fun ThemePreviewCardsRow(
    currentMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThemePreviewCard(
            mode = AppThemeMode.SYSTEM,
            iconKind = DecoIconKind.CONTRAST,
            selected = currentMode == AppThemeMode.SYSTEM,
            onClick = { onSelectMode(AppThemeMode.SYSTEM) },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_option_system")
        )
        ThemePreviewCard(
            mode = AppThemeMode.LIGHT,
            iconKind = DecoIconKind.SUN,
            selected = currentMode == AppThemeMode.LIGHT,
            onClick = { onSelectMode(AppThemeMode.LIGHT) },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_option_light")
        )
        ThemePreviewCard(
            mode = AppThemeMode.DARK,
            iconKind = DecoIconKind.MOON,
            selected = currentMode == AppThemeMode.DARK,
            onClick = { onSelectMode(AppThemeMode.DARK) },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_option_dark")
        )
    }
}

/**
 * 摩登画报主题选择卡：金双线画框（外 1.5dp 金 + 3dp 纸底内衬 + 1dp 弱金），
 * 选中态加金底微染 + 朱点印；不用蓝色 Border。
 */
@Composable
private fun ThemePreviewCard(
    mode: AppThemeMode,
    iconKind: DecoIconKind,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = DecoGold
    val mat = LocalFlipYearPalette.current.decoMat

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) gold.copy(alpha = 0.10f) else mat.copy(alpha = 0.55f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) gold else gold.copy(alpha = 0.45f),
                shape = RoundedCornerShape(3.dp)
            )
            .clickable(onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // 金双线画框：外 1.5dp 金线由卡片描边承担，内衬 1dp 弱金框住微型画片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f)
                .clip(RoundedCornerShape(2.dp))
                .border(
                    1.dp,
                    gold.copy(alpha = if (selected) 0.42f else 0.28f),
                    RoundedCornerShape(2.dp)
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val light = LightFlipYearPalette
                val dark = DarkFlipYearPalette

                when (mode) {
                    AppThemeMode.LIGHT -> {
                        // 日光画廊纸地：decoGround → decoGroundDeep
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(light.decoGround, light.decoGroundDeep)
                            )
                        )
                        // Mini 画报题头卡（纸黄内衬 + 金发丝底线）
                        drawRoundRect(
                            color = light.decoMat,
                            topLeft = Offset(w * 0.12f, h * 0.14f),
                            size = Size(w * 0.76f, h * 0.22f),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawLine(
                            color = light.decoGold,
                            start = Offset(w * 0.12f, h * 0.38f),
                            end = Offset(w * 0.88f, h * 0.38f),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Mini 画报书封（深墨封 + 金描边 + 朱印块）
                        drawRoundRect(
                            color = DecoInkDeep,
                            topLeft = Offset(w * 0.24f, h * 0.44f),
                            size = Size(w * 0.52f, h * 0.36f),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = light.decoGold,
                            topLeft = Offset(w * 0.30f, h * 0.50f),
                            size = Size(w * 0.40f, h * 0.01f)
                        )
                        drawRoundRect(
                            color = light.decoVerm,
                            topLeft = Offset(w * 0.30f, h * 0.56f),
                            size = Size(w * 0.18f, h * 0.09f),
                            cornerRadius = CornerRadius(1.dp.toPx())
                        )
                        // Mini 底栏（纸黄 + 金描边顶缘）
                        drawRoundRect(
                            color = light.decoMat,
                            topLeft = Offset(w * 0.14f, h * 0.85f),
                            size = Size(w * 0.72f, h * 0.09f),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawLine(
                            color = light.decoGold,
                            start = Offset(w * 0.14f, h * 0.85f),
                            end = Offset(w * 0.86f, h * 0.85f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    AppThemeMode.DARK -> {
                        // 暗夜暗房纸地：decoGround → decoGroundDeep
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(dark.decoGround, dark.decoGroundDeep)
                            )
                        )
                        // 金色暗房天窗余辉
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(dark.decoGold.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(w * 0.75f, h * 0.22f),
                                radius = w * 0.65f
                            ),
                            radius = w * 0.65f,
                            center = Offset(w * 0.75f, h * 0.22f)
                        )
                        // Mini 题头卡（金染纸面）
                        drawRoundRect(
                            color = dark.decoGold.copy(alpha = 0.14f),
                            topLeft = Offset(w * 0.12f, h * 0.14f),
                            size = Size(w * 0.76f, h * 0.22f),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawLine(
                            color = dark.decoGold,
                            start = Offset(w * 0.12f, h * 0.38f),
                            end = Offset(w * 0.88f, h * 0.38f),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Mini 画报书封（深墨封 + 金描边 + 朱印块）
                        drawRoundRect(
                            color = DecoInkDeep,
                            topLeft = Offset(w * 0.24f, h * 0.44f),
                            size = Size(w * 0.52f, h * 0.36f),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = dark.decoGold,
                            topLeft = Offset(w * 0.27f, h * 0.47f),
                            size = Size(w * 0.46f, h * 0.30f),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawRoundRect(
                            color = dark.decoVerm,
                            topLeft = Offset(w * 0.30f, h * 0.56f),
                            size = Size(w * 0.18f, h * 0.09f),
                            cornerRadius = CornerRadius(1.dp.toPx())
                        )
                        // Mini 底栏（深地 + 金描边顶缘）
                        drawRoundRect(
                            color = dark.decoGroundDeep,
                            topLeft = Offset(w * 0.14f, h * 0.85f),
                            size = Size(w * 0.72f, h * 0.09f),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawLine(
                            color = dark.decoGold,
                            start = Offset(w * 0.14f, h * 0.85f),
                            end = Offset(w * 0.86f, h * 0.85f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    AppThemeMode.SYSTEM -> {
                        // 左上日光画廊纸地 / 右下暗夜暗房纸地对角切分
                        drawRect(color = light.decoGround)
                        val darkHalf = Path().apply {
                            moveTo(w, 0f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(path = darkHalf, color = dark.decoGround)

                        // 骑缝画报书封（深墨封 + 金描边 + 朱印块）
                        drawRoundRect(
                            color = DecoInkDeep,
                            topLeft = Offset(w * 0.24f, h * 0.30f),
                            size = Size(w * 0.52f, h * 0.44f),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = dark.decoGold,
                            topLeft = Offset(w * 0.28f, h * 0.34f),
                            size = Size(w * 0.44f, h * 0.36f),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawRoundRect(
                            color = dark.decoVerm,
                            topLeft = Offset(w * 0.34f, h * 0.52f),
                            size = Size(w * 0.24f, h * 0.10f),
                            cornerRadius = CornerRadius(1.dp.toPx())
                        )
                    }
                }
            }

            // 选中朱点印（纸黄对勾压朱红底 —— 可读性红线允许的组合）
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(DecoVerm),
                    contentAlignment = Alignment.Center
                ) {
                    DecoIcon(
                        kind = DecoIconKind.CHECK,
                        color = DecoPaperCream,
                        iconSize = 11.dp
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            DecoIcon(
                kind = iconKind,
                color = if (selected) DecoGold else InkSecondary,
                iconSize = 14.dp
            )
            Text(
                text = mode.label,
                fontFamily = DecoDisplayFontFamily,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = if (selected) DecoGold else InkPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
