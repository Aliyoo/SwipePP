package com.example.data

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class CompressionPreset(
    val title: String,
    val subtitle: String,
    val jpegQuality: Int,
    val maxLongEdgePx: Int,
    val estimatedSavingsRatio: Float
) {
    NEAR_LOSSLESS(
        title = "极臻近无损",
        subtitle = "90% 品质（肉眼几乎无差异）· 保留原图尺寸与 EXIF",
        jpegQuality = 90,
        maxLongEdgePx = 4096,
        estimatedSavingsRatio = 0.42f
    ),
    BALANCED(
        title = "均衡轻量化",
        subtitle = "长边缩到 2560px · 兼顾高清画质与省空间",
        jpegQuality = 84,
        maxLongEdgePx = 2560,
        estimatedSavingsRatio = 0.62f
    ),
    COMPACT(
        title = "极致省空间",
        subtitle = "长边缩到 1920px · 适合日常浏览，释放最大空间",
        jpegQuality = 76,
        maxLongEdgePx = 1920,
        estimatedSavingsRatio = 0.76f
    )
}

data class CompressedPhotoUpdate(
    val id: Long,
    val newSizeBytes: Long,
    val newWidthPx: Int,
    val newHeightPx: Int,
    val savedBytes: Long,
    val newCompressedPhoto: PhotoItemEntity? = null
)

data class BatchCompressionResult(
    val compressedUpdates: List<CompressedPhotoUpdate>,
    val skippedAlreadyOptimalCount: Int,
    val skippedVideoCount: Int,
    val failedCount: Int,
    val skippedProtectedCount: Int = 0
) {
    val totalSavedBytes: Long
        get() = compressedUpdates.sumOf { it.savedBytes }
}

data class LocationPreset(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

val CURATED_LOCATION_PRESETS: List<LocationPreset> = listOf(
    LocationPreset("北京", 39.9042, 116.4074),
    LocationPreset("上海", 31.2304, 121.4737),
    LocationPreset("杭州", 30.2741, 120.1551),
    LocationPreset("成都", 30.5728, 104.0668),
    LocationPreset("深圳", 22.5431, 114.0579),
    LocationPreset("广州", 23.1291, 113.2644),
    LocationPreset("西安", 34.3416, 108.9398),
    LocationPreset("重庆", 29.5630, 106.5516),
    LocationPreset("大理", 25.6065, 100.2679),
    LocationPreset("三亚", 18.2528, 109.5119),
    LocationPreset("香港", 22.3193, 114.1694),
    LocationPreset("东京", 35.6762, 139.6503),
    LocationPreset("京都", 35.0116, 135.7681)
)

data class SameDayLocationMatch(
    val photo: PhotoItemEntity,
    val matchedLocationName: String,
    val matchedLatitude: Double,
    val matchedLongitude: Double,
    val deltaMillis: Long
)

/**
 * Pure heuristic helper that matches unlocated photos against geotagged photos taken within
 * [maxWindowHours] hours (default 24h), picking the chronologically closest geotagged neighbor.
 */
fun inferSameDayLocations(
    targets: List<PhotoItemEntity>,
    allPhotos: List<PhotoItemEntity>,
    maxWindowHours: Int = 24
): List<SameDayLocationMatch> {
    if (targets.isEmpty() || allPhotos.isEmpty()) return emptyList()
    val maxWindowMs = maxWindowHours * 3_600_000L

    val located = allPhotos
        .filter { p ->
            p.photoStatus != PhotoStatus.TRASHED &&
                p.photoStatus != PhotoStatus.PURGED &&
                !p.locationName.isNullOrBlank() &&
                p.latitude != null &&
                p.longitude != null &&
                (abs(p.latitude) > 0.0001 || abs(p.longitude) > 0.0001)
        }
        .sortedBy { it.takenAtMillis }

    if (located.isEmpty()) return emptyList()

    val matches = ArrayList<SameDayLocationMatch>()
    for (target in targets) {
        val ts = target.takenAtMillis
        // Binary search nearest timestamp in sorted geotagged list -> O(N log M)
        var lo = 0
        var hi = located.lastIndex
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (located[mid].takenAtMillis < ts) {
                lo = mid + 1
            } else {
                hi = mid
            }
        }
        var best: PhotoItemEntity? = null
        var bestDelta = Long.MAX_VALUE
        for (idx in (lo - 1)..(lo + 1)) {
            if (idx in located.indices) {
                val cand = located[idx]
                val d = abs(cand.takenAtMillis - ts)
                if (d <= maxWindowMs && d < bestDelta) {
                    bestDelta = d
                    best = cand
                }
            }
        }
        if (best != null && best.latitude != null && best.longitude != null && !best.locationName.isNullOrBlank()) {
            matches += SameDayLocationMatch(
                photo = target,
                matchedLocationName = best.locationName,
                matchedLatitude = best.latitude,
                matchedLongitude = best.longitude,
                deltaMillis = bestDelta
            )
        }
    }
    return matches
}

