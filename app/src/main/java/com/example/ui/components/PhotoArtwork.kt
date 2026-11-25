package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FlipYearImageLoaders
import com.example.data.ImageDownsampleTier
import com.example.data.PhotoItemEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SurfaceMuted

// 画片语法：方形/微圆角印章式徽标（不再沿用旧玻璃拟态胶囊），深色 scrim 墨底
private val VideoBadgeShape = RoundedCornerShape(2.dp)
private val VideoBadgeColor = Color(0xCC10131A)

private val VariantSlightTilt = VariantStyle(
    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.92f) }),
    scale = 1.12f,
    rotation = -3.8f,
    shiftFractionX = 0.04f
)
private val VariantDarkExposure = VariantStyle(
    colorFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.68f, 0f, 0f, 0f, -14f,
                0f, 0.68f, 0f, 0f, -14f,
                0f, 0f, 0.70f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    ),
    scale = 1.05f,
    rotation = 1.2f,
    shiftFractionX = -0.02f
)
private val VariantOverexposed = VariantStyle(
    colorFilter = ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                1.22f, 0f, 0f, 0f, 26f,
                0f, 1.20f, 0f, 0f, 24f,
                0f, 0f, 1.16f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    ),
    scale = 1.06f,
    rotation = 1.8f,
    shiftFractionX = 0.03f
)
private val VariantSoftCrop = VariantStyle(
    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.84f) }),
    scale = 1.26f,
    rotation = 2.4f,
    shiftFractionX = -0.06f
)

@DrawableRes
fun resolveSampleDrawable(name: String?): Int {
    return when (name) {
        "img_photo_kyoto" -> R.drawable.img_photo_kyoto
        "img_photo_alps" -> R.drawable.img_photo_alps
        "img_photo_amalfi" -> R.drawable.img_photo_amalfi
        "img_photo_cafe" -> R.drawable.img_photo_cafe
        else -> R.drawable.img_photo_kyoto
    }
}

@Composable
fun PhotoArtwork(
    photo: PhotoItemEntity,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showVideoBadge: Boolean = true,
    downsampleTier: ImageDownsampleTier = ImageDownsampleTier.PREVIEW_CARD,
    targetSizePx: Int? = null,
    enableCrossfade: Boolean = false
) {
    val variant: VariantStyle? = when (photo.visualVariant) {
        "CRISP" -> null
        "SLIGHT_TILT" -> VariantSlightTilt
        "DARK_EXPOSURE" -> VariantDarkExposure
        "OVEREXPOSED" -> VariantOverexposed
        "SOFT_CROP" -> VariantSoftCrop
        else -> null
    }

    val needsOverlayBox = (showVideoBadge && photo.isVideo) || variant != null
    val contentUri = photo.contentUri

    if (!needsOverlayBox) {
        // Fast zero-wrapper hot path for standard real device photos in gallery grids
        if (!contentUri.isNullOrBlank()) {
            val context = LocalContext.current
            val imageLoader = remember(context) { FlipYearImageLoaders.getMediaImageLoader(context) }
            val request = remember(contentUri, photo.isVideo, downsampleTier, targetSizePx, enableCrossfade) {
                FlipYearImageLoaders.buildPhotoImageRequest(
                    context = context,
                    contentUri = contentUri,
                    isVideo = photo.isVideo,
                    tier = downsampleTier,
                    overrideSizePx = targetSizePx,
                    enableCrossfade = enableCrossfade
                )
            }
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier.background(SurfaceMuted)
            )
        } else {
            Image(
                painter = painterResource(id = resolveSampleDrawable(photo.drawableName)),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier.background(SurfaceMuted)
            )
        }
        return
    }

    Box(
        modifier = modifier
            .background(SurfaceMuted)
            .clipToBounds()
    ) {
        val imageModifier = if (variant == null) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = variant.scale
                    scaleY = variant.scale
                    rotationZ = variant.rotation
                    translationX = size.width * variant.shiftFractionX
                }
        }

        if (!contentUri.isNullOrBlank()) {
            val context = LocalContext.current
            val imageLoader = remember(context) { FlipYearImageLoaders.getMediaImageLoader(context) }
            val request = remember(contentUri, photo.isVideo, downsampleTier, targetSizePx, enableCrossfade) {
                FlipYearImageLoaders.buildPhotoImageRequest(
                    context = context,
                    contentUri = contentUri,
                    isVideo = photo.isVideo,
                    tier = downsampleTier,
                    overrideSizePx = targetSizePx,
                    enableCrossfade = enableCrossfade
                )
            }
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = variant?.colorFilter,
                modifier = imageModifier
            )
        } else {
            Image(
                painter = painterResource(id = resolveSampleDrawable(photo.drawableName)),
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = variant?.colorFilter,
                modifier = imageModifier
            )
        }

        if (showVideoBadge && photo.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(VideoBadgeShape)
                    .background(VideoBadgeColor)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DecoIcon(
                    kind = DecoIconKind.PLAY,
                    color = Color.White,
                    iconSize = 12.dp
                )
                Text(
                    text = formatVideoDuration(photo.durationMs),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

private data class VariantStyle(
    val colorFilter: ColorFilter?,
    val scale: Float,
    val rotation: Float,
    val shiftFractionX: Float
)
