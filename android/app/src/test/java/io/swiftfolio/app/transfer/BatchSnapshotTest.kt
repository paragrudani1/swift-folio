package io.swiftfolio.app.transfer

import org.junit.Assert.assertEquals
import org.junit.Test

class BatchSnapshotTest {
    private val done = ItemState(TransferItemStatus.DONE, 10)

    @Test fun `recorded outcomes win, the running file comes from totals, the rest is pending`() {
        val totals = BatchTotals(filesDone = 1, filesTotal = 3, bytesDone = 14, runningIndex = 1, runningBytes = 4)
        val snapshot = BatchSnapshot.assemble(3, mapOf(0 to done), totals, ItemState.QUEUED)
        assertEquals(listOf(done, ItemState(TransferItemStatus.RUNNING, 4), ItemState.QUEUED), snapshot.items)
    }

    @Test fun `a finished batch shows unrecorded files with the batch state`() {
        val cancelled = ItemState(TransferItemStatus.CANCELLED)
        val snapshot = BatchSnapshot.assemble(2, mapOf(0 to done), totals = null, pending = cancelled)
        assertEquals(listOf(done, cancelled), snapshot.items)
    }

    @Test fun `progress uses manifest sizes`() {
        val snapshot = BatchSnapshot(listOf(done, ItemState(TransferItemStatus.RUNNING, 5)))
        val progress = snapshot.progress(listOf(10, 20))
        assertEquals(15, progress.bytesDone)
        assertEquals(30, progress.bytesTotal)
        assertEquals(1, progress.filesDone)
    }
}
