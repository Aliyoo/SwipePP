package com.example

import com.example.data.DeviceMediaSyncRow
import com.example.data.PhotoItemEntity
import com.example.data.buildFavoriteReconcilePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemFavoriteReconcileTest {
    private fun photo(
        key: String,
        favorite: Boolean
    ) = PhotoItemEntity(
        mediaStoreKey = key,
        year = 2026,
        month = 11,
        day = 27,
        takenAtMillis = 1_000L,
        fileSizeBytes = 10L,
        widthPx = 100,
        heightPx = 100,
        contentUri = "content://media/external/images/media/1",
        isFavorite = favorite,
        isSample = false
    )

    private fun row(
        id: Long,
        key: String,
        favorite: Boolean
    ) = DeviceMediaSyncRow(
        id = id,
        mediaStoreKey = key,
        durationMs = 0L,
        mimeType = "image/jpeg",
        imageFormat = "JPEG",
        mediaSubtype = "STANDARD",
        isAnimated = false,
        motionPhotoType = "NONE",
        motionVideoUri = null,
        motionVideoDurationMs = 0L,
        motionVideoOffset = null,
        motionVideoLength = null,
        pairedMediaStoreKey = null,
        fileSizeBytes = 10L,
        widthPx = 100,
        heightPx = 100,
        caption = "",
        bucketName = null,
        isFavorite = favorite
    )

    @Test
    fun systemFavoriteChangesAreImportedAfterMigration() {
        val scanned = listOf(photo("img_1", favorite = true), photo("img_2", favorite = false))
        val existing = mapOf(
            "img_1" to row(1L, "img_1", favorite = false),
            "img_2" to row(2L, "img_2", favorite = true)
        )

        val plan = buildFavoriteReconcilePlan(scanned, existing, preserveLocalFavorites = false)

        assertEquals(listOf(1L), plan.favoriteTrueIds)
        assertEquals(listOf(2L), plan.favoriteFalseIds)
        assertTrue(plan.legacyFavoriteExportCandidates.isEmpty())
    }

    @Test
    fun pendingMigrationExportsLegacyLocalFavoriteInsteadOfErasingIt() {
        val scanned = listOf(photo("img_1", favorite = false))
        val existing = mapOf("img_1" to row(1L, "img_1", favorite = true))

        val plan = buildFavoriteReconcilePlan(scanned, existing, preserveLocalFavorites = true)

        assertTrue(plan.favoriteFalseIds.isEmpty())
        assertEquals(1, plan.legacyFavoriteExportCandidates.size)
        assertTrue(plan.legacyFavoriteExportCandidates.single().isFavorite)
    }

    @Test
    fun matchingFavoritesProduceNoWrites() {
        val scanned = listOf(photo("img_1", favorite = true))
        val existing = mapOf("img_1" to row(1L, "img_1", favorite = true))

        val plan = buildFavoriteReconcilePlan(scanned, existing, preserveLocalFavorites = false)

        assertTrue(plan.favoriteTrueIds.isEmpty())
        assertTrue(plan.favoriteFalseIds.isEmpty())
        assertTrue(plan.legacyFavoriteExportCandidates.isEmpty())
    }
}
