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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.sensor.rememberHapticEngine
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianOverlayGlass
import com.example.ui.theme.ObsidianSatinBottom
import com.example.ui.theme.ObsidianSatinTop
import com.example.ui.theme.SolarAmber
import kotlinx.coroutines.launch
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

    BackHandler(onBack = onDismiss)

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
    val dismissDragY = remember { Animatable(0f) }

    fun removeCurrentOrDismiss() {
        val idx = pagerState.currentPage.coerceIn(0, sessionPhotos.lastIndex)
        sessionPhotos.removeAt(idx)
        if (sessionPhotos.isEmpty()) {
            onDismiss()
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
            onDismiss()
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

    val dismissProgress = (dismissDragY.value / (dismissThresholdPx * 2.4f)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12).copy(alpha = (1f - dismissProgress * 0.65f).coerceIn(0.35f, 1f)))
            .semantics(mergeDescendants = false) { isTraversalGroup = true }
            .pointerInput(Unit) {}
            .testTag("photo_detail_lightbox")
    ) {
        // Static GPU-blurred ambient photo radiance behind darkroom veil
        BlurredAmbientPhotoBackdrop(
            photo = currentPhoto,
            alpha = 0.28f * (1f - dismissProgress * 0.5f),
            blurRadius = 34.dp,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xC2090B10).copy(alpha = (0.76f * (1f - dismissProgress * 0.45f)).coerceIn(0.25f, 0.76f)))
        )

        // HorizontalPager for prev/next photo browsing + vertical swipe-down to close
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1.05f,
            key = { index -> sessionPhotos.getOrNull(index)?.id ?: index.toLong() },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dismissDragY.value
                    val dragScale = (1f - dismissProgress * 0.10f).coerceIn(0.88f, 1f)
                    scaleX = dragScale
                    scaleY = dragScale
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
                                onDismiss()
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

            Box(modifier = Modifier.fillMaxSize()) {
                if (pagePhoto.isVideo) {
                    InlineVideoPlayer(
                        photo = pagePhoto,
                        playWhenReady = isCurrentPage,
                        initialMuted = false,
                        fillCrop = false,
                        showFullControls = true,
                        showChrome = showChrome,
                        onToggleChrome = { showChrome = !showChrome },
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
                            .pointerInput(pagePhoto.id) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    if (!isCurrentPage) return@detectTransformGestures
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
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
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
                    icon = Icons.Filled.Close,
                    contentDescription = "关闭预览",
                    onClick = onDismiss,
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
                        color = Color.White,
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
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quality Score Badge with explicit "画质" label (Stage 4)
                ObsidianGlassPill {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = SolarAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "画质 ${currentPhoto.qualityScore}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
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
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF8FB0FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = photo.locationName.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (photo.caption.isNotBlank()) {
                                Text(
                                    text = photo.caption,
                                    fontFamily = if (isMachineGeneratedFilename(photo.caption)) JetBrainsMonoFontFamily else null,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.56f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = editorialTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
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
                                    color = Color.White.copy(alpha = 0.50f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (!photo.isVideo && zoomScale > 1.05f) {
                        ObsidianGlassPill(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f×", zoomScale),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (videoScrubber != null || hasActions) {
                    ObsidianGlassCard(
                        shape = RoundedCornerShape(22.dp),
                        topColor = ObsidianSatinTop.copy(alpha = 0.94f),
                        bottomColor = ObsidianSatinBottom.copy(alpha = 0.94f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lightbox_bottom_dock")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (videoScrubber != null) {
                                videoScrubber()
                                if (hasActions) {
                                    HorizontalDivider(
                                        thickness = 0.8.dp,
                                        color = Color.White.copy(alpha = 0.10f)
                                    )
                                }
                            }

                            if (hasActions) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (onRestore != null) {
                                        // 摩登画报：回收站流内还原动作为金描边画报钮
                                        // （onRestore 仅在 photoStatus==TRASHED 时出现，即回收站入口）
                                        LightboxActionPill(
                                            icon = Icons.Filled.Restore,
                                            label = "还原至影集",
                                            containerColor = DecoGold.copy(alpha = 0.20f),
                                            iconTint = DecoGold,
                                            labelColor = DecoPaperCream,
                                            borderColor = DecoGold.copy(alpha = 0.55f),
                                            onClick = onRestore,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("lightbox_restore_button")
                                        )
                                    } else {
                                        if (onTrash != null) {
                                            LightboxActionPill(
                                                icon = Icons.Filled.DeleteOutline,
                                                label = "移入回收站",
                                                containerColor = Color.White.copy(alpha = 0.08f),
                                                iconTint = Color(0xFFFF7A8A),
                                                labelColor = Color.White.copy(alpha = 0.92f),
                                                borderColor = Color.White.copy(alpha = 0.14f),
                                                onClick = onTrash,
                                                modifier = Modifier
                                                    .weight(1.15f)
                                                    .testTag("lightbox_trash_button")
                                            )
                                        }
                                        if (onFavorite != null) {
                                            LightboxActionPill(
                                                icon = if (photo.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                label = if (photo.isFavorite) "已收藏" else "收藏",
                                                containerColor = if (photo.isFavorite) {
                                                    SolarAmber.copy(alpha = 0.20f)
                                                } else {
                                                    Color.White.copy(alpha = 0.08f)
                                                },
                                                iconTint = if (photo.isFavorite) Color(0xFFFDE047) else Color(0xFFFCD34D),
                                                labelColor = if (photo.isFavorite) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.92f),
                                                borderColor = if (photo.isFavorite) {
                                                    SolarAmber.copy(alpha = 0.45f)
                                                } else {
                                                    Color.White.copy(alpha = 0.14f)
                                                },
                                                onClick = onFavorite,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("lightbox_favorite_button")
                                            )
                                        }
                                        if (onKeep != null) {
                                            LightboxActionPill(
                                                icon = Icons.Filled.Check,
                                                label = "保留",
                                                containerColor = Color.White.copy(alpha = 0.08f),
                                                iconTint = Color(0xFF6EE7B7),
                                                labelColor = Color.White.copy(alpha = 0.92f),
                                                borderColor = Color.White.copy(alpha = 0.14f),
                                                onClick = onKeep,
                                                modifier = Modifier
                                                    .weight(0.95f)
                                                    .testTag("lightbox_keep_button")
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
}

@Composable
private fun LightboxActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color,
    labelColor: Color,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = if (borderColor != Color.Transparent) BorderStroke(0.85.dp, borderColor) else null,
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
