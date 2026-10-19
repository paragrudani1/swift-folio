package io.swiftfolio.app.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TransferManifestStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val manifest = TransferManifest(
        batchId = "b1",
        kind = TransferKind.UPLOAD,
        bucket = "media-prod",
        prefix = "photos/",
        items = listOf(ManifestItem("a.jpg", "photos/a.jpg", 10, "content://a"), ManifestItem("b.jpg", "photos/b.jpg", 20, "content://b")),
    )

    @Test fun `manifests and outcomes survive a new process`() {
        val store = TransferManifestStore(folder.root)
        store.save(manifest)
        assertTrue(store.record("b1", 0, ItemState(TransferItemStatus.DONE, 10)))
        store.record("b1", 1, ItemState(TransferItemStatus.FAILED, 5, "NetworkError"))
        store.record("b1", 1, ItemState(TransferItemStatus.DONE, 20))

        val restarted = TransferManifestStore(folder.root)
        assertEquals(manifest, restarted.load("b1"))
        assertEquals(
            mapOf(0 to ItemState(TransferItemStatus.DONE, 10), 1 to ItemState(TransferItemStatus.DONE, 20)),
            restarted.outcomes("b1"),
        )
    }

    @Test fun `a torn last line from a crash mid-write is ignored`() {
        TransferManifestStore(folder.root).record("b1", 0, ItemState(TransferItemStatus.DONE, 10))
        folder.root.listFiles()!!.single { it.name.endsWith(".jsonl") }.appendText("{\"index\":1,\"sta")
        assertEquals(mapOf(0 to ItemState(TransferItemStatus.DONE, 10)), TransferManifestStore(folder.root).outcomes("b1"))
    }

    @Test fun `delete removes the manifest and its outcomes`() {
        val store = TransferManifestStore(folder.root)
        store.save(manifest)
        store.record("b1", 0, ItemState(TransferItemStatus.DONE, 10))
        store.delete(listOf("b1"))
        assertNull(store.load("b1"))
        assertTrue(store.outcomes("b1").isEmpty())
        assertFalse(folder.root.listFiles()!!.any { it.name.startsWith("b1") })
    }
}
