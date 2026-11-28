package com.example

import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.MonthPageModel
import com.example.ui.domain.LibraryDerivationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class AnnualTreasureGroupsTest {
    private fun photo(
        id: Long,
        month: Int,
        favorite: Boolean,
        favoritedAt: Long?,
        takenAt: Long,
        status: PhotoStatus = PhotoStatus.KEPT
    ) = PhotoItemEntity(
        id = id,
        year = 2026,
        month = month,
        day = 1,
        takenAtMillis = takenAt,
        fileSizeBytes = 1_000L,
        widthPx = 100,
        heightPx = 100,
        status = status.name,
        isFavorite = favorite,
        favoritedAtMillis = favoritedAt,
        isSample = false
    )

    private fun page(month: Int, photos: List<PhotoItemEntity>) = MonthPageModel(
        year = 2026,
        month = month,
        photos = photos,
        photosByDay = emptyMap(),
        cleanedDays = emptySet(),
        totalPhotos = photos.size,
        videoCount = 0,
        unreviewedCount = 0,
        keptCount = photos.size,
        trashedCount = 0,
        burstGroupCount = 0,
        reclaimableBytes = 0L,
        stamp = null,
        coverPhoto = photos.firstOrNull()
    )

    @Test
    fun annualTreasureGroupsFilterAndSortRecentCuratedFirst() {
        val legacy = photo(1L, 6, favorite = true, favoritedAt = null, takenAt = 100L)
        val olderCurated = photo(2L, 6, favorite = true, favoritedAt = 20L, takenAt = 200L)
        val newerCurated = photo(3L, 6, favorite = true, favoritedAt = 30L, takenAt = 150L)
        val notFavorite = photo(4L, 6, favorite = false, favoritedAt = null, takenAt = 300L)
        val trashedFavorite = photo(5L, 6, favorite = true, favoritedAt = 40L, takenAt = 400L, status = PhotoStatus.TRASHED)
        val julyTreasure = photo(6L, 7, favorite = true, favoritedAt = 10L, takenAt = 100L)

        val groups = LibraryDerivationEngine.buildAnnualTreasureGroups(
            listOf(
                page(6, listOf(legacy, olderCurated, newerCurated, notFavorite, trashedFavorite)),
                page(7, listOf(julyTreasure)),
                page(8, listOf(notFavorite.copy(id = 7L, month = 8)))
            )
        )

        assertEquals(listOf(6, 7), groups.map { it.month })
        assertEquals(listOf(3L, 2L, 1L), groups.first().photos.map { it.id })
        assertEquals(listOf(6L), groups[1].photos.map { it.id })
    }
    @Test
    fun yearbookFavoriteCountExcludesTrashedFavorites() {
        val keptFavorite = photo(1L, 6, favorite = true, favoritedAt = 20L, takenAt = 100L)
        val trashedFavorite = photo(2L, 6, favorite = true, favoritedAt = 30L, takenAt = 200L, status = PhotoStatus.TRASHED)
        val plain = photo(3L, 6, favorite = false, favoritedAt = null, takenAt = 300L)

        val yearbook = LibraryDerivationEngine.buildYearbooksLinear(
            listOf(keptFavorite, trashedFavorite, plain),
            emptyMap(),
            emptyMap()
        ).single()

        assertEquals(1, yearbook.favoriteCount)
    }
}
