package io.swiftfolio.app.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.swiftfolio.app.R
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.app.ui.components.PreviewSurface
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.aurora

/** Stateful wrapper for Settings. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onSignedOut: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToConnect -> onSignedOut()
            }
        }
    }
    SettingsContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onCopy = { clipboard.setText(AnnotatedString(it)) },
    )
}

/** Stateless Settings (DIRECTION-C.md 6.11): grouped glass cards in a collapsing layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ConsoleTheme.colors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Box(modifier.fillMaxSize().aurora()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = colors.glass1,
                        titleContentColor = colors.textPrimary,
                        navigationIconContentColor = colors.textSecondary,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.space8
            LazyColumn(
                contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = padding.calculateTopPadding(), bottom = bottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.space4),
            ) {
                item { ConnectionCard(state, onCopy) }
                item { AppearanceCard(state.themeMode, onSelect = { onAction(SettingsAction.SetTheme(it)) }) }
                item { StorageCard(state.rememberCredentials, onRememberChange = { onAction(SettingsAction.SetRemember(it)) }) }
                item { AboutCard(state.appVersion) }
                item { SignOutButton(enabled = state.isConnected, onClick = { onAction(SettingsAction.RequestSignOut) }) }
            }
        }
        state.signOutDialog?.let { dialog -> SignOutDialogView(state.bucket, dialog, onAction) }
    }
}

private val previewState = SettingsUiState(
    bucket = "media-prod",
    region = "eu-west-1",
    maskedAccessKey = "••••7QX2",
    isConnected = true,
    themeMode = ThemeMode.SYSTEM,
    rememberCredentials = true,
    appVersion = "1.0.0",
)

@Preview(name = "Settings - dark")
@Composable
private fun SettingsDarkPreview() {
    PreviewSurface(darkTheme = true) { SettingsContent(previewState, {}, {}, {}) }
}

@Preview(name = "Settings - light")
@Composable
private fun SettingsLightPreview() {
    PreviewSurface(darkTheme = false) { SettingsContent(previewState.copy(themeMode = ThemeMode.LIGHT), {}, {}, {}) }
}
