package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.sensor.HapticPatternEngine
import com.example.sensor.ShakeDetector
import com.example.sensor.rememberHapticEngine
import com.example.ui.CleanerEntrySource
import com.example.ui.CleanupCardItem
import com.example.ui.components.ArcTimeScaleScrubber
import com.example.ui.components.ArtisticGalleryBackdrop
import com.example.ui.components.CleanedCompletionBadge
import com.example.ui.components.GalleryActionButton
import com.example.ui.components.GalleryButtonTone
import com.example.ui.components.GalleryTactileOrbButton
import com.example.ui.components.InlineVideoPlayer
import com.example.ui.components.ObsidianGlassPill
import com.example.ui.components.ObsidianIconOrb
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.PhotoDetailLightbox
import com.example.ui.components.formatPhotoDateTime
import com.example.ui.components.formatShortTime
import com.example.ui.components.formatStorageSize
import com.example.ui.components.formatVideoDuration
import com.example.ui.components.monthChineseName
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
import com.example.ui.theme.ObsidianOverlayBorderStrong
import com.example.ui.theme.ObsidianOverlayOrb
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberSoft
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.StudioCanvasBottom
import com.example.ui.theme.StudioCanvasTop
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHapticEngine()
    val scope = rememberCoroutineScope()

    // Stage 2 & 3: In-session state drives Revisit mode progression & Completion Screen stats
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

    // Filter out in-session processed cards so Revisit mode (includeReviewed = true) advances cleanly
    // and reaches the Completion Screen when every item has been decided.
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

    // Wrapped decision handlers updating both Room (via ViewModel) and in-session stats/queue
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("cleaner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = InkPrimary
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
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

                // Dynamic Session Reclaimable Storage Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CoralSoft,
                    border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("dynamic_storage_counter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = CoralDanger,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = formatStorageSize(sessionTrashedBytes),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CoralDanger
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
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
                        tint = if (undoStackDepth > 0) CobaltPrimary else InkTertiary.copy(alpha = 0.45f)
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

                // Keep pager page strictly within bounds after deletions, jump to initialFocusDay,
                // handle day-isolation toggle cleanly, and scroll back to undone items.
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

                // Precision camera-dial tick when scrubbing horizontally across the curved Arc timeline
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .drop(1)
                        .distinctUntilChanged()
                        .collect {
                            haptics.pageFlipLift()
                        }
                }

                // Live Active Date Stamp & Optional Single-Day Isolation Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SurfacePure.copy(alpha = 0.86f),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .testTag("cleaner_active_date_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                                fontSize = 11.sp,
                                color = InkPrimary
                            )
                        }
                    }

                    if (initialFocusDay != null && (focusedDayCount > 0 || onlyFocusedDay)) {
                        Surface(
                            onClick = { onlyFocusedDay = !onlyFocusedDay },
                            shape = RoundedCornerShape(50),
                            color = if (onlyFocusedDay) CobaltPrimary else CobaltSoft,
                            border = BorderStroke(
                                1.dp,
                                if (onlyFocusedDay) CobaltPrimary else CobaltPrimary.copy(alpha = 0.28f)
                            ),
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
                                    modifier = Modifier.size(13.dp)
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

                    // Avoid HorizontalPager(key = ...) which triggers Foundation 1.7 matchScrollPositionWithKey
                    // IndexOutOfBounds/reuse crashes when an item is deleted and re-inserted via Undo;
                    // key the inner card composable instead with strict bounds checks.
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
                        val pageOffset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
                        val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1.5f)
                        val isFocused = index == safePage

                        key(item.stableKey) {
                            ModernArcSwipeCard(
                                item = item,
                                pageOffset = pageOffset,
                                absOffset = absOffset,
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
                                    // Stage 2: Swipe down ALWAYS keeps all frames (consistent Burst semantics)
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
                                        item is CleanupCardItem.SinglePhoto -> lightboxPhoto = item.photo
                                    }
                                },
                                onOpenFullscreen = { photo -> lightboxPhoto = photo },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Stage 2: Interactive "Arc as Time Scale" scrubber synced with active card timestamp
                ArcTimeScaleScrubber(
                    queue = displayedQueue,
                    currentIndex = safePage,
                    onJumpToIndex = { targetIdx ->
                        if (targetIdx in displayedQueue.indices && targetIdx != pagerState.currentPage) {
                            scope.launch {
                                runCatching { pagerState.scrollToPage(targetIdx) }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
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
                    Spacer(modifier = Modifier.height(3.dp))
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

                Spacer(modifier = Modifier.height(12.dp))

                when (activeItem) {
                    is CleanupCardItem.BurstStack -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stage 2: Explicit "Keep Best Only" button with live thumbnail preview of the kept frame
                            Surface(
                                onClick = {
                                    haptics.swipeDeleteConfirm()
                                    handleKeepBestInStack(activeItem, activeItem.bestPhoto)
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = InkPrimary,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("keep_best_burst_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .border(1.2.dp, SolarAmber, RoundedCornerShape(9.dp))
                                    ) {
                                        PhotoArtwork(
                                            photo = activeItem.bestPhoto,
                                            contentDescription = "预览将保留的最佳帧",
                                            showVideoBadge = false,
                                            downsampleTier = ImageDownsampleTier.GRID_THUMB,
                                            targetSizePx = 128,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "仅保留最佳 (画质 ${activeItem.bestPhoto.qualityScore})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = SurfacePure,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "其余 ${activeItem.photos.size - 1} 张移入回收站 (${formatStorageSize(activeItem.nonBestBytes)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SurfacePure.copy(alpha = 0.78f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = SolarAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GalleryActionButton(
                                    text = "保留全部",
                                    onClick = {
                                        haptics.swipeKeepConfirm()
                                        handleKeepEntireStack(activeItem, false)
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
                                    text = "对比(${activeItem.photos.size})",
                                    onClick = { inspectingStack = activeItem },
                                    tone = GalleryButtonTone.GLASS,
                                    leadingIcon = Icons.Filled.Compare,
                                    height = 48.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("compare_burst_button")
                                )
                                GalleryActionButton(
                                    text = "整组移走",
                                    onClick = {
                                        haptics.swipeDeleteConfirm()
                                        handleTrashEntireStack(activeItem)
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
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GalleryTactileOrbButton(
                                icon = Icons.Filled.DeleteOutline,
                                label = "移入回收站",
                                accentColor = CoralDanger,
                                tintSurface = CoralSoft,
                                onClick = {
                                    haptics.swipeDeleteConfirm()
                                    handleTrashSingle(activeItem.photo)
                                },
                                modifier = Modifier.testTag("arc_trash_button")
                            )

                            GalleryTactileOrbButton(
                                icon = if (activeItem.photo.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                label = "收藏保留",
                                accentColor = SolarAmber,
                                tintSurface = SolarAmberSoft,
                                onClick = {
                                    haptics.swipeKeepConfirm()
                                    handleKeepSingle(activeItem.photo, true)
                                },
                                modifier = Modifier.testTag("arc_favorite_button")
                            )

                            GalleryTactileOrbButton(
                                icon = Icons.Filled.Check,
                                label = "保留入册",
                                accentColor = EmeraldKeep,
                                tintSurface = EmeraldSoft,
                                onClick = {
                                    haptics.swipeKeepConfirm()
                                    handleKeepSingle(activeItem.photo, false)
                                },
                                modifier = Modifier.testTag("arc_keep_button")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwipeUp,
                        contentDescription = null,
                        tint = InkTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "上滑移入回收站 · 下滑保留 · 拖弧跳时间 · ${stringResource(R.string.shake_hint)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTertiary
                    )
                }
            }
        }
    }

    lightboxPhoto?.let { photo ->
        val flatPhotos = remember(displayedQueue) { displayedQueue.map { it.primaryPhoto } }
        val startIdx = remember(photo, flatPhotos) {
            flatPhotos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        }
        PhotoDetailLightbox(
            photo = photo,
            photos = flatPhotos,
            initialIndex = startIdx,
            onDismiss = { lightboxPhoto = null },
            onKeep = { target, fav -> handleKeepSingle(target, fav) },
            onTrash = { target -> handleTrashSingle(target) }
        )
    }

    inspectingStack?.let { stack ->
        ModalBottomSheet(
            onDismissRequest = { inspectingStack = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfacePure.copy(alpha = 0.94f)
        ) {
            BurstComparisonSheet(
                stack = stack,
                onConfirmKeepBest = { chosen ->
                    inspectingStack = null
                    handleKeepBestInStack(stack, chosen)
                }
            )
        }
    }
}

@Composable
private fun ModernArcSwipeCard(
    item: CleanupCardItem,
    pageOffset: Float,
    absOffset: Float,
    isFocused: Boolean,
    isVideoPlaybackActive: Boolean,
    haptics: HapticPatternEngine,
    onSwipeUpTrash: () -> Unit,
    onSwipeDownKeep: () -> Unit,
    onFavoriteAction: () -> Unit,
    onTapCard: () -> Unit,
    onOpenFullscreen: (PhotoItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dragOffsetY = remember(item.stableKey) { Animatable(0f) }
    // -1 = crossed trash threshold, 0 = neutral, +1 = crossed keep threshold
    var crossedDirection by remember(item.stableKey) { mutableIntStateOf(0) }
    var isCommitting by remember(item.stableKey) { mutableStateOf(false) }

    // Stage 2: dp-calibrated thresholds (~104dp / 98dp) + velocity boost
    val trashThresholdPx = with(density) { 104.dp.toPx() }
    val keepThresholdPx = with(density) { 98.dp.toPx() }
    val flingMinDistancePx = with(density) { 54.dp.toPx() }
    val maxUpDragPx = with(density) { 260.dp.toPx() }
    val maxDownDragPx = with(density) { 180.dp.toPx() }
    val exitUpPx = with(density) { 420.dp.toPx() }
    val exitDownPx = with(density) { 320.dp.toPx() }
    val flingVelocityPxPerSec = with(density) { 560.dp.toPx() }

    // Ensure any card re-entering composition after an Undo starts cleanly centered & interactive
    LaunchedEffect(item.stableKey) {
        isCommitting = false
        crossedDirection = 0
        if (dragOffsetY.value != 0f) {
            dragOffsetY.snapTo(0f)
        }
    }

    val arcDropPx = (pageOffset * pageOffset) * with(density) { 42.dp.toPx() }
    val arcTiltDeg = (-pageOffset * 10f).coerceIn(-15f, 15f)
    val arcScale = lerp(1f, 0.85f, absOffset.coerceIn(0f, 1f))
    val arcAlpha = lerp(1f, 0.65f, absOffset.coerceIn(0f, 1f))
    val trashProgress = (-dragOffsetY.value / trashThresholdPx).coerceIn(0f, 1f)
    val keepProgress = (dragOffsetY.value / keepThresholdPx).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics {
                if (isFocused) {
                    customActions = listOf(
                        CustomAccessibilityAction("保留") {
                            onSwipeDownKeep()
                            true
                        },
                        CustomAccessibilityAction("收藏") {
                            onFavoriteAction()
                            true
                        },
                        CustomAccessibilityAction("移入回收站") {
                            onSwipeUpTrash()
                            true
                        }
                    )
                }
            }
            .graphicsLayer {
                translationY = arcDropPx + dragOffsetY.value
                rotationZ = arcTiltDeg + (dragOffsetY.value * -0.014f)
                scaleX = arcScale * (1f - trashProgress * 0.08f)
                scaleY = arcScale * (1f - trashProgress * 0.08f)
                alpha = arcAlpha * (1f - trashProgress * 0.35f)
                cameraDistance = 12f * this.density
            }
            // Stage 2: Strict touch-slop direction lock + VelocityTracker so horizontal paging
            // jitter never misfires vertical trash/keep gestures.
            .pointerInput(item.stableKey, isFocused) {
                if (!isFocused) return@pointerInput
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (isCommitting) return@awaitEachGesture
                    var totalDx = 0f
                    var totalDy = 0f
                    var lockAxis = 0 // 0 = undecided, 1 = vertical locked, 2 = horizontal yielded
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (lockAxis == 1 && !isCommitting) {
                                val velocityY = velocityTracker.calculateVelocity().y
                                val currentY = dragOffsetY.value
                                crossedDirection = 0
                                val shouldTrash = currentY < -trashThresholdPx ||
                                    (velocityY < -flingVelocityPxPerSec && currentY < -flingMinDistancePx)
                                val shouldKeep = currentY > keepThresholdPx ||
                                    (velocityY > flingVelocityPxPerSec && currentY > flingMinDistancePx)
                                scope.launch {
                                    when {
                                        shouldTrash -> {
                                            isCommitting = true
                                            try {
                                                dragOffsetY.animateTo(-exitUpPx, spring(stiffness = Spring.StiffnessMediumLow))
                                                onSwipeUpTrash()
                                            } finally {
                                                dragOffsetY.snapTo(0f)
                                                isCommitting = false
                                            }
                                        }
                                        shouldKeep -> {
                                            isCommitting = true
                                            try {
                                                dragOffsetY.animateTo(exitDownPx, spring(stiffness = Spring.StiffnessMediumLow))
                                                onSwipeDownKeep()
                                            } finally {
                                                dragOffsetY.snapTo(0f)
                                                isCommitting = false
                                            }
                                        }
                                        else -> {
                                            dragOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                        }
                                    }
                                }
                            }
                            break
                        }

                        val delta = change.positionChange()
                        if (lockAxis == 0) {
                            totalDx += delta.x
                            totalDy += delta.y
                            val absX = totalDx.absoluteValue
                            val absY = totalDy.absoluteValue
                            if (absX > touchSlop || absY > touchSlop) {
                                if (absY > absX * 1.25f) {
                                    lockAxis = 1
                                } else {
                                    // Horizontal gesture: yield cleanly to HorizontalPager
                                    lockAxis = 2
                                    break
                                }
                            }
                        }

                        if (lockAxis == 1 && !isCommitting) {
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val next = (dragOffsetY.value + delta.y).coerceIn(-maxUpDragPx, maxDownDragPx)
                            val nextDir = when {
                                next < -trashThresholdPx -> -1
                                next > keepThresholdPx -> 1
                                else -> 0
                            }
                            if (nextDir != 0 && nextDir != crossedDirection) {
                                haptics.swipeThresholdDetent(isTrash = nextDir == -1)
                            }
                            crossedDirection = nextDir
                            scope.launch { dragOffsetY.snapTo(next) }
                        }
                    }
                }
            }
    ) {
        if (item is CleanupCardItem.BurstStack) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(0.75f)
                    .offset(x = (-12).dp, y = 10.dp)
                    .graphicsLayer { rotationZ = -5.5f }
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfacePure.copy(alpha = 0.72f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(22.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .aspectRatio(0.75f)
                    .offset(x = 12.dp, y = 6.dp)
                    .graphicsLayer { rotationZ = 5f }
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfacePure.copy(alpha = 0.88f))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(22.dp))
            )
        }

        // Crisp Pure-White Modern Gallery Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfacePure,
            border = BorderStroke(1.dp, HairlineBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clickable(onClick = onTapCard)
                .testTag("arc_card_${item.stableKey}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (item.primaryPhoto.isVideo && isFocused) {
                        InlineVideoPlayer(
                            photo = item.primaryPhoto,
                            playWhenReady = isVideoPlaybackActive,
                            initialMuted = true,
                            fillCrop = true,
                            showFullControls = false,
                            onOpenFullscreen = { onOpenFullscreen(item.primaryPhoto) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PhotoArtwork(
                            photo = item.primaryPhoto,
                            contentDescription = stringResource(R.string.cd_photo_card),
                            downsampleTier = ImageDownsampleTier.HERO_CARD,
                            targetSizePx = 960,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Center Play Indicator for non-focused video cards in the pager
                        if (item.primaryPhoto.isVideo) {
                            ObsidianIconOrb(
                                icon = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_video),
                                onClick = null,
                                size = 58.dp,
                                iconSize = 34.dp,
                                borderColor = ObsidianOverlayBorderStrong,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    // Top Floating Frosted Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item is CleanupCardItem.BurstStack) {
                            ObsidianGlassPill(
                                color = ObsidianOverlayOrb
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FilterNone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${item.photos.size} 张连拍",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SolarAmber.copy(alpha = 0.92f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "最佳 · 画质 ${item.bestPhoto.qualityScore}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else if (item.primaryPhoto.widthPx > 0 && item.primaryPhoto.heightPx > 0) {
                            ObsidianGlassPill(
                                color = ObsidianOverlayOrb
                            ) {
                                Text(
                                    text = "${item.primaryPhoto.widthPx}×${item.primaryPhoto.heightPx}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Swipe-up Coral Trash Feedback Overlay
                    if (trashProgress > 0.08f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CoralDanger.copy(alpha = trashProgress * 0.52f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SurfacePure
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        tint = CoralDanger
                                    )
                                    Text(
                                        text = "松手移入回收站 (+${formatStorageSize(item.totalBytes)})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InkPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Swipe-down Emerald Keep Feedback Overlay
                    if (keepProgress > 0.08f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(EmeraldKeep.copy(alpha = keepProgress * 0.44f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SurfacePure
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = EmeraldKeep
                                    )
                                    Text(
                                        text = if (item is CleanupCardItem.BurstStack) {
                                            "松手保留全部 ${item.photos.size} 张连拍"
                                        } else {
                                            "松手保留入册"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InkPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.primaryPhoto.caption.ifBlank { if (item.primaryPhoto.isVideo) "本地视频" else "本地照片" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item is CleanupCardItem.BurstStack) {
                        Text(
                            text = "点此展开",
                            style = MaterialTheme.typography.labelSmall,
                            color = CobaltPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (item.primaryPhoto.isVideo) {
                        Text(
                            text = "内嵌播放中 · 点全屏",
                            style = MaterialTheme.typography.labelSmall,
                            color = VioletVideo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun BurstComparisonSheet(
    stack: CleanupCardItem.BurstStack,
    onConfirmKeepBest: (PhotoItemEntity) -> Unit
) {
    // Stage 2: Purely local selection until user taps explicit Confirm button
    var selectedPhoto by remember(stack) { mutableStateOf(stack.bestPhoto) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "连拍精选对比 · 共 ${stack.photos.size} 张",
                style = MaterialTheme.typography.headlineMedium,
                color = InkPrimary
            )
            Text(
                text = "点击任意帧预览选择，确认后才仅保留选中项并将其他连拍移入回收站。",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(stack.photos) { idx, photo ->
                val isSelected = photo.id == selectedPhoto.id
                val borderWidth by animateFloatAsState(if (isSelected) 2.5f else 1f, label = "burst_sel")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) CobaltSoft else SurfaceMuted,
                    border = BorderStroke(
                        borderWidth.dp,
                        if (isSelected) CobaltPrimary else HairlineBorder
                    ),
                    modifier = Modifier
                        .width(166.dp)
                        .clickable {
                            selectedPhoto = photo
                        }
                        .testTag("burst_choice_${idx}")
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.82f)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            PhotoArtwork(
                                photo = photo,
                                contentDescription = photo.caption,
                                downsampleTier = ImageDownsampleTier.PREVIEW_CARD,
                                targetSizePx = 384,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CobaltPrimary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "保留",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "画质 ${photo.qualityScore}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (photo.qualityScore >= 90) CobaltPrimary else InkSecondary
                            )
                            Text(
                                text = formatShortTime(photo.takenAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkTertiary
                            )
                        }
                        Text(
                            text = photo.caption.ifBlank { "连拍帧 #${idx + 1}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val freedIfConfirmed = stack.photos.filter { it.id != selectedPhoto.id }.sumOf { it.fileSizeBytes }
        GalleryActionButton(
            text = "确认仅保留选中项 · 其余 ${stack.photos.size - 1} 张移入回收站 (${formatStorageSize(freedIfConfirmed)})",
            onClick = { onConfirmKeepBest(selectedPhoto) },
            tone = GalleryButtonTone.OBSIDIAN,
            leadingIcon = Icons.Filled.CheckCircle,
            height = 50.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_burst_selection_button")
        )
    }
}

@Composable
private fun CleanupCompletedCelebration(
    title: String,
    monthNumber: Int?,
    completedDay: Int?,
    remainingInFullQueue: Int,
    sessionKeptCount: Int,
    sessionFavoritedCount: Int,
    sessionTrashedCount: Int,
    sessionTrashedBytes: Long,
    entrySource: CleanerEntrySource,
    onContinueFullMonth: () -> Unit,
    onOpenVault: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSingleDayFinishedWithMoreInMonth = completedDay != null && remainingInFullQueue > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("cleanup_completion_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stage 3: Peak completion stamp animation
        if (monthNumber != null && !isSingleDayFinishedWithMoreInMonth) {
            CleanedCompletionBadge(
                month = monthNumber,
                freedBytes = sessionTrashedBytes,
                animateEntry = true
            )
        } else {
            CleanedCompletionBadge(
                month = monthNumber ?: 12,
                freedBytes = sessionTrashedBytes,
                animateEntry = true
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = when {
                isSingleDayFinishedWithMoreInMonth && monthNumber != null ->
                    "${monthChineseName(monthNumber)} ${completedDay}日已整理完成"
                monthNumber != null ->
                    "${monthChineseName(monthNumber)}整理完成"
                else -> "$title 完成"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = InkPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stage 3: Session-scoped stats summary (not global vault totals)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfacePure,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag("completion_session_stats")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "本次整理统计",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                Text(
                    text = "保留 $sessionKeptCount · 收藏 $sessionFavoritedCount · 移入回收站 $sessionTrashedCount（待释放 ${formatStorageSize(sessionTrashedBytes)}）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isSingleDayFinishedWithMoreInMonth) {
                "当天媒体已全部处理完毕，本月其余日期还有 $remainingInFullQueue 项待整理。"
            } else if (sessionTrashedCount > 0) {
                "移入回收站的项目尚未从设备删除，前往回收站确认后即可释放空间。"
            } else {
                "本次整理的照片和视频均已妥善保留在影集中。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (isSingleDayFinishedWithMoreInMonth) {
            GalleryActionButton(
                text = "继续整理本月剩余 $remainingInFullQueue 项",
                onClick = onContinueFullMonth,
                tone = GalleryButtonTone.OBSIDIAN,
                height = 50.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continue_full_month_button")
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (sessionTrashedCount > 0) {
                GalleryActionButton(
                    text = "去回收站释放空间 (${formatStorageSize(sessionTrashedBytes)})",
                    leadingIcon = Icons.Filled.DeleteOutline,
                    onClick = onOpenVault,
                    tone = GalleryButtonTone.DANGER_GLASS,
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_open_vault_button")
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            GalleryActionButton(
                text = entrySource.backLabel,
                onClick = onDone,
                tone = GalleryButtonTone.GLASS,
                height = 48.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cleanup_done_button")
            )
        } else {
            if (sessionTrashedCount > 0) {
                GalleryActionButton(
                    text = "去回收站释放空间 (${formatStorageSize(sessionTrashedBytes)})",
                    leadingIcon = Icons.Filled.DeleteOutline,
                    onClick = onOpenVault,
                    tone = GalleryButtonTone.OBSIDIAN,
                    height = 50.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("completion_open_vault_button")
                )
                Spacer(modifier = Modifier.height(10.dp))
                GalleryActionButton(
                    text = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.GLASS,
                    height = 48.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            } else {
                GalleryActionButton(
                    text = entrySource.backLabel,
                    onClick = onDone,
                    tone = GalleryButtonTone.OBSIDIAN,
                    height = 50.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cleanup_done_button")
                )
            }
        }
    }
}
