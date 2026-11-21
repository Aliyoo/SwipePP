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
import androidx.compose.ui.graphics.SolidColor
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
import com.example.ui.theme.DecoInkDeep
import com.example.ui.theme.DecoPaperCream
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
    DANGER_GLASS,

    /**
     * 摩登画报 Deco paper button: 浅象牙底 + 深墨字，浅暗主题同值（design-demos/deco-app.html 纸材）
     */
    IMMERSIVE
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
    shape: Shape? = null,
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

    // 未显式传 shape 时按分支取默认：IMMERSIVE 画报纸面 = 3dp（画片圆角语法），
    // 其余 satin/glass 分支维持旧 16dp 胶囊
    val resolvedShape = shape
        ?: if (tone == GalleryButtonTone.IMMERSIVE) RoundedCornerShape(3.dp) else RoundedCornerShape(16.dp)

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
            // 画报纸面：浅象牙平涂，浅暗主题同值
            GalleryButtonTone.IMMERSIVE -> SolidColor(DecoPaperCream)
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
            // 画报金描边（主题金，呼应金双线画框）
            GalleryButtonTone.IMMERSIVE -> Brush.linearGradient(
                colors = listOf(palette.decoGold.copy(alpha = 0.92f), palette.decoGold, palette.decoGold.copy(alpha = 0.72f))
            )
        }
    }

    val contentColor = when (tone) {
        GalleryButtonTone.OBSIDIAN, GalleryButtonTone.SAPPHIRE, GalleryButtonTone.DANGER -> Color.White
        GalleryButtonTone.GLASS -> inkPrimary
        GalleryButtonTone.DANGER_GLASS -> coral
        // 画报深墨字，浅暗主题同值
        GalleryButtonTone.IMMERSIVE -> DecoInkDeep
    }

    val resolvedIconTint = iconTint ?: when (tone) {
        GalleryButtonTone.OBSIDIAN -> SolarAmber
        GalleryButtonTone.SAPPHIRE, GalleryButtonTone.DANGER -> Color.White
        GalleryButtonTone.GLASS -> cobalt
        GalleryButtonTone.DANGER_GLASS -> coral
        GalleryButtonTone.IMMERSIVE -> DecoInkDeep
    }

    val shadowColor = when (tone) {
        GalleryButtonTone.OBSIDIAN -> (if (isDark) cobalt else inkPrimary).copy(alpha = if (isDark) 0.38f else 0.34f)
        GalleryButtonTone.SAPPHIRE -> cobalt.copy(alpha = 0.36f)
        GalleryButtonTone.DANGER -> coral.copy(alpha = 0.34f)
        GalleryButtonTone.GLASS -> (if (isDark) Color.Black else inkPrimary).copy(alpha = if (isDark) 0.42f else 0.12f)
        GalleryButtonTone.DANGER_GLASS -> coral.copy(alpha = 0.18f)
        GalleryButtonTone.IMMERSIVE -> DecoInkDeep.copy(alpha = 0.22f)
    }

    val topSheenColor = when (tone) {
        GalleryButtonTone.GLASS, GalleryButtonTone.DANGER_GLASS ->
            Color.White.copy(alpha = if (isDark) 0.16f else 0.72f)
        GalleryButtonTone.IMMERSIVE -> Color.Transparent // 画报纸面无高光
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
                shape = resolvedShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(resolvedShape)
            .drawBehind {
                drawRect(brush = fillBrush)
                drawLine(
                    color = topSheenColor,
                    start = Offset(0f, 0.75.dp.toPx()),
                    end = Offset(size.width, 0.75.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(width = 1.dp, brush = borderBrush, shape = resolvedShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(
                    color = when (tone) {
                        GalleryButtonTone.IMMERSIVE -> DecoInkDeep
                        GalleryButtonTone.GLASS -> inkPrimary
                        else -> Color.White
                    }
                ),
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
    modifier: Modifier = Modifier,
    orbSize: Dp = 62.dp,
    iconSize: Dp = 26.dp
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
                    Color(0xFF262D40),
                    tintSurface.copy(alpha = 1f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    tintSurface.copy(alpha = 1f)
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
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(orbSize)
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
                modifier = Modifier.size(iconSize)
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

/**
 * Tactile 38dp frosted circular glass orb wrapped in a 48dp accessible hit area for top header
 * navigation and utility controls across SwipePP.
 */
@Composable
fun GalleryHeaderOrbButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val surfacePure = com.example.ui.theme.SurfacePure
    val hairline = com.example.ui.theme.HairlineBorder
    val rimStart = GlassSpecularRimStart
    val shadowTint = if (isDark) Color.Black else InkPrimary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .drawBehind {
                if (enabled) {
                    // Pure circular radial soft shadow (avoids Android HWUI tessellated polygon
                    // bleed-through that occurs when shadowElevation is placed on translucent circles)
                    val radiusPx = 22.dp.toPx()
                    val shadowCenter = Offset(center.x, center.y + 1.5.dp.toPx())
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to shadowTint.copy(alpha = if (isDark) 0.34f else 0.14f),
                            0.68f to shadowTint.copy(alpha = if (isDark) 0.16f else 0.06f),
                            1.0f to Color.Transparent,
                            center = shadowCenter,
                            radius = radiusPx
                        ),
                        radius = radiusPx,
                        center = shadowCenter
                    )
                }
            }
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        androidx.compose.material3.Surface(
            shape = CircleShape,
            color = if (isDark) surfacePure.copy(alpha = if (enabled) 0.78f else 0.42f)
            else surfacePure.copy(alpha = if (enabled) 0.94f else 0.58f),
            border = androidx.compose.foundation.BorderStroke(
                0.9.dp,
                if (isDark) hairline.copy(alpha = if (enabled) 1f else 0.45f)
                else rimStart.copy(alpha = if (enabled) 1f else 0.55f)
            ),
            shadowElevation = 0.dp,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/**
 * Flagship sculpted satin full-width CTA pairing SwipePP's obsidian/danger sheen with a crisp
 * leading icon, unmistakable title, and an optional embedded monospace telemetry badge.
 */
@Composable
fun GallerySculptedHeroButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    badgeText: String? = null,
    tone: GalleryButtonTone = GalleryButtonTone.OBSIDIAN,
    height: Dp = 54.dp,
    shape: Shape? = null,
    enabled: Boolean = true
) {
    val isDark = LocalIsDarkTheme.current
    val palette = LocalFlipYearPalette.current
    val activeBrush = activeChromeSatinBrush()
    val cobalt = CobaltPrimary
    val cobaltDark = CobaltDark
    val coral = CoralDanger
    val inkPrimary = InkPrimary

    // 未显式传 shape 时按分支取默认：IMMERSIVE 画报纸面 = 3dp（画片圆角语法），
    // 其余 satin 分支维持旧 18dp 胶囊
    val resolvedShape = shape
        ?: if (tone == GalleryButtonTone.IMMERSIVE) RoundedCornerShape(3.dp) else RoundedCornerShape(18.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sculpted_hero_scale"
    )

    val fillBrush = remember(tone, isDark, activeBrush, cobalt, cobaltDark, coral) {
        when (tone) {
            GalleryButtonTone.DANGER -> Brush.linearGradient(
                colors = listOf(Color(0xFFF24B61), coral, Color(0xFFC42339))
            )
            GalleryButtonTone.SAPPHIRE -> if (isDark) {
                Brush.linearGradient(colors = listOf(Color(0xFF4F76FF), Color(0xFF3457D9), Color(0xFF233DA6)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFF395EF2), cobalt, cobaltDark))
            }
            // 画报纸面：浅象牙平涂，浅暗主题同值
            GalleryButtonTone.IMMERSIVE -> SolidColor(DecoPaperCream)
            else -> activeBrush
        }
    }
    val borderBrush = remember(tone, palette, coral) {
        when (tone) {
            GalleryButtonTone.DANGER -> Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.12f), Color(0xFFFFA5B1).copy(alpha = 0.34f))
            )
            // 画报金描边（主题金，呼应金双线画框）
            GalleryButtonTone.IMMERSIVE -> Brush.linearGradient(
                colors = listOf(palette.decoGold.copy(alpha = 0.92f), palette.decoGold, palette.decoGold.copy(alpha = 0.72f))
            )
            else -> Brush.linearGradient(
                colors = listOf(
                    palette.activeChromeBorderStart,
                    Color.White.copy(alpha = 0.14f),
                    palette.activeChromeBorderEnd
                )
            )
        }
    }
    val shadowColor = when (tone) {
        GalleryButtonTone.DANGER -> coral.copy(alpha = 0.36f)
        GalleryButtonTone.IMMERSIVE -> DecoInkDeep.copy(alpha = 0.22f)
        else -> (if (isDark) cobalt else inkPrimary).copy(alpha = if (isDark) 0.40f else 0.30f)
    }
    val contentColor = if (tone == GalleryButtonTone.IMMERSIVE) DecoInkDeep else Color.White
    val iconTint = when (tone) {
        GalleryButtonTone.OBSIDIAN -> SolarAmber
        GalleryButtonTone.IMMERSIVE -> DecoInkDeep
        else -> Color.White
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
                elevation = if (pressed) 5.dp else 11.dp,
                shape = resolvedShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(resolvedShape)
            .drawBehind {
                drawRect(brush = fillBrush)
                drawLine(
                    color = if (tone == GalleryButtonTone.IMMERSIVE) Color.Transparent else Color.White.copy(alpha = 0.26f),
                    start = Offset(0f, 0.8.dp.toPx()),
                    end = Offset(size.width, 0.8.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(width = 1.dp, brush = borderBrush, shape = resolvedShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = badgeText != null)
            )
            if (!badgeText.isNullOrBlank()) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(50),
                    color = if (tone == GalleryButtonTone.IMMERSIVE) DecoInkDeep.copy(alpha = 0.10f)
                    else Color.White.copy(alpha = if (tone == GalleryButtonTone.DANGER) 0.18f else 0.14f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.75.dp,
                        if (tone == GalleryButtonTone.IMMERSIVE) DecoInkDeep.copy(alpha = 0.24f)
                        else Color.White.copy(alpha = 0.24f)
                    )
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = com.example.ui.theme.JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = when (tone) {
                            GalleryButtonTone.IMMERSIVE -> DecoInkDeep
                            GalleryButtonTone.OBSIDIAN -> Color(0xFFFDE68A)
                            else -> Color.White
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
