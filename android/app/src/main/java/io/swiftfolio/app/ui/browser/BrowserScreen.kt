package io.swiftfolio.app.ui.browser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.swiftfolio.app.data.StorageUsage
import io.swiftfolio.app.ui.components.ActivityLine
import io.swiftfolio.app.ui.components.ConsoleSnackbarHost
import io.swiftfolio.app.ui.components.PreviewData
import io.swiftfolio.app.ui.components.PreviewSurface
import io.swiftfolio.app.ui.theme.aurora
import io.swiftfolio.core.ObjectEntry

private const val CHROME_HIDE_THRESHOLD = 8f

/** Stateful wrapper: pickers, permissions, clipboard, snackbars, back handling, effects. */
@Composable
fun BrowserScreen(viewModel: BrowserViewModel, onOpenSettings: () -> Unit, onSignedOut: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.onAction(BrowserAction.FilesPicked(uris.map { it.toString() }))
    }
    // Transfer notifications are optional: the picker opens whether or not permission is granted.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        picker.launch(arrayOf(ALL_FILES))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BrowserEffect.NavigateToConnect -> onSignedOut()
            }
        }
    }
    BrowserMessageEffect(state.message, snackbarHostState) { viewModel.onAction(BrowserAction.MessageShown) }
    BackHandler(enabled = state.canGoBack) { viewModel.onAction(BrowserAction.Back) }
    BrowserContent(
        state = state,
        onAction = viewModel::onAction,
        onPickFiles = {
            if (needsNotificationPermission(context)) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                picker.launch(arrayOf(ALL_FILES))
            }
        },
        onOpenSettings = onOpenSettings,
        onCopy = { text ->
            clipboard.setText(AnnotatedString(text))
            viewModel.onAction(BrowserAction.Copied(text))
        },
        snackbarHostState = snackbarHostState,
    )
}

private const val ALL_FILES = "*/*"

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/**
 * Stateless browser (DIRECTION-C.md 6.2-6.10): collapsing top bar, listing, floating path
 * pill and command bar, plus the open sheet or dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserContent(
    state: BrowserUiState,
    onAction: (BrowserAction) -> Unit,
    onPickFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopy: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var chromeVisible by remember { mutableStateOf(true) }
    val chromeConnection = remember { chromeVisibilityConnection { visible -> chromeVisible = visible } }
    Box(modifier.fillMaxSize().aurora()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).nestedScroll(chromeConnection),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = { BrowserTopBar(state, scrollBehavior, onAction, onOpenSettings) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                ObjectListArea(state = state, onAction = onAction, onPickFiles = onPickFiles)
                ActivityLine(visible = state.isBusy, modifier = Modifier.align(Alignment.TopCenter))
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    ConsoleSnackbarHost(snackbarHostState)
                    BottomChrome(state, chromeVisible, onAction, onPickFiles, onCopy)
                }
            }
        }
        BrowserOverlays(state = state, onAction = onAction, onPickFiles = onPickFiles, onCopy = onCopy)
    }
}

/** Hides the path pill while scrolling down and shows it again on scroll up. */
private fun chromeVisibilityConnection(onVisibilityChange: (Boolean) -> Unit) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < -CHROME_HIDE_THRESHOLD) onVisibilityChange(false)
        if (available.y > CHROME_HIDE_THRESHOLD) onVisibilityChange(true)
        return Offset.Zero
    }
}

private val previewState = BrowserUiState(
    bucket = "media-prod",
    region = "eu-west-1",
    isConnected = true,
    prefix = "photos/",
    navigationDepth = 1,
    listing = ListingState.Loaded(PreviewData.entries),
    storage = StorageState.Ready(StorageUsage(totalBytes = 13_314_398_618, objectCount = 1_284)),
)

@Preview(name = "Browser - dark")
@Composable
private fun BrowserDarkPreview() {
    PreviewSurface(darkTheme = true) {
        BrowserContent(previewState, {}, {}, {}, {}, remember { SnackbarHostState() })
    }
}

@Preview(name = "Browser - light, selecting")
@Composable
private fun BrowserLightPreview() {
    val selecting = previewState.copy(
        mode = CommandMode.Select,
        selection = setOf(PreviewData.entries.filterIsInstance<ObjectEntry.File>().first().key),
    )
    PreviewSurface(darkTheme = false) {
        BrowserContent(selecting, {}, {}, {}, {}, remember { SnackbarHostState() })
    }
}

@Preview(name = "Browser - dark, loading")
@Composable
private fun BrowserLoadingPreview() {
    PreviewSurface(darkTheme = true) {
        BrowserContent(previewState.copy(listing = ListingState.Loading, storage = StorageState.Computing), {}, {}, {}, {}, remember { SnackbarHostState() })
    }
}
