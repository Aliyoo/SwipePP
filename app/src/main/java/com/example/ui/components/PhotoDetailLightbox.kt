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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDanger
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
 * Supports pinch-to-zoom, double-tap 2.5x zoom, pan while zoomed, and unified zero-overlap bottom controls.
 *
 * When hosted inside [FlipYearApp], renders directly into [LocalFullScreenOverlayHost] inside
 * `MainActivity`'s true edge-to-edge window instead of spawning a floating Android Dialog window,
 * eliminating Samsung One UI / Android 15–16 dialog window cutout offset and bottom-bar truncation.
 */
@Composable
fun PhotoDetailLightbox(
    photo: PhotoItemEntity,
    onDismiss: () -> Unit,
    onKeep: ((PhotoItemEntity, Boolean) -> Unit)? = null,
    onTrash: ((PhotoItemEntity) -> Unit)? = null,
    onRestore: ((PhotoItemEntity) -> Unit)? = null
) {
    val overlayHost = LocalFullScreenOverlayHost.current
    if (overlayHost != null) {
        val entryId = remember { Any() }
        val latestPhoto by rememberUpdatedState(photo)
        val latestOnDismiss by rememberUpdatedState(onDismiss)
        val latestOnKeep by rememberUpdatedState(onKeep)
        val latestOnTrash by rememberUpdatedState(onTrash)
        val latestOnRestore by rememberUpdatedState(onRestore)

        DisposableEffect(overlayHost, entryId) {
            overlayHost.upsert(entryId) {
                PhotoDetailLightboxContent(
                    photo = latestPhoto,
                    onDismiss = latestOnDismiss,
                    onKeep = latestOnKeep,
                    onTrash = latestOnTrash,
                    onRestore = latestOnRestore
                )
            }
            onDispose {
                overlayHost.remove(entryId)
            }
        }
        return
    }

    // Standalone fallback (previews / isolated tests without FlipYearApp's FullScreenOverlayHost)
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
            onDismiss = onDismiss,
            onKeep = onKeep,
            onTrash = onTrash,
            onRestore = onRestore
        )
    }
}

