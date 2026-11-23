package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollageBackdropStyle
import com.example.data.CollageSlotTransform
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoVerm
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoInkOnGround
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.DecoGround

private val COLLAGE_SPACING_PRESETS = listOf(
    0f to "无缝",
    3f to "细缝",
    8f to "画框",
    14f to "宽白"
)

/**
 * Interactive irregular collage workbench embedded inside YearbookCoverStudioSheet:
 * - Portrait 0.70 monograph stage with exact polygon tap-swap & pinch/pan framing
 * - Quick studio tools (shuffle layout, reset transforms, 1-tap HD export + year watermark toggle)
 * - 3-style irregular template picker (Editorial Diagonal / Polaroid Stack / Gallery Inset)
 * - Gutter spacing & backdrop finish controls
 */
@Composable
internal fun YearbookCollageEditorPanel(
    year: Int,
    editionColor: Color,
    photos: List<PhotoItemEntity>,
    template: CollageTemplate,
    selectedCategory: CollageStyleCategory?,
    spacingDp: Float,
    backdrop: CollageBackdropStyle,
    slotTransforms: List<CollageSlotTransform>,
    activeSlotIndex: Int?,
    includeYearWatermark: Boolean,
    isExporting: Boolean,
    onSelectCategory: (CollageStyleCategory?) -> Unit,
    onSelectTemplate: (CollageTemplate) -> Unit,
    onSelectSpacing: (Float) -> Unit,
    onSelectBackdrop: (CollageBackdropStyle) -> Unit,
    onSlotTap: (Int) -> Unit,
    onClearActiveSlot: () -> Unit,
    onTransformSlot: (slotIndex: Int, updated: CollageSlotTransform) -> Unit,
    onShuffleLayout: () -> Unit,
    onResetTransforms: () -> Unit,
    onToggleYearWatermark: () -> Unit,
    onExportHdCollage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val bookShape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    val bookShadowColor = if (isDark) Color.Black.copy(alpha = 0.56f) else DecoInkOnGround.copy(alpha = 0.28f)
    val hasCustomTransforms = slotTransforms.any { !it.isIdentity }
    val filteredTemplates = if (selectedCategory == null || selectedCategory == CollageStyleCategory.ALL) {
        YearbookCollageTemplates.all
    } else {
        YearbookCollageTemplates.all.filter { it.category == selectedCategory }
    }
    val isPolaroidStyle = template.category == CollageStyleCategory.POLAROID_STACK

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1) Split Hero Stage: Interactive Monograph Collage Canvas (Left) + Studio Tool Column (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Portrait 0.70 Interactive Monograph Collage Canvas
            Box(
                modifier = Modifier
                    .width(164.dp)
                    .aspectRatio(0.70f)
                    .shadow(14.dp, bookShape, spotColor = bookShadowColor)
                    .clip(bookShape)
                    .background(editionColor)
                    .border(0.9.dp, Color.White.copy(alpha = 0.26f), bookShape)
                    .testTag("collage_studio_canvas")
            ) {
                YearbookCollageCanvas(
                    photos = photos,
                    template = template,
                    spacingDp = spacingDp,
                    backdrop = backdrop,
                    editionColor = editionColor,
                    slotTransforms = slotTransforms,
                    interactive = true,
                    activeSlotIndex = activeSlotIndex,
                    downsampleTier = ImageDownsampleTier.HERO_CARD,
                    targetSizePx = 560,
                    onSlotTap = onSlotTap,
                    onTransformSlot = onTransformSlot,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom vignette + year folio watermark preview (non-blocking for gestures)
                if (includeYearWatermark) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, editionColor.copy(alpha = 0.78f))
                                )
                            )
                    )
                    Text(
                        text = year.toString(),
                        fontFamily = DecoDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        letterSpacing = (-0.8).sp,
                        color = DecoInkOnGround,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 15.dp, bottom = 8.dp)
                    )
                }

                // Subtle left hardcover spine groove
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.44f),
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Right: Compact Tactile Studio Actions
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = DecoMat,
                    border = BorderStroke(0.8.dp, DecoGold.copy(alpha = 0.24f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DecoGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${template.category.label} · ${template.slotCount} 格槽位",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = DecoInkOnGround.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                CollageStudioToolButton(
                    label = "随机排版",
                    iconKind = DecoIconKind.RESTART,
                    accent = DecoInkOnGround,
                    onClick = onShuffleLayout,
                    modifier = Modifier.testTag("collage_shuffle_button")
                )

                CollageStudioToolButton(
                    label = if (hasCustomTransforms) "重置缩放" else "构图已居中",
                    iconKind = DecoIconKind.CENTER_FOCUS,
                    accent = if (hasCustomTransforms) DecoGold else DecoInkOnGround.copy(alpha = 0.55f),
                    enabled = hasCustomTransforms || activeSlotIndex != null,
                    onClick = onResetTransforms,
                    modifier = Modifier.testTag("collage_reset_transform_button")
                )

                // 1-Tap HD Export CTA
                Surface(
                    onClick = { if (!isExporting) onExportHdCollage() },
                    shape = RoundedCornerShape(3.dp),
                    color = DecoVerm,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("collage_export_hd_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color = DecoPaperCream,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(15.dp)
                            )
                        } else {
                            DecoIcon(
                                kind = DecoIconKind.ARROW_DOWN,
                                color = DecoPaperCream,
                                iconSize = 15.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isExporting) "导出中…" else "导出高清图",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DecoPaperCream,
                            maxLines = 1
                        )
                    }
                }

                // Watermark toggle pill for HD export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .clickable(onClick = onToggleYearWatermark)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("collage_watermark_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (includeYearWatermark) DecoGold else Color.Transparent)
                            .border(
                                width = 1.2.dp,
                                color = if (includeYearWatermark) DecoGold else DecoInkOnGround.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    ) {
                        if (includeYearWatermark) {
                            DecoIcon(
                                kind = DecoIconKind.CHECK,
                                color = DecoInkOnGround,
                                iconSize = 11.dp
                            )
                        }
                    }
                    Text(
                        text = "导出附带 $year 落款",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = DecoInkOnGround.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2) Live Gesture Guidance Pill
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = if (activeSlotIndex != null) DecoMat else DecoMat.copy(alpha = 0.85f),
            border = BorderStroke(
                0.8.dp,
                if (activeSlotIndex != null) DecoGold.copy(alpha = 0.35f) else DecoGold.copy(alpha = 0.45f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                DecoIcon(
                    kind = DecoIconKind.TOUCH,
                    color = if (activeSlotIndex != null) DecoGold else DecoInkOnGround.copy(alpha = 0.55f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (activeSlotIndex != null) {
                        "已选中第 ${activeSlotIndex + 1} 格 · 双指缩放拖动构图，点另一格互换或点下方照片替换"
                    } else {
                        "点击画布任意槽位选中微调缩放，依次点两格可互换照片位置"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (activeSlotIndex != null) DecoGold else DecoInkOnGround.copy(alpha = 0.72f),
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (activeSlotIndex != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(DecoGold.copy(alpha = 0.14f))
                            .clickable(onClick = onClearActiveSlot)
                            .testTag("collage_clear_active_slot")
                    ) {
                        DecoIcon(
                            kind = DecoIconKind.CLOSE,
                            contentDescription = "取消选中槽位",
                            color = DecoGold,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // 3) Multi-Style Template Library Selector (Category Tabs + Vector Wireframe Strip)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TemplateCategoryPill(
                    label = "全部",
                    selected = selectedCategory == null || selectedCategory == CollageStyleCategory.ALL,
                    onClick = { onSelectCategory(null) },
                    modifier = Modifier.testTag("collage_cat_all")
                )
                CollageStyleCategory.entries
                    .filter { it != CollageStyleCategory.ALL }
                    .forEach { cat ->
                        TemplateCategoryPill(
                            label = cat.label,
                            selected = selectedCategory == cat,
                            onClick = { onSelectCategory(cat) },
                            modifier = Modifier.testTag("collage_cat_${cat.name.lowercase()}")
                        )
                    }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("collage_template_strip")
            ) {
                items(
                    items = filteredTemplates,
                    key = { it.id }
                ) { item ->
                    val isSelected = item.id == template.id
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) DecoGold else DecoGold.copy(alpha = 0.45f),
                        animationSpec = tween(160),
                        label = "template_card_border"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .width(68.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSelected) DecoMat else DecoGround)
                            .border(
                                width = if (isSelected) 1.6.dp else 0.85.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(3.dp)
                            )
                            .clickable { onSelectTemplate(item) }
                            .padding(horizontal = 6.dp, vertical = 7.dp)
                            .testTag("collage_template_${item.id}")
                    ) {
                        CollageTemplateWireframe(
                            template = item,
                            selected = isSelected,
                            accentColor = DecoGold,
                            strokeColor = DecoInkOnGround.copy(alpha = 0.65f),
                            fillColor = if (isDark) DecoMat else DecoMat,
                            modifier = Modifier
                                .width(34.dp)
                                .aspectRatio(0.70f)
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DecoGold else DecoInkOnGround,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 4) Gutter Spacing & Canvas Backdrop Tuning Bar
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = DecoGround,
            border = BorderStroke(0.8.dp, DecoGold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                AnimatedVisibility(visible = !isPolaroidStyle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "留白",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DecoInkOnGround.copy(alpha = 0.72f),
                            modifier = Modifier.width(34.dp)
                        )
                        COLLAGE_SPACING_PRESETS.forEach { (dpVal, label) ->
                            val selected = kotlin.math.abs(spacingDp - dpVal) < 0.6f
                            Surface(
                                onClick = { onSelectSpacing(dpVal) },
                                shape = RoundedCornerShape(3.dp),
                                color = if (selected) DecoGold else DecoMat,
                                border = BorderStroke(
                                    0.75.dp,
                                    if (selected) DecoGold else DecoGold.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 34.dp)
                                    .testTag("collage_spacing_${dpVal.toInt()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) DecoInkOnGround else DecoInkOnGround
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "底纸",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DecoInkOnGround.copy(alpha = 0.72f),
                        modifier = Modifier.width(34.dp)
                    )
                    CollageBackdropStyle.entries.forEach { style ->
                        val selected = backdrop == style
                        val swatchColor = resolveBackdropColor(style, editionColor)
                        Surface(
                            onClick = { onSelectBackdrop(style) },
                            shape = RoundedCornerShape(3.dp),
                            color = if (selected) DecoMat else DecoMat,
                            border = BorderStroke(
                                width = if (selected) 1.3.dp else 0.75.dp,
                                color = if (selected) DecoGold else DecoGold.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 36.dp)
                                .testTag("collage_backdrop_${style.storageKey}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(0.9.dp, DecoInkOnGround.copy(alpha = 0.45f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) DecoGold else DecoInkOnGround,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageStudioToolButton(
    label: String,
    iconKind: DecoIconKind,
    accent: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(3.dp),
        color = DecoGround,
        border = BorderStroke(0.85.dp, DecoGold.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            DecoIcon(
                kind = iconKind,
                color = if (enabled) accent else DecoInkOnGround.copy(alpha = 0.6f),
                iconSize = 15.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) accent else DecoInkOnGround.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TemplateCategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = if (selected) DecoGold else DecoMat,
        border = BorderStroke(0.75.dp, if (selected) DecoGold else DecoGold.copy(alpha = 0.45f)),
        modifier = modifier.heightIn(min = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) DecoInkOnGround else DecoInkOnGround.copy(alpha = 0.72f),
                maxLines = 1
            )
        }
    }
}
