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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.ArcTimeScaleScrubber
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GalleryHeaderOrbButton
import com.example.ui.components.GalleryTactileOrbButton
import com.example.ui.components.ModalSheetDragHandle
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.activeChromeSatinBrush
import com.example.ui.components.formatPhotoDateTime
import com.example.ui.components.formatStorageSize
import com.example.ui.components.formatVideoDuration
import com.example.ui.components.modalSheetTopClearance
import com.example.ui.theme.GlassSpecularRimStart
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.screens.cleaner.BurstComparisonSheet
import com.example.ui.screens.cleaner.CleanupCompletedCelebration
import com.example.ui.screens.cleaner.ModernArcSwipeCard
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralSoft
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberSoft
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.VioletVideo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

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

    ArtisticGalleryBackdrop(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar — unified frosted glass orbs + glowing coral telemetry pill
            val isDarkHeader = LocalIsDarkTheme.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GalleryHeaderOrbButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("cleaner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = InkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
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
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            }
                        )
                    }
                }

                // Dynamic Session Reclaimable Storage Micro-Pill (aligned with Bookshelf ledger pill)
                val hasTrashedInSession = sessionTrashedBytes > 0L
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isDarkHeader) SurfacePure.copy(alpha = 0.78f) else SurfacePure.copy(alpha = 0.92f),
                    border = BorderStroke(
                        0.85.dp,
                        if (hasTrashedInSession) CoralDanger.copy(alpha = 0.42f)
                        else if (isDarkHeader) HairlineBorder else GlassSpecularRimStart
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
                                .background(if (hasTrashedInSession) CoralDanger else InkTertiary.copy(alpha = 0.45f))
                        )
                        Text(
                            text = formatStorageSize(sessionTrashedBytes),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (hasTrashedInSession) CoralDanger else InkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                GalleryHeaderOrbButton(
                    onClick = {
                        if (performSessionUndo(triggeredByShake = false)) {
                            haptics.undoPulse()
                        }
                    },
                    enabled = undoStackDepth > 0,
                    modifier = Modifier.testTag("cleaner_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.cd_undo),
                        tint = if (undoStackDepth > 0) CobaltPrimary else InkTertiary.copy(alpha = 0.45f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Razor-thin animated cleanup progress indicator
            if (initialQueueCount > 0) {
                val progressTarget = (1f - (displayedQueue.size.toFloat() / initialQueueCount.toFloat())).coerceIn(0.04f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = if (displayedQueue.isEmpty()) 1f else progressTarget,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "cleaner_progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 4.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(SurfaceMuted)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(if (displayedQueue.isEmpty()) EmeraldKeep else CobaltPrimary)
                    )
                }
            }

            AnimatedVisibility(
                visible = shakeVisualPulse,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CobaltPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.shake_undo_toast),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
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
                            .padding(vertical = 4.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("cleaner_active_date_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${activeItem.primaryPhoto.month}月${activeItem.primaryPhoto.day}日 · ${safePage + 1}/${displayedQueue.size}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = InkSecondary
                        )
                    }

                    if (initialFocusDay != null && (focusedDayCount > 0 || onlyFocusedDay)) {
                        Surface(
                            onClick = { onlyFocusedDay = !onlyFocusedDay },
                            shape = RoundedCornerShape(50),
                            color = if (onlyFocusedDay) CobaltPrimary else CobaltSoft.copy(alpha = 0.72f),
                            modifier = Modifier.testTag("cleaner_day_filter_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = null,
                                    tint = if (onlyFocusedDay) Color.White else CobaltPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (onlyFocusedDay) {
                                        "仅看 ${initialFocusDay}日 · 切回整月"
                                    } else {
                                        "仅看 ${initialFocusDay}日 ($focusedDayCount)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (onlyFocusedDay) Color.White else CobaltPrimary
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
                    val cardWidth = (maxWidth * 0.75f).coerceIn(244.dp, 326.dp)
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

                // Crisp Date & Metadata Stamp below the Arc
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = formatPhotoDateTime(activeItem.primaryPhoto.takenAtMillis),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeItem.primaryPhoto.locationName?.let { loc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = loc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                        }
                        if (activeItem.primaryPhoto.isVideo) {
                            Text(
                                text = "视频 ${formatVideoDuration(activeItem.primaryPhoto.durationMs)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VioletVideo
                            )
                        }
                        Text(
                            text = formatStorageSize(activeItem.totalBytes),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = InkSecondary
                        )
                    }
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
                                    // Sculpted Satin Obsidian primary CTA with best-frame preview & savings pill
                                    val palette = LocalFlipYearPalette.current
                                    val satinBrush = activeChromeSatinBrush()
                                    val burstShape = RoundedCornerShape(18.dp)
                                    val burstBorder = remember(palette) {
                                        Brush.linearGradient(
                                            listOf(
                                                palette.activeChromeBorderStart,
                                                Color.White.copy(alpha = 0.14f),
                                                palette.activeChromeBorderEnd
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .shadow(10.dp, burstShape, spotColor = InkPrimary.copy(alpha = 0.30f))
                                            .clip(burstShape)
                                            .drawBehind {
                                                drawRect(brush = satinBrush)
                                                drawLine(
                                                    color = Color.White.copy(alpha = 0.26f),
                                                    start = Offset(0f, 0.8.dp.toPx()),
                                                    end = Offset(size.width, 0.8.dp.toPx()),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }
                                            .border(1.dp, burstBorder, burstShape)
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
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .border(1.2.dp, SolarAmber, RoundedCornerShape(9.dp))
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
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "画质 ${current.bestPhoto.qualityScore} · 其余 ${current.photos.size - 1} 张进回收站",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.74f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color.White.copy(alpha = 0.14f),
                                                border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.24f))
                                            ) {
                                                Text(
                                                    text = "省 ${formatStorageSize(current.nonBestBytes)}",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFFDE68A),
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
                                            tone = GalleryButtonTone.GLASS,
                                            leadingIcon = Icons.Filled.Check,
                                            height = 48.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("keep_all_burst_button")
                                        )
                                        GalleryActionButton(
                                            text = "对比(${current.photos.size})",
                                            onClick = { inspectingStack = current },
                                            tone = GalleryButtonTone.GLASS,
                                            leadingIcon = Icons.Filled.Compare,
                                            height = 48.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("compare_burst_button")
                                        )
                                        GalleryActionButton(
                                            text = "整组回收",
                                            onClick = {
                                                haptics.swipeDeleteConfirm()
                                                handleTrashEntireStack(current)
                                            },
                                            tone = GalleryButtonTone.DANGER_GLASS,
                                            leadingIcon = Icons.Filled.DeleteOutline,
                                            height = 48.dp,
                                            shape = RoundedCornerShape(14.dp),
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
                                    GalleryTactileOrbButton(
                                        icon = Icons.Filled.DeleteOutline,
                                        label = "回收",
                                        accentColor = CoralDanger,
                                        tintSurface = CoralSoft,
                                        orbSize = 64.dp,
                                        iconSize = 27.dp,
                                        onClick = {
                                            haptics.swipeDeleteConfirm()
                                            handleTrashSingle(current.photo)
                                        },
                                        modifier = Modifier.testTag("arc_trash_button")
                                    )

                                    GalleryTactileOrbButton(
                                        icon = if (current.photo.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        label = "收藏",
                                        accentColor = SolarAmber,
                                        tintSurface = SolarAmberSoft,
                                        orbSize = 54.dp,
                                        iconSize = 23.dp,
                                        onClick = {
                                            haptics.swipeKeepConfirm()
                                            handleKeepSingle(current.photo, true)
                                        },
                                        modifier = Modifier.testTag("arc_favorite_button")
                                    )

                                    GalleryTactileOrbButton(
                                        icon = Icons.Filled.Check,
                                        label = "保留",
                                        accentColor = EmeraldKeep,
                                        tintSurface = EmeraldSoft,
                                        orbSize = 64.dp,
                                        iconSize = 27.dp,
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
                        Icon(
                            imageVector = Icons.Filled.SwipeUp,
                            contentDescription = null,
                            tint = InkTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "上滑回收 · 下滑保留 · 摇一摇撤销",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkTertiary
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
            containerColor = SurfacePure.copy(alpha = 0.94f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { ModalSheetDragHandle() },
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
