package io.swiftfolio.app.ui.browser

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Runtime permissions a transfer asks for before it starts. The transfer runs whether or not they are granted. */
internal object TransferPermissions {
    /** Notifications (API 33+), so progress and completion are visible outside the app. */
    val forUpload: List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()

    /** Notifications, plus storage on API 26-28 so files land in the public Downloads folder. */
    val forDownload: List<String> =
        forUpload + if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE) else emptyList()
}

/** Asks for missing permissions once, then runs the action that needed them, whatever the answer. */
internal class PermissionGate(
    private val context: Context,
    private val pending: MutableState<(() -> Unit)?>,
    private val request: (Array<String>) -> Unit,
) {
    /** Runs [action] now when [permissions] are granted, otherwise after the system dialog closes. */
    fun runAfterAsking(permissions: List<String>, action: () -> Unit) {
        val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            action()
        } else {
            pending.value = action
            request(missing.toTypedArray())
        }
    }
}

/** A [PermissionGate] bound to this composition's activity result registry. */
@Composable
internal fun rememberPermissionGate(): PermissionGate {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pending.value?.invoke()
        pending.value = null
    }
    return remember(context, launcher) { PermissionGate(context, pending) { permissions -> launcher.launch(permissions) } }
}
