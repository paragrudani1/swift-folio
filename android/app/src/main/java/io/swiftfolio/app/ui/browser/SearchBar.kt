package io.swiftfolio.app.ui.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

/** Search mode of the command bar: close, auto-focused mono input, clear. */
@Composable
internal fun SearchBar(query: String, onAction: (BrowserAction) -> Unit) {
    val colors = ConsoleTheme.colors
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(Modifier.fillMaxSize().padding(horizontal = Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onAction(BrowserAction.ExitMode) }) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close_search))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = { onAction(BrowserAction.QueryChanged(it)) },
                singleLine = true,
                textStyle = ConsoleTheme.mono.input.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accentGlowStart),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onAction(BrowserAction.QueryChanged("")) }) {
                Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear_search), tint = colors.textSecondary)
            }
        }
    }
}
