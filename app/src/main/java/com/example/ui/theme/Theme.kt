package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FlipYearLightColorScheme = lightColorScheme(
    primary = LightFlipYearPalette.cobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = LightFlipYearPalette.cobaltSoft,
    onPrimaryContainer = LightFlipYearPalette.cobaltDark,
    secondary = LightFlipYearPalette.solarAmber,
    onSecondary = LightFlipYearPalette.inkPrimary,
    secondaryContainer = LightFlipYearPalette.solarAmberSoft,
    onSecondaryContainer = LightFlipYearPalette.inkPrimary,
    tertiary = LightFlipYearPalette.emeraldKeep,
    onTertiary = Color.White,
    tertiaryContainer = LightFlipYearPalette.emeraldSoft,
    onTertiaryContainer = Color(0xFF065F46),
    error = LightFlipYearPalette.coralDanger,
    onError = Color.White,
    errorContainer = LightFlipYearPalette.coralSoft,
    onErrorContainer = LightFlipYearPalette.coralDanger,
    background = LightFlipYearPalette.studioCanvas,
    onBackground = LightFlipYearPalette.inkPrimary,
    surface = LightFlipYearPalette.surfacePure,
    onSurface = LightFlipYearPalette.inkPrimary,
    surfaceVariant = LightFlipYearPalette.surfaceMuted,
    onSurfaceVariant = LightFlipYearPalette.inkSecondary,
    surfaceContainerLowest = LightFlipYearPalette.surfacePure,
    surfaceContainerLow = LightFlipYearPalette.studioCanvas,
    surfaceContainer = LightFlipYearPalette.surfaceMuted,
    surfaceContainerHigh = LightFlipYearPalette.surfaceSubtle,
    outline = LightFlipYearPalette.hairlineBorder,
    outlineVariant = LightFlipYearPalette.surfaceSubtle
)

private val FlipYearDarkColorScheme = darkColorScheme(
    primary = DarkFlipYearPalette.cobaltPrimary,
    onPrimary = Color(0xFF09132B),
    primaryContainer = DarkFlipYearPalette.cobaltSoft,
    onPrimaryContainer = DarkFlipYearPalette.cobaltDark,
    secondary = DarkFlipYearPalette.solarAmber,
    onSecondary = Color(0xFF1F1606),
    secondaryContainer = DarkFlipYearPalette.solarAmberSoft,
    onSecondaryContainer = DarkFlipYearPalette.solarAmber,
    tertiary = DarkFlipYearPalette.emeraldKeep,
    onTertiary = Color(0xFF042218),
    tertiaryContainer = DarkFlipYearPalette.emeraldSoft,
    onTertiaryContainer = DarkFlipYearPalette.emeraldKeep,
    error = DarkFlipYearPalette.coralDanger,
    onError = Color(0xFF2A060C),
    errorContainer = DarkFlipYearPalette.coralSoft,
    onErrorContainer = DarkFlipYearPalette.coralDanger,
    background = DarkFlipYearPalette.studioCanvas,
    onBackground = DarkFlipYearPalette.inkPrimary,
    surface = DarkFlipYearPalette.surfacePure,
    onSurface = DarkFlipYearPalette.inkPrimary,
    surfaceVariant = DarkFlipYearPalette.surfaceMuted,
    onSurfaceVariant = DarkFlipYearPalette.inkSecondary,
    surfaceContainerLowest = DarkFlipYearPalette.studioCanvasBottom,
    surfaceContainerLow = DarkFlipYearPalette.studioCanvas,
    surfaceContainer = DarkFlipYearPalette.surfacePure,
    surfaceContainerHigh = DarkFlipYearPalette.surfaceMuted,
    outline = DarkFlipYearPalette.hairlineBorder,
    outlineVariant = DarkFlipYearPalette.surfaceSubtle
)

val FlipYearShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun FlipYearTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = resolveIsDarkTheme(themeMode, isSystemInDarkTheme())
    val palette = if (isDark) DarkFlipYearPalette else LightFlipYearPalette
    val colorScheme = if (isDark) FlipYearDarkColorScheme else FlipYearLightColorScheme

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark,
        LocalFlipYearPalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FlipYearTypography,
            shapes = FlipYearShapes,
            content = content
        )
    }
}
