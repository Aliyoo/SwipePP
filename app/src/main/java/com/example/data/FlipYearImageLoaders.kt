package com.example.data

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.request.videoFrameOption
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Tiered downsampling presets to guarantee no Compose surface ever decodes an unbounded
 * 12MP–48MP camera original into RAM during scrolling or rendering.
 */
enum class ImageDownsampleTier(
    val maxSizePx: Int,
    val bitmapConfig: Bitmap.Config,
    val scale: Scale,
    val allowRgb565: Boolean
) {
    /** Ultra-small texture for GPU ambient blur backdrops (< 40 KB in RAM). */
    AMBIENT_BLUR(
        maxSizePx = 128,
        bitmapConfig = Bitmap.Config.RGB_565,
        scale = Scale.FILL,
        allowRgb565 = true
    ),

    /** Calendar day tiles, map pin thumbnails, peeking burst deck layers (< 45 KB in RAM). */
    MICRO_THUMB(
        maxSizePx = 160,
        bitmapConfig = Bitmap.Config.RGB_565,
        scale = Scale.FILL,
        allowRgb565 = true
    ),

    /** 3-column gallery & Trash Vault grid tiles (~128 KB in RAM vs 48 MB raw camera image). */
    GRID_THUMB(
        maxSizePx = 256,
        bitmapConfig = Bitmap.Config.RGB_565,
        scale = Scale.FILL,
        allowRgb565 = true
    ),

    /** Expanded burst comparison frames & medium preview cards (~450 KB in RAM). */
    PREVIEW_CARD(
        maxSizePx = 480,
        bitmapConfig = Bitmap.Config.RGB_565,
        scale = Scale.FILL,
        allowRgb565 = true
    ),

    /** Bookshelf 3D covers, Arc Cleaner swipe cards, and video poster frames. */
    HERO_CARD(
        maxSizePx = 960,
        bitmapConfig = Bitmap.Config.RGB_565,
        scale = Scale.FIT,
        allowRgb565 = true
    ),

    /** Full-screen pinch-to-zoom lightbox (high fidelity ARGB_8888, capped to prevent 48MP OOM). */
    LIGHTBOX_FULL(
        maxSizePx = 2048,
        bitmapConfig = Bitmap.Config.ARGB_8888,
        scale = Scale.FIT,
        allowRgb565 = false
    )
}

/**
 * Centralized Coil [ImageLoader] registry with strict device-aware memory cache caps,
 * bounded decode concurrency for smooth high-frequency gallery scrolling, and unified
 * system `onTrimMemory` eviction.
 */
object FlipYearImageLoaders {

    private data class CacheBudget(
        val mediaMemoryBytes: Int,
        val mediaDiskBytes: Long,
        val mapMemoryBytes: Int,
        val mapDiskBytes: Long,
        val maxDecodeConcurrency: Int,
        val maxFetchConcurrency: Int
    )

    @Volatile
    private var mediaLoader: ImageLoader? = null

    @Volatile
    private var mapTileLoader: ImageLoader? = null

    private fun computeBudget(context: Context): CacheBudget {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice == true
        val memoryClassMb = (am?.memoryClass ?: 192).coerceAtLeast(64)
        val heapBytes = memoryClassMb.toLong() * 1024L * 1024L

        return when {
            isLowRam || memoryClassMb <= 128 -> CacheBudget(
                mediaMemoryBytes = (heapBytes * 0.12).toInt().coerceIn(12 * MB, 28 * MB),
                mediaDiskBytes = 48L * MB,
                mapMemoryBytes = 10 * MB,
                mapDiskBytes = 48L * MB,
                maxDecodeConcurrency = 2,
                maxFetchConcurrency = 3
            )

            memoryClassMb <= 256 -> CacheBudget(
                mediaMemoryBytes = (heapBytes * 0.15).toInt().coerceIn(20 * MB, 44 * MB),
                mediaDiskBytes = 80L * MB,
                mapMemoryBytes = 16 * MB,
                mapDiskBytes = 72L * MB,
                maxDecodeConcurrency = 3,
                maxFetchConcurrency = 4
            )

            else -> CacheBudget(
                mediaMemoryBytes = (heapBytes * 0.16).toInt().coerceIn(28 * MB, 64 * MB),
                mediaDiskBytes = 112L * MB,
                mapMemoryBytes = 22 * MB,
                mapDiskBytes = 96L * MB,
                maxDecodeConcurrency = 4,
                maxFetchConcurrency = 6
            )
        }
    }

