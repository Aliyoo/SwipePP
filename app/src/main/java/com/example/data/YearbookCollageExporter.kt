package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.example.ui.components.CollageSlotFrameStyle
import com.example.ui.components.CollageTemplate
import com.example.ui.components.insetConvexPolygon
import com.example.ui.components.resolveBackdropColor
import com.example.ui.components.resolveSampleDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class CollageExportResult(
    val success: Boolean,
    val displayName: String,
    val message: String
)

/**
 * Offscreen HD (1400 x 2000 px, exact 0.70 monograph ratio) collage renderer and MediaStore exporter.
 * Shares the exact normalized polygon geometry & per-slot transforms from YearbookCollageEngine
 * so exported gallery images achieve 100% WYSIWYG parity with the live cover preview.
 */
object YearbookCollageExporter {

    private const val EXPORT_WIDTH = 1400
    private const val EXPORT_HEIGHT = 2000
    // Reference preview width (~210dp) to scale dp-based spacing & corner radii proportionally to 1400px
    private const val DP_TO_EXPORT_PX = EXPORT_WIDTH / 220f

    suspend fun exportCollageToGallery(
        context: Context,
        year: Int,
        photos: List<PhotoItemEntity>,
        template: CollageTemplate,
        spacingDp: Float,
        backdrop: CollageBackdropStyle,
        editionColorArgb: Int,
        slotTransforms: List<CollageSlotTransform>,
        includeYearWatermark: Boolean = true
    ): CollageExportResult = withContext(Dispatchers.Default) {
        val fileName = "FlipYear_${year}_Cover_${System.currentTimeMillis() / 1000}.jpg"
        val bitmap = Bitmap.createBitmap(EXPORT_WIDTH, EXPORT_HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val bgColor = resolveBackdropColor(backdrop, androidx.compose.ui.graphics.Color(editionColorArgb)).toArgb()
            canvas.drawColor(bgColor)

            val w = EXPORT_WIDTH.toFloat()
            val h = EXPORT_HEIGHT.toFloat()
            val outerMarginPx = spacingDp * 0.55f * DP_TO_EXPORT_PX
            val innerHalfGutterPx = spacingDp * 0.5f * DP_TO_EXPORT_PX

            val orderedIndices = template.slots.indices.sortedBy { template.slots[it].zIndex }
            val decodedCache = HashMap<Long, Bitmap?>()

            for (slotIdx in orderedIndices) {
                val slot = template.slots[slotIdx]
                val photo = photos.getOrNull(slotIdx) ?: photos.firstOrNull()
                val transform = slotTransforms.getOrNull(slotIdx) ?: CollageSlotTransform()

                val basePts = slot.polygon.map { p ->
                    val x = if (slot.frameStyle == CollageSlotFrameStyle.EDGE_TO_EDGE) {
                        outerMarginPx + p.x * (w - outerMarginPx * 2f).coerceAtLeast(1f)
                    } else {
                        p.x * w
                    }
                    val y = if (slot.frameStyle == CollageSlotFrameStyle.EDGE_TO_EDGE) {
                        outerMarginPx + p.y * (h - outerMarginPx * 2f).coerceAtLeast(1f)
                    } else {
                        p.y * h
                    }
                    Offset(x, y)
                }
                val polyPts = if (slot.frameStyle == CollageSlotFrameStyle.EDGE_TO_EDGE && innerHalfGutterPx > 0.5f) {
                    insetConvexPolygon(basePts, innerHalfGutterPx)
                } else {
                    basePts
                }

                val minX = polyPts.minOf { it.x }
                val minY = polyPts.minOf { it.y }
                val maxX = polyPts.maxOf { it.x }
                val maxY = polyPts.maxOf { it.y }
                val boxW = (maxX - minX).coerceAtLeast(1f)
                val boxH = (maxY - minY).coerceAtLeast(1f)
                val cx = (minX + maxX) / 2f
                val cy = (minY + maxY) / 2f
                val cornerPx = slot.cornerRadiusDp * DP_TO_EXPORT_PX

                val slotBitmap = photo?.let { item ->
                    decodedCache.getOrPut(item.id) {
                        decodePhotoBitmap(context, item, targetMaxPx = max(boxW, boxH).toInt().coerceIn(720, 1800))
                    }
                }

                canvas.save()
                if (slot.rotationDeg != 0f) {
                    canvas.rotate(slot.rotationDeg, cx, cy)
                }

                when (slot.frameStyle) {
                    CollageSlotFrameStyle.POLAROID -> {
                        val paperRect = RectF(minX, minY, maxX, maxY)
                        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#FAF8F3")
                            setShadowLayer(26f, 0f, 14f, Color.argb(110, 0, 0, 0))
                        }
                        canvas.drawRoundRect(paperRect, cornerPx, cornerPx, shadowPaint)

                        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 2f
                            color = Color.argb(24, 0, 0, 0)
                        }
                        canvas.drawRoundRect(paperRect, cornerPx, cornerPx, borderPaint)

                        val sidePad = (boxW * 0.065f).coerceIn(22f, 62f)
                        val bottomPad = (boxH * 0.16f).coerceIn(72f, 160f)
                        val photoRect = RectF(
                            minX + sidePad,
                            minY + sidePad,
                            (maxX - sidePad).coerceAtLeast(minX + sidePad + 1f),
                            (maxY - bottomPad).coerceAtLeast(minY + sidePad + 1f)
                        )
                        val clipPath = Path().apply {
                            addRoundRect(photoRect, 10f, 10f, Path.Direction.CW)
                        }
                        drawBitmapInClippedRect(canvas, clipPath, photoRect, slotBitmap, transform)
                    }

                    CollageSlotFrameStyle.FLOATING_INSET -> {
                        val insetRect = RectF(minX, minY, maxX, maxY)
                        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            setShadowLayer(34f, 0f, 18f, Color.argb(130, 0, 0, 0))
                        }
                        canvas.drawRoundRect(insetRect, cornerPx, cornerPx, shadowPaint)

                        val clipPath = Path().apply {
                            addRoundRect(insetRect, cornerPx, cornerPx, Path.Direction.CW)
                        }
                        drawBitmapInClippedRect(canvas, clipPath, insetRect, slotBitmap, transform)

                        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = 12f
                            color = Color.argb(235, 255, 255, 255)
                        }
                        canvas.drawRoundRect(insetRect, cornerPx, cornerPx, rimPaint)
                    }

