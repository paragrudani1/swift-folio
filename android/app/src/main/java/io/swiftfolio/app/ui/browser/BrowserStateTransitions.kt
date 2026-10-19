package io.swiftfolio.app.ui.browser

import io.swiftfolio.app.data.DeleteProgress
import io.swiftfolio.core.S3Path

// Pure state transitions of the browser. The ViewModel decides when they happen; these decide
// what changes, so each rule is written once and can be read without the ViewModel's plumbing.

/** Opens folder [prefix]: back to browse mode with no query, selection or sheet. */
internal fun BrowserUiState.navigatedTo(prefix: String): BrowserUiState = copy(
    prefix = prefix,
    navigationDepth = S3Path.depth(prefix),
    mode = CommandMode.Browse,
    query = "",
    selection = emptySet(),
    sheet = null,
)

/** Enters search mode, dropping any selection and sheet. */
internal fun BrowserUiState.searching(): BrowserUiState = copy(mode = CommandMode.Search, selection = emptySet(), sheet = null)

/** Leaves search or select mode. */
internal fun BrowserUiState.modeExited(): BrowserUiState = copy(mode = CommandMode.Browse, query = "", selection = emptySet())

/** Enters select mode with [id] selected. */
internal fun BrowserUiState.selecting(id: String): BrowserUiState = copy(mode = CommandMode.Select, selection = setOf(id), sheet = null)

/** Selects or deselects [id]. */
internal fun BrowserUiState.selectionToggled(id: String): BrowserUiState =
    copy(selection = if (id in selection) selection - id else selection + id)

/** Selects every visible row. */
internal fun BrowserUiState.allSelected(): BrowserUiState = copy(mode = CommandMode.Select, selection = visibleEntries.map { it.id }.toSet())

/** Back in browse mode after a bulk action, showing [message]. */
internal fun BrowserUiState.bulkActionDone(message: BrowserMessage?): BrowserUiState =
    copy(sheet = null, mode = CommandMode.Browse, selection = emptySet(), message = message ?: this.message)

/** Shows [progress] in the open delete dialog, if it is still open. */
internal fun BrowserUiState.deleteProgressed(progress: DeleteProgress): BrowserUiState =
    (sheet as? BrowserSheet.ConfirmDelete)?.let { copy(sheet = it.copy(progress = progress)) } ?: this

/** Applies [transform] to the open new-folder sheet, if it is still open. */
internal fun BrowserUiState.newFolderEdited(transform: BrowserSheet.NewFolder.() -> BrowserSheet.NewFolder): BrowserUiState =
    (sheet as? BrowserSheet.NewFolder)?.let { copy(sheet = it.transform()) } ?: this
