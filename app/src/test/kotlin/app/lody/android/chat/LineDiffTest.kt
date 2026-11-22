package app.lody.android.chat

import app.lody.android.chat.LineDiff.Kind
import org.junit.Assert.assertEquals
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

    @Test fun splitPairsAReplacementWithItsContext() {
        val lines = LineDiff.compute("a\nb\nc", "a\nB\nc").hunks.single().lines
        val pairs = LineDiff.pairs(lines)
        assertEquals(3, pairs.size)
        assertEquals("a", pairs[0].left?.text)
        assertEquals("a", pairs[0].right?.text)
        assertEquals("b", pairs[1].left?.text)
        assertEquals("B", pairs[1].right?.text)
        assertEquals("c", pairs[2].left?.text)
        assertEquals("c", pairs[2].right?.text)
    }

    @Test fun splitPairsUnequalRunsKeepTheLongerSide() {
        val lines = LineDiff.compute("a\nb\nc\nd", "a\nd").hunks.single().lines
        val pairs = LineDiff.pairs(lines).filter { it.left?.kind == Kind.Removed || it.right?.kind == Kind.Added }
        assertEquals(2, pairs.size)
        assertEquals("b", pairs[0].left?.text)
        assertEquals(null, pairs[0].right)
        assertEquals("c", pairs[1].left?.text)
        assertEquals(null, pairs[1].right)
    }

    @Test fun splitPairsAnInsertionAgainstNothing() {
        val lines = LineDiff.compute("a\nc", "a\nb\nc").hunks.single().lines
        val pairs = LineDiff.pairs(lines)
        assertEquals(3, pairs.size)
        assertEquals(null, pairs[1].left)
        assertEquals("b", pairs[1].right?.text)
        assertEquals(null, pairs[1].right?.oldNumber)
        assertEquals(2, pairs[1].right?.newNumber)
    }

    @Test fun widestLinesKeepsOnlyTheLongest() {
        val result = LineDiff.Result(
            listOf(
                LineDiff.Hunk(
                    listOf(
                        LineDiff.Line(Kind.Context, "a", 1, 1),
                        LineDiff.Line(Kind.Context, "longest line", 2, 2),
                        LineDiff.Line(Kind.Context, "medium", 3, 3),
                    ),
                    0,
                ),
            ),
            0,
        )

        assertEquals(listOf("longest line", "medium"), widestLines(result, count = 2))
    }
}
