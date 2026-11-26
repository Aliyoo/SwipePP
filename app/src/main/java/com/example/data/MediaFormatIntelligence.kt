package com.example.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
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
        "(?i)(?:[A-Za-z0-9]+:)?(?:MotionPhoto|MicroVideo)\\s*=\\s*['\"]1['\"]"
    )
    val elementForm = Regex(
        "(?i)<\\s*(?:[A-Za-z0-9]+:)?(?:MotionPhoto|MicroVideo)\\s*>\\s*1\\s*<\\s*/\\s*(?:[A-Za-z0-9]+:)?(?:MotionPhoto|MicroVideo)\\s*>"
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

private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int {
    return (bytes[offset].toInt() and 0xff) or
        ((bytes[offset + 1].toInt() and 0xff) shl 8) or
        ((bytes[offset + 2].toInt() and 0xff) shl 16) or
        ((bytes[offset + 3].toInt() and 0xff) shl 24)
}

private fun indexOfAscii(bytes: ByteArray, needle: String, startIndex: Int): Int {
    val target = needle.encodeToByteArray()
    if (target.isEmpty() || startIndex >= bytes.size) return -1
    var i = startIndex.coerceAtLeast(0)
    while (i <= bytes.size - target.size) {
        var matched = true
        for (j in target.indices) {
            if (bytes[i + j] != target[j]) {
                matched = false
                break
            }
        }
        if (matched) return i
        i++
    }
    return -1
}

private fun lastIndexOfAscii(bytes: ByteArray, needle: String): Int {
    val target = needle.encodeToByteArray()
    if (target.isEmpty() || bytes.size < target.size) return -1
    var i = bytes.size - target.size
    while (i >= 0) {
        var matched = true
        for (j in target.indices) {
            if (bytes[i + j] != target[j]) {
                matched = false
                break
            }
        }
        if (matched) return i
        i--
    }
    return -1
}

/**
 * Parses Samsung's SEF footer from the last few KB. This is the important cheap
 * detection path for One UI files: their camera filenames do not carry Google's
 * *MP.jpg hint, and the `MotionPhoto_Data` marker may be several megabytes before EOF.
 */
internal fun scanSamsungFooterInfo(tailBytes: ByteArray, fileSizeBytes: Long): EmbeddedMotionInfo? {
    if (tailBytes.size < 32 || fileSizeBytes < tailBytes.size) return null
    if (String(tailBytes.copyOfRange(tailBytes.size - 4, tailBytes.size), Charsets.US_ASCII) != "SEFT") return null

    val indexLength = readLittleEndianInt(tailBytes, tailBytes.size - 8)
    if (indexLength < 16 || indexLength + 8 > tailBytes.size) return null
    val footerStartInTail = tailBytes.size - indexLength - 8
    if (footerStartInTail < 0 || footerStartInTail + 12 > tailBytes.size) return null
    if (String(tailBytes.copyOfRange(footerStartInTail, footerStartInTail + 4), Charsets.US_ASCII) != "SEFH") return null

    val footerAbsoluteStart = fileSizeBytes - tailBytes.size + footerStartInTail
    val fieldCount = readLittleEndianInt(tailBytes, footerStartInTail + 8)
    if (fieldCount <= 0 || fieldCount > 32) return null
    var entryOffset = footerStartInTail + 12
    repeat(fieldCount) {
        if (entryOffset + 12 > tailBytes.size) return null
        val isMotionData = tailBytes[entryOffset] == 0x00.toByte() &&
            tailBytes[entryOffset + 1] == 0x00.toByte() &&
            tailBytes[entryOffset + 2] == 0x30.toByte() &&
            tailBytes[entryOffset + 3] == 0x0a.toByte()
        if (isMotionData) {
            val offsetBack = readLittleEndianInt(tailBytes, entryOffset + 4).toLong()
            val blockLength = readLittleEndianInt(tailBytes, entryOffset + 8).toLong()
            val blockStart = footerAbsoluteStart - offsetBack
            val headerLength = 8L + "MotionPhoto_Data".encodeToByteArray().size
            val payloadStart = blockStart + headerLength
            val payloadLength = blockLength - headerLength
            val tailAbsoluteStart = fileSizeBytes - tailBytes.size
            val payloadStartInTail = payloadStart - tailAbsoluteStart

            if (payloadLength == 12L && payloadStartInTail >= 0L && payloadStartInTail + 12L <= tailBytes.size) {
                val pointerOffset = payloadStartInTail.toInt()
                val isMpv2Pointer = tailBytes[pointerOffset] == 'm'.code.toByte() &&
                    tailBytes[pointerOffset + 1] == 'p'.code.toByte() &&
                    tailBytes[pointerOffset + 2] == 'v'.code.toByte() &&
                    tailBytes[pointerOffset + 3] == '2'.code.toByte()
                if (isMpv2Pointer) {
                    val videoOffset = readBigEndianInt(tailBytes, pointerOffset + 4).toLong() and 0xffffffffL
                    val videoLength = readBigEndianInt(tailBytes, pointerOffset + 8).toLong() and 0xffffffffL
                    if (videoOffset > 0L && videoLength >= 32L && videoOffset + videoLength <= fileSizeBytes) {
                        return EmbeddedMotionInfo(videoOffset = videoOffset, videoLength = videoLength)
                    }
                }
            }

            if (blockStart >= 0L && payloadLength >= 32L && payloadStart + payloadLength <= fileSizeBytes) {
                return EmbeddedMotionInfo(videoOffset = payloadStart, videoLength = payloadLength)
            }
        }
        entryOffset += 12
    }
    return null
}

/**
 * Locates Samsung's proprietary JPEG trailer. Samsung writes a `MotionPhoto_Data`
 * field followed by the raw MP4 payload, then an `SEFH` validation block. Unlike
 * Google's concatenated format, relying on XMP lengths is unreliable on newer One UI versions.
 */
internal fun scanSamsungMotionPhotoInfo(tailBytes: ByteArray, fileSizeBytes: Long): EmbeddedMotionInfo? {
    if (tailBytes.size < 2_048 || fileSizeBytes < tailBytes.size) return null
    val markerIndex = lastIndexOfAscii(tailBytes, "MotionPhoto_Data")
    if (markerIndex < 0) return null

    val payloadStart = markerIndex + "MotionPhoto_Data".encodeToByteArray().size
    val searchEnd = minOf(payloadStart + 96, tailBytes.size - 4)
    var videoStart = -1
    var i = payloadStart
    while (i < searchEnd) {
        if (tailBytes[i] == 'f'.code.toByte() &&
            tailBytes[i + 1] == 't'.code.toByte() &&
            tailBytes[i + 2] == 'y'.code.toByte() &&
            tailBytes[i + 3] == 'p'.code.toByte()
        ) {
            videoStart = i - 4
            break
        }
        i++
    }
    if (videoStart < payloadStart || videoStart + 16 >= tailBytes.size) return null

    val sefIndex = indexOfAscii(tailBytes, "SEFH", videoStart + 16)
    if (sefIndex <= videoStart) return null
    val length = sefIndex - videoStart
    if (length < 32) return null
    val declaredSize = readBigEndianInt(tailBytes, videoStart)
    if (declaredSize !in 8..length) return null

    val tailAbsoluteStart = fileSizeBytes - tailBytes.size
    return EmbeddedMotionInfo(
        videoOffset = tailAbsoluteStart + videoStart,
        videoLength = length.toLong()
    )
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
        if (fileSizeBytes <= 0L || fileSizeBytes > MAX_MOTION_CONTAINER_BYTES) return null

        val strictMotionFilename = isMotionPhotoFilenameCandidate(displayName)
        val uri = runCatching { Uri.parse(contentUri) }.getOrNull() ?: return null
        readTailBytes(uri, fileSizeBytes, FOOTER_SCAN_BYTES)?.let { footerBytes ->
            scanSamsungFooterInfo(footerBytes, fileSizeBytes)?.let { info ->
                if (hasMp4HeaderAt(uri, info.videoOffset)) return info
            }
        }

        val headBytes = readPrefixBytes(uri, XMP_SCAN_BYTES) ?: return null
        if (headBytes.isEmpty()) return null
        val xmpText = String(headBytes, Charsets.UTF_8)
        val xmpIndicatesMotion = xmpIndicatesMotionPhoto(xmpText)

        // Samsung's normal camera filenames (for example 20251126_091500.jpg) do not
        // follow the official *MP.jpg hint, so positive XMP must be enough to scan the trailer.
        if (!xmpIndicatesMotion && !strictMotionFilename) return null

        val tailBytes = readTailBytes(uri, fileSizeBytes, TAIL_SCAN_BYTES) ?: return null
        if (format == ImageFormat.JPEG) {
            scanSamsungMotionPhotoInfo(tailBytes, fileSizeBytes)?.let { return it }
        }

        val relativeOffset = scanEmbeddedVideoOffset(tailBytes)
        val tailAbsoluteStart = fileSizeBytes - tailBytes.size
        val offset = relativeOffset?.let { tailAbsoluteStart + it }
            ?: scanEmbeddedVideoOffsetForward(uri, fileSizeBytes)
            ?: return null
        val length = fileSizeBytes - offset
        if (offset <= 0L || length < 32L) return null
        return EmbeddedMotionInfo(videoOffset = offset, videoLength = length)
    }

    private fun readPrefixBytes(uri: Uri, maxBytes: Int): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArray(maxBytes)
                var offset = 0
                while (offset < maxBytes) {
                    val count = input.read(out, offset, maxBytes - offset)
                    if (count < 0) break
                    if (count == 0) continue
                    offset += count
                }
                if (offset == 0) byteArrayOf() else out.copyOf(offset)
            }
        }.getOrNull()
    }

    private fun scanEmbeddedVideoOffsetForward(uri: Uri, fileSizeBytes: Long): Long? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val chunk = ByteArray(256 * 1024)
                var bytesRead = 0L
                var carry = byteArrayOf()
                while (true) {
                    val count = input.read(chunk)
                    if (count < 0) break
                    if (count == 0) continue
                    val combined = carry + chunk.copyOf(count)
                    val combinedStart = bytesRead - carry.size
                    var index = 0
                    while (index < combined.size - 12) {
                        if (combined[index] == 'f'.code.toByte() &&
                            combined[index + 1] == 't'.code.toByte() &&
                            combined[index + 2] == 'y'.code.toByte() &&
                            combined[index + 3] == 'p'.code.toByte() &&
                            index >= 4
                        ) {
                            val boxStart = index - 4
                            val absoluteBoxStart = combinedStart + boxStart
                            if (absoluteBoxStart >= 1_024L) {
                                val declaredSize = readBigEndianInt(combined, boxStart).toLong() and 0xffffffffL
                                val remaining = fileSizeBytes - absoluteBoxStart
                                if (declaredSize in 8L..remaining) {
                                    return@use absoluteBoxStart
                                }
                            }
                        }
                        index++
                    }
                    carry = combined.copyOfRange((combined.size - 16).coerceAtLeast(0), combined.size)
                    bytesRead += count
                }
                null
            }
        }.getOrNull()
    }

    private fun hasMp4HeaderAt(uri: Uri, offset: Long): Boolean {
        val bytes = readRangeBytes(uri, offset, 16) ?: return false
        return bytes.size >= 8 &&
            bytes[4] == 'f'.code.toByte() &&
            bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() &&
            bytes[7] == 'p'.code.toByte()
    }

    private fun readRangeBytes(uri: Uri, offset: Long, length: Int): ByteArray? {
        return runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@runCatching null
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                val channel = input.channel
                channel.position(offset)
                val buffer = java.nio.ByteBuffer.allocate(length)
                while (buffer.hasRemaining()) {
                    val count = channel.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                }
                buffer.flip()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                bytes
            }
        }.getOrNull()
    }

    private fun readTailBytes(uri: Uri, fileSizeBytes: Long, maxBytes: Long): ByteArray? {
        return runCatching {
            val length = minOf(fileSizeBytes, maxBytes).toInt()
            if (length <= 0) return@runCatching null
            val start = fileSizeBytes - length
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@runCatching null
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                val channel = input.channel
                channel.position(start)
                val buffer = java.nio.ByteBuffer.allocate(length)
                while (buffer.hasRemaining()) {
                    val count = channel.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                }
                buffer.flip()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                bytes
            }
        }.getOrNull()
    }

    companion object {
        private const val MAX_MOTION_CONTAINER_BYTES = 96L * 1024L * 1024L
        private const val XMP_SCAN_BYTES = 256 * 1024
        private const val FOOTER_SCAN_BYTES = 8L * 1024L
        private const val TAIL_SCAN_BYTES = 48L * 1024L * 1024L
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
