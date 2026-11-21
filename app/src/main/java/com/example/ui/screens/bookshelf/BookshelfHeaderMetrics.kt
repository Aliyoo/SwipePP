package com.example.ui.screens.bookshelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MediaAccessLevel
import com.example.ui.MediaFilter
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.VioletVideo

/**
 * On-demand storage ledger sheet opened from the Bookshelf's compact header summary,
 * keeping the hero viewport calm while preserving full deletion vs compression accounting.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun BookshelfStorageLedgerSheet(
    purgedBytes: Long,
    purgedCount: Int,
    compressedSavedBytes: Long,
    compressedCount: Int,
    pendingTrashBytes: Long,
    pendingTrashCount: Int,
    onOpenVault: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.example.ui.theme.SurfacePure,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("bookshelf_storage_ledger_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "空间释放账本",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Text(
                    text = "删除释放与批量压缩节省分开统计，回收站中的媒体可随时还原",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            FrostedGlassCard(
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactMetricSlot(
                        label = "删除已释放",
                        value = formatStorageSize(purgedBytes),
                        subLabel = "$purgedCount 项",
                        accent = CobaltPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(34.dp),
                        thickness = 1.dp,
                        color = HairlineBorder.copy(alpha = 0.7f)
                    )
                    CompactMetricSlot(
                        label = "压缩共节省",
                        value = formatStorageSize(compressedSavedBytes),
                        subLabel = "$compressedCount 张",
                        accent = EmeraldKeep,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(34.dp),
                        thickness = 1.dp,
                        color = HairlineBorder.copy(alpha = 0.7f)
                    )
                    CompactMetricSlot(
                        label = "回收站待清",
                        value = formatStorageSize(pendingTrashBytes),
                        subLabel = "$pendingTrashCount 项",
                        accent = CoralDanger,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bookshelf_vault_metric")
                    )
                }
            }

            if (pendingTrashCount > 0) {
                GalleryActionButton(
                    text = "去回收站释放空间 (${formatStorageSize(pendingTrashBytes)})",
                    tone = GalleryButtonTone.DANGER,
                    height = 48.dp,
                    onClick = {
                        onDismiss()
                        onOpenVault()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_open_vault_button")
                )
            } else {
                GalleryActionButton(
                    text = "知道了",
                    tone = GalleryButtonTone.GLASS,
                    height = 48.dp,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompactMetricSlot(
    label: String,
    value: String,
    subLabel: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = InkTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.5.sp,
            color = InkSecondary,
            maxLines = 1
        )
    }
}

@Composable
internal fun EmptyLibraryPermissionCard(
    accessLevel: MediaAccessLevel,
    isScanning: Boolean,
    activeFilter: MediaFilter,
    onRequestPermission: () -> Unit,
    onRescan: () -> Unit,
    onResetFilter: () -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrostedGlassCard(
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 8.dp,
            enableAmbientBlur = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CobaltSoft,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                strokeWidth = 2.6.dp,
                                color = CobaltPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (activeFilter == MediaFilter.VIDEOS) Icons.Filled.Videocam else Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = if (activeFilter == MediaFilter.VIDEOS) VioletVideo else CobaltPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                val title = when {
                    isScanning -> "正在扫描本地照片和视频..."
                    activeFilter != MediaFilter.ALL -> "当前筛选分类下暂无媒体"
                    accessLevel == MediaAccessLevel.NONE -> "读取全部本地照片与视频"
                    else -> "设备相册暂未检测到照片或视频"
                }

                val desc = when {
                    isScanning -> "正在按年份和月份自动整理您的相册与连拍组。"
                    activeFilter != MediaFilter.ALL -> "切换回“全部”即可查看您的完整年度影集。"
                    accessLevel == MediaAccessLevel.NONE -> "授权后，SwipePP 将自动在设备本地扫描全部照片和视频，生成 3D 年度影集并智能识别相似连拍，数据全程不上传云端。"
                    else -> "您可以先拍照/录屏后点击重新扫描，或加载精选示例影集（含 4K 视频与连拍）立即体验全部功能。"
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    activeFilter != MediaFilter.ALL -> {
                        GalleryActionButton(
                            text = "查看全部照片和视频",
                            tone = GalleryButtonTone.OBSIDIAN,
                            onClick = onResetFilter,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    accessLevel == MediaAccessLevel.NONE -> {
                        GalleryActionButton(
                            text = stringResource(R.string.action_grant_media),
                            leadingIcon = Icons.Filled.FolderOpen,
                            tone = GalleryButtonTone.OBSIDIAN,
                            onClick = onRequestPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_media_permission_button")
                        )
                        GalleryActionButton(
                            text = "先加载示例影集体验",
                            tone = GalleryButtonTone.GLASS,
                            onClick = onLoadDemo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_demo_archive_button")
                        )
                    }
                    else -> {
                        GalleryActionButton(
                            text = "重新扫描本地相册",
                            leadingIcon = Icons.Filled.Refresh,
                            tone = GalleryButtonTone.OBSIDIAN,
                            onClick = onRescan,
                            modifier = Modifier.fillMaxWidth()
                        )
                        GalleryActionButton(
                            text = stringResource(R.string.action_load_demo),
                            tone = GalleryButtonTone.GLASS,
                            onClick = onLoadDemo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_demo_archive_button")
                        )
                    }
                }
            }
        }
    }
}
