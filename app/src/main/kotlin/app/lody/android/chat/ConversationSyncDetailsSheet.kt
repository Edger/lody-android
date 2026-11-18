package app.lody.android.chat

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.lody.android.R
import app.lody.android.designsystem.LodySpacing
import app.lody.android.designsystem.LodyTheme
import app.lody.android.runtime.ConversationSyncDetails
import app.lody.android.runtime.ConversationSyncDetails.Phase
import kotlinx.coroutines.delay

@get:StringRes
internal val Phase.label: Int
    get() = when (this) {
        Phase.Connecting -> R.string.chat_sync_connecting
        Phase.Syncing -> R.string.chat_sync_syncing
        Phase.Retrying -> R.string.chat_sync_retrying
        Phase.Disconnected -> R.string.chat_sync_disconnected
        Phase.Confirmed -> R.string.chat_sync_confirmed
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationSyncDetailsSheet(details: ConversationSyncDetails, machineName: String, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LodyTheme.colors.bgBase,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = LodySpacing.xl, vertical = LodySpacing.lg)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(LodySpacing.lg),
        ) {
            Text(stringResource(R.string.chat_sync_title), style = LodyTheme.typography.body, color = LodyTheme.colors.textPrimary)
            if (details.phase != Phase.Confirmed) SyncWait(details.since)
            SyncStep(
                details.document,
                if (details.history == null) R.string.chat_sync_messages else R.string.chat_sync_session_state,
            )
            details.history?.let {
                SyncStep(it, R.string.chat_sync_messages, machineName.ifBlank { stringResource(R.string.chat_sync_target) })
            }
        }
    }
}

@Composable
private fun SyncWait(since: Long) {
    var now by remember(since) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    Text(
        stringResource(R.string.chat_sync_waiting, (now - since).coerceAtLeast(0) / 1_000),
        style = LodyTheme.typography.footnote,
        color = LodyTheme.colors.textSecondary,
    )
}

@Composable
private fun SyncStep(step: ConversationSyncDetails.Step, @StringRes label: Int, machineName: String? = null) {
    val colors = LodyTheme.colors
    val typography = LodyTheme.typography
    val device = stringResource(R.string.chat_sync_device)
    val server = stringResource(R.string.chat_sync_server)
    val nodes = listOfNotNull(device, server, machineName)
    val request = nodes.joinToString(" → ")
    val updates = nodes.asReversed().joinToString(" → ")
    val reason = when (step.reason) {
        "timeout" -> R.string.chat_sync_timeout
        "network" -> R.string.chat_sync_network
        "authorization" -> R.string.chat_sync_authorization
        "owner" -> R.string.chat_sync_owner
        "metadata" -> R.string.chat_sync_metadata
        "stream_closed" -> R.string.chat_sync_stream_closed
        "stalled_cursor" -> R.string.chat_sync_stalled_cursor
        "session_limit" -> R.string.chat_sync_limit
        null -> null
        else -> R.string.chat_sync_read
    }
    Column(verticalArrangement = Arrangement.spacedBy(LodySpacing.xs)) {
        Text(stringResource(label), style = typography.body, color = colors.textPrimary)
        Text(stringResource(R.string.chat_sync_request, request), style = typography.footnote, color = colors.textPrimary)
        Text(stringResource(R.string.chat_sync_return_route, updates), style = typography.footnote, color = colors.textPrimary)
        Text(stringResource(step.phase.label), style = typography.footnote, color = colors.textSecondary)
        if (reason != null) Text(stringResource(reason), style = typography.footnote, color = colors.textSecondary)
    }
}
