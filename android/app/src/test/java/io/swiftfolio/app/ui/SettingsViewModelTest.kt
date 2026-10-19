package io.swiftfolio.app.ui

import io.swiftfolio.app.MainDispatcherRule
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.app.fakes.FakeCredentialStore
import io.swiftfolio.app.fakes.FakeS3RepositoryFactory
import io.swiftfolio.app.fakes.FakeSettingsStore
import io.swiftfolio.app.fakes.FakeTransferScheduler
import io.swiftfolio.app.fakes.TestData
import io.swiftfolio.app.session.CancelTransfersAndSignOut
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.transfer.TransferKind
import io.swiftfolio.app.ui.settings.SettingsAction
import io.swiftfolio.app.ui.settings.SettingsEffect
import io.swiftfolio.app.ui.settings.SettingsViewModel
import io.swiftfolio.app.ui.settings.SignOutDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val credentials = FakeCredentialStore()
    private val settings = FakeSettingsStore()
    private val sessionManager = SessionManager(credentials, settings, FakeS3RepositoryFactory())
    private val scheduler = FakeTransferScheduler()

    private suspend fun viewModel(): SettingsViewModel {
        sessionManager.connect(TestData.config, remember = true) { _, _ -> }
        return SettingsViewModel(sessionManager, settings, CancelTransfersAndSignOut(sessionManager, scheduler), appVersion = "1.0.0")
    }

    @Test fun `shows the connection with a masked key`() = runTest {
        val state = viewModel().state.value
        assertEquals(TestData.BUCKET, state.bucket)
        assertEquals("••••MPLE", state.maskedAccessKey)
        assertTrue(state.isConnected)
        assertTrue(state.rememberCredentials)
        assertEquals("1.0.0", state.appVersion)
    }

    @Test fun `theme choice is persisted`() = runTest {
        val vm = viewModel()
        vm.onAction(SettingsAction.SetTheme(ThemeMode.LIGHT))
        assertEquals(ThemeMode.LIGHT, vm.state.value.themeMode)
    }

    @Test fun `turning remember off forgets saved keys`() = runTest {
        val vm = viewModel()
        vm.onAction(SettingsAction.SetRemember(false))
        assertNull(credentials.saved)
        assertEquals(false, vm.state.value.rememberCredentials)
    }

    @Test fun `sign out defaults to forgetting when remember was on and navigates`() = runTest {
        val vm = viewModel()
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.toList(effects) }
        vm.onAction(SettingsAction.RequestSignOut)
        assertEquals(SignOutDialog(forgetCredentials = true), vm.state.value.signOutDialog)
        vm.onAction(SettingsAction.ConfirmSignOut)
        assertNull(sessionManager.session.value)
        assertNull(credentials.saved)
        assertEquals(TransferKind.entries.toList(), scheduler.cancelledKinds)
        assertEquals(listOf(SettingsEffect.NavigateToConnect), effects)
    }

    @Test fun `sign out can keep saved keys`() = runTest {
        val vm = viewModel()
        vm.onAction(SettingsAction.RequestSignOut)
        vm.onAction(SettingsAction.ForgetOnSignOutChanged(false))
        vm.onAction(SettingsAction.ConfirmSignOut)
        assertEquals(TestData.config, credentials.saved)
    }
}
