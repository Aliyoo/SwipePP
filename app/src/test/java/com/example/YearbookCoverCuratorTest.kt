package com.example

import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.YearbookCoverConfigEntity
import com.example.data.YearbookCoverMode
import com.example.ui.YearbookCoverCurator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YearbookCoverCuratorTest {

    private fun photo(
        id: Long,
        month: Int = 5,
        day: Int = 10,
        takenAtMillis: Long = id * 1_000_000L,
        widthPx: Int = 3000,
        heightPx: Int = 4000,
        qualityScore: Int = 88,
        isFavorite: Boolean = false,
        status: PhotoStatus = PhotoStatus.UNREVIEWED,
        bucketName: String? = "Camera",
        locationName: String? = null,
        burstGroupId: String? = null,
        isBestInStack: Boolean = false,
        caption: String = ""
    ) = PhotoItemEntity(
        id = id,
        year = 2025,
        month = month,
        day = day,
        takenAtMillis = takenAtMillis,
        fileSizeBytes = 3_200_000L,
        widthPx = widthPx,
        heightPx = heightPx,
        status = status.name,
        isFavorite = isFavorite,
        burstGroupId = burstGroupId,
        qualityScore = qualityScore,
        isBestInStack = isBestInStack,
        caption = caption,
        bucketName = bucketName,
        locationName = locationName
    )

    @Test
    fun smartCuration_demotesLatestScreenshotAndElevatesHighQualityCameraPhoto() {
        val latestScreenshot = photo(
            id = 100,
            month = 12,
            day = 28,
            takenAtMillis = 999_000_000L,
            widthPx = 1080,
            heightPx = 3200, // ultra-tall screenshot
            qualityScore = 85,
            bucketName = "Screenshots",
            caption = "Screenshot_20251228"
        )
        val kyotoPortrait = photo(
            id = 42,
            month = 4,
            day = 6,
            takenAtMillis = 400_000_000L,
            widthPx = 3024,
            heightPx = 4032, // 3:4 portrait monograph match
            qualityScore = 95,
            isFavorite = true,
            status = PhotoStatus.KEPT,
            bucketName = "Camera",
            locationName = "京都岚山"
        )

        assertTrue(YearbookCoverCurator.isLikelyScreenshotOrUtility(latestScreenshot))
        assertFalse(YearbookCoverCurator.isLikelyScreenshotOrUtility(kyotoPortrait))

        val slides = YearbookCoverCurator.curateSmartCoverSlides(listOf(latestScreenshot, kyotoPortrait))
        assertEquals(1, slides.size)
        assertEquals(kyotoPortrait.id, slides.first().id)
    }

    @Test
    fun coverRankingPrioritizesExplicitTreasureOverHeuristicQuality() {
        val treasured = photo(
            id = 1,
            qualityScore = 55,
            isFavorite = true,
            status = PhotoStatus.KEPT,
            locationName = null
        )
        val highQuality = photo(
            id = 2,
            qualityScore = 98,
            isFavorite = false,
            status = PhotoStatus.KEPT,
            locationName = "京都"
        )

        val ranked = YearbookCoverCurator.rankPhotosForCoverPicker(listOf(highQuality, treasured))

        assertEquals(1L, ranked.first().id)
    }

    @Test
    fun coverRankingDoesNotLetTreasuredScreenshotsDisplacePhotography() {
        val treasuredScreenshot = photo(
            id = 1,
            widthPx = 1080,
            heightPx = 3200,
            qualityScore = 95,
            isFavorite = true,
            status = PhotoStatus.KEPT,
            bucketName = "Screenshots",
            caption = "Screenshot_20251128"
        )
        val cameraPhoto = photo(
            id = 2,
            qualityScore = 90,
            isFavorite = false,
            status = PhotoStatus.KEPT,
            bucketName = "Camera",
            locationName = "京都"
        )

        val ranked = YearbookCoverCurator.rankPhotosForCoverPicker(listOf(treasuredScreenshot, cameraPhoto))

        assertEquals(2L, ranked.first().id)
    }

    @Test
    fun smartCuration_enforcesMonthDiversityAndBurstDeduplication() {
        val photos = listOf(
            // Burst stack in May — should pick at most 1
            photo(id = 1, month = 5, day = 1, qualityScore = 94, burstGroupId = "burst_may", isBestInStack = true),
            photo(id = 2, month = 5, day = 1, qualityScore = 92, burstGroupId = "burst_may"),
            photo(id = 3, month = 5, day = 1, qualityScore = 91, burstGroupId = "burst_may"),
            // Distinct months across the year
            photo(id = 4, month = 1, day = 15, qualityScore = 89, locationName = "北海道"),
            photo(id = 5, month = 8, day = 20, qualityScore = 90, locationName = "青岛"),
            photo(id = 6, month = 10, day = 3, qualityScore = 96, isFavorite = true, locationName = "京都")
        )

        val slides = YearbookCoverCurator.curateSmartCoverSlides(photos, maxSlides = 4)
        assertEquals(4, slides.size)
        // Highest-scoring favorite hero is anchored first
        assertEquals(6L, slides.first().id)
        // All 4 distinct months represented and only 1 from burst_may
        assertEquals(setOf(1, 5, 8, 10), slides.map { it.month }.toSet())
        assertEquals(1, slides.count { it.burstGroupId == "burst_may" })
    }

    @Test
    fun smartCurationExhaustsTreasurePoolBeforeNonTreasureDiversity() {
        val treasured = (1..6).map { index ->
            photo(
                id = index.toLong(),
                month = if (index <= 3) 4 else 9,
                day = index,
                qualityScore = 80 + index,
                isFavorite = true,
                status = PhotoStatus.KEPT,
                bucketName = "Camera"
            )
        }
        val diverseNonTreasures = listOf(
            photo(id = 101, month = 1, qualityScore = 99, status = PhotoStatus.KEPT, locationName = "北海道"),
            photo(id = 102, month = 2, qualityScore = 98, status = PhotoStatus.KEPT, locationName = "青岛"),
            photo(id = 103, month = 3, qualityScore = 97, status = PhotoStatus.KEPT, locationName = "京都"),
            photo(id = 104, month = 5, qualityScore = 96, status = PhotoStatus.KEPT, locationName = "厦门")
        )

        val slides = YearbookCoverCurator.curateSmartCoverSlides(treasured + diverseNonTreasures)

        assertEquals(6, slides.size)
        assertEquals(treasured.map { it.id }.toSet(), slides.map { it.id }.toSet())
        assertTrue(slides.all { it.isFavorite })
    }

    @Test
    fun smartCurationBackfillsNonTreasuresOnlyWhenTreasurePoolIsInsufficient() {
        val treasured = listOf(
            photo(id = 1, month = 4, day = 1, qualityScore = 88, isFavorite = true, status = PhotoStatus.KEPT),
            photo(id = 2, month = 9, day = 2, qualityScore = 86, isFavorite = true, status = PhotoStatus.KEPT)
        )
        val nonTreasures = listOf(
            photo(id = 101, month = 1, qualityScore = 99, status = PhotoStatus.KEPT, locationName = "北海道"),
            photo(id = 102, month = 2, qualityScore = 98, status = PhotoStatus.KEPT, locationName = "青岛"),
            photo(id = 103, month = 3, qualityScore = 97, status = PhotoStatus.KEPT, locationName = "京都"),
            photo(id = 104, month = 5, qualityScore = 96, status = PhotoStatus.KEPT, locationName = "厦门")
        )

        val slides = YearbookCoverCurator.curateSmartCoverSlides(treasured + nonTreasures, maxSlides = 4)

        assertEquals(4, slides.size)
        assertEquals(listOf(1L, 2L), slides.take(2).map { it.id })
        assertEquals(2, slides.count { it.isFavorite })
        assertEquals(setOf(4, 9), slides.filter { it.isFavorite }.map { it.month }.toSet())
    }

    @Test
    fun resolveCover_respectsSingleCustomAndFallsBackWhenTrashed() {
        val p1 = photo(id = 10, month = 3, qualityScore = 95, isFavorite = true)
        val p2 = photo(id = 20, month = 7, qualityScore = 82)
        val singleConfig = YearbookCoverConfigEntity(
            year = 2025,
            mode = YearbookCoverMode.SINGLE_CUSTOM.name,
            selectedPhotoIdsCsv = "20",
            intervalSeconds = 5
        )

        val resolved = YearbookCoverCurator.resolveCoverPresentation(listOf(p1, p2), singleConfig)
        assertEquals(YearbookCoverMode.SINGLE_CUSTOM, resolved.mode)
        assertEquals(listOf(20L), resolved.slides.map { it.id })
        assertEquals(20L, resolved.coverPhoto?.id)

        // When pinned photo is moved to TRASHED, gracefully fall back to Smart Carousel
        val trashedP2 = p2.copy(status = PhotoStatus.TRASHED.name)
        val fallback = YearbookCoverCurator.resolveCoverPresentation(listOf(p1, trashedP2), singleConfig)
        assertEquals(YearbookCoverMode.SMART_CAROUSEL, fallback.mode)
        assertEquals(10L, fallback.coverPhoto?.id)
        assertNotEquals(20L, fallback.coverPhoto?.id)
    }

    @Test
    fun resolveCover_preservesCustomCarouselOrderAndCadence() {
        val photos = listOf(
            photo(id = 1, month = 1),
            photo(id = 2, month = 6),
            photo(id = 3, month = 11)
        )
        val customConfig = YearbookCoverConfigEntity(
            year = 2025,
            mode = YearbookCoverMode.CUSTOM_CAROUSEL.name,
            selectedPhotoIdsCsv = "3,1,2",
            intervalSeconds = 8
        )

        val resolved = YearbookCoverCurator.resolveCoverPresentation(photos, customConfig)
        assertEquals(YearbookCoverMode.CUSTOM_CAROUSEL, resolved.mode)
        assertEquals(listOf(3L, 1L, 2L), resolved.slides.map { it.id })
        assertEquals(8, resolved.intervalSeconds)
    }
}
