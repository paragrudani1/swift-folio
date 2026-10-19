package io.swiftfolio.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.swiftfolio.app.R
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.app.ui.components.StatusChip
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.GlassTier
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.glass
import io.swiftfolio.core.S3Path

/** Label per theme mode, in segment order. */
private val themeLabels: Map<ThemeMode, Int> = mapOf(
    ThemeMode.SYSTEM to R.string.theme_system,
    ThemeMode.LIGHT to R.string.theme_light,
    ThemeMode.DARK to R.string.theme_dark,
)

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = ConsoleTheme.colors
    Column {
        Text(title.uppercase(), style = ConsoleTheme.mono.caps, color = colors.textTertiary, modifier = Modifier.padding(start = Spacing.space1, bottom = Spacing.space2))
        Column(Modifier.fillMaxWidth().glass(MaterialTheme.shapes.medium, GlassTier.Two), content = content)
    }
}

@Composable
private fun transparentItemColors() = ListItemDefaults.colors(
    containerColor = Color.Transparent,
    headlineColor = ConsoleTheme.colors.textPrimary,
    supportingColor = ConsoleTheme.colors.textTertiary,
    leadingIconColor = ConsoleTheme.colors.textSecondary,
)

/** Bucket, region, masked key and copy action. */
@Composable
internal fun ConnectionCard(state: SettingsUiState, onCopy: (String) -> Unit) {
    val colors = ConsoleTheme.colors
    SettingsCard(stringResource(R.string.settings_connection)) {
        ListItem(
            headlineContent = { Text(state.bucket.ifEmpty { stringResource(R.string.not_connected) }, style = ConsoleTheme.mono.row) },
            supportingContent = { Text(stringResource(R.string.settings_connection_detail, state.region, state.maskedAccessKey), style = ConsoleTheme.mono.meta) },
            trailingContent = if (state.isConnected) {
                { StatusChip(stringResource(R.string.status_connected), dotColor = colors.success) }
            } else {
                null
            },
            colors = transparentItemColors(),
        )
        HorizontalDivider(color = colors.border)
        ListItem(
            headlineContent = { Text(stringResource(R.string.copy_bucket_uri)) },
            leadingContent = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
            colors = transparentItemColors(),
            modifier = Modifier.clickable(enabled = state.isConnected) { onCopy(S3Path.s3Uri(state.bucket, "")) },
        )
    }
}

/** System / Light / Dark segmented control. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceCard(themeMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = ConsoleTheme.colors
    SettingsCard(stringResource(R.string.settings_appearance)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(Spacing.space4)) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == themeMode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.glass4,
                        activeContentColor = colors.textPrimary,
                        activeBorderColor = colors.accentGlowStart,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = colors.textSecondary,
                        inactiveBorderColor = colors.borderStrong,
                    ),
                ) {
                    Text(stringResource(themeLabels.getValue(mode)))
                }
            }
        }
    }
}

/** Remember-credentials switch; turning it off wipes the saved keys. */
@Composable
internal fun StorageCard(remember: Boolean, onRememberChange: (Boolean) -> Unit) {
    val colors = ConsoleTheme.colors
    SettingsCard(stringResource(R.string.settings_storage)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.remember_title)) },
            supportingContent = { Text(stringResource(R.string.settings_remember_detail)) },
            trailingContent = {
                Switch(checked = remember, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = colors.accentStart))
            },
            colors = transparentItemColors(),
            modifier = Modifier.heightIn(min = Spacing.folderRowMinHeight).toggleable(value = remember, role = Role.Switch, onValueChange = onRememberChange),
        )
        HorizontalDivider(color = colors.border)
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_download_folder)) },
            supportingContent = { Text(stringResource(R.string.settings_download_folder_value), style = ConsoleTheme.mono.meta) },
            colors = transparentItemColors(),
        )
    }
}

/** Version and font licences. */
@Composable
internal fun AboutCard(version: String) {
    SettingsCard(stringResource(R.string.settings_about)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_version)) },
            trailingContent = { Text(version, style = ConsoleTheme.mono.meta) },
            colors = transparentItemColors(),
        )
        HorizontalDivider(color = ConsoleTheme.colors.border)
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_licenses)) },
            supportingContent = { Text(stringResource(R.string.settings_licenses_detail)) },
            colors = transparentItemColors(),
        )
    }
}

/** Full-width destructive outline button. */
@Composable
internal fun SignOutButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = ConsoleTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(Spacing.hairline, colors.error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
    ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.sign_out))
    }
}

/** "Sign out of <bucket>?" with the forget-credentials checkbox. */
@Composable
internal fun SignOutDialogView(bucket: String, dialog: SignOutDialog, onAction: (SettingsAction) -> Unit) {
    val colors = ConsoleTheme.colors
    AlertDialog(
        onDismissRequest = { onAction(SettingsAction.DismissSignOut) },
        title = { Text(stringResource(R.string.sign_out_title, bucket)) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.touchTarget)
                    .toggleable(value = dialog.forgetCredentials, role = Role.Checkbox, onValueChange = { onAction(SettingsAction.ForgetOnSignOutChanged(it)) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = dialog.forgetCredentials, onCheckedChange = null)
                Text(stringResource(R.string.sign_out_forget), modifier = Modifier.padding(start = Spacing.space2))
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(SettingsAction.ConfirmSignOut) }) { Text(stringResource(R.string.sign_out), color = colors.error) }
        },
        dismissButton = { TextButton(onClick = { onAction(SettingsAction.DismissSignOut) }) { Text(stringResource(R.string.cancel)) } },
        containerColor = colors.glass3,
        shape = MaterialTheme.shapes.large,
    )
}
