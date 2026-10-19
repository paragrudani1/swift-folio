package io.swiftfolio.app.ui

import androidx.lifecycle.SavedStateHandle
import io.swiftfolio.app.MainDispatcherRule
import io.swiftfolio.app.bucket.BrowserUseCases
import io.swiftfolio.app.bucket.StorageState
import io.swiftfolio.app.data.StorageUsage
import io.swiftfolio.app.fakes.FakeCredentialStore
import io.swiftfolio.app.fakes.FakeS3Repository
import io.swiftfolio.app.fakes.FakeS3RepositoryFactory
import io.swiftfolio.app.fakes.FakeSettingsStore
import io.swiftfolio.app.fakes.FakeTransferScheduler
import io.swiftfolio.app.fakes.TestData
import io.swiftfolio.app.session.CancelTransfersAndSignOut
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.transfer.TransferBatch
import io.swiftfolio.app.transfer.TransferItem
import io.swiftfolio.app.transfer.TransferItemStatus
import io.swiftfolio.app.transfer.TransferKind
import io.swiftfolio.app.transfer.TransferQueue
import io.swiftfolio.app.ui.browser.BrowserAction
import io.swiftfolio.app.ui.browser.BrowserEffect
import io.swiftfolio.app.ui.browser.BrowserMessage
import io.swiftfolio.app.ui.browser.BrowserSheet
import io.swiftfolio.app.ui.browser.BrowserViewModel
import io.swiftfolio.app.ui.browser.CommandMode
import io.swiftfolio.app.ui.browser.ListingState
import io.swiftfolio.app.ui.browser.QueueHeading
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.FieldError
import io.swiftfolio.core.S3Result
import io.swiftfolio.core.SortDirection
import io.swiftfolio.core.SortField
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeS3Repository().apply { listings["photos/"] = S3Result.Success(TestData.photosListing) }
    private val credentials = FakeCredentialStore()
    private val sessionManager = SessionManager(credentials, FakeSettingsStore(), FakeS3RepositoryFactory { repository })
    private val scheduler = FakeTransferScheduler()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) = BrowserViewModel(
        sessionManager = sessionManager,
        transferScheduler = scheduler,
        useCases = BrowserUseCases.create(sessionManager),
        signOut = CancelTransfersAndSignOut(sessionManager, scheduler),
        savedState = savedState,
    )

    private suspend fun connectedViewModel(savedState: SavedStateHandle = SavedStateHandle()): BrowserViewModel {
        sessionManager.connect(TestData.config, remember = false) { _, _ -> }
        return viewModel(savedState)
    }

    private fun TestScope.effectsOf(viewModel: BrowserViewModel): List<BrowserEffect> {
        val effects = mutableListOf<BrowserEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(effects) }
        return effects
    }

    private fun upload(status: TransferItemStatus, bytes: Long, isActive: Boolean) = TransferBatch(
        id = "b1",
        kind = TransferKind.UPLOAD,
        bucket = TestData.BUCKET,
        prefix = "",
        items = listOf(TransferItem("a.txt", "a.txt", 10, bytes, status)),
        isActive = isActive,
    )

    @Test fun `binds the session, shows the root and the storage total`() = runTest {
        val vm = connectedViewModel()
        val state = vm.state.value
        assertEquals(TestData.BUCKET, state.bucket)
        assertTrue(state.isConnected)
        assertEquals(ListingState.Loaded(TestData.root), state.listing)
        assertEquals(StorageState.Ready(StorageUsage(4_120, 2)), state.storage)
    }

    @Test fun `no session and nothing saved sends the user to Connect`() = runTest {
        val vm = viewModel()
        val effects = effectsOf(vm)
        assertEquals(listOf(BrowserEffect.NavigateToConnect), effects)
    }

    @Test fun `remembered credentials auto connect`() = runTest {
        credentials.saved = TestData.config
        val vm = viewModel()
        assertTrue(vm.state.value.isConnected)
    }

    @Test fun `the open folder survives process death`() = runTest {
        val savedState = SavedStateHandle()
        connectedViewModel(savedState).onAction(BrowserAction.RowTapped(TestData.photos))
        val recreated = viewModel(savedState)
        assertEquals("photos/", recreated.state.value.prefix)
        assertEquals(ListingState.Loaded(TestData.photosListing), recreated.state.value.listing)
    }

    @Test fun `files picked before the session is bound wait for it`() = runTest {
        credentials.saved = TestData.config
        val gate = CompletableDeferred<Unit>()
        repository.verifyGate = gate
        val vm = viewModel()
        vm.onAction(BrowserAction.FilesPicked(listOf("content://a")))
        assertTrue(scheduler.uploadRequests.isEmpty())
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(Triple(TestData.BUCKET, "", listOf("content://a")), scheduler.uploadRequests.single())
    }

    @Test fun `folders open on tap and back walks up then stops`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowTapped(TestData.photos))
        assertEquals("photos/", vm.state.value.prefix)
        assertEquals(ListingState.Loaded(TestData.photosListing), vm.state.value.listing)
        assertTrue(vm.state.value.canGoBack)
        vm.onAction(BrowserAction.Back)
        assertEquals("", vm.state.value.prefix)
        assertFalse(vm.state.value.canGoBack)
    }

    @Test fun `tapping a file opens its details`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowTapped(TestData.readme))
        assertEquals(BrowserSheet.FileDetails(TestData.readme), vm.state.value.sheet)
        vm.onAction(BrowserAction.Back)
        assertNull(vm.state.value.sheet)
    }

    @Test fun `search filters the folder and back closes search first`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.StartSearch)
        vm.onAction(BrowserAction.QueryChanged("PHO"))
        assertEquals(listOf(TestData.photos), vm.state.value.visibleEntries)
        vm.onAction(BrowserAction.Back)
        assertEquals(CommandMode.Browse, vm.state.value.mode)
        assertEquals(3, vm.state.value.visibleEntries.size)
    }

    @Test fun `sort selection flips direction on the active field`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.SortSelected(SortField.NAME))
        assertEquals(SortDirection.DESCENDING, vm.state.value.sortOrder.direction)
        assertEquals(listOf(TestData.photos, TestData.docs, TestData.readme), vm.state.value.visibleEntries)
    }

    @Test fun `long press selects, taps toggle, select all covers the folder`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowLongPressed(TestData.readme))
        assertEquals(CommandMode.Select, vm.state.value.mode)
        assertEquals(setOf("README.md"), vm.state.value.selection)
        vm.onAction(BrowserAction.RowTapped(TestData.photos))
        assertTrue(vm.state.value.selectionHasFolders)
        vm.onAction(BrowserAction.RowTapped(TestData.photos))
        assertFalse(vm.state.value.isAllSelected)
        vm.onAction(BrowserAction.SelectAll)
        assertEquals(3, vm.state.value.selection.size)
        assertTrue(vm.state.value.isAllSelected)
        vm.onAction(BrowserAction.Back)
        assertTrue(vm.state.value.selection.isEmpty())
    }

    @Test fun `bulk delete asks first, then deletes, reports and reloads without rescanning`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowLongPressed(TestData.readme))
        vm.onAction(BrowserAction.RowTapped(TestData.docs))
        vm.onAction(BrowserAction.DeleteSelection)
        val sheet = vm.state.value.sheet as BrowserSheet.ConfirmDelete
        assertEquals(1, sheet.folderCount)
        assertTrue(sheet.needsHoldToConfirm)
        assertTrue(repository.deletedIds.isEmpty())
        val listsBefore = repository.listCalls.size
        vm.onAction(BrowserAction.ConfirmDelete)
        assertEquals(setOf("README.md", "docs/"), repository.deletedIds.single().toSet())
        assertEquals(BrowserMessage.Deleted(2), vm.state.value.message)
        assertEquals(CommandMode.Browse, vm.state.value.mode)
        assertTrue(repository.listCalls.size > listsBefore)
        assertEquals(1, repository.storageCalls)
        assertEquals(StorageState.Ready(StorageUsage(4_000, 0)), vm.state.value.storage)
    }

    @Test fun `deleting one file needs no hold`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RequestDelete(listOf("README.md")))
        assertFalse((vm.state.value.sheet as BrowserSheet.ConfirmDelete).needsHoldToConfirm)
    }

    @Test fun `partial delete failures are reported`() = runTest {
        repository.deleteFailures = listOf("README.md")
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RequestDelete(listOf("README.md", "docs/")))
        vm.onAction(BrowserAction.ConfirmDelete)
        assertEquals(BrowserMessage.DeletedPartially(deleted = 1, failed = 1), vm.state.value.message)
    }

    @Test fun `new folder validates against the listing and creates the prefix`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.ShowSheet(BrowserSheet.NewFolder()))
        vm.onAction(BrowserAction.NewFolderNameChanged("photos"))
        assertEquals(FieldError.ALREADY_EXISTS, vm.state.value.newFolderError)
        vm.onAction(BrowserAction.CreateFolder)
        assertTrue(repository.createdPrefixes.isEmpty())
        vm.onAction(BrowserAction.NewFolderNameChanged(" invoices "))
        vm.onAction(BrowserAction.CreateFolder)
        assertEquals(listOf("invoices/"), repository.createdPrefixes)
        assertEquals(BrowserMessage.FolderCreated("invoices"), vm.state.value.message)
        assertNull(vm.state.value.sheet)
        assertEquals(1, repository.storageCalls)
    }

    @Test fun `rejected credentials raise the blocking dialog`() = runTest {
        val vm = connectedViewModel()
        val rejected = ConnectError(ConnectErrorKind.INVALID_ACCESS_KEY, "InvalidAccessKeyId")
        repository.listings[""] = S3Result.Failure(rejected)
        vm.onAction(BrowserAction.Refresh)
        assertEquals(rejected, vm.state.value.credentialsRejected)
        assertEquals(TestData.root, vm.state.value.entries)
    }

    @Test fun `signing out from the dialog cancels transfers and leaves`() = runTest {
        val vm = connectedViewModel()
        val effects = effectsOf(vm)
        vm.onAction(BrowserAction.SignOut)
        assertEquals(TransferKind.entries.toList(), scheduler.cancelledKinds)
        assertNull(sessionManager.session.value)
        assertEquals(listOf(BrowserEffect.NavigateToConnect), effects)
    }

    @Test fun `uploads go to the scheduler for the current folder`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowTapped(TestData.photos))
        vm.onAction(BrowserAction.FilesPicked(listOf("content://a", "content://b")))
        assertEquals(Triple(TestData.BUCKET, "photos/", listOf("content://a", "content://b")), scheduler.uploadRequests.single())
        assertEquals(BrowserMessage.UploadsQueued(2), vm.state.value.message)
    }

    @Test fun `finished upload batch refreshes the folder and adds to the total without rescanning`() = runTest {
        val vm = connectedViewModel()
        scheduler.queueState.value = TransferQueue(listOf(upload(TransferItemStatus.RUNNING, bytes = 0, isActive = true)))
        assertTrue(vm.state.value.transferQueue.isActive)
        assertEquals(0, vm.state.value.transferQueue.progress.percent)
        val listsBefore = repository.listCalls.size
        scheduler.queueState.value = TransferQueue(listOf(upload(TransferItemStatus.DONE, bytes = 10, isActive = false)))
        assertEquals(BrowserMessage.UploadsFinished(1, ""), vm.state.value.message)
        assertTrue(repository.listCalls.size > listsBefore)
        assertFalse(vm.state.value.transferQueue.isActive)
        assertEquals(StorageState.Ready(StorageUsage(4_130, 3)), vm.state.value.storage)
        assertEquals(1, repository.storageCalls)
    }

    @Test fun `downloads show in the transfer queue`() = runTest {
        val vm = connectedViewModel()
        val download = upload(TransferItemStatus.RUNNING, bytes = 5, isActive = true).copy(kind = TransferKind.DOWNLOAD)
        scheduler.queueState.value = TransferQueue(listOf(download))
        val queue = vm.state.value.transferQueue
        assertEquals(QueueHeading.Downloading, queue.heading)
        assertEquals(TransferKind.DOWNLOAD, queue.rows.single().kind)
        assertEquals(50, queue.progress.percent)
    }

    @Test fun `queue actions reach the scheduler`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.CancelTransfer("b1", 2))
        vm.onAction(BrowserAction.CancelAllTransfers)
        vm.onAction(BrowserAction.ClearFinishedTransfers)
        assertEquals(listOf("b1" to 2), scheduler.cancelledItems)
        assertEquals(TransferKind.entries.toList(), scheduler.cancelledKinds)
        assertEquals(1, scheduler.clearedFinished)
    }

    @Test fun `downloads skip folders and leave selection mode`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowLongPressed(TestData.readme))
        vm.onAction(BrowserAction.RowTapped(TestData.docs))
        vm.onAction(BrowserAction.DownloadSelection)
        assertEquals(TestData.BUCKET to listOf("README.md"), scheduler.downloadRequests.single())
        assertEquals(CommandMode.Browse, vm.state.value.mode)
        assertEquals(BrowserMessage.DownloadsQueued(1), vm.state.value.message)
    }
}