@Composable
private fun PhotoDetailLightboxContent(
    photo: PhotoItemEntity,
    onDismiss: () -> Unit,
    onKeep: ((PhotoItemEntity, Boolean) -> Unit)?,
    onTrash: ((PhotoItemEntity) -> Unit)?,
    onRestore: ((PhotoItemEntity) -> Unit)?
) {
    val haptics = rememberHapticEngine()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    BackHandler(onBack = onDismiss)

    // Ensure crisp light system bar icons over the pitch-dark #0A0C12 darkroom canvas,
    // restoring the previous theme appearance cleanly when the lightbox closes.
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

    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var showChrome by remember(photo.id) { mutableStateOf(true) }
    val scaleAnim = remember(photo.id) { Animatable(1f) }

    val renderUnifiedBottomHud: @Composable (videoScrubber: (@Composable () -> Unit)?) -> Unit = { videoScrubber ->
        LightboxBottomHud(
            photo = photo,
            visible = showChrome,
            zoomScale = scale,
            videoScrubber = videoScrubber,
            onRestore = if (photo.photoStatus == PhotoStatus.TRASHED && onRestore != null) {
                {
                    haptics.undoPulse()
                    onRestore(photo)
                    onDismiss()
                }
            } else null,
            onTrash = if (photo.photoStatus != PhotoStatus.TRASHED && onTrash != null) {
                {
                    haptics.swipeDeleteConfirm()
                    onTrash(photo)
                    onDismiss()
                }
            } else null,
            onFavorite = if (photo.photoStatus != PhotoStatus.TRASHED && onKeep != null) {
                {
                    haptics.swipeKeepConfirm()
                    onKeep(photo, true)
                    onDismiss()
                }
            } else null,
            onKeep = if (photo.photoStatus != PhotoStatus.TRASHED && onKeep != null) {
                {
                    haptics.swipeKeepConfirm()
                    onKeep(photo, false)
                    onDismiss()
                }
            } else null
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12))
            .semantics(mergeDescendants = false) { isTraversalGroup = true }
            .pointerInput(Unit) {}
            .testTag("photo_detail_lightbox")
    ) {
        // Static GPU-blurred ambient photo radiance behind darkroom veil
        BlurredAmbientPhotoBackdrop(
            photo = photo,
            alpha = 0.28f,
            blurRadius = 34.dp,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xC2090B10))
        )

        // Interactive Zoom & Pan Photo Canvas OR Embedded Inline Video Player
        if (photo.isVideo) {
            InlineVideoPlayer(
                photo = photo,
                playWhenReady = true,
                initialMuted = false,
                fillCrop = false,
                showFullControls = true,
                showChrome = showChrome,
                onToggleChrome = { showChrome = !showChrome },
                bottomDockOverlay = { scrubberContent ->
                    renderUnifiedBottomHud(scrubberContent)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(photo.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
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
                    .pointerInput(photo.id) {
                        detectTapGestures(
                            onTap = { showChrome = !showChrome },
                            onDoubleTap = {
                                scope.launch {
                                    val target = if (scale > 1.4f) 1f else 2.5f
                                    scaleAnim.snapTo(scale)
                                    scaleAnim.animateTo(
                                        targetValue = target,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) {
                                        scale = value
                                        if (value <= 1.05f) offset = Offset.Zero
                                    }
                                }
                            }
                        )
                    }
            ) {
                PhotoArtwork(
                    photo = photo,
                    contentDescription = photo.caption,
                    contentScale = ContentScale.Fit,
                    showVideoBadge = false,
                    downsampleTier = ImageDownsampleTier.LIGHTBOX_FULL,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                renderUnifiedBottomHud(null)
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
                        text = formatPhotoDateTime(photo.takenAtMillis),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    val metaLine = buildString {
                        if (photo.widthPx > 0 && photo.heightPx > 0) {
                            append("${photo.widthPx}×${photo.heightPx} · ")
                        }
                        append(formatStorageSize(photo.fileSizeBytes))
                        if (photo.isVideo) {
                            append(" · ${formatVideoDuration(photo.durationMs)}")
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

                // Quality Score Badge (consistent dark matte pill, zero shadow bleed)
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
                            text = "${photo.qualityScore}",
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
                // Crisp Caption + Location Header directly on protective scrim (no double-card nesting)
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
                        Text(
                            text = photo.caption.ifBlank { if (photo.isVideo) "本地视频" else "本地照片" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!photo.locationName.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF7FA6FF),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = photo.locationName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.82f),
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

                // Single Unified Obsidian Satin Dock (gradient + specular lip + rim)
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
                                        LightboxActionPill(
                                            icon = Icons.Filled.Restore,
                                            label = "还原至影集",
                                            containerColor = CobaltPrimary.copy(alpha = 0.28f),
                                            contentColor = Color(0xFFC7D7FF),
                                            borderColor = CobaltPrimary.copy(alpha = 0.55f),
                                            onClick = onRestore,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("lightbox_restore_button")
                                        )
                                    } else {
                                        if (onTrash != null) {
                                            LightboxActionPill(
                                                icon = Icons.Filled.DeleteOutline,
                                                label = "删除",
                                                containerColor = CoralDanger.copy(alpha = 0.22f),
                                                contentColor = Color(0xFFFF8E9E),
                                                borderColor = CoralDanger.copy(alpha = 0.42f),
                                                onClick = onTrash,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("lightbox_trash_button")
                                            )
                                        }
                                        if (onFavorite != null) {
                                            LightboxActionPill(
                                                icon = if (photo.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                label = if (photo.isFavorite) "已收藏" else "收藏",
                                                containerColor = SolarAmber.copy(alpha = if (photo.isFavorite) 0.28f else 0.18f),
                                                contentColor = Color(0xFFFDE047),
                                                borderColor = SolarAmber.copy(alpha = if (photo.isFavorite) 0.52f else 0.36f),
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
                                                containerColor = EmeraldKeep.copy(alpha = 0.22f),
                                                contentColor = Color(0xFF6EE7B7),
                                                borderColor = EmeraldKeep.copy(alpha = 0.42f),
                                                onClick = onKeep,
                                                modifier = Modifier
                                                    .weight(1f)
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
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = if (borderColor != Color.Transparent) BorderStroke(0.9.dp, borderColor) else null,
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
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
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
