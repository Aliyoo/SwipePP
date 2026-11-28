package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.FlipYearDatabase
import com.example.data.PhotoItemEntity
import com.example.data.PhotoRestoreSpec
import com.example.data.PhotoStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TreasureTimestampRobolectricTest {
    private lateinit var db: FlipYearDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FlipYearDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun photo(id: Long): PhotoItemEntity = PhotoItemEntity(
        id = id,
        mediaStoreKey = "img_$id",
        year = 2026,
        month = 11,
        day = 28,
        takenAtMillis = 1_000L + id,
        fileSizeBytes = 1_000L,
        widthPx = 100,
        heightPx = 100,
        status = PhotoStatus.UNREVIEWED.name,
        isSample = false
    )

    @Test
    fun favoriteWriteStoresTimestampAndUnfavoriteClearsIt() = runTest {
        val dao = db.flipYearDao()
        dao.insertPhotos(listOf(photo(1L)))

        dao.updatePhotosFavorite(listOf(1L), true, 123_456L)
        dao.getPhotosByIds(listOf(1L)).single().let {
            assertEquals(true, it.isFavorite)
            assertEquals(123_456L, it.favoritedAtMillis)
        }

        dao.updatePhotosFavorite(listOf(1L), false, null)
        dao.getPhotosByIds(listOf(1L)).single().let {
            assertEquals(false, it.isFavorite)
            assertNull(it.favoritedAtMillis)
        }
    }

    @Test
    fun restorePhotoStateRestoresExactFavoriteTimestamp() = runTest {
        val dao = db.flipYearDao()
        dao.insertPhotos(listOf(photo(1L)))
        dao.updatePhotosFavorite(listOf(1L), true, 123_456L)

        dao.restorePhotoStatesBatch(
            listOf(
                PhotoRestoreSpec(
                    photoId = 1L,
                    status = PhotoStatus.KEPT.name,
                    trashedAtMillis = null,
                    isFavorite = true,
                    favoritedAtMillis = 42L,
                    bucketName = "Camera",
                    year = 2026,
                    month = 11
                )
            )
        )

        dao.getPhotosByIds(listOf(1L)).single().let {
            assertEquals(PhotoStatus.KEPT.name, it.status)
            assertEquals(true, it.isFavorite)
            assertEquals(42L, it.favoritedAtMillis)
        }
    }

    @Test
    fun legacyFavoriteCanRemainWithoutTimestamp() = runTest {
        val dao = db.flipYearDao()
        dao.insertPhotos(listOf(photo(1L).copy(isFavorite = true, favoritedAtMillis = null)))

        val restored = dao.getPhotosByIds(listOf(1L)).single()

        assertEquals(true, restored.isFavorite)
        assertNull(restored.favoritedAtMillis)
    }
}
