package io.swiftfolio.app.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.swiftfolio.app.ui.theme.Spacing

/** Floating path pill (hidden while searching or scrolling down) above the command bar. */
@Composable
internal fun BottomChrome(
    state: BrowserUiState,
    pillVisible: Boolean,
    onAction: (BrowserAction) -> Unit,
    onPickFiles: () -> Unit,
    onCopy: (String) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
        val pillMaxWidth = maxWidth - Spacing.space8
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                visible = pillVisible && state.mode != CommandMode.Search,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                PathPill(
                    crumbs = state.crumbs,
                    maxWidth = pillMaxWidth,
                    onJump = { onAction(BrowserAction.JumpTo(it)) },
                    onShowAll = { onAction(BrowserAction.ShowSheet(BrowserSheet.PathJump)) },
                    onCopyUri = { onCopy(state.currentUri) },
                )
            }
            CommandBar(
                state = state,
                onAction = onAction,
                onPickFiles = onPickFiles,
                onCopy = onCopy,
                modifier = Modifier.padding(Spacing.commandBarMargin),
            )
        }
    }
}
