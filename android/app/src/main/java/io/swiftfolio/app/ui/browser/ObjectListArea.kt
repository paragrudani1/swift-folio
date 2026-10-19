package io.swiftfolio.app.ui.browser

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.ui.components.SkeletonRow
import io.swiftfolio.app.ui.components.rememberShimmerBrush
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.core.ObjectEntry

private const val SKELETON_ROWS = 8
private const val CD_SLIDE_DIVISOR = 16
private val RefreshLineHeight = 2.dp

/**
 * Pull-to-refresh list area. Folder changes animate as a short `cd` slide: deeper folders
 * enter from the right, parents from the left (DIRECTION-C.md 4.4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObjectListArea(state: BrowserUiState, onAction: (BrowserAction) -> Unit, onPickFiles: () -> Unit) {
    val pullState = rememberPullToRefreshState()
    val enterMillis = ConsoleTheme.motion.standardMillis
    val exitMillis = ConsoleTheme.motion.exitMillis
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(BrowserAction.Refresh) },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = { PullLine(pullState, Modifier.align(Alignment.TopCenter)) },
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it.prefix },
            transitionSpec = { cdTransition(targetState.navigationDepth >= initialState.navigationDepth, enterMillis, exitMillis) },
            label = "folderTransition",
        ) { folderState ->
            FolderList(folderState, onAction, onPickFiles)
        }
    }
}

/** Quiet pull indicator borrowed from Colophon: a thin accent line that grows with the pull. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullLine(pullState: PullToRefreshState, modifier: Modifier) {
    val brush = ConsoleTheme.colors.accentBrush
    Box(
        modifier.fillMaxWidth().height(RefreshLineHeight).drawBehind {
            val fraction = pullState.distanceFraction.coerceIn(0f, 1f)
            drawRect(brush, size = Size(size.width * fraction, size.height))
        },
    )
}

/** Small-distance slide + fade, never a full-screen push. Durations come from the motion tokens. */
private fun cdTransition(deeper: Boolean, enterMillis: Int, exitMillis: Int): ContentTransform {
    val direction = if (deeper) 1 else -1
    val enter = slideInHorizontally(tween(enterMillis)) { direction * it / CD_SLIDE_DIVISOR } + fadeIn(tween(enterMillis))
    val exit = slideOutHorizontally(tween(exitMillis)) { -direction * it / (CD_SLIDE_DIVISOR * 2) } + fadeOut(tween(exitMillis))
    return enter togetherWith exit
}

@Composable
private fun FolderList(state: BrowserUiState, onAction: (BrowserAction) -> Unit, onPickFiles: () -> Unit) {
    val listState = rememberLazyListState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.listBottomInset
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = bottomInset), modifier = Modifier.fillMaxSize()) {
        item(key = "chips") { ConnectionChips(state, onAction) }
        if (state.mode == CommandMode.Search) item(key = "searchSummary") { SearchSummary(state, onAction) }
        listingItems(state, onAction, onPickFiles)
    }
}

private fun LazyListScope.listingItems(state: BrowserUiState, onAction: (BrowserAction) -> Unit, onPickFiles: () -> Unit) {
    val listing = state.listing
    when {
        listing is ListingState.Loading -> items(SKELETON_ROWS) { SkeletonItem() }
        listing is ListingState.Failed && state.entries.isEmpty() -> item(key = "error") { ListingError(state, listing.error, onAction) }
        state.entries.isEmpty() -> item(key = "empty") { EmptyFolder(state, onAction, onPickFiles) }
        state.visibleEntries.isEmpty() -> item(key = "noMatches") { NoMatches(state.query) }
        else -> {
            if (listing is ListingState.Failed) item(key = "stale") { StaleBanner(listing.error, onAction) }
            entryItems(state, onAction)
        }
    }
}

private fun LazyListScope.entryItems(state: BrowserUiState, onAction: (BrowserAction) -> Unit) {
    val folders = state.visibleEntries.filterIsInstance<ObjectEntry.Folder>()
    val files = state.visibleEntries.filterIsInstance<ObjectEntry.File>()
    if (folders.isNotEmpty()) {
        item(key = "header-folders") { SectionHeader(SectionKind.FOLDERS, folders.size, state.sortOrder, onAction) }
        items(folders, key = { it.id }) { entry -> EntryRow(state, entry, onAction) }
    }
    if (files.isNotEmpty()) {
        item(key = "header-files") { SectionHeader(SectionKind.FILES, files.size, state.sortOrder, onAction) }
        items(files, key = { it.id }) { entry -> EntryRow(state, entry, onAction) }
    }
}

@Composable
private fun EntryRow(state: BrowserUiState, entry: ObjectEntry, onAction: (BrowserAction) -> Unit) {
    ObjectRow(
        entry = entry,
        selecting = state.mode == CommandMode.Select,
        selected = entry.id in state.selection,
        query = if (state.mode == CommandMode.Search) state.query else "",
        dimmed = state.listing is ListingState.Failed,
        onTap = { onAction(BrowserAction.RowTapped(entry)) },
        onLongPress = { onAction(BrowserAction.RowLongPressed(entry)) },
        onMore = { onAction(BrowserAction.ShowSheet(BrowserSheet.FileDetails(entry as ObjectEntry.File))) },
    )
}

@Composable
private fun SkeletonItem() {
    SkeletonRow(brush = rememberShimmerBrush())
}
