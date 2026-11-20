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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianOverlayBorder
import com.example.ui.theme.ObsidianOverlayBorderStrong
import com.example.ui.theme.ObsidianOverlayChipOnDark
import com.example.ui.theme.ObsidianOverlayOrb
import com.example.ui.theme.ObsidianOverlayPanel
import com.example.ui.theme.SolarAmber
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
    fillCrop: Boolean = false,
    showFullControls: Boolean = true,
    showChrome: Boolean = true,
    onToggleChrome: (() -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null,
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
    var scrubFraction by remember(contentUri) { mutableFloatStateOf(0f) }

    val exoPlayer = remember(contentUri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (initialMuted) 0f else 1f
            setMediaItem(MediaItem.fromUri(Uri.parse(contentUri)))
            this.playWhenReady = playWhenReady
            prepare()
        }
    }

    // Sync external playWhenReady (e.g. when Arc Cleaner card scrolls in/out of focus)
    LaunchedEffect(exoPlayer, playWhenReady) {
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
            exoPlayer.play()
            isPlaying = true
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
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
            .testTag("inline_video_player")
    ) {
        // Real TextureView-backed ExoPlayer surface
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

        // Buffering spinner
        if (isBuffering && playbackError == null && playWhenReady) {
            ObsidianGlassCard(
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.4.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Playback error recovery pill
        if (playbackError != null) {
            ObsidianGlassPill(
                modifier = Modifier.clickable { togglePlayPause() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "重试播放",
                        tint = SolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$playbackError · 点击重试",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Center Play/Pause Glass Orb (shown when paused, or when chrome is visible in full-control mode)
        AnimatedVisibility(
            visible = playbackError == null && (!isPlaying || (showFullControls && showChrome)),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ObsidianIconOrb(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "暂停视频" else "播放视频",
                onClick = togglePlayPause,
                size = if (showFullControls) 64.dp else 54.dp,
                iconSize = if (showFullControls) 34.dp else 28.dp,
                borderColor = ObsidianOverlayBorderStrong,
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
                        ObsidianIconOrb(
                            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停视频" else "播放视频",
                            onClick = togglePlayPause,
                            size = 34.dp,
                            iconSize = 18.dp,
                            color = ObsidianOverlayChipOnDark,
                            borderColor = null,
                            modifier = Modifier.testTag("inline_video_dock_play_pause")
                        )

                        Text(
                            text = formatVideoDuration(displayPos),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
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
                                thumbColor = Color.White,
                                activeTrackColor = CobaltPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("inline_video_scrubber")
                        )

                        Text(
                            text = formatVideoDuration(safeDuration),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )

                        ObsidianIconOrb(
                            icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "取消静音" else "静音",
                            onClick = { isMuted = !isMuted },
                            size = 34.dp,
                            iconSize = 18.dp,
                            color = ObsidianOverlayChipOnDark,
                            borderColor = null,
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
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ObsidianOverlayPanel,
                        border = BorderStroke(1.dp, ObsidianOverlayBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            scrubberContent()
                        }
                    }
                }
            }
        } else {
            // Compact Card-Inline Overlay Controls (Arc Cleaner Card)
            val progress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ObsidianGlassPill(
                        color = ObsidianOverlayOrb
                    ) {
                        Text(
                            text = "${formatVideoDuration(positionMs)} / ${formatVideoDuration(durationMs)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ObsidianIconOrb(
                            icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "开启声音" else "静音",
                            onClick = { isMuted = !isMuted },
                            size = 34.dp,
                            iconSize = 16.dp,
                            modifier = Modifier.testTag("card_video_mute_toggle")
                        )

                        if (onOpenFullscreen != null) {
                            ObsidianIconOrb(
                                icon = Icons.Filled.Fullscreen,
                                contentDescription = "全屏播放",
                                onClick = onOpenFullscreen,
                                size = 34.dp,
                                iconSize = 17.dp,
                                modifier = Modifier.testTag("card_video_fullscreen_button")
                            )
                        }
                    }
                }

                // Hairline glass playback progress track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
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
            ObsidianIconOrb(
                icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "暂停预览" else "播放预览",
                onClick = { isPlaying = !isPlaying },
                size = if (showFullControls) 64.dp else 54.dp,
                iconSize = if (showFullControls) 34.dp else 28.dp,
                borderColor = ObsidianOverlayBorderStrong
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
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = SolarAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "示例视频动效预览 · 扫描本机视频后支持真实音画播放",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianIconOrb(
                            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停预览" else "播放预览",
                            onClick = { isPlaying = !isPlaying },
                            size = 34.dp,
                            iconSize = 18.dp,
                            color = ObsidianOverlayChipOnDark,
                            borderColor = null
                        )
                        Text(
                            text = formatVideoDuration(elapsedMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Slider(
                            value = progress,
                            onValueChange = { elapsedMs = (it * totalMs).toLong() },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = CobaltPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                        )
                        Text(
                            text = formatVideoDuration(totalMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.72f)
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
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ObsidianOverlayPanel,
                        border = BorderStroke(1.dp, ObsidianOverlayBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            sampleScrubberContent()
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ObsidianGlassPill(
                        color = ObsidianOverlayOrb
                    ) {
                        Text(
                            text = "${formatVideoDuration(elapsedMs)} / ${formatVideoDuration(totalMs)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (onOpenFullscreen != null) {
                        ObsidianIconOrb(
                            icon = Icons.Filled.Fullscreen,
                            contentDescription = "全屏播放",
                            onClick = onOpenFullscreen,
                            size = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}
