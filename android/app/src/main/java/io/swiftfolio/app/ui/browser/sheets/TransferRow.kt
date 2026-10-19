package io.swiftfolio.app.ui.browser.sheets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.R
import io.swiftfolio.app.transfer.TransferItem
import io.swiftfolio.app.transfer.TransferItemStatus
import io.swiftfolio.app.transfer.TransferKind
import io.swiftfolio.app.ui.browser.TransferRowModel
import io.swiftfolio.app.ui.components.FileTypeStyles
import io.swiftfolio.app.ui.components.FileTypeTile
import io.swiftfolio.app.ui.components.StatusChip
import io.swiftfolio.app.ui.components.TileMode
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.ByteFormat
import io.swiftfolio.core.FileKind

private val RowBarHeight = 3.dp
private val DoneIconSize = 20.dp
private const val DONE_ALPHA = 0.6f

/** Per-file row: tile, mono name and size, status (chip, bar, tick) and a cancel button. */
@Composable
internal fun TransferRow(row: TransferRowModel, onCancel: () -> Unit) {
    val colors = ConsoleTheme.colors
    val item = row.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.fileRowMinHeight)
            .alpha(if (item.status == TransferItemStatus.DONE) DONE_ALPHA else 1f)
            .padding(start = Spacing.space6, end = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
    ) {
        FileTypeTile(style = FileTypeStyles.of(FileKind.of(item.name)), mode = TileMode.TYPE)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
            Text(
                text = item.name,
                style = ConsoleTheme.mono.row,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (item.status == TransferItemStatus.CANCELLED) TextDecoration.LineThrough else null,
            )
            StatusLine(item)
        }
        when {
            item.status == TransferItemStatus.DONE ->
                Icon(Icons.Rounded.CheckCircle, stringResource(doneLabel(row.kind)), tint = colors.success, modifier = Modifier.size(DoneIconSize))
            !item.status.isTerminal -> IconButton(onClick = onCancel) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel_transfer, item.name), tint = colors.textSecondary)
            }
        }
    }
}

@StringRes
private fun doneLabel(kind: TransferKind): Int = when (kind) {
    TransferKind.UPLOAD -> R.string.upload_done
    TransferKind.DOWNLOAD -> R.string.download_done
}

@Composable
private fun StatusLine(item: TransferItem) {
    val colors = ConsoleTheme.colors
    val size = item.sizeBytes?.let { ByteFormat.format(it) }.orEmpty()
    when (item.status) {
        TransferItemStatus.QUEUED -> StatusChip(stringResource(R.string.status_queued))
        TransferItemStatus.RUNNING -> {
            val total = item.sizeBytes ?: 0L
            LinearProgressIndicator(
                progress = { if (total > 0) (item.bytesDone.toFloat() / total).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(RowBarHeight),
                color = colors.accentStart,
                trackColor = colors.glass1,
            )
            Text("${ByteFormat.percent(item.bytesDone, total)}% · $size", style = ConsoleTheme.mono.meta, color = colors.textTertiary)
        }
        TransferItemStatus.FAILED -> StatusChip(stringResource(R.string.status_failed, item.errorCode.orEmpty()), textColor = colors.error)
        TransferItemStatus.DONE, TransferItemStatus.CANCELLED -> Text(size, style = ConsoleTheme.mono.meta, color = colors.textTertiary)
    }
}