fun isSafelyCompressibleImage(photo: PhotoItemEntity): Boolean {
    if (photo.isVideo || photo.isLivePhoto || photo.isAnimated) return false
    // Unprobed files may still be undetected embedded-motion photos; an in-place EXIF rewrite
    // (or recompression) would destroy the appended video, so writes wait for the probe.
    if (photo.mediaProbeVersion < com.example.data.MEDIA_PROBE_VERSION) return false
    if (photo.isSample) return true
    return photo.imageFormatKind == ImageFormat.JPEG ||
        (photo.imageFormatKind == ImageFormat.UNKNOWN && photo.mimeType?.equals("image/jpeg", ignoreCase = true) == true)
}

fun isDirectExifWritableImage(photo: PhotoItemEntity): Boolean {
    return isSafelyCompressibleImage(photo) && !photo.isSample
}

/**
 * Handles scoped-storage safe batch image near-lossless compression (preserving EXIF),
 * batch EXIF GPS writing, and forward geocoding for manual location tagging.
 */
class BatchMediaEditor(private val context: Context) {

    fun buildSystemWritePendingIntent(photos: List<PhotoItemEntity>): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uris = photos
            .filter { isDirectExifWritableImage(it) && !it.contentUri.isNullOrBlank() && it.contentUri.startsWith("content://") }
            .map { Uri.parse(it.contentUri) }
            .distinct()
        if (uris.isEmpty()) return null
        return runCatching {
            MediaStore.createWriteRequest(context.contentResolver, uris)
        }.getOrNull()
    }

    suspend fun compressPhotosNearLossless(
        photos: List<PhotoItemEntity>,
        preset: CompressionPreset,
        onProgress: (processed: Int, total: Int, savedBytesSoFar: Long) -> Unit
    ): BatchCompressionResult = withContext(Dispatchers.IO) {
        val imageTargets = photos.filter { !it.isVideo && isSafelyCompressibleImage(it) }
        val skippedVideos = photos.count { it.isVideo }
        val skippedProtected = photos.count { !it.isVideo && !isSafelyCompressibleImage(it) }
        val updates = ArrayList<CompressedPhotoUpdate>(imageTargets.size)
        var skippedOptimal = 0
        var failed = 0
        var processed = 0
        var savedSoFar = 0L

        onProgress(0, imageTargets.size, 0L)

        for (photo in imageTargets) {
            val outcome = if (photo.isSample || photo.contentUri.isNullOrBlank() || !photo.contentUri.startsWith("content://")) {
                compressSamplePhotoMetadata(photo, preset)
            } else {
                compressDevicePhotoSafeCopy(photo, preset)
            }

            when (outcome) {
                is SingleCompressOutcome.Compressed -> {
                    updates += outcome.update
                    savedSoFar += outcome.update.savedBytes
                }
                SingleCompressOutcome.SkippedOptimal -> skippedOptimal++
                SingleCompressOutcome.Failed -> failed++
            }
            processed++
            onProgress(processed, imageTargets.size, savedSoFar)
        }

        BatchCompressionResult(
            compressedUpdates = updates,
            skippedAlreadyOptimalCount = skippedOptimal,
            skippedVideoCount = skippedVideos,
            failedCount = failed,
            skippedProtectedCount = skippedProtected
        )
    }

    /**
     * Writes GPS latitude & longitude into EXIF metadata of real device JPEGs in-place.
     * Returns number of files whose EXIF tags were successfully updated.
     */
    suspend fun writeExifLocationToPhotos(
        updates: List<Pair<PhotoItemEntity, LocationBatchUpdate>>,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val realImages = updates.filter { (photo, loc) ->
            isDirectExifWritableImage(photo) &&
                !photo.contentUri.isNullOrBlank() &&
                photo.contentUri.startsWith("content://") &&
                loc.latitude != null &&
                loc.longitude != null
        }
        var writtenCount = 0
        realImages.forEachIndexed { idx, (photo, loc) ->
            val lat = loc.latitude ?: return@forEachIndexed
            val lon = loc.longitude ?: return@forEachIndexed
            val ok = runCatching {
                val uri = Uri.parse(photo.contentUri)
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    exif.setLatLong(lat, lon)
                    exif.saveAttributes()
                    true
                } ?: false
            }.getOrDefault(false)
            if (ok) writtenCount++
            onProgress(idx + 1, realImages.size)
        }
        writtenCount
    }

    /**
     * Resolves a user-typed city or landmark name into real coordinates using Android Geocoder,
     * falling back to built-in curated presets if the device is offline.
     */
    suspend fun geocodeLocationQuery(rawQuery: String): ResolvedGeoLocation? = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isEmpty()) return@withContext null

        // Instant hit against curated preset list
        CURATED_LOCATION_PRESETS.firstOrNull {
            it.name.equals(query, ignoreCase = true) || query.contains(it.name)
        }?.let { preset ->
            return@withContext ResolvedGeoLocation(
                latitude = preset.latitude,
                longitude = preset.longitude,
                locationName = query
            )
        }

        if (!Geocoder.isPresent()) return@withContext null

        runCatching {
            val geocoder = Geocoder(context, Locale.CHINA)
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocationName(query, 1)
            val first = results?.firstOrNull()
            if (first != null && first.hasLatitude() && first.hasLongitude()) {
                val resolvedName = listOfNotNull(
                    first.locality?.takeIf { it.isNotBlank() },
                    first.subLocality?.takeIf { it.isNotBlank() }
                ).distinct().joinToString(" · ").ifBlank {
                    first.featureName?.takeIf { it.isNotBlank() } ?: query
                }
                ResolvedGeoLocation(
                    latitude = first.latitude,
                    longitude = first.longitude,
                    locationName = if (query.length <= 16) query else resolvedName
                )
            } else {
                null
            }
        }.getOrNull()
    }

    private sealed interface SingleCompressOutcome {
        data class Compressed(val update: CompressedPhotoUpdate) : SingleCompressOutcome
        data object SkippedOptimal : SingleCompressOutcome
        data object Failed : SingleCompressOutcome
    }

    private fun compressSamplePhotoMetadata(
        photo: PhotoItemEntity,
        preset: CompressionPreset
    ): SingleCompressOutcome {
        val minFloorBytes = 180_000L
        if (photo.fileSizeBytes <= minFloorBytes) {
            return SingleCompressOutcome.SkippedOptimal
        }
        val targetBytes = (photo.fileSizeBytes * (1f - preset.estimatedSavingsRatio))
            .toLong()
            .coerceAtLeast(minFloorBytes)
        if (targetBytes >= photo.fileSizeBytes) {
            return SingleCompressOutcome.SkippedOptimal
        }
        val maxOrigEdge = max(photo.widthPx, photo.heightPx).coerceAtLeast(1)
        val scale = if (maxOrigEdge > preset.maxLongEdgePx) {
            preset.maxLongEdgePx.toFloat() / maxOrigEdge.toFloat()
        } else {
            1f
        }
        val newW = (photo.widthPx * scale).roundToInt().coerceAtLeast(1)
        val newH = (photo.heightPx * scale).roundToInt().coerceAtLeast(1)
        val copyCaption = if (photo.caption.contains("已压缩")) {
            photo.caption
        } else {
            "${photo.caption.ifBlank { "示例照片" }} · 已压缩"
        }
        val newCopy = photo.copy(
            id = 0,
            mediaStoreKey = null,
            fileSizeBytes = targetBytes,
            widthPx = newW,
            heightPx = newH,
            status = PhotoStatus.KEPT.name,
            trashedAtMillis = null,
            caption = copyCaption
        )
        return SingleCompressOutcome.Compressed(
            CompressedPhotoUpdate(
                id = photo.id,
                newSizeBytes = targetBytes,
                newWidthPx = newW,
                newHeightPx = newH,
                savedBytes = (photo.fileSizeBytes - targetBytes).coerceAtLeast(0L),
                newCompressedPhoto = newCopy
            )
        )
    }

    /**
     * Safe non-destructive compression: writes compressed JPEG + preserved EXIF to a brand-new
     * MediaStore file instead of overwriting the original in place. The original photo is moved
     * into FlipYear's two-stage Trash Vault so the user can always restore the original if needed.
     */
    private fun compressDevicePhotoSafeCopy(
        photo: PhotoItemEntity,
        preset: CompressionPreset
    ): SingleCompressOutcome {
        val uri = runCatching { Uri.parse(photo.contentUri) }.getOrNull()
            ?: return SingleCompressOutcome.Failed
        val resolver = context.contentResolver

        // 1. Snapshot original EXIF metadata & orientation before decoding
        var savedExifTags = emptyMap<String, String>()
        var orientation = ExifInterface.ORIENTATION_NORMAL
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val map = HashMap<String, String>(PRESERVED_EXIF_TAGS.size)
                for (tag in PRESERVED_EXIF_TAGS) {
                    val value = exif.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        map[tag] = value
                    }
                }
                savedExifTags = map
            }
        }

        // 2. Measure bounds first (OOM-safe decode for 50MP+ photos)
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
        }
        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        if (rawW <= 0 || rawH <= 0) return SingleCompressOutcome.Failed

        var sampleSize = 1
        val rawLongEdge = max(rawW, rawH)
        while ((rawLongEdge / (sampleSize * 2)) >= preset.maxLongEdgePx) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }.getOrNull() ?: return SingleCompressOutcome.Failed

        var workingBitmap: Bitmap = decoded
        try {
            // Apply EXIF rotation so compressed JPEG is stored upright cleanly
            val rotated = applyExifOrientation(workingBitmap, orientation)
            if (rotated !== workingBitmap) {
                workingBitmap.recycle()
                workingBitmap = rotated
            }

            // Precise downscale if long edge still exceeds preset cap
            val curLongEdge = max(workingBitmap.width, workingBitmap.height)
            if (curLongEdge > preset.maxLongEdgePx) {
                val ratio = preset.maxLongEdgePx.toFloat() / curLongEdge.toFloat()
                val targetW = (workingBitmap.width * ratio).roundToInt().coerceAtLeast(1)
                val targetH = (workingBitmap.height * ratio).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(workingBitmap, targetW, targetH, true)
                if (scaled !== workingBitmap) {
                    workingBitmap.recycle()
                    workingBitmap = scaled
                }
            }

            val finalW = workingBitmap.width
            val finalH = workingBitmap.height

            // 3. Compress into a temporary cache file so we can preserve EXIF & check exact final byte size
            val tempFile = File.createTempFile("swipepp_cmp_${photo.id}_", ".jpg", context.cacheDir)
            try {
                FileOutputStream(tempFile).use { fos ->
                    val ok = workingBitmap.compress(Bitmap.CompressFormat.JPEG, preset.jpegQuality, fos)
                    if (!ok) return SingleCompressOutcome.Failed
                }

                // Restore EXIF tags (normalize orientation to NORMAL since pixels are upright)
                if (savedExifTags.isNotEmpty()) {
                    runCatching {
                        val outExif = ExifInterface(tempFile)
                        for ((tag, value) in savedExifTags) {
                            outExif.setAttribute(tag, value)
                        }
                        outExif.setAttribute(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL.toString()
                        )
                        outExif.saveAttributes()
                    }
                }

                val compressedBytes = tempFile.length()
                // Strict safety guard: never write if compressed output isn't at least 4% smaller
                if (compressedBytes <= 0L || compressedBytes >= (photo.fileSizeBytes * 0.96).toLong()) {
                    return SingleCompressOutcome.SkippedOptimal
                }

                // 4. Insert a brand-new MediaStore JPEG file (original remains untouched & recoverable)
                val safeBaseName = photo.caption
                    .substringBeforeLast('.')
                    .replace(Regex("[^A-Za-z0-9_\\-\u4e00-\u9fa5]"), "_")
                    .take(32)
                    .ifBlank { "SwipePP_${photo.id}" }
                val displayName = "${safeBaseName}_cmp_${System.currentTimeMillis() % 100000}.jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DATE_TAKEN, photo.takenAtMillis)
                    put(MediaStore.Images.Media.WIDTH, finalW)
                    put(MediaStore.Images.Media.HEIGHT, finalH)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val targetFolder = photo.bucketName?.takeIf { it.isNotBlank() } ?: "SwipePP"
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/$targetFolder"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val newUri = runCatching {
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                }.getOrNull() ?: return SingleCompressOutcome.Failed

                val writeSuccess = runCatching {
                    resolver.openOutputStream(newUri, "w")?.use { out ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                        true
                    } ?: false
                }.getOrDefault(false)

                if (!writeSuccess) {
                    runCatching { resolver.delete(newUri, null, null) }
                    return SingleCompressOutcome.Failed
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        val doneValues = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        resolver.update(newUri, doneValues, null, null)
                    }
                }

                val newMediaId = runCatching { ContentUris.parseId(newUri) }.getOrNull()
                val newMediaKey = newMediaId?.takeIf { it > 0L }?.let { "img_$it" }
                val newCopy = photo.copy(
                    id = 0,
                    mediaStoreKey = newMediaKey,
                    mimeType = "image/jpeg",
                    imageFormat = ImageFormat.JPEG.name,
                    isAnimated = false,
                    motionPhotoType = MotionPhotoType.NONE.name,
                    motionVideoUri = null,
                    motionVideoDurationMs = 0L,
                    motionVideoOffset = null,
                    motionVideoLength = null,
                    pairedMediaStoreKey = null,
                    contentUri = newUri.toString(),
                    fileSizeBytes = compressedBytes,
                    widthPx = finalW,
                    heightPx = finalH,
                    status = PhotoStatus.KEPT.name,
                    trashedAtMillis = null,
                    isBestInStack = photo.isBestInStack,
                    caption = if (photo.caption.contains("已压缩")) photo.caption else "${photo.caption.ifBlank { "照片" }} · 已压缩"
                )

                return SingleCompressOutcome.Compressed(
                    CompressedPhotoUpdate(
                        id = photo.id,
                        newSizeBytes = compressedBytes,
                        newWidthPx = finalW,
                        newHeightPx = finalH,
                        savedBytes = (photo.fileSizeBytes - compressedBytes).coerceAtLeast(0L),
                        newCompressedPhoto = newCopy
                    )
                )
            } finally {
                tempFile.delete()
            }
        } finally {
            if (!workingBitmap.isRecycled) {
                workingBitmap.recycle()
            }
        }
    }

    private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return source
        }
        return runCatching {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }.getOrDefault(source)
    }

    companion object {
        private val PRESERVED_EXIF_TAGS = listOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP
        )
    }
}
