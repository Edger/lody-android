package app.lody.android.chat

import app.lody.android.chat.LineDiff.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineDiffTest {
    private val old = (1..20).joinToString("\n") { "line $it" }

    @Test fun aChangeShowsAsOneHunkWithContext() {
        val new = old.replace("line 10", "line ten")
        val result = LineDiff.compute(old, new)
        assertEquals(1, result.hunks.size)
        val hunk = result.hunks.single()
        assertEquals(6, hunk.skippedBefore)
        assertEquals(listOf(Kind.Context, Kind.Context, Kind.Context, Kind.Removed, Kind.Added, Kind.Context, Kind.Context, Kind.Context), hunk.lines.map { it.kind })
        assertEquals(10, hunk.lines[3].oldNumber)
        assertEquals(10, hunk.lines[4].newNumber)
        assertEquals(7, result.skippedAfter)
    }

    @Test fun distantChangesFormSeparateHunks() {
        val new = old.replace("line 2\n", "line two\n").replace("line 19", "line nineteen")
        assertEquals(2, LineDiff.compute(old, new).hunks.size)
    }

    @Test fun anAddedFileIsAllAdditions() {
        val result = LineDiff.compute("", "a\nb\n")
        assertEquals(listOf(Kind.Added, Kind.Added), result.hunks.single().lines.map { it.kind })
    }

    @Test fun anOversizedMiddleIsReportedNotComputed() {
        val a = (1..3000).joinToString("\n") { "a$it" }
        val b = (1..3000).joinToString("\n") { "b$it" }
        assertTrue(LineDiff.compute(a, b).tooLarge)
    }

    @Test fun scatteredChangesInALargeFileAreStillDiffed() {
        // Changes near the top, the middle and the bottom: the trimmed middle spans most of the
        // 2500-line file, so a plain LCS table would exceed the cell limit even though only three
        // lines changed.
        val a = (1..2500).joinToString("\n") { "line $it" }
        val b = a.split("\n").toMutableList().apply {
            this[39] = "line forty"
            this[1199] = "line twelve hundred"
            this[2449] = "line twenty-four fifty"
        }.joinToString("\n")
        val result = LineDiff.compute(a, b)
        assertFalse(result.tooLarge)
        assertEquals(3, result.hunks.size)
    }
}
