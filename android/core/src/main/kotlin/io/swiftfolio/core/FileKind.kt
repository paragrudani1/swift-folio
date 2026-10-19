package io.swiftfolio.core

/**
 * File family used for icon and tint selection.
 *
 * The extension table is data, not branching logic: add an extension to an entry's
 * [extensions] set to classify it, or add a new entry. The table mirrors
 * `FILE_EXTENSIONS` in the web app's `constants.ts`.
 */
enum class FileKind(val extensions: Set<String>) {
    IMAGE(setOf("jpg", "jpeg", "png", "gif", "svg", "webp", "bmp", "ico")),
    VIDEO(setOf("mp4", "avi", "mov", "wmv", "flv", "webm")),
    AUDIO(setOf("mp3", "wav", "flac", "aac", "ogg")),
    PDF(setOf("pdf")),
    DOCUMENT(setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp")),
    ARCHIVE(setOf("zip", "rar", "7z", "tar", "gz", "bz2")),
    CODE(
        setOf(
            "js", "ts", "jsx", "tsx", "html", "css", "scss", "sass", "less", "php", "py",
            "java", "cpp", "c", "cs", "rb", "go", "rs", "swift", "kt",
        ),
    ),
    TEXT(setOf("txt", "md", "markdown", "log", "json", "xml", "csv", "yaml", "yml")),
    OTHER(emptySet());

    companion object {
        private val byExtension: Map<String, FileKind> =
            entries.flatMap { kind -> kind.extensions.map { it to kind } }.toMap()

        /** Classifies a file name (or key) by its extension; unknown or missing -> [OTHER]. */
        fun of(name: String): FileKind {
            val extension = S3Path.extension(S3Path.fileName(name)) ?: return OTHER
            return byExtension[extension] ?: OTHER
        }
    }
}
