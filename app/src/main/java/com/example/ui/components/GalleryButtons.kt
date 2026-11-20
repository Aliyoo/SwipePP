package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.GlassLightBottom
import com.example.ui.theme.GlassLightTop
import com.example.ui.theme.GlassSpecularRimEnd
import com.example.ui.theme.GlassSpecularRimMid
import com.example.ui.theme.GlassSpecularRimStart
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarAmber

enum class GalleryButtonTone {
    /** Primary signature sculpted satin with Champagne Gold accent */
    OBSIDIAN,

    /** Curated International Klein Sapphire satin gradient for key cleanup actions */
    SAPPHIRE,

    /** Gallery Vermilion satin gradient for destructive confirmation */
    DANGER,

    /** Frosted translucent glass for secondary actions */
    GLASS,

    /** Soft rose-tinted frosted glass for non-primary destructive actions */
    DANGER_GLASS
}

/**
 * Unified sculpted satin/glass action button across FlipYear, calibrated for both Alabaster Daylight
 * and Nocturnal Leica Darkroom palettes.
 */
@Composable
fun GalleryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: GalleryButtonTone = GalleryButtonTone.OBSIDIAN,
    leadingIcon: ImageVector? = null,
    iconTint: Color? = null,
    height: Dp = 50.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    enabled: Boolean = true
) {
    val isDark = LocalIsDarkTheme.current
    val palette = LocalFlipYearPalette.current
    val activeBrush = activeChromeSatinBrush()
    val glassTop = GlassLightTop
    val glassBottom = GlassLightBottom
    val rimStart = GlassSpecularRimStart
    val rimMid = GlassSpecularRimMid
    val rimEnd = GlassSpecularRimEnd
    val cobalt = CobaltPrimary
    val cobaltDark = CobaltDark
    val coral = CoralDanger
    val inkPrimary = InkPrimary

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.972f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gallery_btn_scale"
    )

    val fillBrush = remember(tone, isDark, activeBrush, glassTop, glassBottom, cobalt, cobaltDark, coral) {
        when (tone) {
            GalleryButtonTone.OBSIDIAN -> activeBrush
            GalleryButtonTone.SAPPHIRE -> if (isDark) {
                Brush.linearGradient(colors = listOf(Color(0xFF4F76FF), Color(0xFF3457D9), Color(0xFF233DA6)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFF395EF2), cobalt, cobaltDark))
            }
            GalleryButtonTone.DANGER -> Brush.linearGradient(
                colors = listOf(Color(0xFFF24B61), coral, Color(0xFFC42339))
            )
            GalleryButtonTone.GLASS -> Brush.verticalGradient(
                colors = listOf(glassTop, glassBottom)
            )
            GalleryButtonTone.DANGER_GLASS -> if (isDark) {
                Brush.verticalGradient(colors = listOf(Color(0xD93D1A25), Color(0xCC2B111A)))
            } else {
                Brush.verticalGradient(colors = listOf(Color(0xE8FFF2F4), Color(0xD4FDE5E9)))
            }
        }
    }

    val borderBrush = remember(tone, isDark, palette, rimStart, rimMid, rimEnd, coral) {
        when (tone) {
            GalleryButtonTone.OBSIDIAN -> Brush.linearGradient(
                colors = listOf(palette.activeChromeBorderStart, Color.White.copy(alpha = 0.08f), palette.activeChromeBorderEnd)
            )
            GalleryButtonTone.SAPPHIRE -> Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.12f), Color(0xFF8AA4FF).copy(alpha = 0.34f))
            )
            GalleryButtonTone.DANGER -> Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.10f), Color(0xFFFFA5B1).copy(alpha = 0.30f))
            )
            GalleryButtonTone.GLASS -> Brush.linearGradient(
                colors = listOf(rimStart, rimMid, rimEnd)
            )
            GalleryButtonTone.DANGER_GLASS -> Brush.linearGradient(
                colors = if (isDark) {
                    listOf(coral.copy(alpha = 0.55f), coral.copy(alpha = 0.28f), coral.copy(alpha = 0.18f))
                } else {
                    listOf(Color.White.copy(alpha = 0.85f), coral.copy(alpha = 0.34f), coral.copy(alpha = 0.22f))
                }
            )
        }
    }

    val contentColor = when (tone) {
        GalleryButtonTone.OBSIDIAN, GalleryButtonTone.SAPPHIRE, GalleryButtonTone.DANGER -> Color.White
        GalleryButtonTone.GLASS -> inkPrimary
        GalleryButtonTone.DANGER_GLASS -> coral
    }

    val resolvedIconTint = iconTint ?: when (tone) {
        GalleryButtonTone.OBSIDIAN -> SolarAmber
        GalleryButtonTone.SAPPHIRE, GalleryButtonTone.DANGER -> Color.White
        GalleryButtonTone.GLASS -> cobalt
        GalleryButtonTone.DANGER_GLASS -> coral
    }

    val shadowColor = when (tone) {
        GalleryButtonTone.OBSIDIAN -> (if (isDark) cobalt else inkPrimary).copy(alpha = if (isDark) 0.38f else 0.34f)
        GalleryButtonTone.SAPPHIRE -> cobalt.copy(alpha = 0.36f)
        GalleryButtonTone.DANGER -> coral.copy(alpha = 0.34f)
        GalleryButtonTone.GLASS -> (if (isDark) Color.Black else inkPrimary).copy(alpha = if (isDark) 0.42f else 0.12f)
        GalleryButtonTone.DANGER_GLASS -> coral.copy(alpha = 0.18f)
    }

    val topSheenColor = when (tone) {
        GalleryButtonTone.GLASS, GalleryButtonTone.DANGER_GLASS ->
            Color.White.copy(alpha = if (isDark) 0.16f else 0.72f)
        else -> Color.White.copy(alpha = 0.24f)
    }

    val elevation = when (tone) {
        GalleryButtonTone.GLASS, GalleryButtonTone.DANGER_GLASS -> 4.dp
        else -> 10.dp
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (enabled) 1f else 0.50f
            }
            .shadow(
                elevation = if (pressed) elevation * 0.5f else elevation,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .drawBehind {
                drawRect(brush = fillBrush)
                drawLine(
                    color = topSheenColor,
                    start = Offset(0f, 0.75.dp.toPx()),
                    end = Offset(size.width, 0.75.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = if (tone == GalleryButtonTone.GLASS) inkPrimary else Color.White),
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Unified sculptural filter & selector capsule chip across Bookshelf, Gallery, Flipbook, and Map.
 */
@Composable
fun GallerySelectableChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    unselectedTint: Color? = null,
    unselectedBorderAccent: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val palette = LocalFlipYearPalette.current
    val activeBrush = activeChromeSatinBrush()
    val glassTop = GlassLightTop
    val glassBottom = GlassLightBottom
    val rimStart = GlassSpecularRimStart
    val rimMid = GlassSpecularRimMid
    val rimEnd = GlassSpecularRimEnd
    val inkPrimary = InkPrimary

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chip_scale"
    )

    val inactiveBrush = remember(unselectedTint, glassTop, glassBottom) {
        if (unselectedTint != null) {
            Brush.verticalGradient(colors = listOf(unselectedTint.copy(alpha = 0.92f), unselectedTint.copy(alpha = 0.78f)))
        } else {
            Brush.verticalGradient(colors = listOf(glassTop, glassBottom))
        }
    }
    val borderBrush = remember(selected, unselectedBorderAccent, isDark, palette, rimStart, rimMid, rimEnd) {
        if (selected) {
            Brush.linearGradient(
                colors = listOf(palette.activeChromeBorderStart, palette.activeChromeBorderEnd)
            )
        } else if (unselectedBorderAccent != null) {
            Brush.linearGradient(
                colors = listOf(
                    if (isDark) unselectedBorderAccent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.85f),
                    unselectedBorderAccent.copy(alpha = 0.40f)
                )
            )
        } else {
            Brush.linearGradient(colors = listOf(rimStart, rimMid, rimEnd))
        }
    }

    val shadowInk = if (isDark) Color.Black else inkPrimary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = if (selected) 6.dp else 3.dp,
                shape = shape,
                ambientColor = shadowInk.copy(alpha = if (selected) 0.28f else 0.14f),
                spotColor = shadowInk.copy(alpha = if (selected) 0.34f else 0.18f)
            )
            .clip(shape)
            .drawBehind {
                drawRect(brush = if (selected) activeBrush else inactiveBrush)
            }
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = if (selected) Color.White else inkPrimary),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            content = content
        )
    }
}

/**
 * Sculptural frosted-glass circular lens-ring orb button for Arc Cleaner primary triage actions.
 */
@Composable
fun GalleryTactileOrbButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    tintSurface: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "orb_scale"
    )

    val fillBrush = remember(tintSurface, isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xEB262D40),
                    tintSurface.copy(alpha = 0.94f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    tintSurface.copy(alpha = 0.92f)
                )
            )
        }
    }
    val rimBrush = remember(accentColor, isDark) {
        Brush.linearGradient(
            colors = listOf(
                if (isDark) accentColor.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.95f),
                accentColor.copy(alpha = 0.38f),
                accentColor.copy(alpha = 0.52f)
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(62.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(
                    elevation = if (pressed) 4.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = if (isDark) 0.30f else 0.24f),
                    spotColor = accentColor.copy(alpha = if (isDark) 0.42f else 0.34f)
                )
                .clip(CircleShape)
                .drawBehind {
                    drawRect(brush = fillBrush)
                }
                .border(width = 1.25.dp, brush = rimBrush, shape = CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = accentColor),
                    onClick = onClick
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = InkSecondary
        )
    }
}
