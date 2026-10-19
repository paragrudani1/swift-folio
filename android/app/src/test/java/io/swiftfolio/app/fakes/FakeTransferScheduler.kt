package io.swiftfolio.app.fakes

import io.swiftfolio.app.transfer.TransferKind
import io.swiftfolio.app.transfer.TransferQueue
import io.swiftfolio.app.transfer.TransferScheduler
import kotlinx.coroutines.flow.MutableStateFlow

/** Records requests; the test drives [queueState] to simulate progress. */
class FakeTransferScheduler : TransferScheduler {
    val queueState = MutableStateFlow(TransferQueue())
    val uploadRequests = mutableListOf<Triple<String, String, List<String>>>()
    val downloadRequests = mutableListOf<Pair<String, List<String>>>()
    val cancelledKinds = mutableListOf<TransferKind>()
    val cancelledItems = mutableListOf<Pair<String, Int>>()
    var readableUploads: Int? = null
    var clearedFinished = 0

    override val queue = queueState

    override suspend fun enqueueUploads(bucket: String, prefix: String, uris: List<String>): Int {
        uploadRequests += Triple(bucket, prefix, uris)
        return readableUploads ?: uris.size
    }

    override suspend fun enqueueDownloads(bucket: String, keys: List<String>): Int {
        downloadRequests += bucket to keys
        return keys.size
    }

    override suspend fun cancelAll(kind: TransferKind) {
        cancelledKinds += kind
    }

    override fun cancelItem(batchId: String, itemIndex: Int) {
        cancelledItems += batchId to itemIndex
    }

    override suspend fun clearFinished() {
        clearedFinished++
    }
}
