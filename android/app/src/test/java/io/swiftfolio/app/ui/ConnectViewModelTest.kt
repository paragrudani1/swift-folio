package io.swiftfolio.app.ui

import io.swiftfolio.app.MainDispatcherRule
import io.swiftfolio.app.data.AppSettings
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.app.fakes.FakeCredentialStore
import io.swiftfolio.app.fakes.FakeS3Repository
import io.swiftfolio.app.fakes.FakeS3RepositoryFactory
import io.swiftfolio.app.fakes.FakeSettingsStore
import io.swiftfolio.app.fakes.TestData
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.app.ui.connect.ConnectAction
import io.swiftfolio.app.ui.connect.ConnectEffect
import io.swiftfolio.app.ui.connect.ConnectField
import io.swiftfolio.app.ui.connect.ConnectViewModel
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.FieldError
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val credentials = FakeCredentialStore()
    private val settings = FakeSettingsStore(AppSettings(themeMode = ThemeMode.DARK))
    private var nextRepository = FakeS3Repository()
    private val factory = FakeS3RepositoryFactory { nextRepository }
    private val sessionManager = SessionManager(credentials, settings, factory)

    private fun viewModel() = ConnectViewModel(sessionManager, settings)

    private fun ConnectViewModel.fill(region: String = "us-east-1") {
        onAction(ConnectAction.AccessKeyChanged(TestData.ACCESS_KEY))
        onAction(ConnectAction.SecretChanged(TestData.SECRET))
        onAction(ConnectAction.BucketChanged(TestData.BUCKET))
        onAction(ConnectAction.RegionSelected(region))
    }

    private fun TestScope.effectsOf(viewModel: ConnectViewModel): List<ConnectEffect> {
        val effects = mutableListOf<ConnectEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(effects) }
        return effects
    }

    @Test fun `invalid form does not connect and reveals every field error`() = runTest {
        val vm = viewModel()
        vm.onAction(ConnectAction.AccessKeyChanged("AKIA"))
        vm.onAction(ConnectAction.Submit)
        val state = vm.state.value
        assertTrue(factory.created.isEmpty())
        assertEquals(ConnectField.entries.toSet(), state.touchedFields)
        assertEquals(FieldError.TOO_SHORT, state.accessKeyError)
        assertEquals(FieldError.EMPTY, state.bucketError)
    }

    @Test fun `errors stay hidden until a field is blurred`() = runTest {
        val vm = viewModel()
        vm.onAction(ConnectAction.BucketChanged("Bad_Bucket"))
        assertNull(vm.state.value.bucketError)
        vm.onAction(ConnectAction.FieldBlurred(ConnectField.BUCKET))
        assertEquals(FieldError.UPPERCASE, vm.state.value.bucketError)
    }

    @Test fun `successful connect ticks every step and navigates`() = runTest {
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fill()
        vm.onAction(ConnectAction.RememberChanged(true))
        vm.onAction(ConnectAction.Submit)
        assertEquals(listOf(ConnectEffect.NavigateToBrowser), effects)
        assertTrue(vm.state.value.steps.all { it.status == StepStatus.DONE })
        assertFalse(vm.state.value.isConnecting)
        assertEquals(TestData.config, credentials.saved)
    }

    @Test fun `wrong region offers a one tap fix that reconnects`() = runTest {
        nextRepository = FakeS3Repository().apply {
            verifyResult = S3Result.Failure(ConnectError(ConnectErrorKind.WRONG_REGION, "PermanentRedirect", suggestedRegion = "eu-west-1"))
        }
        val vm = viewModel()
        val effects = effectsOf(vm)
        vm.fill()
        vm.onAction(ConnectAction.Submit)
        assertEquals("eu-west-1", vm.state.value.error?.suggestedRegion)
        assertEquals(ConnectField.REGION, vm.state.value.errorField)

        nextRepository = FakeS3Repository()
        vm.onAction(ConnectAction.ApplySuggestedRegion)
        assertEquals("eu-west-1", factory.created.last().first.region)
        assertEquals(listOf(ConnectEffect.NavigateToBrowser), effects)
    }

    @Test fun `editing a field clears the server error`() = runTest {
        nextRepository = FakeS3Repository().apply {
            verifyResult = S3Result.Failure(ConnectError(ConnectErrorKind.SIGNATURE_MISMATCH, "SignatureDoesNotMatch"))
        }
        val vm = viewModel()
        vm.fill()
        vm.onAction(ConnectAction.Submit)
        assertEquals(ConnectField.SECRET, vm.state.value.errorField)
        vm.onAction(ConnectAction.SecretChanged(TestData.SECRET + "x"))
        assertNull(vm.state.value.error)
    }

    @Test fun `credentials are not remembered unless the user opts in`() = runTest {
        val vm = viewModel()
        assertFalse(vm.state.value.remember)
        vm.fill()
        vm.onAction(ConnectAction.Submit)
        assertNull(credentials.saved)
    }

    @Test fun `state and actions never print the secret`() = runTest {
        val vm = viewModel()
        vm.fill()
        assertFalse(vm.state.value.toString().contains(TestData.SECRET))
        assertFalse(ConnectAction.SecretChanged(TestData.SECRET).toString().contains(TestData.SECRET))
    }

    @Test fun `leaving the foreground masks the secret`() = runTest {
        val vm = viewModel()
        vm.onAction(ConnectAction.SecretVisibilityToggled)
        vm.onAction(ConnectAction.AppBackgrounded)
        assertFalse(vm.state.value.secretVisible)
    }

    @Test fun `revealed secret is masked again after ten seconds`() = runTest {
        val vm = viewModel()
        vm.onAction(ConnectAction.SecretVisibilityToggled)
        assertTrue(vm.state.value.secretVisible)
        advanceTimeBy(10_001)
        assertFalse(vm.state.value.secretVisible)
    }

    @Test fun `a failed auto connect prefills the form with its error`() = runTest {
        credentials.saved = TestData.config
        val error = ConnectError(ConnectErrorKind.NETWORK, "UnknownHostException")
        val failingManager = SessionManager(credentials, settings, FakeS3RepositoryFactory.failing(error))
        failingManager.restore()
        val vm = ConnectViewModel(failingManager, settings)
        assertEquals(TestData.BUCKET, vm.state.value.bucket)
        assertEquals(error, vm.state.value.error)
    }

    @Test fun `theme toggle cycles the stored mode`() = runTest {
        val vm = viewModel()
        assertEquals(ThemeMode.DARK, vm.state.value.themeMode)
        vm.onAction(ConnectAction.CycleTheme)
        assertEquals(ThemeMode.SYSTEM, vm.state.value.themeMode)
    }
}
