package io.swiftfolio.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing

private val SupportIconSize = 14.dp

/**
 * Console-styled outlined field: transparent container, mono input, `borderStrong` idle
 * border and glow-blue focus border. [errorText] replaces [helperText] and flags the field.
 * [onBlur] fires when the field loses focus after having had it.
 */
@Composable
fun ConsoleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorText: String? = null,
    helperText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onBlur: () -> Unit = {},
) {
    var hadFocus by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().onFocusChanged { focus ->
            if (focus.isFocused) {
                hadFocus = true
            } else if (hadFocus) {
                hadFocus = false
                onBlur()
            }
        },
        enabled = enabled,
        textStyle = ConsoleTheme.mono.input,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText(errorText, helperText),
        isError = errorText != null,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = consoleFieldColors(),
    )
}

private fun supportingText(errorText: String?, helperText: String?): (@Composable () -> Unit)? = when {
    errorText != null -> {
        {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space1), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(SupportIconSize))
                Text(errorText)
            }
        }
    }
    helperText != null -> {
        { Text(helperText, style = ConsoleTheme.mono.meta) }
    }
    else -> null
}

/** Field colours shared by every text input in the app. */
@Composable
fun consoleFieldColors(): TextFieldColors {
    val colors = ConsoleTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        disabledTextColor = colors.textDisabled,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        cursorColor = colors.accentGlowStart,
        focusedBorderColor = colors.accentGlowStart,
        unfocusedBorderColor = colors.borderStrong,
        disabledBorderColor = colors.border,
        errorBorderColor = colors.error,
        focusedLabelColor = colors.accentText,
        unfocusedLabelColor = colors.textSecondary,
        errorLabelColor = colors.error,
        focusedLeadingIconColor = colors.accentText,
        unfocusedLeadingIconColor = colors.textTertiary,
        focusedTrailingIconColor = colors.textSecondary,
        unfocusedTrailingIconColor = colors.textTertiary,
        focusedSupportingTextColor = colors.textTertiary,
        unfocusedSupportingTextColor = colors.textTertiary,
        errorSupportingTextColor = colors.error,
    )
}
