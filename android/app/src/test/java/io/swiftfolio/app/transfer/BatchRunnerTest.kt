package io.swiftfolio.app.transfer

import io.swiftfolio.app.fakes.FakeDownloadTargets
import io.swiftfolio.app.fakes.FakeS3Repository
import io.swiftfolio.app.fakes.FakeUploadSources
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BatchRunnerTest {
    @get:Rule val folder = TemporaryFolder()

    private val repository = FakeS3Repository()
    private val control = TransferControl()
    private val uploads = FakeUploadSources()
    private val downloads = FakeDownloadTargets()
    private val items = listOf("a.txt", "b.txt", "c.txt", "d.txt").map { ManifestItem(it, "docs/$it", sizeBytes = 10, source = "content://$it") }

    private fun runner(store: OutcomeLog, kind: TransferKind = TransferKind.UPLOAD) =
        BatchRunner(TransferManifest("b1", kind, "media-prod", "docs/", items), control, uploads, downloads, store)

    @Test fun `every finished file is recorded before the next one starts`() = runTest {
        val store = TransferManifestStore(folder.root)
        val snapshot = runner(store).run(repository) {}
        assertEquals(items.map { it.key }, repository.uploadedKeys)
        assertTrue(snapshot.items.all { it.status == TransferItemStatus.DONE })
        assertEquals((0..3).toSet(), TransferManifestStore(folder.root).outcomes("b1").keys)
    }

    @Test fun `a re-run skips done and cancelled files and retries failed ones`() = runTest {
        val store = TransferManifestStore(folder.root)
        store.record("b1", 0, ItemState(TransferItemStatus.DONE, 10))
        store.record("b1", 1, ItemState(TransferItemStatus.CANCELLED))
        store.record("b1", 2, ItemState(TransferItemStatus.FAILED, errorCode = "NetworkError"))

        val snapshot = runner(TransferManifestStore(folder.root)).run(repository) {}

        assertEquals(listOf("docs/c.txt", "docs/d.txt"), repository.uploadedKeys)
        assertEquals(
            listOf(TransferItemStatus.DONE, TransferItemStatus.CANCELLED, TransferItemStatus.DONE, TransferItemStatus.DONE),
            snapshot.items.map { it.status },
        )
    }

    @Test fun `without a session every unfinished file fails`() = runTest {
        val snapshot = runner(TransferManifestStore(folder.root)).run(repository = null) {}
        assertTrue(snapshot.items.all { it.status == TransferItemStatus.FAILED && it.errorCode == "SessionExpired" })
        assertTrue(repository.uploadedKeys.isEmpty())
    }

    @Test fun `a file cancelled before it starts is skipped`() = runTest {
        control.cancel(ItemRef("b1", 1))
        val snapshot = runner(TransferManifestStore(folder.root)).run(repository) {}
        assertEquals(TransferItemStatus.CANCELLED, snapshot.items[1].status)
        assertEquals(3, repository.uploadedKeys.size)
    }

    @Test fun `downloads keep their openable uri, failed ones are discarded`() = runTest {
        val store = TransferManifestStore(folder.root)
        val done = runner(store, TransferKind.DOWNLOAD).run(repository) {}
        assertEquals("content://downloads/a.txt", done.items[0].savedUri)
        assertEquals(items.map { it.name }, downloads.committed)

        repository.downloadResult = S3Result.Failure(ConnectError(ConnectErrorKind.ACCESS_DENIED, "AccessDenied"))
        val failed = BatchRunner(TransferManifest("b2", TransferKind.DOWNLOAD, "media-prod", "docs/", items), control, uploads, downloads, store)
            .run(repository) {}
        assertTrue(failed.items.all { it.status == TransferItemStatus.FAILED && it.savedUri == null })
        assertEquals(items.map { it.name }, downloads.discarded)
    }

    @Test fun `progress updates carry totals of the running file`() = runTest {
        val updates = mutableListOf<BatchTotals>()
        runner(TransferManifestStore(folder.root)).run(repository) { updates += it.totals() }
        assertEquals(BatchTotals(filesDone = 0, filesTotal = 4, bytesDone = 0, runningIndex = 0, runningBytes = 0), updates.first())
        assertEquals(BatchTotals(filesDone = 4, filesTotal = 4, bytesDone = 40, runningIndex = null, runningBytes = 0), updates.last())
    }
}
