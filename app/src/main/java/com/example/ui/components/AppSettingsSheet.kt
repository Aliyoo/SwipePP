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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
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
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoVermInk
import com.example.ui.theme.DefaultStartTab
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.FlipbookDefaultView
import com.example.ui.theme.GalleryGridDensity
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.InkTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFlipYearPalette
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TimelineSortOrder
import com.example.ui.theme.UserPreferences

/**
 * 摩登画报 editorial preferences drawer（deco-app.html 造型语法）：
 * 1. Global chronological sort order, empty-month visibility, default views, and gallery density
 * 2. Appearance theme switcher (shared preview artwork)
 * 3. Library maintenance (rescan, permissions, demo archive removal)
 *
 * 造型语法：纸地面（decoGround）+ 纸面分区卡（DecoMat + 1dp 金细线，3dp 小圆角）；
 * 分区标题 = 衬线小标 + 金发丝分隔线；分段控件 = 金描边跳签（选中金底）；
 * 开关 = 金底滑块；图标圆钮 = 金描边小方章。
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
        containerColor = LocalFlipYearPalette.current.decoGround,
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(DecoInkOnGround.copy(alpha = 0.40f))
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
                    fontFamily = DecoDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 1.2.sp,
                    color = InkPrimary
                )
                Text(
                    text = stringResource(R.string.settings_sheet_subtitle),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
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
                            letterSpacing = 0.8.sp,
                            color = InkPrimary
                        )
                        Text(
                            text = "年度 · 月历 · 画廊 · 整理",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = InkTertiary
                        )
                    }

                    SettingsSegmentedCapsule(
                        items = listOf(
                            SegmentedOption(
                                value = TimelineSortOrder.NEWEST_FIRST,
                                label = "最新优先（倒序）",
                                icon = Icons.Outlined.ArrowDownward,
                                testTag = "settings_sort_newest"
                            ),
                            SegmentedOption(
                                value = TimelineSortOrder.OLDEST_FIRST,
                                label = "最早优先（正序）",
                                icon = Icons.Outlined.ArrowUpward,
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

                    // Live directional preview slip（金染纸面小签）
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = DecoGold.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preferences.timelineSortOrder.previewSummary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = DecoGold.copy(alpha = 0.30f),
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
                            letterSpacing = 0.8.sp,
                            color = InkPrimary
                        )
                        Text(
                            text = "关闭时仅展示含照片或视频的月份",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
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
                            checkedThumbColor = LocalFlipYearPalette.current.decoMat,
                            checkedTrackColor = DecoGold,
                            uncheckedThumbColor = DecoGold,
                            uncheckedTrackColor = InkPrimary.copy(alpha = 0.06f),
                            uncheckedBorderColor = DecoGold.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.testTag("settings_show_empty_months_switch")
                    )
                }

                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = DecoGold.copy(alpha = 0.30f),
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
                        letterSpacing = 0.8.sp,
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
                    thickness = 0.8.dp,
                    color = DecoGold.copy(alpha = 0.30f),
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
                            letterSpacing = 0.8.sp,
                            color = InkPrimary
                        )
                        Text(
                            text = "下次启动生效",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
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
                    thickness = 0.8.dp,
                    color = DecoGold.copy(alpha = 0.30f),
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
                        letterSpacing = 0.8.sp,
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
                DecoSectionHeader(title = "外观与主题")
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
                    icon = Icons.Outlined.Refresh,
                    iconTint = DecoGold,
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
                    thickness = 0.8.dp,
                    color = DecoGold.copy(alpha = 0.30f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                val permissionStatus = when (mediaAccessLevel) {
                    MediaAccessLevel.FULL -> "已授权访问全部照片与视频"
                    MediaAccessLevel.PARTIAL -> "当前仅授权部分媒体 · 点此扩展范围"
                    MediaAccessLevel.NONE -> "尚未授予相册访问权限"
                }
                SettingsActionRow(
                    icon = Icons.Outlined.FolderOpen,
                    iconTint = DecoGold,
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
                        thickness = 0.8.dp,
                        color = DecoGold.copy(alpha = 0.30f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    SettingsActionRow(
                        icon = Icons.Outlined.RestartAlt,
                        iconTint = DecoVermInk,
                        sealTint = DecoVerm,
                        titleColor = DecoVermInk,
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

/**
 * 摩登画报分区标题：衬线小标（金、加字距）+ 右侧金发丝分隔线（0.8dp gold 0.35）。
 */
@Composable
private fun DecoSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontFamily = DecoDisplayFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = DecoGold
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(0.8.dp)
                .background(DecoGold.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun SettingsSectionGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DecoSectionHeader(title = title)
        // 纸面分区卡：DecoMat 内衬 + 1dp 金细线 + 3dp 小圆角
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = DecoMat,
            border = BorderStroke(1.dp, DecoGold.copy(alpha = 0.40f)),
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
 * 摩登画报跳签分段控件：金描边纸面轨 + 弹性滑动的金底跳签（选中金底、纸面墨字）。
 */
@Composable
private fun <T> SettingsSegmentedCapsule(
    items: List<SegmentedOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(InkPrimary.copy(alpha = 0.05f))
            .border(1.dp, DecoGold.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
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
                .clip(RoundedCornerShape(2.dp))
                .background(DecoGold)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { option ->
                val isSelected = option.value == selectedValue
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) LocalFlipYearPalette.current.decoMat else InkSecondary,
                    animationSpec = tween(160),
                    label = "seg_text_${option.testTag}"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) LocalFlipYearPalette.current.decoMat else DecoGold.copy(alpha = 0.72f),
                    animationSpec = tween(160),
                    label = "seg_icon_${option.testTag}"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
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
                            fontFamily = if (isSelected) DecoDisplayFontFamily else OutfitFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.8.sp,
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

/**
 * 维护动作行：图标圆钮方章化（金描边小方章 3dp；警示行朱红方章）。
 */
@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    sealTint: Color = DecoGold,
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
        // 金描边小方章（3dp 圆角，1dp 描边）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(sealTint.copy(alpha = 0.10f))
                .border(1.dp, sealTint.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
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
                letterSpacing = 0.8.sp,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = InkTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
