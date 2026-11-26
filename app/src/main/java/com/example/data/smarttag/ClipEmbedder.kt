package com.example.data.smarttag

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * On-device Chinese-CLIP RN50 dual encoder (fp16 ONNX):
 * - vision tower: input `image` [1,3,224,224] fp32, direct 224x224 rescale (the model was
 *   trained with a plain resize, NOT aspect-preserving crop), /255 then CLIP mean/std
 *   normalization; output `unnorm_image_features` [1,1024]
 * - text tower: input `text` [1,52] int64 wordpiece ids (see [CnClipTokenizer]);
 *   output `unnorm_text_features` [1,1024]
 * Both share one embedding space, serving fixed-category zero-shot tagging (offline
 * precomputed `clip_categories.bin`, Chinese prompts) and free-text semantic search.
 *
 * Lazy, thread-confined usage: callers must invoke [analyze]/[encodeText] through the
 * analyzer's mutex; sessions are created on first use and reused.
 */
class ClipEmbedder(private val context: Context) {

    class ModelUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

    private var visionSession: OrtSession? = null
    private var textSession: OrtSession? = null
    private var environment: OrtEnvironment? = null
    private var categoryVectors: List<Pair<SmartCategory, FloatArray>>? = null
    private var tokenizer: CnClipTokenizer? = null

    val isReady: Boolean
        get() = visionSession != null && categoryVectors != null

    @Synchronized
    private fun ensureLoaded() {
        try {
            val env = environment ?: OrtEnvironment.getEnvironment().also { environment = it }
            if (visionSession == null) {
                visionSession = createSession(env, VISION_MODEL_ASSET)
            }
            if (categoryVectors == null) {
                categoryVectors = loadCategoryTable()
            }
        } catch (t: Throwable) {
            throw ModelUnavailableException("CLIP vision model unavailable: ${t.message}", t)
        }
    }

    @Synchronized
    private fun ensureTextLoaded() {
        ensureLoaded()
        try {
            val env = environment!!
            if (textSession == null) {
                textSession = createSession(env, TEXT_MODEL_ASSET)
            }
            if (tokenizer == null) {
                tokenizer = CnClipTokenizer(context)
            }
        } catch (t: Throwable) {
            throw ModelUnavailableException("CLIP text model unavailable: ${t.message}", t)
        }
    }

