package io.swiftfolio.app.ui.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.components.FileTypeStyles
import io.swiftfolio.app.ui.components.FileTypeTile
import io.swiftfolio.app.ui.components.HighlightedText
import io.swiftfolio.app.ui.components.TileMode
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.ByteFormat
import io.swiftfolio.core.Listing
import io.swiftfolio.core.ObjectEntry
import io.swiftfolio.core.TimeFormat

private const val SELECTED_RIM_ALPHA = 0.4f
private const val STALE_ALPHA = 0.7f

/**
 * One listing row: hairline-free slab with a file-type tile, mono name and mono metadata.
 * Long-press enters selection; in selection mode a tap toggles. Never deletes on swipe.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ObjectRow(
    entry: ObjectEntry,
    selecting: Boolean,
    selected: Boolean,
    query: String,
    dimmed: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = ConsoleTheme.colors
    val haptics = LocalHapticFeedback.current
    val shape = MaterialTheme.shapes.small
    val style = if (entry is ObjectEntry.File) FileTypeStyles.of(entry.kind) else FileTypeStyles.folder
    val tileMode = when {
        !selecting -> TileMode.TYPE
        selected -> TileMode.CHECKED
        else -> TileMode.UNCHECKED
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space2)
            .clip(shape)
            .background(if (selected) colors.glass4 else Color.Transparent)
            .border(Spacing.hairline, if (selected) colors.accentGlowStart.copy(alpha = SELECTED_RIM_ALPHA) else Color.Transparent, shape)
            .combinedClickable(
                onClick = onTap,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
                onLongClickLabel = stringResource(R.string.select_item),
            )
            .semantics {
                if (selecting) {
                    role = Role.Checkbox
                    this.selected = selected
                }
            }
            .heightIn(min = if (entry is ObjectEntry.Folder) Spacing.folderRowMinHeight else Spacing.fileRowMinHeight)
            .alpha(if (dimmed) STALE_ALPHA else 1f)
            .padding(horizontal = Spacing.space3, vertical = Spacing.space2 + Spacing.hairline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
    ) {
        FileTypeTile(style = style, mode = tileMode)
        RowText(entry, query, selected, Modifier.weight(1f))
        RowTrailing(entry, selecting, onMore)
    }
}

@Composable
private fun RowText(entry: ObjectEntry, query: String, selected: Boolean, modifier: Modifier) {
    val colors = ConsoleTheme.colors
    val folderLabel = stringResource(R.string.folder_meta)
    val meta = remember(entry, folderLabel) { metaLine(entry, folderLabel) }
    val ranges = remember(entry.name, query) { Listing.matchRanges(entry.name, query) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.space1 / 2)) {
        HighlightedText(text = entry.name, ranges = ranges, style = ConsoleTheme.mono.row, color = colors.textPrimary)
        Text(
            text = meta,
            style = ConsoleTheme.mono.meta,
            color = if (selected) colors.textSecondary else colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowTrailing(entry: ObjectEntry, selecting: Boolean, onMore: () -> Unit) {
    val colors = ConsoleTheme.colors
    when {
        selecting -> Unit
        entry is ObjectEntry.File -> IconButton(onClick = onMore) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.file_actions, entry.name), tint = colors.textSecondary)
        }
        else -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.textTertiary)
    }
}

/** `2.4 MB · 2026-10-17 09:12` for files, a label for folders. */
private fun metaLine(entry: ObjectEntry, folderLabel: String): String = when (entry) {
    is ObjectEntry.Folder -> folderLabel
    is ObjectEntry.File -> listOfNotNull(ByteFormat.format(entry.sizeBytes), TimeFormat.format(entry.lastModifiedEpochMillis)).joinToString(" · ")
}
