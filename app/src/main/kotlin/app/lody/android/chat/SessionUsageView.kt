package app.lody.android.chat

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import app.lody.android.R
import app.lody.android.designsystem.*
import app.lody.android.runtime.SessionUsage
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
internal fun SessionUsageButton(usage: SessionUsage?) {
    if (usage == null || !usage.visible) return
    var open by remember { mutableStateOf(false) }
    val colors = LodyTheme.colors
    val percent = (usage.triggerPercent ?: 0.0).takeIf { it.isFinite() } ?: 0.0
    val label = if (usage.compacting) stringResource(R.string.usage_compacting)
        else stringResource(R.string.usage_open, percent.roundToInt())
    Box {
        // Content-sized with iOS's 2 pt leading inset, so the ring sits close to the agent mark;
        // the trailing room keeps the existing gap to Send.
        Row(
            modifier = Modifier
                .heightIn(min = LodySize.controlMd)
                .clip(RoundedCornerShape(LodyRadius.md))
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = label }
                .padding(start = LodySpacing.xxs, end = LodySpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Compacting keeps the ring and percentage's width, as iOS reserves usageWidth, and
            // swaps only what is drawn for a spinner.
            Box(contentAlignment = Alignment.Center) {
                Row(
                    modifier = if (usage.compacting) Modifier.clearAndSetSemantics {} else Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LodySpacing.xs),
                ) {
                    UsageRing((percent / 100).toFloat(), Modifier.alpha(if (usage.compacting) 0f else 1f))
                    Text(
                        stringResource(R.string.usage_percent, percent.roundToInt()),
                        style = LodyTheme.typography.captionRegular, color = colors.textTertiary,
                        modifier = Modifier.alpha(if (usage.compacting) 0f else 1f),
                    )
                }
                if (usage.compacting) CircularProgressIndicator(Modifier.size(LodyIconSize.sm), color = colors.textTertiary, strokeWidth = LodyStroke.focus)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(Modifier.widthIn(max = LodySize.controlLg * 7).padding(LodySpacing.md), verticalArrangement = Arrangement.spacedBy(LodySpacing.sm)) {
                if (usage.compacting) Text(stringResource(R.string.usage_compacting), style = LodyTheme.typography.caption)
                else usage.context?.let { context ->
                    UsageMeter(stringResource(R.string.usage_context), context.percent)
                    val numbers = NumberFormat.getNumberInstance()
                    Text(stringResource(R.string.usage_tokens, numbers.format(context.used), numbers.format(context.size)), style = LodyTheme.typography.caption)
                }
                usage.quota?.let { quota ->
                    if (usage.context != null || usage.compacting) HorizontalDivider()
                    UsageQuotaView(quota)
                }
            }
        }
    }
}

/** iOS's ChatUsageRing: one continuous track with the round-capped arc drawn over it, so the ring has no break at the arc's ends. */
@Composable
private fun UsageRing(percent: Float, modifier: Modifier = Modifier) {
    val colors = LodyTheme.colors
    Canvas(
        modifier.size(LodyIconSize.sm).semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(percent.coerceIn(0f, 1f), 0f..1f)
        },
    ) {
        val stroke = Stroke(width = LodyStroke.focus.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2f
        // iOS draws the ring as a template tinted by the button's secondaryLabel foreground: the
        // track is that ink at 20%, the arc is the ink itself.
        drawCircle(color = colors.textTertiary.copy(alpha = 0.2f), radius = size.minDimension / 2f - inset, style = stroke)
        if (percent > 0f) {
            drawArc(
                color = colors.textTertiary, startAngle = -90f,
                sweepAngle = 360f * percent.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset), size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke,
            )
        }
    }
}

@Composable
private fun UsageMeter(label: String, percent: Double) {
    val used = stringResource(R.string.usage_used, percent.roundToInt())
    Column(verticalArrangement = Arrangement.spacedBy(LodySpacing.xxs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = LodyTheme.typography.caption, modifier = Modifier.weight(1f))
            Text(used, style = LodyTheme.typography.caption)
        }
        LinearProgressIndicator(progress = { (percent / 100).toFloat() }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $used" }, color = LodyTheme.colors.textSecondary, trackColor = LodyTheme.colors.fillSecondary)
    }
}

@Composable
internal fun ResetTime(epochSeconds: Double) {
    var absolute by remember(epochSeconds) { mutableStateOf(false) }
    val millis = (epochSeconds * 1000).toLong()
    val time = if (absolute) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
        else DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.usage_resets, time), style = LodyTheme.typography.caption, modifier = Modifier.weight(1f))
        TextButton(onClick = { absolute = !absolute }) {
            Text(stringResource(if (absolute) R.string.usage_relative else R.string.usage_absolute), style = LodyTheme.typography.caption)
        }
    }
}

/** "Weekly", "5 hours" or "3d"; a window without a duration is the plan's usage. */
@Composable
internal fun usageWindowName(duration: Double?): String = when (duration) {
    604800.0 -> stringResource(R.string.usage_weekly)
    18000.0 -> stringResource(R.string.usage_five_hours)
    null -> stringResource(R.string.usage_title)
    else -> usageWindowShortName(duration)
}

/** "5h", "7d" or "30m", as the compact quota line on Web. */
@Composable
internal fun usageWindowShortName(seconds: Double): String {
    val unit = if (seconds % 86400 == 0.0) 86400 else if (seconds % 3600 == 0.0) 3600 else 60
    return stringResource(if (unit == 86400) R.string.usage_days else if (unit == 3600) R.string.usage_hours else R.string.usage_minutes, NumberFormat.getNumberInstance().format(seconds / unit))
}

@Composable
internal fun UsageQuotaView(quota: app.lody.android.runtime.UsageQuota) {
    Column(verticalArrangement = Arrangement.spacedBy(LodySpacing.sm)) {
        Text(quota.name ?: stringResource(R.string.usage_model), style = LodyTheme.typography.subhead)
        if (quota.windows.isEmpty()) Text(stringResource(R.string.usage_unavailable), style = LodyTheme.typography.caption)
        quota.windows.sortedByDescending { it.duration ?: 0.0 }.forEach { window ->
            UsageMeter(listOfNotNull(usageWindowName(window.duration), window.label).joinToString(" · "), window.usedPercent)
            window.resetsAt?.let { ResetTime(it) }
        }
    }
}