    // Loads via a file path so ORT memory-maps the weights instead of the Java heap
    // holding a full copy (plus the readBytes copy) — required on small-heap devices.
    private fun createSession(env: OrtEnvironment, asset: String): OrtSession {
        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setInterOpNumThreads(1)
        }
        return env.createSession(modelFileForAsset(asset).absolutePath, opts)
    }

    private fun modelFileForAsset(asset: String): java.io.File {
        val dir = java.io.File(context.filesDir, "onnx_models").apply { mkdirs() }
        val out = java.io.File(dir, asset)
        val assetLen = try {
            context.assets.openFd(asset).use { it.length }
        } catch (t: Throwable) {
            -1L
        }
        val upToDate = out.exists() && assetLen >= 0 && out.length() == assetLen
        if (!upToDate) {
            val tmp = java.io.File(dir, "$asset.tmp")
            context.assets.open(asset).use { input ->
                tmp.outputStream().use { output -> input.copyTo(output, bufferSize = 256 * 1024) }
            }
            if (out.exists()) out.delete()
            tmp.renameTo(out)
        }
        return out
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
     * Computes the normalized 1024-d embedding for [bitmap] (any size; internally rescaled)
     * and zero-shot category assignments. Returns null when inference fails for this input.
     */
    fun analyze(bitmap: Bitmap): ClipAnalysis? {
        ensureLoaded()
        val s = visionSession ?: return null
        val cats = categoryVectors ?: return null
        return try {
            val embedding = runVision(bitmap, s) ?: return null
            val picked = pickCategories(embedding, cats)
            ClipAnalysis(
                embedding = embedding,
                categories = picked.map { it.first },
                topSimilarity = picked.firstOrNull()?.second ?: 0f
            )
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Computes the normalized 1024-d embedding for a free-text query, for cosine-similarity
     * search against stored image embeddings. Returns null when the text model is unavailable.
     */
    fun encodeText(query: String): FloatArray? {
        if (query.isBlank()) return null
        return try {
            ensureTextLoaded()
            val s = textSession ?: return null
            val tk = tokenizer ?: return null
            val ids = tk.encode(query.trim())
            val shape = longArrayOf(1, CnClipTokenizer.CONTEXT_LENGTH.toLong())
            val idBuffer = ByteBuffer.allocateDirect(ids.size * 8)
                .order(ByteOrder.nativeOrder())
                .asLongBuffer()
            idBuffer.put(ids)
            idBuffer.rewind()
            OnnxTensor.createTensor(environment, idBuffer, shape).use { tensor ->
                s.run(mapOf(TEXT_INPUT_NAME to tensor)).use { result ->
                    val raw = (result.get(0).value as? Array<*>)?.get(0) as? FloatArray ?: return null
                    val embedding = raw.copyOf()
                    normalizeInPlace(embedding)
                    embedding
                }
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun runVision(bitmap: Bitmap, s: OrtSession): FloatArray? {
        val input = preprocess(bitmap)
        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        OnnxTensor.createTensor(environment, input, shape).use { tensor ->
            s.run(mapOf(VISION_INPUT_NAME to tensor)).use { result ->
                val raw = (result.get(0).value as? Array<*>)?.get(0) as? FloatArray ?: return null
                val embedding = raw.copyOf()
                normalizeInPlace(embedding)
                return embedding
            }
        }
    }

    /**
     * Cosine similarity of [embedding] against every fixed category. cn-clip RN50 compresses
     * similarities into a narrow band (noise floor ~0.42, correct matches ~0.48-0.51 on the
     * validation samples), so absolute thresholds are meaningless: acceptance is relative to
     * the mean similarity across all categories for this image.
     */
    private fun pickCategories(
        embedding: FloatArray,
        cats: List<Pair<SmartCategory, FloatArray>>
    ): List<Pair<SmartCategory, Float>> {
        val scored = cats.map { (cat, vec) ->
            var dot = 0f
            for (i in 0 until EMBEDDING_DIM) dot += embedding[i] * vec[i]
            cat to dot
        }.sortedByDescending { it.second }
        if (scored.isEmpty()) return emptyList()

        var sum = 0f
        for ((_, sim) in scored) sum += sim
        val floor = sum / scored.size + SmartTagHeuristics.CLIP_CATEGORY_MARGIN_OVER_MEAN

        val chosen = ArrayList<Pair<SmartCategory, Float>>(SmartTagHeuristics.MAX_TAGS_PER_PHOTO)
        for ((cat, sim) in scored) {
            if (chosen.size >= SmartTagHeuristics.MAX_TAGS_PER_PHOTO) break
            if (sim < floor) break
            chosen.add(cat to sim)
        }
        return chosen
    }

    private fun preprocess(src: Bitmap): FloatBuffer {
        // Direct rescale to 224x224 without preserving aspect ratio, matching cn-clip's
        // Resize((224, 224)) training transform.
        val scaled = Bitmap.createScaledBitmap(src, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
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
        // Now normalize per-channel with CLIP mean/std in place.
        for (c in 0 until 3) {
            val mean = CHANNEL_MEAN[c]
            val std = CHANNEL_STD[c]
            val offset = c * plane
            for (i in 0 until plane) {
                val idx = offset + i
                buffer.put(idx, (buffer.get(idx) - mean) / std)
            }
        }
        buffer.rewind()
        // `scaled` is a fresh bitmap unless the source was already exactly 224x224; never
        // recycle shared framework instances (see SmartTaggingPipeline note).
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
            visionSession?.close()
        } catch (_: Throwable) {
        }
        try {
            textSession?.close()
        } catch (_: Throwable) {
        }
        visionSession = null
        textSession = null
    }

    companion object {
        const val INPUT_SIZE = 224
        const val EMBEDDING_DIM = 1024
        const val VISION_INPUT_NAME = "image"
        const val TEXT_INPUT_NAME = "text"
        const val VISION_MODEL_ASSET = "cnclip_vision_fp16.onnx"
        const val TEXT_MODEL_ASSET = "cnclip_text_int8.onnx"
        const val CATEGORY_TABLE_ASSET = "clip_categories.bin"
        private val CHANNEL_MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
        private val CHANNEL_STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

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
