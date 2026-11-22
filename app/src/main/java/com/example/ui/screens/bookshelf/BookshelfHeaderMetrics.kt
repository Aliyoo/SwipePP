package com.example.ui.screens.bookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.MediaAccessLevel
import com.example.ui.MediaFilter
import com.example.ui.components.DecoIcon
import com.example.ui.components.DecoIconKind
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette

private val DecoPanelShape = RoundedCornerShape(3.dp)

/**
 * On-demand storage ledger sheet opened from the Bookshelf's compact header summary,
 * keeping the hero viewport calm while preserving full deletion vs compression accounting.
 * 摩登画报重绘：画报地面容器 + 金线内衬面板 + 墨金字排版，账目口径与入口完全不变。
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val palette = LocalFlipYearPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.decoGround,
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
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
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = DecoInkOnGround
                )
                Text(
                    text = "删除释放与批量压缩节省分开统计，回收站中的媒体可随时还原",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = DecoInkOnGround.copy(alpha = 0.72f)
                )
            }

            // 金线内衬账目面板
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DecoPanelShape)
                    .background(DecoMat.copy(alpha = 0.55f))
                    .border(1.dp, DecoGold.copy(alpha = 0.45f), DecoPanelShape)
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
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(34.dp),
                        thickness = 1.dp,
                        color = DecoGold.copy(alpha = 0.35f)
                    )
                    CompactMetricSlot(
                        label = "压缩共节省",
                        value = formatStorageSize(compressedSavedBytes),
                        subLabel = "$compressedCount 张",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(34.dp),
                        thickness = 1.dp,
                        color = DecoGold.copy(alpha = 0.35f)
                    )
                    CompactMetricSlot(
                        label = "回收站待清",
                        value = formatStorageSize(pendingTrashBytes),
                        subLabel = "$pendingTrashCount 项",
                        dotAccent = DecoVerm,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bookshelf_vault_metric")
                    )
                }
            }

            if (pendingTrashCount > 0) {
                DecoVermButton(
                    text = "去回收站释放空间 (${formatStorageSize(pendingTrashBytes)})",
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
                    tone = GalleryButtonTone.IMMERSIVE,
                    height = 48.dp,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 朱红画报按钮：朱红底 + 纸黄字（可读性红线允许的组合），用于账本内的回收站跳转。
 */
@Composable
private fun DecoVermButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(DecoVerm.copy(alpha = 0.94f), DecoVerm)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            fontFamily = DecoDisplayFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            color = DecoPaperCream,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactMetricSlot(
    label: String,
    value: String,
    subLabel: String,
    dotAccent: Color? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (dotAccent != null) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(dotAccent)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
                color = DecoInkOnGround.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DecoInkOnGround,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            // 标签字号红线 ≥12sp：subLabel 11sp 升 12sp
            fontSize = 12.sp,
            color = DecoInkOnGround.copy(alpha = 0.62f),
            maxLines = 1
        )
    }
}

/**
 * 摩登画报空态卡：金线内衬画框面板 + 墨金字排版。
 * 授权 / 重扫 / 加载示例 / 筛选复位 四条入口与全部 testTag 保持基线不变。
 */
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DecoPanelShape)
                .background(DecoMat.copy(alpha = 0.72f))
                .border(1.dp, DecoGold.copy(alpha = 0.50f), DecoPanelShape)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DecoGold.copy(alpha = 0.12f))
                        .border(1.dp, DecoGold.copy(alpha = 0.40f), RoundedCornerShape(4.dp))
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            strokeWidth = 2.6.dp,
                            color = DecoGold,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        DecoIcon(
                            kind = if (activeFilter == MediaFilter.VIDEOS) DecoIconKind.VIDEOCAM else DecoIconKind.PHOTO_LIBRARY,
                            color = if (activeFilter == MediaFilter.VIDEOS) DecoVerm else DecoGold,
                            iconSize = 30.dp
                        )
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
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    color = DecoInkOnGround,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = desc,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = DecoInkOnGround.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    activeFilter != MediaFilter.ALL -> {
                        GalleryActionButton(
                            text = "查看全部照片和视频",
                            tone = GalleryButtonTone.IMMERSIVE,
                            onClick = onResetFilter,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    accessLevel == MediaAccessLevel.NONE -> {
                        GalleryActionButton(
                            text = stringResource(R.string.action_grant_media),
                            leadingIconKind = DecoIconKind.FOLDER_OPEN,
                            tone = GalleryButtonTone.IMMERSIVE,
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
                            leadingIconKind = DecoIconKind.REFRESH,
                            tone = GalleryButtonTone.IMMERSIVE,
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
