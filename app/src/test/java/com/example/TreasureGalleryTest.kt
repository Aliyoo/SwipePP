package com.example

import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.GalleryEntry
import com.example.ui.components.GallerySmartFilter
import com.example.ui.components.buildTimelineClusterLayout
import com.example.ui.screens.filterGalleryEntriesBySmartAndAlbum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreasureGalleryTest {
    private fun photo(id: Long, favorite: Boolean = false, month: Int = 1) = PhotoItemEntity(
        id = id,
        year = 2026,
        month = month,
        day = 10,
        takenAtMillis = id,
        fileSizeBytes = 1_000L,
        widthPx = 1000,
        heightPx = 1000,
        status = PhotoStatus.KEPT.name,
        isFavorite = favorite
    )

    @Test
    fun treasuredSmartFilterIncludesSinglesAndBurstsContainingFavorites() {
        val favoriteSingle = GalleryEntry.Single(photo(1, favorite = true))
        val plainSingle = GalleryEntry.Single(photo(2))
        val burstFavorite = photo(3, favorite = true)
        val burst = GalleryEntry.Burst(
            groupId = "burst",
            photos = listOf(burstFavorite, photo(4)),
            bestPhoto = burstFavorite
        )

        val filtered = filterGalleryEntriesBySmartAndAlbum(
            allEntries = listOf(favoriteSingle, plainSingle, burst),
            smartFilter = GallerySmartFilter.TREASURED,
            selectedAlbum = null
        )

        assertEquals(listOf(favoriteSingle, burst), filtered)
    }

    @Test
    fun monthlyClusterCountsTreasuredMedia() {
        val favorite = GalleryEntry.Single(photo(1, favorite = true))
        val plain = GalleryEntry.Single(photo(2))

        val layout = buildTimelineClusterLayout(
            entries = listOf(favorite, plain),
            monthStamps = emptyMap()
        )

        val cluster = layout.clusters.single()
        assertEquals(1, cluster.favoriteCount)
        assertTrue(cluster.compactSummaryText.contains("珍藏 1"))
    }
}
