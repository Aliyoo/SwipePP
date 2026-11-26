package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

private val RAW_EXTENSIONS = setOf(
    "dng", "cr2", "cr3", "nef", "nrw", "arw", "orf", "rw2", "raf", "pef", "srw"
)

fun resolveImageFormat(mimeType: String?, displayName: String?): ImageFormat {
    val mime = mimeType?.lowercase(Locale.ROOT).orEmpty()
    val ext = displayName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
    return when {
        mime == "image/jpeg" || mime == "image/jpg" || ext in setOf("jpg", "jpeg") -> ImageFormat.JPEG
        mime == "image/png" || ext == "png" -> ImageFormat.PNG
        mime == "image/gif" || ext == "gif" -> ImageFormat.GIF
        mime == "image/webp" || ext == "webp" -> ImageFormat.WEBP
        mime == "image/heic" || mime == "image/heif" || ext in setOf("heic", "heif") -> ImageFormat.HEIC
        mime == "image/avif" || ext == "avif" -> ImageFormat.AVIF
        mime.contains("dng") || mime.contains("raw") || ext in RAW_EXTENSIONS -> ImageFormat.RAW
        else -> ImageFormat.UNKNOWN
    }
}

fun isPotentiallyAnimatedImage(format: ImageFormat, headerBytes: ByteArray? = null): Boolean {
    return when (format) {
        ImageFormat.GIF -> true
        ImageFormat.WEBP -> headerBytes?.let { bytes ->
            val end = minOf(bytes.size - 4, 96)
            if (end <= 0) {
                false
            } else {
                var found = false
                for (i in 0..end) {
                    if (bytes[i] == 'A'.code.toByte() && i + 3 < bytes.size &&
                        bytes[i + 1] == 'N'.code.toByte() &&
                        bytes[i + 2] == 'I'.code.toByte() &&
                        bytes[i + 3] == 'M'.code.toByte()
                    ) {
                        found = true
                        break
                    }
                }
                found
            }
        } == true
        else -> false
    }
}

fun classifyMediaSubtype(
    bucketName: String?,
    displayName: String?,
    widthPx: Int,
    heightPx: Int,
    isVideo: Boolean
): MediaSubtype {
    val bucket = bucketName?.lowercase(Locale.ROOT).orEmpty()
    val name = displayName?.lowercase(Locale.ROOT).orEmpty()
    val haystack = "$bucket/$name"
    val ratio = if (widthPx > 0 && heightPx > 0) widthPx.toFloat() / heightPx.toFloat() else 1f

    fun containsAny(keywords: List<String>) = keywords.any { it in haystack }

    return when {
        isVideo && containsAny(listOf("screen_record", "screenrecord", "录屏")) -> MediaSubtype.SCREEN_RECORD
        containsAny(listOf("screenshot", "screenshots", "截屏", "截图")) && ratio < 0.45f -> MediaSubtype.LONG_SCREENSHOT
        containsAny(listOf("screenshot", "screenshots", "截屏", "截图")) -> MediaSubtype.SCREENSHOT
        containsAny(
            listOf(
                "document", "documents", "scan", "scanner", "office", "receipt", "invoice",
                "发票", "票据", "文档", "扫描", "二维码", "qrcode"
            )
        ) -> MediaSubtype.DOCUMENT
        containsAny(listOf("wechat", "weixin", "micromsg", "qq", "tim", "telegram", "whatsapp", "微信")) -> MediaSubtype.CHAT_SAVE
        containsAny(listOf("download", "downloads", "cache", "temp", "tmp", "下载")) -> MediaSubtype.DOWNLOAD
        containsAny(listOf("camera", "dcim", "相机")) -> MediaSubtype.CAMERA
        else -> MediaSubtype.STANDARD
    }
}

fun normalizedPairBaseName(displayName: String?): String {
    return displayName
        .orEmpty()
        .substringBeforeLast('.')
        .trim()
        .lowercase(Locale.ROOT)
}

fun isMotionPhotoFilenameCandidate(displayName: String?): Boolean {
    val name = displayName.orEmpty()
    val base = name.substringBeforeLast('.').lowercase(Locale.ROOT)
    return base.endsWith("mp") ||
        base.startsWith("mvimg") ||
        base.contains("motion_photo") ||
        base.contains("motionphoto") ||
        base.contains("live_photo") ||
        base.contains("livephoto")
}

internal fun xmpIndicatesMotionPhoto(xmpText: String): Boolean {
    val attributeForm = Regex(
        "(?i)(?:Camera|GCamera):MotionPhoto\\s*=\\s*['\"]1['\"]"
    )
    val elementForm = Regex(
        "(?i)<\\s*(?:Camera|GCamera):MotionPhoto\\s*>\\s*1\\s*<\\s*/\\s*(?:Camera|GCamera):MotionPhoto\\s*>"
    )
    return attributeForm.containsMatchIn(xmpText) || elementForm.containsMatchIn(xmpText)
}

internal fun xmpDeclaresVideoItem(xmpText: String): Boolean {
    return xmpText.contains("video/mp4", ignoreCase = true) ||
        xmpText.contains("video/quicktime", ignoreCase = true)
}

