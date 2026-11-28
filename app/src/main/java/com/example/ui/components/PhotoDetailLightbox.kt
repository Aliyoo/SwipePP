package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.ImageDownsampleTier
import com.example.data.MediaType
import com.example.data.MotionPhotoPlaybackResolver
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.sensor.rememberHapticEngine
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGoldOnPhoto
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoGroundDeep
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianOverlayGlass
import com.example.ui.theme.SolarAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Editorial full-screen darkroom photo & video inspection lightbox.
 * Supports left/right paging across the current photo collection, swipe-down to close,
 * pinch/double-tap zoom, favorite toggle, and auto-advancing after triage actions.
 */
@Composable
fun PhotoDetailLightbox(
    photo: PhotoItemEntity,
    onDismiss: () -> Unit,
    onKeep: ((PhotoItemEntity, Boolean) -> Unit)? = null,
    onTrash: ((PhotoItemEntity) -> Unit)? = null,
    onRestore: ((PhotoItemEntity) -> Unit)? = null,
    photos: List<PhotoItemEntity> = listOf(photo),
    initialIndex: Int = 0,
    onToggleFavorite: ((PhotoItemEntity, Boolean) -> Unit)? = null
) {
    val overlayHost = LocalFullScreenOverlayHost.current
    if (overlayHost != null) {
        val entryId = remember { Any() }
        val latestPhoto by rememberUpdatedState(photo)
        val latestPhotos by rememberUpdatedState(photos)
        val latestInitialIndex by rememberUpdatedState(initialIndex)
        val latestOnDismiss by rememberUpdatedState(onDismiss)
        val latestOnKeep by rememberUpdatedState(onKeep)
        val latestOnTrash by rememberUpdatedState(onTrash)
        val latestOnRestore by rememberUpdatedState(onRestore)
        val latestOnToggleFavorite by rememberUpdatedState(onToggleFavorite)

        DisposableEffect(overlayHost, entryId) {
            overlayHost.upsert(entryId) {
                PhotoDetailLightboxContent(
                    photo = latestPhoto,
                    photos = latestPhotos,
                    initialIndex = latestInitialIndex,
                    onDismiss = latestOnDismiss,
                    onKeep = latestOnKeep,
                    onTrash = latestOnTrash,
                    onRestore = latestOnRestore,
                    onToggleFavorite = latestOnToggleFavorite
                )
            }
            onDispose {
                overlayHost.remove(entryId)
            }
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view as? DialogWindowProvider)?.window
                ?: (view.parent as? DialogWindowProvider)?.window
            window?.let { w ->
                WindowCompat.setDecorFitsSystemWindows(w, false)
                w.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                w.setGravity(Gravity.FILL)
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                val lp = w.attributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    lp.fitInsetsTypes = 0
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                w.attributes = lp
            }
        }

        PhotoDetailLightboxContent(
            photo = photo,
            photos = photos,
            initialIndex = initialIndex,
            onDismiss = onDismiss,
            onKeep = onKeep,
            onTrash = onTrash,
            onRestore = onRestore,
            onToggleFavorite = onToggleFavorite
        )
    }
}

