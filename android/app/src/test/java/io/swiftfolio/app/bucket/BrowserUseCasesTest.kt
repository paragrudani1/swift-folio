package io.swiftfolio.app.bucket

import io.swiftfolio.app.data.StorageUsage
import io.swiftfolio.app.fakes.FakeS3Repository
import io.swiftfolio.app.fakes.TestData
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserUseCasesTest {
    private val repository = FakeS3Repository().apply { listings["photos/"] = S3Result.Success(TestData.photosListing) }
    private var connected = true
    private val useCases = BrowserUseCases.create { repository.takeIf { connected } }
    private val listings = useCases.listings
    private val storage = useCases.storage
    private val mutations = useCases.mutations

    private fun usage() = (storage.state.value as StorageState.Ready).usage

    @Test fun `listings are cached on success and kept on failure`() = runTest {
        listings.load("photos/")
        assertEquals(TestData.photosListing, listings.cached("photos/"))
        assertEquals(4_000L, listings.sizeOf(TestData.sunset.key))
        repository.listings["photos/"] = S3Result.Failure(ConnectError(ConnectErrorKind.NETWORK, "UnknownHost"))
        assertTrue(listings.load("photos/") is S3Result.Failure)
        assertEquals(TestData.photosListing, listings.cached("photos/"))
    }

    @Test fun `no session is a failure, not a crash`() = runTest {
        connected = false
        assertEquals(NoSessionFailure, listings.load(""))
        storage.rescan()
        assertTrue(storage.state.value is StorageState.Unavailable)
    }

    @Test fun `local adjustments keep the total without rescanning`() = runTest {
        storage.rescan()
        assertEquals(StorageUsage(4_120, 2), usage())
        storage.adjust(bytes = -5_000, objects = 1)
        assertEquals(StorageUsage(0, 3), usage())
        assertEquals(1, repository.storageCalls)
    }

    @Test fun `adjustments are ignored until a total is known`() {
        storage.adjust(bytes = 10, objects = 1)
        assertEquals(StorageState.Computing, storage.state.value)
    }

    @Test fun `creating a folder drops the parent listing and counts one object`() = runTest {
        storage.rescan()
        listings.load("")
        assertEquals(S3Result.Success(Unit), mutations.createFolder("", "invoices"))
        assertEquals(listOf("invoices/"), repository.createdPrefixes)
        assertNull(listings.cached(""))
        assertEquals(StorageUsage(4_120, 3), usage())
    }

    @Test fun `deleting subtracts listed file sizes and the repository's folder sizes`() = runTest {
        storage.rescan()
        listings.load("")
        repository.deleteFreedBytes = 3_000
        mutations.delete(listOf(TestData.readme.key, TestData.docs.prefix)) {}
        assertEquals(StorageUsage(4_120 - 120 - 3_000, 0), usage())
        assertNull(listings.cached(""))
    }

    @Test fun `failed deletes are not subtracted`() = runTest {
        storage.rescan()
        listings.load("")
        repository.deleteFailures = listOf(TestData.readme.key)
        mutations.delete(listOf(TestData.readme.key)) {}
        assertEquals(StorageUsage(4_120, 2), usage())
    }

    @Test fun `a finished upload is added and its folder reloads next time`() = runTest {
        storage.rescan()
        listings.load("photos/")
        mutations.recordUpload("photos/", bytes = 880, objects = 2)
        assertEquals(StorageUsage(5_000, 4), usage())
        assertNull(listings.cached("photos/"))
    }
}
