package com.example.ui.screens.cleaner

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CleanerEntrySource
import com.example.ui.components.CleanedCompletionBadge
import com.example.ui.components.DecoIcon
import com.example.ui.components.DecoIconKind
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.formatStorageSize
import com.example.ui.components.monthChineseName
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVermInk
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette

// ---------------------------------------------------------------------------
// 摩登画报 · 屏内一次性取色与共享助手（design-demos/deco-app.html 取材）
// ---------------------------------------------------------------------------

// 朱红警示字改走 theme/Color.kt 令牌 DecoVermInk（原型 THEMES.vermInk：暗夜暗房 #F0705F / 日光画廊 #A02A1E）。

/**
 * 朱红通栏动作条（原型 VermStrip 变体）：朱红实底 + 纸黄字（主 20sp / 徽记 13sp），
 * 画片圆角 3dp，顶部 1dp 纸黄弱光。决定性动作共用。
 */
@Composable
internal fun DecoVermStripButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    leadingIconKind: DecoIconKind? = null,
    height: Dp = 52.dp,
    enabled: Boolean = true,
    // 宽通栏（完成页/连拍主 CTA）保持 20sp/2sp 画报标题字；
    // 三等分次级底栏（连拍「整组舍弃」）需收窄字号避免 4 字截断并与邻钮字号齐平
    titleFontSize: TextUnit = 20.sp,
    titleLetterSpacing: TextUnit = 2.sp
) {
    val palette = LocalFlipYearPalette.current
    val gold = palette.decoGold
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "deco_verm_strip_scale"
    )

    Surface(
        shape = RoundedCornerShape(3.dp),
        color = palette.decoVerm,
        border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.22f)),
        shadowElevation = 8.dp,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(3.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = DecoPaperCream),
                enabled = enabled,
                onClick = onClick
            )
            .drawBehind {
                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f))))
                drawLine(
                    color = DecoPaperCream.copy(alpha = 0.22f),
                    start = Offset(0f, 0.8.dp.toPx()),
                    end = Offset(size.width, 0.8.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (leadingIconKind != null) {
                DecoIcon(
                    kind = leadingIconKind,
                    color = DecoPaperCream,
                    iconSize = 24.dp
                )
            }
            Text(
                text = title,
                fontSize = titleFontSize,
                fontFamily = DecoDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                color = DecoPaperCream,
                letterSpacing = titleLetterSpacing,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = DecoPaperCream.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, gold.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = badgeText,
                        // 徽记含汉字（「34 项」）走画报美术字；纯数字体积（「1.2 GB」）保留 mono 遥测语法
                        fontFamily = if (badgeText.any { it.code in 0x4E00..0x9FFF }) DecoDisplayFontFamily else JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = DecoPaperCream,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CleanupCompletedCelebration(
    title: String,
    monthNumber: Int?,
    completedDay: Int?,
    remainingInFullQueue: Int,
    sessionKeptCount: Int,
    sessionFavoritedCount: Int,
    sessionTrashedCount: Int,
    sessionTrashedBytes: Long,
    entrySource: CleanerEntrySource,
    onContinueFullMonth: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenMonthlyTreasures: (() -> Unit)? = null,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSingleDayFinishedWithMoreInMonth = completedDay != null && remainingInFullQueue > 0
    val ordinaryKeptCount = (sessionKeptCount - sessionFavoritedCount).coerceAtLeast(0)
    val palette = LocalFlipYearPalette.current
    val decoGold = palette.decoGold
    val decoMat = palette.decoMat
    val inkOnGround = palette.decoInkOnGround
    val vermInk = DecoVermInk

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("cleanup_completion_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CleanedCompletionBadge(
            month = monthNumber ?: 12,
            freedBytes = sessionTrashedBytes,
            animateEntry = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = when {
                isSingleDayFinishedWithMoreInMonth && monthNumber != null ->
                    "${monthChineseName(monthNumber)} ${completedDay}日已整理完成"
                monthNumber != null ->
                    "${monthChineseName(monthNumber)}整理完成"
                else -> "$title 完成"
            },
            fontSize = 22.sp,
            fontFamily = DecoDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            color = inkOnGround,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 金框账册：三栏互斥遥测（普通保留 / 珍藏 / 舍弃）
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = decoMat,
            border = BorderStroke(1.dp, decoGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag("completion_session_stats")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompletionMetricSlot(
                    label = "普通保留",
                    value = "$ordinaryKeptCount",
                    subLabel = "已入册",
                    accent = decoGold,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.height(34.dp),
                    thickness = 1.dp,
                    color = decoGold.copy(alpha = 0.4f)
                )
                CompletionMetricSlot(
                    label = "珍藏",
                    value = "$sessionFavoritedCount",
                    subLabel = "精选资产",
                    accent = decoGold,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.height(34.dp),
                    thickness = 1.dp,
                    color = decoGold.copy(alpha = 0.4f)
                )
                CompletionMetricSlot(
                    label = "舍弃",
                    value = formatStorageSize(sessionTrashedBytes),
                    subLabel = "$sessionTrashedCount 项待清",
                    accent = vermInk,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isSingleDayFinishedWithMoreInMonth) {
                "当天媒体已全部处理完毕，本月其余日期还有 $remainingInFullQueue 项待整理。"
            } else if (sessionTrashedCount > 0) {
                "移入回收站的项目尚未从设备删除，前往回收站确认后即可释放空间。"
            } else {
                "本次整理的照片和视频均已妥善保留在影集中。"
            },
            fontSize = 14.sp,
            color = inkOnGround.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (sessionFavoritedCount > 0 && onOpenMonthlyTreasures != null) {
            GalleryActionButton(
                text = "查看本月珍藏",
                leadingIconKind = DecoIconKind.FAVORITE,
                iconTint = decoGold,
                onClick = onOpenMonthlyTreasures,
                tone = GalleryButtonTone.IMMERSIVE,
                height = 48.dp,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("completion_open_treasures_button")
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isSingleDayFinishedWithMoreInMonth) {
            DecoVermStripButton(
                title = "继续整理本月剩余",
                badgeText = "$remainingInFullQueue 项",
                leadingIconKind = DecoIconKind.CLEAN,
                onClick = onContinueFullMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continue_full_month_button")
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (sessionTrashedCount > 0) {
                GalleryActionButton(
                    text = "去回收站释放空间 (${formatStorageSize(sessionTrashedBytes)})",
                    leadingIconKind = DecoIconKind.DELETE,
                    iconTint = vermInk,
                    onClick = onOpenVault,
                    tone = GalleryButtonTone.IMMERSIVE,
                    height = 48.dp,
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_open_vault_button")
                )
                Spacer(modifier = Modifier.height(4.dp))
                DecoGoldTextLink(
                    text = entrySource.backLabel,
                    gold = decoGold,
                    onClick = onDone,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("cleanup_done_button")
                )
            } else {
                GalleryActionButton(
                    text = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.IMMERSIVE,
                    height = 48.dp,
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            }
        } else {
            if (sessionTrashedCount > 0) {
                DecoVermStripButton(
                    title = "去回收站释放空间",
                    badgeText = formatStorageSize(sessionTrashedBytes),
                    leadingIconKind = DecoIconKind.DELETE,
                    onClick = onOpenVault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_open_vault_button")
                )
                Spacer(modifier = Modifier.height(10.dp))
                GalleryActionButton(
                    text = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.IMMERSIVE,
                    height = 48.dp,
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            } else {
                DecoVermStripButton(
                    title = entrySource.backLabel,
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            }
        }
    }
}

/** 金色下划线文字入口（画报弱动作，呼应原型「撤销上一步」金下划线） */
@Composable
private fun DecoGoldTextLink(
    text: String,
    gold: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = DecoDisplayFontFamily,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            color = gold,
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
private fun CompletionMetricSlot(
    label: String,
    value: String,
    subLabel: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val inkOnGround = DecoInkOnGround
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = DecoDisplayFontFamily,
            color = inkOnGround.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            fontSize = 12.sp,
            fontFamily = DecoDisplayFontFamily,
            color = inkOnGround.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
