package com.example.ui.screens.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GallerySelectableChip
import com.example.ui.components.ModalSheetDragHandle
import com.example.ui.components.ObsidianGlassCard
import com.example.ui.components.formatStorageSize
import com.example.ui.components.modalSheetTopClearance
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSatinBottom
import com.example.ui.theme.ObsidianSatinTop
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryMoreBatchActionsSheet(
    selectedCount: Int,
    compressibleCount: Int,
    missingLocationCount: Int,
    onCompress: () -> Unit,
    onEditLocation: () -> Unit,
    onMove: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfacePure,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { ModalSheetDragHandle() },
        modifier = Modifier
            .modalSheetTopClearance()
            .testTag("gallery_more_actions_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "更多批量操作（已选 $selectedCount 项）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )

            BatchActionSheetRow(
                icon = Icons.Filled.Compress,
                accentColor = EmeraldKeep,
                title = if (compressibleCount > 0) {
                    "${stringResource(R.string.action_batch_compress)}（$compressibleCount 张照片）"
                } else {
                    "批量压缩（当前所选无照片）"
                },
                subtitle = "另存压缩新图，原图安全暂存回收站",
                enabled = compressibleCount > 0,
                onClick = onCompress,
                modifier = Modifier.testTag("bulk_compress_button")
            )

            BatchActionSheetRow(
                icon = Icons.Filled.EditLocationAlt,
                accentColor = SolarAmber,
                title = if (missingLocationCount > 0) {
                    "${stringResource(R.string.action_batch_location)}（含 $missingLocationCount 项缺定位）"
                } else {
                    stringResource(R.string.action_batch_location)
                },
                subtitle = "按同日足迹推断或手动搜索地点补全定位",
                enabled = true,
                onClick = onEditLocation,
                modifier = Modifier.testTag("bulk_location_button")
            )

            BatchActionSheetRow(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                accentColor = CobaltPrimary,
                title = "移动至相册 / 归档年月",
                subtitle = "归类到自定义相册或重新指定年度月份",
                enabled = true,
                onClick = onMove,
                modifier = Modifier.testTag("bulk_move_button")
            )
        }
    }
}

@Composable
private fun BatchActionSheetRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(18.dp),
        color = SurfaceMuted.copy(alpha = if (enabled) 0.62f else 0.32f),
        border = BorderStroke(0.85.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (enabled) accentColor.copy(alpha = 0.14f) else SurfaceMuted,
                border = BorderStroke(0.75.dp, if (enabled) accentColor.copy(alpha = 0.28f) else HairlineBorder),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) accentColor else InkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) InkPrimary else InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = InkSecondary.copy(alpha = if (enabled) 0.72f else 0.30f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun GallerySelectionTopBar(
    selectedCount: Int,
    selectedBytes: Long,
    allSelected: Boolean,
    onCancel: () -> Unit,
    onToggleSelectAll: () -> Unit
) {
    FrostedGlassCard(
        shape = RoundedCornerShape(20.dp),
        borderAccent = CobaltPrimary,
        shadowElevation = 4.dp,
        enableAmbientBlur = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("gallery_selection_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            com.example.ui.components.GalleryHeaderOrbButton(
                onClick = onCancel,
                modifier = Modifier.testTag("gallery_selection_cancel")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_exit_selection),
                    tint = InkPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已选 $selectedCount 项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "合计 ${formatStorageSize(selectedBytes)} · 点击可增减项目",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            GallerySelectableChip(
                selected = allSelected,
                onClick = onToggleSelectAll,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("gallery_selection_toggle_all")
            ) {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = null,
                    tint = if (allSelected) SolarAmber else CobaltPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = stringResource(
                        if (allSelected) R.string.action_deselect_all else R.string.action_select_all
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 12.sp,
                    color = if (allSelected) Color.White else InkPrimary
                )
            }
        }
    }
}

@Composable
internal fun FloatingBulkActionBar(
    selectedCount: Int,
    onKeep: () -> Unit,
    onFavorite: () -> Unit,
    onTrash: () -> Unit,
    onMore: () -> Unit
) {
    val actionsEnabled = selectedCount > 0
    ObsidianGlassCard(
        shape = RoundedCornerShape(22.dp),
        topColor = ObsidianSatinTop,
        bottomColor = ObsidianSatinBottom,
        shadowElevation = 18.dp,
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .alpha(if (actionsEnabled) 1f else 0.56f)
            .testTag("gallery_bulk_action_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BulkActionPillItem(
                icon = Icons.Filled.CheckCircle,
                label = stringResource(R.string.action_bulk_keep),
                iconTint = EmeraldKeep,
                enabled = actionsEnabled,
                onClick = onKeep,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bulk_keep_button")
            )

            BulkActionPillItem(
                icon = Icons.Filled.Favorite,
                label = stringResource(R.string.action_bulk_favorite),
                iconTint = SolarAmber,
                enabled = actionsEnabled,
                onClick = onFavorite,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bulk_favorite_button")
            )

            BulkActionPillItem(
                icon = Icons.Filled.DeleteOutline,
                label = if (selectedCount > 0) "回收站($selectedCount)" else stringResource(R.string.action_bulk_delete),
                iconTint = Color(0xFFFF7A8A),
                containerHighlight = CoralDanger.copy(alpha = 0.24f),
                enabled = actionsEnabled,
                onClick = onTrash,
                modifier = Modifier
                    .weight(1.22f)
                    .testTag("bulk_delete_button")
            )

            BulkActionPillItem(
                icon = Icons.Filled.MoreHoriz,
                label = "更多",
                iconTint = Color(0xFF8FB0FF),
                enabled = actionsEnabled,
                onClick = onMore,
                modifier = Modifier
                    .weight(0.92f)
                    .testTag("bulk_more_button")
            )
        }
    }
}

@Composable
private fun BulkActionPillItem(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    containerHighlight: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(containerHighlight)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
