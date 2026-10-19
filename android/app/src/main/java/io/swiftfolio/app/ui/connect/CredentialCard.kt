package io.swiftfolio.app.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.ErrorTexts
import io.swiftfolio.app.ui.components.ConsoleTextField
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.GlassTier
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.glass
import io.swiftfolio.core.CoreConstants

private const val MASK_CHAR = '•'

/** Glass card holding the four credential inputs and the Remember switch. */
@Composable
internal fun CredentialCard(state: ConnectUiState, onAction: (ConnectAction) -> Unit) {
    val enabled = !state.isConnecting
    Column(
        modifier = Modifier.fillMaxWidth().glass(MaterialTheme.shapes.medium, GlassTier.Two).padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space2),
    ) {
        AccessKeyField(state, enabled, onAction)
        SecretField(state, enabled, onAction)
        BucketField(state, enabled, onAction)
        RegionDropdown(
            region = state.region,
            enabled = enabled,
            isError = state.errorField == ConnectField.REGION,
            onSelect = { onAction(ConnectAction.RegionSelected(it)) },
        )
        HorizontalDivider(color = ConsoleTheme.colors.border)
        RememberRow(remember = state.remember, enabled = enabled, onChange = { onAction(ConnectAction.RememberChanged(it)) })
    }
}

@Composable
private fun AccessKeyField(state: ConnectUiState, enabled: Boolean, onAction: (ConnectAction) -> Unit) {
    val colors = ConsoleTheme.colors
    ConsoleTextField(
        value = state.accessKeyId,
        onValueChange = { onAction(ConnectAction.AccessKeyChanged(it)) },
        label = stringResource(R.string.field_access_key),
        enabled = enabled,
        errorText = state.accessKeyError?.let { ErrorTexts.field(it, ErrorTexts.LengthLimits.ACCESS_KEY) }
            ?: serverErrorText(state, ConnectField.ACCESS_KEY),
        helperText = stringResource(
            R.string.field_access_key_helper,
            CoreConstants.ACCESS_KEY_MIN_LENGTH,
            state.accessKeyId.length,
            CoreConstants.ACCESS_KEY_MAX_LENGTH,
        ),
        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
        trailingIcon = if (state.accessKeyLooksValid) {
            { Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.field_valid), tint = colors.success) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Next),
        onBlur = { onAction(ConnectAction.FieldBlurred(ConnectField.ACCESS_KEY)) },
    )
}

@Composable
private fun SecretField(state: ConnectUiState, enabled: Boolean, onAction: (ConnectAction) -> Unit) {
    ConsoleTextField(
        value = state.secretAccessKey,
        onValueChange = { onAction(ConnectAction.SecretChanged(it)) },
        label = stringResource(R.string.field_secret_key),
        enabled = enabled,
        errorText = state.secretError?.let { ErrorTexts.field(it, ErrorTexts.LengthLimits.SECRET) }
            ?: serverErrorText(state, ConnectField.SECRET),
        helperText = stringResource(R.string.field_secret_key_helper, CoreConstants.SECRET_KEY_MIN_LENGTH),
        trailingIcon = {
            IconToggleButton(checked = state.secretVisible, onCheckedChange = { onAction(ConnectAction.SecretVisibilityToggled) }) {
                Icon(
                    imageVector = if (state.secretVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(if (state.secretVisible) R.string.hide_secret else R.string.show_secret),
                )
            }
        },
        visualTransformation = if (state.secretVisible) VisualTransformation.None else PasswordVisualTransformation(MASK_CHAR),
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        onBlur = { onAction(ConnectAction.FieldBlurred(ConnectField.SECRET)) },
    )
}

@Composable
private fun BucketField(state: ConnectUiState, enabled: Boolean, onAction: (ConnectAction) -> Unit) {
    ConsoleTextField(
        value = state.bucket,
        onValueChange = { onAction(ConnectAction.BucketChanged(it)) },
        label = stringResource(R.string.field_bucket),
        enabled = enabled,
        errorText = state.bucketError?.let { ErrorTexts.field(it, ErrorTexts.LengthLimits.BUCKET) }
            ?: serverErrorText(state, ConnectField.BUCKET),
        leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAction(ConnectAction.Submit) }),
        onBlur = { onAction(ConnectAction.FieldBlurred(ConnectField.BUCKET)) },
    )
}

/** The server error's title when it concerns [field], so the field is flagged too. */
@Composable
private fun serverErrorText(state: ConnectUiState, field: ConnectField): String? =
    state.error?.takeIf { state.errorField == field }?.let { ErrorTexts.title(it) }

@Composable
private fun RememberRow(remember: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = ConsoleTheme.colors
    ListItem(
        headlineContent = { Text(stringResource(R.string.remember_title)) },
        supportingContent = { Text(stringResource(R.string.remember_subtitle)) },
        trailingContent = {
            Switch(
                checked = remember,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accentStart, checkedThumbColor = colors.onAccent),
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = colors.textPrimary,
            supportingColor = colors.textTertiary,
        ),
        modifier = Modifier
            .heightIn(min = Spacing.folderRowMinHeight)
            .toggleable(value = remember, enabled = enabled, role = Role.Switch, onValueChange = onChange),
    )
}
