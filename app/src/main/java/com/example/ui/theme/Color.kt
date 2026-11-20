package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FlipYearPalette(
    val studioCanvas: Color,
    val studioCanvasTop: Color,
    val studioCanvasBottom: Color,
    val surfacePure: Color,
    val surfaceMuted: Color,
    val surfaceSubtle: Color,
    val hairlineBorder: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val cobaltPrimary: Color,
    val cobaltDark: Color,
    val cobaltSoft: Color,
    val coralDanger: Color,
    val coralSoft: Color,
    val emeraldKeep: Color,
    val emeraldSoft: Color,
    val solarAmber: Color,
    val solarAmberSoft: Color,
    val violetVideo: Color,
    val violetVideoSoft: Color,
    val skylightSapphireGlow: Color,
    val skylightChampagneGlow: Color,
    val skylightRoseGlow: Color,
    val glassLightTop: Color,
    val glassLightBottom: Color,
    val glassLightSubtle: Color,
    val glassSpecularRimStart: Color,
    val glassSpecularRimMid: Color,
    val glassSpecularRimEnd: Color,
    val activeChromeTop: Color,
    val activeChromeMid: Color,
    val activeChromeBottom: Color,
    val activeChromeBorderStart: Color,
    val activeChromeBorderEnd: Color
)

/**
 * Contemporary Fine-Art Gallery — Alabaster Daylight & Sculpted Obsidian Palette
 */
val LightFlipYearPalette = FlipYearPalette(
    studioCanvas = Color(0xFFF6F5F0),
    studioCanvasTop = Color(0xFFFDFCFA),
    studioCanvasBottom = Color(0xFFECEAE2),
    surfacePure = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEFECE6),
    surfaceSubtle = Color(0xFFE5E1D8),
    hairlineBorder = Color(0xFFDFDBD0),
    inkPrimary = Color(0xFF121316),
    inkSecondary = Color(0xFF595D68),
    inkTertiary = Color(0xFF8C909C),
    cobaltPrimary = Color(0xFF2548D6),
    cobaltDark = Color(0xFF1A35A8),
    cobaltSoft = Color(0xFFEAEEFE),
    coralDanger = Color(0xFFE5394E),
    coralSoft = Color(0xFFFDECEF),
    emeraldKeep = Color(0xFF0E9F6E),
    emeraldSoft = Color(0xFFE6F8F1),
    solarAmber = Color(0xFFD99429),
    solarAmberSoft = Color(0xFFFDF4E3),
    violetVideo = Color(0xFF6D42D8),
    violetVideoSoft = Color(0xFFF2ECFD),
    skylightSapphireGlow = Color(0x142548D6),
    skylightChampagneGlow = Color(0x18D99429),
    skylightRoseGlow = Color(0x10E5394E),
    glassLightTop = Color(0xD6FFFFFF),
    glassLightBottom = Color(0xB8F8F6F0),
    glassLightSubtle = Color(0x9EF7F5EE),
    glassSpecularRimStart = Color(0xEBFFFFFF),
    glassSpecularRimMid = Color(0x52FFFFFF),
    glassSpecularRimEnd = Color(0x99D8D3C6),
    activeChromeTop = Color(0xFF2B3245),
    activeChromeMid = Color(0xFF181D2B),
    activeChromeBottom = Color(0xFF0E1119),
    activeChromeBorderStart = Color(0x52FFFFFF),
    activeChromeBorderEnd = Color(0x24FFFFFF)
)

/**
 * Nocturnal Leica Darkroom — Velvet Obsidian Slate, Warm Parchment Ivory & Champagne Gold Palette
 */
val DarkFlipYearPalette = FlipYearPalette(
    studioCanvas = Color(0xFF0E1017),
    studioCanvasTop = Color(0xFF141823),
    studioCanvasBottom = Color(0xFF090B10),
    surfacePure = Color(0xFF181C26),
    surfaceMuted = Color(0xFF202634),
    surfaceSubtle = Color(0xFF293042),
    hairlineBorder = Color(0xFF30384C),
    inkPrimary = Color(0xFFF3F1EC),
    inkSecondary = Color(0xFFA7AEBF),
    inkTertiary = Color(0xFF727A8E),
    cobaltPrimary = Color(0xFF6B8FFF),
    cobaltDark = Color(0xFFA8C1FF),
    cobaltSoft = Color(0xFF1A2542),
    coralDanger = Color(0xFFFF6477),
    coralSoft = Color(0xFF381922),
    emeraldKeep = Color(0xFF2DD49A),
    emeraldSoft = Color(0xFF123027),
    solarAmber = Color(0xFFF0B44C),
    solarAmberSoft = Color(0xFF332613),
    violetVideo = Color(0xFFA685FF),
    violetVideoSoft = Color(0xFF261D3E),
    skylightSapphireGlow = Color(0x284B73FF),
    skylightChampagneGlow = Color(0x22F0B44C),
    skylightRoseGlow = Color(0x1EFF6477),
    glassLightTop = Color(0xDB222838),
    glassLightBottom = Color(0xC9161A25),
    glassLightSubtle = Color(0xB31B202E),
    glassSpecularRimStart = Color(0x47FFFFFF),
    glassSpecularRimMid = Color(0x1CFFFFFF),
    glassSpecularRimEnd = Color(0x368AA8FF),
    activeChromeTop = Color(0xFF3956A8),
    activeChromeMid = Color(0xFF293E7E),
    activeChromeBottom = Color(0xFF1D2C5C),
    activeChromeBorderStart = Color(0x70A8C1FF),
    activeChromeBorderEnd = Color(0x336B8FFF)
)

