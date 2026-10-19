// pluralStringResource was introduced as experimental; opting in is harmless if it is stable.
@file:OptIn(ExperimentalComposeUiApi::class)

package io.swiftfolio.app.ui.browser

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.swiftfolio.app.R
import io.swiftfolio.app.bucket.StorageState
import io.swiftfolio.app.ui.ErrorTexts
import io.swiftfolio.app.ui.components.GradientButton
import io.swiftfolio.app.ui.components.StateMessage
import io.swiftfolio.app.ui.components.StatusChip
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.ByteFormat
import io.swiftfolio.core.ConnectError
import io.swiftfolio.core.ConnectErrorKind
import io.swiftfolio.core.EntryTypeFilter
import io.swiftfolio.core.SortDirection
import io.swiftfolio.core.SortField
import io.swiftfolio.core.SortOrder

private const val WARNING_TINT_ALPHA = 0.12f

/** List sections; each knows its label. */
internal enum class SectionKind(@StringRes val label: Int) {
    FOLDERS(R.string.section_folders),
    FILES(R.string.section_files),
}

/** Sort field labels, shared by the FILES header and the sort sheet. */
internal val sortFieldLabels: Map<SortField, Int> = mapOf(
    SortField.NAME to R.string.sort_name,
    SortField.MODIFIED to R.string.sort_modified,
    SortField.SIZE to R.string.sort_size,
)

/** Icon for a full-area listing error, by kind (lock for permissions, cloud for network). */
private val errorIcons: Map<ConnectErrorKind, ImageVector> = mapOf(
    ConnectErrorKind.ACCESS_DENIED to Icons.Outlined.Lock,
    ConnectErrorKind.NETWORK to Icons.Rounded.CloudOff,
    ConnectErrorKind.TIMEOUT to Icons.Rounded.CloudOff,
)

/** Status chips under the headline: connection, region and the storage total. */
@Composable
internal fun ConnectionChips(state: BrowserUiState, onAction: (BrowserAction) -> Unit) {
    val colors = ConsoleTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(
            text = stringResource(if (state.isConnected) R.string.status_connected else R.string.status_connecting),
            dotColor = if (state.isConnected) colors.success else colors.warning,
        )
        if (state.region.isNotEmpty()) StatusChip(text = state.region)
        StatusChip(
            text = storageLabel(state.storage),
            modifier = Modifier.minimumInteractiveComponentSize().clickable(onClickLabel = stringResource(R.string.refresh_storage)) {
                onAction(BrowserAction.RefreshStorage)
            },
        )
    }
}

@Composable
private fun storageLabel(storage: StorageState): String = when (storage) {
    StorageState.Computing -> stringResource(R.string.storage_computing)
    is StorageState.Ready -> stringResource(
        R.string.storage_total,
        ByteFormat.format(storage.usage.totalBytes),
        pluralStringResource(R.plurals.object_count, storage.usage.objectCount.toInt(), storage.usage.objectCount),
    )
    is StorageState.Unavailable -> stringResource(R.string.storage_unavailable)
}

/** `FOLDERS · 4` / `FILES · 27`; the files header carries the sort shortcut. */
@Composable
internal fun SectionHeader(kind: SectionKind, count: Int, sortOrder: SortOrder, onAction: (BrowserAction) -> Unit) {
    val colors = ConsoleTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.gutter, end = Spacing.space2, top = Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.section_header, stringResource(kind.label), count).uppercase(),
            style = ConsoleTheme.mono.caps,
            color = colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        if (kind == SectionKind.FILES) {
            TextButton(onClick = { onAction(BrowserAction.ShowSheet(BrowserSheet.Sort)) }) {
                val arrow = if (sortOrder.direction == SortDirection.ASCENDING) "↑" else "↓"
                Text("${stringResource(sortFieldLabels.getValue(sortOrder.field)).lowercase()} $arrow", style = ConsoleTheme.mono.meta, color = colors.accentText)
            }
        }
    }
}

