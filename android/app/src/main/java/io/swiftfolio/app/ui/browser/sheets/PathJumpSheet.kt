package io.swiftfolio.app.ui.browser.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.Crumb
import io.swiftfolio.core.S3Path

private val IndentPerLevel = 12.dp

/** Every ancestor of the current folder, indented by depth, plus copy actions. */
@Composable
internal fun PathJumpSheet(crumbs: List<Crumb>, bucket: String, onJump: (String) -> Unit, onCopy: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = ConsoleTheme.colors
    val currentPrefix = crumbs.lastOrNull()?.prefix.orEmpty()
    ConsoleSheet(onDismiss = onDismiss, title = stringResource(R.string.path_jump_title)) {
        crumbs.forEachIndexed { depth, crumb ->
            Text(
                text = crumb.label + if (crumb.prefix.isEmpty()) "" else "/",
                style = ConsoleTheme.mono.path,
                color = if (crumb.isCurrent) colors.textPrimary else colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = stringResource(R.string.jump_to_folder)) { onJump(crumb.prefix) }
                    .heightIn(min = Spacing.touchTarget)
                    .padding(start = Spacing.space6 + IndentPerLevel * depth, top = Spacing.space3, bottom = Spacing.space3),
            )
        }
        HorizontalDivider(color = colors.border)
        CopyRow(stringResource(R.string.copy_s3_uri), Icons.Rounded.Link) { onCopy(S3Path.s3Uri(bucket, currentPrefix)) }
        CopyRow(stringResource(R.string.copy_prefix), Icons.Rounded.ContentCopy) { onCopy(currentPrefix) }
    }
}

@Composable
private fun CopyRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val colors = ConsoleTheme.colors
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = colors.textPrimary, leadingIconColor = colors.textSecondary),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
