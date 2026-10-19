package io.swiftfolio.app.data.aws

import java.io.FilterOutputStream
import java.io.OutputStream

/**
 * Forwards writes to [delegate] and reports the running byte count after each write.
 * Closing this stream does not close [delegate]; the caller owns it.
 */
internal class CountingOutputStream(
    delegate: OutputStream,
    private val onCount: (Long) -> Unit,
) : FilterOutputStream(delegate) {
    private var count = 0L

    override fun write(b: Int) {
        out.write(b)
        advance(1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        advance(len.toLong())
    }

    override fun close() {
        flush()
    }

    private fun advance(bytes: Long) {
        count += bytes
        onCount(count)
    }
}
