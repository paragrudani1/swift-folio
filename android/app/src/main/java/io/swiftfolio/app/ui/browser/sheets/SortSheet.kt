package io.swiftfolio.app.ui.browser.sheets

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.browser.sortFieldLabels
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.core.SortDirection
import io.swiftfolio.core.SortField
import io.swiftfolio.core.SortOrder

/** Icon per sort field; labels come from the shared label table. */
private val sortIcons: Map<SortField, ImageVector> = mapOf(
    SortField.NAME to Icons.Rounded.SortByAlpha,
    SortField.MODIFIED to Icons.Rounded.Schedule,
    SortField.SIZE to Icons.Rounded.Straighten,
)

/** Sort options; picking the active field flips its direction (web-app behaviour). */
@Composable
internal fun SortSheet(order: SortOrder, onSelect: (SortField) -> Unit, onDismiss: () -> Unit) {
    val colors = ConsoleTheme.colors
    ConsoleSheet(onDismiss = onDismiss, title = stringResource(R.string.sort_title)) {
        SortField.entries.forEach { field ->
            val active = field == order.field
            ListItem(
                headlineContent = { Text(stringResource(sortFieldLabels.getValue(field))) },
                leadingContent = { Icon(sortIcons.getValue(field), contentDescription = null) },
                trailingContent = if (active) {
                    { DirectionIcon(order.direction) }
                } else {
                    null
                },
                colors = ListItemDefaults.colors(
                    containerColor = if (active) colors.glass4 else Color.Transparent,
                    headlineColor = colors.textPrimary,
                    leadingIconColor = if (active) colors.accentText else colors.textTertiary,
                    trailingIconColor = colors.accentText,
                ),
                modifier = Modifier.clickable(role = Role.RadioButton) { onSelect(field) },
            )
        }
    }
}

@Composable
private fun DirectionIcon(direction: SortDirection) {
    val ascending = direction == SortDirection.ASCENDING
    Icon(
        imageVector = if (ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
        contentDescription = stringResource(if (ascending) R.string.sort_ascending else R.string.sort_descending),
    )
}
