package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.sensor.rememberHapticEngine
import com.example.ui.FlipYearUiState
import com.example.ui.LocalFloatingDockInset
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.daysRemainingInVault
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.StudioCanvasBottom
import com.example.ui.theme.StudioCanvasTop
import com.example.ui.theme.SurfacePure

@Composable
fun TrashVaultScreen(
    uiState: FlipYearUiState,
    onRestorePhoto: (PhotoItemEntity) -> Unit,
    onRestoreAll: () -> Unit,
    onConfirmPurgeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPurgeDialog by remember { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    val haptics = rememberHapticEngine()
    val trashed = uiState.trashedPhotos
    val floatingDockInset = LocalFloatingDockInset.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    ArtisticGalleryBackdrop(
        warmAccent = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "清理回收站",
                    style = MaterialTheme.typography.headlineLarge,
                    color = InkPrimary
                )
                Text(
                    text = stringResource(R.string.vault_retention_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }

            // Reclaimable Storage Frosted Glass Summary Card
            FrostedGlassCard(
                shape = RoundedCornerShape(22.dp),
                borderAccent = if (trashed.isNotEmpty()) CoralDanger else null,
                shadowElevation = 6.dp,
                enableAmbientBlur = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "待释放空间",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary
                            )
                            Text(
                                text = formatStorageSize(uiState.pendingTrashBytes),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                color = if (trashed.isNotEmpty()) CoralDanger else EmeraldKeep
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "历史总计释放",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkTertiary
                            )
                            Text(
                                text = formatStorageSize(uiState.stats.totalPurgedBytes),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CobaltPrimary
                            )
                            Text(
                                text = "已清理 ${uiState.stats.totalPurgedCount} 项",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldKeep
                            )
                        }
                    }

                    if (trashed.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GalleryActionButton(
                                text = stringResource(R.string.action_restore_all),
                                onClick = {
                                    haptics.undoPulse()
                                    onRestoreAll()
                                },
                                tone = GalleryButtonTone.GLASS,
                                leadingIcon = Icons.Filled.Restore,
                                height = 46.dp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vault_restore_all_button")
                            )

                            GalleryActionButton(
                                text = "确认清理 (${trashed.size})",
                                onClick = { showPurgeDialog = true },
                                tone = GalleryButtonTone.DANGER,
                                leadingIcon = Icons.Filled.DeleteForever,
                                height = 46.dp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .testTag("vault_purge_button")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (trashed.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 32.dp + floatingDockInset + navBarBottom),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldKeep,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "回收站很干净",
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "在弧形时光轴中上滑清理的照片和视频会先暂存在这里，您可以随时还原或一键释放空间。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 6.dp,
                        bottom = 20.dp + floatingDockInset + navBarBottom
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vault_photo_grid")
                ) {
                    items(trashed, key = { it.id }) { photo ->
                        ModernVaultMediaCard(
                            photo = photo,
                            onPreview = { lightboxPhoto = photo },
                            onRestore = {
                                haptics.undoPulse()
                                onRestorePhoto(photo)
                            },
                            modifier = Modifier.animateItem(
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    lightboxPhoto?.let { photo ->
        PhotoDetailLightbox(
            photo = photo,
            onDismiss = { lightboxPhoto = null },
            onRestore = onRestorePhoto
        )
    }

    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            containerColor = SurfacePure,
            icon = {
                Icon(
                    imageVector = Icons.Filled.CleaningServices,
                    contentDescription = null,
                    tint = CoralDanger
                )
            },
            title = {
                Text(
                    text = "确认释放 ${formatStorageSize(uiState.pendingTrashBytes)}？",
                    color = InkPrimary
                )
            },
            text = {
                Text(
                    text = "即将清理这 ${trashed.size} 项照片与视频（系统将请求将真实本地文件移入 Android 系统回收站，30 天内仍可在系统相册恢复）。",
                    color = InkSecondary
                )
            },
            confirmButton = {
                GalleryActionButton(
                    text = stringResource(R.string.action_purge_confirm),
                    onClick = {
                        haptics.swipeDeleteConfirm()
                        showPurgeDialog = false
                        onConfirmPurgeAll()
                    },
                    tone = GalleryButtonTone.DANGER,
                    height = 42.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("vault_dialog_confirm_purge")
                )
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("取消", color = InkSecondary)
                }
            }
        )
    }
}

@Composable
private fun ModernVaultMediaCard(
    photo: PhotoItemEntity,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfacePure,
        border = BorderStroke(1.dp, HairlineBorder),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.95f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .clickable(onClick = onPreview)
            ) {
                PhotoArtwork(
                    photo = photo,
                    contentDescription = photo.caption,
                    downsampleTier = ImageDownsampleTier.GRID_THUMB,
                    targetSizePx = 288,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = CoralSoft.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "剩 ${daysRemainingInVault(photo.trashedAtMillis)} 天",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CoralDanger,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = formatStorageSize(photo.fileSizeBytes),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${photo.year}.${photo.month}.${photo.day}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTertiary
                    )
                    Text(
                        text = photo.caption.ifBlank { if (photo.isVideo) "待清理视频" else "待清理照片" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onRestore,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("restore_photo_${photo.id}")
                ) {
                    Text(
                        text = stringResource(R.string.action_restore),
                        color = CobaltPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
