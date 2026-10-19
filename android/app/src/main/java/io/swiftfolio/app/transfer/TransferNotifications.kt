package io.swiftfolio.app.transfer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import io.swiftfolio.app.MainActivity
import io.swiftfolio.app.R
import io.swiftfolio.core.TransferProgress
import java.util.UUID

/** Builds the "Transfers" channel notifications: foreground progress and completion. */
class TransferNotifications(private val context: Context) {

    /** Creates the channel once; safe to call repeatedly (minSdk 26 always has channels). */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_transfers),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = context.getString(R.string.notification_channel_transfers_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Foreground notification for a running batch, with a Cancel action. */
    fun foregroundInfo(workId: UUID, manifest: TransferManifest, progress: TransferProgress): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transfer)
            .setContentTitle(progressTitle(manifest, progress))
            .setContentText(context.getString(R.string.notification_progress_text, progress.percent))
            .setProgress(PERCENT, progress.percent, progress.bytesTotal == 0L && progress.filesDone == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.action_cancel), WorkManager.getInstance(context).createCancelPendingIntent(workId))
            .build()
        val id = foregroundId(manifest.batchId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    /** Posts the completion notification, if the user allows notifications. */
    @SuppressLint("MissingPermission") // Guarded by canPostNotifications() right above the notify call.
    fun showFinished(manifest: TransferManifest, result: BatchResult) {
        if (!canPostNotifications()) return
        val done = result.snapshot.statuses.count { it == TransferItemStatus.DONE }
        val failed = result.snapshot.statuses.count { it == TransferItemStatus.FAILED }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transfer)
            .setContentTitle(finishedTitle(manifest.kind, done))
            .setContentText(context.resources.getQuantityString(R.plurals.notification_failed_count, failed, failed).takeIf { failed > 0 })
            .setAutoCancel(true)
            .setContentIntent(result.savedUris.singleOrNull()?.let(::viewFileIntent) ?: openAppIntent())
            .build()
        NotificationManagerCompat.from(context).notify(finishedId(manifest.batchId), notification)
    }

    private fun canPostNotifications(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun progressTitle(manifest: TransferManifest, progress: TransferProgress): String {
        val current = (progress.filesDone + 1).coerceAtMost(progress.filesTotal.coerceAtLeast(1))
        val res = if (manifest.kind == TransferKind.UPLOAD) R.string.notification_uploading else R.string.notification_downloading
        return context.getString(res, current, manifest.items.size)
    }

    private fun finishedTitle(kind: TransferKind, done: Int): String {
        val res = if (kind == TransferKind.UPLOAD) R.plurals.notification_uploaded else R.plurals.notification_downloaded
        return context.resources.getQuantityString(res, done, done)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun viewFileIntent(uri: Uri): PendingIntent = PendingIntent.getActivity(
        context,
        uri.hashCode(),
        Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun foregroundId(batchId: String): Int = batchId.hashCode() and ID_MASK

    private fun finishedId(batchId: String): Int = (batchId.hashCode() and ID_MASK) xor FINISHED_ID_SALT

    private companion object {
        const val CHANNEL_ID = "transfers"
        const val PERCENT = 100
        const val ID_MASK = 0x0FFFFFFF
        const val FINISHED_ID_SALT = 0x10000000
    }
}
