package io.swiftfolio.app.ui.connect

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.components.consoleFieldColors
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.AwsRegions

/**
 * Read-only region field with an exposed dropdown of the standard AWS regions
 * (code in mono, place name in tertiary text). Only the menu's open state is local UI state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RegionDropdown(region: String, enabled: Boolean, isError: Boolean, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val location = remember(region) { AwsRegions.find(region)?.location.orEmpty() }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = region,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            isError = isError,
            singleLine = true,
            textStyle = ConsoleTheme.mono.input,
            label = { Text(stringResource(R.string.field_region), style = MaterialTheme.typography.labelMedium) },
            supportingText = { Text(location, style = ConsoleTheme.mono.meta) },
            leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.medium,
            colors = consoleFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            containerColor = ConsoleTheme.colors.glass3,
        ) {
            AwsRegions.all.forEach { option ->
                DropdownMenuItem(
                    text = { RegionOption(option.code, option.location) },
                    onClick = {
                        onSelect(option.code)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun RegionOption(code: String, location: String) {
    val colors = ConsoleTheme.colors
    Row {
        Text(code, style = ConsoleTheme.mono.path, color = colors.textPrimary)
        Spacer(Modifier.width(Spacing.space3))
        Text(location, style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
    }
}
