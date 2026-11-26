package com.example

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
import com.example.ui.TimelineSearchQuery
import com.example.ui.extractDescriptiveKeywordTags
import com.example.ui.filterGalleryEntriesBySearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SmartContentTaggingTest {

    private fun photo(
        id: Long,
        labels: String = "",
        caption: String = "",
        subtype: MediaSubtype = MediaSubtype.STANDARD,
        mediaType: String = MediaType.IMAGE.name
    ) = PhotoItemEntity(
        id = id,
        mediaType = mediaType,
        year = 2026, month = 6, day = 1,
        takenAtMillis = 1_780_000_000_000L + id,
        fileSizeBytes = 2_000_000L, widthPx = 3000, heightPx = 4000,
        status = PhotoStatus.UNREVIEWED.name,
        caption = caption,
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
    fun freeTextTokenHitsSmartAliases() {
        val entries = listOf(
            GalleryEntry.Single(photo(1, labels = "PET", caption = "周末")),
            GalleryEntry.Single(photo(2, labels = "SNOW", caption = "周末"))
        )
        val filtered = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "猫"))
        assertEquals(1, filtered.size)
        assertEquals(1L, (filtered.first() as GalleryEntry.Single).photo.id)
    }

    @Test
    fun placeNameTokenDoesNotHitShortAliases() {
        val entries = listOf(
            GalleryEntry.Single(photo(1, labels = "WATER", caption = "")),
            GalleryEntry.Single(photo(2, labels = "CITY", caption = ""))
        )
        // "上海" must not match WATER through its single-char alias "海".
        val filtered = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "上海"))
        assertTrue(filtered.isEmpty())
        // Multi-char aliases still match by containment.
        val beach = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "海滩"))
        assertEquals(1, beach.size)
        assertEquals(1L, (beach.first() as GalleryEntry.Single).photo.id)
    }

    @Test
    fun compoundPetQueriesHitPetTags() {
        val entries = listOf(
            GalleryEntry.Single(photo(1, labels = "PET", caption = "")),
            GalleryEntry.Single(photo(2, labels = "CITY", caption = ""))
        )
        val cats = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "猫咪"))
        assertEquals(1, cats.size)
        val dogs = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "狗狗"))
        assertEquals(1, dogs.size)
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
        assertEquals("美食", tags.first()) // 2 occurrences beats city 2 via insertion order? both 2: food first seen
    }

    @Test
    fun screenshotAndDocumentDescriptiveTags() {
        val shot = photo(1, subtype = MediaSubtype.SCREENSHOT)
        val longShot = photo(2, subtype = MediaSubtype.LONG_SCREENSHOT)
        val docBySubtype = photo(3, subtype = MediaSubtype.DOCUMENT)
        val docByContent = photo(4, labels = "DOCUMENT,CITY")
        val plain = photo(5)

        assertTrue(DescriptiveTag.SCREENSHOT.matches(shot))
        assertTrue(DescriptiveTag.LONG_SCREENSHOT.matches(longShot))
        assertFalse(DescriptiveTag.SCREENSHOT.matches(plain))
        assertTrue(DescriptiveTag.DOCUMENT.matches(docBySubtype))
        assertTrue(DescriptiveTag.DOCUMENT.matches(docByContent))
        assertFalse(DescriptiveTag.DOCUMENT.matches(plain))
    }

    @Test
    fun embeddingFp16RoundTripKeepsPrecision() {
        val original = FloatArray(512) { i -> (i % 37) * 0.01f - 0.18f }
        val bytes = encodeEmbeddingFp16(original)
        assertEquals(1024, bytes.size)
        val decoded = decodeEmbeddingFp16(bytes)
        assertEquals(512, decoded.size)
        for (i in original.indices) {
            assertTrue("idx $i diff ${abs(original[i] - decoded[i])}", abs(original[i] - decoded[i]) < 0.001f)
        }
    }

    @Test
    fun mlKitLabelHintMappingIsTolerant() {
        assertTrue(SmartCategory.PET.matchesMlKitLabel("Dog"))
        assertTrue(SmartCategory.FOOD.matchesMlKitLabel("Food"))
        assertTrue(SmartCategory.WATER.matchesMlKitLabel("Sea"))
        assertFalse(SmartCategory.PET.matchesMlKitLabel("Catering")) // substring trap: 'cat' inside word
        assertFalse(SmartCategory.FOOD.matchesMlKitLabel("Footwear"))
    }

    @Test
    fun videoEntriesWithContentLabelsAreSearchable() {
        val video = photo(7, labels = "WATER,SUNSET", mediaType = MediaType.VIDEO.name)
        val entries = listOf(GalleryEntry.Single(video))
        val filtered = filterGalleryEntriesBySearch(entries, TimelineSearchQuery(keyword = "海"))
        assertEquals(1, filtered.size)
    }
}
