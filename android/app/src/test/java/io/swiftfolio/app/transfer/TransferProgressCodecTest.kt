package io.swiftfolio.app.transfer

import androidx.work.Data
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferProgressCodecTest {
    @Test fun `totals round-trip`() {
        val totals = BatchTotals(filesDone = 3, filesTotal = 7, bytesDone = 4_096, runningIndex = 3, runningBytes = 512)
        assertEquals(totals, TransferProgressCodec.decode(TransferProgressCodec.encode(totals)))
        val between = totals.copy(runningIndex = null, runningBytes = 0)
        assertEquals(between, TransferProgressCodec.decode(TransferProgressCodec.encode(between)))
    }

    @Test fun `data size does not grow with the batch`() {
        val small = BatchSnapshot(List(1) { ItemState(TransferItemStatus.DONE, 10) })
        val huge = BatchSnapshot(List(10_000) { ItemState(TransferItemStatus.FAILED, 10, "NetworkError") })
        val smallKeys = TransferProgressCodec.encode(small.totals()).keyValueMap.keys
        val hugeKeys = TransferProgressCodec.encode(huge.totals()).keyValueMap.keys
        assertEquals(smallKeys, hugeKeys)
        assertEquals(5, hugeKeys.size)
    }

    @Test fun `data without totals decodes to null`() {
        assertNull(TransferProgressCodec.decode(Data.EMPTY))
    }
}
