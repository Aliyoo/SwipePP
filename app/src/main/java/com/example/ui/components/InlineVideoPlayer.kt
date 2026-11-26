package com.example.ui.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

/**
 * Embedded inline Media3 ExoPlayer (TextureView-backed) that works cleanly inside both
 * full-screen Lightbox dialogs and 3D-transformed Arc Cleaner cards without SurfaceView clipping issues.
 * Gracefully renders a live animated preview for bundled demo video placeholders that lack a local URI.
 */
@OptIn(UnstableApi::class)
@Composable
fun InlineVideoPlayer(
    photo: PhotoItemEntity,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true,
    initialMuted: Boolean = false,
    initialPositionMs: Long = 0L,
    fillCrop: Boolean = false,
    showFullControls: Boolean = true,
    showChrome: Boolean = true,
    onToggleChrome: (() -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null,
    onFullscreenPlaybackState: ((positionMs: Long, isMuted: Boolean) -> Unit)? = null,
    onPlaybackCheckpoint: ((positionMs: Long, isMuted: Boolean) -> Unit)? = null,
    pauseAfterCompletion: Boolean = false,
    bottomDockOverlay: (@Composable (scrubberContent: @Composable () -> Unit) -> Unit)? = null
) {
    val contentUri = photo.contentUri?.takeIf { it.isNotBlank() }

    if (contentUri == null) {
        SampleVideoInlinePreview(
            photo = photo,
            playWhenReady = playWhenReady,
            fillCrop = fillCrop,
            showFullControls = showFullControls,
            showChrome = showChrome,
            onToggleChrome = onToggleChrome,
            onOpenFullscreen = onOpenFullscreen,
            bottomDockOverlay = bottomDockOverlay,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember(contentUri) { mutableStateOf(playWhenReady) }
    var isMuted by remember(contentUri) { mutableStateOf(initialMuted) }
    var isBuffering by remember(contentUri) { mutableStateOf(true) }
    var hasRenderedFirstFrame by remember(contentUri) { mutableStateOf(false) }
    var playbackError by remember(contentUri) { mutableStateOf<String?>(null) }
    var positionMs by remember(contentUri) { mutableLongStateOf(0L) }
    var durationMs by remember(contentUri) { mutableLongStateOf(photo.durationMs.coerceAtLeast(1L)) }
    var isScrubbing by remember(contentUri) { mutableStateOf(false) }
    var openingFullscreen by remember(contentUri) { mutableStateOf(false) }
    var scrubFraction by remember(contentUri) { mutableFloatStateOf(0f) }

    val exoPlayer = remember(contentUri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = if (pauseAfterCompletion) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
            volume = if (initialMuted) 0f else 1f
            setMediaItem(MediaItem.fromUri(Uri.parse(contentUri)), initialPositionMs.coerceAtLeast(0L))
            this.playWhenReady = playWhenReady
            prepare()
        }
    }

    // Sync external playWhenReady (e.g. when Arc Cleaner card scrolls in/out of focus)
    LaunchedEffect(exoPlayer, playWhenReady) {
        if (playWhenReady && exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = playWhenReady
        if (!playWhenReady) {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playbackError = null
                    val actualDur = exoPlayer.duration
                    if (actualDur > 0L) durationMs = actualDur
                }
                if (state == Player.STATE_ENDED && pauseAfterCompletion && exoPlayer.playWhenReady) {
                    exoPlayer.pause()
                    positionMs = exoPlayer.duration.coerceAtLeast(exoPlayer.currentPosition).coerceAtLeast(0L)
                    isPlaying = false
                }
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                isBuffering = false
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = "视频解码受限或文件不可用"
            }
        }
        exoPlayer.addListener(listener)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (playWhenReady && isPlaying) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            if (!openingFullscreen) {
                onPlaybackCheckpoint?.invoke(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.volume <= 0f)
            }
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Smooth timeline polling while playing
    LaunchedEffect(exoPlayer, isPlaying, isScrubbing) {
        while (true) {
            if (!isScrubbing) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.takeIf { it > 0L } ?: durationMs
                positionMs = pos
                durationMs = dur.coerceAtLeast(1L)
            }
            delay(if (isPlaying) 120L else 350L)
        }
    }

    val togglePlayPause: () -> Unit = {
        if (playbackError != null) {
            playbackError = null
            isBuffering = true
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPlaying = false
        } else {
            val replayFromEnd = pauseAfterCompletion && (
                exoPlayer.playbackState == Player.STATE_ENDED ||
                    (durationMs > 1L && positionMs >= durationMs)
                )
            if (replayFromEnd) {
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(contentUri)), 0L)
                exoPlayer.prepare()
            }
            exoPlayer.playWhenReady = true
            isPlaying = true
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .testTag("inline_video_player")
    ) {
        // Real TextureView-backed ExoPlayer surface (tap surface lives behind overlay controls)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (showFullControls && onToggleChrome != null) {
                            onToggleChrome()
                        } else {
                            togglePlayPause()
                        }
                    }
                )
        ) {
            AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.view_inline_video_player, null, false) as PlayerView).apply {
                    player = exoPlayer
                    resizeMode = if (fillCrop) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    useController = false
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
                view.resizeMode = if (fillCrop) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Poster frame remains visible until first decoded video frame paints (zero black flash)
        if (!hasRenderedFirstFrame || playbackError != null) {
            PhotoArtwork(
                photo = photo,
                contentDescription = photo.caption,
                contentScale = if (fillCrop) ContentScale.Crop else ContentScale.Fit,
                showVideoBadge = false,
                downsampleTier = ImageDownsampleTier.HERO_CARD,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering spinner（摩登画报：深墨印底 + 金色转轮，压视频暗 scrim 保证可读）
        if (isBuffering && playbackError == null && playWhenReady) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DecoScrimInk.copy(alpha = 0.72f))
                    .border(1.dp, DecoGold.copy(alpha = 0.55f), CircleShape)
            ) {
                CircularProgressIndicator(
                    color = DecoGold,
                    strokeWidth = 2.4.dp,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Playback error recovery pill（摩登画报朱红小签：朱底 + 纸黄字 + 纸黄受光边）
        if (playbackError != null) {
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = DecoVerm,
                border = BorderStroke(0.9.dp, DecoPaperCream.copy(alpha = 0.25f)),
                modifier = Modifier.clickable { togglePlayPause() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DecoIcon(
                        kind = DecoIconKind.REFRESH,
                        color = DecoPaperCream,
                        iconSize = 16.dp,
                        contentDescription = "重试播放"
                    )
                    Text(
                        text = "$playbackError · 点击重试",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = DecoDisplayFontFamily,
                        color = DecoPaperCream
                    )
                }
            }
        }

        // Center Play/Pause Seal Orb (shown when paused, or when chrome is visible in full-control mode)
        AnimatedVisibility(
            visible = playbackError == null && (!isPlaying || (showFullControls && showChrome)),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DecoVideoInkOrb(
                iconKind = if (isPlaying) DecoIconKind.PAUSE else DecoIconKind.PLAY,
                contentDescription = if (isPlaying) "暂停视频" else "播放视频",
                onClick = togglePlayPause,
                size = if (showFullControls) 64.dp else 54.dp,
                iconSize = if (showFullControls) 34.dp else 28.dp,
                modifier = Modifier.testTag("inline_video_play_pause")
            )
        }

        if (showFullControls) {
            val scrubberContent: @Composable () -> Unit = {
                if (playbackError == null) {
                    val safeDuration = durationMs.coerceAtLeast(1L)
                    val displayPos = if (isScrubbing) (scrubFraction * safeDuration).toLong() else positionMs
                    val sliderValue = if (isScrubbing) {
                        scrubFraction
                    } else {
                        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DecoSealChipButton(
                            iconKind = if (isPlaying) DecoIconKind.PAUSE else DecoIconKind.PLAY,
                            contentDescription = if (isPlaying) "暂停视频" else "播放视频",
                            onClick = togglePlayPause,
                            modifier = Modifier.testTag("inline_video_dock_play_pause")
                        )

                        Text(
                            text = formatVideoDuration(displayPos),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DecoInkOnGround
                        )

                        Slider(
                            value = sliderValue,
                            onValueChange = { fraction ->
                                isScrubbing = true
                                scrubFraction = fraction.coerceIn(0f, 1f)
                            },
                            onValueChangeFinished = {
                                val seekTarget = (scrubFraction * safeDuration).toLong()
                                exoPlayer.seekTo(seekTarget)
                                positionMs = seekTarget
                                isScrubbing = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = DecoPaperCream,
                                activeTrackColor = DecoGold,
                                inactiveTrackColor = DecoGold.copy(alpha = 0.30f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("inline_video_scrubber")
                        )

                        Text(
                            text = formatVideoDuration(safeDuration),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = DecoInkOnGround.copy(alpha = 0.62f)
                        )

                        DecoSealChipButton(
                            iconKind = if (isMuted) DecoIconKind.VOLUME_OFF else DecoIconKind.VOLUME,
                            contentDescription = if (isMuted) "取消静音" else "静音",
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.testTag("inline_video_mute_toggle")
                        )
                    }
                }
            }

            if (bottomDockOverlay != null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    bottomDockOverlay(scrubberContent)
                }
            } else {
                AnimatedVisibility(
                    visible = showChrome && playbackError == null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // 摩登画报控制托盘：画报纸面版面（金发丝描边 + 3dp 小圆角，平涂无玻璃高光）
                    DecoPosterPanel(
                        radius = 3.dp,
                        borderAlpha = 0.5f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            scrubberContent()
                        }
                    }
                }
            }
        } else {
            // Stage 2: Compact Card-Inline Overlay Controls (Arc Cleaner Card) —
            // no horizontal progress bar inside the card to avoid conflicting with HorizontalPager swipes.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 深墨时间签（压视频暗 scrim + 金发丝描边 + 纸黄字，可读性红线组合）
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = DecoScrimInk.copy(alpha = 0.78f),
                    border = BorderStroke(0.9.dp, DecoGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${formatVideoDuration(positionMs)} / ${formatVideoDuration(durationMs)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DecoPaperCream,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DecoVideoInkOrb(
                        iconKind = if (isMuted) DecoIconKind.VOLUME_OFF else DecoIconKind.VOLUME,
                        contentDescription = if (isMuted) "取消静音" else "静音",
                        onClick = { isMuted = !isMuted },
                        size = 44.dp,
                        iconSize = 18.dp,
                        modifier = Modifier.testTag("card_video_mute_toggle")
                    )

                    if (onOpenFullscreen != null) {
                        DecoVideoInkOrb(
                            iconKind = DecoIconKind.FULLSCREEN,
                            contentDescription = "全屏播放",
                            onClick = {
                                openingFullscreen = true
                                onFullscreenPlaybackState?.invoke(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.volume <= 0f)
                                exoPlayer.pause()
                                exoPlayer.stop()
                                onOpenFullscreen()
                            },
                            size = 44.dp,
                            iconSize = 18.dp,
                            modifier = Modifier.testTag("card_video_fullscreen_button")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smooth inline animated cinema preview for bundled sample demo video entries (which have no local MP4 file).
 */
@Composable
private fun SampleVideoInlinePreview(
    photo: PhotoItemEntity,
    playWhenReady: Boolean,
    fillCrop: Boolean,
    showFullControls: Boolean,
    showChrome: Boolean,
    onToggleChrome: (() -> Unit)?,
    onOpenFullscreen: (() -> Unit)?,
    bottomDockOverlay: (@Composable (scrubberContent: @Composable () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember(photo.id) { mutableStateOf(playWhenReady) }
    val totalMs = photo.durationMs.coerceAtLeast(12_000L)
    var elapsedMs by remember(photo.id) { mutableLongStateOf(0L) }

    LaunchedEffect(playWhenReady) {
        isPlaying = playWhenReady
    }

    LaunchedEffect(isPlaying, totalMs) {
        while (isPlaying) {
            delay(100L)
            elapsedMs = (elapsedMs + 100L) % totalMs
        }
    }

    val motionTransition = rememberInfiniteTransition(label = "sample_video_ken_burns")
    val kenBurnsScale by motionTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sample_kb_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .testTag("inline_video_sample_preview")
    ) {
        PhotoArtwork(
            photo = photo,
            contentDescription = photo.caption,
            contentScale = if (fillCrop) ContentScale.Crop else ContentScale.Fit,
            showVideoBadge = false,
            downsampleTier = ImageDownsampleTier.HERO_CARD,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (showFullControls && onToggleChrome != null) {
                            onToggleChrome()
                        } else {
                            isPlaying = !isPlaying
                        }
                    }
                )
                .graphicsLayer {
                    val s = if (isPlaying) kenBurnsScale else 1f
                    scaleX = s
                    scaleY = s
                }
        )

        AnimatedVisibility(
            visible = !isPlaying || (showFullControls && showChrome),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DecoVideoInkOrb(
                iconKind = if (isPlaying) DecoIconKind.PAUSE else DecoIconKind.PLAY,
                contentDescription = if (isPlaying) "暂停预览" else "播放预览",
                onClick = { isPlaying = !isPlaying },
                size = if (showFullControls) 64.dp else 54.dp,
                iconSize = if (showFullControls) 34.dp else 28.dp
            )
        }

        val progress = (elapsedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

        if (showFullControls) {
            val sampleScrubberContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DecoIcon(
                            kind = DecoIconKind.VIDEOCAM,
                            color = DecoGold,
                            iconSize = 13.dp
                        )
                        Text(
                            text = "示例视频动效预览 · 扫描本机视频后支持真实音画播放",
                            style = MaterialTheme.typography.labelSmall,
                            color = DecoInkOnGround.copy(alpha = 0.62f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DecoSealChipButton(
                            iconKind = if (isPlaying) DecoIconKind.PAUSE else DecoIconKind.PLAY,
                            contentDescription = if (isPlaying) "暂停预览" else "播放预览",
                            onClick = { isPlaying = !isPlaying }
                        )
                        Text(
                            text = formatVideoDuration(elapsedMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DecoInkOnGround
                        )
                        Slider(
                            value = progress,
                            onValueChange = { elapsedMs = (it * totalMs).toLong() },
                            colors = SliderDefaults.colors(
                                thumbColor = DecoPaperCream,
                                activeTrackColor = DecoGold,
                                inactiveTrackColor = DecoGold.copy(alpha = 0.30f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                        )
                        Text(
                            text = formatVideoDuration(totalMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = DecoInkOnGround.copy(alpha = 0.62f)
                        )
                    }
                }
            }

            if (bottomDockOverlay != null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    bottomDockOverlay(sampleScrubberContent)
                }
            } else {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // 摩登画报控制托盘：画报纸面版面（金发丝描边 + 3dp 小圆角，平涂无玻璃高光）
                    DecoPosterPanel(
                        radius = 3.dp,
                        borderAlpha = 0.5f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            sampleScrubberContent()
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 深墨时间签（压视频暗 scrim + 金发丝描边 + 纸黄字，可读性红线组合）
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = DecoScrimInk.copy(alpha = 0.78f),
                    border = BorderStroke(0.9.dp, DecoGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${formatVideoDuration(elapsedMs)} / ${formatVideoDuration(totalMs)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DecoPaperCream,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (onOpenFullscreen != null) {
                    DecoVideoInkOrb(
                        iconKind = DecoIconKind.FULLSCREEN,
                        contentDescription = "全屏播放",
                        onClick = onOpenFullscreen,
                        size = 44.dp,
                        iconSize = 18.dp
                    )
                }
            }
        }
    }
}

// =====================================================================================
// 摩登画报视频 chrome 私有件（deco-app.html 造型语法）：
// 深墨印钮 = 压视频/照片控件的可读性红线组合（DecoScrimInk 暗 scrim 底 + 金描边 + 纸黄图标）；
// 方章控制钮 = 压画报纸面托盘的金描边小方章（3dp 圆角 + 纸地底 + 金图标）。
// 色彩一律走 ui/theme Deco 令牌（日光画廊 / 暗夜暗房双套自适应），不引入硬编码色。
// =====================================================================================

/** 深墨印钮：圆形印章钮，暗 scrim 底 + 金环 + 纸黄图标，压视频时保证可读。 */
@Composable
private fun DecoVideoInkOrb(
    iconKind: DecoIconKind,
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp,
    iconSize: Dp
) {
    val orbContent: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            DecoIcon(
                kind = iconKind,
                color = DecoPaperCream,
                iconSize = iconSize,
                contentDescription = contentDescription
            )
        }
    }
    val orbModifier = modifier.size(size)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = DecoScrimInk.copy(alpha = 0.72f),
            border = BorderStroke(1.2.dp, DecoGold.copy(alpha = 0.85f)),
            modifier = orbModifier,
            content = orbContent
        )
    } else {
        Surface(
            shape = CircleShape,
            color = DecoScrimInk.copy(alpha = 0.72f),
            border = BorderStroke(1.2.dp, DecoGold.copy(alpha = 0.85f)),
            modifier = orbModifier,
            content = orbContent
        )
    }
}

/** 方章控制钮：纸地底 + 1.2dp 金描边小方章（3dp 圆角），压画报纸面控制托盘。 */
@Composable
private fun DecoSealChipButton(
    iconKind: DecoIconKind,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp
) {
    val gold = DecoGold
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = DecoGround.copy(alpha = 0.55f),
        border = BorderStroke(1.2.dp, gold),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            DecoIcon(
                kind = iconKind,
                color = gold,
                iconSize = iconSize,
                contentDescription = contentDescription
            )
        }
    }
}
