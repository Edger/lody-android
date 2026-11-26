package app.lody.android.chat

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.createBitmap
import app.lody.android.R
import app.lody.android.designsystem.LodyIcons
import app.lody.android.designsystem.LodySpacing
import app.lody.android.designsystem.LodyTheme
import app.lody.android.runtime.RuntimeScope
import app.lody.android.runtime.TurnImage
import app.lody.android.runtime.TurnItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** The states the developer gallery previews for one transcript picture. */
internal enum class DebugImageState { Loaded, Loading, Failed }

/** The preview state a debug picture asks for, from its session image id. */
internal fun debugImageState(id: String): DebugImageState = when {
    id.startsWith("debug-loading") -> DebugImageState.Loading
    id.startsWith("debug-failed") -> DebugImageState.Failed
    else -> DebugImageState.Loaded
}

/**
 * Settings → developer mode → Image states: the transcript's picture controls in every state,
 * backed by a fake [SessionImageReader] so no account, network or session is involved. Loaded
 * pictures open the ordinary full-screen viewer; a failed tile's caption retries.
 */
@Composable
internal fun ImageDebugScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val colors = LodyTheme.colors
    val accent = colors.accentDefault.toArgb()
    val sunken = colors.bgSunken.toArgb()
    val overlay = Color.White.copy(alpha = 0.16f).toArgb()
    val source = remember(accent, sunken, overlay) {
        SessionImageSource(
            SessionImageReader { _, _, image, _ -> debugImageBytes(image, accent, sunken, overlay) },
            RuntimeScope("debug-account", "debug-workspace"),
            "debug-session",
        )
    }
    val loadedName = stringResource(R.string.debug_images_loaded)
    val loadingName = stringResource(R.string.debug_images_loading)
    val failedName = stringResource(R.string.debug_images_failed)
    fun image(id: String, name: String) = TurnImage(id, name, null, 600, 400)
    val tileLoaded = image("debug-loaded-tile", loadedName)
    val tileLoading = image("debug-loading-tile", loadingName)
    val tileFailed = image("debug-failed-tile", failedName)
    val agentLoaded = image("debug-loaded-agent", loadedName)
    val agentLoading = image("debug-loading-agent", loadingName)
    val agentFailed = image("debug-failed-agent", failedName)
    var viewer by remember { mutableStateOf<TurnImage?>(null) }

    Scaffold(containerColor = colors.bgBase, topBar = {
        Row(
            Modifier.fillMaxWidth().background(colors.bgBase).statusBarsPadding().padding(horizontal = LodySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(LodyIcons.Back, stringResource(R.string.settings_agent_back)) }
            Text(stringResource(R.string.debug_images_title), Modifier.weight(1f), style = LodyTheme.typography.headline, color = colors.textPrimary)
        }
    }) { padding ->
        CompositionLocalProvider(LocalSessionImages provides source) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                item {
                    Text(
                        stringResource(R.string.debug_images_hint),
                        style = LodyTheme.typography.footnote, color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = LodySpacing.xxl, vertical = LodySpacing.sm),
                    )
                }
                item {
                    DebugHeading(stringResource(R.string.debug_images_compact))
                    DebugStateRow(loadedName) { AttachmentImageTile(tileLoaded, onClick = { viewer = tileLoaded }) }
                    DebugStateRow(loadingName) { AttachmentImageTile(tileLoading, onClick = {}) }
                    DebugStateRow(failedName) { AttachmentImageTile(tileFailed, onClick = {}) }
                }
                item {
                    DebugHeading(stringResource(R.string.debug_images_agent))
                    DebugLabel(loadedName)
                    TurnImages(TurnItem.Images("debug-agent-loaded", listOf(agentLoaded)))
                    DebugLabel(loadingName)
                    TurnImages(TurnItem.Images("debug-agent-loading", listOf(agentLoading)))
                    DebugLabel(failedName)
                    TurnImages(TurnItem.Images("debug-agent-failed", listOf(agentFailed)))
                }
                item {
                    DebugHeading(stringResource(R.string.debug_images_stack))
                    TurnImages(TurnItem.Images("debug-stack", listOf(
                        image("debug-loaded-stack-1", loadedName),
                        image("debug-loaded-stack-2", loadedName),
                        image("debug-loaded-stack-3", loadedName),
                    )))
                }
                item { Spacer(Modifier.height(LodySpacing.xxl)) }
            }
            viewer?.let { image -> ImageViewer(listOf(image), 0) { viewer = null } }
        }
    }
}

@Composable
private fun DebugHeading(title: String) {
    Text(
        title,
        style = LodyTheme.typography.footnote, color = LodyTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = LodySpacing.xxl, vertical = LodySpacing.md),
    )
}

@Composable
private fun DebugLabel(label: String) {
    Text(
        label,
        style = LodyTheme.typography.caption, color = LodyTheme.colors.textTertiary,
        modifier = Modifier.padding(start = LodySpacing.xxl, top = LodySpacing.sm),
    )
}

@Composable
private fun DebugStateRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = LodySpacing.lg, vertical = LodySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LodySpacing.md),
    ) {
        Text(label, Modifier.weight(1f), style = LodyTheme.typography.body, color = LodyTheme.colors.textSecondary)
        content()
    }
}

/** The fake endpoint behind every preview: suspend for loading, fail, or draw a sample. */
private suspend fun debugImageBytes(image: TurnImage, accent: Int, sunken: Int, overlay: Int): ByteArray =
    when (debugImageState(image.id)) {
        DebugImageState.Loading -> awaitCancellation()
        DebugImageState.Failed -> throw SessionImageFailure("debug")
        DebugImageState.Loaded -> withContext(Dispatchers.Default) { sampleImage(image.id, accent, sunken, overlay) }
    }

/** A 600×400 gradient whose circles move per id, so a stack's pictures differ. */
private fun sampleImage(id: String, accent: Int, sunken: Int, overlay: Int): ByteArray {
    val width = 600
    val height = 400
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), accent, sunken, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null
    paint.color = overlay
    val shift = (id.hashCode() and Int.MAX_VALUE) % 3 * 48
    repeat(5) { index ->
        canvas.drawCircle((90 + index * 105).toFloat(), (120 + shift + index * 24).toFloat(), (34 + index * 7).toFloat(), paint)
    }
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.toByteArray()
    }
}
