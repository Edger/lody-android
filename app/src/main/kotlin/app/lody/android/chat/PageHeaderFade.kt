package app.lody.android.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import app.lody.android.designsystem.LodySize
import app.lody.android.designsystem.LodyTheme

/**
 * Page colour over a floating header, fading out below it so content reads beneath. Shared by the
 * conversation header and the changed-files sheets so the two treatments cannot drift.
 */
@Composable
internal fun Modifier.pageHeaderFade(): Modifier {
    val base = LodyTheme.colors.bgBase
    val fade = LodySize.headerFade
    return drawBehind {
        val fadePx = fade.toPx()
        drawRect(
            Brush.verticalGradient(
                0f to base,
                size.height / (size.height + fadePx) to base.copy(alpha = 0.92f),
                1f to base.copy(alpha = 0f),
                endY = size.height + fadePx,
            ),
            size = Size(size.width, size.height + fadePx),
        )
    }
}
