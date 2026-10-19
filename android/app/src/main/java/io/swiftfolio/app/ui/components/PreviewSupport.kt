package io.swiftfolio.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.swiftfolio.app.ui.theme.SwiftFolioTheme
import io.swiftfolio.app.ui.theme.aurora
import io.swiftfolio.core.ObjectEntry

/** Wraps preview content in the theme and the aurora canvas. */
@Composable
fun PreviewSurface(darkTheme: Boolean, content: @Composable () -> Unit) {
    SwiftFolioTheme(darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize().aurora()) { content() }
    }
}

/** Deterministic sample data for previews. */
object PreviewData {
    /** A fixed timestamp (2026-10-17) so previews never change. */
    private const val SAMPLE_TIME = 1_792_222_320_000L

    /** A small listing with every row type. */
    val entries: List<ObjectEntry> = listOf(
        ObjectEntry.Folder("photos/2026/"),
        ObjectEntry.Folder("photos/raw/"),
        ObjectEntry.File("photos/invoice-2026-final.pdf", 2_516_582, SAMPLE_TIME),
        ObjectEntry.File("photos/sunset.jpg", 4_200_000, SAMPLE_TIME),
        ObjectEntry.File("photos/backup.tar.gz", 310_000_000, SAMPLE_TIME),
        ObjectEntry.File("photos/notes.md", 1_800, SAMPLE_TIME),
    )
}
