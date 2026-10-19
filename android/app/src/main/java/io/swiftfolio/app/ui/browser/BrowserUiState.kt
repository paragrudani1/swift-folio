package io.swiftfolio.app.ui.browser

import io.swiftfolio.app.AppConstants
import io.swiftfolio.app.bucket.StorageState
import io.swiftfolio.app.data.DeleteProgress
import io.swiftfolio.app.transfer.TransferBatch
import io.swiftfolio.core.Breadcrumbs
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.Crumb
import io.swiftfolio.core.EntryTypeFilter
import io.swiftfolio.core.FieldError
import io.swiftfolio.core.FolderNameValidator
import io.swiftfolio.core.Listing
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.S3Path
import io.swiftfolio.core.SortField
import io.swiftfolio.core.SortOrder

/** What the list area shows. */
sealed interface ListingState {
    /** First load of a folder with nothing cached: skeleton rows. */
    data object Loading : ListingState

    /** Rows of the current folder. */
    data class Loaded(val entries: List<ObjectEntry>) : ListingState

    /** The listing failed; [stale] rows (if cached) stay visible, dimmed. */
    data class Failed(val error: ConnectError, val stale: List<ObjectEntry>?) : ListingState
}

/** The three command-bar modes (DIRECTION-C.md 6.2-6.4). */
sealed interface CommandMode {
    /** Default: up, filter affordance, new folder, upload. */
    data object Browse : CommandMode

    /** Live filter of the current folder. */
    data object Search : CommandMode

    /** Multi-select with bulk actions. */
    data object Select : CommandMode
}

/** Modal surfaces of the browser. At most one is open. */
sealed interface BrowserSheet {
    /** Sort options. */
    data object Sort : BrowserSheet

    /** Full breadcrumb list for deep paths. */
    data object PathJump : BrowserSheet

    /** Upload and download queue. */
    data object Transfers : BrowserSheet

    /** Actions for one file. */
    data class FileDetails(val file: ObjectEntry.File) : BrowserSheet

    /** Create folder form. */
    data class NewFolder(val name: String = "", val isCreating: Boolean = false, val error: ConnectError? = null) : BrowserSheet

    /** Delete confirmation for [ids] (keys and/or prefixes); [progress] is set while deleting. */
    data class ConfirmDelete(val ids: List<String>, val progress: DeleteProgress? = null) : BrowserSheet {
        /** Number of folders (recursive deletes) among [ids]. */
        val folderCount: Int get() = ids.count { S3Path.isFolderMarker(it) }

        /** Large deletes and any folder delete must be confirmed by holding the button. */
        val needsHoldToConfirm: Boolean
            get() = progress == null && (ids.size >= AppConstants.HOLD_TO_CONFIRM_THRESHOLD || folderCount > 0)
    }
}

/** One-time messages shown as snackbars. */
sealed interface BrowserMessage {
    /** All requested objects were deleted. */
    data class Deleted(val count: Int) : BrowserMessage

    /** Some deletes failed. */
    data class DeletedPartially(val deleted: Int, val failed: Int) : BrowserMessage

    /** A folder was created. */
    data class FolderCreated(val name: String) : BrowserMessage

    /** Uploads were queued. */
    data class UploadsQueued(val count: Int) : BrowserMessage

    /** Downloads were queued. */
    data class DownloadsQueued(val count: Int) : BrowserMessage

    /** An upload batch finished into [prefix]. */
    data class UploadsFinished(val count: Int, val prefix: String) : BrowserMessage

    /** None of the picked files could be read. */
    data object NothingToUpload : BrowserMessage

    /** Text was copied to the clipboard. */
    data class Copied(val text: String) : BrowserMessage

    /** An operation failed. */
    data class OperationFailed(val error: ConnectError) : BrowserMessage
}

/** Browser screen state. Derived values are computed once per instance. */
data class BrowserUiState(
    val bucket: String = "",
    val region: String = "",
    val isConnected: Boolean = false,
    val prefix: String = "",
    val navigationDepth: Int = 0,
    val listing: ListingState = ListingState.Loading,
    val isRefreshing: Boolean = false,
    val mode: CommandMode = CommandMode.Browse,
    val query: String = "",
    val typeFilter: EntryTypeFilter = EntryTypeFilter.ALL,
    val sortOrder: SortOrder = SortOrder(),
    val selection: Set<String> = emptySet(),
    val storage: StorageState = StorageState.Computing,
    val sheet: BrowserSheet? = null,
    val transferBatches: List<TransferBatch> = emptyList(),
    val message: BrowserMessage? = null,
    val credentialsRejected: ConnectError? = null,
) {
    /** Rows of the folder (fresh or stale). */
    val entries: List<ObjectEntry> by lazy {
        when (val current = listing) {
            ListingState.Loading -> emptyList()
            is ListingState.Loaded -> current.entries
            is ListingState.Failed -> current.stale.orEmpty()
        }
    }

    /** Rows after search filter and sort. */
    val visibleEntries: List<ObjectEntry> by lazy {
        val activeQuery = if (mode == CommandMode.Search) query else ""
        val activeFilter = if (mode == CommandMode.Search) typeFilter else EntryTypeFilter.ALL
        Listing.visible(entries, activeQuery, sortOrder, activeFilter)
    }

    /** Breadcrumbs for the path pill. */
    val crumbs: List<Crumb> by lazy { Breadcrumbs.build(bucket, prefix) }

    /** Full breadcrumbs (no collapsing) for the path jump sheet. */
    val allCrumbs: List<Crumb> by lazy { Breadcrumbs.build(bucket, prefix, maxVisible = Int.MAX_VALUE) }

    /** Display name of the current folder (bucket at the root). */
    val title: String get() = S3Path.folderName(prefix).ifEmpty { bucket }

    /** `s3://bucket/prefix` of the current folder. */
    val currentUri: String get() = S3Path.s3Uri(bucket, prefix)

    /** True when the back gesture has something to undo inside the browser. */
    val canGoBack: Boolean get() = sheet != null || mode != CommandMode.Browse || prefix.isNotEmpty()

    /** True while any network activity runs (drives the top activity line). */
    val isBusy: Boolean
        get() = listing == ListingState.Loading || isRefreshing || storage == StorageState.Computing ||
            (sheet as? BrowserSheet.ConfirmDelete)?.progress != null

    /** True when the selection contains folders (downloads are files only). */
    val selectionHasFolders: Boolean get() = selection.any { S3Path.isFolderMarker(it) }

    /** True when every visible row is selected (the top bar then offers Clear instead of Select all). */
    val isAllSelected: Boolean by lazy { visibleEntries.isNotEmpty() && visibleEntries.all { it.id in selection } }

    /** Rows, progress and heading of the transfer queue (sheet and command-bar chip). */
    val transferQueue: TransferQueueSummary by lazy { TransferQueueSummary.of(transferBatches) }

    /** Validation of the new-folder name, or null when valid or no sheet is open. */
    val newFolderError: FieldError? by lazy {
        val sheet = sheet as? BrowserSheet.NewFolder ?: return@lazy null
        if (sheet.name.isEmpty()) return@lazy null
        val folderNames = entries.filterIsInstance<ObjectEntry.Folder>().map { it.name }
        FolderNameValidator.validate(sheet.name, prefix, folderNames)
    }
}

