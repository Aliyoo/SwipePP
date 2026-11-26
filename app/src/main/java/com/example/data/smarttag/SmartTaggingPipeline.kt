package com.example.data.smarttag

import android.util.Log
import com.example.data.FlipYearDao
import com.example.data.PhotoEmbeddingEntity
import com.example.data.SmartTagBatchUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Incremental background indexer that walks every photo row whose stored
 * `contentLabelVersion` is older than the current taxonomy, decodes a small bitmap,
 * runs ML Kit + MobileCLIP analysis, and writes tags in batched Room transactions
 * (one Flow invalidation per batch instead of one per photo).
 */
class SmartTaggingPipeline(
    private val dao: FlipYearDao,
    private val analyzer: SmartTagAnalyzer,
    private val drawableResolver: (String?) -> Int?
) {

    data class Progress(
        val processed: Int = 0,
        val total: Int = 0,
        val tagged: Int = 0,
        val isRunning: Boolean = false
    )

    /**
     * Processes up to [maxItems] pending photos in batches. Cooperative cancellation friendly:
     * the caller owns the coroutine scope and may cancel at any time between items.
     *
     * @return number of photos successfully analyzed (tagged or explicitly marked empty)
     */
    suspend fun runPending(
        maxItems: Int = Int.MAX_VALUE,
        onProgress: (Progress) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val version = SmartTagHeuristics.CONTENT_LABEL_VERSION
        val total = dao.countPhotosPendingSmartTagging(version).coerceAtMost(maxItems)
        if (total <= 0) {
            onProgress(Progress(0, 0, 0, isRunning = false))
            return@withContext 0
        }
        onProgress(Progress(0, total, 0, isRunning = true))

        var processed = 0
        var tagged = 0
        var keepGoing = true
        while (keepGoing) {
            currentCoroutineContext().ensureActive()
            val batchLimit = if (processed == 0) FIRST_BATCH_SIZE else BATCH_SIZE
            val batch = dao.getPhotosPendingSmartTagging(version, batchLimit)
            if (batch.isEmpty()) break

            val labelUpdates = ArrayList<SmartTagBatchUpdate>(batch.size)
            val embeddings = ArrayList<PhotoEmbeddingEntity>(batch.size)

            for (photo in batch) {
                if (processed >= total) break
                try {
                    currentCoroutineContext().ensureActive()
                } catch (ce: CancellationException) {
                    keepGoing = false
                    break
                }
                val bitmap = analyzer.decodeBitmapFor(photo, drawableResolver)
                if (bitmap != null) {
                    val result = analyzer.analyze(bitmap)
                    labelUpdates.add(
                        SmartTagBatchUpdate(
                            id = photo.id,
                            labelsCsv = result.categoryKeys.joinToString(",")
                        )
                    )
                    result.embeddingFp16?.let {
                        embeddings.add(
                            PhotoEmbeddingEntity(
                                photoId = photo.id,
                                embeddingFp16 = it,
                                updatedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    if (result.categoryKeys.isNotEmpty()) tagged++
                } else {
                    // Undecodable rows are still advanced past this version to avoid infinite retries.
                    labelUpdates.add(SmartTagBatchUpdate(id = photo.id, labelsCsv = ""))
                }
                processed++
                if (processed % 4 == 0) {
                    onProgress(Progress(processed, total, tagged, isRunning = true))
                }
                delay(INTER_ITEM_DELAY_MS)
            }

            try {
                if (labelUpdates.isNotEmpty()) {
                    dao.applySmartTagBatch(labelUpdates, embeddings, version)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "batch write failed", t)
            }
            onProgress(Progress(processed, total, tagged, isRunning = keepGoing))
            if (!keepGoing || processed >= total) break
        }

        onProgress(Progress(processed, total, tagged, isRunning = false))
        processed
    }

    fun close() = analyzer.close()

    companion object {
        // NOTE: decoded bitmaps are never recycled here — resource drawables share a single
        // framework-cached Bitmap instance, and recycling it corrupts both later inference
        // and any UI currently rendering the same drawable. The small 512px decodes are
        // short-lived garbage the GC reclaims cheaply.
        private const val TAG = "SmartTaggingPipeline"
        private const val FIRST_BATCH_SIZE = 12
        private const val BATCH_SIZE = 24
        private const val INTER_ITEM_DELAY_MS = 24L
    }
}
