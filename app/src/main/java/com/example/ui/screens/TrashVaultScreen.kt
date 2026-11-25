package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.sensor.rememberHapticEngine
import com.example.ui.FlipYearUiState
import com.example.ui.LocalFloatingDockInset
import com.example.ui.components.DecoIcon
import com.example.ui.components.DecoIconKind
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.daysStagedInVault
import com.example.ui.components.formatStorageSize
import com.example.ui.components.formatVaultStagedLabel
import com.example.ui.components.decoPaperGroundBackground
import com.example.ui.components.isMachineGeneratedFilename
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoGroundDeep
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme

// ------------------------------------------------------------------
// 回收站 · 摩登画报 屏内一次性色彩与常数
// 地面文字一律从主题 deco 令牌（ink/gold）派生；以下硬色仅允许出现在
// 照片暗部压底 / 朱红底 / 画报错位阴影上（可读性红线）。
// ------------------------------------------------------------------

// 压照片暗 scrim：线性 0.62 → 透明 → 0.94（deco-app.html 可读性红线）
private val VaultPhotoScrimTop = Color.Black.copy(alpha = 0.62f)
private val VaultPhotoScrimBottom = Color.Black.copy(alpha = 0.94f)
// 照片暗部上的深底签（原型 rgba(19,41,31,0.88)）
private val VaultSignDarkBg = Color(0xFF13291F).copy(alpha = 0.88f)
// 美术字错位阴影（原型 artShadow 2/4/6dp）
private val VaultArtShadowBlack = Color.Black.copy(alpha = 0.55f)
private val VaultArtShadowBlur = Color.Black.copy(alpha = 0.35f)

// 临期示警阈值：暂存 ≥14 天以朱红签示警。
// 回收站保留期无倒计时（见 vault_retention_note：确认后才清理），
// 故以「暂存愈久」替代原型「剩 N 天」的临期语义。
private const val VaultUrgentStagedDays = 14

// 放射太阳纹 24 扇形（deco 造型语法）
private const val VaultSunburstWedges = 24

// 金描边上的字色：主题派生（dark onGold=ground，light onGold=mat）
private val VaultOnGold: Color
    @Composable get() = if (LocalIsDarkTheme.current) DecoGround else DecoMat

private val VaultDecoShape = RoundedCornerShape(3.dp)
private val VaultTileShape = RoundedCornerShape(2.dp)

