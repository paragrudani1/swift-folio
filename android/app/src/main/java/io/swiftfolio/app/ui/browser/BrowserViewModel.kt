package io.swiftfolio.app.ui.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.swiftfolio.app.bucket.BrowserUseCases
import io.swiftfolio.app.data.DeleteSummary
import io.swiftfolio.app.session.RestoreResult
import io.swiftfolio.app.session.Session
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.session.SignOut
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
 * Browser state holder. It turns intents into state and delegates the work: listings, the
 * storage total and bucket changes go through [BrowserUseCases], transfers through
 * [TransferScheduler] and sign-out through [SignOut]. It makes no S3 calls itself.
 *
 * The current folder survives process death in [savedState]. Files picked before the session
 * is bound (a picker result delivered to a recreated screen) wait until it is.
 */
class BrowserViewModel(
    private val sessionManager: SessionManager,
    private val transferScheduler: TransferScheduler,
    private val useCases: BrowserUseCases,
    private val signOut: SignOut,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoredState())
    private val effectChannel = Channel<BrowserEffect>(Channel.BUFFERED)
    private var loadJob: Job? = null
    private var storageJob: Job? = null
    private var deleteJob: Job? = null
    private var activeUploadIds: Set<String> = emptySet()
    private var pendingUploads: List<String> = emptyList()

    /** Current screen state. */
    val state: StateFlow<BrowserUiState> = mutableState.asStateFlow()

    /** One-shot navigation effects. */
    val effects: Flow<BrowserEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { useCases.storage.state.collect { storage -> update { copy(storage = storage) } } }
        viewModelScope.launch { transferScheduler.queue.collect(::onTransferQueue) }
        viewModelScope.launch { attachSession() }
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
            is BrowserAction.RowLongPressed -> update { selecting(action.entry.id) }
            is BrowserAction.JumpTo -> navigateTo(action.prefix)
            BrowserAction.NavigateUp -> navigateTo(S3Path.parentOf(state.value.prefix))
            BrowserAction.Back -> onBack()
            BrowserAction.Refresh -> load(state.value.prefix, isRefresh = true)
            BrowserAction.RefreshStorage -> rescanStorage()
        }
    }

    private fun onMode(action: BrowserAction.Mode) {
        when (action) {
            BrowserAction.StartSearch -> update { searching() }
            is BrowserAction.QueryChanged -> update { copy(query = action.query) }
            is BrowserAction.TypeFilterChanged -> update { copy(typeFilter = action.filter) }
            is BrowserAction.SortSelected -> update { copy(sortOrder = sortOrder.select(action.field), sheet = null) }
            BrowserAction.SelectAll -> update { allSelected() }
            BrowserAction.ClearSelection -> update { copy(selection = emptySet()) }
            BrowserAction.ExitMode -> update { modeExited() }
        }
    }

    private fun onSheet(action: BrowserAction.Sheet) {
        when (action) {
            is BrowserAction.ShowSheet -> update { copy(sheet = action.sheet) }
            BrowserAction.DismissSheet -> dismissSheet()
            is BrowserAction.RequestDelete -> update { copy(sheet = BrowserSheet.ConfirmDelete(action.ids)) }
            BrowserAction.DeleteSelection -> update { copy(sheet = BrowserSheet.ConfirmDelete(selection.toList())) }
            BrowserAction.ConfirmDelete -> confirmDelete()
            is BrowserAction.NewFolderNameChanged -> update { newFolderEdited { copy(name = action.name, error = null) } }
            BrowserAction.CreateFolder -> createFolder()
        }
    }

    private fun onTransfer(action: BrowserAction.Transfer) {
        when (action) {
            is BrowserAction.Download -> download(action.keys)
            BrowserAction.DownloadSelection -> download(state.value.selection.filterNot(S3Path::isFolderMarker))
            is BrowserAction.FilesPicked -> upload(action.uris)
            BrowserAction.CancelAllTransfers -> viewModelScope.launch { TransferKind.entries.forEach { transferScheduler.cancelAll(it) } }
            is BrowserAction.CancelTransfer -> transferScheduler.cancelItem(action.batchId, action.itemIndex)
            BrowserAction.ClearFinishedTransfers -> viewModelScope.launch { transferScheduler.clearFinished() }
        }
    }

    private fun onFeedback(action: BrowserAction.Feedback) {
        when (action) {
            is BrowserAction.Copied -> update { copy(message = BrowserMessage.Copied(action.text)) }
            BrowserAction.MessageShown -> update { copy(message = null) }
            BrowserAction.EditCredentials -> leave(signingOut = false)
            BrowserAction.SignOut -> leave(signingOut = true)
        }
    }

    // region Session and loading

    private fun restoredState(): BrowserUiState {
        val prefix = savedState.get<String>(KEY_PREFIX).orEmpty()
        return BrowserUiState(prefix = prefix, navigationDepth = S3Path.depth(prefix))
    }

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
        session.initialListing?.let { useCases.listings.seed(prefix = "", entries = it) }
        update { copy(bucket = session.config.bucket, region = session.config.region, isConnected = true) }
        load(state.value.prefix, isRefresh = false)
        rescanStorage()
        val waiting = pendingUploads
        pendingUploads = emptyList()
        upload(waiting)
    }

    private fun load(prefix: String, isRefresh: Boolean) {
        if (!state.value.isConnected) return
        val cached = useCases.listings.cached(prefix)
        update { copy(listing = cached?.let(ListingState::Loaded) ?: ListingState.Loading, isRefreshing = isRefresh) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = useCases.listings.load(prefix)
            if (state.value.prefix != prefix) return@launch
            when (result) {
                is S3Result.Success -> update { copy(listing = ListingState.Loaded(result.value), isRefreshing = false) }
                is S3Result.Failure -> onListingFailed(result.error, useCases.listings.cached(prefix))
            }
        }
    }

    private fun onListingFailed(error: ConnectError, stale: List<ObjectEntry>?) = update {
        copy(listing = ListingState.Failed(error, stale), isRefreshing = false, credentialsRejected = error.takeIf { it.kind.isCredentialProblem })
    }

    /** Lists the whole bucket again; only on connect and on explicit refresh. */
    private fun rescanStorage() {
        if (!state.value.isConnected) return
        storageJob?.cancel()
        storageJob = viewModelScope.launch { useCases.storage.rescan() }
    }

    // endregion

    // region Navigation

    private fun onRowTapped(entry: ObjectEntry) {
        when {
            state.value.mode == CommandMode.Select -> update { selectionToggled(entry.id) }
            entry is ObjectEntry.Folder -> navigateTo(entry.prefix)
            entry is ObjectEntry.File -> update { copy(sheet = BrowserSheet.FileDetails(entry)) }
        }
    }

    private fun navigateTo(prefix: String) {
        val target = S3Path.normalizePrefix(prefix)
        if (target == state.value.prefix) return
        savedState[KEY_PREFIX] = target
        update { navigatedTo(target) }
        load(target, isRefresh = false)
    }

    private fun onBack() {
        val current = state.value
        when {
            current.sheet != null -> dismissSheet()
            current.mode != CommandMode.Browse -> update { modeExited() }
            current.prefix.isNotEmpty() -> navigateTo(S3Path.parentOf(current.prefix))
        }
    }

    private fun dismissSheet() {
        deleteJob?.cancel()
        update { copy(sheet = null) }
    }

    // endregion

    // region Mutations

    private fun confirmDelete() {
        val sheet = state.value.sheet as? BrowserSheet.ConfirmDelete ?: return
        if (sheet.progress != null || !state.value.isConnected) return
        deleteJob = viewModelScope.launch {
            val result = useCases.mutations.delete(sheet.ids) { progress -> update { deleteProgressed(progress) } }
            update { bulkActionDone(deleteMessage(result)) }
            load(state.value.prefix, isRefresh = true)
        }
    }

    private fun deleteMessage(result: S3Result<DeleteSummary>): BrowserMessage = when (result) {
        is S3Result.Success -> if (result.value.failedKeys.isEmpty()) {
            BrowserMessage.Deleted(result.value.deleted)
        } else {
            BrowserMessage.DeletedPartially(result.value.deleted, result.value.failedKeys.size)
        }
        is S3Result.Failure -> BrowserMessage.OperationFailed(result.error)
    }

    private fun createFolder() {
        val current = state.value
        val sheet = current.sheet as? BrowserSheet.NewFolder ?: return
        if (!current.isConnected || sheet.name.isBlank() || current.newFolderError != null || sheet.isCreating) return
        val name = sheet.name.trim()
        update { newFolderEdited { copy(isCreating = true) } }
        viewModelScope.launch {
            when (val result = useCases.mutations.createFolder(current.prefix, name)) {
                is S3Result.Success -> {
                    update { copy(sheet = null, message = BrowserMessage.FolderCreated(name)) }
                    load(state.value.prefix, isRefresh = true)
                }
                is S3Result.Failure -> update { newFolderEdited { copy(isCreating = false, error = result.error) } }
            }
        }
    }

    // endregion

    // region Transfers

    private fun download(keys: List<String>) {
        if (keys.isEmpty() || !state.value.isConnected) return
        val bucket = state.value.bucket
        update { bulkActionDone(message = null) }
        viewModelScope.launch {
            val queued = transferScheduler.enqueueDownloads(bucket, keys)
            if (queued > 0) update { copy(message = BrowserMessage.DownloadsQueued(queued)) }
        }
    }

    /** Queues [uris]; before the session is bound they are held, so they never go to an empty bucket. */
    private fun upload(uris: List<String>) {
        if (uris.isEmpty()) return
        val current = state.value
        if (!current.isConnected) {
            pendingUploads = pendingUploads + uris
            return
        }
        viewModelScope.launch {
            val queued = transferScheduler.enqueueUploads(current.bucket, current.prefix, uris)
            update { copy(message = if (queued == 0) BrowserMessage.NothingToUpload else BrowserMessage.UploadsQueued(queued)) }
        }
    }

    /** Mirrors the queue and applies each upload batch that finished while this screen watched. */
    private fun onTransferQueue(queue: TransferQueue) {
        val uploads = queue.ofKind(TransferKind.UPLOAD)
        val finished = uploads.filter { it.id in activeUploadIds && !it.isActive }
        activeUploadIds = uploads.filter { it.isActive }.map { it.id }.toSet()
        update { copy(transferBatches = queue.batches) }
        finished.forEach(::onUploadBatchFinished)
    }

    private fun onUploadBatchFinished(batch: TransferBatch) {
        val done = batch.items.filter { it.status == TransferItemStatus.DONE }
        update { copy(message = BrowserMessage.UploadsFinished(done.size, batch.prefix)) }
        if (batch.bucket != state.value.bucket) return
        useCases.mutations.recordUpload(batch.prefix, bytes = done.sumOf { it.sizeBytes ?: it.bytesDone }, objects = done.size)
        if (batch.prefix == state.value.prefix) load(batch.prefix, isRefresh = true)
    }

    // endregion

    private fun leave(signingOut: Boolean) {
        viewModelScope.launch {
            if (signingOut) signOut(forgetCredentials = false)
            update { copy(credentialsRejected = null) }
            effectChannel.send(BrowserEffect.NavigateToConnect)
        }
    }

    private inline fun update(transform: BrowserUiState.() -> BrowserUiState) = mutableState.update { it.transform() }

    private companion object {
        const val KEY_PREFIX = "browser_prefix"
    }
}
