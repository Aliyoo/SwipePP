package com.example.data.smarttag

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min
import kotlin.math.sqrt

/**
 * On-device MobileCLIP-S0 vision encoder (fp16 ONNX, input 256x256 RGB rescaled to 0..1,
 * no mean/std normalization per apple/MobileCLIP preprocessor config) plus the offline
 * precomputed fixed-category text embedding table from `clip_categories.bin`.
 *
 * Lazy, thread-confined usage: callers must invoke [analyze] from a single-threaded
 * dispatcher; the session is created on first use and reused.
 */
class ClipEmbedder(private val context: Context) {

    class ModelUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private var session: OrtSession? = null
    private var environment: OrtEnvironment? = null
    private var categoryVectors: List<Pair<SmartCategory, FloatArray>>? = null

    val isReady: Boolean
        get() = session != null && categoryVectors != null

    @Synchronized
    private fun ensureLoaded() {
        if (session != null && categoryVectors != null) return
        try {
            val env = environment ?: OrtEnvironment.getEnvironment().also { environment = it }
            if (session == null) {
                val bytes = context.assets.open(VISION_MODEL_ASSET).use { it.readBytes() }
                val opts = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(2)
                    setInterOpNumThreads(1)
                }
                session = env.createSession(bytes, opts)
            }
            if (categoryVectors == null) {
                categoryVectors = loadCategoryTable()
            }
        } catch (t: Throwable) {
            throw ModelUnavailableException("CLIP model unavailable: ${t.message}", t)
        }
    }

    private fun loadCategoryTable(): List<Pair<SmartCategory, FloatArray>> {
        val bytes = context.assets.open(CATEGORY_TABLE_ASSET).use { it.readBytes() }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buf.remaining() >= 12) { "category table too small" }
        val magic = ByteArray(4).also { buf.get(it) }.decodeToString()
        require(magic == "CLCT") { "bad category table magic: $magic" }
        val count = buf.int
        val dim = buf.int
        require(dim == EMBEDDING_DIM) { "unexpected embedding dim $dim" }
        val out = ArrayList<Pair<SmartCategory, FloatArray>>(count)
        repeat(count) {
            val keyLen = buf.int
            val keyBytes = ByteArray(keyLen).also { buf.get(it) }
            val key = keyBytes.decodeToString()
            val floats = FloatArray(dim)
            repeat(dim) { i ->
                floats[i] = halfToFloat(buf.short)
            }
            val cat = SmartCategory.fromKey(key)
            if (cat != null) out.add(cat to floats)
        }
        return out
    }

    data class ClipAnalysis(
        val embedding: FloatArray,
        val categories: List<SmartCategory>,
        val topSimilarity: Float
    )

    /**
     * Computes the normalized 512-d embedding for [bitmap] (any size; internally rescaled)
     * and zero-shot category assignments. Returns null when inference fails for this input.
     */
    fun analyze(bitmap: Bitmap): ClipAnalysis? {
        ensureLoaded()
        val s = session ?: return null
        val cats = categoryVectors ?: return null
        return try {
            val input = preprocess(bitmap)
            val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
            OnnxTensor.createTensor(environment, input, shape).use { tensor ->
                s.run(mapOf(INPUT_NAME to tensor)).use { result ->
                    val raw = when (val v = result.get(0).value) {
                        is Array<*> -> (v[0] as FloatArray)
                        else -> return null
                    }
                    val embedding = raw.copyOf()
                    normalizeInPlace(embedding)
                    embedding to pickCategories(embedding, cats)
                }
            }.let { (embedding, picked) ->
                ClipAnalysis(
                    embedding = embedding,
                    categories = picked.map { it.first },
                    topSimilarity = picked.firstOrNull()?.second ?: 0f
                )
            }
        } catch (t: Throwable) {
            null
        }
    }

    /** Cosine similarity of [embedding] against every fixed category, sorted descending. */
    private fun pickCategories(
        embedding: FloatArray,
        cats: List<Pair<SmartCategory, FloatArray>>
    ): List<Pair<SmartCategory, Float>> {
        val scored = cats.map { (cat, vec) ->
            var dot = 0f
            for (i in 0 until EMBEDDING_DIM) dot += embedding[i] * vec[i]
            cat to dot
        }.sortedByDescending { it.second }

        val chosen = ArrayList<Pair<SmartCategory, Float>>(SmartTagHeuristics.MAX_TAGS_PER_PHOTO)
        val top = scored.firstOrNull() ?: return chosen
        if (top.second < SmartTagHeuristics.CLIP_MIN_SIMILARITY) return chosen
        for ((cat, sim) in scored) {
            if (chosen.size >= SmartTagHeuristics.MAX_TAGS_PER_PHOTO) break
            if (sim < SmartTagHeuristics.CLIP_MIN_SIMILARITY) break
            if (top.second - sim > SmartTagHeuristics.CLIP_TIE_MARGIN) break
            chosen.add(cat to sim)
        }
        return chosen
    }

    private fun preprocess(src: Bitmap): FloatBuffer {
        // Resize shortest edge to INPUT_SIZE then center-crop, matching CLIPFeatureExtractor.
        val w = src.width
        val h = src.height
        val scale = INPUT_SIZE.toFloat() / min(w, h)
        val rw = Math.round(w * scale).coerceAtLeast(INPUT_SIZE)
        val rh = Math.round(h * scale).coerceAtLeast(INPUT_SIZE)
        val scaled = if (w != rw || h != rh) {
            Bitmap.createScaledBitmap(src, rw, rh, true)
        } else {
            src
        }
        val left = (rw - INPUT_SIZE) / 2
        val top = (rh - INPUT_SIZE) / 2
        val cropped = Bitmap.createBitmap(scaled, left, top, INPUT_SIZE, INPUT_SIZE)

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        cropped.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        val direct = ByteBuffer.allocateDirect(3 * INPUT_SIZE * INPUT_SIZE * 4)
            .order(ByteOrder.nativeOrder())
        val buffer = direct.asFloatBuffer()
        val plane = INPUT_SIZE * INPUT_SIZE
        val inv = 1f / 255f
        for (i in pixels.indices) {
            val p = pixels[i]
            buffer.put(i, ((p shr 16) and 0xFF) * inv)
            buffer.put(plane + i, ((p shr 8) and 0xFF) * inv)
            buffer.put(2 * plane + i, (p and 0xFF) * inv)
        }
        buffer.rewind()
        if (cropped !== scaled && cropped !== src) cropped.recycle()
        if (scaled !== src) scaled.recycle()
        return buffer
    }

    private fun normalizeInPlace(v: FloatArray) {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = sqrt(sum)
        if (norm > 0f) {
            for (i in v.indices) v[i] /= norm
        }
    }

    @Synchronized
    fun close() {
        try {
            session?.close()
        } catch (_: Throwable) {
        }
        session = null
    }

    companion object {
        const val INPUT_SIZE = 256
        const val EMBEDDING_DIM = 512
        const val INPUT_NAME = "pixel_values"
        const val VISION_MODEL_ASSET = "clip_vision_fp16.onnx"
        const val CATEGORY_TABLE_ASSET = "clip_categories.bin"

        private fun halfToFloat(h: Short): Float {
            val bits = h.toInt() and 0xFFFF
            val sign = (bits ushr 15) and 0x1
            val exp = (bits ushr 10) and 0x1F
            val frac = bits and 0x3FF
            val value = when {
                exp == 0 -> frac.toFloat() / 1024f * pow2(-14)
                exp == 31 -> if (frac == 0) Float.POSITIVE_INFINITY else Float.NaN
                else -> (1f + frac.toFloat() / 1024f) * pow2(exp - 15)
            }
            return if (sign == 1) -value else value
        }

        private fun pow2(e: Int): Float {
            var r = 1f
            var i = 0
            val step = if (e >= 0) 2f else 0.5f
            while (i < kotlin.math.abs(e)) {
                r *= step
                i++
            }
            return r
        }
    }
}

