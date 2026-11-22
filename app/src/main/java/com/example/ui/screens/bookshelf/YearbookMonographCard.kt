package com.example.ui.screens.bookshelf

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.YearbookSummary
import com.example.ui.components.PhotoArtwork
import com.example.ui.theme.DecoGold
import com.example.ui.theme.DecoGround
import com.example.ui.theme.DecoArtShadowVerm
import com.example.ui.theme.DecoDisplayFontFamily
import com.example.ui.theme.DecoMat
import com.example.ui.theme.DecoPaperCream
import com.example.ui.theme.DecoScrimInk
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily

/**
 * 摩登画报 · 干支纪年（2026 丙午 / 2025 乙巳 / 2024 甲辰 / 2023 癸卯 / 2022 壬寅，公式通推任意年份）。
 */
private val DecoGanChars = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val DecoZhiChars = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

// 压照片暗 scrim 与美术字错位影朱红层收敛到 theme/Color.kt 共享常量
// DecoScrimInk（原型 rgba(13,24,20,*)）与 DecoArtShadowVerm（原型 artShadow 第 2 层 rgba(158,42,29,0.6)）

internal fun decoGanzhiOfYear(year: Int): String {
    val gan = DecoGanChars[(year - 4).mod(DecoGanChars.size)]
    val zhi = DecoZhiChars[(year - 4).mod(DecoZhiChars.size)]
    return "$gan$zhi"
}

/**
 * 摩登画报年度影集画片：金双线画框（外 1.5dp 金 + 3dp 内衬 + 1dp 弱金）+ 全幅年度封面照
 * + 放射太阳纹 + 美术字大年份（错位阴影 2/4/6dp）与干支副标 + 方章换封面按钮。
 * 交互基线保持不变：滑动轮播、中央卡点击进 Flipbook、封面定制、环境光封面切换。
 */
