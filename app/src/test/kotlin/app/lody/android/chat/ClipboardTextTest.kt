package app.lody.android.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardTextTest {
    @Test
    fun `an ascii payload at the character limit is allowed`() {
        assertFalse(clipboardTextTooLarge("x".repeat(MaxClipboardChars)))
    }

    @Test
    fun `more than the character limit is refused`() {
        assertTrue(clipboardTextTooLarge("x".repeat(MaxClipboardChars + 1)))
    }

    @Test
    fun `cjk bytes at the binder budget are allowed`() {
        assertFalse(clipboardTextTooLarge("字".repeat(300_000)))
    }

    @Test
    fun `cjk bytes above the binder budget are refused`() {
        assertTrue(clipboardTextTooLarge("字".repeat(300_001)))
    }
}
