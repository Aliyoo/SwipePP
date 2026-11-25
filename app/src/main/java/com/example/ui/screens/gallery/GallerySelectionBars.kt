package com.example.ui.screens.gallery

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DecoIcon
import com.example.ui.components.DecoIconKind
import com.example.ui.components.DecoSheetDragHandle
import com.example.ui.components.formatStorageSize
import com.example.ui.components.modalSheetTopClearance
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoDisplayFontFamily

/* ---- 摩登画报 · 选择栏装饰（deco-app.html 金双线画框 / 朱红通栏 / 方章） ---- */
private val SelectionBarShape = RoundedCornerShape(3.dp)
private val SelectionSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val VermStripShape = RoundedCornerShape(4.dp)

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
        containerColor = DecoMat,
        shape = SelectionSheetShape,
        dragHandle = { DecoSheetDragHandle() },
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
                fontFamily = DecoDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = DecoInkOnGround
            )

            BatchActionSheetRow(
                iconKind = DecoIconKind.COMPRESS,
                accentColor = DecoGold,
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
                iconKind = DecoIconKind.EDIT_LOCATION,
                accentColor = DecoGold,
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
                iconKind = DecoIconKind.FOLDER_MOVE,
                accentColor = DecoGold,
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
    iconKind: DecoIconKind,
    accentColor: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = SelectionBarShape,
        color = DecoGold.copy(alpha = if (enabled) 0.08f else 0.04f),
        border = BorderStroke(1.dp, DecoGold.copy(alpha = if (enabled) 0.45f else 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 方章图标座（正方、弱金描边）
            Surface(
                shape = SelectionBarShape,
                color = accentColor.copy(alpha = if (enabled) 0.14f else 0.06f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = if (enabled) 0.35f else 0.16f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DecoIcon(
                        kind = iconKind,
                        color = if (enabled) accentColor else DecoInkOnGround.copy(alpha = 0.4f),
                        iconSize = 20.dp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) DecoInkOnGround else DecoInkOnGround.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    color = DecoInkOnGround.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DecoIcon(
                kind = DecoIconKind.CHEVRON_RIGHT,
                color = DecoInkOnGround.copy(alpha = if (enabled) 0.72f else 0.3f),
                iconSize = 20.dp
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
    // 金双线画框：外 1.5dp 金 + 底色内衬 + 1dp 弱金（deco-app.html GoldFrame）
    val frameGold = DecoGold
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(SelectionBarShape)
            .background(DecoMat.copy(alpha = 0.82f))
            .drawBehind {
                drawRoundRect(
                    color = frameGold.copy(alpha = 0.4f),
                    topLeft = Offset(3.5.dp.toPx(), 3.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 7.dp.toPx(),
                        size.height - 7.dp.toPx()
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
            .border(1.5.dp, DecoGold, SelectionBarShape)
            .testTag("gallery_selection_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 方章取消钮
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("gallery_selection_cancel")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(SelectionBarShape)
                        .border(1.5.dp, DecoGold.copy(alpha = 0.85f), SelectionBarShape)
                ) {
                    DecoIcon(
                        kind = DecoIconKind.CLOSE,
                        color = DecoGold,
                        iconSize = 17.dp,
                        contentDescription = stringResource(R.string.cd_exit_selection)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已选 $selectedCount 项",
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DecoInkOnGround,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "合计 ${formatStorageSize(selectedBytes)} · 点击可增减项目",
                    fontFamily = DecoDisplayFontFamily,
                    fontSize = 12.sp,
                    color = DecoInkOnGround.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 金边跳签（全选 / 取消全选）
            Surface(
                onClick = onToggleSelectAll,
                shape = RoundedCornerShape(3.dp),
                color = if (allSelected) DecoGold else Color.Transparent,
                border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("gallery_selection_toggle_all")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DecoIcon(
                        kind = DecoIconKind.SELECT_ALL,
                        color = if (allSelected) DecoMat else DecoGold,
                        iconSize = 15.dp
                    )
                    Text(
                        text = stringResource(
                            if (allSelected) R.string.action_deselect_all else R.string.action_select_all
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = DecoDisplayFontFamily,
                        fontSize = 12.sp,
                        color = if (allSelected) DecoMat else DecoInkOnGround.copy(alpha = 0.78f)
                    )
                }
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
    // 朱红通栏动作条（deco-app.html VermStrip）：朱底 + 纸黄动作字 + 中缝 1dp 金渐变
    Box(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .alpha(if (actionsEnabled) 1f else 0.56f)
            .clip(VermStripShape)
            .background(DecoVerm)
            .drawBehind {
                // 顶缘纸黄高光细线（inset 0 1px 0 rgba(239,227,200,0.22)）
                drawLine(
                    color = DecoPaperCream.copy(alpha = 0.22f),
                    start = Offset(0f, 0.8.dp.toPx()),
                    end = Offset(size.width, 0.8.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .testTag("gallery_bulk_action_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BulkActionPillItem(
                iconKind = DecoIconKind.CHECK_CIRCLE,
                label = stringResource(R.string.action_bulk_keep),
                enabled = actionsEnabled,
                onClick = onKeep,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bulk_keep_button")
            )

            VermStripDivider()

            BulkActionPillItem(
                iconKind = DecoIconKind.FAVORITE,
                favoriteSelected = true,
                label = stringResource(R.string.action_bulk_favorite),
                enabled = actionsEnabled,
                onClick = onFavorite,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bulk_favorite_button")
            )

            VermStripDivider()

            BulkActionPillItem(
                iconKind = DecoIconKind.DELETE,
                label = if (selectedCount > 0) "回收站($selectedCount)" else stringResource(R.string.action_bulk_delete),
                containerHighlight = DecoPaperCream.copy(alpha = 0.14f),
                enabled = actionsEnabled,
                onClick = onTrash,
                modifier = Modifier
                    .weight(1.22f)
                    .testTag("bulk_delete_button")
            )

            VermStripDivider()

            BulkActionPillItem(
                iconKind = DecoIconKind.MORE_HORIZ,
                label = "更多",
                enabled = actionsEnabled,
                onClick = onMore,
                modifier = Modifier
                    .weight(0.92f)
                    .testTag("bulk_more_button")
            )
        }
    }
}

/** 中缝 1dp 金渐变分隔（朱红通栏内部，deco-app.html VermStrip 中缝） */
@Composable
private fun VermStripDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .width(1.dp)
            .height(26.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DecoGold.copy(alpha = 0f),
                        DecoGold.copy(alpha = 0.8f),
                        DecoGold.copy(alpha = 0f)
                    )
                )
            )
    )
}

@Composable
private fun BulkActionPillItem(
    iconKind: DecoIconKind,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    containerHighlight: Color = Color.Transparent,
    favoriteSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(containerHighlight)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        DecoIcon(
            kind = iconKind,
            color = DecoPaperCream,
            selected = favoriteSelected,
            iconSize = 21.dp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = DecoDisplayFontFamily,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.SemiBold,
            color = DecoPaperCream.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
