package app.lody.android.chat

import android.content.Context

/** The diff page's two layout choices, persisted globally like iOS `diffStyle`. */
internal enum class DiffStyle(val value: String) {
    Unified("unified"),
    Split("split"),
}

/**
 * The diff page's presentation preferences. iOS stores the style in `LocalStore` under `diffStyle`
 * and starts unified; Android keeps the same two keys beside the composer drafts. Wrapping is on by
 * default so the page opens the way it always read.
 */
internal object DiffPreferences {
    private const val FILE = "lody.chat"
    private const val STYLE = "diff_style"
    private const val WRAP = "diff_wrap"

    fun style(context: Context): DiffStyle {
        val stored = preferences(context).getString(STYLE, null)
        return DiffStyle.entries.firstOrNull { it.value == stored } ?: DiffStyle.Unified
    }

    fun setStyle(context: Context, style: DiffStyle) {
        preferences(context).edit().putString(STYLE, style.value).apply()
    }

    fun wrap(context: Context): Boolean = preferences(context).getBoolean(WRAP, true)

    fun setWrap(context: Context, wrap: Boolean) {
        preferences(context).edit().putBoolean(WRAP, wrap).apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
