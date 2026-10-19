package io.swiftfolio.app.data.aws

import java.io.InputStream

/**
 * Reads an [InputStream] in fixed-size chunks with one chunk of look-ahead, so the uploader
 * knows up front whether a single PutObject is enough. Works without knowing the stream
 * length and avoids `InputStream.readNBytes` (API 33+ on Android).
 *
 * The constructor performs blocking reads; create it on an I/O dispatcher.
 */
internal class ChunkReader(private val input: InputStream, private val chunkSize: Int) {
    private var current: ByteArray? = readChunk()
    private var following: ByteArray? = readFollowing(current)

    /** True when the whole stream fits in one chunk (possibly empty). Fixed at construction. */
    val isSingleChunk: Boolean = following == null

    /** Returns the next chunk, or null when the stream is exhausted. An empty stream yields one empty chunk. */
    fun next(): ByteArray? {
        val chunk = current ?: return null
        current = following
        following = readFollowing(current)
        return chunk
    }

    private fun readFollowing(chunk: ByteArray?): ByteArray? =
        if (chunk != null && chunk.size == chunkSize) readChunk().takeIf { it.isNotEmpty() } else null

    private fun readChunk(): ByteArray {
        val buffer = ByteArray(chunkSize)
        var filled = 0
        while (filled < chunkSize) {
            val read = input.read(buffer, filled, chunkSize - filled)
            if (read < 0) break
            filled += read
        }
        return if (filled == chunkSize) buffer else buffer.copyOf(filled)
    }
}
