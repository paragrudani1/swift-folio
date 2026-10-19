package io.swiftfolio.app.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatchWorkTest {
    @Test fun `only batches whose work ended are cleared`() {
        val works = listOf(
            BatchWork("done", isFinished = true),
            BatchWork("running", isFinished = false),
            BatchWork("queued", isFinished = false),
            BatchWork("cancelled", isFinished = true),
        )
        assertEquals(setOf("done", "cancelled"), finishedBatchIds(works))
    }

    @Test fun `a batch that is still pending is never cleared`() {
        val works = listOf(BatchWork("b1", isFinished = true), BatchWork("b1", isFinished = false))
        assertEquals(emptySet<String>(), finishedBatchIds(works))
    }

    @Test fun `batch tags round-trip`() {
        assertEquals("b1", BatchTags.batchIdOf(setOf(BatchTags.TRANSFER, BatchTags.forBatch("b1"))))
        assertNull(BatchTags.batchIdOf(setOf(BatchTags.TRANSFER)))
    }
}