@Composable
internal fun ModernYearbook3DCard(
    summary: YearbookSummary,
    isFocused: Boolean,
    isCarouselPaused: Boolean,
    onCustomizeCover: () -> Unit,
    onActiveSlideChanged: (PhotoItemEntity?) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = DecoGold
    val mat = DecoMat
    val ground = DecoGround

    val decoShape = RoundedCornerShape(4.dp)
    val photoShape = RoundedCornerShape(2.dp)

    val slides = remember(summary.coverSlides, summary.coverPhoto) {
        summary.coverSlides.ifEmpty { listOfNotNull(summary.coverPhoto) }
    }
    val slideKeys = remember(slides) { slides.map { it.id } }
    var currentSlideIndex by remember(summary.year, slideKeys) { mutableIntStateOf(0) }
    val safeSlideIndex = if (slides.isEmpty()) 0 else currentSlideIndex.coerceIn(0, slides.lastIndex)
    val displayedPhoto = slides.getOrNull(safeSlideIndex) ?: summary.coverPhoto
    val slideProgress = remember(summary.year, slideKeys) { Animatable(0f) }

    val isCarouselActive = isFocused && !isCarouselPaused && slides.size > 1

    LaunchedEffect(displayedPhoto, isFocused) {
        if (isFocused) {
            onActiveSlideChanged(displayedPhoto)
        }
    }

    // Smooth museum-grade slideshow clock + Ken Burns progress — strictly active ONLY on the focused center book
    LaunchedEffect(summary.year, slideKeys, summary.coverIntervalSeconds, isCarouselActive) {
        if (!isCarouselActive) {
            slideProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val durationMs = (summary.coverIntervalSeconds.coerceIn(3, 12) * 1000)
        while (true) {
            slideProgress.snapTo(0f)
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
            if (slides.size > 1) {
                currentSlideIndex = (currentSlideIndex + 1) % slides.size
            }
        }
    }

    // 摩登画报外框：主仓库按压缩放反馈（interaction + spring）× 18dp 投影 + 金双框衬纸（画报造型）
    val bookInteraction = remember { MutableInteractionSource() }
    val isBookPressed by bookInteraction.collectIsPressedAsState()
    val bookPressScaleState = animateFloatAsState(
        targetValue = if (isBookPressed) 0.982f else 1f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "book_press_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = bookPressScaleState.value
                scaleX = s
                scaleY = s
            }
            .shadow(elevation = 18.dp, shape = decoShape, spotColor = Color.Black.copy(alpha = 0.52f))
            .clip(decoShape)
            // 内衬底色：金双框之间的画框衬纸
            .background(mat)
            .border(width = 1.5.dp, color = gold, shape = decoShape)
            .clickable(
                interactionSource = bookInteraction,
                indication = androidx.compose.material3.ripple(color = gold),
                onClick = onClick
            )
    ) {
        // 画框内画片视窗：外 1.5dp 金 → 3dp 内衬 → 1dp 弱金 → 全幅封面照
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.5.dp)
                .clip(photoShape)
                .border(width = 1.dp, color = gold.copy(alpha = 0.40f), shape = photoShape)
        ) {
            if (displayedPhoto != null) {
                val panDirection = if (safeSlideIndex % 2 == 0) 1f else -1f

                Crossfade(
                    targetState = displayedPhoto,
                    animationSpec = tween(durationMillis = 820),
                    label = "yearbook_cover_crossfade",
                    modifier = Modifier.fillMaxSize()
                ) { slidePhoto ->
                    PhotoArtwork(
                        photo = slidePhoto,
                        contentDescription = stringResource(R.string.cd_yearbook_cover),
                        showVideoBadge = false,
                        downsampleTier = ImageDownsampleTier.HERO_CARD,
                        targetSizePx = 768,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (isCarouselActive) {
                                    val p = slideProgress.value
                                    val kenBurnsScale = lerp(1.00f, 1.07f, p)
                                    scaleX = kenBurnsScale
                                    scaleY = kenBurnsScale
                                    translationX = lerp(-6f, 6f, p) * panDirection
                                } else {
                                    scaleX = 1f
                                    scaleY = 1f
                                    translationX = 0f
                                }
                            }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(ground))
            }

            // 放射太阳纹：24 扇形（每 15° 一瓣、瓣宽 6.5°），金色 alpha 0.12~0.16，压在画片左下角
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sunburstSize = size.width * 0.92f
                val radius = sunburstSize / 2f
                val center = Offset(-sunburstSize * 0.10f, size.height + sunburstSize * 0.07f)
                val wedgeColor = gold.copy(alpha = 0.14f)
                val wedgeRect = Rect(
                    left = center.x - radius,
                    top = center.y - radius,
                    right = center.x + radius,
                    bottom = center.y + radius
                )
                var angleDeg = 0f
                repeat(24) {
                    val startRad = Math.toRadians(angleDeg.toDouble())
                    val wedgePath = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(
                            center.x + (radius * kotlin.math.cos(startRad)).toFloat(),
                            center.y + (radius * kotlin.math.sin(startRad)).toFloat()
                        )
                        arcTo(
                            rect = wedgeRect,
                            startAngleDegrees = angleDeg,
                            sweepAngleDegrees = 6.5f,
                            forceMoveTo = false
                        )
                        close()
                    }
                    drawPath(path = wedgePath, color = wedgeColor)
                    angleDeg += 15f
                }
            }

            // 金色柔光 wash（demo soft-light 径向，中心 50%/30%）+ 暗角（像素坐标，随画片尺寸缩放）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    gold.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.50f, size.height * 0.30f),
                                radius = size.width * 0.85f
                            )
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DecoScrimInk.copy(alpha = 0.50f)
                                ),
                                center = Offset(size.width * 0.50f, size.height * 0.42f),
                                radius = size.width * 0.95f
                            )
                        )
                    }
            )

            // 可读性红线：压照片文字必须暗 scrim（线性 0.62 → 透明 → 0.94）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to DecoScrimInk.copy(alpha = 0.62f),
                            0.42f to Color.Transparent,
                            1.0f to DecoScrimInk.copy(alpha = 0.94f)
                        )
                    )
            )

            // 画片内容层（theirs 的 drawBehind 零重组进度条与方章按压反馈已按画报语法接入）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部：轮播进度条 + 已整理方章 + 换封面方章
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (slides.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cover_carousel_bars_${summary.year}"),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            slides.indices.forEach { idx ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(DecoPaperCream.copy(alpha = 0.26f))
                                        .drawBehind {
                                            // theirs 零重组逻辑：segmentFill 只在 draw 相期读取 slideProgress
                                            val segmentFill = when {
                                                idx < safeSlideIndex -> 1f
                                                idx == safeSlideIndex -> if (isCarouselActive) slideProgress.value.coerceIn(0.08f, 1f) else 1f
                                                else -> 0f
                                            }
                                            if (segmentFill > 0f) {
                                                drawRect(
                                                    color = DecoPaperCream.copy(alpha = if (idx == safeSlideIndex) 0.95f else 0.62f),
                                                    size = Size(width = size.width * segmentFill, height = size.height)
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (summary.unreviewedCount == 0 && summary.totalPhotos > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ground.copy(alpha = 0.66f))
                                    .border(1.dp, gold.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "年度已整理",
                                    tint = gold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(1.dp))
                        }

                        // 方章换封面按钮：40dp 正方、1.5dp 描边、竖排两字（仅中央聚焦卡展示，基线门槛不变）
                        // theirs 按压反馈（cover_orb_scale spring）接入方章
                        if (isFocused) {
                            val coverOrbInteraction = remember { MutableInteractionSource() }
                            val coverOrbPressed by coverOrbInteraction.collectIsPressedAsState()
                            val coverOrbScaleState = animateFloatAsState(
                                targetValue = if (coverOrbPressed) 0.90f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "cover_orb_scale"
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        val s = coverOrbScaleState.value
                                        scaleX = s
                                        scaleY = s
                                    }
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ground.copy(alpha = 0.62f))
                                    .border(1.5.dp, gold, RoundedCornerShape(4.dp))
                                    .clickable(
                                        interactionSource = coverOrbInteraction,
                                        indication = androidx.compose.material3.ripple(color = gold),
                                        onClickLabel = stringResource(R.string.cd_customize_cover),
                                        onClick = onCustomizeCover
                                    )
                                    .testTag("customize_cover_button_${summary.year}")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "封",
                                        fontSize = 13.sp,
                                        lineHeight = 14.sp,
                                        fontFamily = DecoDisplayFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = gold
                                    )
                                    Text(
                                        text = "面",
                                        fontSize = 13.sp,
                                        lineHeight = 14.sp,
                                        fontFamily = DecoDisplayFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = gold
                                    )
                                }
                            }
                        }
                    }
                }

                // 底部：美术字大年份（EditorialSerif 900 + 错位阴影 2/4/6dp）+ 干支副标 + 归档行
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 美术字年份：黑色 2dp 错位 → 朱红 4dp 错位 → 纸黄主字（6dp 软影）
                        Box {
                            Text(
                                text = summary.year.toString(),
                                fontFamily = EditorialSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 58.sp,
                                lineHeight = 50.sp,
                                letterSpacing = (-0.5).sp,
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                            )
                            Text(
                                text = summary.year.toString(),
                                fontFamily = EditorialSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 58.sp,
                                lineHeight = 50.sp,
                                letterSpacing = (-0.5).sp,
                                color = DecoArtShadowVerm,
                                modifier = Modifier.offset(x = 4.dp, y = 4.dp)
                            )
                            Text(
                                text = summary.year.toString(),
                                fontFamily = EditorialSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 58.sp,
                                lineHeight = 50.sp,
                                letterSpacing = (-0.5).sp,
                                color = DecoPaperCream,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        offset = Offset(6f, 6f),
                                        blurRadius = 14f
                                    )
                                )
                            )
                        }

                        Text(
                            text = "${decoGanzhiOfYear(summary.year)}年",
                            fontFamily = DecoDisplayFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = gold,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    offset = Offset(2f, 2f)
                                )
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 归档行：总量与月度归档进度（压暗部，纸黄文字）+ 视频计数
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val archiveSummary = buildString {
                            append("${summary.totalPhotos} 项")
                            if (summary.totalMonthsCount > 0) {
                                append(" · 已归档 ${summary.cleanedMonthsCount}/${summary.totalMonthsCount} 月")
                            }
                        }
                        Text(
                            text = archiveSummary,
                            fontSize = 13.sp,
                            letterSpacing = 0.4.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = DecoPaperCream.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (summary.videoCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.34f))
                                    .border(0.75.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = gold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${summary.videoCount}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DecoPaperCream.copy(alpha = 0.92f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = DecoPaperCream.copy(alpha = 0.78f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 归档进度条：纸黄轨 + 金尖
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(DecoPaperCream.copy(alpha = 0.22f))
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = summary.progressFraction,
                            animationSpec = spring(),
                            label = "book_progress"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(DecoPaperCream.copy(alpha = 0.9f), gold)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
