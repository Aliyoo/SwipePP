package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.sensor.ShakeDetector
import com.example.sensor.rememberHapticEngine
import com.example.ui.CleanerEntrySource
import com.example.ui.CleanupCardItem
import com.example.ui.components.ArcTimeScaleScrubber
import com.example.ui.components.DecoIcon
import com.example.ui.components.DecoIconKind
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.DecoOrbTone
import com.example.ui.components.DecoPaperGround
import com.example.ui.components.DecoSealButton
import com.example.ui.components.DecoSealOrb
import com.example.ui.components.DecoSheetDragHandle
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.formatPhotoDateTime
import com.example.ui.components.formatStorageSize
import com.example.ui.components.modalSheetTopClearance
import com.example.ui.components.formatVideoDuration
import com.example.ui.screens.cleaner.BurstComparisonSheet
import com.example.ui.screens.cleaner.CleanupCompletedCelebration
import com.example.ui.screens.cleaner.DecoVermStripButton
import com.example.ui.screens.cleaner.ModernArcSwipeCard
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoGroundDeep
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoVermInk
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.TimelineSortOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun CleanupCardItem.matchesDay(day: Int): Boolean = when (this) {
    is CleanupCardItem.SinglePhoto -> photo.day == day
    is CleanupCardItem.BurstStack -> primaryPhoto.day == day || photos.any { it.day == day }
}

private data class SessionDecisionRecord(
    val cardKey: String,
    val keptDelta: Int = 0,
    val favoritedDelta: Int = 0,
    val trashedIds: Set<Long> = emptySet(),
    val trashedBytes: Long = 0L
)

// ---------------------------------------------------------------------------
// 摩登画报 · 屏内一次性构件（design-demos/deco-app.html：PaperGround / SealBtn / 印钮）
// ---------------------------------------------------------------------------

/** 干支纪年副标：2026丙午 2025乙巳 2024甲辰 2023癸卯 2022壬寅（含通用六十甲子推算） */
private fun ganzhiOfYear(year: Int): String {
    if (year < 1) return ""
    val stems = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val branches = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val i = Math.floorMod(year - 4, 10)
    val j = Math.floorMod(year - 4, 12)
    return "${stems[i]}${branches[j]}"
}

// 纸地、方章与决断印钮收敛到共享组件 com.example.ui.components.DecoPaperGround /
// DecoSealButton / DecoSealOrb（纸纹 alpha 统一 0.016；方章衬线金墨字 + 48dp 命中区；
// 印钮金环/朱红实印三态，弧形清理与灯箱共用）

