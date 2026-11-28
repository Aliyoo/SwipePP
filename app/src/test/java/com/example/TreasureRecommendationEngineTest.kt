package com.example

import com.example.data.MediaSubtype
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.domain.TreasureRecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TreasureRecommendationEngineTest {
    private fun takenAt(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        set(year, month - 1, day, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun photo(
        id: Long,
        favorite: Boolean = false,
        quality: Int = 88,
        labels: String = "",
        status: PhotoStatus = PhotoStatus.KEPT,
        burstGroupId: String? = null,
        best: Boolean = false,
        bucket: String = "Camera",
        caption: String = "photo$id",
        subtype: MediaSubtype = MediaSubtype.CAMERA,
        day: Int = id.toInt().coerceIn(1, 27)
    ) = PhotoItemEntity(
        id = id,
        year = 2026,
        month = 6,
        day = day,
        takenAtMillis = takenAt(2026, 6, day),
        fileSizeBytes = 2_000_000L,
        widthPx = 3000,
        heightPx = 4000,
        mediaSubtype = subtype.name,
        status = status.name,
        isFavorite = favorite,
        qualityScore = quality,
        contentLabels = labels,
        burstGroupId = burstGroupId,
        isBestInStack = best,
        bucketName = bucket,
        caption = caption,
        isSample = false
    )

    @Test
    fun recommendationsExcludeExistingTreasureTrashUtilityAndLowQuality() {
        val input = listOf(
            photo(1, favorite = true),
            photo(2, status = PhotoStatus.TRASHED),
            photo(3, bucket = "Screenshots", caption = "Screenshot_20260603"),
            photo(4, quality = 40),
            photo(5, quality = 91),
            photo(6, quality = 95, subtype = MediaSubtype.DOCUMENT, day = 6),
            photo(7, quality = 94, subtype = MediaSubtype.DOWNLOAD, day = 7)
        )

        val result = TreasureRecommendationEngine.recommend(2026, input)

        assertEquals(listOf(5L), result.map { it.photo.id })
    }

    @Test
    fun recommendationsBoostSharedSemanticLabelsOnlyAfterTreasureCorpus() {
        val treasures = (1..5).map {
            photo(it.toLong(), favorite = true, labels = "NATURE,SUNSET", day = it)
        }
        val similar = photo(20, quality = 82, labels = "NATURE,SKY", day = 20)
        val unrelatedHighQuality = photo(21, quality = 95, labels = "FOOD", day = 21)

        val semanticOn = TreasureRecommendationEngine.recommend(2026, treasures + similar + unrelatedHighQuality)
        assertEquals(20L, semanticOn.first().photo.id)
        assertTrue(semanticOn.first().reason.contains("山野自然"))

        val fewerTreasures = treasures.dropLast(1)
        val semanticOff = TreasureRecommendationEngine.recommend(2026, fewerTreasures + similar + unrelatedHighQuality)
        assertEquals(21L, semanticOff.first().photo.id)
    }

    @Test
    fun recommendationsDeduplicateBurstAndCapSameDay() {
        val input = listOf(
            photo(1, quality = 91, burstGroupId = "b1", best = true, day = 1),
            photo(2, quality = 95, burstGroupId = "b1", day = 1),
            photo(3, quality = 90, day = 2),
            photo(4, quality = 89, day = 2),
            photo(5, quality = 88, day = 2),
            photo(6, quality = 87, day = 3)
        )

        val result = TreasureRecommendationEngine.recommend(2026, input, maxRecommendations = 9)

        assertEquals(1, result.count { it.photo.burstGroupId == "b1" })
        assertTrue(result.count { it.photo.day == 2 } <= 2)
    }
}
