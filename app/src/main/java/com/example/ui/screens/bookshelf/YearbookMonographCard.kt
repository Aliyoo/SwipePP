package com.example.ui.screens.bookshelf

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.YearbookSummary
import com.example.ui.components.ObsidianGlassCard
import com.example.ui.components.ObsidianGlassPill
import com.example.ui.components.PhotoArtwork
import com.example.ui.theme.EditionCobalt
import com.example.ui.theme.EditionObsidian
import com.example.ui.theme.EditionSlatePlum
import com.example.ui.theme.EditionTeal
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarAmber

@Composable
internal fun ModernYearbook3DCard(
    summary: YearbookSummary,
    isFocused: Boolean,
    isCarouselPaused: Boolean,
    onCustomizeCover: () -> Unit,
    onActiveSlideChanged: (PhotoItemEntity?) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coverColor = when (summary.editionIndex) {
        0 -> EditionObsidian
        1 -> EditionCobalt
        2 -> EditionTeal
        else -> EditionSlatePlum
    }
    val bookShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp)
    val rimBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.24f)
            )
        )
    }

    val slides = remember(summary.coverSlides, summary.coverPhoto) {
        summary.coverSlides.ifEmpty { listOfNotNull(summary.coverPhoto) }
    }
    val slideKeys = remember(slides) { slides.map { it.id } }
    var currentSlideIndex by remember(summary.year, slideKeys) { mutableIntStateOf(0) }
    val safeSlideIndex = if (slides.isEmpty()) 0 else currentSlideIndex.coerceIn(0, slides.lastIndex)
    val displayedPhoto = slides.getOrNull(safeSlideIndex) ?: summary.coverPhoto
    val slideProgress = remember(summary.year, slideKeys) { Animatable(0f) }

    val isCarouselActive = isFocused && !isCarouselPaused && slides.size > 1

    LaunchedEffect(displayedPhoto, isFocused) {
        if (isFocused) {
            onActiveSlideChanged(displayedPhoto)
        }
    }

    // Smooth museum-grade slideshow clock + Ken Burns progress — strictly active ONLY on the focused center book
    LaunchedEffect(summary.year, slideKeys, summary.coverIntervalSeconds, isCarouselActive) {
        if (!isCarouselActive) {
            slideProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val durationMs = (summary.coverIntervalSeconds.coerceIn(3, 12) * 1000)
        while (true) {
            slideProgress.snapTo(0f)
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
            if (slides.size > 1) {
                currentSlideIndex = (currentSlideIndex + 1) % slides.size
            }
        }
    }

    val bookSpotShadow = if (LocalIsDarkTheme.current) Color.Black.copy(alpha = 0.60f) else InkPrimary.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .shadow(elevation = 22.dp, shape = bookShape, spotColor = bookSpotShadow)
            .clip(bookShape)
            .background(coverColor)
            .clickable(onClick = onClick)
    ) {
        // 100% Full-Bleed Edge-to-Edge Living Cover Showcase (Crossfade + whisper-subtle Ken Burns drift)
        if (displayedPhoto != null) {
            val kenBurnsScale = if (isCarouselActive) lerp(1.00f, 1.07f, slideProgress.value) else 1f
            val kenBurnsPanX = if (isCarouselActive) {
                val direction = if (safeSlideIndex % 2 == 0) 1f else -1f
                lerp(-6f, 6f, slideProgress.value) * direction
            } else {
                0f
            }

            Crossfade(
                targetState = displayedPhoto,
                animationSpec = tween(durationMillis = 820),
                label = "yearbook_cover_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { slidePhoto ->
                PhotoArtwork(
                    photo = slidePhoto,
                    contentDescription = stringResource(R.string.cd_yearbook_cover),
                    showVideoBadge = false,
                    downsampleTier = ImageDownsampleTier.HERO_CARD,
                    targetSizePx = 768,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                            translationX = kenBurnsPanX
                        }
                )
            }
        }

        // Cinematic Editorial Vignette for crisp legibility at top & bottom while preserving center photo clarity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to coverColor.copy(alpha = 0.72f),
                        0.34f to coverColor.copy(alpha = 0.12f),
                        0.66f to coverColor.copy(alpha = 0.18f),
                        1.0f to coverColor.copy(alpha = 0.86f)
                    )
                )
        )

        // Tactile Woven Book Spine Crease on Left Edge
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.44f),
                            Color.White.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Subtle dark right-edge contour falloff
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(10.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f)
                        )
                    )
                )
        )

        // Outer Luminous Glass Contour Rim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, brush = rimBrush, shape = bookShape)
        )

        // Editorial Monograph Typography & Telemetry Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 16.dp, top = if (slides.size > 1) 12.dp else 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (slides.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cover_carousel_bars_${summary.year}"),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        slides.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.26f))
                            ) {
                                val segmentFill = when {
                                    idx < safeSlideIndex -> 1f
                                    idx == safeSlideIndex -> if (isCarouselActive) slideProgress.value.coerceIn(0.08f, 1f) else 1f
                                    else -> 0f
                                }
                                if (segmentFill > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(segmentFill)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = if (idx == safeSlideIndex) 0.96f else 0.72f))
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "EDITION // ${summary.year % 100}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Text(
                            text = summary.year.toString(),
                            fontFamily = EditorialSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp,
                            lineHeight = 46.sp,
                            letterSpacing = (-1.0).sp,
                            color = Color.White
                        )
                    }

                    if (summary.unreviewedCount == 0 && summary.totalPhotos > 0) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldKeep,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "年度已整理",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(16.dp)
                            )
                        }
                    } else {
                        ObsidianGlassPill {
                            Text(
                                text = "${summary.unreviewedCount} 待理",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Frosted Obsidian Glass Archive Plate + Live Slide Context & Cover Customizer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        if (slides.size > 1 && displayedPhoto != null) {
                            ObsidianGlassPill {
                                AnimatedContent(
                                    targetState = displayedPhoto to safeSlideIndex,
                                    transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
                                    label = "cover_slide_meta"
                                ) { (slidePhoto, idx) ->
                                    val locationSuffix = slidePhoto.locationName?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                                    val slideLabel = if (locationSuffix.isNotEmpty()) {
                                        "${slidePhoto.month}月$locationSuffix"
                                    } else {
                                        "${slidePhoto.month}月精选 · ${idx + 1}/${slides.size}"
                                    }
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = SolarAmber,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = slideLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.5.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else if (summary.videoCount > 0) {
                            ObsidianGlassPill {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = SolarAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${summary.videoCount} 视频",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    if (isFocused) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    onClickLabel = stringResource(R.string.cd_customize_cover),
                                    onClick = onCustomizeCover
                                )
                                .testTag("customize_cover_button_${summary.year}")
                        ) {
                            ObsidianGlassPill {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = stringResource(R.string.cd_customize_cover),
                                        tint = SolarAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_customize_cover),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                ObsidianGlassCard(
                    shape = RoundedCornerShape(14.dp),
                    topColor = Color(0x88141720),
                    bottomColor = Color(0xA60E1016),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${summary.cleanedMonthsCount}/${summary.totalMonthsCount} 月归档",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Text(
                                text = "${summary.totalPhotos} ITEMS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = SolarAmber
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f))
                        ) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = summary.progressFraction,
                                animationSpec = spring(),
                                label = "book_progress"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.White, SolarAmber)
                                        )
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.cover_tap_flip_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
