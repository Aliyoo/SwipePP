package com.example

import com.example.data.MediaType
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.DateRangeFilter
import com.example.ui.DescriptiveTag
import com.example.ui.GalleryEntry
import com.example.ui.SEARCH_UNLOCATED_TOKEN
import com.example.ui.TimelineSearchQuery
import com.example.ui.extractDescriptiveKeywordTags
import com.example.ui.filterGalleryEntriesBySearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineSearchFilterTest {

    private fun sampleEntries(): List<GalleryEntry> {
        val kyotoBest = PhotoItemEntity(
            id = 1L,
            year = 2026, month = 10, day = 18,
            takenAtMillis = 1_792_300_000_000L,
            fileSizeBytes = 5_400_000L, widthPx = 4032, heightPx = 3024,
            status = PhotoStatus.UNREVIEWED.name,
            isFavorite = true, burstGroupId = "burst_kyoto",
            qualityScore = 96, isBestInStack = true, visualVariant = "CRISP",
            caption = "祇园黄昏灯笼与红叶 · 最佳构图",
            bucketName = "旅行日记",
            locationName = "京都 · 祇园", latitude = 35.0037, longitude = 135.7786
        )
        val kyotoDark = PhotoItemEntity(
            id = 2L,
            year = 2026, month = 10, day = 18,
            takenAtMillis = 1_792_300_002_000L,
            fileSizeBytes = 4_900_000L, widthPx = 4032, heightPx = 3024,
            status = PhotoStatus.UNREVIEWED.name,
            burstGroupId = "burst_kyoto",
            qualityScore = 64, visualVariant = "DARK_EXPOSURE",
            caption = "祇园黄昏连拍 #2 · 欠曝偏暗",
            bucketName = "旅行日记",
            locationName = "京都 · 祇园", latitude = 35.0037, longitude = 135.7786
        )
        val tokyoVideo = PhotoItemEntity(
            id = 3L,
            mediaType = MediaType.VIDEO.name, durationMs = 24_000L,
            year = 2026, month = 5, day = 12,
            takenAtMillis = 1_778_560_000_000L,
            fileSizeBytes = 48_000_000L, widthPx = 3840, heightPx = 2160,
            status = PhotoStatus.KEPT.name,
            qualityScore = 92, visualVariant = "CRISP",
            caption = "4K 60fps · 东京雨后手冲拉花实拍",
            bucketName = "日常咖啡",
            locationName = "东京 · 表参道", latitude = 35.6652, longitude = 139.7121
        )
        val unlocatedScreenshot = PhotoItemEntity(
            id = 4L,
            year = 2025, month = 11, day = 8,
            takenAtMillis = 1_762_580_000_000L,
            fileSizeBytes = 1_200_000L, widthPx = 1080, heightPx = 2400,
            status = PhotoStatus.UNREVIEWED.name,
            qualityScore = 80, visualVariant = "CRISP",
            caption = "行程攻略截图",
            bucketName = "Screenshots",
            locationName = null, latitude = null, longitude = null
        )
        return listOf(
            GalleryEntry.Burst("burst_kyoto", listOf(kyotoBest, kyotoDark), kyotoBest),
            GalleryEntry.Single(tokyoVideo),
            GalleryEntry.Single(unlocatedScreenshot)
        )
    }

    @Test
    fun filterByDateRange_narrowsEntriesAccurately() {
        val entries = sampleEntries()
        val range = DateRangeFilter(
            startDateKey = DateRangeFilter.encodeDateKey(2026, 5, 1),
            endDateKey = DateRangeFilter.encodeDateKey(2026, 6, 30)
        )
        val result = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(dateRange = range)
        )
        assertEquals(1, result.size)
        assertEquals(3L, (result[0] as GalleryEntry.Single).photo.id)
    }

    @Test
    fun filterByLocationAndUnlocatedToken_supportsMultiSelect() {
        val entries = sampleEntries()
        val result = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(selectedLocations = setOf("京都 · 祇园", SEARCH_UNLOCATED_TOKEN))
        )
        assertEquals(2, result.size)
        assertTrue(result[0] is GalleryEntry.Burst)
        assertEquals(4L, (result[1] as GalleryEntry.Single).photo.id)
    }

    @Test
    fun filterByDescriptiveTagsAndKeywordTags_combinesCriteria() {
        val entries = sampleEntries()
        val videoResult = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(selectedTags = setOf(DescriptiveTag.VIDEO))
        )
        assertEquals(1, videoResult.size)
        assertEquals(3L, (videoResult[0] as GalleryEntry.Single).photo.id)

        val portraitResult = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(selectedTags = setOf(DescriptiveTag.PORTRAIT))
        )
        assertEquals(1, portraitResult.size)
        assertEquals(4L, (portraitResult[0] as GalleryEntry.Single).photo.id)

        val sceneResult = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(selectedKeywordTags = setOf("红叶"))
        )
        assertEquals(1, sceneResult.size)
        assertTrue(sceneResult[0] is GalleryEntry.Burst)
    }

    @Test
    fun freeTextMultiTokenSearch_matchesDateLocationCaptionAndTagAlias() {
        val entries = sampleEntries()
        val result = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(keyword = "2026-10 京都 红叶 收藏")
        )
        assertEquals(1, result.size)
        assertTrue(result[0] is GalleryEntry.Burst)

        val videoAlias = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(keyword = "2026年5月 视频")
        )
        assertEquals(1, videoAlias.size)
        assertEquals(3L, (videoAlias[0] as GalleryEntry.Single).photo.id)
    }

    @Test
    fun extractDescriptiveKeywordTags_findsRealSceneDescriptors() {
        val tags = extractDescriptiveKeywordTags(sampleEntries())
        assertTrue("Expected '黄昏' or '红叶' in extracted tags: $tags", "黄昏" in tags && "红叶" in tags)
        assertTrue("Expected '4K' in extracted tags: $tags", "4K" in tags)
    }

    @Test
    fun dateRangeUtcRoundTrip_preservesExactCalendarDay() {
        val key = DateRangeFilter.encodeDateKey(2026, 10, 18)
        val utc = DateRangeFilter.dateKeyToUtcMillis(key)
        assertEquals(key, DateRangeFilter.utcMillisToDateKey(utc))
    }
}
