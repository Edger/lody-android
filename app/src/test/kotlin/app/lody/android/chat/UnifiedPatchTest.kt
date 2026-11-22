package app.lody.android.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedPatchTest {
    @Test fun aReplacementEmitsAWalkableHunk() {
        assertEquals(
            """
            |===================================================================
            |--- a/src/a.kt
            |+++ b/src/a.kt
            |@@ -1,3 +1,3 @@
            | a
            |-b
            |+B
            | c
            |""".trimMargin(),
            UnifiedPatch.make("src/a.kt", "a\nb\nc\n", "a\nB\nc\n"),
        )
    }

    @Test fun anAddedFileStartsAtZero() {
        assertEquals(
            """
            |===================================================================
            |--- a/new.kt
            |+++ b/new.kt
            |@@ -0,0 +1,2 @@
            |+x
            |+y
            |""".trimMargin(),
            UnifiedPatch.make("new.kt", "", "x\ny\n"),
        )
    }

    @Test fun aDeletedFileEndsAtZero() {
        assertEquals(
            """
            |===================================================================
            |--- a/gone.kt
            |+++ b/gone.kt
            |@@ -1,1 +0,0 @@
            |-x
            |""".trimMargin(),
            UnifiedPatch.make("gone.kt", "x\n", ""),
        )
    }

    @Test fun aMissingFinalNewlineIsMarkedOnBothSides() {
        val patch = UnifiedPatch.make("a.kt", "a", "b")
        assertEquals(
            listOf("@@ -1,1 +1,1 @@", "-a", "\\ No newline at end of file", "+b", "\\ No newline at end of file"),
            patch.lines().drop(3).dropLast(1),
        )
    }

    @Test fun aNonAsciiPathIsQuotedWithOctalBytes() {
        assertEquals("\"a/caf\\303\\251.kt\"", UnifiedPatch.quoted("a/café.kt"))
        assertEquals("a/plain.kt", UnifiedPatch.quoted("a/plain.kt"))
    }

    @Test fun aLongCommonPrefixIsTrimmedFromTheHunk() {
        val old = (1..20).joinToString("\n") { "line $it" } + "\n"
        val new = old.replace("line 20", "line twenty")

        val patch = UnifiedPatch.make("a.kt", old, new)

        assertTrue(patch.contains("@@ -16,5 +16,5 @@"))
        assertFalse(patch.contains("\n line 1\n"))
    }

    @Test fun anOversizedMiddleFallsBackToAWholeFileHunk() {
        val old = (1..100).joinToString("\n") { "a$it" } + "\n"
        val new = (1..100).joinToString("\n") { "b$it" } + "\n"

        val patch = UnifiedPatch.make("a.kt", old, new, limit = 10)

        assertTrue(patch.contains("@@ -1,100 +1,100 @@"))
        assertTrue(patch.contains("-a100"))
        assertTrue(patch.contains("+b100"))
    }
}
