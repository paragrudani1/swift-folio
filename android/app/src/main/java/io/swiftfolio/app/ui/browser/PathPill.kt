package io.swiftfolio.app.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.floatingGlass
import io.swiftfolio.core.Crumb

private val PillIconSize = 16.dp

/**
 * Floating breadcrumb pill. Tap a segment to jump there, tap `…` for the full path sheet,
 * long-press any segment to copy the current `s3://` URI.
 */
@Composable
internal fun PathPill(
    crumbs: List<Crumb>,
    maxWidth: Dp,
    onJump: (String) -> Unit,
    onShowAll: () -> Unit,
    onCopyUri: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ConsoleTheme.colors
    Row(
        modifier = modifier
            .widthIn(max = maxWidth)
            .height(Spacing.pathPillHeight)
            .floatingGlass(CircleShape)
            .horizontalScroll(rememberScrollState(), reverseScrolling = true)
            .padding(horizontal = Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space1),
    ) {
        Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(PillIconSize))
        crumbs.forEachIndexed { index, crumb ->
            if (index > 0) Text("/", style = ConsoleTheme.mono.path, color = colors.textTertiary)
            CrumbText(crumb, onClick = { if (crumb.isEllipsis) onShowAll() else onJump(crumb.prefix) }, onLongClick = onCopyUri)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrumbText(crumb: Crumb, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = ConsoleTheme.colors
    Text(
        text = crumb.label,
        style = ConsoleTheme.mono.path,
        color = if (crumb.isCurrent) colors.textPrimary else colors.textSecondary,
        maxLines = 1,
        modifier = Modifier
            .combinedClickable(
                onClickLabel = stringResource(if (crumb.isEllipsis) R.string.show_full_path else R.string.jump_to_folder),
                onLongClickLabel = stringResource(R.string.copy_s3_uri),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = Spacing.space1, vertical = Spacing.space2),
    )
}
