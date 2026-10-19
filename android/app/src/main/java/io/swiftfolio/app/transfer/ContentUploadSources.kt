package io.swiftfolio.app.transfer

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import io.swiftfolio.app.data.UploadSource
import io.swiftfolio.core.UploadPlanner
import java.io.FileNotFoundException
import java.io.InputStream

/** Opens the payload of an upload item and manages its read grant. */
interface UploadSourceProvider {
    /** An [UploadSource] for [item]. Opening its stream throws when the document is gone or unreadable. */
    fun open(item: ManifestItem): UploadSource

    /** Releases the persistable read grant of [item], if one was taken. Idempotent; never throws. */
    fun release(item: ManifestItem)
}

/**
 * Bridges picked documents (content URIs) to [UploadSource]s. Takes a persistable read
 * grant when the provider allows it, so a queued upload survives process death.
 */
class ContentUploadSources(private val resolver: ContentResolver) : UploadSourceProvider {

    /** Reads name, size and type of [uri] and plans its key under [prefix]; null if unreadable. */
    fun describe(uri: String, prefix: String): ManifestItem? {
        val parsed = Uri.parse(uri)
        val (name, size) = queryNameAndSize(parsed) ?: return null
        keepReadAccess(parsed)
        return ManifestItem(
            name = name,
            key = UploadPlanner.objectKey(prefix, name),
            sizeBytes = size,
            source = uri,
            contentType = resolver.getType(parsed),
        )
    }

    override fun open(item: ManifestItem): UploadSource {
        val uri = Uri.parse(requireNotNull(item.source) { "Upload item has no source URI" })
        return object : UploadSource {
            override val sizeBytes: Long? = item.sizeBytes
            override val contentType: String? = item.contentType
            override fun openStream(): InputStream =
                resolver.openInputStream(uri) ?: throw FileNotFoundException("Provider returned no stream")
        }
    }

    override fun release(item: ManifestItem) {
        val source = item.source ?: return
        try {
            resolver.releasePersistableUriPermission(Uri.parse(source), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (error: SecurityException) {
            Log.d(TAG, "No persistable grant to release: ${error::class.java.simpleName}")
        }
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long?>? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return try {
            resolver.query(uri, projection, null, null, null)?.use { cursor -> readRow(cursor, uri) }
        } catch (error: SecurityException) {
            Log.w(TAG, "Cannot read picked document: ${error::class.java.simpleName}")
            null
        }
    }

    private fun readRow(cursor: Cursor, uri: Uri): Pair<String, Long?>? {
        if (!cursor.moveToFirst()) return null
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        val name = (if (nameIndex >= 0) cursor.getString(nameIndex) else null) ?: uri.lastPathSegment ?: return null
        val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
        return name to size
    }

    private fun keepReadAccess(uri: Uri) {
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (error: SecurityException) {
            Log.d(TAG, "Provider offers no persistable grant; upload needs the app process alive: ${error::class.java.simpleName}")
        }
    }

    private companion object {
        const val TAG = "UploadSources"
    }
}
