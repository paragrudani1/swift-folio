package io.swiftfolio.app.ui.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.swiftfolio.app.R
import io.swiftfolio.app.data.ConnectStep
import io.swiftfolio.app.data.StepStatus
import io.swiftfolio.app.ui.components.GradientButton
import io.swiftfolio.app.ui.components.PreviewSurface
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.aurora
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind

private val ContentMaxWidth = 560.dp

/** Stateful wrapper: wires [ConnectViewModel] to [ConnectContent] and handles effects. */
@Composable
fun ConnectScreen(viewModel: ConnectViewModel, onConnected: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ConnectEffect.NavigateToBrowser -> onConnected()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAction(ConnectAction.AppBackgrounded) }
    ConnectContent(state = state, onAction = viewModel::onAction)
}

/** Stateless Connect screen (DIRECTION-C.md 6.1). */
@Composable
fun ConnectContent(state: ConnectUiState, onAction: (ConnectAction) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Box(modifier.fillMaxSize().aurora(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter),
        ) {
            AppMarkRow(themeMode = state.themeMode, onCycleTheme = { onAction(ConnectAction.CycleTheme) })
            Spacer(Modifier.height(Spacing.space12))
            ConnectHeader()
            Spacer(Modifier.height(Spacing.space6))
            CredentialCard(state = state, onAction = onAction)
            ErrorBannerSlot(state.error, onAction)
            Spacer(Modifier.height(Spacing.space6))
            GradientButton(
                text = stringResource(R.string.connect_button),
                onClick = { onAction(ConnectAction.Submit) },
                enabled = state.canSubmit,
                loading = state.isConnecting,
                loadingLabel = stringResource(R.string.connect_button_loading, state.bucket),
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
            )
            ConnectChecklist(steps = state.steps, modifier = Modifier.padding(top = Spacing.space4))
            Spacer(Modifier.height(Spacing.space6))
            Text(stringResource(R.string.connect_permissions_footer), style = ConsoleTheme.mono.meta, color = ConsoleTheme.colors.textTertiary)
            Spacer(Modifier.height(Spacing.space8))
        }
    }
}

@Composable
private fun ConnectHeader() {
    val colors = ConsoleTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.connect_eyebrow), style = ConsoleTheme.mono.caps, color = colors.textTertiary)
        Text(stringResource(R.string.connect_title), style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
        Text(stringResource(R.string.connect_subtitle), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
    }
}

@Composable
private fun ErrorBannerSlot(error: ConnectError?, onAction: (ConnectAction) -> Unit) {
    AnimatedVisibility(visible = error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        error?.let {
            ConnectErrorBanner(
                error = it,
                onApplyRegion = { onAction(ConnectAction.ApplySuggestedRegion) },
                onDismiss = { onAction(ConnectAction.DismissError) },
                modifier = Modifier.padding(top = Spacing.space4),
            )
        }
    }
}

@Preview(name = "Connect - dark")
@Composable
private fun ConnectDarkPreview() {
    PreviewSurface(darkTheme = true) {
        ConnectContent(
            state = ConnectUiState(
                accessKeyId = "AKIAIOSFODNN7EXAMPLE",
                bucket = "media-prod",
                isConnecting = true,
                steps = listOf(
                    StepRow(ConnectStep.REGION, StepStatus.DONE),
                    StepRow(ConnectStep.SIGNING, StepStatus.RUNNING),
                    StepRow(ConnectStep.LISTING, StepStatus.PENDING),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Connect - light, wrong region")
@Composable
private fun ConnectLightPreview() {
    PreviewSurface(darkTheme = false) {
        ConnectContent(
            state = ConnectUiState(
                accessKeyId = "AKIAIOSFODNN7EXAMPLE",
                bucket = "media-prod",
                error = ConnectError(ConnectErrorKind.WRONG_REGION, "PermanentRedirect", suggestedRegion = "eu-west-1"),
            ),
            onAction = {},
        )
    }
}