internal fun scanEmbeddedVideoOffset(bytes: ByteArray): Long? {
    if (bytes.size < 2_048) return null
    var i = 1_024
    while (i < bytes.size - 16) {
        if (bytes[i] == 'f'.code.toByte() &&
            bytes[i + 1] == 't'.code.toByte() &&
            bytes[i + 2] == 'y'.code.toByte() &&
            bytes[i + 3] == 'p'.code.toByte()
        ) {
            val boxStart = i - 4
            if (boxStart >= 1_024) {
                val declaredSize = readBigEndianInt(bytes, boxStart)
                val remaining = bytes.size - boxStart
                if (declaredSize in 8..remaining) {
                    return boxStart.toLong()
                }
            }
        }
        i++
    }
    return null
}

private fun readBigEndianInt(bytes: ByteArray, offset: Int): Int {
    return ((bytes[offset].toInt() and 0xff) shl 24) or
        ((bytes[offset + 1].toInt() and 0xff) shl 16) or
        ((bytes[offset + 2].toInt() and 0xff) shl 8) or
        (bytes[offset + 3].toInt() and 0xff)
}

data class EmbeddedMotionInfo(
    val videoOffset: Long,
    val videoLength: Long
)

class EmbeddedMotionPhotoDetector(private val context: Context) {

    fun detect(
        contentUri: String,
        mimeType: String?,
        displayName: String?,
        fileSizeBytes: Long
    ): EmbeddedMotionInfo? {
        val format = resolveImageFormat(mimeType, displayName)
        if (format !in setOf(ImageFormat.JPEG, ImageFormat.HEIC, ImageFormat.AVIF)) return null
        if (!isMotionPhotoFilenameCandidate(displayName)) return null
        if (fileSizeBytes <= 0L || fileSizeBytes > MAX_MOTION_CONTAINER_BYTES) return null

        val uri = runCatching { Uri.parse(contentUri) }.getOrNull() ?: return null
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null

        val xmpText = String(bytes.copyOfRange(0, minOf(bytes.size, 256 * 1024)), Charsets.UTF_8)
        val xmpConfirmed = xmpIndicatesMotionPhoto(xmpText) && xmpDeclaresVideoItem(xmpText)
        val offset = scanEmbeddedVideoOffset(bytes) ?: return null
        val length = bytes.size - offset
        if (length < 32L) return null

        // Official readers may use the strict *MP.jpg filename pattern, but XMP can be absent on
        // older Samsung/Xiaomi files. Accept positive XMP or the strict filename plus a confirmed trailing MP4 box.
        val strictMotionFilename = displayName.orEmpty().substringBeforeLast('.').lowercase(Locale.ROOT).endsWith("mp")
        if (!xmpConfirmed && !strictMotionFilename) return null
        return EmbeddedMotionInfo(videoOffset = offset, videoLength = length)
    }

    companion object {
        private const val MAX_MOTION_CONTAINER_BYTES = 96L * 1024L * 1024L
    }
}

object MotionPhotoPlaybackResolver {

    fun resolvePlaybackUri(context: Context, photo: PhotoItemEntity): String? {
        if (!photo.isLivePhoto) return null
        return when (photo.motionPhotoKind) {
            MotionPhotoType.PAIRED -> photo.motionVideoUri
            MotionPhotoType.EMBEDDED -> extractEmbeddedVideo(context, photo)
            MotionPhotoType.NONE -> null
        }
    }

    private fun extractEmbeddedVideo(context: Context, photo: PhotoItemEntity): String? {
        val sourceUriString = photo.contentUri ?: return null
        val sourceUri = runCatching { Uri.parse(sourceUriString) }.getOrNull() ?: return null
        val resolver = context.contentResolver
        val bytes = runCatching {
            resolver.openInputStream(sourceUri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        val offset = photo.motionVideoOffset?.takeIf { it in 1..bytes.size.toLong() }
            ?: scanEmbeddedVideoOffset(bytes)
            ?: return null
        val length = (photo.motionVideoLength ?: (bytes.size - offset)).coerceAtMost(bytes.size - offset)
        if (offset < 0L || length < 32L || offset + length > bytes.size) return null

        val key = sha256("$sourceUriString|$offset|$length").take(24)
        val dir = File(context.cacheDir, "motion_photo_videos").apply { mkdirs() }
        val outFile = File(dir, "motion_$key.mp4")
        if (!outFile.exists() || outFile.length() != length) {
            val tmp = File(dir, "motion_$key.tmp")
            runCatching {
                FileOutputStream(tmp).use { out ->
                    out.write(bytes, offset.toInt(), length.toInt())
                    out.flush()
                }
            }.getOrElse {
                tmp.delete()
                return null
            }
            if (!tmp.renameTo(outFile)) {
                tmp.delete()
                return null
            }
            pruneMotionVideoCache(dir)
        }
        return Uri.fromFile(outFile).toString()
    }

    private fun pruneMotionVideoCache(dir: File, maxBytes: Long = 256L * 1024L * 1024L) {
        val files = dir.listFiles { file -> file.isFile && file.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        var total = 0L
        for (file in files) {
            total += file.length()
            if (total > maxBytes) {
                runCatching { file.delete() }
            }
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
