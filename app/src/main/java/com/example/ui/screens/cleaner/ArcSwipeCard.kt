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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.sensor.HapticPatternEngine
import com.example.ui.CleanupCardItem
import com.example.ui.components.InlineVideoPlayer
import com.example.ui.components.PhotoArtwork
import com.example.ui.components.formatEditorialPhotoTitle
import com.example.ui.components.formatStorageSize
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoInkDeep
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.LocalFlipYearPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// 摩登画报 · 屏内一次性色彩（design-demos/deco-app.html 取材，浅暗主题同值）
// 照片压暗 scrim / 深色徽章芯片的取色，永远不用于地面文字
// 压照片 scrim 基墨收敛到 theme/Color.kt 共享常量 DecoScrimInk（原型 rgba(13,24,20,*)
private val DecoPhotoChipBase = Color(0xFF132920)    // 原型压照片徽章: rgba(19,41,31,0.8~0.88)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ModernArcSwipeCard(
    item: CleanupCardItem,
    pageOffsetProvider: () -> Float,
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

    val arcDropBasePx = with(density) { 42.dp.toPx() }
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

    val showTrashOverlay by remember(item.stableKey, trashThresholdPx) {
        derivedStateOf { (-dragOffsetY.value / trashThresholdPx) > 0.08f }
    }
    val showKeepOverlay by remember(item.stableKey, keepThresholdPx) {
        derivedStateOf { (dragOffsetY.value / keepThresholdPx) > 0.08f }
    }

    val palette = LocalFlipYearPalette.current
    val decoGold = palette.decoGold
    val decoVerm = palette.decoVerm
    val decoMat = palette.decoMat

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
                val pageOffset = pageOffsetProvider()
                val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1.5f)
                val clampedAbs = absOffset.coerceIn(0f, 1f)
                val dragY = dragOffsetY.value
                val trashProgress = (-dragY / trashThresholdPx).coerceIn(0f, 1f)

                val arcDropPx = (pageOffset * pageOffset) * arcDropBasePx
                val arcTiltDeg = (-pageOffset * 10f).coerceIn(-15f, 15f)
                val arcScale = lerp(1f, 0.85f, clampedAbs)
                val arcAlpha = lerp(1f, 0.65f, clampedAbs)

                translationY = arcDropPx + dragY
                rotationZ = arcTiltDeg + (dragY * -0.014f)
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
            // 连拍底层画片：纸材薄板 + 金色弱描边（原型 2.6deg 弧叠的等价迁移）
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(0.75f)
                    .offset(x = (-12).dp, y = 10.dp)
                    .graphicsLayer { rotationZ = -5.5f }
                    .clip(RoundedCornerShape(3.dp))
                    .background(decoMat.copy(alpha = 0.82f))
                    .border(1.dp, decoGold.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .aspectRatio(0.75f)
                    .offset(x = 12.dp, y = 6.dp)
                    .graphicsLayer { rotationZ = 5f }
                    .clip(RoundedCornerShape(3.dp))
                    .background(decoMat.copy(alpha = 0.94f))
                    .border(1.dp, decoGold.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            )
        }

        // 金双线画框卡片：外 1.5dp 金描边 → 3dp 纸材内衬 → 1dp 弱金内线，画片圆角 3dp
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = decoMat,
            border = BorderStroke(1.5.dp, decoGold),
            shadowElevation = 8.dp,
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
            // 3dp 纸材内衬 + 1dp 弱金内线
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(1.dp, decoGold.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
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
                        // 金环播放印钮（压照片暗部）
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(DecoPhotoChipBase.copy(alpha = 0.72f))
                                .border(2.dp, decoGold, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_video),
                                tint = DecoPaperCream,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // 压照片暗 scrim（可读性红线：线性 0.62 → 透明，含顶部弱压）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to DecoScrimInk.copy(alpha = 0.30f),
                                0.38f to Color.Transparent,
                                1f to DecoScrimInk.copy(alpha = 0.62f)
                            )
                        )
                )

                // Top Deco Badge Chips（压照片暗部：纸黄文字 + 暗房芯片）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item is CleanupCardItem.BurstStack) {
                        DecoPhotoChip {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterNone,
                                    contentDescription = null,
                                    tint = DecoPaperCream,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${item.photos.size} 张连拍",
                                    fontSize = 12.sp,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = DecoPaperCream
                                )
                            }
                        }

                        // 朱红实印：最佳帧徽章
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = decoVerm,
                            border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = DecoPaperCream,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "最佳 · 画质 ${item.bestPhoto.qualityScore}",
                                    fontSize = 12.sp,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = DecoPaperCream
                                )
                            }
                        }
                    } else if (item.primaryPhoto.widthPx > 0 && item.primaryPhoto.heightPx > 0) {
                        DecoPhotoChip {
                            Text(
                                text = "${item.primaryPhoto.widthPx}×${item.primaryPhoto.heightPx}",
                                fontSize = 12.sp,
                                color = DecoPaperCream,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Double-tap Favorite Heart Bloom Overlay
                AnimatedVisibility(
                    visible = showFavoriteHeartBurst,
                    enter = fadeIn() + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = fadeOut() + scaleOut(targetScale = 1.25f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = decoGold,
                        shadowElevation = 10.dp,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "收藏保留",
                                tint = DecoPaperCream,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Swipe-up 朱红回收站反馈（theirs draw 相期 alpha，拖拽帧零重组）× 画报纸章造型
                if (showTrashOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val p = (-dragOffsetY.value / trashThresholdPx).coerceIn(0f, 1f)
                                drawRect(color = decoVerm.copy(alpha = p * 0.55f))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = DecoPaperCream,
                            border = BorderStroke(1.dp, decoGold.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    tint = decoVerm
                                )
                                Text(
                                    text = "松手移入回收站 (+${formatStorageSize(item.totalBytes)})",
                                    fontSize = 14.sp,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DecoInkDeep
                                )
                            }
                        }
                    }
                }

                // Swipe-down 金色保留反馈（theirs draw 相期 alpha，拖拽帧零重组）× 画报纸章造型
                if (showKeepOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val p = (dragOffsetY.value / keepThresholdPx).coerceIn(0f, 1f)
                                drawRect(color = decoGold.copy(alpha = p * 0.42f))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = DecoPaperCream,
                            border = BorderStroke(1.dp, decoGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = decoGold
                                )
                                Text(
                                    text = if (item is CleanupCardItem.BurstStack) {
                                        "松手保留全部 ${item.photos.size} 张连拍"
                                    } else {
                                        "松手保留入册"
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = DecoDisplayFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DecoInkDeep
                                )
                            }
                        }
                    }
                }

                // 卡片底部信息行（压照片暗部）：纸黄标题 + 金色提示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = formatEditorialPhotoTitle(item.primaryPhoto),
                        fontSize = 14.sp,
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = DecoPaperCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item is CleanupCardItem.BurstStack) {
                        Text(
                            text = "点此展开",
                            fontSize = 12.sp,
                            fontFamily = DecoDisplayFontFamily,
                            color = decoGold,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (item.primaryPhoto.isVideo) {
                        Text(
                            text = "内嵌播放中 · 点全屏",
                            fontSize = 12.sp,
                            fontFamily = DecoDisplayFontFamily,
                            color = decoGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** 暗房芯片（压照片）：深色底 + 纸黄字 + 纸黄弱描边，画片圆角 2dp */
@Composable
private fun DecoPhotoChip(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = DecoPhotoChipBase.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, DecoPaperCream.copy(alpha = 0.22f)),
        content = content
    )
}
