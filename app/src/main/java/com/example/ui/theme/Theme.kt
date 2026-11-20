package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FlipYearLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltSoft,
    onPrimaryContainer = CobaltDark,
    secondary = SolarAmber,
    onSecondary = InkPrimary,
    secondaryContainer = SolarAmberSoft,
    onSecondaryContainer = InkPrimary,
    tertiary = EmeraldKeep,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldSoft,
    onTertiaryContainer = Color(0xFF065F46),
    error = CoralDanger,
    onError = Color.White,
    errorContainer = CoralSoft,
    onErrorContainer = CoralDanger,
    background = StudioCanvas,
    onBackground = InkPrimary,
    surface = SurfacePure,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = SurfacePure,
    surfaceContainerLow = StudioCanvas,
    surfaceContainer = SurfaceMuted,
    surfaceContainerHigh = SurfaceSubtle,
    outline = HairlineBorder,
    outlineVariant = SurfaceSubtle
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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlipYearLightColorScheme,
        typography = FlipYearTypography,
        shapes = FlipYearShapes,
        content = content
    )
}
