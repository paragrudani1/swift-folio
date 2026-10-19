package io.swiftfolio.app.transfer

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import io.swiftfolio.app.AppConstants
import io.swiftfolio.core.S3Path
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Where a downloaded file is written. Call [openOutputStream] once, then exactly one of
 * [commit] (makes the file visible) or [discard] (removes the partial file).
 */
interface DownloadTarget {
    /** Content URI (as text) that other apps can open after [commit], or null when not shareable. */
    val shareableUri: String?

    /** Opens the destination for writing. Throws [IOException] when it cannot be created. */
    fun openOutputStream(): OutputStream

    /** Publishes the finished file. */
    fun commit()

    /** Deletes the partial file. */
    fun discard()
}

/** Where finished downloads are placed; decides the completion wording. */
enum class DownloadLocation {
    /** The public `Download/SwiftFolio` folder; kept after uninstall. */
    PUBLIC_DOWNLOADS,

    /** The app's own external files; deleted on uninstall (API 26-28 without storage permission). */
    APP_STORAGE,
}

/** Creates destinations for downloads. */
interface DownloadTargets {
    /** Where targets created now are placed. */
    val location: DownloadLocation

    /** A target for a file called [fileName]. Throws [IOException] when no destination can be created. */
    fun create(fileName: String): DownloadTarget
}

/**
 * Creates [DownloadTarget]s in the public `Download/SwiftFolio` folder: through MediaStore on
 * API 29+, or as a file plus a media scan on API 26-28 when WRITE_EXTERNAL_STORAGE was granted.
 * Without that permission on API 26-28, files go to the app's external Downloads folder.
 * Files written on API 26-28 are shared through the app's FileProvider.
 */
class DownloadTargetFactory(private val context: Context) : DownloadTargets {

    override val location: DownloadLocation
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || canWritePublicDownloads()) {
            DownloadLocation.PUBLIC_DOWNLOADS
        } else {
            DownloadLocation.APP_STORAGE
        }

    override fun create(fileName: String): DownloadTarget {
        val mimeType = S3Path.extension(fileName)?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: DEFAULT_MIME_TYPE
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStoreTarget(context.contentResolver, fileName, mimeType)
            location == DownloadLocation.PUBLIC_DOWNLOADS -> FileTarget(context, legacyPublicDir(), fileName, mimeType, scan = true)
            else -> FileTarget(context, appDownloadsDir(), fileName, mimeType, scan = false)
        }
    }

    private fun canWritePublicDownloads(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION") // Only used on API 26-28, where this is the public Downloads folder.
    private fun legacyPublicDir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SUBFOLDER)

    private fun appDownloadsDir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, SUBFOLDER)

    internal companion object {
        const val SUBFOLDER = "SwiftFolio"
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private class MediaStoreTarget(
    private val resolver: ContentResolver,
    fileName: String,
    mimeType: String,
) : DownloadTarget {
    private val uri: Uri = resolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + DownloadTargetFactory.SUBFOLDER)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        },
    ) ?: throw IOException("MediaStore refused to create the download")

    override val shareableUri: String = uri.toString()

    override fun openOutputStream(): OutputStream =
        resolver.openOutputStream(uri) ?: throw IOException("MediaStore returned no stream")

    override fun commit() {
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    }

    override fun discard() {
        resolver.delete(uri, null, null)
    }
}

/** A plain file (API 26-28), shared through the FileProvider and optionally media-scanned. */
private class FileTarget(
    private val context: Context,
    directory: File,
    fileName: String,
    private val mimeType: String,
    private val scan: Boolean,
) : DownloadTarget {
    private val file: File = uniqueFile(directory.apply { mkdirs() }, fileName)

    override val shareableUri: String? = try {
        FileProvider.getUriForFile(context, context.packageName + AppConstants.FILE_PROVIDER_SUFFIX, file).toString()
    } catch (error: IllegalArgumentException) {
        Log.w(TAG, "Download folder is not shared by the FileProvider: ${error::class.java.simpleName}")
        null
    }

    override fun openOutputStream(): OutputStream = FileOutputStream(file)

    override fun commit() {
        if (scan) MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
    }

    override fun discard() {
        if (file.exists() && !file.delete()) Log.w(TAG, "Partial download could not be deleted")
    }

    private fun uniqueFile(directory: File, fileName: String): File {
        val candidate = File(directory, fileName)
        if (!candidate.exists()) return candidate
        val stem = candidate.nameWithoutExtension
        val suffix = candidate.extension.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
        return generateSequence(1) { it + 1 }.map { File(directory, "$stem ($it)$suffix") }.first { !it.exists() }
    }

    private companion object {
        const val TAG = "DownloadTargets"
    }
}
