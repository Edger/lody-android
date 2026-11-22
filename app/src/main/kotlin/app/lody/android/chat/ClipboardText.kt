package app.lody.android.chat

/**
 * A clipboard write reserves room in the binder transaction shared with the system server, so a
 * multi-megabyte file or patch can throw `TransactionTooLargeException` and kill the app.
 */
internal const val MaxClipboardChars = 500_000

/** The binder buffer is about 1 MB for the whole transaction; stay well below it in UTF-8 bytes. */
internal const val MaxClipboardBytes = 900_000

/** True when [text] is too large to hand to the clipboard as one plain-text clip. */
internal fun clipboardTextTooLarge(text: String): Boolean {
    if (text.length > MaxClipboardChars) return true
    var bytes = 0
    var index = 0
    while (index < text.length) {
        val char = text[index]
        bytes += when {
            char.code < 0x80 -> 1
            char.code < 0x800 -> 2
            char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate() -> {
                index++
                4
            }
            else -> 3
        }
        if (bytes > MaxClipboardBytes) return true
        index++
    }
    return false
}