                    CollageSlotFrameStyle.EDGE_TO_EDGE -> {
                        val clipPath = buildAndroidRoundedPolygonPath(polyPts, cornerPx)
                        val boundsRect = RectF(minX, minY, maxX, maxY)
                        drawBitmapInClippedRect(canvas, clipPath, boundsRect, slotBitmap, transform)
                    }
                }
                canvas.restore()
            }

            if (includeYearWatermark) {
                drawEditorialYearStamp(canvas, year, editionColorArgb)
            }

            decodedCache.values.forEach { bmp ->
                if (bmp != null && !bmp.isRecycled) bmp.recycle()
            }

            saveBitmapToGallery(context, bitmap, fileName)
        } catch (e: Exception) {
            CollageExportResult(
                success = false,
                displayName = fileName,
                message = "导出失败：${e.localizedMessage ?: "请稍后重试"}"
            )
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun drawBitmapInClippedRect(
        canvas: Canvas,
        clipPath: Path,
        destRect: RectF,
        bitmap: Bitmap?,
        transform: CollageSlotTransform
    ) {
        canvas.save()
        canvas.clipPath(clipPath)
        if (bitmap == null) {
            val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#232938") }
            canvas.drawRect(destRect, placeholder)
            canvas.restore()
            return
        }

        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val dw = destRect.width().coerceAtLeast(1f)
        val dh = destRect.height().coerceAtLeast(1f)

        val baseScale = max(dw / bw, dh / bh)
        val userScale = transform.scale.coerceIn(1f, 3f)
        val totalScale = baseScale * userScale

        val matrix = Matrix()
        matrix.postTranslate(-bw / 2f, -bh / 2f)
        matrix.postScale(totalScale, totalScale)
        matrix.postTranslate(
            destRect.centerX() + transform.offsetX * dw,
            destRect.centerY() + transform.offsetY * dh
        )

        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(bitmap, matrix, bitmapPaint)
        canvas.restore()
    }

    private fun drawEditorialYearStamp(canvas: Canvas, year: Int, editionColorArgb: Int) {
        val w = EXPORT_WIDTH.toFloat()
        val h = EXPORT_HEIGHT.toFloat()
        val r = Color.red(editionColorArgb)
        val g = Color.green(editionColorArgb)
        val b = Color.blue(editionColorArgb)

        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.66f, 0f, h,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(145, r, g, b),
                    Color.argb(228, r, g, b)
                ),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, h * 0.66f, w, h, scrimPaint)

        val yearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 168f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = -0.03f
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 255, 255, 255)
            textSize = 36f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            letterSpacing = 0.22f
        }
        canvas.drawText(year.toString(), 96f, h - 148f, yearPaint)
        canvas.drawText("ANNUAL MONOGRAPH · FLIPYEAR", 102f, h - 88f, subtitlePaint)
    }

    private fun buildAndroidRoundedPolygonPath(points: List<Offset>, cornerRadiusPx: Float): Path {
        val path = Path()
        if (points.size < 3) return path
        if (cornerRadiusPx <= 0.5f) {
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
            path.close()
            return path
        }
        val n = points.size
        for (i in 0 until n) {
            val prev = points[(i - 1 + n) % n]
            val curr = points[i]
            val next = points[(i + 1) % n]
            val v1x = prev.x - curr.x
            val v1y = prev.y - curr.y
            val v2x = next.x - curr.x
            val v2y = next.y - curr.y
            val len1 = hypot(v1x, v1y).coerceAtLeast(1e-4f)
            val len2 = hypot(v2x, v2y).coerceAtLeast(1e-4f)
            val rad = min(cornerRadiusPx, min(len1 * 0.35f, len2 * 0.35f))

            val startX = curr.x + v1x / len1 * rad
            val startY = curr.y + v1y / len1 * rad
            val endX = curr.x + v2x / len2 * rad
            val endY = curr.y + v2y / len2 * rad

            if (i == 0) path.moveTo(startX, startY) else path.lineTo(startX, startY)
            path.quadTo(curr.x, curr.y, endX, endY)
        }
        path.close()
        return path
    }

    private fun decodePhotoBitmap(context: Context, photo: PhotoItemEntity, targetMaxPx: Int): Bitmap? {
        val uriStr = photo.contentUri
        if (!uriStr.isNullOrBlank()) {
            val uri = runCatching { Uri.parse(uriStr) }.getOrNull()
            if (uri != null) {
                if (photo.isVideo) {
                    val frame = runCatching {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } finally {
                            runCatching { retriever.release() }
                        }
                    }.getOrNull()
                    if (frame != null) return frame
                } else {
                    val decoded = runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(context.contentResolver, uri)
                            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                val maxSide = max(info.size.width, info.size.height)
                                if (maxSide > targetMaxPx) {
                                    val ratio = targetMaxPx.toFloat() / maxSide.toFloat()
                                    decoder.setTargetSize(
                                        (info.size.width * ratio).toInt().coerceAtLeast(1),
                                        (info.size.height * ratio).toInt().coerceAtLeast(1)
                                    )
                                }
                            }
                        } else {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                    }.getOrNull()
                    if (decoded != null) return decoded
                }
            }
        }

        // Graceful fallback to bundled sample drawable
        return runCatching {
            val resId = resolveSampleDrawable(photo.drawableName)
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            BitmapFactory.decodeResource(context.resources, resId, opts)
        }.getOrNull()
    }

    private suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): CollageExportResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FlipYear")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext CollageExportResult(false, fileName, "无法创建相册保存记录")

            val wrote = runCatching {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
                } ?: false
            }.getOrDefault(false)

            if (wrote) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                CollageExportResult(true, fileName, "已导出高清拼图封面至相册 Pictures/FlipYear")
            } else {
                resolver.delete(uri, null, null)
                CollageExportResult(false, fileName, "写入高清封面文件失败")
            }
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FlipYear").apply { mkdirs() }
            val target = File(dir, fileName)
            FileOutputStream(target).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }
            CollageExportResult(true, fileName, "已保存高清封面至 ${target.name}")
        }
    }
}
