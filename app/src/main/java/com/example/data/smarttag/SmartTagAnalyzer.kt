package com.example.data.smarttag

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.example.data.PhotoItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * MobileCLIP zero-shot tagging against the fixed smart taxonomy. The design keeps a seam for
 * adding ML Kit object labels later, but the shipped implementation is CLIP-only: it needs no
 * Google Play services, no model downloads, and has zero transitive dependency cost.
 */
class SmartTagAnalyzer(context: Context) {

    private val appContext = context.applicationContext
    private val clipEmbedder = ClipEmbedder(appContext)
    private val clipMutex = Mutex()

    data class SmartTagResult(
        val categoryKeys: List<String>,
        val embeddingFp16: ByteArray?,
        val mlKitLabels: List<String>,
        val usedClip: Boolean
    )

    suspend fun analyze(input: Bitmap, includeClip: Boolean = true): SmartTagResult {
        val bitmap = if (input.config != Bitmap.Config.ARGB_8888) {
            input.copy(Bitmap.Config.ARGB_8888, false) ?: input
        } else {
            input
        }
        var embeddingBytes: ByteArray? = null
        var usedClip = false
        val clipCategories = mutableListOf<SmartCategory>()
        if (includeClip) {
            val analysis = runClip(bitmap)
            if (analysis != null) {
                usedClip = true
                clipCategories.addAll(analysis.categories)
                embeddingBytes = encodeEmbeddingFp16(analysis.embedding)
            }
        }

        val capped = clipCategories.take(SmartTagHeuristics.MAX_TAGS_PER_PHOTO + 2)
        return SmartTagResult(
            categoryKeys = capped.map { it.key },
            embeddingFp16 = embeddingBytes,
            mlKitLabels = emptyList(),
            usedClip = usedClip
        )
    }

    private suspend fun runClip(bitmap: Bitmap): ClipEmbedder.ClipAnalysis? =
        withContext(Dispatchers.Default) {
            clipMutex.withLock {
                try {
                    clipEmbedder.analyze(bitmap)
                } catch (t: Throwable) {
                    Log.w(TAG, "CLIP inference failed", t)
                    null
                }
            }
        }

    /**
     * Decodes a downsampled bitmap for any photo row: device media (content Uri),
     * bundled sample drawables, or the first frame for videos.
     */
    suspend fun decodeBitmapFor(photo: PhotoItemEntity, drawableResolver: (String?) -> Int?): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (photo.isVideo) {
                    // Demo videos carry no playable content Uri; fall back to their bundled artwork.
                    decodeVideoFrame(photo) ?: decodeDrawableArtwork(photo, drawableResolver)
                } else if (!photo.contentUri.isNullOrBlank()) {
                    decodeSampledImage(photo.contentUri)
                } else {
                    decodeDrawableArtwork(photo, drawableResolver)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "decode failed for photo ${photo.id}", t)
                null
            }
        }

    private fun decodeDrawableArtwork(photo: PhotoItemEntity, drawableResolver: (String?) -> Int?): Bitmap? {
        if (photo.drawableName.isNullOrBlank()) return null
        val resId = drawableResolver(photo.drawableName)
        if (resId == null || resId == 0) return null
        val drawable = appContext.resources.getDrawable(resId, null)
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            @Suppress("UseKtx")
            drawable.toBitmap(width = 512, height = 512)
        }
    }

    private fun decodeVideoFrame(photo: PhotoItemEntity): Bitmap? {
        val uriStr = photo.contentUri ?: return null
        if (!uriStr.startsWith("content://")) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, Uri.parse(uriStr))
            val timeUs = (photo.durationMs.coerceAtLeast(1000L) / 2) * 1000L
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun decodeSampledImage(uriStr: String): Bitmap? {
        val uri = Uri.parse(uriStr)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return try {
                val source = ImageDecoder.createSource(appContext.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val maxDim = maxOf(w, h)
                    if (maxDim > DECODE_TARGET_MAX_DIM) {
                        val scale = DECODE_TARGET_MAX_DIM.toFloat() / maxDim
                        decoder.setTargetSize(
                            (w * scale).toInt().coerceAtLeast(1),
                            (h * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (_: Throwable) {
                decodeSampledImageLegacy(uri)
            }
        }
        return decodeSampledImageLegacy(uri)
    }

    private fun decodeSampledImageLegacy(uri: Uri): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: return null
        // decodeStream always returns null in inJustDecodeBounds mode even on success;
        // the dimensions land in `bounds`, so the null result must not be treated as failure.
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxDim / (sample * 2) >= DECODE_TARGET_MAX_DIM) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    fun close() {
        clipEmbedder.close()
    }

    companion object {
        private const val TAG = "SmartTagAnalyzer"
        private const val DECODE_TARGET_MAX_DIM = 512
    }
}
