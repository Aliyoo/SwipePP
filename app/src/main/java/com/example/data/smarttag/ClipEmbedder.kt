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
 * On-device Chinese-CLIP RN50 dual encoder:
 * - vision tower (`cnclip_vision_fp16.onnx`): input `image` [1,3,224,224] fp32, direct 224x224
 *   rescale + CLIP mean/std normalization; output `unnorm_image_features` [1,1024]
 * - text tower (`cnclip_text_int8.onnx`): input `text` [1,52] int64 wordpiece ids (see
 *   [CnClipTokenizer]); output `unnorm_text_features` [1,1024]
 * - category table (`clip_categories.bin`, CLC2 format): unnormalized mean vector of 60 neutral
 *   Chinese concepts (for zero-centered Hubness debiasing) + concrete multi-prototype vectors per
 *   [SmartCategory] (max-pooled instead of centroid-averaged).
 *
 * Vision and text sessions load and lock independently so user semantic search is never blocked
 * behind background vision indexing or vision model initialization.
 */
class ClipEmbedder(private val context: Context) {

    class ModelUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

    data class CategoryTable(
        val neutralMean: FloatArray,
        val categories: List<Pair<SmartCategory, List<FloatArray>>>
    )

    private val envLock = Any()
    private val visionLock = Any()
    private val textLock = Any()
    private val tableLock = Any()

    @Volatile private var environment: OrtEnvironment? = null
    @Volatile private var visionSession: OrtSession? = null
    @Volatile private var textSession: OrtSession? = null
    @Volatile private var categoryTable: CategoryTable? = null
    @Volatile private var tokenizer: CnClipTokenizer? = null

    val isReady: Boolean
        get() = visionSession != null && categoryTable != null

    private fun getOrCreateEnv(): OrtEnvironment {
        environment?.let { return it }
        synchronized(envLock) {
            return environment ?: OrtEnvironment.getEnvironment().also { environment = it }
        }
    }

    private fun ensureCategoryTableLoaded(): CategoryTable {
        categoryTable?.let { return it }
        synchronized(tableLock) {
            categoryTable?.let { return it }
            return try {
                loadCategoryTable().also { categoryTable = it }
            } catch (t: Throwable) {
                throw ModelUnavailableException("CLIP category table unavailable: ${t.message}", t)
            }
        }
    }

    private fun ensureVisionLoaded(): OrtSession {
        ensureCategoryTableLoaded()
        visionSession?.let { return it }
        synchronized(visionLock) {
            visionSession?.let { return it }
            return try {
                createSession(getOrCreateEnv(), VISION_MODEL_ASSET).also { visionSession = it }
            } catch (t: Throwable) {
                throw ModelUnavailableException("CLIP vision model unavailable: ${t.message}", t)
            }
        }
    }

    private fun ensureTextLoaded(): Pair<OrtSession, CnClipTokenizer> {
        val existingSession = textSession
        val existingTokenizer = tokenizer
        if (existingSession != null && existingTokenizer != null) {
            return existingSession to existingTokenizer
        }
        synchronized(textLock) {
            return try {
                val s = textSession ?: createSession(getOrCreateEnv(), TEXT_MODEL_ASSET).also { textSession = it }
                val tk = tokenizer ?: CnClipTokenizer(context).also { tokenizer = it }
                s to tk
            } catch (t: Throwable) {
                throw ModelUnavailableException("CLIP text model unavailable: ${t.message}", t)
            }
        }
    }

