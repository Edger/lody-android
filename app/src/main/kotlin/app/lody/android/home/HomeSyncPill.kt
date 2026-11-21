// Web counterpart: `mobile/mobile-connection-status.tsx`; iOS: LodyKit `HomeSyncPill`.

package app.lody.android.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lody.android.R
import app.lody.android.designsystem.LodyElevations
import app.lody.android.designsystem.LodyIconSize
import app.lody.android.designsystem.LodyIcons
import app.lody.android.designsystem.LodyLayout
import app.lody.android.designsystem.LodySpacing
import app.lody.android.designsystem.LodySpring
import app.lody.android.designsystem.LodyTheme
import app.lody.android.designsystem.shadow
import kotlinx.coroutines.delay

/** Catalog connection as Home shows it: nothing while live. */
enum class HomeSyncState { Live, Syncing, Connecting }

/** What the pill shows; null while live. */
internal enum class HomeSyncShown { Syncing, Connecting, Recovered }

/** How long "Connected" stays after trouble clears, as on web. */
private const val RECOVERED_MILLIS = 1_200L

/**
 * The pill's shown state, owned by the screen so the workspace capsule can collapse while it is
 * up (iOS `InboxChrome.syncShown`): syncing / connecting, then a brief "Connected" flash.
 */
@Composable
internal fun rememberHomeSyncShown(state: HomeSyncState): HomeSyncShown? {
    var shown by remember { mutableStateOf<HomeSyncShown?>(null) }
    LaunchedEffect(state) {
        when (state) {
            HomeSyncState.Syncing -> shown = HomeSyncShown.Syncing
            HomeSyncState.Connecting -> shown = HomeSyncShown.Connecting
            HomeSyncState.Live -> if (shown != null && shown != HomeSyncShown.Recovered) {
                shown = HomeSyncShown.Recovered
                delay(RECOVERED_MILLIS)
                shown = null
            }
        }
    }
    return shown
}

/**
 * Centred floating status over the Home top bar. Syncing and reconnecting show a spinner (the
 * runtime keeps retrying, so a lost connection reads as "Connecting", never "Offline"), and a
 * recovery shows "Connected" briefly before the pill leaves. It takes no touches.
 */
@Composable
internal fun HomeSyncPill(shown: HomeSyncShown?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = shown,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(LodySpring.snappy()) + scaleIn(LodySpring.snappy(), initialScale = 0.9f) + slideInVertically(LodySpring.snappy()) { -it / 4 }) togetherWith
                (fadeOut(LodySpring.snappy()) + scaleOut(LodySpring.snappy(), targetScale = 0.9f) + slideOutVertically(LodySpring.snappy()) { -it / 4 })
        },
        contentAlignment = Alignment.Center,
        label = "sync",
    ) { current -> if (current != null) Pill(current) }
}

@Composable
private fun Pill(shown: HomeSyncShown) {
    val colors = LodyTheme.colors
    val (label, tint) = when (shown) {
        HomeSyncShown.Syncing -> stringResource(R.string.home_syncing) to colors.textSecondary
        HomeSyncShown.Connecting -> stringResource(R.string.home_connecting) to colors.textSecondary
        HomeSyncShown.Recovered -> stringResource(R.string.home_connected) to colors.successDefault
    }
    Row(
        modifier = Modifier
            .widthIn(max = LodyLayout.trailingMetaMaxWidth * 1.5f)
            .dropShadow(CircleShape, LodyElevations.sm.shadow())
            .background(colors.bgOverlay, CircleShape)
            .border(0.5.dp, colors.borderSubtle, CircleShape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = LodySpacing.md, vertical = LodySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LodySpacing.xs),
    ) {
        when (shown) {
            HomeSyncShown.Syncing, HomeSyncShown.Connecting -> app.lody.android.designsystem.LodyLoadingIndicator(Modifier.size(LodyIconSize.xs), color = colors.textSecondary)
            HomeSyncShown.Recovered -> Icon(LodyIcons.Check, contentDescription = null, tint = colors.successDefault, modifier = Modifier.size(LodyIconSize.xs))
        }
        Text(label, style = LodyTheme.typography.footnote, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
