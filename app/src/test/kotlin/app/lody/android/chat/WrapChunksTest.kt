package app.lody.android.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WrapChunksTest {
    @Test
    fun `a short line stays one chunk`() {
        assertEquals(listOf("val x = 1"), wrapChunks("val x = 1", size = 2_000))
    }

    @Test
    fun `a long line is cut at the chunk size`() {
        val chunks = wrapChunks("x".repeat(5_000), size = 2_000)

        assertEquals(listOf(2_000, 2_000, 1_000), chunks.map(String::length))
        assertEquals("x".repeat(5_000), chunks.joinToString(""))
    }

    @Test
    fun `an empty line stays one chunk`() {
        assertEquals(listOf(""), wrapChunks("", size = 2_000))
    }

    @Test
    fun `a cut never separates a surrogate pair`() {
        // The pair spans the 2-character boundary; the first chunk takes both halves instead.
        val chunks = wrapChunks("a😀b", size = 2)

        assertEquals(listOf("a😀", "b"), chunks)
        assertEquals("a😀b", chunks.joinToString(""))
        assertTrue(chunks.all { it.isNotEmpty() })
    }

    @Test
    fun `a wrapped line above the chunk cap is cut with the hint`() {
        val shown = cappedWrappedLine("x".repeat(MaxWrappedLineChars + 10), "hint")

        assertEquals(MaxWrappedLineChars, shown.substringBefore("… ").length)
        assertTrue(shown.endsWith("… hint"))
    }

    @Test
    fun `a wrapped line at the chunk cap is unchanged`() {
        val text = "x".repeat(MaxWrappedLineChars)

        assertEquals(text, cappedWrappedLine(text, "hint"))
    }
}