/** 手势提示短句：色条下划线（回收=朱红警示字 / 保留=金） */
@Composable
private fun DecoHintSpan(text: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            letterSpacing = 1.sp,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color.copy(alpha = 0.6f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcCleanerScreen(
    title: String,
    monthNumber: Int?,
    queue: List<CleanupCardItem>,
    undoStackDepth: Int,
    onBack: () -> Unit,
    onKeepPhoto: (PhotoItemEntity, Boolean) -> Unit,
    onTrashPhoto: (PhotoItemEntity) -> Unit,
    onKeepEntireStack: (CleanupCardItem.BurstStack, Boolean) -> Unit,
    onKeepBestInStack: (CleanupCardItem.BurstStack, PhotoItemEntity) -> Unit,
    onTrashEntireStack: (CleanupCardItem.BurstStack) -> Unit,
    onSelectBestInStack: (String, Long) -> Unit,
    onUndoSession: (Boolean) -> String?,
    onOpenVaultForSession: (Set<Long>) -> Unit = {},
    includeReviewed: Boolean = false,
    entrySource: CleanerEntrySource = CleanerEntrySource.BOOKSHELF,
    initialFocusDay: Int? = null,
    timelineSortOrder: TimelineSortOrder = TimelineSortOrder.NEWEST_FIRST,
    onToggleFavorite: (PhotoItemEntity, Boolean) -> Unit = { p, f -> onKeepPhoto(p, f) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHapticEngine()
    val scope = rememberCoroutineScope()

    var processedCardKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sessionHistory by remember { mutableStateOf<List<SessionDecisionRecord>>(emptyList()) }
    val sessionKeptCount = remember(sessionHistory) { sessionHistory.sumOf { it.keptDelta } }
    val sessionFavoritedCount = remember(sessionHistory) { sessionHistory.sumOf { it.favoritedDelta } }
    val sessionTrashedIds = remember(sessionHistory) {
        sessionHistory.flatMapTo(LinkedHashSet()) { it.trashedIds }
    }
    val sessionTrashedBytes = remember(sessionHistory) { sessionHistory.sumOf { it.trashedBytes } }

    fun recordDecision(record: SessionDecisionRecord) {
        processedCardKeys = processedCardKeys + record.cardKey
        sessionHistory = sessionHistory + record
    }

    var onlyFocusedDay by rememberSaveable(initialFocusDay) { mutableStateOf(false) }

    val activeSessionQueue = remember(queue, processedCardKeys) {
        if (processedCardKeys.isEmpty()) queue
        else queue.filter { it.stableKey !in processedCardKeys }
    }

    val focusedDayCount = remember(activeSessionQueue, initialFocusDay) {
        if (initialFocusDay != null) activeSessionQueue.count { it.matchesDay(initialFocusDay) } else 0
    }
    val displayedQueue = remember(activeSessionQueue, initialFocusDay, onlyFocusedDay) {
        if (initialFocusDay != null && onlyFocusedDay) {
            activeSessionQueue.filter { it.matchesDay(initialFocusDay) }
        } else {
            activeSessionQueue
        }
    }

    var inspectingStack by remember { mutableStateOf<CleanupCardItem.BurstStack?>(null) }
    var lightboxPhoto by remember { mutableStateOf<PhotoItemEntity?>(null) }
    var lightboxContextPhotos by remember { mutableStateOf<List<PhotoItemEntity>>(emptyList()) }
    var resumeBurstGroupIdAfterLightbox by remember { mutableStateOf<String?>(null) }
    var shakeVisualPulse by remember { mutableStateOf(false) }
    var previousQueueKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingRestoreFocusKey by remember { mutableStateOf<String?>(null) }
    var awaitingUndoRestore by remember { mutableStateOf(false) }
    val initialQueueCount = remember(onlyFocusedDay, queue.isNotEmpty()) {
        (displayedQueue.size + processedCardKeys.size).coerceAtLeast(1)
    }

    fun performSessionUndo(triggeredByShake: Boolean): Boolean {
        val restoredKey = onUndoSession(triggeredByShake) ?: return false
        awaitingUndoRestore = true
        processedCardKeys = processedCardKeys - restoredKey
        val lastIdx = sessionHistory.indexOfLast { it.cardKey == restoredKey }
        if (lastIdx >= 0) {
            sessionHistory = sessionHistory.toMutableList().also { it.removeAt(lastIdx) }
        } else if (sessionHistory.isNotEmpty()) {
            sessionHistory = sessionHistory.dropLast(1)
        }
        pendingRestoreFocusKey = restoredKey
        return true
    }

    val handleTrashSingle: (PhotoItemEntity) -> Unit = { photo ->
        recordDecision(
            SessionDecisionRecord(
                cardKey = "single_${photo.id}",
                trashedIds = setOf(photo.id),
                trashedBytes = photo.fileSizeBytes
            )
        )
        onTrashPhoto(photo)
    }
    val handleKeepSingle: (PhotoItemEntity, Boolean) -> Unit = { photo, favorite ->
        recordDecision(
            SessionDecisionRecord(
                cardKey = "single_${photo.id}",
                keptDelta = 1,
                favoritedDelta = if (favorite) 1 else 0
            )
        )
        onKeepPhoto(photo, favorite)
    }
    val handleKeepEntireStack: (CleanupCardItem.BurstStack, Boolean) -> Unit = { stack, favorite ->
        recordDecision(
            SessionDecisionRecord(
                cardKey = stack.stableKey,
                keptDelta = stack.photos.size,
                favoritedDelta = if (favorite) 1 else 0
            )
        )
        onKeepEntireStack(stack, favorite)
    }
    val handleKeepBestInStack: (CleanupCardItem.BurstStack, PhotoItemEntity) -> Unit = { stack, chosen ->
        val others = stack.photos.filter { it.id != chosen.id }
        recordDecision(
            SessionDecisionRecord(
                cardKey = stack.stableKey,
                keptDelta = 1,
                trashedIds = others.mapTo(HashSet()) { it.id },
                trashedBytes = others.sumOf { it.fileSizeBytes }
            )
        )
        onKeepBestInStack(stack, chosen)
    }
    val handleTrashEntireStack: (CleanupCardItem.BurstStack) -> Unit = { stack ->
        recordDecision(
            SessionDecisionRecord(
                cardKey = stack.stableKey,
                trashedIds = stack.photos.mapTo(HashSet()) { it.id },
                trashedBytes = stack.totalBytes
            )
        )
        onTrashEntireStack(stack)
    }

    val shakeDetector = remember(context) { ShakeDetector(context) }
    LaunchedEffect(shakeDetector) {
        shakeDetector.shakeEvents.collect {
            val undone = performSessionUndo(triggeredByShake = true)
            if (undone) {
                haptics.undoPulse()
                shakeVisualPulse = true
                delay(1600)
                shakeVisualPulse = false
            }
        }
    }

    LaunchedEffect(displayedQueue) {
        val currentKeys = displayedQueue.map { it.stableKey }
        if (awaitingUndoRestore && currentKeys.size > previousQueueKeys.size) {
            val prevSet = previousQueueKeys.toHashSet()
            val newlyRestored = currentKeys.firstOrNull { it !in prevSet }
            if (newlyRestored != null) {
                pendingRestoreFocusKey = newlyRestored
            }
            awaitingUndoRestore = false
        }
        previousQueueKeys = currentKeys

        val currentInspect = inspectingStack ?: return@LaunchedEffect
        val updated = displayedQueue.filterIsInstance<CleanupCardItem.BurstStack>()
            .firstOrNull { it.groupId == currentInspect.groupId }
        inspectingStack = updated
    }

    DecoPaperGround(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar — 方章返回/撤销 + 画报题名 + 已释放空间朱红计费章
            val isDarkHeader = LocalIsDarkTheme.current
            val vermInk = DecoVermInk
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    DecoSealButton(
                        labelTop = "返",
                        labelBottom = "回",
                        onClick = onBack,
                        modifier = Modifier.testTag("cleaner_back_button")
                    )
                    Column {
                        Text(
                            text = title,
                            fontSize = 22.sp,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DecoInkOnGround,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                displayedQueue.isEmpty() -> "本次整理完毕"
                                onlyFocusedDay && initialFocusDay != null -> "${initialFocusDay}日 · 剩余 ${displayedQueue.size} 项"
                                includeReviewed -> "重温模式 · 剩余 ${displayedQueue.size} 项"
                                else -> "剩余 ${displayedQueue.size} 项待整理"
                            },
                            fontSize = 12.sp,
                            fontFamily = DecoDisplayFontFamily,
                            letterSpacing = 0.5.sp,
                            color = DecoGold,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            }
                        )
                    }
                }

                // Dynamic Session Reclaimable Storage Chip (deco 画片章 · 等价迁移)
                val hasTrashedInSession = sessionTrashedBytes > 0L
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = DecoMat.copy(alpha = 0.9f),
                    border = BorderStroke(
                        1.dp,
                        if (hasTrashedInSession) DecoVerm.copy(alpha = 0.6f) else DecoGold.copy(alpha = 0.55f)
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier.testTag("dynamic_storage_counter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (hasTrashedInSession) DecoVerm else DecoGold.copy(alpha = 0.45f))
                        )
                        Text(
                            text = formatStorageSize(sessionTrashedBytes),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (hasTrashedInSession) vermInk else DecoInkOnGround.copy(alpha = 0.72f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                DecoSealButton(
                    labelTop = "撤",
                    labelBottom = "销",
                    onClick = {
                        if (performSessionUndo(triggeredByShake = false)) {
                            haptics.undoPulse()
                        }
                    },
                    enabled = undoStackDepth > 0,
                    modifier = Modifier.testTag("cleaner_undo_button")
                )
            }

            // 金刻度进度：24 格金尺（已决=实金 / 未决=弱金）
            if (initialQueueCount > 0) {
                val progressTarget = (1f - (displayedQueue.size.toFloat() / initialQueueCount.toFloat())).coerceIn(0.04f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = if (displayedQueue.isEmpty()) 1f else progressTarget,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "cleaner_progress"
                )
                val segmentCount = 24
                val filledSegments = (animatedProgress * segmentCount).roundToInt().coerceIn(0, segmentCount)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(segmentCount) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (index < filledSegments || displayedQueue.isEmpty()) DecoGold
                                    else DecoGold.copy(alpha = 0.22f)
                                )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = shakeVisualPulse,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                // 摇一摇撤销 · 朱红通栏脉冲 toast（纸黄字压朱红底）
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = DecoVerm,
                    border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.22f)),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DecoIcon(
                            kind = DecoIconKind.PHONE,
                            color = DecoPaperCream,
                            iconSize = 15.dp
                        )
                        Text(
                            text = stringResource(R.string.shake_undo_toast),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = DecoDisplayFontFamily,
                            color = DecoPaperCream
                        )
                    }
                }
            }

            if (displayedQueue.isEmpty()) {
                CleanupCompletedCelebration(
                    title = title,
                    monthNumber = monthNumber,
                    completedDay = if (onlyFocusedDay) initialFocusDay else null,
                    remainingInFullQueue = if (onlyFocusedDay) activeSessionQueue.size else 0,
                    sessionKeptCount = sessionKeptCount,
                    sessionFavoritedCount = sessionFavoritedCount,
                    sessionTrashedCount = sessionTrashedIds.size,
                    sessionTrashedBytes = sessionTrashedBytes,
                    entrySource = entrySource,
                    onContinueFullMonth = { onlyFocusedDay = false },
                    onOpenVault = { onOpenVaultForSession(sessionTrashedIds) },
                    onDone = onBack,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val initialFocusIndex = remember(initialFocusDay) {
                    if (initialFocusDay != null) {
                        displayedQueue.indexOfFirst { it.matchesDay(initialFocusDay) }.coerceAtLeast(0)
                    } else {
                        0
                    }
                }
                val pagerState = rememberPagerState(
                    initialPage = initialFocusIndex.coerceIn(0, displayedQueue.lastIndex),
                    pageCount = { displayedQueue.size }
                )
                var hasAppliedInitialFocus by remember(initialFocusDay) {
                    mutableStateOf(initialFocusDay == null || displayedQueue.isNotEmpty())
                }
                var previousOnlyFocusedDay by remember(initialFocusDay) { mutableStateOf(onlyFocusedDay) }

                val safePage = pagerState.currentPage.coerceIn(0, displayedQueue.lastIndex)
                val activeItem = displayedQueue[safePage]

                LaunchedEffect(displayedQueue.size, pendingRestoreFocusKey, onlyFocusedDay) {
                    if (displayedQueue.isEmpty()) return@LaunchedEffect
                    val restoreKey = pendingRestoreFocusKey
                    if (restoreKey != null) {
                        val restoreIdx = displayedQueue.indexOfFirst { it.stableKey == restoreKey }
                        pendingRestoreFocusKey = null
                        if (restoreIdx in displayedQueue.indices && restoreIdx != pagerState.currentPage) {
                            runCatching { pagerState.scrollToPage(restoreIdx) }
                        }
                    } else if (onlyFocusedDay != previousOnlyFocusedDay) {
                        previousOnlyFocusedDay = onlyFocusedDay
                        val targetIdx = if (!onlyFocusedDay && initialFocusDay != null) {
                            displayedQueue.indexOfFirst { it.matchesDay(initialFocusDay) }.coerceAtLeast(0)
                        } else {
                            0
                        }
                        if (targetIdx in displayedQueue.indices) {
                            runCatching { pagerState.scrollToPage(targetIdx) }
                        }
                    } else if (!hasAppliedInitialFocus && initialFocusDay != null) {
                        hasAppliedInitialFocus = true
                        val targetIdx = displayedQueue.indexOfFirst { it.matchesDay(initialFocusDay) }.coerceAtLeast(0)
                        if (targetIdx in displayedQueue.indices && targetIdx != pagerState.currentPage) {
                            runCatching { pagerState.scrollToPage(targetIdx) }
                        }
                    } else if (pagerState.currentPage >= displayedQueue.size) {
                        runCatching { pagerState.scrollToPage(displayedQueue.lastIndex.coerceAtLeast(0)) }
                    }
                }

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .drop(1)
                        .distinctUntilChanged()
                        .collect {
                            haptics.pageFlipLift()
                        }
                }

                // Frameless Live Date Status Strip & Optional Single-Day Isolation Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(DecoGround.copy(alpha = 0.42f))
                            .border(1.dp, DecoGold.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("cleaner_active_date_pill")
                    ) {
                        DecoIcon(
                            kind = DecoIconKind.CALENDAR,
                            color = DecoGold,
                            iconSize = 12.dp
                        )
                        Text(
                            text = "${activeItem.primaryPhoto.month}月${activeItem.primaryPhoto.day}日 · ${safePage + 1}/${displayedQueue.size}",
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = DecoInkOnGround
                        )
                    }

                    if (initialFocusDay != null && (focusedDayCount > 0 || onlyFocusedDay)) {
                        val onGold = if (isDarkHeader) DecoGround else DecoMat
                        Surface(
                            onClick = { onlyFocusedDay = !onlyFocusedDay },
                            shape = RoundedCornerShape(3.dp),
                            color = if (onlyFocusedDay) DecoGold else Color.Transparent,
                            border = BorderStroke(1.dp, if (onlyFocusedDay) DecoGold else DecoGold.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("cleaner_day_filter_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DecoIcon(
                                    kind = DecoIconKind.FILTER,
                                    color = if (onlyFocusedDay) onGold else DecoGold,
                                    iconSize = 12.dp
                                )
                                Text(
                                    text = if (onlyFocusedDay) {
                                        "仅看 ${initialFocusDay}日 · 切回整月"
                                    } else {
                                        "仅看 ${initialFocusDay}日 ($focusedDayCount)"
                                    },
                                    fontSize = 12.sp,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (onlyFocusedDay) onGold else DecoInkOnGround
                                )
                            }
                        }
                    }
                }

                // Curved Chronological Arc Stage
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // 卡宽同时受宽与高约束：卡比例 0.75（宽:高），纵向溢出会压住日期 pill 与金刻度
                    val cardWidth = minOf(
                        (maxWidth * 0.75f).coerceIn(244.dp, 326.dp),
                        ((maxHeight * 0.92f) * 0.75f).coerceAtLeast(244.dp),
                    )
                    val sidePad = ((maxWidth - cardWidth) / 2f).coerceAtLeast(24.dp)

                    HorizontalPager(
                        state = pagerState,
                        pageSize = PageSize.Fixed(cardWidth),
                        contentPadding = PaddingValues(horizontal = sidePad),
                        pageSpacing = 12.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("arc_timeline_pager")
                    ) { index ->
                        val item = displayedQueue.getOrNull(index) ?: return@HorizontalPager
                        val isFocused = index == safePage

                        key(item.stableKey) {
                            ModernArcSwipeCard(
                                item = item,
                                pageOffsetProvider = {
                                    val raw = pagerState.currentPageOffsetFraction
                                    val safeFrac = if (raw.isFinite()) raw else 0f
                                    (pagerState.currentPage - index) + safeFrac
                                },
                                isFocused = isFocused,
                                isVideoPlaybackActive = isFocused && lightboxPhoto == null,
                                haptics = haptics,
                                onSwipeUpTrash = {
                                    haptics.swipeDeleteConfirm()
                                    when (item) {
                                        is CleanupCardItem.SinglePhoto -> handleTrashSingle(item.photo)
                                        is CleanupCardItem.BurstStack -> handleTrashEntireStack(item)
                                    }
                                },
                                onSwipeDownKeep = {
                                    haptics.swipeKeepConfirm()
                                    when (item) {
                                        is CleanupCardItem.SinglePhoto -> handleKeepSingle(item.photo, false)
                                        is CleanupCardItem.BurstStack -> handleKeepEntireStack(item, false)
                                    }
                                },
                                onFavoriteAction = {
                                    haptics.swipeKeepConfirm()
                                    when (item) {
                                        is CleanupCardItem.SinglePhoto -> handleKeepSingle(item.photo, true)
                                        is CleanupCardItem.BurstStack -> handleKeepEntireStack(item, true)
                                    }
                                },
                                onTapCard = {
                                    when {
                                        !isFocused -> scope.launch {
                                            if (index in displayedQueue.indices) {
                                                runCatching { pagerState.animateScrollToPage(index) }
                                            }
                                        }
                                        item is CleanupCardItem.BurstStack -> inspectingStack = item
                                        item is CleanupCardItem.SinglePhoto -> {
                                            resumeBurstGroupIdAfterLightbox = null
                                            lightboxContextPhotos = displayedQueue.map { it.primaryPhoto }
                                            lightboxPhoto = item.photo
                                        }
                                    }
                                },
                                onOpenFullscreen = { photo ->
                                    resumeBurstGroupIdAfterLightbox = null
                                    lightboxContextPhotos = if (item is CleanupCardItem.BurstStack) {
                                        item.photos
                                    } else {
                                        displayedQueue.map { it.primaryPhoto }
                                    }
                                    lightboxPhoto = photo
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Interactive "Arc as Time Scale" scrubber with Burst markers
                ArcTimeScaleScrubber(
                    queue = displayedQueue,
                    currentIndex = safePage,
                    sortOrder = timelineSortOrder,
                    onJumpToIndex = { targetIdx ->
                        if (targetIdx in displayedQueue.indices && targetIdx != pagerState.currentPage) {
                            scope.launch {
                                runCatching { pagerState.scrollToPage(targetIdx) }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )

                // 卡片下日期地点一行（干支纪年副标 + 日期 · 地点 · 视频 · 体积）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    val ganzhi = ganzhiOfYear(activeItem.primaryPhoto.year)
                    if (ganzhi.isNotEmpty()) {
                        Text(
                            text = "${ganzhi}年",
                            fontSize = 12.sp,
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = DecoGold
                        )
                        Text(text = "·", fontSize = 12.sp, color = DecoGold.copy(alpha = 0.6f))
                    }
                    Text(
                        text = formatPhotoDateTime(activeItem.primaryPhoto.takenAtMillis),
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DecoInkOnGround
                    )
                    activeItem.primaryPhoto.locationName?.let { loc ->
                        Text(
                            text = loc,
                            fontSize = 12.sp,
                            fontFamily = DecoDisplayFontFamily,
                            color = DecoInkOnGround.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (activeItem.primaryPhoto.isVideo) {
                        Text(
                            text = "视频 ${formatVideoDuration(activeItem.primaryPhoto.durationMs)}",
                            fontFamily = DecoDisplayFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DecoGold
                        )
                    }
                    Text(
                        text = formatStorageSize(activeItem.totalBytes),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = DecoInkOnGround.copy(alpha = 0.72f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fixed-height Action Deck: eliminates vertical layout shift when swiping between
                // SinglePhoto and BurstStack items!
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp)
                ) {
                    AnimatedContent(
                        targetState = activeItem,
                        contentKey = { it is CleanupCardItem.BurstStack },
                        transitionSpec = {
                            fadeIn(tween(170)) togetherWith fadeOut(tween(130))
                        },
                        label = "arc_action_deck"
                    ) { current ->
                        when (current) {
                            is CleanupCardItem.BurstStack -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 朱红通栏主 CTA：最佳帧缩图 + 画质/省空间徽章
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(DecoVerm)
                                            .border(1.dp, DecoPaperCream.copy(alpha = 0.22f), RoundedCornerShape(3.dp))
                                            .clickable {
                                                haptics.swipeDeleteConfirm()
                                                handleKeepBestInStack(current, current.bestPhoto)
                                            }
                                            .padding(horizontal = 10.dp)
                                            .testTag("keep_best_burst_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .border(1.2.dp, DecoGold, RoundedCornerShape(2.dp))
                                                    .clickable {
                                                        resumeBurstGroupIdAfterLightbox = null
                                                        lightboxContextPhotos = current.photos
                                                        lightboxPhoto = current.bestPhoto
                                                    }
                                                    .testTag("burst_best_thumb_preview")
                                            ) {
                                                PhotoArtwork(
                                                    photo = current.bestPhoto,
                                                    contentDescription = "点击查看大图预览最佳帧",
                                                    showVideoBadge = false,
                                                    downsampleTier = ImageDownsampleTier.GRID_THUMB,
                                                    targetSizePx = 128,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "仅保留最佳帧",
                                                    fontSize = 19.sp,
                                                    fontFamily = DecoDisplayFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    color = DecoPaperCream,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "画质 ${current.bestPhoto.qualityScore} · 其余 ${current.photos.size - 1} 张进回收站",
                                                    fontSize = 12.sp,
                                                    fontFamily = DecoDisplayFontFamily,
                                                    color = DecoPaperCream.copy(alpha = 0.78f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(2.dp),
                                                color = DecoPaperCream.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.7f))
                                            ) {
                                                Text(
                                                    text = "省 ${formatStorageSize(current.nonBestBytes)}",
                                                    fontFamily = DecoDisplayFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = DecoPaperCream,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        GalleryActionButton(
                                            text = "全部保留",
                                            onClick = {
                                                haptics.swipeKeepConfirm()
                                                handleKeepEntireStack(current, false)
                                            },
                                            tone = GalleryButtonTone.IMMERSIVE,
                                            // 次要钮去图标：三钮等分窄屏放不下「整组回收」，图标让位给朱红主 CTA
                                            height = 48.dp,
                                            shape = RoundedCornerShape(3.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("keep_all_burst_button")
                                        )
                                        GalleryActionButton(
                                            text = "对比(${current.photos.size})",
                                            onClick = { inspectingStack = current },
                                            tone = GalleryButtonTone.IMMERSIVE,
                                            height = 48.dp,
                                            shape = RoundedCornerShape(3.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("compare_burst_button")
                                        )
                                        DecoVermStripButton(
                                            title = "整组回收",
                                            leadingIconKind = DecoIconKind.DELETE,
                                            height = 48.dp,
                                            onClick = {
                                                haptics.swipeDeleteConfirm()
                                                handleTrashEntireStack(current)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("trash_burst_button")
                                        )
                                    }
                                }
                            }

                            is CleanupCardItem.SinglePhoto -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 28.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 入站：朱红实印
                                    DecoSealOrb(
                                        label = "回收",
                                        orbSize = 64.dp,
                                        tone = DecoOrbTone.VERM_SOLID,
                                        onClick = {
                                            haptics.swipeDeleteConfirm()
                                            handleTrashSingle(current.photo)
                                        },
                                        modifier = Modifier.testTag("arc_trash_button")
                                    )

                                    // 收藏：金环小章（原型印钮纯美术字，无图标）
                                    DecoSealOrb(
                                        label = "收藏",
                                        orbSize = 54.dp,
                                        tone = DecoOrbTone.GOLD_RING,
                                        onClick = {
                                            haptics.swipeKeepConfirm()
                                            handleKeepSingle(current.photo, true)
                                        },
                                        modifier = Modifier.testTag("arc_favorite_button")
                                    )

                                    // 保留：金环印钮（外圈余辉）
                                    DecoSealOrb(
                                        label = "保留",
                                        orbSize = 64.dp,
                                        tone = DecoOrbTone.GOLD_RING_HALO,
                                        onClick = {
                                            haptics.swipeKeepConfirm()
                                            handleKeepSingle(current.photo, false)
                                        },
                                        modifier = Modifier.testTag("arc_keep_button")
                                    )
                                }
                            }
                        }
                    }
                }

                // Auto-fading gesture hint: smoothly retracts after 2 decisions to give the card stage maximum room
                AnimatedVisibility(
                    visible = sessionHistory.size < 2,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(260)) + shrinkVertically(tween(260))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    ) {
                        DecoIcon(
                            kind = DecoIconKind.SWIPE_UP,
                            color = DecoGold,
                            iconSize = 13.dp
                        )
                        DecoHintSpan(text = "上滑回收", color = vermInk)
                        Text(text = "·", fontSize = 12.5.sp, color = DecoInkOnGround.copy(alpha = 0.5f))
                        DecoHintSpan(text = "下滑保留", color = DecoGold)
                        Text(text = "·", fontSize = 12.5.sp, color = DecoInkOnGround.copy(alpha = 0.5f))
                        Text(
                            text = "摇一摇撤销",
                            fontSize = 12.5.sp,
                            letterSpacing = 1.sp,
                            color = DecoInkOnGround.copy(alpha = 0.72f)
                        )
                    }
                }
                if (sessionHistory.size >= 2) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    lightboxPhoto?.let { photo ->
        val flatPhotos = remember(lightboxContextPhotos, displayedQueue) {
            lightboxContextPhotos.ifEmpty { displayedQueue.map { it.primaryPhoto } }
        }
        val startIdx = remember(photo, flatPhotos) {
            flatPhotos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        }
        val isFromBurstStack = resumeBurstGroupIdAfterLightbox != null ||
            displayedQueue.any { it is CleanupCardItem.BurstStack && it.photos.any { p -> p.id == photo.id } }
        PhotoDetailLightbox(
            photo = photo,
            photos = flatPhotos,
            initialIndex = startIdx,
            onDismiss = {
                lightboxPhoto = null
                val resumeId = resumeBurstGroupIdAfterLightbox
                resumeBurstGroupIdAfterLightbox = null
                if (resumeId != null) {
                    inspectingStack = displayedQueue.filterIsInstance<CleanupCardItem.BurstStack>()
                        .firstOrNull { it.groupId == resumeId }
                }
            },
            onKeep = { target, fav ->
                if (isFromBurstStack) {
                    onKeepPhoto(target, fav)
                } else {
                    handleKeepSingle(target, fav)
                }
            },
            onTrash = { target ->
                if (isFromBurstStack) {
                    onTrashPhoto(target)
                } else {
                    handleTrashSingle(target)
                }
            },
            onToggleFavorite = onToggleFavorite
        )
    }

    inspectingStack?.let { stack ->
        ModalBottomSheet(
            onDismissRequest = { inspectingStack = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DecoMat,
            dragHandle = { DecoSheetDragHandle() },
            modifier = Modifier.modalSheetTopClearance()
        ) {
            BurstComparisonSheet(
                stack = stack,
                onConfirmKeepBest = { chosen ->
                    inspectingStack = null
                    handleKeepBestInStack(stack, chosen)
                },
                onInspectPhoto = { targetPhoto ->
                    // Dismiss sheet window temporarily so FullScreenOverlayHost Lightbox renders cleanly on top,
                    // then restore BurstComparisonSheet upon Lightbox dismissal
                    resumeBurstGroupIdAfterLightbox = stack.groupId
                    inspectingStack = null
                    lightboxContextPhotos = stack.photos
                    lightboxPhoto = targetPhoto
                }
            )
        }
    }
}
