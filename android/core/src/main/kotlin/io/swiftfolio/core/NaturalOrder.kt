package io.swiftfolio.core

/**
 * Case-insensitive "natural" string order, so `file2` sorts before `file10`.
 *
 * Ties (strings equal ignoring case) fall back to plain ordinal order, which keeps the
 * comparator consistent with equals and the sort stable across runs.
 */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val natural = compareChunks(chunk(a.lowercase()), chunk(b.lowercase()))
        return if (natural != 0) natural else a.compareTo(b)
    }

    private fun compareChunks(left: List<String>, right: List<String>): Int {
        for (index in 0 until minOf(left.size, right.size)) {
            val result = compareChunk(left[index], right[index])
            if (result != 0) return result
        }
        return left.size.compareTo(right.size)
    }

    private fun compareChunk(a: String, b: String): Int {
        val bothNumeric = a.first().isDigit() && b.first().isDigit()
        if (!bothNumeric) return a.compareTo(b)
        val aDigits = a.trimStart('0')
        val bDigits = b.trimStart('0')
        if (aDigits.length != bDigits.length) return aDigits.length.compareTo(bDigits.length)
        val byValue = aDigits.compareTo(bDigits)
        return if (byValue != 0) byValue else a.length.compareTo(b.length)
    }

    private fun chunk(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (char in text) {
            if (current.isNotEmpty() && current.last().isDigit() != char.isDigit()) {
                chunks += current.toString()
                current.clear()
            }
            current.append(char)
        }
        chunks += current.toString()
        return chunks
    }
}