@Composable
private fun PhotoDetailLightboxContent(
    photo: PhotoItemEntity,
    photos: List<PhotoItemEntity>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onKeep: ((PhotoItemEntity, Boolean) -> Unit)?,
    onTrash: ((PhotoItemEntity) -> Unit)?,
    onRestore: ((PhotoItemEntity) -> Unit)?,
    onToggleFavorite: ((PhotoItemEntity, Boolean) -> Unit)?
) {
    val haptics = rememberHapticEngine()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 112.dp.toPx() }

    val revealAnim = remember { Animatable(0f) }
    val dismissDragY = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        revealAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)
        )
    }

    fun requestAnimatedDismiss(fromSwipe: Boolean = false) {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            try {
                if (fromSwipe) {
                    launch {
                        dismissDragY.animateTo(
                            targetValue = dismissDragY.value + dismissThresholdPx * 0.85f,
                            animationSpec = tween(durationMillis = 145, easing = FastOutLinearInEasing)
                        )
                    }
                }
                revealAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 145, easing = FastOutLinearInEasing)
                )
            } finally {
                onDismiss()
            }
        }
    }

    BackHandler(onBack = { requestAnimatedDismiss() })

    DisposableEffect(view) {
        val window = (view as? DialogWindowProvider)?.window
            ?: (view.parent as? DialogWindowProvider)?.window
            ?: context.findActivity()?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val prevLightStatus = controller?.isAppearanceLightStatusBars
        val prevLightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (prevLightStatus != null) {
                controller.isAppearanceLightStatusBars = prevLightStatus
            }
            if (prevLightNav != null) {
                controller.isAppearanceLightNavigationBars = prevLightNav
            }
        }
    }

    val initialList = remember {
        val base = photos.ifEmpty { listOf(photo) }
        if (base.any { it.id == photo.id }) base else listOf(photo)
    }
    val sessionPhotos = remember { mutableStateListOf<PhotoItemEntity>().apply { addAll(initialList) } }
    LaunchedEffect(photos) {
        val sessionIndexById = sessionPhotos.mapIndexed { index, item -> item.id to index }.toMap()
        photos.forEach { updated ->
            val index = sessionIndexById[updated.id]
            if (index != null && sessionPhotos[index] != updated) {
                sessionPhotos[index] = updated
            }
        }
    }
    val resolvedStartIndex = remember {
        if (initialIndex in initialList.indices && initialList[initialIndex].id == photo.id) {
            initialIndex
        } else {
            initialList.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
        }
    }

    if (sessionPhotos.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = resolvedStartIndex.coerceIn(0, sessionPhotos.lastIndex),
        pageCount = { sessionPhotos.size }
    )
    val currentIndex = pagerState.currentPage.coerceIn(0, sessionPhotos.lastIndex)
    val currentPhoto = sessionPhotos[currentIndex]

    var scale by remember(currentPhoto.id) { mutableFloatStateOf(1f) }
    var offset by remember(currentPhoto.id) { mutableStateOf(Offset.Zero) }
    var showChrome by remember { mutableStateOf(true) }
    val scaleAnim = remember(currentPhoto.id) { Animatable(1f) }

    fun removeCurrentOrDismiss() {
        val idx = pagerState.currentPage.coerceIn(0, sessionPhotos.lastIndex)
        sessionPhotos.removeAt(idx)
        if (sessionPhotos.isEmpty()) {
            requestAnimatedDismiss()
        } else {
            val nextIdx = idx.coerceAtMost(sessionPhotos.lastIndex)
            scope.launch { pagerState.scrollToPage(nextIdx) }
        }
    }

    fun advanceOrDismiss() {
        val idx = pagerState.currentPage
        if (idx < sessionPhotos.lastIndex) {
            scope.launch { pagerState.animateScrollToPage(idx + 1) }
        } else {
            requestAnimatedDismiss()
        }
    }

    val renderUnifiedBottomHud: @Composable (targetPhoto: PhotoItemEntity, videoScrubber: (@Composable () -> Unit)?) -> Unit =
        { targetPhoto, videoScrubber ->
            LightboxBottomHud(
                photo = targetPhoto,
                visible = showChrome,
                zoomScale = if (targetPhoto.id == currentPhoto.id) scale else 1f,
                videoScrubber = videoScrubber,
                onRestore = if (targetPhoto.photoStatus == PhotoStatus.TRASHED && onRestore != null) {
                    {
                        haptics.undoPulse()
                        onRestore(targetPhoto)
                        removeCurrentOrDismiss()
                    }
                } else null,
                onTrash = if (targetPhoto.photoStatus != PhotoStatus.TRASHED && onTrash != null) {
                    {
                        haptics.swipeDeleteConfirm()
                        onTrash(targetPhoto)
                        removeCurrentOrDismiss()
                    }
                } else null,
                onFavorite = if (targetPhoto.photoStatus != PhotoStatus.TRASHED && (onToggleFavorite != null || onKeep != null)) {
                    {
                        haptics.swipeKeepConfirm()
                        if (onToggleFavorite != null) {
                            val nextFav = !targetPhoto.isFavorite
                            onToggleFavorite(targetPhoto, nextFav)
                            val idx = sessionPhotos.indexOfFirst { it.id == targetPhoto.id }
                            if (idx >= 0) {
                                sessionPhotos[idx] = sessionPhotos[idx].copy(
                                    isFavorite = nextFav,
                                    favoritedAtMillis = if (nextFav) System.currentTimeMillis() else null,
                                    status = PhotoStatus.KEPT.name
                                )
                            }
                        } else if (onKeep != null) {
                            onKeep(targetPhoto, true)
                            advanceOrDismiss()
                        }
                    }
                } else null,
                onKeep = if (targetPhoto.photoStatus != PhotoStatus.TRASHED && onKeep != null) {
                    {
                        haptics.swipeKeepConfirm()
                        onKeep(targetPhoto, false)
                        advanceOrDismiss()
                    }
                } else null
            )
        }

    val dismissScaleRangePx = dismissThresholdPx * 2.4f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val reveal = revealAnim.value.coerceIn(0f, 1f)
                val dragProgress = (dismissDragY.value / dismissScaleRangePx).coerceIn(0f, 1f)
                val scrimAlpha = (1f - dragProgress * 0.65f).coerceIn(0.35f, 1f) * reveal
                drawRect(Color(0xFF0A0C12).copy(alpha = scrimAlpha))
            }
            .semantics(mergeDescendants = false) { isTraversalGroup = true }
            .pointerInput(Unit) {}
            .testTag("photo_detail_lightbox")
    ) {
        // Static GPU-blurred ambient photo radiance behind darkroom veil (GPU-modulated alpha, zero recomposition)
        BlurredAmbientPhotoBackdrop(
            photo = currentPhoto,
            alpha = 0.28f,
            blurRadius = 34.dp,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val reveal = revealAnim.value.coerceIn(0f, 1f)
                    val dragProgress = (dismissDragY.value / dismissScaleRangePx).coerceIn(0f, 1f)
                    alpha = (1f - dragProgress * 0.5f) * reveal
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val reveal = revealAnim.value.coerceIn(0f, 1f)
                    val dragProgress = (dismissDragY.value / dismissScaleRangePx).coerceIn(0f, 1f)
                    val veilAlpha = (0.76f * (1f - dragProgress * 0.45f)).coerceIn(0.25f, 0.76f) * reveal
                    drawRect(Color(0xFF090B10).copy(alpha = veilAlpha))
                }
        )

        // HorizontalPager for prev/next photo browsing + vertical swipe-down to close (100% GPU-driven)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1.05f,
            key = { index -> sessionPhotos.getOrNull(index)?.id ?: index.toLong() },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val reveal = revealAnim.value.coerceIn(0f, 1f)
                    val dragProgress = (dismissDragY.value / dismissScaleRangePx).coerceIn(0f, 1f)
                    val apertureScale = lerp(0.93f, 1f, reveal)
                    val dragScale = (1f - dragProgress * 0.11f).coerceIn(0.87f, 1f)
                    val combinedScale = apertureScale * dragScale
                    translationY = dismissDragY.value
                    scaleX = combinedScale
                    scaleY = combinedScale
                    alpha = reveal
                }
                .pointerInput(scale <= 1.05f, dismissThresholdPx) {
                    if (scale > 1.05f) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            val nextY = (dismissDragY.value + dragAmount).coerceAtLeast(0f)
                            if (nextY > 0f) {
                                change.consume()
                                scope.launch { dismissDragY.snapTo(nextY) }
                            }
                        },
                        onDragEnd = {
                            if (dismissDragY.value >= dismissThresholdPx) {
                                requestAnimatedDismiss(fromSwipe = true)
                            } else {
                                scope.launch {
                                    dismissDragY.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { dismissDragY.animateTo(0f) }
                        }
                    )
                }
                .testTag("lightbox_pager")
        ) { pageIndex ->
            val pagePhoto = sessionPhotos.getOrNull(pageIndex) ?: return@HorizontalPager
            val isCurrentPage = pageIndex == pagerState.currentPage

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val absOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                            .absoluteValue
                            .coerceIn(0f, 1f)
                        if (absOffset > 0.002f) {
                            val pageScale = lerp(1f, 0.93f, absOffset)
                            scaleX = pageScale
                            scaleY = pageScale
                            alpha = lerp(1f, 0.68f, absOffset)
                        }
                    }
            ) {
                val livePlaybackUri by produceState<String?>(
                    initialValue = null,
                    pagePhoto.contentUri,
                    pagePhoto.motionVideoUri,
                    pagePhoto.motionPhotoType
                ) {
                    if (!pagePhoto.isLivePhoto) {
                        value = null
                    } else {
                        value = withContext(Dispatchers.IO) {
                            MotionPhotoPlaybackResolver.resolvePlaybackUri(context, pagePhoto)
                        }
                    }
                }
                val liveVideoPhoto = remember(pagePhoto, livePlaybackUri) {
                    livePlaybackUri?.let { uri ->
                        pagePhoto.copy(
                            mediaType = MediaType.VIDEO.name,
                            durationMs = pagePhoto.motionVideoDurationMs,
                            contentUri = uri,
                            mimeType = "video/mp4"
                        )
                    }
                }
                val playableVideoPhoto = if (pagePhoto.isVideo) pagePhoto else liveVideoPhoto

                if (playableVideoPhoto != null) {
                    InlineVideoPlayer(
                        photo = playableVideoPhoto,
                        playWhenReady = isCurrentPage,
                        initialMuted = false,
                        fillCrop = false,
                        showFullControls = true,
                        showChrome = showChrome,
                        onToggleChrome = { showChrome = !showChrome },
                        pauseAfterCompletion = pagePhoto.isLivePhoto,
                        bottomDockOverlay = { scrubberContent ->
                            renderUnifiedBottomHud(pagePhoto, scrubberContent)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            // Single-finger drags stay unconsumed at 1x so HorizontalPager can page.
                            .pointerInput(pagePhoto.id) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var sawMultiTouch = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressedCount = event.changes.count { it.pressed }
                                        if (pressedCount == 0) break
                                        if (pressedCount >= 2) sawMultiTouch = true

                                        if (isCurrentPage && (sawMultiTouch || scale > 1.02f)) {
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                                            scale = newScale
                                            if (newScale > 1.02f) {
                                                val maxShiftX = (size.width * (newScale - 1f)) / 2f
                                                val maxShiftY = (size.height * (newScale - 1f)) / 2f
                                                offset = Offset(
                                                    x = (offset.x + pan.x).coerceIn(-maxShiftX, maxShiftX),
                                                    y = (offset.y + pan.y).coerceIn(-maxShiftY, maxShiftY)
                                                )
                                            } else {
                                                offset = Offset.Zero
                                            }
                                            event.changes.forEach { change ->
                                                if (change.positionChanged()) change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(pagePhoto.id) {
                                detectTapGestures(
                                    onTap = { showChrome = !showChrome },
                                    onDoubleTap = { tapOffset ->
                                        if (!isCurrentPage) return@detectTapGestures
                                        scope.launch {
                                            val targetScale = if (scale > 1.4f) 1f else 2.5f
                                            val startScale = scale
                                            val startOffset = offset
                                            val targetOffset = if (targetScale > 1.05f) {
                                                val centerX = size.width / 2f
                                                val centerY = size.height / 2f
                                                val maxShiftX = (size.width * (targetScale - 1f)) / 2f
                                                val maxShiftY = (size.height * (targetScale - 1f)) / 2f
                                                Offset(
                                                    x = ((centerX - tapOffset.x) * (targetScale - 1f)).coerceIn(-maxShiftX, maxShiftX),
                                                    y = ((centerY - tapOffset.y) * (targetScale - 1f)).coerceIn(-maxShiftY, maxShiftY)
                                                )
                                            } else {
                                                Offset.Zero
                                            }
                                            scaleAnim.snapTo(0f)
                                            scaleAnim.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) {
                                                val t = value
                                                scale = startScale + (targetScale - startScale) * t
                                                offset = Offset(
                                                    x = startOffset.x + (targetOffset.x - startOffset.x) * t,
                                                    y = startOffset.y + (targetOffset.y - startOffset.y) * t
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        PhotoArtwork(
                            photo = pagePhoto,
                            contentDescription = pagePhoto.caption.ifBlank { "照片预览" },
                            contentScale = ContentScale.Fit,
                            showVideoBadge = false,
                            downsampleTier = ImageDownsampleTier.LIGHTBOX_FULL,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (isCurrentPage) {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                                }
                        )
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        renderUnifiedBottomHud(pagePhoto, null)
                    }
                }
            }
        }

        // Top High-Contrast Darkroom Header Scrim + Controls
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(190, easing = FastOutSlowInEasing)) { -it / 3 },
            exit = fadeOut(tween(130)) +
                slideOutVertically(tween(140)) { -it / 3 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    alpha = revealAnim.value.coerceIn(0f, 1f)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xCC06080D),
                            0.65f to Color(0x6606080D),
                            1.0f to Color.Transparent
                        )
                    )
                    .pointerInput(Unit) {}
                    .windowInsetsPadding(
                        WindowInsets.statusBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ObsidianIconOrb(
                    iconKind = DecoIconKind.CLOSE,
                    contentDescription = "关闭预览",
                    onClick = { requestAnimatedDismiss() },
                    color = ObsidianOverlayGlass,
                    modifier = Modifier.testTag("lightbox_close_button")
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = buildString {
                            append(formatPhotoDateTime(currentPhoto.takenAtMillis))
                            if (sessionPhotos.size > 1) {
                                append(" · ${currentIndex + 1}/${sessionPhotos.size}")
                            }
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = DecoPaperCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val metaLine = buildString {
                        if (currentPhoto.widthPx > 0 && currentPhoto.heightPx > 0) {
                            append("${currentPhoto.widthPx}×${currentPhoto.heightPx} · ")
                        }
                        append(formatStorageSize(currentPhoto.fileSizeBytes))
                        if (currentPhoto.isVideo) {
                            append(" · ${formatVideoDuration(currentPhoto.durationMs)}")
                        }
                    }
                    Text(
                        text = metaLine,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = DecoPaperCream.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentPhoto.isLivePhoto) {
                        LivePhotoDetailBadge()
                    }

                    // Quality Score Badge with explicit "画质" label (Stage 4)
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = DecoVerm,
                        border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DecoIcon(
                                kind = DecoIconKind.STAR,
                                color = DecoPaperCream,
                                iconSize = 13.dp
                            )
                            Text(
                                text = "画质 ${currentPhoto.qualityScore}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = DecoPaperCream
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePhotoDetailBadge() {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = DecoScrimInk.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, DecoGoldOnPhoto.copy(alpha = 0.62f)),
        modifier = Modifier.testTag("lightbox_live_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            DecoIcon(
                kind = DecoIconKind.LIVE,
                color = DecoGoldOnPhoto,
                iconSize = 12.dp
            )
            Text(
                text = "LIVE",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = DecoGoldOnPhoto
            )
        }
    }
}

/**
 * Unified, single-layer darkroom bottom HUD that smoothly integrates protective scrim,
 * media caption/location, optional video transport scrubber, and high-contrast triage actions
 * with zero overlapping translucent boxes or shadow-bleed artifacts.
 */
@Composable
private fun LightboxBottomHud(
    photo: PhotoItemEntity,
    visible: Boolean,
    zoomScale: Float,
    videoScrubber: (@Composable () -> Unit)?,
    onRestore: (() -> Unit)?,
    onTrash: (() -> Unit)?,
    onFavorite: (() -> Unit)?,
    onKeep: (() -> Unit)?
) {
    val hasActions = onRestore != null || onTrash != null || onFavorite != null || onKeep != null

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.28f to Color(0x8807090F),
                        0.72f to Color(0xD907090F),
                        1.0f to Color(0xF207090F)
                    )
                )
                .pointerInput(Unit) {}
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val hasLocation = !photo.locationName.isNullOrBlank()
                        val editorialTitle = remember(photo.id, photo.caption, photo.locationName, photo.takenAtMillis) {
                            formatEditorialPhotoTitle(photo)
                        }
                        if (hasLocation) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DecoIcon(
                                    kind = DecoIconKind.LOCATION,
                                    // 压照片暗 scrim：亮香槟金定位印迹（替代旧克莱因蓝 #8FB0FF）
                                    color = DecoGoldOnPhoto,
                                    iconSize = 14.dp
                                )
                                Text(
                                    text = photo.locationName.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (photo.caption.isNotBlank()) {
                                Text(
                                    text = photo.caption,
                                    fontFamily = if (isMachineGeneratedFilename(photo.caption)) JetBrainsMonoFontFamily else DecoDisplayFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.56f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = editorialTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = DecoDisplayFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val showRawFilename = photo.caption.isNotBlank() && isMachineGeneratedFilename(photo.caption)
                            if (showRawFilename) {
                                Text(
                                    text = photo.caption,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.50f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (!photo.isVideo && zoomScale > 1.05f) {
                        // 变焦读数印签：画片语法——墨色底 + 金细线方角（替代旧玻璃胶囊 ObsidianGlassPill）
                        Row(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DecoScrimInk.copy(alpha = 0.72f))
                                .border(1.dp, DecoGoldOnPhoto.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f×", zoomScale),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = DecoGoldOnPhoto,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (videoScrubber != null || hasActions) {
                    // 摩登画报：底栏托盘与停靠栏同语法——纸地渐变面板 + 1dp 金细线，3dp 小圆角
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lightbox_bottom_dock")
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.verticalGradient(listOf(DecoGround, DecoGroundDeep))
                            )
                            .border(1.dp, DecoGold.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (videoScrubber != null) {
                            videoScrubber()
                            if (hasActions) {
                                HorizontalDivider(
                                    thickness = 0.8.dp,
                                    color = DecoGold.copy(alpha = 0.35f)
                                )
                            }
                        }

                        if (hasActions) {
                            // 决断印钮行：与弧形清理同语法（朱红实印/金环小章/金环印钮）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onRestore != null) {
                                    // 回收站流内还原：金环印钮（onRestore 仅在 photoStatus==TRASHED 时出现）
                                    DecoSealOrb(
                                        label = "还原",
                                        orbSize = 64.dp,
                                        tone = DecoOrbTone.GOLD_RING_HALO,
                                        onClick = onRestore,
                                        testTag = "lightbox_restore_button"
                                    )
                                } else {
                                    if (onTrash != null) {
                                        // 入站：朱红实印
                                        DecoSealOrb(
                                            label = "舍弃",
                                            orbSize = 64.dp,
                                            tone = DecoOrbTone.VERM_SOLID,
                                            onClick = onTrash,
                                            testTag = "lightbox_trash_button"
                                        )
                                    }
                                    if (onFavorite != null) {
                                        // 珍藏：金环小章；已珍藏加外圈余辉（原型印钮纯美术字）
                                        DecoSealOrb(
                                            label = "珍藏",
                                            orbSize = 54.dp,
                                            tone = if (photo.isFavorite) DecoOrbTone.GOLD_RING_HALO else DecoOrbTone.GOLD_RING,
                                            onClick = onFavorite,
                                            testTag = "lightbox_favorite_button",
                                            accessibilityLabel = if (photo.isFavorite) {
                                                "已珍藏，激活后移出珍藏"
                                            } else {
                                                "未珍藏，激活后收入珍藏"
                                            }
                                        )
                                    }
                                    if (onKeep != null) {
                                        // 保留：金环印钮（外圈余辉）
                                        DecoSealOrb(
                                            label = "保留",
                                            orbSize = 64.dp,
                                            tone = DecoOrbTone.GOLD_RING_HALO,
                                            onClick = onKeep,
                                            testTag = "lightbox_keep_button"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