/** Result count and type filter chips while searching. */
@Composable
internal fun SearchSummary(state: BrowserUiState, onAction: (BrowserAction) -> Unit) {
    val colors = ConsoleTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = Spacing.space2)) {
        Text(
            text = pluralStringResource(R.plurals.search_matches, state.visibleEntries.size, state.visibleEntries.size, "/" + state.prefix).uppercase(),
            style = ConsoleTheme.mono.caps,
            color = colors.textTertiary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            TypeFilterChip(state.typeFilter, EntryTypeFilter.FILES_ONLY, R.string.filter_files_only, onAction)
            TypeFilterChip(state.typeFilter, EntryTypeFilter.FOLDERS_ONLY, R.string.filter_folders_only, onAction)
        }
    }
}

@Composable
private fun TypeFilterChip(current: EntryTypeFilter, value: EntryTypeFilter, @StringRes label: Int, onAction: (BrowserAction) -> Unit) {
    val colors = ConsoleTheme.colors
    val selected = current == value
    FilterChip(
        selected = selected,
        onClick = { onAction(BrowserAction.TypeFilterChanged(if (selected) EntryTypeFilter.ALL else value)) },
        label = { Text(stringResource(label)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.glass4, selectedLabelColor = colors.textPrimary),
    )
}

/** Empty folder (or empty bucket) with duplicated New folder / Upload actions for discoverability. */
@Composable
internal fun EmptyFolder(state: BrowserUiState, onAction: (BrowserAction) -> Unit, onPickFiles: () -> Unit) {
    StateMessage(
        icon = Icons.Outlined.FolderOpen,
        title = stringResource(if (state.prefix.isEmpty()) R.string.empty_bucket else R.string.empty_folder),
        detail = state.currentUri,
    ) {
        OutlinedButton(onClick = { onAction(BrowserAction.ShowSheet(BrowserSheet.NewFolder())) }) {
            Text(stringResource(R.string.new_folder))
        }
        GradientButton(text = stringResource(R.string.upload_files), onClick = onPickFiles, minHeight = Spacing.touchTarget)
    }
}

/** No search results in this folder. */
@Composable
internal fun NoMatches(query: String) {
    StateMessage(
        icon = Icons.Rounded.SearchOff,
        title = stringResource(R.string.no_matches, query),
        detail = stringResource(R.string.no_matches_hint),
    )
}

/** Full-area error when nothing is cached for this folder. */
@Composable
internal fun ListingError(state: BrowserUiState, error: ConnectError, onAction: (BrowserAction) -> Unit) {
    StateMessage(
        icon = errorIcons[error.kind] ?: Icons.Rounded.ErrorOutline,
        iconTint = ConsoleTheme.colors.error,
        title = ErrorTexts.title(error),
        detail = "${error.code} · ${state.currentUri}",
    ) {
        OutlinedButton(onClick = { onAction(BrowserAction.Refresh) }) { Text(stringResource(R.string.retry)) }
        if (state.prefix.isNotEmpty()) {
            OutlinedButton(onClick = { onAction(BrowserAction.NavigateUp) }) { Text(stringResource(R.string.go_up)) }
        }
    }
}

/** Non-blocking banner above stale rows when a refresh failed. */
@Composable
internal fun StaleBanner(error: ConnectError, onAction: (BrowserAction) -> Unit) {
    val colors = ConsoleTheme.colors
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.space2)
            .background(colors.glass2, shape)
            .background(colors.warning.copy(alpha = WARNING_TINT_ALPHA), shape)
            .border(Spacing.hairline, colors.border, shape)
            .padding(start = Spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = Spacing.space3)) {
            Text(ErrorTexts.title(error), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(error.code, style = ConsoleTheme.mono.meta, color = colors.textSecondary)
        }
        Spacer(Modifier.width(Spacing.space2))
        TextButton(onClick = { onAction(BrowserAction.Refresh) }) { Text(stringResource(R.string.retry_now)) }
    }
}
