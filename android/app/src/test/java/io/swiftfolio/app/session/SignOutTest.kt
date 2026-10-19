package io.swiftfolio.app.session

import io.swiftfolio.app.fakes.FakeCredentialStore
import io.swiftfolio.app.fakes.FakeS3RepositoryFactory
import io.swiftfolio.app.fakes.FakeSettingsStore
import io.swiftfolio.app.fakes.FakeTransferScheduler
import io.swiftfolio.app.fakes.TestData
import io.swiftfolio.app.transfer.TransferKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignOutTest {
    private val credentials = FakeCredentialStore()
    private val manager = SessionManager(credentials, FakeSettingsStore(), FakeS3RepositoryFactory())
    private val scheduler = FakeTransferScheduler()

    @Test fun `signing out cancels every transfer before ending the session`() = runTest {
        manager.connect(TestData.config, remember = true) { _, _ -> }
        CancelTransfersAndSignOut(manager, scheduler)(forgetCredentials = true)
        assertEquals(TransferKind.entries.toList(), scheduler.cancelledKinds)
        assertNull(manager.session.value)
        assertNull(credentials.saved)
    }
}
