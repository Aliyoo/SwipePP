package com.example

import com.example.data.CompressionPreset
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.inferSameDayLocations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchMediaEditorTest {

    private fun makePhoto(
        id: Long,
        takenAtMillis: Long,
        locationName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        status: PhotoStatus = PhotoStatus.UNREVIEWED
    ): PhotoItemEntity = PhotoItemEntity(
        id = id,
        year = 2026,
        month = 5,
        day = 12,
        takenAtMillis = takenAtMillis,
        fileSizeBytes = 4_500_000L,
        widthPx = 4032,
        heightPx = 3024,
        status = status.name,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude
    )

    @Test
    fun inferSameDayLocations_picksChronologicallyClosestGeotaggedNeighbor() {
        val base = 1_778_560_000_000L
        val hour = 3_600_000L

        val hangzhou = makePhoto(1L, base - 5 * hour, "杭州", 30.2741, 120.1551)
        val shanghai = makePhoto(2L, base + 2 * hour, "上海", 31.2304, 121.4737)
        val missing = makePhoto(3L, base)

        val matches = inferSameDayLocations(
            targets = listOf(missing),
            allPhotos = listOf(hangzhou, shanghai, missing),
            maxWindowHours = 24
        )

        assertEquals(1, matches.size)
        assertEquals(3L, matches[0].photo.id)
        assertEquals("上海", matches[0].matchedLocationName)
        assertEquals(2 * hour, matches[0].deltaMillis)
    }

    @Test
    fun inferSameDayLocations_ignoresPhotosOutside24HourWindowOrTrashed() {
        val base = 1_778_560_000_000L
        val hour = 3_600_000L

        val tooFar = makePhoto(1L, base - 30 * hour, "北京", 39.9042, 116.4074)
        val trashedClose = makePhoto(2L, base + 1 * hour, "成都", 30.5728, 104.0668, PhotoStatus.TRASHED)
        val nullIsland = makePhoto(3L, base + 2 * hour, "未知", 0.0, 0.0)
        val missing = makePhoto(4L, base)

        val matches = inferSameDayLocations(
            targets = listOf(missing),
            allPhotos = listOf(tooFar, trashedClose, nullIsland, missing),
            maxWindowHours = 24
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun compressionPresets_areMonotonicInQualityAndSavings() {
        val nearLossless = CompressionPreset.NEAR_LOSSLESS
        val balanced = CompressionPreset.BALANCED
        val compact = CompressionPreset.COMPACT

        assertTrue(nearLossless.jpegQuality > balanced.jpegQuality)
        assertTrue(balanced.jpegQuality > compact.jpegQuality)
        assertTrue(nearLossless.maxLongEdgePx > balanced.maxLongEdgePx)
        assertTrue(balanced.maxLongEdgePx > compact.maxLongEdgePx)
        assertTrue(nearLossless.estimatedSavingsRatio < balanced.estimatedSavingsRatio)
        assertTrue(balanced.estimatedSavingsRatio < compact.estimatedSavingsRatio)
    }
}
