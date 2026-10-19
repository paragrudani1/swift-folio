package io.swiftfolio.app.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.R
import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.app.ui.ErrorTexts
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.ConnectError

private val MarkSize = 28.dp
private val MarkShape = RoundedCornerShape(8.dp)
private val StepIconSize = 16.dp
private val StepSpinnerStroke = 2.dp
private val BannerIconSize = 20.dp
private const val ERROR_TINT_ALPHA = 0.12f
private const val ERROR_BORDER_ALPHA = 0.40f

/** Theme toggle table: icon and accessible label for each mode. */
private val themeToggle: Map<ThemeMode, Pair<ImageVector, Int>> = mapOf(
    ThemeMode.SYSTEM to (Icons.Rounded.BrightnessAuto to R.string.theme_toggle_system),
    ThemeMode.LIGHT to (Icons.Rounded.LightMode to R.string.theme_toggle_light),
    ThemeMode.DARK to (Icons.Rounded.DarkMode to R.string.theme_toggle_dark),
)

/** Checklist labels per step. */
private val stepLabels: Map<ConnectStep, Int> = mapOf(
    ConnectStep.REGION to R.string.connect_step_region,
    ConnectStep.SIGNING to R.string.connect_step_signing,
    ConnectStep.LISTING to R.string.connect_step_listing,
)

/** Accessible status of a checklist line (the icon alone would rely on colour/shape). */
private val stepStatusLabels: Map<StepStatus, Int> = mapOf(
    StepStatus.PENDING to R.string.step_status_pending,
    StepStatus.RUNNING to R.string.step_status_running,
    StepStatus.DONE to R.string.step_status_done,
    StepStatus.FAILED to R.string.step_status_failed,
)

/** App mark on the left, theme cycle toggle on the right. */
@Composable
internal fun AppMarkRow(themeMode: ThemeMode, onCycleTheme: () -> Unit) {
    val colors = ConsoleTheme.colors
    val (icon, label) = themeToggle.getValue(themeMode)
    Row(Modifier.fillMaxWidth().height(Spacing.touchTarget), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(MarkSize).background(colors.accentBrush, MarkShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(StepIconSize))
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onCycleTheme) {
            Icon(icon, contentDescription = stringResource(label), tint = colors.textSecondary)
        }
    }
}

/**
 * Error card with the AWS code in mono and, for a wrong region, the one-tap fix
 * "Switch to <region>" (borrowed from Squishy Bucket).
 */
@Composable
internal fun ConnectErrorBanner(error: ConnectError, onApplyRegion: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ConsoleTheme.colors
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.glass2, shape)
            .background(colors.error.copy(alpha = ERROR_TINT_ALPHA), shape)
            .border(Spacing.hairline, colors.error.copy(alpha = ERROR_BORDER_ALPHA), shape)
            .padding(start = Spacing.space4, top = Spacing.space2, bottom = Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(BannerIconSize))
            Spacer(Modifier.size(Spacing.space2))
            Text(ErrorTexts.title(error), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_dismiss), tint = colors.textSecondary)
            }
        }
        Text(error.code, style = ConsoleTheme.mono.meta, color = colors.textSecondary)
        Text(ErrorTexts.hint(error), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(end = Spacing.space4))
        error.suggestedRegion?.let { region ->
            FilledTonalButton(onClick = onApplyRegion, modifier = Modifier.padding(top = Spacing.space2)) {
                Text(stringResource(R.string.connect_switch_region, region))
            }
        }
    }
}

/** Live mono checklist; each line ticks when its request completes. */
@Composable
internal fun ConnectChecklist(steps: List<StepRow>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        steps.forEach { row -> ChecklistLine(row) }
    }
}

@Composable
private fun ChecklistLine(row: StepRow) {
    val colors = ConsoleTheme.colors
    val statusLabel = stringResource(stepStatusLabels.getValue(row.status))
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = statusLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
    ) {
        when (row.status) {
            StepStatus.PENDING -> Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = colors.textTertiary, modifier = Modifier.size(StepIconSize))
            StepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(StepIconSize), strokeWidth = StepSpinnerStroke, color = colors.accentText)
            StepStatus.DONE -> Icon(Icons.Rounded.CheckCircle, null, tint = colors.success, modifier = Modifier.size(StepIconSize))
            StepStatus.FAILED -> Icon(Icons.Rounded.Cancel, null, tint = colors.error, modifier = Modifier.size(StepIconSize))
        }
        val textColor = if (row.status == StepStatus.FAILED) colors.error else colors.textSecondary
        Text(stringResource(stepLabels.getValue(row.step)), style = ConsoleTheme.mono.meta, color = textColor)
    }
}
