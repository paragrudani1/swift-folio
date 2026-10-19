package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FileKindTest {
    @Test fun `classifies the web app extension table`() {
        assertEquals(FileKind.IMAGE, FileKind.of("photo.JPG"))
        assertEquals(FileKind.VIDEO, FileKind.of("clip.webm"))
        assertEquals(FileKind.AUDIO, FileKind.of("song.flac"))
        assertEquals(FileKind.PDF, FileKind.of("invoice.pdf"))
        assertEquals(FileKind.DOCUMENT, FileKind.of("plan.xlsx"))
        assertEquals(FileKind.ARCHIVE, FileKind.of("backup.tar.gz"))
        assertEquals(FileKind.CODE, FileKind.of("Main.kt"))
        assertEquals(FileKind.TEXT, FileKind.of("data.json"))
    }

    @Test fun `unknown or missing extension is OTHER`() {
        assertEquals(FileKind.OTHER, FileKind.of("binary.xyz"))
        assertEquals(FileKind.OTHER, FileKind.of("Makefile"))
        assertEquals(FileKind.OTHER, FileKind.of("folder/.hidden"))
    }

    @Test fun `classification uses the last path segment`() {
        assertEquals(FileKind.IMAGE, FileKind.of("dir.pdf/picture.png"))
    }

    @Test fun `no extension belongs to two kinds`() {
        val all = FileKind.entries.flatMap { it.extensions }
        assertEquals(all.size, all.toSet().size)
    }
}
