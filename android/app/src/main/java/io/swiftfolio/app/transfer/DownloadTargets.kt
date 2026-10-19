package io.swiftfolio.app.transfer

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
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
    /** Content URI that can be opened after [commit], or null when not shareable. */
    val uri: Uri?

    /** Opens the destination for writing. Throws [IOException] when it cannot be created. */
    fun openOutputStream(): OutputStream

    /** Publishes the finished file. */
    fun commit()

    /** Deletes the partial file. */
    fun discard()
}

/**
 * Creates [DownloadTarget]s: the public `Download/SwiftFolio` collection via MediaStore on
 * API 29+, or the app-specific external Downloads directory on API 26-28 (no storage
 * permission needed on either).
 */
class DownloadTargetFactory(private val context: Context) {

    /** A target for a file called [fileName]. */
    fun create(fileName: String): DownloadTarget {
        val mimeType = S3Path.extension(fileName)?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: DEFAULT_MIME_TYPE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStoreTarget(context.contentResolver, fileName, mimeType)
        } else {
            AppFilesTarget(appDownloadsDir(), fileName)
        }
    }

    private fun appDownloadsDir(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(base, SUBFOLDER)
    }

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
    override val uri: Uri = resolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + DownloadTargetFactory.SUBFOLDER)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        },
    ) ?: throw IOException("MediaStore refused to create the download")

    override fun openOutputStream(): OutputStream =
        resolver.openOutputStream(uri) ?: throw IOException("MediaStore returned no stream")

    override fun commit() {
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    }

    override fun discard() {
        resolver.delete(uri, null, null)
    }
}

private class AppFilesTarget(directory: File, fileName: String) : DownloadTarget {
    private val file: File = uniqueFile(directory.apply { mkdirs() }, fileName)

    override val uri: Uri? = null

    override fun openOutputStream(): OutputStream = FileOutputStream(file)

    override fun commit() = Unit

    override fun discard() {
        file.delete()
    }

    private fun uniqueFile(directory: File, fileName: String): File {
        val candidate = File(directory, fileName)
        if (!candidate.exists()) return candidate
        val stem = candidate.nameWithoutExtension
        val suffix = candidate.extension.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()
        return generateSequence(1) { it + 1 }.map { File(directory, "$stem ($it)$suffix") }.first { !it.exists() }
    }
}
