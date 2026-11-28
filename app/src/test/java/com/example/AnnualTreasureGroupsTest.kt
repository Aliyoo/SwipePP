package com.example

import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.domain.LibraryDerivationEngine
import com.example.ui.theme.TimelineSortOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AnnualTreasureGroupsTest {
    private fun takenAt(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun photo(
        id: Long,
        month: Int,
        favorite: Boolean,
        favoritedAt: Long?,
        takenAt: Long,
        status: PhotoStatus = PhotoStatus.KEPT,
        year: Int = 2026,
        day: Int = 1
    ) = PhotoItemEntity(
        id = id,
        year = year,
        month = month,
        day = day,
        takenAtMillis = takenAt,
        fileSizeBytes = 1_000L,
        widthPx = 100,
        heightPx = 100,
        status = status.name,
        isFavorite = favorite,
        favoritedAtMillis = favoritedAt,
        isSample = false
    )

    @Test
    fun annualTreasureGroupsFilterAndSortByCaptureTime() {
        val legacy = photo(1L, 6, favorite = true, favoritedAt = null, takenAt = takenAt(2026, 6, 1))
        val olderCurated = photo(2L, 6, favorite = true, favoritedAt = 90L, takenAt = takenAt(2026, 6, 2))
        val newerCurated = photo(3L, 6, favorite = true, favoritedAt = 10L, takenAt = takenAt(2026, 6, 3))
        val notFavorite = photo(4L, 6, favorite = false, favoritedAt = null, takenAt = takenAt(2026, 6, 4))
        val trashedFavorite = photo(
            5L,
            6,
            favorite = true,
            favoritedAt = 40L,
            takenAt = takenAt(2026, 6, 5),
            status = PhotoStatus.TRASHED
        )
        val julyTreasure = photo(6L, 7, favorite = true, favoritedAt = 10L, takenAt = takenAt(2026, 7, 1))

        val groups = LibraryDerivationEngine.buildAnnualTreasureGroups(
            year = 2026,
            allPhotos = listOf(legacy, olderCurated, newerCurated, notFavorite, trashedFavorite, julyTreasure)
        )

        assertEquals(listOf(7, 6), groups.map { it.month })
        assertEquals(listOf(6L), groups[0].photos.map { it.id })
        assertEquals(listOf(3L, 2L, 1L), groups[1].photos.map { it.id })
    }

    @Test
    fun annualTreasureGroupsUseCaptureTimestampInsteadOfStoredScanMonth() {
        val storedAsDecember2025 = photo(
            id = 1L,
            year = 2025,
            month = 12,
            day = 31,
            favorite = true,
            favoritedAt = 20L,
            takenAt = takenAt(2026, 6, 8)
        )
        val actualDecember2026 = photo(
            id = 2L,
            year = 2026,
            month = 12,
            favorite = true,
            favoritedAt = 30L,
            takenAt = takenAt(2026, 12, 1)
        )
        val actual2025 = photo(
            id = 3L,
            year = 2026,
            month = 6,
            favorite = true,
            favoritedAt = 40L,
            takenAt = takenAt(2025, 6, 1)
        )

        val groups = LibraryDerivationEngine.buildAnnualTreasureGroups(
            year = 2026,
            allPhotos = listOf(storedAsDecember2025, actualDecember2026, actual2025)
        )

        assertEquals(listOf(12, 6), groups.map { it.month })
        assertEquals(listOf(2L), groups[0].photos.map { it.id })
        assertEquals(listOf(1L), groups[1].photos.map { it.id })
    }

    @Test
    fun annualTreasureGroupsRespectTimelineMonthOrder() {
        val june = photo(1L, 6, favorite = true, favoritedAt = 20L, takenAt = takenAt(2026, 6, 1))
        val july = photo(2L, 7, favorite = true, favoritedAt = 30L, takenAt = takenAt(2026, 7, 1))

        val oldestFirst = LibraryDerivationEngine.buildAnnualTreasureGroups(
            year = 2026,
            allPhotos = listOf(june, july),
            sortOrder = TimelineSortOrder.OLDEST_FIRST
        )

        assertEquals(listOf(6, 7), oldestFirst.map { it.month })
    }

    @Test
    fun yearbookFavoriteCountExcludesTrashedFavorites() {
        val keptFavorite = photo(1L, 6, favorite = true, favoritedAt = 20L, takenAt = takenAt(2026, 6, 1))
        val trashedFavorite = photo(
            2L,
            6,
            favorite = true,
            favoritedAt = 30L,
            takenAt = takenAt(2026, 6, 2),
            status = PhotoStatus.TRASHED
        )
        val plain = photo(3L, 6, favorite = false, favoritedAt = null, takenAt = takenAt(2026, 6, 3))

        val yearbook = LibraryDerivationEngine.buildYearbooksLinear(
            listOf(keptFavorite, trashedFavorite, plain),
            emptyMap(),
            emptyMap()
        ).single()

        assertEquals(1, yearbook.favoriteCount)
    }
}
