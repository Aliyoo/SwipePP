package com.example

import com.example.data.LocalMediaScanner
import com.example.data.MediaSubtype
import com.example.data.MediaType
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.data.smarttag.SmartCategory
import com.example.data.smarttag.decodeEmbeddingFp16
import com.example.data.smarttag.encodeEmbeddingFp16
import com.example.data.smarttag.hasSmartContentFor
import com.example.data.smarttag.smartCategories
import com.example.ui.DescriptiveTag
import com.example.ui.GalleryEntry
import com.example.ui.ScoredSemanticCandidate
import com.example.ui.TimelineSearchQuery
import com.example.ui.extractDescriptiveKeywordTags
import com.example.ui.filterGalleryEntriesBySearch
import com.example.ui.selectSemanticMatchesByLift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SmartContentTaggingTest {

    private fun photo(
        id: Long,
        labels: String = "",
        caption: String = "",
        subtype: MediaSubtype = MediaSubtype.STANDARD,
        mediaType: String = MediaType.IMAGE.name,
        takenAtMillis: Long = 1_780_000_000_000L + id,
        widthPx: Int = 3000,
        heightPx: Int = 4000,
        bucketName: String? = "Camera"
    ) = PhotoItemEntity(
        id = id,
        mediaStoreKey = "img_$id",
        mediaType = mediaType,
        year = 2026, month = 6, day = 1,
        takenAtMillis = takenAtMillis,
        fileSizeBytes = 2_000_000L, widthPx = widthPx, heightPx = heightPx,
        status = PhotoStatus.UNREVIEWED.name,
        caption = caption,
        bucketName = bucketName,
        mediaSubtype = subtype.name,
        contentLabels = labels
    )

    @Test
    fun categoryKeysRoundTripCsv() {
        val p = photo(1, labels = "FOOD,DRINK,CITY")
        assertEquals(listOf(SmartCategory.FOOD, SmartCategory.DRINK, SmartCategory.CITY), p.smartCategories())
    }

    @Test
    fun hasSmartContentMatchesKeyAndZhLabel() {
        val p = photo(1, labels = "PET,SUNSET")
        assertTrue(p.hasSmartContentFor("PET"))
        assertTrue(p.hasSmartContentFor("宠物"))
        assertFalse(p.hasSmartContentFor("美食"))
    }

    @Test
    fun keywordChipMatchesContentLabels() {
        val entries = listOf(
            GalleryEntry.Single(photo(1, labels = "FOOD,DRINK", caption = "brunch")),
            GalleryEntry.Single(photo(2, labels = "CITY", caption = "street"))
        )
        val query = TimelineSearchQuery(selectedKeywordTags = setOf("美食"))
        val filtered = filterGalleryEntriesBySearch(entries, query)
        assertEquals(1, filtered.size)
        assertEquals(1L, (filtered.first() as GalleryEntry.Single).photo.id)
    }

    @Test
    fun categoryLevelSynonymsHitLexicallyWhileHyponymsDeferToSemanticSearch() {
        val entries = listOf(
            GalleryEntry.Single(photo(1, labels = "PET", caption = "IMG_001")),
            GalleryEntry.Single(photo(2, labels = "VEHICLE", caption = "IMG_002")),
            GalleryEntry.Single(photo(3, labels = "WATER", caption = "IMG_003"))
        )
        // Category-level synonyms hit immediately via contentLabels.
        val pets = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "宠物"))
        assertEquals(1, pets.size)
        assertEquals(1L, (pets.first() as GalleryEntry.Single).photo.id)

        val seascape = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "海景"))
        assertEquals(1, seascape.size)
        assertEquals(3L, (seascape.first() as GalleryEntry.Single).photo.id)

        // Mutually exclusive hyponyms (cat vs dog, airplane vs car) must NOT lexically match
        // a coarse bucket — otherwise searching "猫咪" returns every dog photo.
        assertTrue(filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "猫咪")).isEmpty())
        assertTrue(filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "飞机")).isEmpty())
        assertTrue(filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "上海")).isEmpty())
        assertTrue(filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "小猫熊")).isEmpty())
        // Single-char noise never fans out across categories.
        assertTrue(filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "物")).isEmpty())

        // With semantic lift for photo 1, "猫咪" hits only photo 1.
        val semanticCat = filterGalleryEntriesBySearch(
            entries,
            TimelineSearchQuery(keyword = "猫咪"),
            semanticScores = mapOf(1L to 0.068f)
        )
        assertEquals(1, semanticCat.size)
        assertEquals(1L, (semanticCat.first() as GalleryEntry.Single).photo.id)
    }

    @Test
    fun extractionSurfacesSmartCategoriesByFrequency() {
        val entries = listOf(
            GalleryEntry.Single(photo(1, labels = "FOOD", caption = "")),
            GalleryEntry.Single(photo(2, labels = "FOOD,CITY", caption = "")),
            GalleryEntry.Single(photo(3, labels = "CITY", caption = ""))
        )
        val tags = extractDescriptiveKeywordTags(entries, limit = 5)
        assertTrue(tags.contains("美食"))
        assertTrue(tags.contains("城市街巷"))
        assertEquals("美食", tags.first())
    }

    @Test
    fun screenshotDocumentAndSunsetWarmToneDescriptiveTags() {
        val shot = photo(1, subtype = MediaSubtype.SCREENSHOT)
        val longShot = photo(2, subtype = MediaSubtype.LONG_SCREENSHOT)
        val docBySubtype = photo(3, subtype = MediaSubtype.DOCUMENT)
        val docByContent = photo(4, labels = "DOCUMENT,CITY")
        val sunsetByContent = photo(5, labels = "SUNSET,WATER")
        val plain = photo(6)

        assertTrue(DescriptiveTag.SCREENSHOT.matches(shot))
        assertTrue(DescriptiveTag.LONG_SCREENSHOT.matches(longShot))
        assertFalse(DescriptiveTag.SCREENSHOT.matches(plain))
        assertTrue(DescriptiveTag.DOCUMENT.matches(docBySubtype))
        assertTrue(DescriptiveTag.DOCUMENT.matches(docByContent))
        assertFalse(DescriptiveTag.DOCUMENT.matches(plain))
        assertTrue(DescriptiveTag.WARM_TONE.matches(sunsetByContent))
        assertFalse(DescriptiveTag.WARM_TONE.matches(plain))
    }

    @Test
    fun embeddingFp16RoundTripKeepsPrecision() {
        val original = FloatArray(1024) { i -> (i % 37) * 0.01f - 0.18f }
        val bytes = encodeEmbeddingFp16(original)
        assertEquals(2048, bytes.size)
        val decoded = decodeEmbeddingFp16(bytes)
        assertEquals(1024, decoded.size)
        for (i in original.indices) {
            assertTrue("idx $i diff ${abs(original[i] - decoded[i])}", abs(original[i] - decoded[i]) < 0.001f)
        }
    }

    @Test
    fun mlKitLabelHintMappingIsTolerant() {
        assertTrue(SmartCategory.PET.matchesMlKitLabel("Dog"))
        assertTrue(SmartCategory.FOOD.matchesMlKitLabel("Food"))
        assertTrue(SmartCategory.WATER.matchesMlKitLabel("Sea"))
        assertFalse(SmartCategory.PET.matchesMlKitLabel("Catering"))
        assertFalse(SmartCategory.FOOD.matchesMlKitLabel("Footwear"))
    }

    @Test
    fun videoEntriesWithContentLabelsAreSearchable() {
        val video = photo(7, labels = "WATER,SUNSET", mediaType = MediaType.VIDEO.name)
        val entries = listOf(GalleryEntry.Single(video))
        val filtered = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "海景"))
        assertEquals(1, filtered.size)
    }

    @Test
    fun debiasedLiftScoringRejectsHighHubnessDocumentsAndRanksTrueHitsFirst() {
        // Simulates a gallery containing a high-Hubness white document (raw=0.468, lift=+0.008)
        // when searching for an absent concept ("猫咪"): must return emptyMap.
        val absentQuery = listOf(
            ScoredSemanticCandidate(photoId = 10L, lift = 0.008f, rawCosine = 0.468f),
            ScoredSemanticCandidate(photoId = 11L, lift = 0.003f, rawCosine = 0.455f),
            ScoredSemanticCandidate(photoId = 12L, lift = -0.004f, rawCosine = 0.440f),
            ScoredSemanticCandidate(photoId = 13L, lift = -0.008f, rawCosine = 0.431f),
            ScoredSemanticCandidate(photoId = 14L, lift = -0.012f, rawCosine = 0.425f),
            ScoredSemanticCandidate(photoId = 15L, lift = -0.015f, rawCosine = 0.418f)
        )
        assertTrue(selectSemanticMatchesByLift(absentQuery).isEmpty())

        // Simulates a genuine query ("咖啡"): two strong coffee photos + high-Hubness document noise.
        val coffeeQuery = listOf(
            ScoredSemanticCandidate(photoId = 1L, lift = 0.078f, rawCosine = 0.515f),
            ScoredSemanticCandidate(photoId = 2L, lift = 0.053f, rawCosine = 0.492f),
            ScoredSemanticCandidate(photoId = 3L, lift = 0.011f, rawCosine = 0.465f), // doc_page noise
            ScoredSemanticCandidate(photoId = 4L, lift = -0.006f, rawCosine = 0.440f),
            ScoredSemanticCandidate(photoId = 5L, lift = -0.014f, rawCosine = 0.432f),
            ScoredSemanticCandidate(photoId = 6L, lift = -0.020f, rawCosine = 0.421f)
        )
        val matched = selectSemanticMatchesByLift(coffeeQuery)
        assertEquals(listOf(1L, 2L), matched.keys.toList())
    }

    @Test
    fun tightenedBurstGroupingPreventsBundlingScreenshotsChatSavesOrSlowShots() {
        val base = 1_780_000_000_000L
        val cam1 = photo(1, subtype = MediaSubtype.CAMERA, takenAtMillis = base, widthPx = 4000, heightPx = 3000)
        val cam2 = photo(2, subtype = MediaSubtype.CAMERA, takenAtMillis = base + 1_200L, widthPx = 4000, heightPx = 3000)
        val cam3Slow = photo(3, subtype = MediaSubtype.CAMERA, takenAtMillis = base + 6_500L, widthPx = 4000, heightPx = 3000)
        val cam4Portrait = photo(4, subtype = MediaSubtype.CAMERA, takenAtMillis = base + 7_200L, widthPx = 3000, heightPx = 4000)
        val chat1 = photo(5, subtype = MediaSubtype.CHAT_SAVE, takenAtMillis = base + 20_000L, bucketName = "WeChat")
        val chat2 = photo(6, subtype = MediaSubtype.CHAT_SAVE, takenAtMillis = base + 20_500L, bucketName = "WeChat")

        val grouped = LocalMediaScanner.groupBurstImages(listOf(cam1, cam2, cam3Slow, cam4Portrait, chat1, chat2))
            .associateBy { it.id }

        // cam1 and cam2 (1.2s apart, same aspect ratio) form one burst
        assertTrue(grouped.getValue(1).burstGroupId != null)
        assertEquals(grouped.getValue(1).burstGroupId, grouped.getValue(2).burstGroupId)
        // cam3Slow (5.3s gap) and cam4Portrait (aspect mismatch) stay separate singles
        assertNull(grouped.getValue(3).burstGroupId)
        assertNull(grouped.getValue(4).burstGroupId)
        // WeChat saved images never form a burst even 0.5s apart
        assertNull(grouped.getValue(5).burstGroupId)
        assertNull(grouped.getValue(6).burstGroupId)
    }
}
