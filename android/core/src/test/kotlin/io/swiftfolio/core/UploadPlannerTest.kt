package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadPlannerTest {
    private val mib = 1024L * 1024L

    @Test fun `single put up to five MiB`() {
        assertTrue(UploadPlanner.isSinglePut(0))
        assertTrue(UploadPlanner.isSinglePut(5 * mib))
        assertFalse(UploadPlanner.isSinglePut(5 * mib + 1))
    }

    @Test fun `parts cover the file exactly`() {
        val size = 12 * mib + 7
        val parts = UploadPlanner.parts(size)
        assertEquals(3, parts.size)
        assertEquals(listOf(1, 2, 3), parts.map { it.partNumber })
        assertEquals(size, parts.sumOf { it.length })
        assertEquals(10 * mib, parts.last().offset)
        assertEquals(emptyList<PartRange>(), UploadPlanner.parts(0))
    }

    @Test fun `part size grows to respect the part limit`() {
        assertEquals(5 * mib, UploadPlanner.partSize(null))
        assertEquals(5 * mib, UploadPlanner.partSize(50_000 * mib))
        val huge = 100_000 * mib
        val partSize = UploadPlanner.partSize(huge)
        assertEquals(10 * mib, partSize)
        assertTrue(UploadPlanner.parts(huge, partSize).size <= CoreConstants.MAX_MULTIPART_PARTS)
    }

    @Test fun `object keys never create folders by accident`() {
        assertEquals("p/a_b.txt", UploadPlanner.objectKey("p/", "a/b.txt"))
        assertEquals("upload", UploadPlanner.objectKey("", "  "))
    }

    @Test fun `deduplicate appends a counter before the extension`() {
        val existing = setOf("p/a.txt", "p/a (1).txt", "p/README")
        assertEquals("p/b.txt", UploadPlanner.deduplicate("p/b.txt", existing))
        assertEquals("p/a (2).txt", UploadPlanner.deduplicate("p/a.txt", existing))
        assertEquals("p/README (1)", UploadPlanner.deduplicate("p/README", existing))
    }

    @Test fun `transfer progress aggregates bytes or files`() {
        val progress = TransferProgress.of(listOf(50L to 100L, 100L to 100L, 7L to null), finished = 1)
        assertEquals(150L, progress.bytesDone)
        assertEquals(200L, progress.bytesTotal)
        assertEquals(75, progress.percent)
        val unknownSizes = TransferProgress.of(listOf(1L to null, 2L to null), finished = 1)
        assertEquals(0.5f, unknownSizes.fraction)
        assertEquals(0f, TransferProgress.EMPTY.fraction)
    }
}
