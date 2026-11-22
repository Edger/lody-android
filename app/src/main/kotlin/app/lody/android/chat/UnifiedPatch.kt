package app.lody.android.chat

/** Builds the unified patch copied from the diff page's More menu (iOS `UnifiedPatch`), pure. */
object UnifiedPatch {
    /** Same cell budget as LineDiff; past it the patch degrades to one whole-file hunk. */
    private const val MaxDiffCells = 4_000_000L

    private data class Line(val text: String, val newline: Boolean)

    private enum class Kind { Context, Delete, Insert }

    private data class Run(val kind: Kind, val lines: List<Line>)

    private data class HunkLine(val sign: String, val line: Line)

    private data class Hunk(val oldStart: Int, val oldLines: Int, val newStart: Int, val newLines: Int, val lines: List<HunkLine>)

    fun make(path: String, old: String, new: String, context: Int = 4, limit: Long = MaxDiffCells): String {
        val output = mutableListOf(
            "===================================================================",
            "--- " + quoted("a/" + path),
            "+++ " + quoted("b/" + path),
        )
        for (hunk in hunks(trimmedRuns(lines(old), lines(new), limit), context)) {
            val oldStart = if (hunk.oldLines == 0) hunk.oldStart - 1 else hunk.oldStart
            val newStart = if (hunk.newLines == 0) hunk.newStart - 1 else hunk.newStart
            output += "@@ -$oldStart,${hunk.oldLines} +$newStart,${hunk.newLines} @@"
            for (line in hunk.lines) {
                output += line.sign + line.line.text
                if (!line.line.newline) output += "\\ No newline at end of file"
            }
        }
        return output.joinToString("\n") + "\n"
    }

    fun quoted(name: String): String {
        val bytes = name.toByteArray(Charsets.UTF_8)
        val needsQuote = bytes.any { byte ->
            val value = byte.toInt() and 0xFF
            value < 0x20 || value > 0x7E || value == 0x22 || value == 0x5C
        }
        if (!needsQuote) return name
        val escapes = mapOf(
            0x07 to "\\a", 0x08 to "\\b", 0x09 to "\\t", 0x0A to "\\n",
            0x0B to "\\v", 0x0C to "\\f", 0x0D to "\\r", 0x22 to "\\\"", 0x5C to "\\\\",
        )
        val body = buildString {
            for (byte in bytes) {
                val value = byte.toInt() and 0xFF
                escapes[value]?.let { append(it); continue }
                if (value in 0x20..0x7E) append(value.toChar())
                else append('\\').append(value.toString(8).padStart(3, '0'))
            }
        }
        return "\"" + body + "\""
    }

    private fun lines(text: String): List<Line> {
        if (text.isEmpty()) return emptyList()
        val parts = text.split('\n').toMutableList()
        val trailing = text.endsWith("\n")
        if (trailing) parts.removeAt(parts.lastIndex)
        return parts.mapIndexed { index, part -> Line(part, newline = trailing || index < parts.lastIndex) }
    }

    /** Common leading and trailing lines stay context, so a one-line edit costs one LCS cell, not
     * the square of the file. A middle still over [limit] becomes one delete-all / insert-all run,
     * which is a valid patch and allocates no table. */
    private fun trimmedRuns(old: List<Line>, new: List<Line>, limit: Long): List<Run> {
        var prefix = 0
        while (prefix < old.size && prefix < new.size && old[prefix] == new[prefix]) prefix++
        var suffix = 0
        while (suffix < old.size - prefix && suffix < new.size - prefix &&
            old[old.lastIndex - suffix] == new[new.lastIndex - suffix]) suffix++
        val middleOld = old.subList(prefix, old.size - suffix)
        val middleNew = new.subList(prefix, new.size - suffix)
        val middle = if (middleOld.size.toLong() * middleNew.size > limit) {
            listOfNotNull(
                Run(Kind.Delete, middleOld).takeIf { middleOld.isNotEmpty() },
                Run(Kind.Insert, middleNew).takeIf { middleNew.isNotEmpty() },
            )
        } else {
            runs(middleOld, middleNew)
        }
        return buildList {
            if (prefix > 0) add(Run(Kind.Context, old.subList(0, prefix)))
            addAll(middle)
            if (suffix > 0) add(Run(Kind.Context, old.subList(old.size - suffix, old.size)))
        }
    }

    /** LCS over the changed middle, removals before additions where both change. */
    private fun runs(old: List<Line>, new: List<Line>): List<Run> {
        val n = old.size
        val m = new.size
        val table = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            table[i][j] = if (old[i] == new[j]) table[i + 1][j + 1] + 1 else maxOf(table[i + 1][j], table[i][j + 1])
        }
        val out = ArrayList<Run>()
        fun append(kind: Kind, line: Line) {
            val last = out.lastOrNull()
            if (last != null && last.kind == kind) out[out.lastIndex] = last.copy(lines = last.lines + line)
            else out += Run(kind, listOf(line))
        }
        var i = 0
        var j = 0
        while (i < n || j < m) {
            when {
                i < n && j < m && old[i] == new[j] -> { append(Kind.Context, old[i]); i++; j++ }
                j >= m || (i < n && table[i + 1][j] >= table[i][j + 1]) -> { append(Kind.Delete, old[i]); i++ }
                else -> { append(Kind.Insert, new[j]); j++ }
            }
        }
        return out
    }

    private fun hunks(input: List<Run>, context: Int): List<Hunk> {
        val runs = input + Run(Kind.Context, emptyList())
        val hunks = ArrayList<Hunk>()
        var oldStart = 0
        var newStart = 0
        var oldLine = 1
        var newLine = 1
        var current: MutableList<HunkLine> = ArrayList()
        for ((index, run) in runs.withIndex()) {
            if (run.kind != Kind.Context) {
                if (oldStart == 0) {
                    oldStart = oldLine
                    newStart = newLine
                    if (index > 0) {
                        current = runs[index - 1].lines.takeLast(context).map { HunkLine(" ", it) }.toMutableList()
                        oldStart -= current.size
                        newStart -= current.size
                    }
                }
                val sign = if (run.kind == Kind.Insert) "+" else "-"
                run.lines.forEach { current += HunkLine(sign, it) }
                if (run.kind == Kind.Insert) newLine += run.lines.size else oldLine += run.lines.size
                continue
            }
            if (oldStart != 0) {
                if (run.lines.size <= context * 2 && index < runs.size - 2) {
                    run.lines.forEach { current += HunkLine(" ", it) }
                } else {
                    val size = minOf(run.lines.size, context)
                    run.lines.take(size).forEach { current += HunkLine(" ", it) }
                    hunks += Hunk(oldStart, oldLine - oldStart + size, newStart, newLine - newStart + size, current)
                    oldStart = 0
                    newStart = 0
                    current = ArrayList()
                }
            }
            oldLine += run.lines.size
            newLine += run.lines.size
        }
        return hunks
    }
}
