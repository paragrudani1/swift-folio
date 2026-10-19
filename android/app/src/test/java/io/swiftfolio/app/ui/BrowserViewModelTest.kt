package io.swiftfolio.app.ui

import io.swiftfolio.app.MainDispatcherRule
import io.swiftfolio.app.data.StorageUsage
import io.swiftfolio.app.fakes.FakeCredentialStore
import io.swiftfolio.app.fakes.FakeS3Repository
import io.swiftfolio.app.fakes.FakeS3RepositoryFactory
import io.swiftfolio.app.fakes.FakeSettingsStore
import io.swiftfolio.app.fakes.FakeTransferScheduler
import io.swiftfolio.app.fakes.TestData
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
import io.swiftfolio.app.ui.browser.StorageState
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.FieldError
import io.swiftfolio.core.S3Result
import io.swiftfolio.core.SortDirection
import io.swiftfolio.core.SortField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private suspend fun connectedViewModel(): BrowserViewModel {
        sessionManager.connect(TestData.config, remember = false) { _, _ -> }
        return BrowserViewModel(sessionManager, scheduler)
    }

    private fun TestScope.effectsOf(viewModel: BrowserViewModel): List<BrowserEffect> {
        val effects = mutableListOf<BrowserEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(effects) }
        return effects
    }

    @Test fun `binds the session, shows the root and the storage total`() = runTest {
        val vm = connectedViewModel()
        val state = vm.state.value
        assertEquals(TestData.BUCKET, state.bucket)
        assertTrue(state.isConnected)
        assertEquals(ListingState.Loaded(TestData.root), state.listing)
        assertEquals(StorageState.Ready(StorageUsage(4_120, 2)), state.storage)
    }

    @Test fun `no session and nothing saved sends the user to Connect`() = runTest {
        val vm = BrowserViewModel(sessionManager, scheduler)
        val effects = effectsOf(vm)
        assertEquals(listOf(BrowserEffect.NavigateToConnect), effects)
    }

    @Test fun `remembered credentials auto connect`() = runTest {
        credentials.saved = TestData.config
        val vm = BrowserViewModel(sessionManager, scheduler)
        assertTrue(vm.state.value.isConnected)
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
        vm.onAction(BrowserAction.SelectAll)
        assertEquals(3, vm.state.value.selection.size)
        vm.onAction(BrowserAction.Back)
        assertTrue(vm.state.value.selection.isEmpty())
    }

    @Test fun `bulk delete asks first, then deletes, reports and reloads`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowLongPressed(TestData.readme))
        vm.onAction(BrowserAction.RowTapped(TestData.docs))
        vm.onAction(BrowserAction.DeleteSelection)
        val sheet = vm.state.value.sheet as BrowserSheet.ConfirmDelete
        assertEquals(1, sheet.folderCount)
        assertTrue(repository.deletedIds.isEmpty())
        val listsBefore = repository.listCalls.size
        vm.onAction(BrowserAction.ConfirmDelete)
        assertEquals(setOf("README.md", "docs/"), repository.deletedIds.single().toSet())
        assertEquals(BrowserMessage.Deleted(2), vm.state.value.message)
        assertEquals(CommandMode.Browse, vm.state.value.mode)
        assertTrue(repository.listCalls.size > listsBefore)
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
    }

    @Test fun `rejected credentials raise the blocking dialog`() = runTest {
        val vm = connectedViewModel()
        val rejected = ConnectError(ConnectErrorKind.INVALID_ACCESS_KEY, "InvalidAccessKeyId")
        repository.listings[""] = S3Result.Failure(rejected)
        vm.onAction(BrowserAction.Refresh)
        assertEquals(rejected, vm.state.value.credentialsRejected)
        assertEquals(TestData.root, vm.state.value.entries)
    }

    @Test fun `uploads go to the scheduler for the current folder`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowTapped(TestData.photos))
        vm.onAction(BrowserAction.FilesPicked(listOf("content://a", "content://b")))
        assertEquals(Triple(TestData.BUCKET, "photos/", listOf("content://a", "content://b")), scheduler.uploadRequests.single())
        assertEquals(BrowserMessage.UploadsQueued(2), vm.state.value.message)
    }

    @Test fun `finished upload batch refreshes the folder`() = runTest {
        val vm = connectedViewModel()
        val item = TransferItem("a.txt", "a.txt", 10, 0, TransferItemStatus.RUNNING)
        val running = TransferBatch("b1", TransferKind.UPLOAD, TestData.BUCKET, "", listOf(item), isActive = true)
        scheduler.queueState.value = TransferQueue(listOf(running))
        assertEquals(0, vm.state.value.uploadProgress?.percent)
        val listsBefore = repository.listCalls.size
        val done = running.copy(items = listOf(item.copy(bytesDone = 10, status = TransferItemStatus.DONE)), isActive = false)
        scheduler.queueState.value = TransferQueue(listOf(done))
        assertEquals(BrowserMessage.UploadsFinished(1, ""), vm.state.value.message)
        assertTrue(repository.listCalls.size > listsBefore)
        assertNull(vm.state.value.uploadProgress)
    }

    @Test fun `downloads skip folders and leave selection mode`() = runTest {
        val vm = connectedViewModel()
        vm.onAction(BrowserAction.RowLongPressed(TestData.readme))
        vm.onAction(BrowserAction.RowTapped(TestData.docs))
        vm.onAction(BrowserAction.DownloadSelection)
        assertEquals(TestData.BUCKET to listOf("README.md"), scheduler.downloadRequests.single())
        assertEquals(CommandMode.Browse, vm.state.value.mode)
    }
}
