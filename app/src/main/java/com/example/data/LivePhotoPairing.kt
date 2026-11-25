package com.example.data

import kotlin.math.abs

internal data class ScannedMediaItem(
    val photo: PhotoItemEntity,
    val displayName: String
)

internal data class LivePhotoPairingResult(
    val images: List<PhotoItemEntity>,
    val consumedVideoKeys: Set<String>
)

/**
 * Pairs exported iOS-style Live Photos (`IMG_0001.HEIC` + `IMG_0001.MOV`) into one logical
 * image item. The paired video is intentionally not indexed as a standalone gallery video.
 */
internal fun pairLivePhotoItems(
    images: List<ScannedMediaItem>,
    videos: List<ScannedMediaItem>
): LivePhotoPairingResult {
    if (images.isEmpty() || videos.isEmpty()) {
        return LivePhotoPairingResult(images.map { it.photo }, emptySet())
    }

    val videosByBase = HashMap<String, MutableList<ScannedMediaItem>>()
    for (video in videos) {
        val base = normalizedPairBaseName(video.displayName)
        if (base.isNotBlank()) {
            videosByBase.getOrPut(base) { mutableListOf() }.add(video)
        }
    }

    val consumedVideoKeys = HashSet<String>()
    val pairedImages = ArrayList<PhotoItemEntity>(images.size)

    for (imageItem in images) {
        val image = imageItem.photo
        val format = image.imageFormatKind
        val base = normalizedPairBaseName(imageItem.displayName)
        val candidates = videosByBase[base].orEmpty().filter { videoItem ->
            val video = videoItem.photo
            video.mediaStoreKey !in consumedVideoKeys &&
                video.durationMs in 1L..MAX_LIVE_VIDEO_DURATION_MS &&
                bucketMatches(image.bucketName, video.bucketName) &&
                abs(video.takenAtMillis - image.takenAtMillis) <= MAX_LIVE_PAIR_TIME_DELTA_MS &&
                aspectRatioMatches(image, video)
        }
        val pairedVideo = candidates.minByOrNull {
            abs(it.photo.takenAtMillis - image.takenAtMillis)
        }?.photo

        if (pairedVideo != null && format in LIVE_STILL_FORMATS) {
            pairedVideo.mediaStoreKey?.let { consumedVideoKeys.add(it) }
            pairedImages += image.copy(
                isAnimated = true,
                motionPhotoType = MotionPhotoType.PAIRED.name,
                motionVideoUri = pairedVideo.contentUri,
                motionVideoDurationMs = pairedVideo.durationMs,
                motionVideoOffset = null,
                motionVideoLength = null,
                pairedMediaStoreKey = pairedVideo.mediaStoreKey
            )
        } else {
            pairedImages += image
        }
    }

    return LivePhotoPairingResult(
        images = pairedImages,
        consumedVideoKeys = consumedVideoKeys
    )
}

private fun aspectRatioMatches(image: PhotoItemEntity, video: PhotoItemEntity): Boolean {
    if (image.widthPx <= 0 || image.heightPx <= 0 || video.widthPx <= 0 || video.heightPx <= 0) return false
    val a = image.widthPx.toFloat() / image.heightPx
    val b = video.widthPx.toFloat() / video.heightPx
    return abs(a - b) / a < 0.03f
}

private fun bucketMatches(imageBucket: String?, videoBucket: String?): Boolean {
    val image = imageBucket?.trim().orEmpty()
    val video = videoBucket?.trim().orEmpty()
    return image.isEmpty() || video.isEmpty() || image.equals(video, ignoreCase = true)
}

private val LIVE_STILL_FORMATS = setOf(
    ImageFormat.JPEG,
    ImageFormat.HEIC,
    ImageFormat.AVIF,
    ImageFormat.PNG
)

private const val MAX_LIVE_PAIR_TIME_DELTA_MS = 5_000L
private const val MAX_LIVE_VIDEO_DURATION_MS = 8_000L
