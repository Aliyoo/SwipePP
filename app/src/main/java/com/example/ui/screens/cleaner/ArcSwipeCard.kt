package com.example.ui.screens.cleaner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.sensor.HapticPatternEngine
import com.example.ui.CleanupCardItem
import com.example.ui.components.InlineVideoPlayer
import com.example.ui.components.ObsidianGlassPill
import com.example.ui.components.ObsidianIconOrb
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.formatEditorialPhotoTitle
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.ObsidianOverlayBorderStrong
import com.example.ui.theme.ObsidianOverlayOrb
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.VioletVideo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ModernArcSwipeCard(
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
    var crossedDirection by remember(item.stableKey) { mutableIntStateOf(0) }
    var isCommitting by remember(item.stableKey) { mutableStateOf(false) }
    var showFavoriteHeartBurst by remember(item.stableKey) { mutableStateOf(false) }

    val trashThresholdPx = with(density) { 104.dp.toPx() }
    val keepThresholdPx = with(density) { 98.dp.toPx() }
    val flingMinDistancePx = with(density) { 54.dp.toPx() }
    val maxUpDragPx = with(density) { 260.dp.toPx() }
    val maxDownDragPx = with(density) { 180.dp.toPx() }
    val exitUpPx = with(density) { 420.dp.toPx() }
    val exitDownPx = with(density) { 320.dp.toPx() }
    val flingVelocityPxPerSec = with(density) { 560.dp.toPx() }

    LaunchedEffect(item.stableKey) {
        isCommitting = false
        crossedDirection = 0
        showFavoriteHeartBurst = false
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
            .pointerInput(item.stableKey, isFocused) {
                if (!isFocused) return@pointerInput
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (isCommitting) return@awaitEachGesture
                    var totalDx = 0f
                    var totalDy = 0f
                    var lockAxis = 0
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

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfacePure,
            border = BorderStroke(1.dp, HairlineBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .combinedClickable(
                    onClick = onTapCard,
                    onDoubleClick = {
                        if (isFocused && !isCommitting) {
                            scope.launch {
                                showFavoriteHeartBurst = true
                                delay(260L)
                                showFavoriteHeartBurst = false
                                onFavoriteAction()
                            }
                        }
                    }
                )
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
                            ObsidianGlassPill(color = ObsidianOverlayOrb) {
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
                            ObsidianGlassPill(color = ObsidianOverlayOrb) {
                                Text(
                                    text = "${item.primaryPhoto.widthPx}×${item.primaryPhoto.heightPx}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Double-tap Favorite Heart Bloom Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showFavoriteHeartBurst,
                        enter = fadeIn() + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                        exit = fadeOut() + scaleOut(targetScale = 1.25f),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SolarAmber,
                            shadowElevation = 12.dp,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "收藏保留",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
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
                        text = formatEditorialPhotoTitle(item.primaryPhoto),
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
