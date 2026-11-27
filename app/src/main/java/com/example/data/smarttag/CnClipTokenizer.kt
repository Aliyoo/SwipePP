package com.example.data.smarttag

import android.content.Context
import java.text.Normalizer

/**
 * Pure-Kotlin port of the BERT FullTokenizer (BasicTokenizer + WordpieceTokenizer,
 * do_lower_case=true) used by Chinese-CLIP, faithfully matching the Python behavior the
 * offline embeddings were validated with: clean text, space out CJK chars, lowercase,
 * strip accents, split punctuation, then greedy longest-match-first wordpiece with "##"
 * continuations. Output format: [CLS] + tokens + [SEP], zero-padded to [CONTEXT_LENGTH].
 */
class CnClipTokenizer(context: Context) {

    private val vocab: Map<String, Int> = loadVocab(context)
    private val clsId: Int = vocab.getValue("[CLS]")
    private val sepId: Int = vocab.getValue("[SEP]")
    private val unkId: Int = vocab.getValue("[UNK]")

    private fun loadVocab(context: Context): Map<String, Int> {
        val map = HashMap<String, Int>(22000)
        context.assets.open(VOCAB_ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEachIndexed { index, token -> map[token] = index }
        }
        return map
    }

    /** Tokenizes [text] into a padded fixed-length id array matching the ONNX input. */
    fun encode(text: String): LongArray {
        val basicTokens = basicTokenize(text)
        val pieces = ArrayList<String>(basicTokens.size * 2)
        for (token in basicTokens) {
            pieces.addAll(wordpiece(token))
        }
        val truncated = pieces.take(CONTEXT_LENGTH - 2)
        val ids = LongArray(CONTEXT_LENGTH)
        ids[0] = clsId.toLong()
        truncated.forEachIndexed { i, piece ->
            ids[i + 1] = (vocab[piece] ?: unkId).toLong()
        }
        ids[truncated.size + 1] = sepId.toLong()
        return ids
    }

    private fun basicTokenize(text: String): List<String> {
        val cleaned = cleanText(text)
        val cjkSpaced = spaceChineseChars(cleaned)
        val tokens = mutableListOf<String>()
        for (token in whitespaceSplit(cjkSpaced)) {
            var t = token.lowercase()
            t = stripAccents(t)
            tokens.addAll(splitOnPunctuation(t))
        }
        return tokens
    }

    private fun cleanText(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text) {
            val cp = ch.code
            if (cp == 0 || cp == 0xfffd || isControl(ch)) continue
            out.append(if (isWhitespace(ch)) ' ' else ch)
        }
        return out.toString()
    }

    private fun spaceChineseChars(text: String): String {
        val out = StringBuilder(text.length * 2)
        for (ch in text) {
            if (isChineseChar(ch.code)) {
                out.append(' ').append(ch).append(' ')
            } else {
                out.append(ch)
            }
        }
        return out.toString()
    }

    private fun isChineseChar(cp: Int): Boolean =
        (cp in 0x4E00..0x9FFF) || (cp in 0x3400..0x4DBF) || (cp in 0x20000..0x2A6DF) ||
            (cp in 0x2A700..0x2B73F) || (cp in 0x2B740..0x2B81F) || (cp in 0x2B820..0x2CEAF) ||
            (cp in 0xF900..0xFAFF) || (cp in 0x2F800..0x2FA1F)

    private fun isWhitespace(ch: Char): Boolean =
        ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || Character.getType(ch) == Character.SPACE_SEPARATOR.toInt()

    private fun isControl(ch: Char): Boolean {
        if (ch == '\t' || ch == '\n' || ch == '\r') return false
        val type = Character.getType(ch)
        return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
    }

    private fun isPunctuation(ch: Char): Boolean {
        val cp = ch.code
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) return true
        val type = Character.getType(ch)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
            type == Character.DASH_PUNCTUATION.toInt() ||
            type == Character.START_PUNCTUATION.toInt() ||
            type == Character.END_PUNCTUATION.toInt() ||
            type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.OTHER_PUNCTUATION.toInt()
    }

    private fun stripAccents(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        val out = StringBuilder(normalized.length)
        for (ch in normalized) {
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) out.append(ch)
        }
        return out.toString()
    }

    private fun splitOnPunctuation(text: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (ch in text) {
            if (isPunctuation(ch)) {
                if (current.isNotEmpty()) {
                    out.add(current.toString())
                    current.clear()
                }
                out.add(ch.toString())
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out
    }

    private fun whitespaceSplit(text: String): List<String> =
        text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun wordpiece(token: String): List<String> {
        if (token.length > MAX_INPUT_CHARS_PER_WORD) return listOf("[UNK]")
        val out = mutableListOf<String>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var found: String? = null
            while (start < end) {
                var substr = token.substring(start, end)
                if (start > 0) substr = "##$substr"
                if (vocab.containsKey(substr)) {
                    found = substr
                    break
                }
                end--
            }
            if (found == null) return listOf("[UNK]")
            out.add(found)
            start = end
        }
        return out
    }

    companion object {
        const val CONTEXT_LENGTH = 52
        private const val VOCAB_ASSET = "cnclip_vocab.txt"
        private const val MAX_INPUT_CHARS_PER_WORD = 200
    }
}
