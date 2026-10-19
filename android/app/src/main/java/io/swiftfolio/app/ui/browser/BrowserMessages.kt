// pluralStringResource was introduced as experimental; opting in is harmless if it is stable.
@file:OptIn(ExperimentalComposeUiApi::class)

package io.swiftfolio.app.ui.browser

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.ErrorTexts

/** Shows [message] once as a snackbar, then reports it as shown. */
@Composable
internal fun BrowserMessageEffect(message: BrowserMessage?, hostState: SnackbarHostState, onShown: () -> Unit) {
    val text = message?.let { messageText(it) }
    val shown = rememberUpdatedState(onShown)
    LaunchedEffect(message) {
        if (text != null) {
            hostState.showSnackbar(text)
            shown.value()
        }
    }
}

/** The text of each message type. */
@Composable
private fun messageText(message: BrowserMessage): String = when (message) {
    is BrowserMessage.Deleted -> pluralStringResource(R.plurals.message_deleted, message.count, message.count)
    is BrowserMessage.DeletedPartially -> stringResource(R.string.message_deleted_partially, message.deleted, message.failed)
    is BrowserMessage.FolderCreated -> stringResource(R.string.message_folder_created, message.name)
    is BrowserMessage.UploadsQueued -> pluralStringResource(R.plurals.message_uploads_queued, message.count, message.count)
    is BrowserMessage.DownloadsQueued -> pluralStringResource(R.plurals.message_downloads_queued, message.count, message.count)
    is BrowserMessage.UploadsFinished -> pluralStringResource(R.plurals.message_uploads_finished, message.count, message.count, "/" + message.prefix)
    BrowserMessage.NothingToUpload -> stringResource(R.string.message_nothing_to_upload)
    is BrowserMessage.Copied -> stringResource(R.string.message_copied, message.text.lineSequence().first())
    is BrowserMessage.OperationFailed -> stringResource(R.string.message_failed, ErrorTexts.title(message.error), message.error.code)
}