/** User intents on the browser. */
sealed interface BrowserAction {
    /** Folder navigation, refresh and row taps. */
    sealed interface Navigation : BrowserAction

    /** Search, sort and selection modes. */
    sealed interface Mode : BrowserAction

    /** Opening, editing and confirming sheets. */
    sealed interface Sheet : BrowserAction

    /** Uploads and downloads. */
    sealed interface Transfer : BrowserAction

    /** Snackbar feedback and leaving the browser. */
    sealed interface Feedback : BrowserAction

    /** A row was tapped. */
    data class RowTapped(val entry: ObjectEntry) : BrowserAction.Navigation

    /** A row was long-pressed (enters select mode). */
    data class RowLongPressed(val entry: ObjectEntry) : BrowserAction.Navigation

    /** Go to [prefix] (path pill or jump sheet). */
    data class JumpTo(val prefix: String) : BrowserAction.Navigation

    /** Up one folder. */
    data object NavigateUp : BrowserAction.Navigation

    /** System back. */
    data object Back : BrowserAction.Navigation

    /** Pull to refresh or retry. */
    data object Refresh : BrowserAction.Navigation

    /** Recompute the storage total. */
    data object RefreshStorage : BrowserAction.Navigation

    /** Enter search mode. */
    data object StartSearch : BrowserAction.Mode

    /** Search text changed. */
    data class QueryChanged(val query: String) : BrowserAction.Mode

    /** Search type filter changed. */
    data class TypeFilterChanged(val filter: EntryTypeFilter) : BrowserAction.Mode

    /** Sort field picked. */
    data class SortSelected(val field: SortField) : BrowserAction.Mode

    /** Select every visible row. */
    data object SelectAll : BrowserAction.Mode

    /** Deselect everything (stay in select mode). */
    data object ClearSelection : BrowserAction.Mode

    /** Leave search or select mode. */
    data object ExitMode : BrowserAction.Mode

    /** Open [sheet]. */
    data class ShowSheet(val sheet: BrowserSheet) : BrowserAction.Sheet

    /** Close the open sheet (cancels a running delete). */
    data object DismissSheet : BrowserAction.Sheet

    /** Ask for confirmation before deleting [ids]. */
    data class RequestDelete(val ids: List<String>) : BrowserAction.Sheet

    /** Delete the selection (asks for confirmation). */
    data object DeleteSelection : BrowserAction.Sheet

    /** Confirmed in the dialog. */
    data object ConfirmDelete : BrowserAction.Sheet

    /** New-folder name edited. */
    data class NewFolderNameChanged(val name: String) : BrowserAction.Sheet

    /** Create the folder typed in the sheet. */
    data object CreateFolder : BrowserAction.Sheet

    /** Download [keys]. */
    data class Download(val keys: List<String>) : BrowserAction.Transfer

    /** Download the selected files. */
    data object DownloadSelection : BrowserAction.Transfer

    /** Files were picked for upload (content URIs as strings). */
    data class FilesPicked(val uris: List<String>) : BrowserAction.Transfer

    /** Cancel every queued and running upload and download. */
    data object CancelAllTransfers : BrowserAction.Transfer

    /** Cancel one file of a batch. */
    data class CancelTransfer(val batchId: String, val itemIndex: Int) : BrowserAction.Transfer

    /** Hide finished batches. */
    data object ClearFinishedTransfers : BrowserAction.Transfer

    /** Something was copied (for the confirmation chip). */
    data class Copied(val text: String) : BrowserAction.Feedback

    /** The snackbar for [message] was shown. */
    data object MessageShown : BrowserAction.Feedback

    /** From the credentials-rejected dialog. */
    data object EditCredentials : BrowserAction.Feedback

    /** From the credentials-rejected dialog. */
    data object SignOut : BrowserAction.Feedback
}

/** One-shot effects. */
sealed interface BrowserEffect {
    /** Go to Connect (no session, rejected credentials, or sign-out). */
    data object NavigateToConnect : BrowserEffect
}
