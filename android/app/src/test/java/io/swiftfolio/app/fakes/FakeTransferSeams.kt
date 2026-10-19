package io.swiftfolio.app.fakes

import io.swiftfolio.app.data.UploadSource
import io.swiftfolio.app.transfer.DownloadLocation
import io.swiftfolio.app.transfer.DownloadTarget
import io.swiftfolio.app.transfer.DownloadTargets
import io.swiftfolio.app.transfer.ManifestItem
import io.swiftfolio.app.transfer.UploadSourceProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Upload sources backed by empty streams; records released grants. */
class FakeUploadSources : UploadSourceProvider {
    val released = mutableListOf<String>()

    override fun open(item: ManifestItem): UploadSource = object : UploadSource {
        override val sizeBytes: Long? = item.sizeBytes
        override val contentType: String? = item.contentType
        override fun openStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    }

    override fun release(item: ManifestItem) {
        released += item.key
    }
}

/** Download targets in memory; records which were committed or discarded. */
class FakeDownloadTargets : DownloadTargets {
    val committed = mutableListOf<String>()
    val discarded = mutableListOf<String>()

    override val location: DownloadLocation = DownloadLocation.PUBLIC_DOWNLOADS

    override fun create(fileName: String): DownloadTarget = object : DownloadTarget {
        override val shareableUri: String = "content://downloads/$fileName"
        override fun openOutputStream(): OutputStream = ByteArrayOutputStream()
        override fun commit() {
            committed += fileName
        }
        override fun discard() {
            discarded += fileName
        }
    }
}
