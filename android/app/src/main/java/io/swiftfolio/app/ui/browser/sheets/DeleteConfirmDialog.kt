// pluralStringResource was introduced as experimental; opting in is harmless if it is stable.
@file:OptIn(ExperimentalComposeUiApi::class)

package io.swiftfolio.app.ui.browser.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.browser.BrowserSheet
import io.swiftfolio.app.ui.components.HoldToConfirmButton
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

private const val PREVIEW_KEYS = 5
private const val ICON_BADGE_ALPHA = 0.12f
private val IconBadgeSize = 48.dp

/**
 * Delete confirmation (DIRECTION-C.md 6.10). Large deletes and any folder delete need
 * hold-to-confirm; while deleting, the confirm button shows `deleted / total`.
 */
@Composable
internal fun DeleteConfirmDialog(sheet: BrowserSheet.ConfirmDelete, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = ConsoleTheme.colors
    val count = sheet.ids.size
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(Modifier.size(IconBadgeSize).background(colors.error.copy(alpha = ICON_BADGE_ALPHA), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = colors.error)
            }
        },
        title = {
            Text(
                if (count == 1 && sheet.folderCount == 0) {
                    stringResource(R.string.delete_file_title)
                } else {
                    pluralStringResource(R.plurals.delete_items_title, count, count)
                },
            )
        },
        text = { DeleteBody(sheet) },
        confirmButton = { DeleteConfirmButton(sheet, onConfirm) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = colors.glass3,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.border(Spacing.hairline, colors.rimBrush, MaterialTheme.shapes.large),
    )
}

@Composable
private fun DeleteBody(sheet: BrowserSheet.ConfirmDelete) {
    val colors = ConsoleTheme.colors
    val codeShape = MaterialTheme.shapes.small
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Column(
            Modifier.fillMaxWidth().background(colors.glass2, codeShape).border(Spacing.hairline, colors.border, codeShape).padding(Spacing.space3),
        ) {
            sheet.ids.take(PREVIEW_KEYS).forEach { id ->
                Text(id, style = ConsoleTheme.mono.meta, color = colors.textPrimary, maxLines = 2)
            }
            val hidden = sheet.ids.size - PREVIEW_KEYS
            if (hidden > 0) Text(stringResource(R.string.and_more, hidden), style = ConsoleTheme.mono.meta, color = colors.textTertiary)
        }
        if (sheet.folderCount > 0) {
            Text(
                pluralStringResource(R.plurals.delete_includes_folders, sheet.folderCount, sheet.folderCount),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.warning,
            )
        }
        Text(stringResource(R.string.delete_cannot_undo), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DeleteConfirmButton(sheet: BrowserSheet.ConfirmDelete, onConfirm: () -> Unit) {
    val colors = ConsoleTheme.colors
    val progress = sheet.progress
    val label = if (progress != null) {
        stringResource(R.string.delete_progress, progress.deleted, progress.total)
    } else {
        stringResource(R.string.delete)
    }
    if (sheet.needsHoldToConfirm) {
        HoldToConfirmButton(text = label, holdHint = stringResource(R.string.hold_to_delete), onConfirm = onConfirm)
    } else {
        Button(
            onClick = onConfirm,
            enabled = progress == null,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.destructiveContainer,
                contentColor = colors.onDestructive,
                disabledContainerColor = colors.destructiveContainer,
                disabledContentColor = colors.onDestructive,
            ),
        ) {
            Text(label, style = ConsoleTheme.mono.path)
        }
    }
}
