package app.lody.android.chat

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.lody.android.R
import app.lody.android.designsystem.LodyIconSize
import app.lody.android.designsystem.LodyIcons
import app.lody.android.designsystem.LodyRadius
import app.lody.android.designsystem.LodySize
import app.lody.android.designsystem.LodySpacing
import app.lody.android.designsystem.LodyStroke
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
                HorizontalDivider(color = LodyTheme.colors.borderSubtle)
                SyncStep(it, R.string.chat_sync_messages, machineName.ifBlank { stringResource(R.string.chat_sync_target) })
            }
            HorizontalDivider(color = LodyTheme.colors.borderSubtle)
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(LodySpacing.lg, Alignment.CenterHorizontally),
            ) {
                FlowLegend(LodyIcons.ArrowRight, R.string.chat_sync_request, request = true)
                FlowLegend(LodyIcons.ArrowLeft, R.string.chat_sync_return_route)
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
    Row(horizontalArrangement = Arrangement.spacedBy(LodySpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Icon(LodyIcons.Clock, null, Modifier.size(LodyIconSize.sm), tint = LodyTheme.colors.iconSecondary)
        Text(
            stringResource(R.string.chat_sync_waiting, (now - since).coerceAtLeast(0) / 1_000),
            style = LodyTheme.typography.footnote,
            color = LodyTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun SyncStep(step: ConversationSyncDetails.Step, @StringRes label: Int, machineName: String? = null) {
    val colors = LodyTheme.colors
    val typography = LodyTheme.typography
    val nodes = listOfNotNull(
        SyncNode(LodyIcons.Smartphone, stringResource(R.string.chat_sync_device)),
        SyncNode(LodyIcons.Server, stringResource(R.string.chat_sync_server)),
        machineName?.let { SyncNode(LodyIcons.Desktop, it) },
    )
    val (phaseIcon, phaseColor) = when (step.phase) {
        Phase.Confirmed -> LodyIcons.Check to colors.successDefault
        Phase.Retrying, Phase.Disconnected -> LodyIcons.Alert to colors.warningDefault
        Phase.Connecting, Phase.Syncing -> LodyIcons.Clock to colors.iconSecondary
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(LodySpacing.sm)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LodySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(label), Modifier.weight(1f), style = typography.body, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(LodySpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Icon(phaseIcon, null, Modifier.size(LodyIconSize.sm), tint = phaseColor)
                Text(stringResource(step.phase.label), style = typography.footnote, color = colors.textSecondary)
            }
        }
        SyncFlow(nodes)
        if (reason != null) Text(stringResource(reason), style = typography.footnote, color = colors.textSecondary)
    }
}

private data class SyncNode(val icon: ImageVector, val label: String)

@Composable
private fun SyncFlow(nodes: List<SyncNode>) {
    val colors = LodyTheme.colors
    val request = stringResource(R.string.chat_sync_request)
    val updates = stringResource(R.string.chat_sync_return_route)
    val outgoing = nodes.joinToString(" → ") { it.label }
    val incoming = nodes.asReversed().joinToString(" → ") { it.label }
    Row(
        Modifier.fillMaxWidth().padding(vertical = LodySpacing.xs).clearAndSetSemantics {
            role = Role.Image
            contentDescription = "$request $outgoing. $updates $incoming."
        },
    ) {
        nodes.forEachIndexed { index, node ->
            if (index > 0) FlowLink(Modifier.weight(0.65f))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LodySpacing.xs),
            ) {
                val tileShape = RoundedCornerShape(LodyRadius.lg)
                Box(
                    Modifier.width(LodySize.controlLg).height(LodySize.controlMd)
                        .background(colors.bgSunken, tileShape)
                        .border(LodyStroke.default, colors.borderSubtle, tileShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(node.icon, null, Modifier.size(LodyIconSize.md), tint = colors.iconPrimary)
                }
                Text(
                    node.label, style = LodyTheme.typography.footnote, color = colors.textPrimary,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FlowLink(modifier: Modifier = Modifier) {
    val request = LodyTheme.colors.accentDefault
    val returned = LodyTheme.colors.textSecondary
    Canvas(modifier.height(LodySize.controlMd)) {
        val tip = LodySpacing.xs.toPx()
        val left = LodySpacing.xxs.toPx()
        val right = size.width - left
        val upper = center.y - tip
        val lower = center.y + tip
        val weight = LodyStroke.strong.toPx()

        drawLine(request, Offset(left, upper), Offset(right, upper), weight, StrokeCap.Round)
        drawLine(request, Offset(right - tip, upper - tip), Offset(right, upper), weight, StrokeCap.Round)
        drawLine(request, Offset(right - tip, upper + tip), Offset(right, upper), weight, StrokeCap.Round)
        drawLine(
            returned, Offset(right, lower), Offset(left, lower), LodyStroke.default.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(tip, tip)),
        )
        drawLine(returned, Offset(left + tip, lower - tip), Offset(left, lower), weight, StrokeCap.Round)
        drawLine(returned, Offset(left + tip, lower + tip), Offset(left, lower), weight, StrokeCap.Round)
    }
}

@Composable
private fun FlowLegend(icon: ImageVector, @StringRes label: Int, request: Boolean = false) {
    val colors = LodyTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(LodySpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(LodyIconSize.sm), tint = if (request) colors.accentDefault else colors.iconSecondary)
        Text(stringResource(label), style = LodyTheme.typography.footnote, color = colors.textSecondary)
    }
}
