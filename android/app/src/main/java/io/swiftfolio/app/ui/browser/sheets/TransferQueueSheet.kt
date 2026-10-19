// pluralStringResource was introduced as experimental; opting in is harmless if it is stable.
@file:OptIn(ExperimentalComposeUiApi::class)

package io.swiftfolio.app.ui.browser.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.browser.BrowserAction
import io.swiftfolio.app.ui.browser.QueueHeading
import io.swiftfolio.app.ui.browser.TransferQueueSummary
import io.swiftfolio.app.ui.components.StatusChip
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.ByteFormat
import io.swiftfolio.core.TransferProgress

private val OverallBarHeight = 6.dp
private val QueueMaxHeight = 360.dp

/** Transfer queue: heading, overall progress, per-file rows (uploads and downloads), and queue actions. */
@Composable
internal fun TransferQueueSheet(
    queue: TransferQueueSummary,
    onAction: (BrowserAction) -> Unit,
    onPickFiles: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConsoleSheet(onDismiss = onDismiss) {
        QueueHeader(queue.heading, canCancel = queue.isActive, onCancelAll = { onAction(BrowserAction.CancelAllTransfers) })
        OverallProgress(queue.progress)
        LazyColumn(Modifier.heightIn(max = QueueMaxHeight)) {
            items(queue.rows, key = { "${it.batchId}-${it.index}" }) { row ->
                TransferRow(row, onCancel = { onAction(BrowserAction.CancelTransfer(row.batchId, row.index)) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPickFiles) { Text(stringResource(R.string.add_more_files)) }
            Spacer(Modifier.weight(1f))
            if (queue.hasFinished) {
                TextButton(onClick = { onAction(BrowserAction.ClearFinishedTransfers) }) { Text(stringResource(R.string.clear_finished)) }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.hide)) }
        }
    }
}

@Composable
private fun QueueHeader(heading: QueueHeading, canCancel: Boolean, onCancelAll: () -> Unit) {
    val colors = ConsoleTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.space6, end = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
    ) {
        val title = when (heading) {
            is QueueHeading.UploadingTo -> R.string.uploading_to
            QueueHeading.Downloading -> R.string.queue_downloads
            QueueHeading.Transfers -> R.string.queue_transfers
        }
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        if (heading is QueueHeading.UploadingTo) {
            StatusChip(text = "/" + heading.prefix, modifier = Modifier.weight(1f, fill = false), textColor = colors.textPrimary)
        }
        Spacer(Modifier.weight(1f))
        if (canCancel) {
            TextButton(onClick = onCancelAll) { Text(stringResource(R.string.cancel_all), color = colors.error) }
        }
    }
}

@Composable
private fun OverallProgress(progress: TransferProgress) {
    val colors = ConsoleTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.space6, vertical = Spacing.space3)) {
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth().height(OverallBarHeight),
            color = colors.accentStart,
            trackColor = colors.glass1,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(Spacing.space2))
        Text(
            text = stringResource(
                R.string.upload_stats,
                ByteFormat.format(progress.bytesDone),
                ByteFormat.format(progress.bytesTotal),
                pluralStringResource(R.plurals.files_progress, progress.filesTotal, progress.filesDone, progress.filesTotal),
            ),
            style = ConsoleTheme.mono.meta,
            color = colors.textSecondary,
        )
    }
}
