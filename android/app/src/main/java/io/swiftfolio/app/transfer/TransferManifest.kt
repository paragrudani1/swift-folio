package io.swiftfolio.app.transfer

import kotlinx.serialization.Serializable

/**
 * The persisted description of a batch, written before the work is enqueued so the worker
 * and the UI can recover names and keys after process death. Contains no credentials.
 *
 * @property source content URI (uploads) or null (downloads).
 */
@Serializable
data class ManifestItem(
    val name: String,
    val key: String,
    val sizeBytes: Long? = null,
    val source: String? = null,
    val contentType: String? = null,
)

/** A batch as stored on disk. */
@Serializable
data class TransferManifest(
    val batchId: String,
    val kind: TransferKind,
    val bucket: String,
    val prefix: String,
    val items: List<ManifestItem>,
)
