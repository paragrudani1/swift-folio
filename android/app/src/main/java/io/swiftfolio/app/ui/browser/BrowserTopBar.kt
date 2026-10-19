// pluralStringResource was introduced as experimental; opting in is harmless if it is stable.
@file:OptIn(ExperimentalComposeUiApi::class)

package io.swiftfolio.app.ui.browser

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

private const val TITLE_MAX_LINES = 2

/**
 * Collapsing large top bar with the folder name as headline (borrowed from Colophon), or the
 * selection bar while selecting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserTopBar(
    state: BrowserUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onAction: (BrowserAction) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val enterMillis = ConsoleTheme.motion.quickMillis
    val exitMillis = ConsoleTheme.motion.exitMillis
    AnimatedContent(
        targetState = state.mode == CommandMode.Select,
        transitionSpec = { fadeIn(tween(enterMillis)) togetherWith fadeOut(tween(exitMillis)) },
        label = "topBarMode",
    ) { selecting ->
        if (selecting) {
            SelectionTopBar(state, onAction)
        } else {
            LargeTopAppBar(
                title = { Text(state.title, maxLines = TITLE_MAX_LINES, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.open_settings))
                    }
                },
                colors = consoleTopBarColors(),
                scrollBehavior = scrollBehavior,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(state: BrowserUiState, onAction: (BrowserAction) -> Unit) {
    val colors = ConsoleTheme.colors
    val allSelected = state.isAllSelected
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { onAction(BrowserAction.ExitMode) }) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.exit_selection))
            }
        },
        title = {
            Text(
                text = pluralStringResource(R.plurals.selected_count, state.selection.size, state.selection.size).uppercase(),
                style = ConsoleTheme.mono.caps,
                color = colors.onAccent,
                modifier = Modifier.background(colors.accentBrush, CircleShape).padding(horizontal = Spacing.space3, vertical = Spacing.space1),
            )
        },
        actions = {
            TextButton(onClick = { onAction(if (allSelected) BrowserAction.ClearSelection else BrowserAction.SelectAll) }) {
                Text(stringResource(if (allSelected) R.string.clear_selection else R.string.select_all))
            }
        },
        colors = consoleTopBarColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun consoleTopBarColors(): TopAppBarColors {
    val colors = ConsoleTheme.colors
    return TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = colors.glass1,
        titleContentColor = colors.textPrimary,
        actionIconContentColor = colors.textSecondary,
        navigationIconContentColor = colors.textSecondary,
    )
}