    /**
     * Primary media [ImageLoader] shared across Gallery, Bookshelf, Flipbook, Arc Cleaner,
     * Trash Vault, Lightbox, and Ambient Blur surfaces.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMediaImageLoader(context: Context): ImageLoader {
        return mediaLoader ?: synchronized(this) {
            mediaLoader ?: run {
                val appCtx = context.applicationContext
                val budget = computeBudget(appCtx)
                ImageLoader.Builder(appCtx)
                    .components {
                        add(VideoFrameDecoder.Factory())
                    }
                    .memoryCache {
                        MemoryCache.Builder(appCtx)
                            // Explicit hard byte cap instead of unbounded percentage of large heaps
                            .maxSizeBytes(budget.mediaMemoryBytes)
                            .strongReferencesEnabled(true)
                            // Disabling weak reference retention prevents evicted scroll thumbnails from
                            // accumulating and causing stop-the-world GC spikes during rapid flings
                            .weakReferencesEnabled(false)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(appCtx.cacheDir.resolve("photo_thumb_cache"))
                            .maxSizeBytes(budget.mediaDiskBytes)
                            .build()
                    }
                    // Bound concurrent decodes so fast gallery flings cancel queued off-screen work
                    // cleanly without allocating dozens of simultaneous decode buffers
                    .decoderDispatcher(Dispatchers.Default.limitedParallelism(budget.maxDecodeConcurrency))
                    .fetcherDispatcher(Dispatchers.IO.limitedParallelism(budget.maxFetchConcurrency))
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .allowRgb565(true)
                    .allowHardware(true)
                    .respectCacheHeaders(false)
                    .crossfade(false)
                    .build()
                    .also { mediaLoader = it }
            }
        }
    }

    /**
     * Dedicated [ImageLoader] for slippy map tiles with an isolated, tightly bounded memory pool
     * so map panning never evicts gallery thumbnails and vice versa.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMapTileImageLoader(context: Context): ImageLoader {
        return mapTileLoader ?: synchronized(this) {
            mapTileLoader ?: run {
                val appCtx = context.applicationContext
                val budget = computeBudget(appCtx)
                ImageLoader.Builder(appCtx)
                    .memoryCache {
                        MemoryCache.Builder(appCtx)
                            .maxSizeBytes(budget.mapMemoryBytes)
                            .strongReferencesEnabled(true)
                            .weakReferencesEnabled(false)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(appCtx.cacheDir.resolve("map_tile_cache"))
                            .maxSizeBytes(budget.mapDiskBytes)
                            .build()
                    }
                    .decoderDispatcher(Dispatchers.Default.limitedParallelism(3))
                    .fetcherDispatcher(Dispatchers.IO.limitedParallelism(6))
                    // ARGB_8888 avoids PNG-8 indexed/alpha decode failures on some Android devices
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .allowHardware(false)
                    // Prevent Coil's ConnectivityManager observer from falsely blocking requests on emulators/VPNs
                    .networkObserverEnabled(false)
                    .crossfade(false)
                    .respectCacheHeaders(false)
                    .build()
                    .also { mapTileLoader = it }
            }
        }
    }

    /**
     * Builds a strictly downsampled [ImageRequest] with deterministic cache keys so rapid
     * bidirectional gallery scrolling hits memory cache consistently without duplicate entries.
     */
    fun buildPhotoImageRequest(
        context: Context,
        contentUri: String,
        isVideo: Boolean,
        tier: ImageDownsampleTier,
        overrideSizePx: Int? = null,
        enableCrossfade: Boolean = false
    ): ImageRequest {
        val resolvedPx = (overrideSizePx ?: tier.maxSizePx).coerceIn(96, tier.maxSizePx.coerceAtLeast(overrideSizePx ?: 96))
        val cacheKey = buildString(contentUri.length + 20) {
            append(contentUri)
            append('#')
            append(resolvedPx)
            append(if (isVideo) 'v' else 'i')
            append(if (tier.allowRgb565) '5' else '8')
        }
        return ImageRequest.Builder(context)
            .data(Uri.parse(contentUri))
            .size(resolvedPx, resolvedPx)
            .scale(tier.scale)
            .precision(Precision.INEXACT)
            .bitmapConfig(tier.bitmapConfig)
            .allowRgb565(tier.allowRgb565)
            // Keep hardware bitmaps enabled for grid/preview tiers (zero Dalvik heap overhead),
            // disable only for ambient blur where RenderEffect / software sampling may interact
            .allowHardware(tier != ImageDownsampleTier.AMBIENT_BLUR)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .apply {
                if (isVideo) {
                    // Keyframe-only seek avoids full P/B-frame software decoding during fast gallery flings
                    videoFrameMillis(0L)
                    videoFrameOption(MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                if (enableCrossfade) {
                    crossfade(140)
                } else {
                    crossfade(false)
                }
            }
            .build()
    }

    /**
     * Responds to Android memory pressure callbacks by releasing cached bitmaps gracefully.
     */
    fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                clearMemoryCaches()
            }

            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                mediaLoader?.memoryCache?.let { cache ->
                    cache.trimMemory(level)
                }
                mapTileLoader?.memoryCache?.clear()
            }
        }
    }

    fun clearMemoryCaches() {
        mediaLoader?.memoryCache?.clear()
        mapTileLoader?.memoryCache?.clear()
    }

    private const val MB = 1024 * 1024
}
