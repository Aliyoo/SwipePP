package com.example.ui.screens.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DecoIcon
import com.example.ui.components.DecoIconKind
import com.example.ui.components.DecoSheetDragHandle
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.formatStorageSize
import com.example.ui.components.modalSheetTopClearance
import com.example.ui.components.monthChineseName
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoDisplayFontFamily

private val CURATED_ALBUM_PRESETS = listOf("精选珍藏", "旅行日记", "家庭相册", "灵感素材")

/* ---- 摩登画报 · 纸面弹层装饰（deco-app.html 金边跳签 / 金双线画框） ---- */
private val MoveSheetShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
private val MoveFrameShape = RoundedCornerShape(3.dp)
private val MoveTagShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BulkMoveDestinationSheet(
    selectedCount: Int,
    selectedBytes: Long,
    existingAlbums: List<String>,
    availableYears: List<Int>,
    defaultYear: Int,
    defaultMonth: Int,
    onDismiss: () -> Unit,
    onConfirmMove: (albumName: String?, targetYear: Int?, targetMonth: Int?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val albumSuggestions = remember(existingAlbums) {
        (CURATED_ALBUM_PRESETS + existingAlbums).distinct()
    }
    var albumInput by remember { mutableStateOf(albumSuggestions.firstOrNull() ?: "精选珍藏") }
    var changeYearMonth by remember { mutableStateOf(false) }
    var chosenYear by remember { mutableIntStateOf(defaultYear) }
    var chosenMonth by remember { mutableIntStateOf(defaultMonth.coerceIn(1, 12)) }

    val canConfirm = albumInput.isNotBlank() || changeYearMonth

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DecoMat,
        shape = MoveSheetShape,
        dragHandle = { DecoSheetDragHandle() },
        modifier = Modifier
            .modalSheetTopClearance()
            .testTag("bulk_move_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bulk_move_title),
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = DecoInkOnGround
                    )
                    Text(
                        text = "已选 $selectedCount 项媒体 · 共 ${formatStorageSize(selectedBytes)}",
                        fontFamily = DecoDisplayFontFamily,
                        fontSize = 12.sp,
                        color = DecoInkOnGround.copy(alpha = 0.62f)
                    )
                }
                // 方章图标座
                Surface(
                    shape = MoveFrameShape,
                    color = DecoGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.35f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        DecoIcon(
                            kind = DecoIconKind.FOLDER_MOVE,
                            color = DecoGold,
                            iconSize = 20.dp
                        )
                    }
                }
            }

            Text(
                text = "快速选择目标相册",
                fontFamily = DecoDisplayFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DecoInkOnGround.copy(alpha = 0.72f)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(albumSuggestions, key = { it }) { album ->
                    val selected = albumInput.trim() == album
                    DecoMoveJumpTag(
                        selected = selected,
                        onClick = { albumInput = album },
                        modifier = Modifier.testTag("bulk_move_preset_$album")
                    ) {
                        DecoIcon(
                            kind = DecoIconKind.FOLDER,
                            color = if (selected) DecoMat else DecoGold.copy(alpha = 0.85f),
                            iconSize = 14.dp
                        )
                        Text(
                            text = album,
                            fontSize = 12.sp,
                            fontFamily = DecoDisplayFontFamily,
                            color = if (selected) DecoMat else DecoInkOnGround.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = albumInput,
                onValueChange = { albumInput = it },
                singleLine = true,
                label = { Text(stringResource(R.string.bulk_move_album_label)) },
                placeholder = { Text(stringResource(R.string.bulk_move_album_placeholder)) },
                leadingIcon = {
                    DecoIcon(
                        kind = DecoIconKind.FOLDER,
                        color = DecoGold
                    )
                },
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DecoGold,
                    unfocusedBorderColor = DecoGold.copy(alpha = 0.4f),
                    cursorColor = DecoGold,
                    focusedTextColor = DecoInkOnGround,
                    unfocusedTextColor = DecoInkOnGround,
                    focusedContainerColor = DecoGround.copy(alpha = 0.45f),
                    unfocusedContainerColor = DecoGround.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bulk_move_album_input")
            )

            // 金双线画框：外 1dp 金 + 内衬 + 1dp 弱金
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MoveFrameShape)
                    .background(DecoGround.copy(alpha = 0.5f))
                    .border(1.dp, DecoGold.copy(alpha = 0.6f), MoveFrameShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            DecoIcon(
                                kind = DecoIconKind.CALENDAR,
                                color = DecoGold,
                                iconSize = 18.dp
                            )
                            Text(
                                text = stringResource(R.string.bulk_move_date_toggle),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DecoInkOnGround
                            )
                        }
                        Switch(
                            checked = changeYearMonth,
                            onCheckedChange = { changeYearMonth = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DecoPaperCream,
                                checkedTrackColor = DecoVerm
                            ),
                            modifier = Modifier.testTag("bulk_move_date_switch")
                        )
                    }

                    AnimatedVisibility(visible = changeYearMonth) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(availableYears, key = { it }) { yr ->
                                    val isSel = yr == chosenYear
                                    DecoMoveJumpTag(
                                        selected = isSel,
                                        onClick = { chosenYear = yr }
                                    ) {
                                        Text(
                                            text = "${yr}年",
                                            fontFamily = DecoDisplayFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) DecoMat else DecoInkOnGround.copy(alpha = 0.78f)
                                        )
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items((1..12).toList(), key = { it }) { month ->
                                    val isSel = month == chosenMonth
                                    DecoMoveJumpTag(
                                        selected = isSel,
                                        onClick = { chosenMonth = month }
                                    ) {
                                        Text(
                                            text = monthChineseName(month),
                                            fontSize = 12.sp,
                                            fontFamily = DecoDisplayFontFamily,
                                            color = if (isSel) DecoMat else DecoInkOnGround.copy(alpha = 0.78f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GalleryActionButton(
                    text = "取消",
                    tone = GalleryButtonTone.GLASS,
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("bulk_move_cancel_button")
                )
                GalleryActionButton(
                    text = "确认移动 ($selectedCount)",
                    leadingIconKind = DecoIconKind.FOLDER_MOVE,
                    tone = GalleryButtonTone.IMMERSIVE,
                    enabled = canConfirm,
                    onClick = {
                        onConfirmMove(
                            albumInput.trim().takeIf { it.isNotBlank() },
                            if (changeYearMonth) chosenYear else null,
                            if (changeYearMonth) chosenMonth else null
                        )
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("bulk_move_confirm_button")
                )
            }
        }
    }
}

/** 金边跳签（选中金底 + 内衬字，未选中金描边透明底） */
@Composable
private fun DecoMoveJumpTag(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MoveTagShape,
        color = if (selected) DecoGold else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) DecoGold else DecoGold.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            content = content
        )
    }
}
