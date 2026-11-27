package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.smarttag.CnClipTokenizer
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Golden-value parity test against the reference Python FullTokenizer used to validate the
 * cn-clip models offline: identical inputs must produce identical id sequences (the model's
 * retrieval quality depends on exact tokenization parity).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CnClipTokenizerTest {

    private val tokenizer by lazy {
        CnClipTokenizer(ApplicationProvider.getApplicationContext<Context>())
    }

    private fun encodeNonzero(text: String): LongArray {
        val ids = tokenizer.encode(text)
        return ids.filter { it != 0L }.toLongArray()
    }

    @Test
    fun chineseQueriesMatchPythonReference() {
        assertArrayEquals(longArrayOf(101, 7434, 2255, 102), encodeNonzero("雪山"))
        assertArrayEquals(longArrayOf(101, 3862, 6804, 4638, 1814, 2356, 102), encodeNonzero("海边的城市"))
        assertArrayEquals(longArrayOf(101, 4344, 1488, 102), encodeNonzero("猫咪"))
        assertArrayEquals(longArrayOf(101, 3189, 5862, 3198, 1146, 4638, 1921, 4958, 102), encodeNonzero("日落时分的天空"))
    }

    @Test
    fun mixedLatinLowercasesAndWordpieces() {
        assertArrayEquals(longArrayOf(101, 9706, 1476, 1565, 102), encodeNonzero("coffee 咖啡"))
    }

    @Test
    fun outputIsFixedLengthAndBookended() {
        val ids = tokenizer.encode("雪")
        org.junit.Assert.assertEquals(CnClipTokenizer.CONTEXT_LENGTH, ids.size)
        org.junit.Assert.assertEquals(101L, ids[0])
        org.junit.Assert.assertEquals(0L, ids[ids.size - 1])
    }
}
