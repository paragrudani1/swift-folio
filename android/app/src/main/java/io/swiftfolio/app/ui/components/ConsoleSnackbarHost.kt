package io.swiftfolio.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

/** Snackbars styled as glass3 with the luminous rim. */
@Composable
fun ConsoleSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = ConsoleTheme.colors
    SnackbarHost(hostState = state, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = colors.glass3,
            contentColor = colors.textPrimary,
            actionColor = colors.accentText,
            dismissActionContentColor = colors.textSecondary,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.border(Spacing.hairline, colors.rimBrush, MaterialTheme.shapes.large),
        )
    }
}
