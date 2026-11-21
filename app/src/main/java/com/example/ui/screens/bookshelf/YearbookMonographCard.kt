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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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

        // Editorial Vignette: whisper-light at top, rich soft falloff only at bottom for typography legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to coverColor.copy(alpha = 0.32f),
                        0.22f to Color.Transparent,
                        0.58f to Color.Transparent,
                        0.82f to coverColor.copy(alpha = 0.58f),
                        1.0f to coverColor.copy(alpha = 0.90f)
                    )
                )
        )

        // Physical Paper Page-Edge Stack along right edge (gives tactile hardcover monograph depth)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(5.dp)
                .fillMaxHeight()
                .padding(vertical = 3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color(0xFFF6F2EA).copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.16f),
                            Color(0xFFF6F2EA).copy(alpha = 0.46f),
                            Color.Black.copy(alpha = 0.24f)
                        )
                    )
                )
        )

        // Tactile Debossed Linen Book Spine & Hinge Groove on Left Edge
        Box(
            modifier = Modifier
                .width(17.dp)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.48f),
                        0.32f to Color.White.copy(alpha = 0.14f),
                        0.66f to Color.Black.copy(alpha = 0.30f),
                        0.78f to Color.White.copy(alpha = 0.22f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // Outer Luminous Glass & Foil Contour Rim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, brush = rimBrush, shape = bookShape)
        )

        // Minimalist Editorial Cover Overlay: center photography stays completely unobstructed
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Ultra-thin slideshow progress bars + refined frosted glass change-cover orb
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (slides.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 2.dp)
                            .testTag("cover_carousel_bars_${summary.year}"),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        slides.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.24f))
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
                                            .background(Color.White.copy(alpha = if (idx == safeSlideIndex) 0.95f else 0.62f))
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (summary.unreviewedCount == 0 && summary.totalPhotos > 0) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldKeep.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.44f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "年度已整理",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(15.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    if (isFocused) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .drawBehind {
                                    // Smooth circular ambient shadow halo (no HWUI elevation polygon bleed-through)
                                    val radiusPx = 21.dp.toPx()
                                    val shadowCenter = Offset(center.x, center.y + 1.5.dp.toPx())
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            0.0f to Color.Black.copy(alpha = 0.34f),
                                            0.68f to Color.Black.copy(alpha = 0.14f),
                                            1.0f to Color.Transparent,
                                            center = shadowCenter,
                                            radius = radiusPx
                                        ),
                                        radius = radiusPx,
                                        center = shadowCenter
                                    )
                                }
                                .clip(CircleShape)
                                .clickable(
                                    onClickLabel = stringResource(R.string.cd_customize_cover),
                                    onClick = onCustomizeCover
                                )
                                .testTag("customize_cover_button_${summary.year}")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x7A0C1018),
                                border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.36f)),
                                shadowElevation = 0.dp,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = stringResource(R.string.cd_customize_cover),
                                        tint = Color.White.copy(alpha = 0.94f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom: Museum Monograph Foil Typography directly over soft gradient (zero nested cards)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = summary.year.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = EditorialSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 52.sp,
                            lineHeight = 50.sp,
                            letterSpacing = (-1.4).sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.42f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                                blurRadius = 10f
                            )
                        ),
                        color = Color.White
                    )

                    if (summary.videoCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.30f),
                            border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = SolarAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${summary.videoCount}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }
                    }
                }

                // Hairline editorial rule separating year title from archive progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.75.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.36f),
                                    Color.White.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val archiveSummary = buildString {
                        append("${summary.totalPhotos} 项")
                        if (summary.totalMonthsCount > 0) {
                            append(" · 已归档 ${summary.cleanedMonthsCount}/${summary.totalMonthsCount} 月")
                        }
                    }
                    Text(
                        text = archiveSummary,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.sp,
                        letterSpacing = 0.2.sp,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Hairline archive progress bar with luminous champagne-amber tip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
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
            }
        }
    }
}
