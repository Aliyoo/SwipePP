package com.example.ui.screens.cleaner

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.CleanupCardItem
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.formatShortTime
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianOverlayBorder
import com.example.ui.theme.ObsidianOverlayGlass
import com.example.ui.theme.SurfaceMuted

@Composable
internal fun BurstComparisonSheet(
    stack: CleanupCardItem.BurstStack,
    onConfirmKeepBest: (PhotoItemEntity) -> Unit,
    onInspectPhoto: (PhotoItemEntity) -> Unit = {}
) {
    var selectedPhoto by remember(stack) { mutableStateOf(stack.bestPhoto) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "连拍精选对比 · 共 ${stack.photos.size} 张",
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary
                )
                Text(
                    text = "轻触切换保留帧，再次点击或点放大钮查看原图细节，确认后仅保留选中项。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = { onInspectPhoto(selectedPhoto) },
                shape = RoundedCornerShape(50),
                color = CobaltSoft,
                border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.testTag("burst_inspect_selected_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = "查看原图详情",
                        tint = CobaltPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "看大图",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CobaltPrimary
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(stack.photos, key = { _, p -> p.id }) { idx, photo ->
                val isSelected = photo.id == selectedPhoto.id
                val borderWidth by animateFloatAsState(if (isSelected) 2.5f else 1f, label = "burst_sel")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) CobaltSoft else SurfaceMuted,
                    border = BorderStroke(
                        borderWidth.dp,
                        if (isSelected) CobaltPrimary else HairlineBorder
                    ),
                    modifier = Modifier
                        .width(166.dp)
                        .clickable {
                            if (isSelected) {
                                onInspectPhoto(photo)
                            } else {
                                selectedPhoto = photo
                            }
                        }
                        .testTag("burst_choice_${idx}")
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.82f)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            PhotoArtwork(
                                photo = photo,
                                contentDescription = photo.caption,
                                downsampleTier = ImageDownsampleTier.PREVIEW_CARD,
                                targetSizePx = 384,
                                modifier = Modifier.fillMaxSize()
                            )

                            // One-tap full-screen detail inspect orb on every burst frame
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(36.dp)
                                    .clickable {
                                        selectedPhoto = photo
                                        onInspectPhoto(photo)
                                    }
                                    .padding(6.dp)
                                    .testTag("burst_inspect_detail_${idx}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(ObsidianOverlayGlass)
                                        .border(0.8.dp, ObsidianOverlayBorder, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInFull,
                                        contentDescription = "查看第 ${idx + 1} 张详情",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CobaltPrimary,
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
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "保留",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (photo.qualityScore >= 90) CobaltPrimary else InkSecondary
                            )
                            Text(
                                text = formatShortTime(photo.takenAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkTertiary
                            )
                        }
                        Text(
                            text = photo.caption.ifBlank { "连拍帧 #${idx + 1}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val freedIfConfirmed = stack.photos.filter { it.id != selectedPhoto.id }.sumOf { it.fileSizeBytes }
        com.example.ui.components.GallerySculptedHeroButton(
            title = "确认仅保留此张",
            badgeText = "移走 ${stack.photos.size - 1} 张 · ${formatStorageSize(freedIfConfirmed)}",
            onClick = { onConfirmKeepBest(selectedPhoto) },
            tone = GalleryButtonTone.OBSIDIAN,
            leadingIcon = Icons.Filled.CheckCircle,
            height = 52.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_burst_selection_button")
        )
    }
}
