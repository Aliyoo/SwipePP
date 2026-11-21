package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.ui.components.GallerySculptedHeroButton
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.formatStorageSize
import com.example.ui.components.formatVaultStagedLabel
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure

@Composable
fun TrashVaultScreen(
    uiState: FlipYearUiState,
    onRestorePhoto: (PhotoItemEntity) -> Unit,
    onRestoreAll: (List<PhotoItemEntity>) -> Unit,
    onConfirmPurgeTargets: (List<PhotoItemEntity>) -> Unit,
    onClearSessionFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPurgeDialog by remember { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val haptics = rememberHapticEngine()

    val sessionFilterIds = uiState.vaultSessionFilterIds
    val displayedTrashed = remember(uiState.trashedPhotos, sessionFilterIds) {
        if (sessionFilterIds == null) uiState.trashedPhotos
        else uiState.trashedPhotos.filter { it.id in sessionFilterIds }
    }
    LaunchedEffect(displayedTrashed) {
        val validIds = displayedTrashed.map { it.id }.toSet()
        selectedIds = selectedIds.intersect(validIds)
    }

    val targets = remember(displayedTrashed, selectedIds) {
        if (selectedIds.isNotEmpty()) displayedTrashed.filter { it.id in selectedIds } else displayedTrashed
    }
    val displayedBytes = remember(displayedTrashed) { displayedTrashed.sumOf { it.fileSizeBytes } }
    val targetBytes = remember(targets) { targets.sumOf { it.fileSizeBytes } }
    val isPartialSelection = selectedIds.isNotEmpty()
    val allSelected = displayedTrashed.isNotEmpty() && selectedIds.size == displayedTrashed.size

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

            // Session filter banner when arriving from Cleaner completion page
            if (sessionFilterIds != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CobaltSoft,
                    border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "仅看本次清理（${displayedTrashed.size} 项）",
                            style = MaterialTheme.typography.labelLarge,
                            color = CobaltPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onClearSessionFilter,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("vault_clear_session_filter")
                        ) {
                            Text(
                                text = "查看全部 (${uiState.trashedPhotos.size})",
                                color = CobaltPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (displayedTrashed.isEmpty()) {
                // Compact Storage Summary Card (empty state)
                FrostedGlassCard(
                    shape = RoundedCornerShape(22.dp),
                    borderAccent = null,
                    shadowElevation = 4.dp,
                    enableAmbientBlur = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    VaultCompactSummaryContent(
                        isPartialSelection = false,
                        hasItems = false,
                        displayBytes = 0L,
                        targetsCount = 0,
                        totalPurgedBytes = uiState.stats.totalPurgedBytes,
                        totalCompressedSavedBytes = uiState.stats.totalCompressedSavedBytes
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        text = if (sessionFilterIds != null && uiState.trashedPhotos.isNotEmpty()) {
                            "本次清理项已全部处理"
                        } else {
                            "回收站很干净"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (sessionFilterIds != null && uiState.trashedPhotos.isNotEmpty()) {
                            "回收站中还有 ${uiState.trashedPhotos.size} 项历史暂存项目，可点击上方查看全部。"
                        } else {
                            "在清理器中上滑移入回收站的照片和视频会先暂存在这里，您可以随时还原或按需释放空间。"
                        },
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
                        top = 2.dp,
                        bottom = 96.dp + floatingDockInset + navBarBottom
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vault_photo_grid")
                ) {
                    // Slim Focused Pending-Storage Card: gives maximum vertical space to photo inspection
                    item(key = "vault_summary_header", span = { GridItemSpan(maxLineSpan) }) {
                        FrostedGlassCard(
                            shape = RoundedCornerShape(20.dp),
                            borderAccent = CoralDanger,
                            shadowElevation = 4.dp,
                            enableAmbientBlur = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            VaultCompactSummaryContent(
                                isPartialSelection = isPartialSelection,
                                hasItems = true,
                                displayBytes = if (isPartialSelection) targetBytes else displayedBytes,
                                targetsCount = targets.size,
                                totalPurgedBytes = uiState.stats.totalPurgedBytes,
                                totalCompressedSavedBytes = uiState.stats.totalCompressedSavedBytes
                            )
                        }
                    }

                    item(key = "vault_select_bar", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPartialSelection) {
                                    "已勾选 ${selectedIds.size} / ${displayedTrashed.size} 项"
                                } else {
                                    "长按或勾选卡片可批量还原或清理"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary
                            )
                            TextButton(
                                onClick = {
                                    selectedIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        displayedTrashed.map { it.id }.toSet()
                                    }
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("vault_toggle_select_all")
                            ) {
                                Text(
                                    text = if (allSelected) "取消全选" else "全选",
                                    color = CobaltPrimary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    items(displayedTrashed, key = { it.id }) { photo ->
                        val isSelected = photo.id in selectedIds
                        ModernVaultMediaCard(
                            photo = photo,
                            isSelected = isSelected,
                            onToggleSelect = {
                                haptics.bookshelfDetent()
                                selectedIds = if (isSelected) selectedIds - photo.id else selectedIds + photo.id
                            },
                            onPreview = {
                                if (isPartialSelection) {
                                    haptics.bookshelfDetent()
                                    selectedIds = if (isSelected) selectedIds - photo.id else selectedIds + photo.id
                                } else {
                                    lightboxPhoto = photo
                                }
                            },
                            onRestore = {
                                haptics.undoPulse()
                                selectedIds = selectedIds - photo.id
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

        // Thumb-friendly Floating Bottom Action Dock (sits cleanly above the main navigation dock)
        if (displayedTrashed.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = floatingDockInset + navBarBottom + 10.dp
                    )
            ) {
                FrostedGlassCard(
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 12.dp,
                    enableAmbientBlur = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GalleryActionButton(
                            text = if (isPartialSelection) "还原 (${targets.size})" else stringResource(R.string.action_restore_all),
                            onClick = {
                                haptics.undoPulse()
                                onRestoreAll(targets)
                                selectedIds = emptySet()
                            },
                            tone = GalleryButtonTone.GLASS,
                            leadingIcon = Icons.Filled.Restore,
                            height = 48.dp,
                            shape = RoundedCornerShape(15.dp),
                            modifier = Modifier
                                .weight(0.92f)
                                .testTag("vault_restore_all_button")
                        )

                        GallerySculptedHeroButton(
                            title = if (isPartialSelection) "清理已选" else "确认清理",
                            badgeText = "${targets.size} 项 · ${formatStorageSize(targetBytes)}",
                            onClick = { showPurgeDialog = true },
                            tone = GalleryButtonTone.DANGER,
                            leadingIcon = Icons.Filled.DeleteForever,
                            height = 48.dp,
                            shape = RoundedCornerShape(15.dp),
                            modifier = Modifier
                                .weight(1.45f)
                                .testTag("vault_purge_button")
                        )
                    }
                }
            }
        }
    }

    lightboxPhoto?.let { photo ->
        val startIdx = displayedTrashed.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        PhotoDetailLightbox(
            photo = photo,
            photos = displayedTrashed,
            initialIndex = startIdx,
            onDismiss = { lightboxPhoto = null },
            onRestore = onRestorePhoto
        )
    }

    if (showPurgeDialog && targets.isNotEmpty()) {
        val isLegacyPermanentDelete = Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            targets.any { !it.isSample && !it.contentUri.isNullOrBlank() }

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
                    text = if (isLegacyPermanentDelete) {
                        "永久删除 ${targets.size} 项（${formatStorageSize(targetBytes)}）？"
                    } else {
                        "确认释放 ${formatStorageSize(targetBytes)}？"
                    },
                    color = InkPrimary
                )
            },
            text = {
                Text(
                    text = if (isLegacyPermanentDelete) {
                        "当前系统版本不支持移入系统回收站。确认后这 ${targets.size} 项本地文件将被永久删除，无法恢复；未能删除的文件会留在回收站并提示原因。"
                    } else {
                        "即将清理这 ${targets.size} 项照片与视频（系统将请求将真实本地文件移入 Android 系统回收站，30 天内仍可在系统相册恢复）。"
                    },
                    color = InkSecondary
                )
            },
            confirmButton = {
                GalleryActionButton(
                    text = if (isLegacyPermanentDelete) "永久删除，无法恢复" else stringResource(R.string.action_purge_confirm),
                    onClick = {
                        haptics.swipeDeleteConfirm()
                        showPurgeDialog = false
                        onConfirmPurgeTargets(targets)
                        selectedIds = emptySet()
                    },
                    tone = GalleryButtonTone.DANGER,
                    height = 48.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("vault_dialog_confirm_purge")
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showPurgeDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("取消", color = InkSecondary)
                }
            }
        )
    }
}

@Composable
private fun VaultCompactSummaryContent(
    isPartialSelection: Boolean,
    hasItems: Boolean,
    displayBytes: Long,
    targetsCount: Int,
    totalPurgedBytes: Long,
    totalCompressedSavedBytes: Long
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isPartialSelection) "已选待释放空间" else "本次待释放空间",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
                Text(
                    text = formatStorageSize(displayBytes),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = if (hasItems) CoralDanger else EmeraldKeep
                )
            }
            if (hasItems) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CoralSoft,
                    border = BorderStroke(0.75.dp, CoralDanger.copy(alpha = 0.28f))
                ) {
                    Text(
                        text = "$targetsCount 项待确认",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CoralDanger,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.7.dp, color = HairlineBorder.copy(alpha = 0.7f))

        Text(
            text = "历史累计已释放 ${formatStorageSize(totalPurgedBytes)} · 压缩节省 ${formatStorageSize(totalCompressedSavedBytes)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = InkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernVaultMediaCard(
    photo: PhotoItemEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SurfacePure,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CobaltPrimary else HairlineBorder
        ),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.95f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .combinedClickable(
                        onClick = onPreview,
                        onLongClick = onToggleSelect
                    )
            ) {
                PhotoArtwork(
                    photo = photo,
                    contentDescription = photo.caption.ifBlank { if (photo.isVideo) "待清理视频" else "待清理照片" },
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

                // Honest staged duration badge (Stage 1.2)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CoralSoft.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = formatVaultStagedLabel(photo.trashedAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CoralDanger,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // 48dp accessible selection checkbox target
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clickable(role = Role.Checkbox, onClick = onToggleSelect)
                        .semantics {
                            contentDescription = if (isSelected) "已勾选此项，点击取消勾选" else "未勾选，点击勾选此项"
                        }
                        .padding(8.dp)
                        .testTag("vault_select_${photo.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CobaltPrimary else Color.Black.copy(alpha = 0.48f))
                            .border(1.5.dp, Color.White, CircleShape)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
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
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${photo.year}.${photo.month}.${photo.day}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp,
                        color = InkTertiary,
                        maxLines = 1
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
                val restoreLabel = stringResource(R.string.action_restore)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRestore)
                        .semantics { contentDescription = restoreLabel }
                        .testTag("restore_photo_${photo.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CobaltSoft,
                        border = BorderStroke(0.75.dp, CobaltPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
