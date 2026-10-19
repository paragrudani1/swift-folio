package io.swiftfolio.app.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.swiftfolio.app.data.S3Repository
import io.swiftfolio.app.session.RestoreResult
import io.swiftfolio.app.session.Session
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.transfer.TransferBatch
import io.swiftfolio.app.transfer.TransferItemStatus
import io.swiftfolio.app.transfer.TransferKind
import io.swiftfolio.app.transfer.TransferQueue
import io.swiftfolio.app.transfer.TransferScheduler
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.S3Path
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Browser state holder. Folder navigation is an in-memory prefix (not a nav destination),
 * listings are cached per prefix and revalidated on revisit. Every S3 call goes through
 * [S3Repository]; transfers go through [TransferScheduler].
 */
class BrowserViewModel(
    private val sessionManager: SessionManager,
    private val transferScheduler: TransferScheduler,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BrowserUiState())
    private val effectChannel = Channel<BrowserEffect>(Channel.BUFFERED)
    private val listingCache = mutableMapOf<String, List<ObjectEntry>>()
    private var repository: S3Repository? = null
    private var loadJob: Job? = null
    private var storageJob: Job? = null
    private var deleteJob: Job? = null
    private var activeUploadIds: Set<String> = emptySet()

    /** Current screen state. */
    val state: StateFlow<BrowserUiState> = mutableState.asStateFlow()

    /** One-shot navigation effects. */
    val effects: Flow<BrowserEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { attachSession() }
        viewModelScope.launch { transferScheduler.queue.collect(::onTransferQueue) }
    }

    /** Single entry point for user intents; each family is handled exhaustively. */
    fun onAction(action: BrowserAction) {
        when (action) {
            is BrowserAction.Navigation -> onNavigation(action)
            is BrowserAction.Mode -> onMode(action)
            is BrowserAction.Sheet -> onSheet(action)
            is BrowserAction.Transfer -> onTransfer(action)
            is BrowserAction.Feedback -> onFeedback(action)
        }
    }

    private fun onNavigation(action: BrowserAction.Navigation) {
        when (action) {
            is BrowserAction.RowTapped -> onRowTapped(action.entry)
            is BrowserAction.RowLongPressed -> enterSelection(action.entry.id)
            is BrowserAction.JumpTo -> navigateTo(action.prefix)
            BrowserAction.NavigateUp -> navigateTo(S3Path.parentOf(state.value.prefix))
            BrowserAction.Back -> onBack()
            BrowserAction.Refresh -> load(state.value.prefix, isRefresh = true)
            BrowserAction.RefreshStorage -> computeStorage()
        }
    }

    private fun onMode(action: BrowserAction.Mode) {
        when (action) {
            BrowserAction.StartSearch -> update { copy(mode = CommandMode.Search, selection = emptySet(), sheet = null) }
            is BrowserAction.QueryChanged -> update { copy(query = action.query) }
            is BrowserAction.TypeFilterChanged -> update { copy(typeFilter = action.filter) }
            is BrowserAction.SortSelected -> update { copy(sortOrder = sortOrder.select(action.field), sheet = null) }
            BrowserAction.SelectAll -> update { copy(mode = CommandMode.Select, selection = visibleEntries.map { it.id }.toSet()) }
            BrowserAction.ClearSelection -> update { copy(selection = emptySet()) }
            BrowserAction.ExitMode -> exitMode()
        }
    }

    private fun onSheet(action: BrowserAction.Sheet) {
        when (action) {
            is BrowserAction.ShowSheet -> update { copy(sheet = action.sheet) }
            BrowserAction.DismissSheet -> dismissSheet()
            is BrowserAction.RequestDelete -> update { copy(sheet = BrowserSheet.ConfirmDelete(action.ids)) }
            BrowserAction.DeleteSelection -> update { copy(sheet = BrowserSheet.ConfirmDelete(selection.toList())) }
            BrowserAction.ConfirmDelete -> confirmDelete()
            is BrowserAction.NewFolderNameChanged -> updateNewFolder { copy(name = action.name, error = null) }
            BrowserAction.CreateFolder -> createFolder()
        }
    }

    private fun onTransfer(action: BrowserAction.Transfer) {
        when (action) {
            is BrowserAction.Download -> download(action.keys)
            BrowserAction.DownloadSelection -> download(state.value.selection.filterNot(S3Path::isFolderMarker))
            is BrowserAction.FilesPicked -> upload(action.uris)
            BrowserAction.CancelAllUploads -> transferScheduler.cancelAll(TransferKind.UPLOAD)
            is BrowserAction.CancelUpload -> transferScheduler.cancelItem(action.batchId, action.itemIndex)
            BrowserAction.ClearFinishedUploads -> transferScheduler.clearFinished()
        }
    }

    private fun onFeedback(action: BrowserAction.Feedback) {
        when (action) {
            is BrowserAction.Copied -> update { copy(message = BrowserMessage.Copied(action.text)) }
            BrowserAction.MessageShown -> update { copy(message = null) }
            BrowserAction.EditCredentials -> leave(signOut = false)
            BrowserAction.SignOut -> leave(signOut = true)
        }
    }

    // region Session and loading

    private suspend fun attachSession() {
        val session = sessionManager.session.value ?: when (val restored = sessionManager.restore()) {
            is RestoreResult.Connected -> restored.session
            RestoreResult.NothingSaved, is RestoreResult.Failed -> {
                effectChannel.send(BrowserEffect.NavigateToConnect)
                return
            }
        }
        bind(session)
    }

    private fun bind(session: Session) {
        repository = session.repository
        session.initialListing?.let { listingCache[""] = it }
        update { copy(bucket = session.config.bucket, region = session.config.region, isConnected = true) }
        load(prefix = "", isRefresh = false)
        computeStorage()
    }

    private fun load(prefix: String, isRefresh: Boolean) {
        val repo = repository ?: return
        val cached = listingCache[prefix]
        update { copy(listing = cached?.let(ListingState::Loaded) ?: ListingState.Loading, isRefreshing = isRefresh) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = repo.list(prefix)
            if (state.value.prefix != prefix) return@launch
            when (result) {
                is S3Result.Success -> {
                    listingCache[prefix] = result.value
                    update { copy(listing = ListingState.Loaded(result.value), isRefreshing = false) }
                }
                is S3Result.Failure -> onListingFailed(result.error, listingCache[prefix])
            }
        }
    }

    private fun onListingFailed(error: ConnectError, stale: List<ObjectEntry>?) {
        update {
            copy(
                listing = ListingState.Failed(error, stale),
                isRefreshing = false,
                credentialsRejected = error.takeIf { it.kind.isCredentialProblem },
            )
        }
    }

    private fun computeStorage() {
        val repo = repository ?: return
        storageJob?.cancel()
        update { copy(storage = StorageState.Computing) }
        storageJob = viewModelScope.launch {
            val storage = when (val result = repo.computeStorageUsage()) {
                is S3Result.Success -> StorageState.Ready(result.value)
                is S3Result.Failure -> StorageState.Unavailable(result.error)
            }
            update { copy(storage = storage) }
        }
    }

    // endregion

    // region Navigation and modes

    private fun onRowTapped(entry: ObjectEntry) {
        val current = state.value
        when {
            current.mode == CommandMode.Select -> toggleSelection(entry.id)
            entry is ObjectEntry.Folder -> navigateTo(entry.prefix)
            entry is ObjectEntry.File -> update { copy(sheet = BrowserSheet.FileDetails(entry)) }
        }
    }

    private fun navigateTo(prefix: String) {
        val target = S3Path.normalizePrefix(prefix)
        if (target == state.value.prefix) return
        update {
            copy(
                prefix = target,
                navigationDepth = S3Path.depth(target),
                mode = CommandMode.Browse,
                query = "",
                selection = emptySet(),
                sheet = null,
            )
        }
        load(target, isRefresh = false)
    }

    private fun onBack() {
        val current = state.value
        when {
            current.sheet != null -> dismissSheet()
            current.mode != CommandMode.Browse -> exitMode()
            current.prefix.isNotEmpty() -> navigateTo(S3Path.parentOf(current.prefix))
        }
    }

    private fun exitMode() = update { copy(mode = CommandMode.Browse, query = "", selection = emptySet()) }

    private fun enterSelection(id: String) = update { copy(mode = CommandMode.Select, selection = setOf(id), sheet = null) }

    private fun toggleSelection(id: String) = update {
        copy(selection = if (id in selection) selection - id else selection + id)
    }

    private fun dismissSheet() {
        deleteJob?.cancel()
        update { copy(sheet = null) }
    }

    // endregion

    // region Mutations

    private fun confirmDelete() {
        val repo = repository ?: return
        val sheet = state.value.sheet as? BrowserSheet.ConfirmDelete ?: return
        if (sheet.progress != null) return
        deleteJob = viewModelScope.launch {
            val result = repo.delete(sheet.ids) { progress ->
                update { copy(sheet = (this.sheet as? BrowserSheet.ConfirmDelete)?.copy(progress = progress) ?: this.sheet) }
            }
            val message = when (result) {
                is S3Result.Success -> if (result.value.failedKeys.isEmpty()) {
                    BrowserMessage.Deleted(result.value.deleted)
                } else {
                    BrowserMessage.DeletedPartially(result.value.deleted, result.value.failedKeys.size)
                }
                is S3Result.Failure -> BrowserMessage.OperationFailed(result.error)
            }
            update { copy(sheet = null, mode = CommandMode.Browse, selection = emptySet(), message = message) }
            invalidateAndReload()
        }
    }

    private fun createFolder() {
        val repo = repository ?: return
        val current = state.value
        val sheet = current.sheet as? BrowserSheet.NewFolder ?: return
        if (sheet.name.isBlank() || current.newFolderError != null || sheet.isCreating) return
        val name = sheet.name.trim()
        updateNewFolder { copy(isCreating = true) }
        viewModelScope.launch {
            when (val result = repo.createFolder(S3Path.childPrefix(current.prefix, name))) {
                is S3Result.Success -> {
                    update { copy(sheet = null, message = BrowserMessage.FolderCreated(name)) }
                    invalidateAndReload()
                }
                is S3Result.Failure -> updateNewFolder { copy(isCreating = false, error = result.error) }
            }
        }
    }

    private fun invalidateAndReload() {
        listingCache.clear()
        load(state.value.prefix, isRefresh = true)
        computeStorage()
    }

    private fun updateNewFolder(transform: BrowserSheet.NewFolder.() -> BrowserSheet.NewFolder) = update {
        (sheet as? BrowserSheet.NewFolder)?.let { copy(sheet = it.transform()) } ?: this
    }

    // endregion

    // region Transfers

    private fun download(keys: List<String>) {
        if (keys.isEmpty()) return
        val bucket = state.value.bucket
        update { copy(sheet = null, mode = CommandMode.Browse, selection = emptySet()) }
        viewModelScope.launch {
            transferScheduler.enqueueDownloads(bucket, keys)
            update { copy(message = BrowserMessage.DownloadsQueued(keys.size)) }
        }
    }

    private fun upload(uris: List<String>) {
        if (uris.isEmpty()) return
        val current = state.value
        viewModelScope.launch {
            val queued = transferScheduler.enqueueUploads(current.bucket, current.prefix, uris)
            val message = if (queued == 0) BrowserMessage.NothingToUpload else BrowserMessage.UploadsQueued(queued)
            update { copy(message = message) }
        }
    }

    /** Mirrors the upload queue and refreshes the listing when a batch into this folder finishes. */
    private fun onTransferQueue(queue: TransferQueue) {
        val uploads = queue.ofKind(TransferKind.UPLOAD)
        val finished = uploads.filter { it.id in activeUploadIds && !it.isActive }
        activeUploadIds = uploads.filter { it.isActive }.map { it.id }.toSet()
        update { copy(uploadBatches = uploads) }
        finished.forEach(::onUploadBatchFinished)
    }

    private fun onUploadBatchFinished(batch: TransferBatch) {
        val done = batch.items.count { it.status == TransferItemStatus.DONE }
        update { copy(message = BrowserMessage.UploadsFinished(done, batch.prefix)) }
        listingCache.remove(batch.prefix)
        if (batch.prefix == state.value.prefix) load(batch.prefix, isRefresh = true)
        computeStorage()
    }

    // endregion

    private fun leave(signOut: Boolean) {
        viewModelScope.launch {
            if (signOut) sessionManager.signOut(forgetCredentials = false)
            update { copy(credentialsRejected = null) }
            effectChannel.send(BrowserEffect.NavigateToConnect)
        }
    }

    private inline fun update(transform: BrowserUiState.() -> BrowserUiState) = mutableState.update { it.transform() }
}