/** Encodes a normalized float embedding as little-endian float16 bytes for compact Room storage. */
fun encodeEmbeddingFp16(embedding: FloatArray): ByteArray {
    val out = ByteBuffer.allocate(embedding.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (f in embedding) {
        out.putShort(floatToHalf(f))
    }
    return out.array()
}

fun decodeEmbeddingFp16(bytes: ByteArray): FloatArray {
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val out = FloatArray(bytes.size / 2)
    var i = 0
    while (buf.remaining() >= 2) {
        val bits = buf.short.toInt() and 0xFFFF
        val sign = (bits ushr 15) and 0x1
        val exp = (bits ushr 10) and 0x1F
        val frac = bits and 0x3FF
        val value = when {
            exp == 0 -> frac.toFloat() / 1024f * 6.103515625e-05f
            exp == 31 -> if (frac == 0) Float.POSITIVE_INFINITY else Float.NaN
            else -> {
                var m = 1f + frac.toFloat() / 1024f
                var e = exp - 15
                while (e > 0) { m *= 2f; e-- }
                while (e < 0) { m /= 2f; e++ }
                m
            }
        }
        out[i++] = if (sign == 1) -value else value
    }
    return out
}

private fun floatToHalf(f: Float): Short {
    val bits = java.lang.Float.floatToIntBits(f)
    val sign = (bits ushr 16) and 0x8000
    var value = (bits and 0x7FFFFFFF)
    if (value >= 0x477FE000) return (sign or 0x7C00).toShort() // overflow -> inf
    if (value < 0x38800000) return sign.toShort() // flush sub-2^-14 magnitudes (fp16 denormals) to zero
    value = value or 0x1000 // round-to-nearest-even bias
    val exp = ((value ushr 23) and 0xFF) - 127 + 15
    val mantissa = value ushr 13
    return (sign or (exp shl 10) or (mantissa and 0x3FF)).toShort()
}
