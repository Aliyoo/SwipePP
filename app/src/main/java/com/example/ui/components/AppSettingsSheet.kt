package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MediaAccessLevel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.DefaultStartTab
import com.example.ui.theme.FlipbookDefaultView
import com.example.ui.theme.GalleryGridDensity
import com.example.ui.theme.GlassSpecularRimStart
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.StudioCanvas
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfacePure
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.UserPreferences

/**
 * Unified editorial preferences drawer:
 * 1. Global chronological sort order, empty-month visibility, default views, and gallery density
 * 2. Appearance theme switcher (shared preview artwork)
 * 3. Library maintenance (rescan, permissions, demo archive removal)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsSheet(
    preferences: UserPreferences,
    mediaAccessLevel: MediaAccessLevel,
    totalImageCount: Int,
    totalVideoCount: Int,
    isScanningDevice: Boolean,
    hasSamplePhotos: Boolean,
    onSelectTimelineSortOrder: (TimelineSortOrder) -> Unit,
    onToggleShowEmptyMonths: (Boolean) -> Unit,
    onSelectFlipbookDefaultView: (FlipbookDefaultView) -> Unit,
    onSelectDefaultStartTab: (DefaultStartTab) -> Unit,
    onSelectGalleryGridDensity: (GalleryGridDensity) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onRescanDeviceMedia: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onRemoveDemoArchive: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("app_settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Text(
                    text = stringResource(R.string.settings_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            // Section 1: Chronology & Default Views
            SettingsSectionGroup(title = "浏览与时间轴") {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "全局时间顺序",
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        Text(
                            text = "年度 · 月历 · 画廊 · 整理",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = InkTertiary
                        )
                    }

                    SettingsSegmentedCapsule(
                        items = listOf(
                            SegmentedOption(
                                value = TimelineSortOrder.NEWEST_FIRST,
                                label = "最新优先（倒序）",
                                icon = Icons.Filled.ArrowDownward,
                                testTag = "settings_sort_newest"
                            ),
                            SegmentedOption(
                                value = TimelineSortOrder.OLDEST_FIRST,
                                label = "最早优先（正序）",
                                icon = Icons.Filled.ArrowUpward,
                                testTag = "settings_sort_oldest"
                            )
                        ),
                        selectedValue = preferences.timelineSortOrder,
                        onSelect = { order ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTimelineSortOrder(order)
                        },
                        modifier = Modifier.testTag("settings_sort_order_toggle")
                    )

                    // Live directional preview pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceSubtle.copy(alpha = 0.72f),
                        border = BorderStroke(0.75.dp, HairlineBorder.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preferences.timelineSortOrder.previewSummary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = HairlineBorder.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                // Show Empty Months toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleShowEmptyMonths(!preferences.showEmptyMonths)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("settings_show_empty_months_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "月历显示空白月份",
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        Text(
                            text = "关闭时仅展示含照片或视频的月份",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            color = InkSecondary
                        )
                    }
                    Switch(
                        checked = preferences.showEmptyMonths,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleShowEmptyMonths(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CobaltPrimary,
                            uncheckedThumbColor = InkSecondary,
                            uncheckedTrackColor = SurfaceSubtle,
                            uncheckedBorderColor = HairlineBorder
                        ),
                        modifier = Modifier.testTag("settings_show_empty_months_switch")
                    )
                }

                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = HairlineBorder.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                // Flipbook default view
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "月历默认视图",
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    SettingsSegmentedCapsule(
                        items = listOf(
                            SegmentedOption(
                                value = FlipbookDefaultView.CALENDAR,
                                label = FlipbookDefaultView.CALENDAR.label,
                                testTag = "settings_flipbook_calendar"
                            ),
                            SegmentedOption(
                                value = FlipbookDefaultView.GALLERY,
                                label = FlipbookDefaultView.GALLERY.label,
                                testTag = "settings_flipbook_gallery"
                            )
                        ),
                        selectedValue = preferences.flipbookDefaultView,
                        onSelect = { view ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectFlipbookDefaultView(view)
                        },
                        modifier = Modifier.testTag("settings_flipbook_default_view")
                    )
                }

                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = HairlineBorder.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                // Default launch tab
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App 默认启动页",
                            fontFamily = OutfitFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        Text(
                            text = "下次启动生效",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = InkTertiary
                        )
                    }
                    SettingsSegmentedCapsule(
                        items = listOf(
                            SegmentedOption(
                                value = DefaultStartTab.BOOKSHELF,
                                label = DefaultStartTab.BOOKSHELF.label,
                                testTag = "settings_start_tab_bookshelf"
                            ),
                            SegmentedOption(
                                value = DefaultStartTab.GALLERY,
                                label = DefaultStartTab.GALLERY.label,
                                testTag = "settings_start_tab_gallery"
                            ),
                            SegmentedOption(
                                value = DefaultStartTab.MAP,
                                label = DefaultStartTab.MAP.label,
                                testTag = "settings_start_tab_map"
                            )
                        ),
                        selectedValue = preferences.defaultStartTab,
                        onSelect = { tab ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectDefaultStartTab(tab)
                        },
                        modifier = Modifier.testTag("settings_default_start_tab")
                    )
                }

                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = HairlineBorder.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                // Gallery grid density
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "画廊网格密度",
                        fontFamily = OutfitFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    SettingsSegmentedCapsule(
                        items = listOf(
                            SegmentedOption(
                                value = GalleryGridDensity.STANDARD,
                                label = GalleryGridDensity.STANDARD.label,
                                testTag = "settings_density_standard"
                            ),
                            SegmentedOption(
                                value = GalleryGridDensity.COMPACT,
                                label = GalleryGridDensity.COMPACT.label,
                                testTag = "settings_density_compact"
                            )
                        ),
                        selectedValue = preferences.galleryGridDensity,
                        onSelect = { density ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectGalleryGridDensity(density)
                        },
                        modifier = Modifier.testTag("settings_gallery_density")
                    )
                }
            }

            // Section 2: Appearance Theme (shares preview artwork with standalone ThemeAppearanceSheet)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "外观与主题",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                ThemePreviewCardsRow(
                    currentMode = preferences.themeMode,
                    onSelectMode = { mode ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectThemeMode(mode)
                    }
                )
            }

            // Section 3: Library & Maintenance
            SettingsSectionGroup(title = "相册与维护") {
                val scanSubtitle = when {
                    isScanningDevice -> "正在增量扫描本地媒体…"
                    totalImageCount + totalVideoCount > 0 ->
                        "已索引 $totalImageCount 张照片 · $totalVideoCount 个视频"
                    else -> "扫描设备相册以构建年度影集"
                }
                SettingsActionRow(
                    icon = Icons.Filled.Refresh,
                    iconTint = CobaltPrimary,
                    title = stringResource(R.string.action_scan_media),
                    subtitle = scanSubtitle,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                        if (mediaAccessLevel == MediaAccessLevel.NONE) {
                            onRequestMediaPermission()
                        } else {
                            onRescanDeviceMedia()
                        }
                    },
                    modifier = Modifier.testTag("scan_media_button")
                )

                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = HairlineBorder.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                val permissionStatus = when (mediaAccessLevel) {
                    MediaAccessLevel.FULL -> "已授权访问全部照片与视频"
                    MediaAccessLevel.PARTIAL -> "当前仅授权部分媒体 · 点此扩展范围"
                    MediaAccessLevel.NONE -> "尚未授予相册访问权限"
                }
                SettingsActionRow(
                    icon = Icons.Filled.FolderOpen,
                    iconTint = CobaltPrimary,
                    title = "管理相册访问权限",
                    subtitle = permissionStatus,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                        onRequestMediaPermission()
                    },
                    modifier = Modifier.testTag("settings_manage_permissions")
                )

                if (hasSamplePhotos) {
                    HorizontalDivider(
                        thickness = 0.75.dp,
                        color = HairlineBorder.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    SettingsActionRow(
                        icon = Icons.Outlined.RestartAlt,
                        iconTint = CoralDanger,
                        titleColor = CoralDanger,
                        title = "移除示例影集",
                        subtitle = "清除内置体验照片，仅保留设备真实相册",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                            onRemoveDemoArchive()
                        },
                        modifier = Modifier.testTag("settings_remove_demo")
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontFamily = OutfitFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfacePure,
            border = BorderStroke(0.85.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

private data class SegmentedOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val testTag: String
)

/**
 * Tactile spring-sliding satin segmented pill matching FlipbookViewModeSwitch aesthetics.
 */
@Composable
private fun <T> SettingsSegmentedCapsule(
    items: List<SegmentedOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val selectedIndex = items.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
    val trackBg = if (isDark) SurfaceMuted.copy(alpha = 0.90f) else SurfaceSubtle.copy(alpha = 0.88f)
    val trackBorder = if (isDark) HairlineBorder else GlassSpecularRimStart

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackBg)
            .border(0.85.dp, trackBorder, RoundedCornerShape(14.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / items.size.coerceAtLeast(1)
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "settings_segmented_thumb"
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(11.dp))
                .background(activeChromeSatinBrush())
                .border(
                    width = 0.75.dp,
                    color = Color.White.copy(alpha = if (isDark) 0.16f else 0.24f),
                    shape = RoundedCornerShape(11.dp)
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { option ->
                val isSelected = option.value == selectedValue
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else InkSecondary,
                    animationSpec = tween(160),
                    label = "seg_text_${option.testTag}"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) SolarAmber else InkTertiary,
                    animationSpec = tween(160),
                    label = "seg_icon_${option.testTag}"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        }
                        .clickable { onSelect(option.value) }
                        .testTag(option.testTag)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        if (option.icon != null) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = option.label,
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = InkPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.5.sp,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = InkTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
