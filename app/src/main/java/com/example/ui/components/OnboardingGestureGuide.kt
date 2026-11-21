package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwipeVertical
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure

private data class GestureGuideRow(
    val icon: ImageVector,
    val tint: Color,
    val containerColor: Color,
    val title: String,
    val detail: String
)

/**
 * Stage 4: One-time skippable gesture guide introducing core FlipYear interactions:
 * swipe up (trash), swipe down (keep), shake to undo (in cleaner session),
 * long-press multi-select, and fast timeline scrollbar.
 */
@Composable
fun OnboardingGestureGuideOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val coralDanger = CoralDanger
    val coralSoft = CoralSoft
    val emeraldKeep = EmeraldKeep
    val emeraldSoft = EmeraldSoft
    val cobaltPrimary = CobaltPrimary
    val cobaltSoft = CobaltSoft
    val solarAmber = SolarAmber
    val solarAmberSoft = SolarAmberSoft
    val items = remember(isDark) {
        listOf(
            GestureGuideRow(
                icon = Icons.Filled.ArrowUpward,
                tint = coralDanger,
                containerColor = coralSoft,
                title = "上滑 · 移入回收站",
                detail = "在弧形清理中向上滑动，将不需要的照片或视频先暂存到回收站，不会立刻删除。"
            ),
            GestureGuideRow(
                icon = Icons.Filled.ArrowDownward,
                tint = emeraldKeep,
                containerColor = emeraldSoft,
                title = "下滑 · 保留（连拍全部保留）",
                detail = "向下滑动直接保留当前照片或整组连拍；如需仅留最佳帧，可点底部“仅留最佳”。"
            ),
            GestureGuideRow(
                icon = Icons.Filled.Vibration,
                tint = cobaltPrimary,
                containerColor = cobaltSoft,
                title = "摇一摇 · 撤销清理",
                detail = "在清理页面误操作时轻摇手机，即可撤销本次清理会话内的上一步决定。"
            ),
            GestureGuideRow(
                icon = Icons.Filled.TouchApp,
                tint = solarAmber,
                containerColor = solarAmberSoft,
                title = "长按 · 多选批量整理",
                detail = "在画廊中长按任意照片进入多选模式，可批量保留、收藏、移入回收站或压缩。"
            ),
            GestureGuideRow(
                icon = Icons.Filled.SwipeVertical,
                tint = cobaltPrimary,
                containerColor = cobaltSoft,
                title = "右侧快速滚动条 · 飞跃月份",
                detail = "拖动画廊右侧的时间滑块，快速跨年跨月定位照片。"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isDark) 0.78f else 0.56f))
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
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfacePure,
            border = BorderStroke(1.dp, HairlineBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
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
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = "所有删除都先进入安全回收站，随时可撤销或还原",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("onboarding_skip_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "跳过引导",
                            tint = InkSecondary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { row ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceMuted.copy(alpha = 0.55f),
                            border = BorderStroke(0.8.dp, HairlineBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = row.containerColor,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = row.icon,
                                            contentDescription = null,
                                            tint = row.tint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = row.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = row.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
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
                    GalleryActionButton(
                        text = "跳过",
                        tone = GalleryButtonTone.GLASS,
                        height = 48.dp,
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.36f)
                            .testTag("onboarding_skip_button")
                    )
                    GalleryActionButton(
                        text = "知道了，开始整理",
                        leadingIcon = Icons.Filled.CheckCircle,
                        tone = GalleryButtonTone.OBSIDIAN,
                        height = 48.dp,
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.64f)
                            .testTag("onboarding_start_button")
                    )
                }
            }
        }
    }
}
