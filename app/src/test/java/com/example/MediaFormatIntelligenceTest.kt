package com.example

import com.example.data.ImageFormat
import com.example.data.MediaSubtype
import com.example.data.MediaType
import com.example.data.MotionPhotoType
import com.example.data.PhotoItemEntity
import com.example.data.ScannedMediaItem
import com.example.data.classifyMediaSubtype
import com.example.data.isPotentiallyAnimatedImage
import com.example.data.isSafelyCompressibleImage
import com.example.data.pairLivePhotoItems
import com.example.data.resolveImageFormat
import com.example.data.scanEmbeddedVideoOffset
import com.example.data.xmpDeclaresVideoItem
import com.example.data.xmpIndicatesMotionPhoto
import com.example.ui.DescriptiveTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFormatIntelligenceTest {

    private fun photo(
        key: String,
        mediaType: MediaType = MediaType.IMAGE,
        displayBase: String = key,
        takenAt: Long = 1_700_000_000_000L,
        durationMs: Long = 0L,
        format: ImageFormat = ImageFormat.UNKNOWN,
        subtype: MediaSubtype = MediaSubtype.STANDARD,
        isAnimated: Boolean = false,
        motionType: MotionPhotoType = MotionPhotoType.NONE,
        motionVideoUri: String? = null
    ): PhotoItemEntity {
        return PhotoItemEntity(
            mediaStoreKey = key,
            mediaType = mediaType.name,
            durationMs = durationMs,
            mimeType = if (mediaType == MediaType.VIDEO) "video/quicktime" else null,
            imageFormat = format.name,
            mediaSubtype = subtype.name,
            isAnimated = isAnimated,
            motionPhotoType = motionType.name,
            motionVideoUri = motionVideoUri,
            motionVideoDurationMs = durationMs.takeIf { motionType != MotionPhotoType.NONE } ?: 0L,
            year = 2024,
            month = 5,
            day = 1,
            takenAtMillis = takenAt,
            fileSizeBytes = 2_000_000L,
            widthPx = 1200,
            heightPx = 900,
            contentUri = "content://media/$key",
            caption = displayBase,
            bucketName = "Camera",
            isSample = false
        )
    }

    @Test
    fun resolveImageFormat_usesMimeAndExtensions() {
        assertEquals(ImageFormat.JPEG, resolveImageFormat("image/jpeg", "a.bin"))
        assertEquals(ImageFormat.PNG, resolveImageFormat(null, "a.PNG"))
        assertEquals(ImageFormat.GIF, resolveImageFormat("image/gif", null))
        assertEquals(ImageFormat.WEBP, resolveImageFormat(null, "a.webp"))
        assertEquals(ImageFormat.HEIC, resolveImageFormat("image/heif", "a.heic"))
        assertEquals(ImageFormat.AVIF, resolveImageFormat(null, "a.avif"))
        assertEquals(ImageFormat.RAW, resolveImageFormat(null, "a.dng"))
        assertEquals(ImageFormat.UNKNOWN, resolveImageFormat(null, "a.bin"))
    }

    @Test
    fun animatedWebp_detectionRequiresAnimChunk() {
        val animated = ByteArray(128)
        "ANIM".encodeToByteArray().copyInto(animated, destinationOffset = 40)
        assertTrue(isPotentiallyAnimatedImage(ImageFormat.WEBP, animated))
        assertFalse(isPotentiallyAnimatedImage(ImageFormat.WEBP, ByteArray(128)))
        assertTrue(isPotentiallyAnimatedImage(ImageFormat.GIF, null))
    }

    @Test
    fun classifyMediaSubtype_detectsDeterministicTypeBuckets() {
        assertEquals(
            MediaSubtype.SCREENSHOT,
            classifyMediaSubtype("Screenshots", "Screenshot_2024.png", 1080, 2400, false)
        )
        assertEquals(
            MediaSubtype.LONG_SCREENSHOT,
            classifyMediaSubtype("Screenshots", "Screenshot_long.png", 900, 5000, false)
        )
        assertEquals(
            MediaSubtype.SCREEN_RECORD,
            classifyMediaSubtype("screen_record", "record.mp4", 1080, 2400, true)
        )
        assertEquals(
            MediaSubtype.DOCUMENT,
            classifyMediaSubtype("Scanner", "invoice_scan.jpg", 1200, 900, false)
        )
        assertEquals(
            MediaSubtype.CHAT_SAVE,
            classifyMediaSubtype("WeiXin", "mmexport.jpg", 1200, 900, false)
        )
        assertEquals(
            MediaSubtype.CAMERA,
            classifyMediaSubtype("Camera", "IMG_0001.jpg", 1200, 900, false)
        )
    }

    @Test
    fun xmpAndEmbeddedMotionScanner_detectsTrailingMp4Box() {
        val xmp = """
            <x:xmpmeta>
              <rdf:Description GCamera:MotionPhoto="1">
                <Container:Item Mime="video/mp4"/>
              </rdf:Description>
            </x:xmpmeta>
        """.trimIndent()
        assertTrue(xmpIndicatesMotionPhoto(xmp))
        assertTrue(xmpDeclaresVideoItem(xmp))

        val bytes = ByteArray(2_500)
        val boxStart = 1_800
        val declaredSize = bytes.size - boxStart
        bytes[boxStart] = (declaredSize ushr 24).toByte()
        bytes[boxStart + 1] = (declaredSize ushr 16).toByte()
        bytes[boxStart + 2] = (declaredSize ushr 8).toByte()
        bytes[boxStart + 3] = declaredSize.toByte()
        "ftyp".encodeToByteArray().copyInto(bytes, destinationOffset = boxStart + 4)
        assertEquals(boxStart.toLong(), scanEmbeddedVideoOffset(bytes))
        assertNull(scanEmbeddedVideoOffset(ByteArray(1_200)))
    }

    @Test
    fun livePhotoPairing_pairsSameBasenameStillAndShortVideo() {
        val image = photo("img_1", displayBase = "IMG_0001", format = ImageFormat.HEIC)
        val video = photo(
            "vid_1",
            mediaType = MediaType.VIDEO,
            displayBase = "IMG_0001",
            takenAt = image.takenAtMillis + 800L,
            durationMs = 2_700L
        )
        val result = pairLivePhotoItems(
            images = listOf(ScannedMediaItem(image, "IMG_0001.HEIC")),
            videos = listOf(ScannedMediaItem(video, "IMG_0001.MOV"))
        )

        assertEquals(setOf("vid_1"), result.consumedVideoKeys)
        val paired = result.images.single()
        assertEquals(MotionPhotoType.PAIRED, paired.motionPhotoKind)
        assertTrue(paired.isLivePhoto)
        assertEquals(video.contentUri, paired.motionVideoUri)
        assertEquals(listOf(image.contentUri, video.contentUri), paired.physicalMediaUris)
    }

    @Test
    fun livePhotoPairing_rejectsLongOrFarVideos() {
        val image = photo("img_1", displayBase = "IMG_0001", format = ImageFormat.HEIC)
        val longVideo = photo(
            "vid_long",
            mediaType = MediaType.VIDEO,
            displayBase = "IMG_0001",
            takenAt = image.takenAtMillis,
            durationMs = 9_000L
        )
        val farVideo = photo(
            "vid_far",
            mediaType = MediaType.VIDEO,
            displayBase = "IMG_0001",
            takenAt = image.takenAtMillis + 30_000L,
            durationMs = 2_000L
        )
        val result = pairLivePhotoItems(
            images = listOf(ScannedMediaItem(image, "IMG_0001.HEIC")),
            videos = listOf(
                ScannedMediaItem(longVideo, "IMG_0001.MOV"),
                ScannedMediaItem(farVideo, "IMG_0001.MOV")
            )
        )

        assertTrue(result.consumedVideoKeys.isEmpty())
        assertEquals(MotionPhotoType.NONE, result.images.single().motionPhotoKind)
    }

    @Test
    fun descriptiveTags_matchLiveTypeAndResolutionFacts() {
        val live = photo(
            "live",
            format = ImageFormat.HEIC,
            motionType = MotionPhotoType.PAIRED,
            motionVideoUri = "content://media/vid_live"
        )
        val screenshot = photo("shot", format = ImageFormat.PNG, subtype = MediaSubtype.SCREENSHOT)
        val uhd = photo(
            "video",
            mediaType = MediaType.VIDEO,
            durationMs = 4_000L
        ).copy(widthPx = 3840, heightPx = 2160)

        assertTrue(DescriptiveTag.LIVE_PHOTO.matches(live))
        assertTrue(DescriptiveTag.SCREENSHOT.matches(screenshot))
        assertTrue(DescriptiveTag.UHD_VIDEO.matches(uhd))
        assertFalse(DescriptiveTag.UHD_VIDEO.matches(uhd.copy(heightPx = 1080)))
    }

    @Test
    fun compressionProtection_skipsLiveAnimatedAndSpecialFormats() {
        val jpeg = photo("jpeg", format = ImageFormat.JPEG)
        val gif = photo("gif", format = ImageFormat.GIF, isAnimated = true)
        val heic = photo("heic", format = ImageFormat.HEIC)
        val live = photo(
            "live",
            format = ImageFormat.JPEG,
            motionType = MotionPhotoType.PAIRED,
            motionVideoUri = "content://media/vid_live"
        )

        assertTrue(isSafelyCompressibleImage(jpeg))
        assertFalse(isSafelyCompressibleImage(gif))
        assertFalse(isSafelyCompressibleImage(heic))
        assertFalse(isSafelyCompressibleImage(live))
    }
}
