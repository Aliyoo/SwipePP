package com.example

import com.example.data.MediaType
import com.example.data.MonthStampEntity
import com.example.data.PhotoItemEntity
import com.example.data.PhotoStatus
import com.example.ui.GalleryEntry
import com.example.ui.components.TimelineGridItem
import com.example.ui.components.buildTimelineClusterLayout
import com.example.ui.components.formatMonthSummaryStamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineMonthStampTest {

    private fun photo(
        id: Long,
        year: Int,
        month: Int,
        isVideo: Boolean = false,
        bytes: Long = 1_000_000L
    ): PhotoItemEntity = PhotoItemEntity(
        id = id,
        mediaType = if (isVideo) MediaType.VIDEO.name else MediaType.IMAGE.name,
        year = year,
        month = month,
        day = 10,
        takenAtMillis = year * 1_000_000L + month * 10_000L + id,
        fileSizeBytes = bytes,
        widthPx = 4000,
        heightPx = 3000,
        status = PhotoStatus.UNREVIEWED.name
    )

    @Test
    fun buildTimelineClusterLayout_groupsMonthlyClustersAndComputesStamps() {
        assertEquals("Jan 2026", formatMonthSummaryStamp(2026, 1))
        assertEquals("Dec 2025", formatMonthSummaryStamp(2025, 12))

        val janBurst = GalleryEntry.Burst(
            groupId = "b1",
            photos = listOf(photo(1, 2026, 1, bytes = 2_000_000L), photo(2, 2026, 1, bytes = 3_000_000L)),
            bestPhoto = photo(1, 2026, 1, bytes = 2_000_000L)
        )
        val janVideo = GalleryEntry.Single(photo(3, 2026, 1, isVideo = true, bytes = 5_000_000L))
        val decPhoto = GalleryEntry.Single(photo(4, 2025, 12, bytes = 4_000_000L))

        val stamps = mapOf(
            "2025-12" to MonthStampEntity("2025-12", 2025, 12, 100L, 4_000_000L)
        )

        val layout = buildTimelineClusterLayout(
            entries = listOf(janBurst, janVideo, decPhoto),
            monthStamps = stamps
        )

        assertEquals(2, layout.clusters.size)
        // 2 dividers + 3 entries = 5 grid items
        assertEquals(5, layout.gridItems.size)
        assertTrue(layout.gridItems[0] is TimelineGridItem.MonthDivider)
        assertTrue(layout.gridItems[3] is TimelineGridItem.MonthDivider)

        val janCluster = layout.clusters[0]
        assertEquals("Jan 2026", janCluster.stampLabel)
        assertEquals(2, janCluster.photoCount)
        assertEquals(1, janCluster.videoCount)
        assertEquals(1, janCluster.burstGroupCount)
        assertEquals(10_000_000L, janCluster.totalBytes)
        assertEquals(0, janCluster.firstGridIndex)
        assertFalse(janCluster.isCleaned)

        val decCluster = layout.clusters[1]
        assertEquals("Dec 2025", decCluster.stampLabel)
        assertEquals(1, decCluster.photoCount)
        assertEquals(0, decCluster.burstGroupCount)
        assertEquals(3, decCluster.firstGridIndex)
        assertTrue(decCluster.isCleaned)

        // Binary search lookup across grid indices
        assertEquals("Jan 2026", layout.clusterForVisibleIndex(0)?.stampLabel)
        assertEquals("Jan 2026", layout.clusterForVisibleIndex(2)?.stampLabel)
        assertEquals("Dec 2025", layout.clusterForVisibleIndex(3)?.stampLabel)
        assertEquals("Dec 2025", layout.clusterForVisibleIndex(4)?.stampLabel)
    }
}
