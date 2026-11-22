package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Editorial 衬线（拉丁美术字专用：品牌字标 SwipePP、纯数字装饰）。
 * 字重规格：家族只注册 Normal(400) ~ Bold(700)，无 Black(900) 字面（res/font 仅一个
 * playfair_display.ttf）；任何 FontWeight.Black(900) 请求都会由 Compose 家族解析回落到
 * Bold(700) 渲染。因此美术字一律显式请求 FontWeight.Bold —— 代码即真值。
 *
 * 注意：Playfair 无汉字字形 —— 中文展示字一律改用 [DecoDisplayFontFamily]，
 * 否则汉字会回退系统黑体、丢失画报美术字语感。
 */
val EditorialSerifFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

/**
 * 站酷小薇（画报美术字·汉字展示专用，原型同源字体 ZCOOL XiaoWei，OFL 授权）。
 * 上游字体的囗封闭结构字轮廓同向绕行（TrueType nonzero 下镂空被填成实心块），
 * 经大字号逐字实证，真缺陷仅 8 字：回圃圊徊痼圄崮蛔——已用内轮廓反向修复
 * （回 呈「外环 + 内实心口」，用户确认的小薇原生设计味）；其余字形一律原生保留。
 * 剥离 fpgm/prep/cvt/gasp 提示表保证渲染一致。单字重(400)：Bold/Medium 请求都解析到本字面。
 */
val DecoDisplayFontFamily = FontFamily(
    Font(R.font.zcool_xiaowei, FontWeight.Normal),
    Font(R.font.zcool_xiaowei, FontWeight.Medium),
    Font(R.font.zcool_xiaowei, FontWeight.SemiBold),
    Font(R.font.zcool_xiaowei, FontWeight.Bold)
)

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val FlipYearTypography = Typography(
    // Pure sculptural display numerals (Monograph Year 2026, Spread Month 01..12)
    displayLarge = TextStyle(
        fontFamily = EditorialSerifFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily = EditorialSerifFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    // Unified modern geometric headings (harmonious with CJK titles)
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    // Unified crisp UI button, chip, and badge labels (no monospace clash with CJK)
    labelLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.15.sp
    )
)

