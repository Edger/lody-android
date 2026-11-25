package app.lody.android.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.unit.dp

/**
 * The floating bar-item surface: overlay fill, hairline outline and the small shadow, the Android
 * stand-in for iOS `LodyGlass` capsules. Apply before padding so the surface wraps the content.
 */
@Composable
fun Modifier.lodyFloatingCapsule(
    elevation: LodyElevation = LodyElevations.sm,
    outline: Boolean = true,
): Modifier {
    val colors = LodyTheme.colors
    val surface = dropShadow(CircleShape, elevation.shadow())
        .clip(CircleShape)
        .background(colors.bgOverlay, CircleShape)
    return if (outline) surface.border(0.5.dp, colors.borderSubtle, CircleShape) else surface
}
