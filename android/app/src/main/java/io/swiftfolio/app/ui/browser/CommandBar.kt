package io.swiftfolio.app.ui.browser

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.swiftfolio.app.R
import io.swiftfolio.app.ui.theme.ConsoleTheme
import io.swiftfolio.app.ui.theme.Spacing
import io.swiftfolio.app.ui.theme.floatingGlass
import io.swiftfolio.core.TransferProgress

private const val MODE_SCALE_IN = 0.96f
private const val DISABLED_ALPHA = 0.38f
private const val ERROR_SLAB_ALPHA = 0.12f
private val ProgressRingSize = 24.dp
private val SearchPillHeight = 44.dp
private val BarActionWidth = 64.dp

/** Floating command bar; the content swaps per [CommandMode]. */
@Composable
internal fun CommandBar(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    onPickFiles: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = ConsoleTheme.motion
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.commandBarHeight)
            .floatingGlass(MaterialTheme.shapes.large)
            .animateContentSize(tween(motion.quickMillis)),
    ) {
        AnimatedContent(
            targetState = state.mode,
            transitionSpec = { (fadeIn(tween(motion.quickMillis)) + scaleIn(initialScale = MODE_SCALE_IN)) togetherWith fadeOut(tween(motion.exitMillis)) },
            label = "commandBarMode",
        ) { mode ->
            when (mode) {
                CommandMode.Browse -> BrowseBar(state, onAction, onPickFiles)
                CommandMode.Search -> SearchBar(state.query, onAction)
                CommandMode.Select -> SelectBar(state, onAction, onCopy)
            }
        }
    }
}

@Composable
private fun BrowseBar(state: BrowserUiState, onAction: (BrowserAction) -> Unit, onPickFiles: () -> Unit) {
    val colors = ConsoleTheme.colors
    Row(Modifier.fillMaxSize().padding(horizontal = Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onAction(BrowserAction.NavigateUp) }, enabled = state.prefix.isNotEmpty()) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.up_to_parent))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = SearchPillHeight)
                .background(colors.glass2, CircleShape)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.search_folder)) { onAction(BrowserAction.StartSearch) }
                .padding(horizontal = Spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary, maxLines = 1)
        }
        IconButton(onClick = { onAction(BrowserAction.ShowSheet(BrowserSheet.NewFolder())) }) {
            Icon(Icons.Rounded.CreateNewFolder, contentDescription = stringResource(R.string.new_folder), tint = colors.textSecondary)
        }
        UploadButton(
            progress = state.uploadProgress,
            onClick = { if (state.uploadProgress != null) onAction(BrowserAction.ShowSheet(BrowserSheet.Uploads)) else onPickFiles() },
        )
    }
}

/** Gradient upload button that morphs into a progress chip while uploads run. */
@Composable
private fun UploadButton(progress: TransferProgress?, onClick: () -> Unit) {
    val colors = ConsoleTheme.colors
    val label = if (progress == null) stringResource(R.string.upload_files) else stringResource(R.string.show_uploads, progress.percent)
    Row(
        modifier = Modifier
            .heightIn(min = Spacing.touchTarget)
            .widthIn(min = Spacing.touchTarget)
            .background(colors.accentBrush, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = if (progress == null) 0.dp else Spacing.space3)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2, Alignment.CenterHorizontally),
    ) {
        if (progress == null) {
            Icon(Icons.Rounded.Upload, contentDescription = label, tint = colors.onAccent)
        } else {
            CircularProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.size(ProgressRingSize),
                color = colors.onAccent,
                trackColor = colors.onAccent.copy(alpha = DISABLED_ALPHA),
                strokeWidth = 3.dp,
            )
            Text(stringResource(R.string.percent, progress.percent), style = ConsoleTheme.mono.meta, color = colors.onAccent)
        }
    }
}

@Composable
private fun SelectBar(state: BrowserUiState, onAction: (BrowserAction) -> Unit, onCopy: (String) -> Unit) {
    val hasSelection = state.selection.isNotEmpty()
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BarAction(Icons.Rounded.Download, stringResource(R.string.download), enabled = hasSelection && !state.selectionHasFolders) {
            onAction(BrowserAction.DownloadSelection)
        }
        BarAction(Icons.Rounded.ContentCopy, stringResource(R.string.copy_keys), enabled = hasSelection) {
            onCopy(state.selection.sorted().joinToString("\n"))
        }
        BarAction(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), enabled = hasSelection, destructive = true) {
            onAction(BrowserAction.DeleteSelection)
        }
    }
}

/** Icon over a label, 64dp wide; destructive actions sit on an error-tinted slab. */
@Composable
private fun BarAction(icon: ImageVector, label: String, enabled: Boolean, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = ConsoleTheme.colors
    val tint = if (destructive) colors.error else colors.textPrimary
    Column(
        modifier = Modifier
            .width(BarActionWidth)
            .heightIn(min = Spacing.touchTarget)
            .background(if (destructive) colors.error.copy(alpha = ERROR_SLAB_ALPHA) else Color.Transparent, MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(vertical = Spacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}