val LocalFlipYearPalette = staticCompositionLocalOf { LightFlipYearPalette }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

// Reactive Semantic Gallery Palette Tokens
val StudioCanvas: Color
    @Composable get() = LocalFlipYearPalette.current.studioCanvas
val StudioCanvasTop: Color
    @Composable get() = LocalFlipYearPalette.current.studioCanvasTop
val StudioCanvasBottom: Color
    @Composable get() = LocalFlipYearPalette.current.studioCanvasBottom

val SurfacePure: Color
    @Composable get() = LocalFlipYearPalette.current.surfacePure
val SurfaceMuted: Color
    @Composable get() = LocalFlipYearPalette.current.surfaceMuted
val SurfaceSubtle: Color
    @Composable get() = LocalFlipYearPalette.current.surfaceSubtle
val HairlineBorder: Color
    @Composable get() = LocalFlipYearPalette.current.hairlineBorder

val InkPrimary: Color
    @Composable get() = LocalFlipYearPalette.current.inkPrimary
val InkSecondary: Color
    @Composable get() = LocalFlipYearPalette.current.inkSecondary
val InkTertiary: Color
    @Composable get() = LocalFlipYearPalette.current.inkTertiary

// Curated Museum Accents (International Klein Sapphire, Gallery Vermilion, Botanical Jade, Champagne Gold)
val CobaltPrimary: Color
    @Composable get() = LocalFlipYearPalette.current.cobaltPrimary
val CobaltDark: Color
    @Composable get() = LocalFlipYearPalette.current.cobaltDark
val CobaltSoft: Color
    @Composable get() = LocalFlipYearPalette.current.cobaltSoft

val CoralDanger: Color
    @Composable get() = LocalFlipYearPalette.current.coralDanger
val CoralSoft: Color
    @Composable get() = LocalFlipYearPalette.current.coralSoft

val EmeraldKeep: Color
    @Composable get() = LocalFlipYearPalette.current.emeraldKeep
val EmeraldSoft: Color
    @Composable get() = LocalFlipYearPalette.current.emeraldSoft

val SolarAmber: Color
    @Composable get() = LocalFlipYearPalette.current.solarAmber
val SolarAmberSoft: Color
    @Composable get() = LocalFlipYearPalette.current.solarAmberSoft

val VioletVideo: Color
    @Composable get() = LocalFlipYearPalette.current.violetVideo
val VioletVideoSoft: Color
    @Composable get() = LocalFlipYearPalette.current.violetVideoSoft

// Ambient Gallery Skylight Wash Tones
val SkylightSapphireGlow: Color
    @Composable get() = LocalFlipYearPalette.current.skylightSapphireGlow
val SkylightChampagneGlow: Color
    @Composable get() = LocalFlipYearPalette.current.skylightChampagneGlow
val SkylightRoseGlow: Color
    @Composable get() = LocalFlipYearPalette.current.skylightRoseGlow

// Subtle Glassmorphism Translucent Surface & Specular Rim Tokens
val GlassLightTop: Color
    @Composable get() = LocalFlipYearPalette.current.glassLightTop
val GlassLightBottom: Color
    @Composable get() = LocalFlipYearPalette.current.glassLightBottom
val GlassLightSubtle: Color
    @Composable get() = LocalFlipYearPalette.current.glassLightSubtle
val GlassSpecularRimStart: Color
    @Composable get() = LocalFlipYearPalette.current.glassSpecularRimStart
val GlassSpecularRimMid: Color
    @Composable get() = LocalFlipYearPalette.current.glassSpecularRimMid
val GlassSpecularRimEnd: Color
    @Composable get() = LocalFlipYearPalette.current.glassSpecularRimEnd

// Collector Monograph Hardcover Spine & Editorial Accent Tones (invariant across themes)
val EditionObsidian = Color(0xFF16181F)
val EditionCobalt = Color(0xFF1B2E5C)
val EditionTeal = Color(0xFF143C42)
val EditionSlatePlum = Color(0xFF2D253B)

// Dark Smoked Glass & Sculpted Obsidian Media Overlay Chrome (invariant darkroom material over imagery)
val GlassObsidianTop = Color(0xB3151820)      // ~70% dark smoked glass
val GlassObsidianBottom = Color(0x8C0E1016)   // ~55% deep obsidian frost
val GlassDarkRimStart = Color(0x6BFFFFFF)
val GlassDarkRimMid = Color(0x24FFFFFF)
val GlassDarkRimEnd = Color(0x3DFFFFFF)

val ObsidianSatinTop = Color(0xFF2B3245)
val ObsidianSatinMid = Color(0xFF181D2B)
val ObsidianSatinBottom = Color(0xFF0E1119)
val ObsidianOverlayOrb = Color(0xB31A2232)         // ~70% — media play orbs, icon buttons on imagery
val ObsidianOverlayGlass = Color(0xCC1C2433)       // ~80% — badges & pills floating on media
val ObsidianOverlayPanel = Color(0xF21B2331)       // ~95% — docks & panels over scrolling content
val ObsidianOverlayChipOnDark = Color(0x1FFFFFFF)  // ~12% white — small chips inside dark docks
val ObsidianOverlayBorder = Color(0x38FFFFFF)      // ~22% hairline rim on dark chrome
val ObsidianOverlayBorderStrong = Color(0xA6FFFFFF)// ~65% — hero play orb emphasis rim