    /**
     * Returns the unnormalized mean vector of 60 neutral Chinese visual concepts from
     * `clip_categories.bin`. Dotting any unit image embedding with this vector estimates that
     * image's baseline cosine similarity against an average generic query (image Hubness), so
     * subtracting it yields a zero-centered semantic lift score.
     */
    fun getNeutralConceptBaseline(): FloatArray? {
        return try {
            ensureCategoryTableLoaded().neutralMean
        } catch (_: Throwable) {
            null
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
        // noBackupFilesDir: 150MB+ 的模型副本会撑爆 25MB 云备份配额，导致应用数据备份被整体跳过
        val dir = java.io.File(context.noBackupFilesDir, "onnx_models").apply { mkdirs() }
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

    private fun loadCategoryTable(): CategoryTable {
        val bytes = context.assets.open(CATEGORY_TABLE_ASSET).use { it.readBytes() }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buf.remaining() >= 12) { "category table too small" }
        val magic = ByteArray(4).also { buf.get(it) }.decodeToString()
        require(magic == "CLC2") { "bad category table magic: $magic (expected CLC2)" }
        val dim = buf.int
        val count = buf.int
        require(dim == EMBEDDING_DIM) { "unexpected embedding dim $dim" }

        val neutralMean = readFp16Vector(buf, dim)
        val out = ArrayList<Pair<SmartCategory, List<FloatArray>>>(count)
        repeat(count) {
            val keyLen = buf.int
            val keyBytes = ByteArray(keyLen).also { buf.get(it) }
            val key = keyBytes.decodeToString()
            val protoCount = buf.int
            val protos = ArrayList<FloatArray>(protoCount)
            repeat(protoCount) {
                protos.add(readFp16Vector(buf, dim))
            }
            val cat = SmartCategory.fromKey(key)
            if (cat != null && protos.isNotEmpty()) {
                out.add(cat to protos)
            }
        }
        return CategoryTable(neutralMean = neutralMean, categories = out)
    }

    private fun readFp16Vector(buf: ByteBuffer, dim: Int): FloatArray {
        val floats = FloatArray(dim)
        repeat(dim) { i ->
            floats[i] = halfToFloat(buf.short)
        }
        return floats
    }

    data class ClipAnalysis(
        val embedding: FloatArray,
        val categories: List<SmartCategory>,
        val topSimilarity: Float,
        val isLowInfo: Boolean = false
    )

    private data class PreprocessedImage(
        val tensorBuffer: FloatBuffer,
        val lumaStd: Float
    )

    /**
     * Computes the normalized 1024-d embedding for [bitmap] (any size; internally rescaled)
     * and zero-centered multi-prototype category assignments. Solid-color / blank frames are
     * flagged [ClipAnalysis.isLowInfo] with empty categories so accidental pocket shots never
     * pollute tags or semantic search.
     */
    fun analyze(bitmap: Bitmap): ClipAnalysis? {
        val s = ensureVisionLoaded()
        val table = ensureCategoryTableLoaded()
        return try {
            val preprocessed = preprocess(bitmap)
            if (preprocessed.lumaStd < SmartTagHeuristics.MIN_IMAGE_LUMA_STD) {
                return ClipAnalysis(
                    embedding = FloatArray(0),
                    categories = emptyList(),
                    topSimilarity = 0f,
                    isLowInfo = true
                )
            }
            val embedding = runVision(preprocessed.tensorBuffer, s) ?: return null
            val picked = pickCategories(embedding, table)
            ClipAnalysis(
                embedding = embedding,
                categories = picked.map { it.first },
                topSimilarity = picked.firstOrNull()?.second ?: 0f,
                isLowInfo = false
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
            val (s, tk) = ensureTextLoaded()
            val env = getOrCreateEnv()
            val ids = tk.encode(query.trim())
            val shape = longArrayOf(1, CnClipTokenizer.CONTEXT_LENGTH.toLong())
            val idBuffer = ByteBuffer.allocateDirect(ids.size * 8)
                .order(ByteOrder.nativeOrder())
                .asLongBuffer()
            idBuffer.put(ids)
            idBuffer.rewind()
            OnnxTensor.createTensor(env, idBuffer, shape).use { tensor ->
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

    private fun runVision(input: FloatBuffer, s: OrtSession): FloatArray? {
        val env = getOrCreateEnv()
        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        OnnxTensor.createTensor(env, input, shape).use { tensor ->
            s.run(mapOf(VISION_INPUT_NAME to tensor)).use { result ->
                val raw = (result.get(0).value as? Array<*>)?.get(0) as? FloatArray ?: return null
                val embedding = raw.copyOf()
                normalizeInPlace(embedding)
                return embedding
            }
        }
    }

    /**
     * Multi-prototype zero-centered lift category selection.
     * Each category's raw score is `max_{p in subPrototypes} dot(embedding, p)`, minus the
     * image's baseline similarity `dot(embedding, neutralMean)`. Removing the shared text cone
     * eliminates Hubness and sticky-category bias (`LANDMARK`, `SCREEN`, `PERSON`).
     */
    private fun pickCategories(
        embedding: FloatArray,
        table: CategoryTable
    ): List<Pair<SmartCategory, Float>> {
        val neutral = table.neutralMean
        var imageBaseline = 0f
        for (i in 0 until EMBEDDING_DIM) {
            imageBaseline += embedding[i] * neutral[i]
        }

        val scored = table.categories.map { (cat, protos) ->
            var bestDot = Float.NEGATIVE_INFINITY
            for (vec in protos) {
                var dot = 0f
                for (i in 0 until EMBEDDING_DIM) dot += embedding[i] * vec[i]
                if (dot > bestDot) bestDot = dot
            }
            cat to (bestDot - imageBaseline)
        }.sortedByDescending { it.second }
        if (scored.isEmpty()) return emptyList()

        val topLift = scored.first().second
        if (topLift < SmartTagHeuristics.CLIP_CATEGORY_MIN_LIFT) return emptyList()
        val relativeFloor = (topLift - SmartTagHeuristics.CLIP_CATEGORY_TOP1_MARGIN)
            .coerceAtLeast(SmartTagHeuristics.CLIP_CATEGORY_MIN_LIFT)

        val chosen = ArrayList<Pair<SmartCategory, Float>>(SmartTagHeuristics.MAX_TAGS_PER_PHOTO)
        for ((cat, lift) in scored) {
            if (chosen.size >= SmartTagHeuristics.MAX_TAGS_PER_PHOTO) break
            if (lift < SmartTagHeuristics.CLIP_CATEGORY_MIN_LIFT) break
            if (lift < relativeFloor && lift < SmartTagHeuristics.CLIP_CATEGORY_STRONG_LIFT) break
            chosen.add(cat to lift)
        }
        return chosen
    }

    private fun preprocess(src: Bitmap): PreprocessedImage {
        // Direct rescale to 224x224 matching cn-clip's Resize((224, 224)) training transform.
        val scaled = Bitmap.createScaledBitmap(src, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        val direct = ByteBuffer.allocateDirect(3 * INPUT_SIZE * INPUT_SIZE * 4)
            .order(ByteOrder.nativeOrder())
        val buffer = direct.asFloatBuffer()
        val plane = INPUT_SIZE * INPUT_SIZE
        val inv = 1f / 255f
        var lumaSum = 0.0
        var lumaSqSum = 0.0
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) * inv
            val g = ((p shr 8) and 0xFF) * inv
            val b = (p and 0xFF) * inv
            val luma = 0.299 * r + 0.587 * g + 0.114 * b
            lumaSum += luma
            lumaSqSum += luma * luma
            buffer.put(i, (r - CHANNEL_MEAN[0]) / CHANNEL_STD[0])
            buffer.put(plane + i, (g - CHANNEL_MEAN[1]) / CHANNEL_STD[1])
            buffer.put(2 * plane + i, (b - CHANNEL_MEAN[2]) / CHANNEL_STD[2])
        }
        buffer.rewind()
        val mean = lumaSum / plane
        val variance = (lumaSqSum / plane - mean * mean).coerceAtLeast(0.0)
        // `scaled` is a fresh bitmap unless the source was already exactly 224x224; never
        // recycle shared framework instances (see SmartTaggingPipeline note).
        return PreprocessedImage(tensorBuffer = buffer, lumaStd = sqrt(variance).toFloat())
    }

    private fun normalizeInPlace(v: FloatArray) {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = sqrt(sum)
        if (norm > 0f) {
            for (i in v.indices) v[i] /= norm
        }
    }

    fun closeVision() {
        synchronized(visionLock) {
            try {
                visionSession?.close()
            } catch (_: Throwable) {
            }
            visionSession = null
        }
    }

    fun closeText() {
        synchronized(textLock) {
            try {
                textSession?.close()
            } catch (_: Throwable) {
            }
            textSession = null
        }
    }

    fun close() {
        closeVision()
        closeText()
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

/** Zero-allocation variant of [decodeEmbeddingFp16]: decodes into [out] (must be at least bytes.size/2 long). */
fun decodeEmbeddingFp16Into(bytes: ByteArray, out: FloatArray) {
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    var i = 0
    while (buf.remaining() >= 2 && i < out.size) {
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
