package com.example.data

import android.Manifest
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

enum class MediaAccessLevel {
    NONE,
    PARTIAL,
    FULL
}

data class ResolvedGeoLocation(
    val latitude: Double,
    val longitude: Double,
    val locationName: String
)

data class LegacyDeleteResult(
    val deletedPhotos: List<PhotoItemEntity>,
    val failedPhotos: List<PhotoItemEntity>,
    val failureReason: String? = null,
    val recoverableIntentSender: IntentSender? = null,
    val remainingUnprocessedPhotos: List<PhotoItemEntity> = emptyList()
) {
    val freedBytes: Long
        get() = deletedPhotos.sumOf { it.fileSizeBytes }
}

class LocalMediaScanner(private val context: Context) {

    // Thread-safe ~13km grid cache so Geocoder is queried at most once per city/district
    private val geocodeCache = ConcurrentHashMap<String, String>()

    fun requiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun checkMediaAccess(): MediaAccessLevel {
        val hasImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        val hasVideos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            hasImages
        }

        if (hasImages && hasVideos) return MediaAccessLevel.FULL
        if (hasImages || hasVideos) return MediaAccessLevel.PARTIAL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val selectedGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
            if (selectedGranted) return MediaAccessLevel.PARTIAL
        }
        return MediaAccessLevel.NONE
    }

    fun hasMediaLocationAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    suspend fun scanAllLocalPhotosAndVideos(): List<PhotoItemEntity> = withContext(Dispatchers.IO) {
        if (checkMediaAccess() == MediaAccessLevel.NONE) return@withContext emptyList()

        val rawImages = queryImages()
        val rawVideos = queryVideos()
        val pairing = pairLivePhotoItems(rawImages, rawVideos)
        val imageDisplayNames = rawImages.mapNotNull { item ->
            item.photo.mediaStoreKey?.let { it to item.displayName }
        }.toMap()
        // Embedded-motion detection is file-IO heavy (2-4 opens per photo) and moved to the
        // background media probe so first-scan rows reach the gallery in seconds.
        val groupedImages = assignBurstGroups(pairing.images)
        val visibleVideos = rawVideos.map { it.photo }.filter { video ->
            video.mediaStoreKey !in pairing.consumedVideoKeys
        }
        (groupedImages + visibleVideos).sortedByDescending { it.takenAtMillis }
    }

    suspend fun resolveLocationForItem(item: PhotoItemEntity): ResolvedGeoLocation? = withContext(Dispatchers.IO) {
        if (item.isSample) return@withContext null
        val uriStr = item.contentUri?.takeIf { it.isNotBlank() } ?: return@withContext null
        val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return@withContext null

        val coords = if (item.isVideo) {
            readVideoGps(uri)
        } else {
            readExifGps(uri)
        } ?: return@withContext null

        val label = resolveLocationLabel(item.bucketName, coords.first, coords.second)
        ResolvedGeoLocation(
            latitude = coords.first,
            longitude = coords.second,
            locationName = label
        )
    }

    /**
     * Background media probe result for one photo: embedded-motion playback fields and the
     * animated-image flag, or null when the file carries no probe-able extras.
     */
    data class MediaProbeResult(
        val isAnimated: Boolean,
        val motionPhotoType: String,
        val motionVideoUri: String?,
        val motionVideoOffset: Long?,
        val motionVideoLength: Long?
    )

    /**
     * Probes one image file for embedded motion video and animated WebP content.
     * IO heavy (2-4 file opens); must stay off the scan critical path. Paired live photos
     * and non-image media need no probing — callers skip them.
     */
    fun probeImageMedia(photo: PhotoItemEntity, displayName: String?): MediaProbeResult? {
        if (photo.isVideo) return null
        // No readable file behind this row (e.g. sample drawables): a genuine nothing-to-probe,
        // as opposed to an IO failure which the detector now signals by throwing.
        if (photo.contentUri.isNullOrBlank()) return null
        var animated = false
        var motionType = MotionPhotoType.NONE.name
        var motionUri: String? = null
        var motionOffset: Long? = null
        var motionLength: Long? = null

        if (photo.motionPhotoKind == MotionPhotoType.NONE) {
            val info = EmbeddedMotionPhotoDetector(context).detect(
                contentUri = photo.contentUri.orEmpty(),
                mimeType = photo.mimeType,
                displayName = displayName,
                fileSizeBytes = photo.fileSizeBytes
            )
            if (info != null) {
                animated = true
                motionType = MotionPhotoType.EMBEDDED.name
                motionUri = photo.contentUri
                motionOffset = info.videoOffset
                motionLength = info.videoLength
            }
        }
        if (!animated && photo.imageFormatKind == ImageFormat.WEBP) {
            animated = isAnimatedImageContent(Uri.parse(photo.contentUri.orEmpty()), photo.imageFormatKind)
        }
        if (!animated && motionType == MotionPhotoType.NONE.name) return null
        return MediaProbeResult(
            isAnimated = animated,
            motionPhotoType = motionType,
            motionVideoUri = motionUri,
            motionVideoOffset = motionOffset,
            motionVideoLength = motionLength
        )
    }

    private fun queryImages(): List<ScannedMediaItem> {
        val results = mutableListOf<ScannedMediaItem>()
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
            }
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.LATITUDE)
                add(MediaStore.Images.Media.LONGITUDE)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val favoriteCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                } else {
                    -1
                }
                @Suppress("DEPRECATION")
                val latCol = cursor.getColumnIndex(MediaStore.Images.Media.LATITUDE)
                @Suppress("DEPRECATION")
                val lonCol = cursor.getColumnIndex(MediaStore.Images.Media.LONGITUDE)

                val cal = Calendar.getInstance()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val mimeType = cursor.getString(mimeCol)
                    val imageFormat = resolveImageFormat(mimeType, displayName)
                    val name = displayName.substringBeforeLast('.')
                    val rawTaken = cursor.getLong(takenCol)
                    val dateAddedSec = cursor.getLong(addedCol)
                    val takenAt = when {
                        rawTaken > 0L -> rawTaken
                        dateAddedSec > 0L -> dateAddedSec * 1000L
                        else -> System.currentTimeMillis()
                    }
                    val size = cursor.getLong(sizeCol).coerceAtLeast(1L)
                    val width = cursor.getInt(widthCol).coerceAtLeast(0)
                    val height = cursor.getInt(heightCol).coerceAtLeast(0)
                    val bucket = cursor.getString(bucketCol)
                    val systemFavorite = favoriteCol >= 0 && !cursor.isNull(favoriteCol) && cursor.getInt(favoriteCol) != 0

                    cal.timeInMillis = takenAt

                    var lat: Double? = null
                    var lon: Double? = null
                    var locationLabel: String? = null

                    // Zero-IO legacy MediaStore GPS columns on Android < 10
                    var hasLegacyGps = false
                    if (latCol >= 0 && lonCol >= 0 && !cursor.isNull(latCol) && !cursor.isNull(lonCol)) {
                        val cLat = cursor.getDouble(latCol)
                        val cLon = cursor.getDouble(lonCol)
                        if (isValidCoordinates(cLat, cLon)) {
                            lat = cLat
                            lon = cLon
                            locationLabel = resolveLocationLabel(bucket, cLat, cLon)
                            hasLegacyGps = true
                        }
                    }

                    val mp = (width.toLong() * height.toLong()) / 1_000_000.0
                    val quality = ((mp * 3.2).toInt().coerceIn(45, 68) +
                        ((size / 1_000_000.0) * 5.5).toInt().coerceIn(8, 28)).coerceIn(58, 98)

                    val mediaSubtype = classifyMediaSubtype(
                        bucketName = bucket,
                        displayName = displayName,
                        widthPx = width,
                        heightPx = height,
                        isVideo = false
                    )
                    // GIF is certain from the MIME type alone; animated WebP needs a file
                    // sniff, which the background media probe owns (mediaProbeVersion).
                    val animated = imageFormat == ImageFormat.GIF

                    results += ScannedMediaItem(
                        photo = PhotoItemEntity(
                            mediaStoreKey = "img_$id",
                            mediaType = MediaType.IMAGE.name,
                            durationMs = 0L,
                            mimeType = mimeType,
                            imageFormat = imageFormat.name,
                            mediaSubtype = mediaSubtype.name,
                            isAnimated = animated,
                            year = cal.get(Calendar.YEAR),
                            month = cal.get(Calendar.MONTH) + 1,
                            day = cal.get(Calendar.DAY_OF_MONTH),
                            takenAtMillis = takenAt,
                            fileSizeBytes = size,
                            widthPx = width,
                            heightPx = height,
                            drawableName = null,
                            contentUri = contentUri.toString(),
                            status = PhotoStatus.UNREVIEWED.name,
                            isFavorite = systemFavorite,
                            burstGroupId = null,
                            qualityScore = quality,
                            isBestInStack = false,
                            visualVariant = "CRISP",
                            caption = name.ifBlank { "本地照片" },
                            bucketName = bucket,
                            locationName = locationLabel,
                            latitude = lat,
                            longitude = lon,
                            isSample = false,
                            gpsScanned = hasLegacyGps
                        ),
                        displayName = displayName
                    )
                }
            }
        }
        return results
    }

    private fun queryVideos(): List<ScannedMediaItem> {
        val results = mutableListOf<ScannedMediaItem>()
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.DATE_TAKEN)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_FAVORITE)
            }
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.LATITUDE)
                add(MediaStore.Video.Media.LONGITUDE)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC, ${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val favoriteCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE)
                } else {
                    -1
                }
                @Suppress("DEPRECATION")
                val latCol = cursor.getColumnIndex(MediaStore.Video.Media.LATITUDE)
                @Suppress("DEPRECATION")
                val lonCol = cursor.getColumnIndex(MediaStore.Video.Media.LONGITUDE)

                val cal = Calendar.getInstance()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val mimeType = cursor.getString(mimeCol)
                    val name = displayName.substringBeforeLast('.')
                    val rawTaken = cursor.getLong(takenCol)
                    val dateAddedSec = cursor.getLong(addedCol)
                    val takenAt = when {
                        rawTaken > 0L -> rawTaken
                        dateAddedSec > 0L -> dateAddedSec * 1000L
                        else -> System.currentTimeMillis()
                    }
                    val size = cursor.getLong(sizeCol).coerceAtLeast(1L)
                    val width = cursor.getInt(widthCol).coerceAtLeast(0)
                    val height = cursor.getInt(heightCol).coerceAtLeast(0)
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val bucket = cursor.getString(bucketCol)
                    val systemFavorite = favoriteCol >= 0 && !cursor.isNull(favoriteCol) && cursor.getInt(favoriteCol) != 0

                    cal.timeInMillis = takenAt

                    var lat: Double? = null
                    var lon: Double? = null
                    var locationLabel: String? = null

                    var hasLegacyGps = false
                    if (latCol >= 0 && lonCol >= 0 && !cursor.isNull(latCol) && !cursor.isNull(lonCol)) {
                        val cLat = cursor.getDouble(latCol)
                        val cLon = cursor.getDouble(lonCol)
                        if (isValidCoordinates(cLat, cLon)) {
                            lat = cLat
                            lon = cLon
                            locationLabel = resolveLocationLabel(bucket, cLat, cLon)
                            hasLegacyGps = true
                        }
                    }

                    val mediaSubtype = classifyMediaSubtype(
                        bucketName = bucket,
                        displayName = displayName,
                        widthPx = width,
                        heightPx = height,
                        isVideo = true
                    )

                    results += ScannedMediaItem(
                        photo = PhotoItemEntity(
                            mediaStoreKey = "vid_$id",
                            mediaType = MediaType.VIDEO.name,
                            durationMs = duration,
                            mimeType = mimeType,
                            imageFormat = ImageFormat.UNKNOWN.name,
                            mediaSubtype = mediaSubtype.name,
                            isAnimated = false,
                            year = cal.get(Calendar.YEAR),
                            month = cal.get(Calendar.MONTH) + 1,
                            day = cal.get(Calendar.DAY_OF_MONTH),
                            takenAtMillis = takenAt,
                            fileSizeBytes = size,
                            widthPx = width,
                            heightPx = height,
                            drawableName = null,
                            contentUri = contentUri.toString(),
                            status = PhotoStatus.UNREVIEWED.name,
                            isFavorite = systemFavorite,
                            burstGroupId = null,
                            qualityScore = 90,
                            isBestInStack = false,
                            visualVariant = "CRISP",
                            caption = name.ifBlank { "本地视频" },
                            bucketName = bucket,
                            locationName = locationLabel,
                            latitude = lat,
                            longitude = lon,
                            isSample = false,
                            gpsScanned = hasLegacyGps
                        ),
                        displayName = displayName
                    )
                }
            }
        }
        return results
    }

    private fun isAnimatedImageContent(contentUri: Uri, format: ImageFormat): Boolean {
        if (format == ImageFormat.GIF) return true
        if (format != ImageFormat.WEBP) return false
        val header = runCatching {
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                val buffer = ByteArray(128)
                val count = input.read(buffer)
                if (count > 0) buffer.copyOf(count) else byteArrayOf()
            }
        }.getOrNull() ?: return false
        return isPotentiallyAnimatedImage(format, header)
    }

    private fun readExifGps(uri: Uri): Pair<Double, Double>? {
        val candidateUris = buildCandidateUris(uri)
        for (targetUri in candidateUris) {
            val coords = runCatching {
                context.contentResolver.openInputStream(targetUri)?.use { input ->
                    val exif = ExifInterface(input)
                    val latLong = exif.latLong
                    if (latLong != null && isValidCoordinates(latLong[0], latLong[1])) {
                        latLong[0] to latLong[1]
                    } else {
                        null
                    }
                }
            }.getOrNull()
            if (coords != null) return coords
        }
        return null
    }

    /**
     * Reads ISO-6709 GPS coordinates embedded in MP4/MOV video metadata via MediaMetadataRetriever.
     */
    private fun readVideoGps(uri: Uri): Pair<Double, Double>? {
        val candidateUris = buildCandidateUris(uri)
        for (targetUri in candidateUris) {
            val coords = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, targetUri)
                    val isoLocation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                    parseIso6709Location(isoLocation)
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrNull()
            if (coords != null) return coords
        }
        return null
    }

    private fun buildCandidateUris(uri: Uri): List<Uri> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasMediaLocationAccess()) {
            listOfNotNull(
                runCatching { MediaStore.setRequireOriginal(uri) }.getOrNull(),
                uri
            )
        } else {
            listOf(uri)
        }
    }

    private fun parseIso6709Location(raw: String?): Pair<Double, Double>? {
        if (raw.isNullOrBlank()) return null
        val match = ISO_6709_REGEX.find(raw.trim()) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return if (isValidCoordinates(lat, lon)) lat to lon else null
    }

    private fun isValidCoordinates(lat: Double, lon: Double): Boolean {
        return lat.isFinite() &&
            lon.isFinite() &&
            lat in -85.0..85.0 &&
            lon in -180.0..180.0 &&
            (abs(lat) > 0.0001 || abs(lon) > 0.0001)
    }

    private fun resolveLocationLabel(bucket: String?, lat: Double, lon: Double): String {
        val gridLat = kotlin.math.round(lat * 8.0) / 8.0
        val gridLon = kotlin.math.round(lon * 8.0) / 8.0
        val cacheKey = String.format(Locale.US, "%.3f,%.3f", gridLat, gridLon)

        geocodeCache[cacheKey]?.let { return it }

        val geocoded = reverseGeocodeCity(lat, lon)
        if (!geocoded.isNullOrBlank()) {
            geocodeCache[cacheKey] = geocoded
            return geocoded
        }

        val normalized = bucket?.trim().orEmpty()
        val isGenericBucket = normalized.isBlank() || normalized.lowercase(Locale.US) in GENERIC_CAMERA_BUCKETS
        val fallback = if (!isGenericBucket) {
            normalized
        } else {
            val ns = if (gridLat >= 0) "N" else "S"
            val ew = if (gridLon >= 0) "E" else "W"
            String.format(Locale.US, "%.1f°%s %.1f°%s", abs(gridLat), ns, abs(gridLon), ew)
        }
        geocodeCache[cacheKey] = fallback
        return fallback
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeCity(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull() ?: return@runCatching null
            val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
            val district = addr.subLocality?.takeIf { it != city }
            when {
                !city.isNullOrBlank() && !district.isNullOrBlank() -> "$city · $district"
                !city.isNullOrBlank() -> city
                !addr.countryName.isNullOrBlank() -> addr.countryName
                else -> null
            }
        }.getOrNull()
    }

    private fun assignBurstGroups(images: List<PhotoItemEntity>): List<PhotoItemEntity> =
        groupBurstImages(images)

    fun buildSystemTrashPendingIntent(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            MediaStore.createTrashRequest(context.contentResolver, uris, true)
        }.getOrNull()
    }

    /**
     * Real physical deletion for Android 10 (API 29) and below where MediaStore.createTrashRequest
     * is unavailable. Deletes each MediaStore URI individually, stops cleanly on API 29
     * RecoverableSecurityException so the UI can request user consent and retry remaining items,
     * and records exact succeeded vs failed items so freed bytes never over-report.
     */
    suspend fun deleteDevicePhotosLegacy(photos: List<PhotoItemEntity>): LegacyDeleteResult =
        withContext(Dispatchers.IO) {
            val deleted = mutableListOf<PhotoItemEntity>()
            val failed = mutableListOf<PhotoItemEntity>()
            var lastReason: String? = null
            val resolver = context.contentResolver

            for (index in photos.indices) {
                val photo = photos[index]
                val physicalUris = photo.physicalMediaUris
                if (photo.isSample || physicalUris.isEmpty() || physicalUris.any { !it.startsWith("content://") }) {
                    deleted += photo
                    continue
                }
                val uris = physicalUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
                if (uris.size != physicalUris.size) {
                    failed += photo
                    lastReason = "无效的文件路径"
                    continue
                }
                var deletedRows = 0
                var itemFailureReason: String? = null
                try {
                    for (uri in uris.distinct()) {
                        deletedRows += resolver.delete(uri, null, null)
                    }
                    if (deletedRows >= uris.distinct().size) {
                        deleted += photo
                    } else {
                        failed += photo
                        lastReason = "系统未完整删除该媒体组（可能已被移动或受保护）"
                    }
                } catch (sec: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
                        val remaining = photos.subList(index, photos.size)
                        return@withContext LegacyDeleteResult(
                            deletedPhotos = deleted,
                            failedPhotos = failed,
                            failureReason = "需要系统授权才能永久删除部分文件",
                            recoverableIntentSender = sec.userAction.actionIntent.intentSender,
                            remainingUnprocessedPhotos = remaining
                        )
                    } else {
                        itemFailureReason = "缺少删除权限，部分文件受系统保护"
                    }
                } catch (t: Throwable) {
                    itemFailureReason = t.localizedMessage?.take(48) ?: "文件删除失败"
                }
                if (itemFailureReason != null) {
                    failed += photo
                    lastReason = itemFailureReason
                }
            }

            LegacyDeleteResult(
                deletedPhotos = deleted,
                failedPhotos = failed,
                failureReason = if (failed.isNotEmpty()) lastReason ?: "部分文件未能删除" else null,
                recoverableIntentSender = null
            )
        }

    companion object {
        private const val MOTION_DETECT_CONCURRENCY = 8
        internal const val BURST_WINDOW_MS = 2_000L
        internal const val BURST_MAX_SPAN_MS = 12_000L
        private const val BURST_ASPECT_TOLERANCE = 0.15f
        private val ISO_6709_REGEX = Regex("""([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)""")
        private val GENERIC_CAMERA_BUCKETS = setOf(
            "camera", "dcim", "pictures", "screenshots", "download", "downloads",
            "weixin", "wechat", "qq", "photos", "movies", "video", "0"
        )

        private fun isBurstEligible(photo: PhotoItemEntity): Boolean {
            if (photo.isVideo) return false
            return photo.mediaSubtypeKind == MediaSubtype.CAMERA ||
                photo.mediaSubtypeKind == MediaSubtype.STANDARD
        }

        private fun aspectRatiosCompatible(a: PhotoItemEntity, b: PhotoItemEntity): Boolean {
            if (a.widthPx <= 0 || a.heightPx <= 0 || b.widthPx <= 0 || b.heightPx <= 0) return true
            val ra = a.widthPx.toFloat() / a.heightPx.toFloat()
            val rb = b.widthPx.toFloat() / b.heightPx.toFloat()
            return abs(ra - rb) <= BURST_ASPECT_TOLERANCE
        }

        internal fun groupBurstImages(images: List<PhotoItemEntity>): List<PhotoItemEntity> {
            if (images.size < 2) return images
            val sorted = images.sortedBy { it.takenAtMillis }
            val result = mutableListOf<PhotoItemEntity>()
            var i = 0
            while (i < sorted.size) {
                val anchor = sorted[i]
                if (!isBurstEligible(anchor)) {
                    result += anchor
                    i++
                    continue
                }
                var j = i + 1
                while (j < sorted.size) {
                    val cand = sorted[j]
                    val prev = sorted[j - 1]
                    val withinStep = abs(cand.takenAtMillis - prev.takenAtMillis) <= BURST_WINDOW_MS
                    val withinSpan = abs(cand.takenAtMillis - anchor.takenAtMillis) <= BURST_MAX_SPAN_MS
                    if (isBurstEligible(cand) &&
                        cand.bucketName == anchor.bucketName &&
                        withinStep &&
                        withinSpan &&
                        aspectRatiosCompatible(anchor, cand)
                    ) {
                        j++
                    } else {
                        break
                    }
                }
                val cluster = sorted.subList(i, j)
                if (cluster.size >= 2) {
                    val groupId = "burst_ms_${cluster.first().mediaStoreKey}"
                    val bestIdx = cluster.indices.maxByOrNull { cluster[it].qualityScore } ?: 0
                    cluster.forEachIndexed { idx, item ->
                        result += item.copy(
                            burstGroupId = groupId,
                            isBestInStack = idx == bestIdx
                        )
                    }
                } else {
                    result += anchor
                }
                i = j
            }
            return result
        }
    }
}
