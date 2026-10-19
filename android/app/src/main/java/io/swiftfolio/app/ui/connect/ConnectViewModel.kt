package io.swiftfolio.app.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.swiftfolio.app.AppConstants
import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.SettingsStore
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.session.SessionManager
import io.swiftfolio.core.S3Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the Connect form state and delegates verification to [SessionManager].
 * Contains no S3 or Android framework calls.
 */
class ConnectViewModel(
    private val sessionManager: SessionManager,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConnectUiState())
    private val effectChannel = Channel<ConnectEffect>(Channel.BUFFERED)
    private var remaskJob: Job? = null

    /** Current screen state. */
    val state: StateFlow<ConnectUiState> = mutableState.asStateFlow()

    /** One-shot navigation effects. */
    val effects: Flow<ConnectEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { settingsStore.settings.collect { settings -> mutableState.update { it.copy(themeMode = settings.themeMode) } } }
        viewModelScope.launch { prefill() }
    }

    /** Single entry point for user intents. */
    fun onAction(action: ConnectAction) {
        when (action) {
            is ConnectAction.AccessKeyChanged -> edit { copy(accessKeyId = action.value.trim()) }
            is ConnectAction.SecretChanged -> edit { copy(secretAccessKey = action.value) }
            is ConnectAction.BucketChanged -> edit { copy(bucket = action.value.trim()) }
            is ConnectAction.RegionSelected -> edit { copy(region = action.code) }
            is ConnectAction.RememberChanged -> mutableState.update { it.copy(remember = action.remember) }
            is ConnectAction.FieldBlurred -> mutableState.update { it.copy(touchedFields = it.touchedFields + action.field) }
            ConnectAction.SecretVisibilityToggled -> toggleSecret()
            ConnectAction.AppBackgrounded -> mutableState.update { it.copy(secretVisible = false) }
            ConnectAction.Submit -> connect()
            ConnectAction.ApplySuggestedRegion -> applySuggestedRegion()
            ConnectAction.DismissError -> mutableState.update { it.copy(error = null) }
            ConnectAction.CycleTheme -> viewModelScope.launch { settingsStore.setThemeMode(state.value.themeMode.next()) }
        }
    }

    /** Fills the form from a failed auto-connect or from saved credentials, if the user has not typed yet. */
    private suspend fun prefill() {
        val failure = sessionManager.takeRestoreFailure()
        val saved = failure?.config ?: sessionManager.savedConfig() ?: return
        val remember = settingsStore.settings.first().rememberCredentials
        mutableState.update { current ->
            if (current.accessKeyId.isNotEmpty() || current.bucket.isNotEmpty()) return@update current
            current.copy(
                accessKeyId = saved.accessKeyId,
                secretAccessKey = saved.secretAccessKey,
                bucket = saved.bucket,
                region = saved.region,
                remember = remember,
                error = failure?.error,
            )
        }
    }

    /** Editing a field clears a stale server error and the checklist. */
    private fun edit(transform: ConnectUiState.() -> ConnectUiState) {
        mutableState.update { it.transform().copy(error = null, steps = if (it.isConnecting) it.steps else emptyList()) }
    }

    private fun toggleSecret() {
        val visible = !state.value.secretVisible
        mutableState.update { it.copy(secretVisible = visible) }
        remaskJob?.cancel()
        if (visible) {
            remaskJob = viewModelScope.launch {
                delay(AppConstants.SECRET_REVEAL_MILLIS)
                mutableState.update { it.copy(secretVisible = false) }
            }
        }
    }

    private fun applySuggestedRegion() {
        val region = state.value.error?.suggestedRegion ?: return
        mutableState.update { it.copy(region = region, error = null) }
        connect()
    }

    private fun connect() {
        val current = state.value
        if (!current.canSubmit) {
            mutableState.update { it.copy(touchedFields = ConnectField.entries.toSet()) }
            return
        }
        mutableState.update {
            it.copy(isConnecting = true, error = null, secretVisible = false, steps = ConnectStep.entries.map { step -> StepRow(step, StepStatus.PENDING) })
        }
        viewModelScope.launch {
            val result = sessionManager.connect(current.config, remember = current.remember, onStep = ::updateStep)
            when (result) {
                is S3Result.Success -> {
                    mutableState.update { it.copy(isConnecting = false) }
                    effectChannel.send(ConnectEffect.NavigateToBrowser)
                }
                is S3Result.Failure -> mutableState.update { it.copy(isConnecting = false, error = result.error) }
            }
        }
    }

    private fun updateStep(step: ConnectStep, status: StepStatus) {
        mutableState.update { state ->
            state.copy(steps = state.steps.map { row -> if (row.step == step) row.copy(status = status) else row })
        }
    }
}
