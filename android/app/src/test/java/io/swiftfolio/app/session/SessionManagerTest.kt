package io.swiftfolio.app.session

import io.swiftfolio.app.fakes.FakeCredentialStore
import io.swiftfolio.app.fakes.FakeS3Repository
import io.swiftfolio.app.fakes.FakeS3RepositoryFactory
import io.swiftfolio.app.fakes.FakeSettingsStore
import io.swiftfolio.app.fakes.TestData
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    private val credentials = FakeCredentialStore()
    private val settings = FakeSettingsStore()
    private val factory = FakeS3RepositoryFactory()
    private val manager = SessionManager(credentials, settings, factory)

    @Test fun `connect with remember saves credentials and activates the session`() = runTest {
        val result = manager.connect(TestData.config, remember = true) { _, _ -> }
        assertTrue(result is S3Result.Success)
        assertEquals(TestData.config, credentials.saved)
        assertTrue(settings.settings.first().rememberCredentials)
        assertEquals(TestData.root, manager.session.value?.initialListing)
    }

    @Test fun `connect without remember wipes previously saved credentials`() = runTest {
        credentials.saved = TestData.config
        manager.connect(TestData.config, remember = false) { _, _ -> }
        assertNull(credentials.saved)
    }

    @Test fun `failed connect closes the repository and keeps no session`() = runTest {
        val denied = ConnectError(ConnectErrorKind.ACCESS_DENIED, "AccessDenied")
        val failing = FakeS3RepositoryFactory.failing(denied)
        val failingManager = SessionManager(credentials, settings, failing)
        val result = failingManager.connect(TestData.config, remember = true) { _, _ -> }
        assertEquals(S3Result.Failure(denied), result)
        assertNull(failingManager.session.value)
        assertTrue(failing.created.single().second.closed)
        assertNull(credentials.saved)
    }

    @Test fun `restore reports nothing saved`() = runTest {
        assertEquals(RestoreResult.NothingSaved, manager.restore())
    }

    @Test fun `restore failure is handed to Connect exactly once`() = runTest {
        credentials.saved = TestData.config
        val error = ConnectError(ConnectErrorKind.NETWORK, "UnknownHostException")
        val failingManager = SessionManager(credentials, settings, FakeS3RepositoryFactory.failing(error))
        val restored = failingManager.restore()
        assertTrue(restored is RestoreResult.Failed)
        assertEquals(error, failingManager.takeRestoreFailure()?.error)
        assertNull(failingManager.takeRestoreFailure())
    }

    @Test fun `sign out can forget credentials`() = runTest {
        manager.connect(TestData.config, remember = true) { _, _ -> }
        val repository = manager.session.value!!.repository as FakeS3Repository
        manager.signOut(forgetCredentials = true)
        assertNull(manager.session.value)
        assertTrue(repository.closed)
        assertNull(credentials.saved)
    }

    @Test fun `turning remember off wipes saved credentials immediately`() = runTest {
        manager.connect(TestData.config, remember = true) { _, _ -> }
        manager.setRememberCredentials(false)
        assertNull(credentials.saved)
        manager.setRememberCredentials(true)
        assertEquals(TestData.config, credentials.saved)
    }

    @Test fun `background work reuses the session repository or builds one from saved keys`() = runTest {
        manager.connect(TestData.config, remember = true) { _, _ -> }
        val sessionRepository = manager.session.value!!.repository
        assertSame(sessionRepository, manager.repositoryFor(TestData.BUCKET))
        assertTrue(manager.isSessionRepository(sessionRepository))
        manager.signOut(forgetCredentials = false)
        val rebuilt = manager.repositoryFor(TestData.BUCKET)
        assertTrue(rebuilt != null && !manager.isSessionRepository(rebuilt))
        assertNull(manager.repositoryFor("other-bucket"))
    }
}
