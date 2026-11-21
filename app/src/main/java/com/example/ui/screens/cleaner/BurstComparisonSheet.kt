package com.example.ui.screens.cleaner

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.CleanupCardItem
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.formatShortTime
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette

// 摩登画报 · 屏内一次性色彩（design-demos/deco-app.html 取材）
private val DecoSheetGoldBrush = { gold: Color ->
    Brush.horizontalGradient(
        0f to Color.Transparent,
        0.15f to gold.copy(alpha = 0.67f),
        0.85f to gold.copy(alpha = 0.67f),
        1f to Color.Transparent
    )
}

@Composable
internal fun BurstComparisonSheet(
    stack: CleanupCardItem.BurstStack,
    onConfirmKeepBest: (PhotoItemEntity) -> Unit
) {
    val palette = LocalFlipYearPalette.current
    val decoGold = palette.decoGold
    val decoMat = palette.decoMat
    val decoVerm = palette.decoVerm
    val inkOnGround = palette.decoInkOnGround
    var selectedPhoto by remember(stack) { mutableStateOf(stack.bestPhoto) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "连拍精选对比 · 共 ${stack.photos.size} 张",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = inkOnGround
            )
            // 金色分隔细线（画报小节标尺）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DecoSheetGoldBrush(decoGold))
            )
            Text(
                text = "点击任意帧预览选择，确认后才仅保留选中项并将其他连拍移入回收站。",
                fontSize = 14.sp,
                color = inkOnGround.copy(alpha = 0.66f)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(stack.photos) { idx, photo ->
                val isSelected = photo.id == selectedPhoto.id
                // 金框画片：选中 2.5dp 金描边 + 金晕；未选 1dp 弱金
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = decoMat,
                    border = BorderStroke(
                        if (isSelected) 2.5.dp else 1.dp,
                        if (isSelected) decoGold else decoGold.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .width(166.dp)
                        .clickable {
                            selectedPhoto = photo
                        }
                        .testTag("burst_choice_${idx}")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(5.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.background(decoGold.copy(alpha = 0.08f))
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.82f)
                                .clip(RoundedCornerShape(2.dp))
                                .border(1.dp, decoGold.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                        ) {
                            PhotoArtwork(
                                photo = photo,
                                contentDescription = photo.caption,
                                downsampleTier = ImageDownsampleTier.PREVIEW_CARD,
                                targetSizePx = 384,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                // 朱红实印：选中帧徽记
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = decoVerm,
                                    border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = DecoPaperCream,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "保留",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DecoPaperCream
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "画质 ${photo.qualityScore}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (photo.qualityScore >= 90) decoGold else inkOnGround.copy(alpha = 0.66f)
                            )
                            Text(
                                text = formatShortTime(photo.takenAtMillis),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = inkOnGround.copy(alpha = 0.55f)
                            )
                        }
                        Text(
                            text = photo.caption.ifBlank { "连拍帧 #${idx + 1}" },
                            fontSize = 12.sp,
                            color = inkOnGround,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val freedIfConfirmed = stack.photos.filter { it.id != selectedPhoto.id }.sumOf { it.fileSizeBytes }
        // 朱红通栏确认条（主 20sp 纸黄字，画片圆角 3dp）
        DecoVermStripButton(
            title = "确认仅保留此张",
            badgeText = "移走 ${stack.photos.size - 1} 张 · ${formatStorageSize(freedIfConfirmed)}",
            leadingIcon = Icons.Filled.CheckCircle,
            onClick = { onConfirmKeepBest(selectedPhoto) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_burst_selection_button")
        )
    }
}
