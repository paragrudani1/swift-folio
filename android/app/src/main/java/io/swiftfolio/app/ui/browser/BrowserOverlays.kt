package io.swiftfolio.app.ui.browser

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.browser.sheets.DeleteConfirmDialog
import io.swiftfolio.app.ui.browser.sheets.FileDetailsSheet
import io.swiftfolio.app.ui.browser.sheets.NewFolderSheet
import io.swiftfolio.app.ui.browser.sheets.PathJumpSheet
import io.swiftfolio.app.ui.browser.sheets.SortSheet
import io.swiftfolio.app.ui.browser.sheets.UploadQueueSheet
import io.swiftfolio.app.ui.theme.ConsoleTheme

/** Renders the open sheet or dialog. One `when` over the sealed [BrowserSheet]. */
@Composable
internal fun BrowserOverlays(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    onPickFiles: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val dismiss = { onAction(BrowserAction.DismissSheet) }
    when (val sheet = state.sheet) {
        null -> Unit
        BrowserSheet.Sort -> SortSheet(state.sortOrder, onSelect = { onAction(BrowserAction.SortSelected(it)) }, onDismiss = dismiss)
        BrowserSheet.PathJump -> PathJumpSheet(state.allCrumbs, state.bucket, onJump = { onAction(BrowserAction.JumpTo(it)) }, onCopy = onCopy, onDismiss = dismiss)
        BrowserSheet.Uploads -> UploadQueueSheet(state.uploadBatches, state.prefix, onAction, onPickFiles, onDismiss = dismiss)
        is BrowserSheet.FileDetails -> FileDetailsSheet(sheet.file, state.bucket, onAction, onCopy, onDismiss = dismiss)
        is BrowserSheet.NewFolder -> NewFolderSheet(sheet, state.bucket, state.prefix, state.newFolderError, onAction, onDismiss = dismiss)
        is BrowserSheet.ConfirmDelete -> DeleteConfirmDialog(sheet, onConfirm = { onAction(BrowserAction.ConfirmDelete) }, onDismiss = dismiss)
    }
    state.credentialsRejected?.let { error ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.credentials_rejected_title)) },
            text = { Text(error.code, style = ConsoleTheme.mono.meta) },
            confirmButton = {
                TextButton(onClick = { onAction(BrowserAction.EditCredentials) }) { Text(stringResource(R.string.edit_credentials)) }
            },
            dismissButton = {
                TextButton(onClick = { onAction(BrowserAction.SignOut) }) { Text(stringResource(R.string.sign_out)) }
            },
            containerColor = ConsoleTheme.colors.glass3,
            shape = MaterialTheme.shapes.large,
        )
    }
}
