package io.swiftfolio.app.ui.browser.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.browser.BrowserAction
import io.swiftfolio.app.ui.components.FileTypeStyles
import io.swiftfolio.app.ui.components.FileTypeTile
import io.swiftfolio.app.ui.components.TileMode
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.ByteFormat
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.S3Path
import io.swiftfolio.core.TimeFormat

private const val NAME_MAX_LINES = 3

/** Details of one file with download, copy and delete actions. */
@Composable
internal fun FileDetailsSheet(
    file: ObjectEntry.File,
    bucket: String,
    onAction: (BrowserAction) -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ConsoleTheme.colors
    val style = FileTypeStyles.of(file.kind)
    ConsoleSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6, vertical = Spacing.space2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileTypeTile(style = style, mode = TileMode.TYPE)
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, maxLines = NAME_MAX_LINES, overflow = TextOverflow.Ellipsis)
                Text(file.key, style = ConsoleTheme.mono.meta, color = colors.textTertiary)
            }
        }
        MetaLine(stringResource(R.string.details_size), ByteFormat.format(file.sizeBytes))
        MetaLine(stringResource(R.string.details_modified), TimeFormat.format(file.lastModifiedEpochMillis) ?: stringResource(R.string.unknown_value))
        MetaLine(stringResource(R.string.details_type), stringResource(style.labelRes))
        HorizontalDivider(color = colors.border, modifier = Modifier.padding(top = Spacing.space2))
        ActionRow(stringResource(R.string.download), Icons.Rounded.Download) { onAction(BrowserAction.Download(listOf(file.key))) }
        ActionRow(stringResource(R.string.copy_key), Icons.Rounded.ContentCopy) { onCopy(file.key) }
        ActionRow(stringResource(R.string.copy_s3_uri), Icons.Rounded.Link) { onCopy(S3Path.s3Uri(bucket, file.key)) }
        ActionRow(stringResource(R.string.delete), Icons.Rounded.DeleteOutline, destructive = true) {
            onAction(BrowserAction.RequestDelete(listOf(file.key)))
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    val colors = ConsoleTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.space6, vertical = Spacing.space1)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textTertiary, modifier = Modifier.weight(1f))
        Text(value, style = ConsoleTheme.mono.meta, color = colors.textPrimary)
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = ConsoleTheme.colors
    val tint = if (destructive) colors.error else colors.textPrimary
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = tint, leadingIconColor = tint),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