@Composable
fun TrashVaultScreen(
    uiState: FlipYearUiState,
    onRestorePhoto: (PhotoItemEntity) -> Unit,
    onRestoreAll: (List<PhotoItemEntity>) -> Unit,
    onConfirmPurgeTargets: (List<PhotoItemEntity>) -> Unit,
    onClearSessionFilter: () -> Unit = {},
    onBrowseToOrganize: (() -> Unit)? = null,
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

    VaultDecoGround(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
        ) {
            // 美术字画报报头：标题 + 金渐隐线 + 保留期说明
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VaultArtText(
                        text = "废弃回收站",
                        fontSize = 28.sp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(DecoGold.copy(alpha = 0.66f), Color.Transparent)
                                )
                            )
                    )
                }
                Text(
                    text = stringResource(R.string.vault_retention_note),
                    fontSize = 12.5.sp,
                    color = DecoInkOnGround.copy(alpha = 0.62f),
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // Session filter banner when arriving from Cleaner completion page
            if (sessionFilterIds != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 10.dp)
                        .clip(VaultDecoShape)
                        .background(DecoMat.copy(alpha = 0.92f))
                        .border(1.dp, DecoGold.copy(alpha = 0.45f), VaultDecoShape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "仅看本次舍弃（${displayedTrashed.size} 项）",
                            fontSize = 13.sp,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = DecoInkOnGround,
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
                                fontFamily = DecoDisplayFontFamily,
                                color = DecoGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = displayedTrashed.isEmpty(),
                transitionSpec = {
                    (fadeIn(tween(210, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
                        fadeOut(tween(130))
                },
                label = "vault_empty_content_switch",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { isVaultEmpty ->
                if (isVaultEmpty) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Compact Storage Summary (empty state)
                        VaultGoldFrame(
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

                        // 空态重心上移：内容组靠近汇总卡（顶部 ~18% 留白）而非整块剩余高度正中，
                        // 消除汇总卡与盾徽之间的大段空白断层；矮窗（横屏）下可滚动，保证引导按钮可达
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                        val emptyViewportHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .heightIn(min = emptyViewportHeight)
                                .padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 24.dp + floatingDockInset + navBarBottom),
                            verticalArrangement = Arrangement.spacedBy(0.dp, BiasAlignment.Vertical(-0.45f)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                    DecoIcon(
                        kind = DecoIconKind.SHIELD,
                        // 空态徽记：主题金（画报色板；弃 Emerald 绿旧语义色）
                        color = DecoGold,
                        iconSize = 60.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    VaultArtText(
                        text = if (sessionFilterIds != null && uiState.trashedPhotos.isNotEmpty()) {
                            "本次舍弃项已全部处理"
                        } else {
                            "回收站很干净"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (sessionFilterIds != null && uiState.trashedPhotos.isNotEmpty()) {
                            "回收站中还有 ${uiState.trashedPhotos.size} 项历史暂存项目，可点击上方查看全部。"
                        } else {
                            "整理时上滑舍弃的照片和视频会先暂存于此，你可随时还原或确认释放空间。"
                        },
                        fontSize = 13.sp,
                        color = DecoInkOnGround.copy(alpha = 0.66f),
                        textAlign = TextAlign.Center
                        )
                        val showOrganizeCta = onBrowseToOrganize != null &&
                            !(sessionFilterIds != null && uiState.trashedPhotos.isNotEmpty())
                        if (showOrganizeCta) {
                            Spacer(modifier = Modifier.height(20.dp))
                            // 空态引导动作：金描边方章按钮回书架开始整理（避免空态成为死胡同）
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(VaultDecoShape)
                                    .border(1.dp, DecoGold, VaultDecoShape)
                                    .clickable { onBrowseToOrganize() }
                                    .padding(horizontal = 22.dp)
                                    .testTag("vault_empty_organize_button")
                            ) {
                                Text(
                                    text = "去整理相册",
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 2.sp,
                                    color = DecoGold
                                )
                            }
                        }
                        }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 2.dp,
                            bottom = 104.dp + floatingDockInset + navBarBottom
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("vault_photo_grid")
                    ) {
                    // Framed summary panel: art-figure pending space + history metrics
                    item(key = "vault_summary_header", span = { GridItemSpan(maxLineSpan) }) {
                        VaultGoldFrame(modifier = Modifier.fillMaxWidth()) {
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
                                fontSize = 12.sp,
                                color = DecoInkOnGround.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
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
                                    fontFamily = DecoDisplayFontFamily,
                                    color = DecoGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    items(displayedTrashed, key = { it.id }) { photo ->
                        val isSelected = photo.id in selectedIds
                        VaultDecoMediaCard(
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
        }

        // Thumb-friendly Floating Bottom Action Dock (画报金线坞：还原金边 + 确认清理朱红)
        AnimatedVisibility(
            visible = displayedTrashed.isNotEmpty(),
            enter = fadeIn(tween(190, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 2 },
            exit = fadeOut(tween(135)) +
                slideOutVertically(tween(150)) { it / 2 },
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
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = VaultDecoShape,
                            ambientColor = Color.Black.copy(alpha = 0.38f),
                            spotColor = Color.Black.copy(alpha = 0.38f)
                        )
                        .clip(VaultDecoShape)
                        .background(DecoMat.copy(alpha = 0.97f))
                        .border(1.dp, DecoGold.copy(alpha = 0.5f), VaultDecoShape)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VaultDecoOutlineButton(
                            text = if (isPartialSelection) "还原 (${targets.size})" else stringResource(R.string.action_restore_all),
                            leadingIconKind = DecoIconKind.RESTORE,
                            onClick = {
                                haptics.undoPulse()
                                onRestoreAll(targets)
                                selectedIds = emptySet()
                            },
                            modifier = Modifier
                                .weight(0.92f)
                                .testTag("vault_restore_all_button")
                        )

                        VaultDecoVermilionButton(
                            title = if (isPartialSelection) "清理已选" else "确认清理",
                            badgeText = "${targets.size} 项 · ${formatStorageSize(targetBytes)}",
                            leadingIconKind = DecoIconKind.DELETE_FOREVER,
                            onClick = { showPurgeDialog = true },
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
            containerColor = DecoMat,
            shape = VaultDecoShape,
            titleContentColor = DecoInkOnGround,
            textContentColor = DecoInkOnGround.copy(alpha = 0.78f),
            icon = {
                DecoIcon(
                    kind = DecoIconKind.CLEAN,
                    color = DecoVerm,
                    iconSize = 26.dp
                )
            },
            title = {
                Text(
                    text = if (isLegacyPermanentDelete) {
                        "永久删除 ${targets.size} 项（${formatStorageSize(targetBytes)}）？"
                    } else {
                        "确认释放 ${formatStorageSize(targetBytes)}？"
                    },
                    fontFamily = DecoDisplayFontFamily,
                    color = DecoInkOnGround
                )
            },
            text = {
                Text(
                    text = if (isLegacyPermanentDelete) {
                        "当前系统版本不支持移入系统回收站。确认后这 ${targets.size} 项本地文件将被永久删除，无法恢复；未能删除的文件会留在回收站并提示原因。"
                    } else {
                        "即将清理这 ${targets.size} 项照片与视频（系统将请求将真实本地文件移入 Android 系统回收站，30 天内仍可在系统相册恢复）。"
                    },
                    color = DecoInkOnGround.copy(alpha = 0.78f)
                )
            },
            confirmButton = {
                VaultDecoVermilionButton(
                    title = if (isLegacyPermanentDelete) "永久删除，无法恢复" else stringResource(R.string.action_purge_confirm),
                    onClick = {
                        haptics.swipeDeleteConfirm()
                        showPurgeDialog = false
                        onConfirmPurgeTargets(targets)
                        selectedIds = emptySet()
                    },
                    modifier = Modifier.testTag("vault_dialog_confirm_purge")
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showPurgeDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "取消",
                        fontFamily = DecoDisplayFontFamily,
                        color = DecoInkOnGround.copy(alpha = 0.72f)
                    )
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
    // 零释放数走主题金弱化态（画报色板；弃 Emerald 绿旧语义色）
    val numberColor = if (hasItems) DecoVerm else DecoGold
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (isPartialSelection) "已选待释放空间" else "本次待释放空间",
                    fontSize = 12.sp,
                    fontFamily = DecoDisplayFontFamily,
                    color = DecoInkOnGround.copy(alpha = 0.62f)
                )
                // 美术字大数（待释放空间）
                VaultArtText(
                    text = formatStorageSize(displayBytes),
                    fontSize = 38.sp,
                    fontFamily = EditorialSerifFontFamily,
                    color = numberColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (hasItems) {
                Surface(
                    shape = VaultTileShape,
                    color = DecoVerm,
                    border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "$targetsCount 项待确认",
                        fontFamily = DecoDisplayFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DecoPaperCream,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 金色发丝分隔线（原型 borderTop 1px 金 0.45）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DecoGold.copy(alpha = 0.45f))
        )

        Text(
            text = "历史累计已释放 ${formatStorageSize(totalPurgedBytes)} · 压缩节省 ${formatStorageSize(totalCompressedSavedBytes)}",
            fontFamily = DecoDisplayFontFamily,
            fontSize = 12.sp,
            color = DecoInkOnGround.copy(alpha = 0.66f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultDecoMediaCard(
    photo: PhotoItemEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = DecoInkOnGround
    val gold = DecoGold
    val mat = DecoMat
    val isUrgent = daysStagedInVault(photo.trashedAtMillis) >= VaultUrgentStagedDays
    val restoreLabel = stringResource(R.string.action_restore)
    val metaLabel = photo.caption.ifBlank { if (photo.isVideo) "待清理视频" else "待清理照片" }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(VaultTileShape)
                .background(mat.copy(alpha = 0.95f))
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) ink else gold.copy(alpha = 0.5f),
                    shape = VaultTileShape
                )
        ) {
            // 画片：照片 + 暗 scrim + 勾选方章 + 暂存时长签/字节
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.95f)
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
                // 可读性红线 scrim：线性 0.62 → 透明 → 0.94
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to VaultPhotoScrimTop,
                                0.42f to Color.Transparent,
                                1.0f to VaultPhotoScrimBottom
                            )
                        )
                )

                // 48dp accessible selection checkbox target（朱金方章）
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
                            .size(22.dp)
                            .clip(VaultTileShape)
                            .background(if (isSelected) gold else VaultSignDarkBg)
                            .border(
                                1.dp,
                                if (isSelected) VaultOnGold.copy(alpha = 0.6f) else DecoPaperCream.copy(alpha = 0.55f),
                                VaultTileShape
                            )
                    ) {
                        if (isSelected) {
                            DecoIcon(
                                kind = DecoIconKind.CHECK,
                                color = VaultOnGold,
                                iconSize = 15.dp
                            )
                        }
                    }
                }

                // 单卡还原方章：38dp 正方 / 1.5dp 金描边 / 竖排两字，48dp 可点区
                Box(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(48.dp)
                        .clickable(onClick = onRestore)
                        .semantics { contentDescription = restoreLabel }
                        .testTag("restore_photo_${photo.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(38.dp)
                            .clip(VaultTileShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.5.dp, gold, VaultTileShape)
                    ) {
                        Text(
                            text = "还\n原",
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = DecoPaperCream,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 暂存时长签（临期朱红）+ 字节：压照片暗部，纸黄字
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 7.dp, bottom = 7.dp)
                ) {
                    Surface(
                        shape = VaultTileShape,
                        color = if (isUrgent) DecoVerm else VaultSignDarkBg,
                        border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = formatVaultStagedLabel(photo.trashedAtMillis),
                            fontSize = 12.sp,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isUrgent) DecoPaperCream else DecoPaperCream.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formatStorageSize(photo.fileSizeBytes),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DecoPaperCream,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }

            // 日期说明条：画报内衬底
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${photo.year}.${photo.month}.${photo.day}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = ink.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metaLabel,
                    // 机器文件名走 mono（latin 保留），中文兜底/题注走画报美术字
                    fontFamily = if (photo.caption.isNotBlank() && isMachineGeneratedFilename(photo.caption)) {
                        JetBrainsMonoFontFamily
                    } else {
                        DecoDisplayFontFamily
                    },
                    fontSize = 12.sp,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 选中金晕环（原型 boxShadow 0 0 0 3px goldDim）
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.dp, gold.copy(alpha = 0.55f), VaultTileShape)
            )
        }
    }
}

// ------------------------------------------------------------------
// 摩登画报 · 回收站屏内画框/美术字/太阳纹/按钮 原语
// ------------------------------------------------------------------

/** 金双线画框：外 1.5dp 金 + 3dp 内衬 + 1dp 弱金，画片圆角 3dp */
@Composable
private fun VaultGoldFrame(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val gold = DecoGold
    val mat = DecoMat
    Box(
        modifier
            .shadow(
                elevation = 6.dp,
                shape = VaultDecoShape,
                ambientColor = Color.Black.copy(alpha = 0.30f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(VaultDecoShape)
            .background(mat.copy(alpha = 0.92f))
            .border(1.5.dp, gold, VaultDecoShape)
            .padding(3.dp)
            .border(1.dp, gold.copy(alpha = 0.45f), VaultTileShape)
            .padding(contentPadding)
    ) {
        content()
    }
}

private const val VAULT_FULL_ART_SHADOW_MIN_SP = 36f

/** 美术字（DecoDisplay 小薇 Bold）/错位阴影随字号等比：黑 0.024em · 朱 0.049em · 模糊 0.073em（原型 82px 大年份 2/4/6px 的同比例） */
@Composable
private fun VaultArtText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = DecoInkOnGround,
    fontFamily: FontFamily = DecoDisplayFontFamily,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val verm = DecoVerm
    val density = LocalDensity.current
    // 错位影偏移随字号等比（原型 artShadow 只用于 82px 大年份，固定 dp 在中小标题上会糊成叠影）
    fun emOffset(ratio: Float): androidx.compose.ui.unit.Dp = with(fontSize) { value * ratio }.dp
    val blurShadow = remember(fontSize, density) {
        Shadow(
            color = VaultArtShadowBlur,
            offset = with(density) { Offset(emOffset(0.073f).toPx(), emOffset(0.073f).toPx()) },
            blurRadius = with(density) { emOffset(0.17f).toPx() }
        )
    }
    val vermOffset = remember(fontSize, density) { emOffset(0.049f) }
    val inkOffset = remember(fontSize, density) { emOffset(0.024f) }
    val style = remember(fontSize, fontFamily, fontWeight) {
        TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize,
            lineHeight = fontSize
        )
    }
    // 三层错位影只给 ≥36sp 展示级大字（如释放空间大数字）；页眉/空态标题仅保留一层清爽朱红错位
    val fullArtShadow = fontSize.value >= VAULT_FULL_ART_SHADOW_MIN_SP
    Box(modifier) {
        // 错位阴影层对无障碍隐藏，仅顶层正稿参与语义
        if (fullArtShadow) {
            Text(
                text = text,
                color = Color.Transparent,
                style = style.copy(shadow = blurShadow),
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        Text(
            text = text,
            color = verm.copy(alpha = if (fullArtShadow) 0.60f else 0.42f),
            style = style,
            modifier = Modifier
                .offset(x = vermOffset, y = vermOffset)
                .clearAndSetSemantics { }
        )
        if (fullArtShadow) {
            Text(
                text = text,
                color = VaultArtShadowBlack,
                style = style,
                modifier = Modifier
                    .offset(x = inkOffset, y = inkOffset)
                    .clearAndSetSemantics { }
            )
        }
        Text(text = text, color = color, style = style)
    }
}

/** 放射太阳纹：24 扇形（金 alpha ≤0.16，画报地面纹样） */
@Composable
private fun VaultSunburst(
    diameter: Dp,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val gold = DecoGold
    Canvas(modifier.size(diameter)) {
        val wedgeSweep = 360f / VaultSunburstWedges
        val raySweep = wedgeSweep / 2.4f
        repeat(VaultSunburstWedges) { i ->
            drawArc(
                color = gold.copy(alpha = alpha),
                startAngle = i * wedgeSweep,
                sweepAngle = raySweep,
                useCenter = true
            )
        }
    }
}

/** 纸纹地面：主题 deco 底渐变 + 横向纸纹线 + 右上太阳纹 */
@Composable
private fun VaultDecoGround(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            // 纸纹横线走共享缓存绘制（原型 repeating-linear-gradient 3px/4px，ink 0.016）
            .decoPaperGroundBackground(DecoGround, DecoGroundDeep, DecoInkOnGround)
    ) {
        // 太阳纹收进报头装饰带：缩小并上移右移，下缘止于标题区，不再横穿汇总卡与空态
        VaultSunburst(
            diameter = 176.dp,
            alpha = 0.12f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 58.dp, y = (-64).dp)
        )
        content()
    }
}

/** 金边通栏按钮：1.5dp 金描边 + 金字（全部还原） */
@Composable
private fun VaultDecoOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIconKind: DecoIconKind? = null
) {
    val gold = DecoGold
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(VaultDecoShape)
            .background(gold.copy(alpha = 0.10f))
            .border(1.5.dp, gold, VaultDecoShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIconKind != null) {
                DecoIcon(
                    kind = leadingIconKind,
                    color = gold,
                    iconSize = 16.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontFamily = DecoDisplayFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = gold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 朱红通栏按钮：朱红底 + 纸黄字 + 顶缘米金高光（确认清理） */
@Composable
private fun VaultDecoVermilionButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    leadingIconKind: DecoIconKind? = null
) {
    val verm = DecoVerm
    val cream = DecoPaperCream
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .shadow(
                elevation = 10.dp,
                shape = VaultDecoShape,
                ambientColor = verm.copy(alpha = 0.45f),
                spotColor = verm.copy(alpha = 0.45f)
            )
            .clip(VaultDecoShape)
            .background(verm)
            .drawBehind {
                // 朱红通栏顶缘米金高光（原型 inset 0 1px 0 rgba(239,227,200,0.22)）
                drawLine(
                    color = cream.copy(alpha = 0.22f),
                    start = Offset(0f, 0.75.dp.toPx()),
                    end = Offset(size.width, 0.75.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIconKind != null) {
                DecoIcon(
                    kind = leadingIconKind,
                    color = cream,
                    iconSize = 16.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontFamily = DecoDisplayFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = cream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!badgeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = VaultTileShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, cream.copy(alpha = 0.32f))
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cream.copy(alpha = 0.92f),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
