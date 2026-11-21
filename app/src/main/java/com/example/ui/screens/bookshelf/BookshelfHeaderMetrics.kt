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
 * Compact frosted-glass telemetry bar freeing up vertical height for the 3D Bookshelf stage.
 */
@Composable
internal fun CompactStudioMetricsBar(
    purgedBytes: Long,
    compressedSavedBytes: Long,
    pendingTrashBytes: Long,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
        enableAmbientBlur = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactMetricSlot(
                label = "已释放",
                value = formatStorageSize(purgedBytes),
                accent = CobaltPrimary,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.height(22.dp),
                thickness = 1.dp,
                color = HairlineBorder.copy(alpha = 0.7f)
            )
            CompactMetricSlot(
                label = "压缩省",
                value = formatStorageSize(compressedSavedBytes),
                accent = EmeraldKeep,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.height(22.dp),
                thickness = 1.dp,
                color = HairlineBorder.copy(alpha = 0.7f)
            )
            CompactMetricSlot(
                label = "回收站",
                value = formatStorageSize(pendingTrashBytes),
                accent = CoralDanger,
                onClick = if (pendingTrashBytes > 0L) onOpenVault else null,
                showChevron = pendingTrashBytes > 0L,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bookshelf_vault_metric")
            )
        }
    }
}

@Composable
private fun CompactMetricSlot(
    label: String,
    value: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = stringResource(R.string.cd_open_vault_metric),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.5.sp,
                color = InkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
                    accessLevel == MediaAccessLevel.NONE -> "授权后，FlipYear 将自动在设备本地扫描全部照片和视频，生成 3D 年度影集并智能识别相似连拍，数据全程不上传云端。"
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
