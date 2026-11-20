package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CobaltSoft
import com.example.ui.theme.DarkFlipYearPalette
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.LightFlipYearPalette
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.SurfacePure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeAppearanceSheet(
    currentMode: AppThemeMode,
    onSelectMode: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioCanvas,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(InkTertiary.copy(alpha = 0.42f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp)
                .testTag("theme_appearance_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.theme_appearance_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Text(
                    text = stringResource(R.string.theme_appearance_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemePreviewCard(
                    mode = AppThemeMode.SYSTEM,
                    icon = Icons.Filled.BrightnessAuto,
                    selected = currentMode == AppThemeMode.SYSTEM,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMode(AppThemeMode.SYSTEM)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("theme_option_system")
                )
                ThemePreviewCard(
                    mode = AppThemeMode.LIGHT,
                    icon = Icons.Filled.LightMode,
                    selected = currentMode == AppThemeMode.LIGHT,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMode(AppThemeMode.LIGHT)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("theme_option_light")
                )
                ThemePreviewCard(
                    mode = AppThemeMode.DARK,
                    icon = Icons.Filled.DarkMode,
                    selected = currentMode == AppThemeMode.DARK,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMode(AppThemeMode.DARK)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("theme_option_dark")
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CobaltSoft,
                border = BorderStroke(0.9.dp, CobaltPrimary.copy(alpha = 0.24f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (currentMode) {
                            AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                            AppThemeMode.LIGHT -> Icons.Filled.LightMode
                            AppThemeMode.DARK -> Icons.Filled.DarkMode
                        },
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${currentMode.label} · ${currentMode.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = InkPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    mode: AppThemeMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfacePure)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) CobaltPrimary else HairlineBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Miniature architectural preview artwork of each gallery atmosphere
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .border(0.8.dp, HairlineBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when (mode) {
                    AppThemeMode.LIGHT -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(LightFlipYearPalette.studioCanvasTop, LightFlipYearPalette.studioCanvasBottom)
                            )
                        )
                        // Mini gallery header card
                        drawRoundRect(
                            color = LightFlipYearPalette.surfacePure,
                            topLeft = Offset(w * 0.12f, h * 0.14f),
                            size = Size(w * 0.76f, h * 0.22f),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        // Mini monograph book card
                        drawRoundRect(
                            color = Color(0xFF1B2E5C),
                            topLeft = Offset(w * 0.24f, h * 0.44f),
                            size = Size(w * 0.52f, h * 0.36f),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        // Mini dock
                        drawRoundRect(
                            color = Color(0xEBFFFFFF),
                            topLeft = Offset(w * 0.14f, h * 0.85f),
                            size = Size(w * 0.72f, h * 0.09f),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }

                    AppThemeMode.DARK -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(DarkFlipYearPalette.studioCanvasTop, DarkFlipYearPalette.studioCanvasBottom)
                            )
                        )
                        // Subtle golden darkroom skylight
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(DarkFlipYearPalette.skylightChampagneGlow.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(w * 0.75f, h * 0.22f),
                                radius = w * 0.65f
                            ),
                            radius = w * 0.65f,
                            center = Offset(w * 0.75f, h * 0.22f)
                        )
                        // Mini smoked glass header
                        drawRoundRect(
                            color = DarkFlipYearPalette.surfaceMuted,
                            topLeft = Offset(w * 0.12f, h * 0.14f),
                            size = Size(w * 0.76f, h * 0.22f),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        // Mini monograph book card with golden accent line
                        drawRoundRect(
                            color = Color(0xFF283A6B),
                            topLeft = Offset(w * 0.24f, h * 0.44f),
                            size = Size(w * 0.52f, h * 0.36f),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        drawRoundRect(
                            color = DarkFlipYearPalette.solarAmber,
                            topLeft = Offset(w * 0.32f, h * 0.70f),
                            size = Size(w * 0.24f, h * 0.04f),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                        // Mini dark dock
                        drawRoundRect(
                            color = Color(0xEB1E2536),
                            topLeft = Offset(w * 0.14f, h * 0.85f),
                            size = Size(w * 0.72f, h * 0.09f),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }

                    AppThemeMode.SYSTEM -> {
                        // Top-left Alabaster Daylight half
                        drawRect(color = LightFlipYearPalette.studioCanvas)
                        // Bottom-right Nocturnal Darkroom diagonal half
                        val darkHalf = Path().apply {
                            moveTo(w, 0f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(path = darkHalf, color = DarkFlipYearPalette.studioCanvas)

                        // Centered mini book straddling daylight & darkroom
                        drawRoundRect(
                            color = Color(0xFF202B47),
                            topLeft = Offset(w * 0.24f, h * 0.30f),
                            size = Size(w * 0.52f, h * 0.44f),
                            cornerRadius = CornerRadius(7.dp.toPx())
                        )
                        drawRoundRect(
                            color = DarkFlipYearPalette.solarAmber,
                            topLeft = Offset(w * 0.34f, h * 0.60f),
                            size = Size(w * 0.32f, h * 0.05f),
                            cornerRadius = CornerRadius(3.dp.toPx())
                        )
                    }
                }
            }

            // Selection checkmark orb
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(CobaltPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CobaltPrimary else InkSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) CobaltPrimary else InkPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
