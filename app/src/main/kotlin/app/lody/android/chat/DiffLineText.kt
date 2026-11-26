package app.lody.android.chat

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints

/**
 * Largest run of one wrapped diff line handed to a single `Text`. A generated or minified line can
 * be hundreds of kilobytes; one `Text` builds one `StaticLayout` for it on the main thread, and
 * 280k characters stalled the UI for ~21 s and ANR'd a physical device. Bounded runs keep each
 * layout cheap while still showing every character.
 */
internal const val MaxWrapChunkChars = 2_000

/**
 * Extra characters measured past [MaxWrapChunkChars] so the layout has line breaks beyond the cut.
 * A monospace code line holds well under this many characters at any phone width.
 */
internal const val WrapChunkProbeChars = 512

/** Most runs one wrapped line may become; past this the tail is replaced by a hint. */
internal const val MaxWrapChunks = 500

/**
 * Longest wrapped line drawn in full, sized for about [MaxWrapChunks] runs. A cut lands on a line
 * start a little below [MaxWrapChunkChars], so the run count can exceed [MaxWrapChunks] by the
 * lines that fit in one probe; the last run also carries the truncation hint.
 */
internal const val MaxWrappedLineChars = (MaxWrapChunks - 1) * MaxWrapChunkChars

/** Caps one wrapped line so a multi-megabyte line cannot become thousands of main-thread layouts. */
internal fun cappedWrappedLine(text: String, hint: String): String =
    if (text.length <= MaxWrappedLineChars) text else text.take(MaxWrappedLineChars) + "… " + hint

/** The [count] longest shown lines; source-width measurement only needs the widest candidates. */
internal fun widestLines(result: LineDiff.Result, count: Int = 32): List<String> =
    result.hunks.asSequence()
        .flatMap { it.lines.asSequence() }
        .map { it.text }
        .sortedByDescending { it.length }
        .take(count)
        .toList()

/**
 * Cuts one wrapped line into runs of at most [size] characters. The runs rejoin seamlessly except
 * for the forced line break at each boundary, which lands mid-line and is invisible in wrapped
 * code. A boundary never splits a surrogate pair, which would render as two broken glyphs.
 */
internal fun wrapChunks(text: String, size: Int = MaxWrapChunkChars): List<String> {
    require(size > 0) { "size must be positive" }
    if (text.length <= size) return listOf(text)
    val chunks = ArrayList<String>((text.length + size - 1) / size)
    var start = 0
    while (start < text.length) {
        var end = minOf(start + size, text.length)
        // Keep a surrogate pair in one run, or each half renders as a replacement glyph.
        if (end < text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end++
        chunks += text.substring(start, end)
        start = end
    }
    return chunks
}

/**
 * Cuts a long line into runs that each start where the previous run left a line: the layout of a
 * short probe shows the line starts, and the largest one before [target] becomes the cut. Rendering
 * the runs separately therefore reproduces the single layout's breaks, with no short line at a
 * chunk seam. Falls back to [wrapChunks] when no width is available.
 */
internal fun lineBoundaryChunks(
    text: String,
    widthPx: Int,
    style: TextStyle,
    measurer: TextMeasurer,
    target: Int = MaxWrapChunkChars,
): List<String> {
    if (text.length <= target || widthPx <= 0) return wrapChunks(text, target)
    val chunks = ArrayList<String>()
    var start = 0
    while (text.length - start > target) {
        val probe = text.substring(start, minOf(start + target + WrapChunkProbeChars, text.length))
        val layout = measurer.measure(
            text = AnnotatedString(probe),
            style = style,
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            constraints = Constraints(maxWidth = widthPx),
            skipCache = true,
        )
        // The cut is a line start of the probe, so the next run begins exactly where that line did.
        var cut = 0
        for (line in 1 until layout.lineCount) {
            val offset = layout.getLineStart(line)
            if (offset in 1..target) cut = offset
        }
        val step = if (cut > 0) cut else target
        chunks += text.substring(start, start + step)
        start += step
    }
    chunks += text.substring(start)
    return chunks
}
