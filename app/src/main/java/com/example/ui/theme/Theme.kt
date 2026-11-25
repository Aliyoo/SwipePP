package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

// Material3 底层令牌全面映射到摩登画报（Modern Deco Pictorial）语法：
// primary=金（正向/激活）、error=朱红（决断/危险）、surface/background=纸地墨绿、onX=画片墨。
// 旧「克莱因蓝玻璃拟态」色板已淘汰——任何回落到 M3 默认着色的组件（对话框、焦点态、
// 水波纹、Snackbar）都必须呈现画报纸金语法而非蓝色。
private val FlipYearLightColorScheme = lightColorScheme(
    primary = LightFlipYearPalette.decoGold,
    onPrimary = DecoPaperCream,
    primaryContainer = LightFlipYearPalette.decoMat,
    onPrimaryContainer = LightFlipYearPalette.decoInkOnGround,
    secondary = LightFlipYearPalette.decoGold,
    onSecondary = DecoPaperCream,
    secondaryContainer = LightFlipYearPalette.decoGroundDeep,
    onSecondaryContainer = LightFlipYearPalette.decoInkOnGround,
    tertiary = LightFlipYearPalette.decoVerm,
    onTertiary = DecoPaperCream,
    tertiaryContainer = LightFlipYearPalette.decoMat,
    onTertiaryContainer = LightFlipYearPalette.decoVerm,
    error = LightFlipYearPalette.decoVerm,
    onError = DecoPaperCream,
    errorContainer = LightFlipYearPalette.decoMat,
    onErrorContainer = LightFlipYearPalette.decoVerm,
    background = LightFlipYearPalette.decoGround,
    onBackground = LightFlipYearPalette.decoInkOnGround,
    surface = LightFlipYearPalette.decoGround,
    onSurface = LightFlipYearPalette.decoInkOnGround,
    surfaceVariant = LightFlipYearPalette.decoGroundDeep,
    onSurfaceVariant = LightFlipYearPalette.inkSecondary,
    surfaceContainerLowest = LightFlipYearPalette.decoMat,
    surfaceContainerLow = LightFlipYearPalette.decoGround,
    surfaceContainer = LightFlipYearPalette.decoGroundDeep,
    surfaceContainerHigh = LightFlipYearPalette.decoMat,
    outline = LightFlipYearPalette.decoGold,
    outlineVariant = LightFlipYearPalette.decoGold.copy(alpha = 0.4f)
)

private val FlipYearDarkColorScheme = darkColorScheme(
    primary = DarkFlipYearPalette.decoGold,
    onPrimary = DecoInkDeep,
    primaryContainer = DarkFlipYearPalette.decoGroundDeep,
    onPrimaryContainer = DarkFlipYearPalette.decoInkOnGround,
    secondary = DarkFlipYearPalette.decoGold,
    onSecondary = DecoInkDeep,
    secondaryContainer = DarkFlipYearPalette.decoGroundDeep,
    onSecondaryContainer = DarkFlipYearPalette.decoInkOnGround,
    tertiary = DarkFlipYearPalette.decoVermInk,
    onTertiary = DecoInkDeep,
    tertiaryContainer = DarkFlipYearPalette.decoGroundDeep,
    onTertiaryContainer = DarkFlipYearPalette.decoVermInk,
    error = DarkFlipYearPalette.decoVermInk,
    onError = DecoInkDeep,
    errorContainer = DarkFlipYearPalette.decoVerm,
    onErrorContainer = DecoPaperCream,
    background = DarkFlipYearPalette.decoGround,
    onBackground = DarkFlipYearPalette.decoInkOnGround,
    surface = DarkFlipYearPalette.decoGround,
    onSurface = DarkFlipYearPalette.decoInkOnGround,
    surfaceVariant = DarkFlipYearPalette.decoGroundDeep,
    onSurfaceVariant = DarkFlipYearPalette.inkSecondary,
    surfaceContainerLowest = DarkFlipYearPalette.decoGroundDeep,
    surfaceContainerLow = DarkFlipYearPalette.decoGround,
    surfaceContainer = DarkFlipYearPalette.decoGroundDeep,
    surfaceContainerHigh = DarkFlipYearPalette.decoMat,
    outline = DarkFlipYearPalette.decoGold,
    outlineVariant = DarkFlipYearPalette.decoGold.copy(alpha = 0.4f)
)

// 画片方角语法：2~4dp 微圆角（design-demos/deco-app.html GoldFrame radius=2），
// 覆盖全部 M3 形状令牌，杜绝旧玻璃拟态时代 18~30dp 大圆角从默认槽位漏出
val FlipYearShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
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
