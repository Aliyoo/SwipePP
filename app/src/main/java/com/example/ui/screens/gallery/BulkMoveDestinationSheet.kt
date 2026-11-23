package com.example.ui.screens.gallery

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GallerySelectableChip
import com.example.ui.components.ModalSheetDragHandle
import com.example.ui.components.formatStorageSize
import com.example.ui.components.modalSheetTopClearance
import com.example.ui.components.monthChineseName
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure

private val CURATED_ALBUM_PRESETS = listOf("精选珍藏", "旅行日记", "家庭相册", "灵感素材")

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
        containerColor = SurfacePure,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { ModalSheetDragHandle() },
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        text = "已选 $selectedCount 项媒体 · 共 ${formatStorageSize(selectedBytes)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.5.sp,
                        color = InkSecondary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = CobaltSoft,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = "快速选择目标相册",
                style = MaterialTheme.typography.labelLarge,
                color = InkSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(albumSuggestions, key = { it }) { album ->
                    val selected = albumInput.trim() == album
                    GallerySelectableChip(
                        selected = selected,
                        onClick = { albumInput = album },
                        modifier = Modifier.testTag("bulk_move_preset_$album")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = if (selected) SolarAmber else CobaltPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = album,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = if (selected) Color.White else InkPrimary
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
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = CobaltPrimary
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CobaltPrimary,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = SurfaceMuted.copy(alpha = 0.45f),
                    unfocusedContainerColor = SurfaceMuted.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bulk_move_album_input")
            )

            FrostedGlassCard(
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
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
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.bulk_move_date_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = InkPrimary
                            )
                        }
                        Switch(
                            checked = changeYearMonth,
                            onCheckedChange = { changeYearMonth = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CobaltPrimary
                            ),
                            modifier = Modifier.testTag("bulk_move_date_switch")
                        )
                    }

                    AnimatedVisibility(visible = changeYearMonth) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(availableYears, key = { it }) { yr ->
                                    val isSel = yr == chosenYear
                                    GallerySelectableChip(
                                        selected = isSel,
                                        onClick = { chosenYear = yr }
                                    ) {
                                        Text(
                                            text = "${yr}年",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else InkPrimary
                                        )
                                    }
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items((1..12).toList(), key = { it }) { month ->
                                    val isSel = month == chosenMonth
                                    GallerySelectableChip(
                                        selected = isSel,
                                        onClick = { chosenMonth = month }
                                    ) {
                                        Text(
                                            text = monthChineseName(month),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontSize = 11.5.sp,
                                            color = if (isSel) Color.White else InkPrimary
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
                    leadingIcon = Icons.AutoMirrored.Filled.DriveFileMove,
                    tone = GalleryButtonTone.OBSIDIAN,
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
