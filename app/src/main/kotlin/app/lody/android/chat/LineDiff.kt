package app.lody.android.chat

/**
 * A line diff for the diff page, pure. Common leading and trailing lines are trimmed, the middle
 * gets a longest-common-subsequence diff, and unchanged runs collapse to [context] lines around
 * each change, so a page shows hunks rather than the whole file. A middle larger than [limit] cells
 * is split at lines unique to both sides first, so a few changes scattered through a long file do
 * not need one table for the whole span; a span with no such line reports [Result.tooLarge]
 * instead of spending the phone's memory.
 */
object LineDiff {
    enum class Kind { Context, Added, Removed }

    /** One shown line; numbers are 1-based in the old / new file (null on the side it is absent from). */
    data class Line(val kind: Kind, val text: String, val oldNumber: Int?, val newNumber: Int?)

    /** A run of shown lines; [skippedBefore] unchanged lines were folded away above it. */
    data class Hunk(val lines: List<Line>, val skippedBefore: Int)

    data class Result(val hunks: List<Hunk>, val skippedAfter: Int, val tooLarge: Boolean = false)

    fun compute(old: String, new: String, context: Int = 3, limit: Long = 4_000_000): Result {
        val a = lines(old)
        val b = lines(new)
        var prefix = 0
        while (prefix < a.size && prefix < b.size && a[prefix] == b[prefix]) prefix++
        var suffix = 0
        while (suffix < a.size - prefix && suffix < b.size - prefix && a[a.size - 1 - suffix] == b[b.size - 1 - suffix]) suffix++
        val midA = a.subList(prefix, a.size - suffix)
        val midB = b.subList(prefix, b.size - suffix)

        val all = ArrayList<Line>(a.size + b.size)
        for (i in 0 until prefix) all += Line(Kind.Context, a[i], i + 1, i + 1)
        if (!appendSegment(midA, 0, midA.size, midB, 0, midB.size, prefix, limit, all)) {
            return Result(emptyList(), 0, tooLarge = true)
        }
        for (k in 0 until suffix) {
            val i = a.size - suffix + k
            val j = b.size - suffix + k
            all += Line(Kind.Context, a[i], i + 1, j + 1)
        }
        return hunks(all, context)
    }

    /**
     * Diffs one span into [out]. A span within [limit] cells takes the LCS table; a larger span is
     * split at [pivots] and its pieces diffed separately. False when a large span has no pivot to
     * split on.
     */
    private fun appendSegment(
        a: List<String>, a0: Int, a1: Int,
        b: List<String>, b0: Int, b1: Int,
        offset: Int, limit: Long, out: MutableList<Line>,
    ): Boolean {
        if (a0 == a1) {
            for (j in b0 until b1) out += Line(Kind.Added, b[j], null, offset + j + 1)
            return true
        }
        if (b0 == b1) {
            for (i in a0 until a1) out += Line(Kind.Removed, a[i], offset + i + 1, null)
            return true
        }
        if ((a1 - a0).toLong() * (b1 - b0) <= limit) {
            out += commonSubsequence(a, a0, a1, b, b0, b1, offset)
            return true
        }
        val pivots = pivots(a, a0, a1, b, b0, b1)
        if (pivots.isEmpty()) return false
        var ai = a0
        var bi = b0
        for ((i, j) in pivots) {
            if (!appendSegment(a, ai, i, b, bi, j, offset, limit, out)) return false
            out += Line(Kind.Context, a[i], offset + i + 1, offset + j + 1)
            ai = i + 1
            bi = j + 1
        }
        return appendSegment(a, ai, a1, b, bi, b1, offset, limit, out)
    }

    /** LCS over the span; removals before additions where both change. */
    private fun commonSubsequence(
        a: List<String>, a0: Int, a1: Int,
        b: List<String>, b0: Int, b1: Int,
        offset: Int,
    ): List<Line> {
        val n = a1 - a0
        val m = b1 - b0
        val table = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            table[i][j] = if (a[a0 + i] == b[b0 + j]) table[i + 1][j + 1] + 1 else maxOf(table[i + 1][j], table[i][j + 1])
        }
        val out = ArrayList<Line>(n + m)
        var i = 0
        var j = 0
        while (i < n || j < m) {
            when {
                i < n && j < m && a[a0 + i] == b[b0 + j] -> { out += Line(Kind.Context, a[a0 + i], offset + a0 + i + 1, offset + b0 + j + 1); i++; j++ }
                j >= m || (i < n && table[i + 1][j] >= table[i][j + 1]) -> { out += Line(Kind.Removed, a[a0 + i], offset + a0 + i + 1, null); i++ }
                else -> { out += Line(Kind.Added, b[b0 + j], null, offset + b0 + j + 1); j++ }
            }
        }
        return out
    }

    /**
     * Lines occurring exactly once on each side of the span, ordered into the longest increasing
     * run by position. Matching them first lets each gap between them use its own small LCS table.
     */
    private fun pivots(
        a: List<String>, a0: Int, a1: Int,
        b: List<String>, b0: Int, b1: Int,
    ): List<Pair<Int, Int>> {
        val inA = HashMap<String, Int>()
        for (i in a0 until a1) inA[a[i]] = if (inA.containsKey(a[i])) -1 else i
        val inB = HashMap<String, Int>()
        for (j in b0 until b1) inB[b[j]] = if (inB.containsKey(b[j])) -1 else j
        val candidates = ArrayList<Pair<Int, Int>>()
        for (i in a0 until a1) {
            val line = a[i]
            if (inA[line] != i) continue
            val j = inB[line] ?: continue
            if (j < 0) continue
            candidates += i to j
        }
        val tails = IntArray(candidates.size)
        val previous = IntArray(candidates.size) { -1 }
        var length = 0
        for (k in candidates.indices) {
            val value = candidates[k].second
            var lo = 0
            var hi = length
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (candidates[tails[mid]].second < value) lo = mid + 1 else hi = mid
            }
            if (lo > 0) previous[k] = tails[lo - 1]
            tails[lo] = k
            if (lo == length) length++
        }
        val out = ArrayList<Pair<Int, Int>>(length)
        var k = if (length == 0) -1 else tails[length - 1]
        while (k >= 0) {
            out += candidates[k]
            k = previous[k]
        }
        out.reverse()
        return out
    }

    private fun lines(text: String): List<String> = if (text.isEmpty()) emptyList() else text.removeSuffix("\n").split("\n")

    private fun hunks(all: List<Line>, context: Int): Result {
        val changed = all.indices.filter { all[it].kind != Kind.Context }
        if (changed.isEmpty()) return Result(emptyList(), all.size)
        val keep = BooleanArray(all.size)
        for (index in changed) for (k in maxOf(0, index - context)..minOf(all.lastIndex, index + context)) keep[k] = true
        val hunks = ArrayList<Hunk>()
        var skipped = 0
        var current = ArrayList<Line>()
        for (index in all.indices) {
            if (keep[index]) {
                current += all[index]
            } else {
                if (current.isNotEmpty()) { hunks += Hunk(current, skipped); current = ArrayList(); skipped = 0 }
                skipped++
            }
        }
        if (current.isNotEmpty()) { hunks += Hunk(current, skipped); skipped = 0 }
        return Result(hunks, skipped)
    }
}
