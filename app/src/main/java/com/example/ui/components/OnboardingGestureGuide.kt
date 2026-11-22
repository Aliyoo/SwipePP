package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoVermInk
import com.example.ui.theme.DecoVermShadowInk
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LocalIsDarkTheme

private data class GestureGuideRow(
    val icon: ImageVector,
    /** 小方章描边/印色：回收站动作用朱红，其余为金。 */
    val sealColor: Color,
    val title: String,
    val detail: String
)

/**
 * Stage 4: One-time skippable gesture guide introducing core FlipYear interactions:
 * swipe up (trash), swipe down (keep), shake to undo (in cleaner session),
 * long-press multi-select, and fast timeline scrollbar.
 *
 * 摩登画报造型语法：金双线画框（外 1.5dp 金 + 3dp 纸底内衬 + 1dp 弱金）纸面卡，
 * 手势行图标方章化（金描边小方章 3dp，回收站动作用朱红章），
 * CTA = 朱红通栏（主 20sp 纸黄字）+ 次动作金描边跳签。
 */
@Composable
fun OnboardingGestureGuideOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val vermSeal = DecoVermInk
    val goldSeal = DecoGold
    val items = remember(vermSeal, goldSeal) {
        listOf(
            GestureGuideRow(
                icon = Icons.Outlined.ArrowUpward,
                sealColor = vermSeal,
                title = "上滑 · 移入回收站",
                detail = "在弧形清理中向上滑动，将不需要的照片或视频先暂存到回收站，不会立刻删除。"
            ),
            GestureGuideRow(
                icon = Icons.Outlined.ArrowDownward,
                sealColor = goldSeal,
                title = "下滑 · 保留（连拍全部保留）",
                detail = "向下滑动直接保留当前照片或整组连拍；如需仅留最佳帧，可点底部“仅留最佳”。"
            ),
            GestureGuideRow(
                icon = Icons.Outlined.Vibration,
                sealColor = goldSeal,
                title = "摇一摇 · 撤销清理",
                detail = "在清理页面误操作时轻摇手机，即可撤销本次清理会话内的上一步决定。"
            ),
            GestureGuideRow(
                icon = Icons.Outlined.TouchApp,
                sealColor = goldSeal,
                title = "长按 · 多选批量整理",
                detail = "在画廊中长按任意照片进入多选模式，可批量保留、收藏、移入回收站或压缩。"
            ),
            GestureGuideRow(
                icon = Icons.Outlined.SwipeVertical,
                sealColor = goldSeal,
                title = "右侧快速滚动条 · 飞跃月份",
                detail = "拖动画廊右侧的时间滑块，快速跨年跨月定位照片。"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DecoScrimInk.copy(alpha = if (isDark) 0.82f else 0.62f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("onboarding_gesture_guide"),
        contentAlignment = Alignment.Center
    ) {
        // 画报纸面卡 + 金双线画框（外 1.5dp 金 / 3dp 纸底内衬 / 1dp 弱金）
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = DecoMat,
            border = BorderStroke(1.5.dp, DecoGold),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .border(1.dp, DecoGold.copy(alpha = 0.40f), RoundedCornerShape(2.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 17.dp, vertical = 17.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "快速上手 · 整理手势",
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 1.2.sp,
                                color = InkPrimary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "所有删除都先进入安全回收站，随时可撤销或还原",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = InkSecondary
                            )
                        }
                        // 跳过小方章（金描边 3dp）
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(DecoInkOnGround.copy(alpha = 0.06f))
                                .border(1.5.dp, DecoGold, RoundedCornerShape(3.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                )
                                .testTag("onboarding_skip_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "跳过引导",
                                tint = DecoGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items.forEach { row ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = InkPrimary.copy(alpha = 0.05f),
                                border = BorderStroke(0.8.dp, DecoGold.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 金描边小方章（回收站动作为朱红章）
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(row.sealColor.copy(alpha = 0.10f))
                                            .border(1.dp, row.sealColor.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                                    ) {
                                        Icon(
                                            imageVector = row.icon,
                                            contentDescription = null,
                                            tint = row.sealColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = row.title,
                                            fontFamily = DecoDisplayFontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = InkPrimary
                                        )
                                        Text(
                                            text = row.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 14.sp,
                                            lineHeight = 19.sp,
                                            color = InkSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 次动作：金描边跳签
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(0.30f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .border(1.5.dp, DecoGold, RoundedCornerShape(3.dp))
                                .clickable(onClick = onDismiss)
                                .testTag("onboarding_skip_button")
                        ) {
                            Text(
                                text = "跳过",
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 2.sp,
                                color = DecoGold
                            )
                        }
                        // 主 CTA：朱红通栏（主 20sp 纸黄字，顶缘受光发丝 + 朱红投影墨）
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(0.70f)
                                .height(52.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(3.dp),
                                    ambientColor = DecoVermShadowInk.copy(alpha = 0.38f),
                                    spotColor = DecoVermShadowInk.copy(alpha = 0.45f)
                                )
                                .clip(RoundedCornerShape(3.dp))
                                .background(DecoVerm)
                                .drawBehind {
                                    drawLine(
                                        color = DecoPaperCream.copy(alpha = 0.22f),
                                        start = Offset(0f, 0.75.dp.toPx()),
                                        end = Offset(size.width, 0.75.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                .clickable(onClick = onDismiss)
                                .testTag("onboarding_start_button")
                        ) {
                            Text(
                                text = "知道了，开始整理",
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = 1.5.sp,
                                color = DecoPaperCream,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
